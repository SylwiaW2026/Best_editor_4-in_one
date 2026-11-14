package com.example.ui

import com.example.data.model.GameMod
import com.example.data.model.EquipSlot
import com.example.data.model.ItemArmory
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
    fun nativeSaveSkipsUnknownItemsAndContinuesReadingFollowingItems() {
        val raw = ByteBuffer.allocate(1266).order(ByteOrder.LITTLE_ENDIAN)
        val unknownItemOffset = 0x7C
        val knownItemOffset = unknownItemOffset + 19
        raw.putInt(unknownItemOffset, 1)
        raw.putShort(unknownItemOffset + 6, ItemArmory.FULL_ITEM_LIBRARY.size.toShort())
        raw.putInt(knownItemOffset, 1)
        raw.putShort(knownItemOffset + 6, 0)

        val character = CharacterViewModel().parseNativePlayerPack(raw.array())

        assertEquals(1, character.equipment.size)
        assertEquals(ItemArmory.FULL_ITEM_LIBRARY[0], character.equipment[EquipSlot.NECK])
    }
}
