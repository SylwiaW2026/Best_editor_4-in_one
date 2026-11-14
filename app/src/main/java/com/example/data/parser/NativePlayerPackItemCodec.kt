package com.example.data.parser

import com.example.data.model.DiabloItem
import com.example.data.model.EquipSlot
import com.example.data.model.HeroCharacter
import com.example.data.model.NativeItemPack
import com.example.data.model.NativeItemState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

internal data class NativeItemDecodeResult(
    val equipment: Map<EquipSlot, DiabloItem>,
    val inventory: List<DiabloItem>,
    val belt: List<DiabloItem?>,
    val inventoryGrid: List<Int>,
    val state: NativeItemState
)

internal data class NativeItemPayload(
    val equipment: ByteArray,
    val inventory: ByteArray,
    val inventoryGrid: ByteArray,
    val inventoryCount: Int,
    val belt: ByteArray
)

internal object NativePlayerPackItemCodec {
    const val PLAYER_PACK_SIZE = 1266
    const val ITEM_COUNT = 40
    const val BELT_SIZE = 8
    const val EQUIPMENT_OFFSET = 0x7C
    const val INVENTORY_OFFSET = 0x101
    const val INVENTORY_GRID_OFFSET = 0x3F9
    const val INVENTORY_COUNT_OFFSET = 0x421
    const val BELT_OFFSET = 0x422

    private val equipmentSlots = listOf(
        EquipSlot.HEAD,
        EquipSlot.LEFT_RING,
        EquipSlot.RIGHT_RING,
        EquipSlot.NECK,
        EquipSlot.LEFT_HAND,
        EquipSlot.RIGHT_HAND,
        EquipSlot.TORSO
    )

    fun decode(raw: ByteArray, isHellfire: Boolean, isSpawn: Boolean): NativeItemDecodeResult {
        require(raw.size >= PLAYER_PACK_SIZE) {
            "Hero payload too small: ${raw.size} bytes; expected at least $PLAYER_PACK_SIZE."
        }
        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        val count = buffer.get(INVENTORY_COUNT_OFFSET).toInt() and 0xFF
        require(count <= ITEM_COUNT) {
            "Native inventory count $count exceeds the $ITEM_COUNT supported slots."
        }

        val body = List(equipmentSlots.size) { index ->
            NativeItemPack.read(buffer, EQUIPMENT_OFFSET + index * NativeItemPack.SIZE)
        }
        val inventory = List(ITEM_COUNT) { index ->
            NativeItemPack.read(buffer, INVENTORY_OFFSET + index * NativeItemPack.SIZE)
        }
        val grid = List(ITEM_COUNT) { index ->
            buffer.get(INVENTORY_GRID_OFFSET + index).toInt()
        }
        val belt = List(BELT_SIZE) { index ->
            NativeItemPack.read(buffer, BELT_OFFSET + index * NativeItemPack.SIZE)
        }
        val state = NativeItemState(
            equipment = body,
            inventory = inventory,
            inventoryGrid = grid,
            inventoryCount = count,
            belt = belt,
            isHellfire = isHellfire,
            isSpawn = isSpawn
        )

        val equipmentItems = equipmentSlots.mapIndexedNotNull { index, slot ->
            itemFromPack(body[index])?.let { slot to it }
        }.toMap()
        val inventoryItems = (0 until count).mapNotNull { index ->
            itemFromPack(inventory[index], index, grid)
        }
        val beltItems = belt.map { itemFromPack(it) }

        return NativeItemDecodeResult(
            equipment = equipmentItems,
            inventory = inventoryItems,
            belt = beltItems,
            inventoryGrid = grid,
            state = state
        )
    }

    fun encode(hero: HeroCharacter, isHellfire: Boolean, isSpawn: Boolean): NativeItemPayload {
        val state = requireNotNull(hero.nativeItemState) {
            "The character has no native item baseline. Import an original .sv/.hsv save first."
        }
        require(state.equipment.size == equipmentSlots.size)
        require(state.inventory.size == ITEM_COUNT)
        require(state.inventoryGrid.size == ITEM_COUNT)
        require(state.belt.size == BELT_SIZE)
        require(state.inventoryCount in 0..ITEM_COUNT)
        require(hero.belt.size == BELT_SIZE) {
            "Native belt must contain exactly $BELT_SIZE slots."
        }
        require(state.isHellfire == isHellfire && state.isSpawn == isSpawn) {
            "Changing Vanilla/Hellfire or Spawn mode requires item index conversion, which is not supported."
        }
        require(hero.nativeInventoryGrid.size == ITEM_COUNT) {
            "Native inventory grid must contain exactly $ITEM_COUNT cells."
        }
        require(hero.nativeInventoryGrid.all { it in -ITEM_COUNT..ITEM_COUNT }) {
            "Native inventory grid contains an item reference outside supported bounds."
        }
        require(hero.inventory.size <= ITEM_COUNT) {
            "Native inventory supports at most $ITEM_COUNT items."
        }

        val equipmentRecords = state.equipment.toMutableList()
        equipmentSlots.forEachIndexed { index, slot ->
            val item = hero.equipment[slot]
            equipmentRecords[index] = if (item == null) {
                if (state.equipment[index].isEmpty) state.equipment[index] else NativeItemPack.EMPTY
            } else {
                item.nativePack ?: unsupportedItem(item)
            }
        }

        val inventoryPayload = encodeInventory(hero, state)
        val beltRecords = state.belt.mapIndexed { index, oldPack ->
            val item = hero.belt.getOrNull(index)
            if (item == null) {
                if (oldPack.isEmpty) oldPack else NativeItemPack.EMPTY
            } else {
                item.nativePack ?: unsupportedItem(item)
            }
        }

        return NativeItemPayload(
            equipment = encodeRecords(equipmentRecords),
            inventory = encodeRecords(inventoryPayload.records),
            inventoryGrid = inventoryPayload.grid.map { it.toByte() }.toByteArray(),
            inventoryCount = inventoryPayload.count,
            belt = encodeRecords(beltRecords)
        )
    }

    private data class EncodedInventory(
        val records: List<NativeItemPack>,
        val grid: List<Int>,
        val count: Int
    )

    private fun encodeInventory(hero: HeroCharacter, state: NativeItemState): EncodedInventory {
        val visibleBaseline = (0 until state.inventoryCount).mapNotNull { index ->
            state.inventory[index].takeUnless { it.isEmpty }?.let { index to it }
        }
        val unchanged = hero.inventory.size == visibleBaseline.size &&
            hero.inventory.indices.all { itemIndex ->
                val item = hero.inventory[itemIndex]
                val baseline = visibleBaseline[itemIndex]
                item.nativeInventoryIndex == baseline.first && item.nativePack == baseline.second
            } &&
            hero.nativeInventoryGrid == state.inventoryGrid

        if (unchanged) {
            return EncodedInventory(state.inventory, state.inventoryGrid, state.inventoryCount)
        }

        val outputIndices = mutableMapOf<Int, Int>()
        hero.inventory.forEachIndexed { outputIndex, item ->
            val nativeIndex = item.nativeInventoryIndex
            if (nativeIndex != null) {
                require(nativeIndex in 0 until state.inventoryCount) {
                    "Inventory item '${item.name}' has an invalid native slot $nativeIndex."
                }
                require(outputIndices.put(nativeIndex, outputIndex) == null) {
                    "Two inventory items refer to native slot $nativeIndex."
                }
            } else {
                require(item.gridWidth in 1..10 && item.gridHeight in 1..4) {
                    "Cannot place '${item.name}' in the native inventory without verified item dimensions."
                }
            }
        }

        val outputGrid = MutableList(ITEM_COUNT) { 0 }
        hero.nativeInventoryGrid.forEachIndexed { cell, oldReference ->
            if (oldReference == 0) return@forEachIndexed
            val oldIndex = abs(oldReference) - 1
            require(oldIndex in 0 until state.inventoryCount) {
                "Inventory grid cell $cell refers to invalid native item ${abs(oldReference)}."
            }
            require(!state.inventory[oldIndex].isEmpty) {
                "Inventory grid cell $cell refers to an empty native inventory record."
            }
            val newIndex = outputIndices[oldIndex] ?: return@forEachIndexed
            outputGrid[cell] = if (oldReference > 0) newIndex + 1 else -(newIndex + 1)
        }

        val records = MutableList(ITEM_COUNT) { NativeItemPack.EMPTY }
        hero.inventory.forEachIndexed { index, item ->
            records[index] = item.nativePack ?: unsupportedItem(item)
            if (item.nativeInventoryIndex == null) {
                placeInInventory(outputGrid, index, item.gridWidth, item.gridHeight)
            }
        }

        return EncodedInventory(records, outputGrid, hero.inventory.size)
    }

    private fun placeInInventory(grid: MutableList<Int>, itemIndex: Int, width: Int, height: Int) {
        val gridId = itemIndex + 1
        for (y in 0..(4 - height)) {
            for (x in 0..(10 - width)) {
                val cells = (0 until height).flatMap { row ->
                    (0 until width).map { column -> (y + row) * 10 + x + column }
                }
                if (cells.any { grid[it] != 0 }) continue
                cells.forEach { cell ->
                    val cellX = cell % 10
                    val cellY = cell / 10
                    grid[cell] = if (cellX == x && cellY == y + height - 1) gridId else -gridId
                }
                return
            }
        }
        throw IllegalArgumentException("No free inventory space for item slot $gridId.")
    }

    private fun itemFromPack(
        pack: NativeItemPack,
        inventoryIndex: Int? = null,
        grid: List<Int>? = null
    ): DiabloItem? {
        if (pack.isEmpty) return null
        val geometry = inventoryIndex?.let { index ->
            grid?.let { cells -> itemGridGeometry(cells, index) }
        }
        return DiabloItem(
            name = "Native item #${pack.index}",
            baseType = "Native item",
            durability = pack.durability,
            maxDurability = pack.maxDurability,
            charges = pack.charges,
            maxCharges = pack.maxCharges,
            gridX = geometry?.first ?: -1,
            gridY = geometry?.second ?: -1,
            gridWidth = geometry?.third ?: 0,
            gridHeight = geometry?.fourth ?: 0,
            iSeed = pack.seed.toInt(),
            itemIndex = pack.index,
            nativePack = pack,
            nativeInventoryIndex = inventoryIndex
        )
    }

    private fun itemGridGeometry(grid: List<Int>, inventoryIndex: Int): Quadruple? {
        val cells = grid.indices.filter { abs(grid[it]) == inventoryIndex + 1 }
        if (cells.isEmpty()) return null
        val x = cells.minOf { it % 10 }
        val y = cells.minOf { it / 10 }
        val width = cells.maxOf { it % 10 } - x + 1
        val height = cells.maxOf { it / 10 } - y + 1
        require(cells.size == width * height) {
            "Native inventory item ${inventoryIndex + 1} has a non-rectangular grid footprint."
        }
        return Quadruple(x, y, width, height)
    }

    private data class Quadruple(
        val first: Int,
        val second: Int,
        val third: Int,
        val fourth: Int
    )

    private fun encodeRecords(records: List<NativeItemPack>): ByteArray {
        return ByteArray(records.size * NativeItemPack.SIZE).also { bytes ->
            records.forEachIndexed { index, pack ->
                pack.toByteArray().copyInto(bytes, index * NativeItemPack.SIZE)
            }
        }
    }

    private fun unsupportedItem(item: DiabloItem): Nothing {
        throw IllegalArgumentException(
            "'${item.fullTitle}' has no verified native Diablo item record and cannot be written to this save."
        )
    }
}
