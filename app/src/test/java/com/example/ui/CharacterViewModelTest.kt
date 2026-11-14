package com.example.ui

import com.example.data.model.GameMod
import com.example.data.model.EquipSlot
import com.example.data.model.NativeItemPack
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CharacterViewModelTest {
    @Test
    fun vanillaLevelIsClampedToFifty() {
        val viewModel = CharacterViewModel()
        viewModel.setGameMod(GameMod.VANILLA_DIABLO)
        viewModel.updateLevel(60)
        assertEquals(50, viewModel.character.value.level)
    }

    @Test
    fun vkdModLevelAllowsSixtyAndClampsAboveIt() {
        val viewModel = CharacterViewModel()
        viewModel.setGameMod(GameMod.VKD_MOD)
        viewModel.updateLevel(60)
        assertEquals(60, viewModel.character.value.level)
        viewModel.updateLevel(75)
        assertEquals(60, viewModel.character.value.level)
    }

    @Test
    fun levelCannotDropBelowOne() {
        val viewModel = CharacterViewModel()
        viewModel.updateLevel(0)
        assertEquals(1, viewModel.character.value.level)
    }

    @Test
    fun nativeSaveUsesNativeEquipmentOrderAndKeepsUnknownItems() {
        val raw = ByteBuffer.allocate(1266).order(ByteOrder.LITTLE_ENDIAN)
        val unknownItem = NativeItemPack(
            seed = 0x12345678,
            createInfo = 0xABCD,
            index = 0x4321,
            idFlags = 1,
            durability = 12,
            maxDurability = 40,
            charges = 0,
            maxCharges = 0,
            value = 0,
            buff = 0x1234
        )
        for (index in 0 until 7) {
            NativeItemPack.EMPTY.toByteArray().copyInto(raw.array(), 0x7C + index * NativeItemPack.SIZE)
        }
        for (index in 0 until 40) {
            NativeItemPack.EMPTY.toByteArray().copyInto(raw.array(), 0x101 + index * NativeItemPack.SIZE)
        }
        for (index in 0 until 8) {
            NativeItemPack.EMPTY.toByteArray().copyInto(raw.array(), 0x422 + index * NativeItemPack.SIZE)
        }
        unknownItem.toByteArray().copyInto(raw.array(), 0x7C)
        raw.put(0x421, 0.toByte())

        val character = CharacterViewModel().parseNativePlayerPack(raw.array())

        assertEquals(1, character.equipment.size)
        assertEquals(unknownItem, character.equipment[EquipSlot.HEAD]?.nativePack)
        assertEquals("Native item #17185", character.equipment[EquipSlot.HEAD]?.name)
    }
}
