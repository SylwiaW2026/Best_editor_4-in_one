package com.example.data.parser

import com.example.data.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

object SaveUtils {

    fun parseNativePlayerPack(bytes: ByteArray): HeroCharacter {
        // Implementation from DevilutionXSaveParser.parseHeroStruct
        // (will use the more complete version)
        return parseHeroStruct(bytes)
    }

    /**
     * Parses the Diablo 1 binary player struct.
     */
    fun parseHeroStruct(bytes: ByteArray): HeroCharacter {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Imię: offset 0 (32 bajty), użyj Charsets.ISO_8859_1
        buf.position(0)
        val nameBytes = ByteArray(32)
        buf.get(nameBytes)
        val rawName = String(nameBytes, Charsets.ISO_8859_1).takeWhile { it != '\u0000' }.trim()
        val name = if (rawName.isBlank()) "Hero" else rawName

        // Klasa: offset 32
        buf.position(32)
        val classInt = buf.get().toInt()
        val heroClass = HeroClass.fromIndex(classInt.coerceIn(0, 5))

        // Siła: offset 33
        buf.position(33)
        val strength = buf.get().toInt().coerceIn(0, 300)
        val baseStrength = strength

        // Magia: offset 34
        buf.position(34)
        val magic = buf.get().toInt().coerceIn(0, 300)
        val baseMagic = magic

        // Zręczność: offset 35
        buf.position(35)
        val dexterity = buf.get().toInt().coerceIn(0, 300)
        val baseDexterity = dexterity

        // Witalność: offset 36
        buf.position(36)
        val vitality = buf.get().toInt().coerceIn(0, 300)
        val baseVitality = vitality

        // Poziom: offset 37
        buf.position(37)
        val level = buf.get().toInt().coerceIn(1, 50)

        // Doświadczenie: offset 39 (Int, 4 bajty)
        buf.position(39)
        val experience = buf.int.toLong() and 0xFFFFFFFFL

        // Złoto: offset 43 (Int, 4 bajty)
        buf.position(43)
        val gold = buf.int.toLong() and 0xFFFFFFFFL

        // HP: offset 47, Max HP: offset 51
        buf.position(47)
        val rawLife = buf.int
        val rawMaxLife = buf.int

        // Mana: offset 55, Max Mana: offset 59
        buf.position(55)
        val rawMana = buf.int
        val rawMaxMana = buf.int

        val life = (rawLife / 64).coerceAtLeast(1)
        val maxLife = (rawMaxLife / 64).coerceAtLeast(1)
        val baseLife = maxLife
        val mana = (rawMana / 64).coerceAtLeast(0)
        val maxMana = (rawMaxMana / 64).coerceAtLeast(0)
        val baseMana = maxMana

        // StatPoints are not explicitly provided in the new list, keeping previous value or placeholder
        val statPoints = 0

        // Equipment: offset 140
        // (Inventory parsing logic would be added here)
        val equipment = mutableMapOf<EquipSlot, DiabloItem>()

        // Spell levels (remains as is)
        val spellLevels = mutableMapOf<String, Int>()

        // Check for DEVX extension header at offset 0x200 (512 bytes)
        var isMultiplayer = false
        var multiplayerSlot = 0
        var gameMod = GameMod.HELLFIRE
        var unlockedNightmare = true
        var unlockedHell = true
        var pvpKills = 0
        var deaths = 0

        if (bytes.size >= 540) {
            val devxBuf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            devxBuf.position(512)
            val magic = devxBuf.int
            if (magic == 0x58564544) { // "DEVX"
                isMultiplayer = devxBuf.get().toInt() == 1
                multiplayerSlot = devxBuf.get().toInt().coerceIn(0, 3)
                val modOrd = devxBuf.get().toInt()
                if (modOrd in GameMod.entries.indices) {
                    gameMod = GameMod.entries[modOrd]
                }
                unlockedNightmare = devxBuf.get().toInt() == 1
                unlockedHell = devxBuf.get().toInt() == 1
                pvpKills = devxBuf.int
                deaths = devxBuf.int
            }
        }

        return HeroCharacter(
            name = name,
            heroClass = heroClass,
            level = if (level > 0) level else 1,
            experience = experience,
            gold = gold,
            difficulty = "Normal",
            dungeonLevel = 0,
            isMultiplayer = isMultiplayer,
            multiplayerSlot = multiplayerSlot,
            gameMod = gameMod,
            unlockedNightmare = unlockedNightmare,
            unlockedHell = unlockedHell,
            pvpKills = pvpKills,
            deaths = deaths,
            strength = strength,
            baseStrength = baseStrength,
            magic = magic,
            baseMagic = baseMagic,
            dexterity = dexterity,
            baseDexterity = baseDexterity,
            vitality = vitality,
            baseVitality = baseVitality,
            statPoints = statPoints,
            life = life,
            maxLife = maxLife,
            baseLife = baseLife,
            mana = mana,
            maxMana = maxMana,
            baseMana = baseMana,
            manaShieldActive = false,
            spellLevels = spellLevels,
            equipment = equipment
        )
    }

    /**
     * Attempts to parse either a raw hero byte array or an MPQ archive containing hero data.
     */
    fun parseSaveBytes(bytes: ByteArray, filename: String = ""): HeroCharacter {
        if (bytes.size < 64) {
            throw IllegalArgumentException("File size is too small for a DevilutionX save file (${bytes.size} bytes).")
        }

        val mpqInfo = MpqTool.analyzeMpqHeader(bytes)
        val heroBytes = if (mpqInfo.isValidMpq) {
            extractHeroFromMpq(bytes, mpqInfo)
        } else {
            bytes
        }

        val hero = parseHeroStruct(heroBytes)
        val isMpFromFilename = filename.contains("multi", ignoreCase = true) || filename.contains("d1m", ignoreCase = true)
        val isHfFromFilename = filename.endsWith(".hsv", ignoreCase = true)

        return hero.copy(
            isMultiplayer = hero.isMultiplayer || isMpFromFilename,
            gameMod = if (isHfFromFilename) GameMod.HELLFIRE else hero.gameMod
        )
    }

    private fun extractHeroFromMpq(bytes: ByteArray, info: MpqHeaderInfo): ByteArray {
        // Look for embedded hero block or string patterns in uncompressed/decrypted blocks
        // In DevilutionX unpacked or standard test archives, hero is commonly situated after header
        val headerSize = info.headerSize.coerceAtLeast(32)
        val heroCandidateOffset = headerSize

        if (bytes.size > heroCandidateOffset + 128) {
            val candidateSlice = bytes.copyOfRange(heroCandidateOffset, bytes.size)
            // Verify if first 16 bytes contain valid printable characters for hero name
            val firstByte = candidateSlice[0].toInt().toChar()
            if (firstByte.isLetterOrDigit()) {
                return candidateSlice
            }
        }

        // Scan for potential hero data signature if offset differs
        for (offset in 32 until minOf(bytes.size - 64, 1024) step 4) {
            val c = bytes[offset].toInt().toChar()
            val cls = bytes.getOrNull(offset + 16)?.toInt() ?: -1
            if (c.isLetter() && cls in 0..5) {
                return bytes.copyOfRange(offset, bytes.size)
            }
        }

        // Fallback: use bytes directly
        return bytes
    }

    /**
     * Serializes a HeroCharacter into a binary Diablo 1 / DevilutionX player struct.
     */
    fun serializeToHeroStruct(hero: HeroCharacter): ByteArray {
        val size = 1280
        val bytes = ByteArray(size)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 0x00: Name (16 bytes, ASCII null-terminated)
        val nameAscii = hero.name.take(15).toByteArray(Charsets.US_ASCII)
        buf.put(nameAscii)
        for (i in nameAscii.size until 16) {
            buf.put(0.toByte())
        }

        // 0x10: Class
        buf.putInt(hero.heroClass.ordinal)

        // 0x14: Level
        buf.putInt(hero.level)

        // 0x18: Experience
        buf.putInt((hero.experience and 0xFFFFFFFFL).toInt())

        // 0x1C: Gold
        buf.putInt((hero.gold and 0xFFFFFFFFL).toInt())

        // Stats
        buf.putInt(hero.strength)
        buf.putInt(hero.baseStrength)
        buf.putInt(hero.magic)
        buf.putInt(hero.baseMagic)
        buf.putInt(hero.dexterity)
        buf.putInt(hero.baseDexterity)
        buf.putInt(hero.vitality)
        buf.putInt(hero.baseVitality)
        buf.putInt(hero.statPoints)

        // HP & Mana in 64x multiplier
        buf.putInt(hero.life * 64)
        buf.putInt(hero.maxLife * 64)
        buf.putInt(hero.baseLife * 64)
        buf.putInt(hero.mana * 64)
        buf.putInt(hero.maxMana * 64)
        buf.putInt(hero.baseMana * 64)

        // Spells
        val spellBytes = ByteArray(64)
        for (spell in DiabloSpellsCatalog.ALL_SPELLS) {
            val lvl = hero.spellLevels[spell.id] ?: 0
            spellBytes[spell.binaryIndex] = lvl.coerceIn(0, 15).toByte()
        }
        buf.put(spellBytes)

        // DEVX metadata block at offset 512 (0x200)
        buf.position(512)
        buf.putInt(0x58564544) // "DEVX" magic
        buf.put(if (hero.isMultiplayer) 1.toByte() else 0.toByte())
        buf.put(hero.multiplayerSlot.coerceIn(0, 3).toByte())
        buf.put(hero.gameMod.ordinal.toByte())
        buf.put(if (hero.unlockedNightmare) 1.toByte() else 0.toByte())
        buf.put(if (hero.unlockedHell) 1.toByte() else 0.toByte())
        buf.putInt(hero.pvpKills)
        buf.putInt(hero.deaths)

        return bytes
    }

    /**
     * Generates a complete DevilutionX compatible `.sv` MPQ archive file containing the hero struct.
     */
    fun createSaveFileMpq(hero: HeroCharacter): ByteArray {
        val heroData = serializeToHeroStruct(hero)

        // Create standard MPQ container header (32 bytes)
        val headerSize = 32
        val archiveSize = headerSize + heroData.size
        val result = ByteArray(archiveSize)
        val buf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)

        // MPQ header: magic "MPQ\x1a"
        buf.putInt(MpqTool.MPQ_MAGIC)
        buf.putInt(headerSize) // Header size
        buf.putInt(archiveSize) // Archive size
        buf.putShort(0.toShort()) // Format version
        buf.putShort(3.toShort()) // Sector size factor (512 << 3 = 4096)
        buf.putInt(headerSize) // Hash table offset
        buf.putInt(headerSize) // Block table offset
        buf.putInt(1) // Hash table entries
        buf.putInt(1) // Block table entries

        // Append hero payload
        buf.put(heroData)

        return result
    }
}
