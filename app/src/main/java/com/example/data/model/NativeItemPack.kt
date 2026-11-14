package com.example.data.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class NativeItemPack(
    val seed: Long,
    val createInfo: Int,
    val index: Int,
    val idFlags: Int,
    val durability: Int,
    val maxDurability: Int,
    val charges: Int,
    val maxCharges: Int,
    val value: Int,
    val buff: Long
) {
    val isEmpty: Boolean
        get() = index == EMPTY_INDEX

    fun toByteArray(): ByteArray {
        require(seed in 0..UINT32_MAX)
        require(createInfo in 0..UINT16_MAX)
        require(index in 0..UINT16_MAX)
        require(idFlags in 0..UINT8_MAX)
        require(durability in 0..UINT8_MAX)
        require(maxDurability in 0..UINT8_MAX)
        require(charges in 0..UINT8_MAX)
        require(maxCharges in 0..UINT8_MAX)
        require(value in 0..UINT16_MAX)
        require(buff in 0..UINT32_MAX)

        return ByteBuffer.allocate(SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(seed.toInt())
            putShort(createInfo.toShort())
            putShort(index.toShort())
            put(idFlags.toByte())
            put(durability.toByte())
            put(maxDurability.toByte())
            put(charges.toByte())
            put(maxCharges.toByte())
            putShort(value.toShort())
            putInt(buff.toInt())
        }.array()
    }

    companion object {
        const val SIZE = 19
        const val EMPTY_INDEX = 0xFFFF

        private const val UINT8_MAX = 0xFF
        private const val UINT16_MAX = 0xFFFF
        private const val UINT32_MAX = 0xFFFF_FFFFL

        val EMPTY = NativeItemPack(
            seed = 0,
            createInfo = 0,
            index = EMPTY_INDEX,
            idFlags = 0,
            durability = 0,
            maxDurability = 0,
            charges = 0,
            maxCharges = 0,
            value = 0,
            buff = 0
        )

        fun read(buffer: ByteBuffer, offset: Int): NativeItemPack {
            require(offset >= 0 && offset + SIZE <= buffer.limit()) {
                "ItemPack at offset $offset exceeds buffer size ${buffer.limit()}."
            }
            val source = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            return NativeItemPack(
                seed = source.getInt(offset).toLong() and UINT32_MAX,
                createInfo = source.getShort(offset + 4).toInt() and UINT16_MAX,
                index = source.getShort(offset + 6).toInt() and UINT16_MAX,
                idFlags = source.get(offset + 8).toInt() and UINT8_MAX,
                durability = source.get(offset + 9).toInt() and UINT8_MAX,
                maxDurability = source.get(offset + 10).toInt() and UINT8_MAX,
                charges = source.get(offset + 11).toInt() and UINT8_MAX,
                maxCharges = source.get(offset + 12).toInt() and UINT8_MAX,
                value = source.getShort(offset + 13).toInt() and UINT16_MAX,
                buff = source.getInt(offset + 15).toLong() and UINT32_MAX
            )
        }
    }
}

data class NativeItemState(
    val equipment: List<NativeItemPack>,
    val inventory: List<NativeItemPack>,
    val inventoryGrid: List<Int>,
    val inventoryCount: Int,
    val belt: List<NativeItemPack>,
    val isHellfire: Boolean,
    val isSpawn: Boolean
)
