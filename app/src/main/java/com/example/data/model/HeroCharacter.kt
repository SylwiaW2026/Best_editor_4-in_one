package com.example.data.model

data class HeroCharacter(
    val name: String = "Hero",
    val heroClass: HeroClass = HeroClass.WARRIOR,
    val level: Int = 1,
    val experience: Long = 0,
    val gold: Long = 100,
    val difficulty: String = "Normal",
    val dungeonLevel: Int = 0, // 0 = Town, 1-16 Cathedral to Hell, 17-24 Hellfire

    // Game Mod & Multiplayer
    val isMultiplayer: Boolean = false,
    val multiplayerSlot: Int = 0, // 0 to 3 for single_0 / multi_0
    val gameMod: GameMod = GameMod.HELLFIRE,
    val unlockedNightmare: Boolean = true,
    val unlockedHell: Boolean = true,
    val pvpKills: Int = 0,
    val deaths: Int = 0,

    // Stats
    val strength: Int = 30,
    val baseStrength: Int = 30,
    val magic: Int = 10,
    val baseMagic: Int = 10,
    val dexterity: Int = 20,
    val baseDexterity: Int = 20,
    val vitality: Int = 25,
    val baseVitality: Int = 25,
    val statPoints: Int = 0,

    // Life & Mana (Standard Diablo numbers or 64x internal)
    val life: Int = 70,
    val maxLife: Int = 70,
    val baseLife: Int = 70,
    val mana: Int = 10,
    val maxMana: Int = 10,
    val baseMana: Int = 10,
    val manaShieldActive: Boolean = false,

    // Spells: spellId -> level (0..15)
    val spellLevels: Map<String, Int> = emptyMap(),

    // Equipment: Slot -> Item
    val equipment: Map<EquipSlot, DiabloItem> = emptyMap(),

    // Backpack inventory (up to 40 slots)
    val inventory: List<DiabloItem> = emptyList(),

    // Belt / Speed Bar (8 slots)
    val belt: List<DiabloItem?> = List(8) { null },

    // Quests completed count or flags
    val questsCompleted: Int = 0
) {
    companion object {
        // Diablo 1 Level to XP table
        val LEVEL_XP_TABLE = longArrayOf(
            0L, 0L, 2000L, 4620L, 8040L, 12489L, 18258L, 25712L, 35309L, 47622L, // 1-9
            63364L, 83419L, 108879L, 141086L, 181683L, 231075L, 313656L, 424067L, 571190L, 766569L, // 10-19
            1025156L, 1366227L, 1814568L, 2401895L, 3168651L, 4166271L, 5459523L, 7130496L, 9281874L, 12042092L, // 20-29
            15571031L, 20066900L, 25774405L, 32994399L, 42095202L, 53525811L, 67831218L, 85683411L, 107909214L, 135517105L, // 30-39
            169726868L, 211996515L, 264104272L, 328221808L, 406963404L, 503417531L, 621376848L, 765419981L, 940802380L, 1154091557L, // 40-49
            1413867624L // 50
        )

        fun xpForLevel(lvl: Int): Long {
            val clamped = lvl.coerceIn(1, 50)
            return LEVEL_XP_TABLE.getOrElse(clamped) { 0L }
        }

        fun levelForXp(xp: Long): Int {
            for (i in 50 downTo 1) {
                if (xp >= LEVEL_XP_TABLE[i]) return i
            }
            return 1
        }

        fun createStarter(heroClass: HeroClass, name: String = "Hero"): HeroCharacter {
            val starterSpells = when (heroClass) {
                HeroClass.SORCERER -> mapOf("firebolt" to 1, "charged_bolt" to 1)
                HeroClass.ROGUE -> mapOf("identify" to 1)
                HeroClass.WARRIOR -> mapOf("healing" to 1)
                else -> emptyMap()
            }

            return HeroCharacter(
                name = name,
                heroClass = heroClass,
                level = 1,
                experience = 0,
                gold = 250,
                difficulty = "Normal",
                dungeonLevel = 0,
                strength = heroClass.baseStrength,
                baseStrength = heroClass.baseStrength,
                magic = heroClass.baseMagic,
                baseMagic = heroClass.baseMagic,
                dexterity = heroClass.baseDexterity,
                baseDexterity = heroClass.baseDexterity,
                vitality = heroClass.baseVitality,
                baseVitality = heroClass.baseVitality,
                statPoints = 0,
                life = heroClass.baseHp,
                maxLife = heroClass.baseHp,
                baseLife = heroClass.baseHp,
                mana = heroClass.baseMana,
                maxMana = heroClass.baseMana,
                baseMana = heroClass.baseMana,
                spellLevels = starterSpells
            )
        }

        fun createGodly(heroClass: HeroClass, name: String = "Archon"): HeroCharacter {
            val allSpellsMax = DiabloSpellsCatalog.ALL_SPELLS.associate { it.id to 15 }
            fun catalogItem(query: String): DiabloItem? =
                ItemArmory.LEGENDARY_CATALOG.firstOrNull { it.name.contains(query, ignoreCase = true) }

            val godlyItems = mutableMapOf<EquipSlot, DiabloItem>()
            when (heroClass) {
                HeroClass.WARRIOR -> {
                    catalogItem("Royal Crest")?.let { godlyItems[EquipSlot.HEAD] = it }
                    catalogItem("Godly Plate")?.let { godlyItems[EquipSlot.TORSO] = it }
                    catalogItem("Grandfather")?.let { godlyItems[EquipSlot.LEFT_HAND] = it }
                    catalogItem("Stormshield")?.let { godlyItems[EquipSlot.RIGHT_HAND] = it }
                    catalogItem("Obsidian Amulet")?.let { godlyItems[EquipSlot.NECK] = it }
                    catalogItem("Obsidian Ring")?.let { godlyItems[EquipSlot.LEFT_RING] = it }
                    catalogItem("Dragon's Ring")?.let { godlyItems[EquipSlot.RIGHT_RING] = it }
                }
                HeroClass.ROGUE -> {
                    catalogItem("Veil of Steel")?.let { godlyItems[EquipSlot.HEAD] = it }
                    catalogItem("Demonspike")?.let { godlyItems[EquipSlot.TORSO] = it }
                    catalogItem("Windforce")?.let { godlyItems[EquipSlot.LEFT_HAND] = it }
                    catalogItem("Obsidian Amulet")?.let { godlyItems[EquipSlot.NECK] = it }
                    catalogItem("Obsidian Ring")?.let { godlyItems[EquipSlot.LEFT_RING] = it }
                    catalogItem("Obsidian Ring")?.let { godlyItems[EquipSlot.RIGHT_RING] = it }
                }
                HeroClass.SORCERER -> {
                    catalogItem("Thinking Cap")?.let { godlyItems[EquipSlot.HEAD] = it }
                    catalogItem("Naj's Light Plate")?.let { godlyItems[EquipSlot.TORSO] = it }
                    catalogItem("Arch-Angel's")?.let { godlyItems[EquipSlot.LEFT_HAND] = it }
                    catalogItem("Obsidian Amulet")?.let { godlyItems[EquipSlot.NECK] = it }
                    catalogItem("Dragon's Ring")?.let { godlyItems[EquipSlot.LEFT_RING] = it }
                    catalogItem("Obsidian Ring")?.let { godlyItems[EquipSlot.RIGHT_RING] = it }
                }
                else -> {}
            }

            return HeroCharacter(
                name = name,
                heroClass = heroClass,
                level = 50,
                experience = xpForLevel(50),
                gold = 999999,
                difficulty = "Hell",
                dungeonLevel = 16,
                strength = heroClass.maxStrength,
                baseStrength = heroClass.maxStrength,
                magic = heroClass.maxMagic,
                baseMagic = heroClass.maxMagic,
                dexterity = heroClass.maxDexterity,
                baseDexterity = heroClass.maxDexterity,
                vitality = heroClass.maxVitality,
                baseVitality = heroClass.maxVitality,
                statPoints = 50,
                life = 650,
                maxLife = 650,
                baseLife = 450,
                mana = 700,
                maxMana = 700,
                baseMana = 500,
                manaShieldActive = true,
                spellLevels = allSpellsMax,
                equipment = godlyItems,
                inventory = listOfNotNull(
                    catalogItem("King's") ?: DiabloItem(name = "King's", baseType = "Unknown", defaultSlot = EquipSlot.HEAD),
                    catalogItem("Naj's Puzzler") ?: DiabloItem(name = "Naj's Puzzler", baseType = "Unknown", defaultSlot = EquipSlot.LEFT_RING),
                    catalogItem("Full Rejuvenation") ?: DiabloItem(name = "Full Rejuvenation", baseType = "Unknown", defaultSlot = EquipSlot.NECK)
                ),
                questsCompleted = 16
            )
        }
    }
}
