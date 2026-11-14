package com.example.data.parser

import com.example.data.model.DiabloItem
import com.example.data.model.EquipSlot
import com.example.data.model.HeroCharacter
import com.example.data.model.ItemArmory
import com.example.data.model.NativeItemPack
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NativePlayerPackItemCodecTest {
    @Test
    fun nativeItemPackRoundTripsEveryPackedField() {
        val pack = NativeItemPack(
            seed = 0xFEDCBA98L,
            createInfo = 0xABCD,
            index = 0x5432,
            idFlags = 0x81,
            durability = 254,
            maxDurability = 255,
            charges = 7,
            maxCharges = 16,
            value = 0x1234,
            buff = 0x87654321L
        )

        assertEquals(pack, NativeItemPack.read(ByteBuffer.wrap(pack.toByteArray()), 0))
    }

    @Test
    fun zeroSeedWithIndexZeroIsNotAnEmptyItem() {
        val pack = NativeItemPack(
            seed = 0,
            createInfo = 0,
            index = 0,
            idFlags = 0,
            durability = 0,
            maxDurability = 0,
            charges = 0,
            maxCharges = 0,
            value = 0,
            buff = 0
        )

        assertFalse(pack.isEmpty)
    }

    @Test
    fun unchangedNativeItemsPreserveAllRecordsAndGridBytes() {
        val raw = playerPackWithItems()
        val decoded = NativePlayerPackItemCodec.decode(raw, isHellfire = false, isSpawn = false)
        val hero = HeroCharacter(
            equipment = decoded.equipment,
            inventory = decoded.inventory,
            belt = decoded.belt,
            nativeInventoryGrid = decoded.inventoryGrid,
            nativeItemState = decoded.state
        )

        val encoded = NativePlayerPackItemCodec.encode(hero, isHellfire = false, isSpawn = false)

        assertArrayEquals(raw.copyOfRange(0x7C, 0x101), encoded.equipment)
        assertArrayEquals(raw.copyOfRange(0x101, 0x3F9), encoded.inventory)
        assertArrayEquals(raw.copyOfRange(0x3F9, 0x421), encoded.inventoryGrid)
        assertArrayEquals(raw.copyOfRange(0x422, 0x4BA), encoded.belt)
        assertEquals(2, encoded.inventoryCount)
        assertEquals(2, decoded.inventory.size)
        assertEquals(1, decoded.belt.count { it != null })
    }

    @Test
    fun removingAnInventoryItemCompactsRecordsAndRemapsGridReferences() {
        val decoded = NativePlayerPackItemCodec.decode(
            playerPackWithItems(),
            isHellfire = false,
            isSpawn = false
        )

        val encoded = NativePlayerPackItemCodec.encode(
            HeroCharacter(
                equipment = decoded.equipment,
                inventory = listOf(decoded.inventory[1]),
                belt = decoded.belt,
                nativeInventoryGrid = decoded.inventoryGrid,
                nativeItemState = decoded.state
            ),
            isHellfire = false,
            isSpawn = false
        )

        assertEquals(1, encoded.inventoryCount)
        assertEquals(0, encoded.inventoryGrid[0])
        assertEquals(-1, encoded.inventoryGrid[2])
        assertEquals(1, encoded.inventoryGrid[12])
        assertEquals(-1, encoded.inventoryGrid[13])
        assertEquals(decoded.inventory[1].nativePack, NativeItemPack.read(ByteBuffer.wrap(encoded.inventory), 0))
        assertEquals(NativeItemPack.EMPTY, NativeItemPack.read(ByteBuffer.wrap(encoded.inventory), NativeItemPack.SIZE))
    }

    @Test
    fun equippingAnInventoryItemMovesItsPackOutOfTheInventory() {
        val decoded = NativePlayerPackItemCodec.decode(
            playerPackWithItems(),
            isHellfire = false,
            isSpawn = false
        )
        val movedItem = decoded.inventory[0]
        val hero = HeroCharacter(
            equipment = decoded.equipment + (EquipSlot.NECK to movedItem),
            inventory = listOf(decoded.inventory[1]),
            belt = decoded.belt,
            nativeInventoryGrid = decoded.inventoryGrid,
            nativeItemState = decoded.state
        )

        val encoded = NativePlayerPackItemCodec.encode(hero, isHellfire = false, isSpawn = false)

        assertEquals(movedItem.nativePack, NativeItemPack.read(ByteBuffer.wrap(encoded.equipment), 3 * NativeItemPack.SIZE))
        assertEquals(decoded.inventory[1].nativePack, NativeItemPack.read(ByteBuffer.wrap(encoded.inventory), 0))
        assertEquals(NativeItemPack.EMPTY, NativeItemPack.read(ByteBuffer.wrap(encoded.inventory), NativeItemPack.SIZE))
        assertEquals(0, encoded.inventoryGrid[0])
        assertEquals(-1, encoded.inventoryGrid[2])
    }

    @Test
    fun catalogItemsWithoutVerifiedNativeRecordsCannotBeSaved() {
        val decoded = NativePlayerPackItemCodec.decode(
            emptyPlayerPack(),
            isHellfire = false,
            isSpawn = false
        )
        val hero = HeroCharacter(
            equipment = mapOf(EquipSlot.HEAD to ItemArmory.FULL_ITEM_LIBRARY.first()),
            nativeItemState = decoded.state
        )

        assertThrows(IllegalArgumentException::class.java) {
            NativePlayerPackItemCodec.encode(hero, isHellfire = false, isSpawn = false)
        }
    }

    private fun playerPackWithItems(): ByteArray {
        val raw = emptyPlayerPack()
        putItem(raw, 0x7C, samplePack(index = 0x2345))
        putItem(raw, 0x7C + NativeItemPack.SIZE, samplePack(index = 0x3456))
        putItem(raw, 0x101, samplePack(index = 0x4567))
        putItem(raw, 0x101 + NativeItemPack.SIZE, samplePack(index = 0x5678))
        putItem(raw, 0x422, samplePack(index = 0x6789))
        raw[0x421] = 2
        raw[0x3F9] = 1
        raw[0x3F9 + 2] = -2
        raw[0x3F9 + 3] = -2
        raw[0x3F9 + 12] = 2
        raw[0x3F9 + 13] = -2
        return raw
    }

    private fun emptyPlayerPack(): ByteArray {
        val raw = ByteArray(1266)
        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        for (index in 0 until 7) {
            buffer.putShort(0x7C + index * NativeItemPack.SIZE + 6, 0xFFFF.toShort())
        }
        for (index in 0 until 40) {
            buffer.putShort(0x101 + index * NativeItemPack.SIZE + 6, 0xFFFF.toShort())
        }
        for (index in 0 until 8) {
            buffer.putShort(0x422 + index * NativeItemPack.SIZE + 6, 0xFFFF.toShort())
        }
        return raw
    }

    private fun putItem(raw: ByteArray, offset: Int, item: NativeItemPack) {
        item.toByteArray().copyInto(raw, offset)
    }

    private fun samplePack(index: Int) = NativeItemPack(
        seed = index.toLong() * 17,
        createInfo = 0x1234,
        index = index,
        idFlags = 1,
        durability = 31,
        maxDurability = 60,
        charges = 2,
        maxCharges = 4,
        value = 0xABCD,
        buff = 0xDEADBEEFL
    )
}
