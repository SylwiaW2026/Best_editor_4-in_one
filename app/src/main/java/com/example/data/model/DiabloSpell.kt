package com.example.data.model

enum class SpellSchool(val displayName: String) {
    FIRE("Fire Magic"),
    LIGHTNING("Lightning Magic"),
    ARCANE("Arcane & Utility"),
    FORBIDDEN("Forbidden Arts"),
    HELLFIRE("Hellfire Spells")
}

data class DiabloSpell(
    val id: String,
    val binaryIndex: Int,
    val name: String,
    val school: SpellSchool,
    val minLevel: Int = 1,
    val maxLevel: Int = 15,
    val manaCost: String,
    val description: String
)

object DiabloSpellsCatalog {
    val ALL_SPELLS = listOf(
        // Fire
        DiabloSpell("firebolt", 1, "Firebolt", SpellSchool.FIRE, 1, 15, "6", "Fires a concentrated bolt of flame at target."),
        DiabloSpell("fireball", 12, "Fireball", SpellSchool.FIRE, 1, 15, "16", "Explodes into a fiery blast dealing heavy splash damage."),
        DiabloSpell("firewall", 6, "Fire Wall", SpellSchool.FIRE, 1, 15, "28", "Creates an incinerating barrier that consumes all who pass."),
        DiabloSpell("inferno", 20, "Inferno", SpellSchool.FIRE, 1, 15, "19", "Streams continuous gouts of devastating flame."),
        DiabloSpell("flame_wave", 15, "Flame Wave", SpellSchool.FIRE, 1, 15, "35", "Sends a sweeping wall of fire across the battlefield."),
        DiabloSpell("guardian", 13, "Guardian", SpellSchool.FIRE, 1, 15, "50", "Summons a three-headed dragon from the nether realms."),
        DiabloSpell("apocalypse", 23, "Apocalypse", SpellSchool.FIRE, 1, 15, "60", "Calls celestial fire raining destruction upon all enemies."),

        // Lightning
        DiabloSpell("charged_bolt", 29, "Charged Bolt", SpellSchool.LIGHTNING, 1, 15, "6", "Releases erratically dancing electric sparks."),
        DiabloSpell("lightning", 3, "Lightning", SpellSchool.LIGHTNING, 1, 15, "19", "Strikes enemies with a straight discharge of high-voltage lightning."),
        DiabloSpell("chain_lightning", 14, "Chain Lightning", SpellSchool.LIGHTNING, 1, 15, "30", "Arcs multiple lethal electrical branches jumping between foes."),
        DiabloSpell("nova", 18, "Nova", SpellSchool.LIGHTNING, 1, 15, "60", "Expands a deadly 360-degree shockwave ring of electrical destruction."),
        DiabloSpell("flash", 4, "Flash", SpellSchool.LIGHTNING, 1, 15, "30", "Emits an instantaneous close-quarters burst of blinding shock."),

        // Arcane & Utility
        DiabloSpell("healing", 2, "Healing", SpellSchool.ARCANE, 1, 15, "8", "Channels divine energy to restore the caster's lost hit points."),
        DiabloSpell("heal_other", 33, "Heal Other", SpellSchool.ARCANE, 1, 15, "8", "Directs restorative holy energy to mend wounded companions."),
        DiabloSpell("town_portal", 7, "Town Portal", SpellSchool.ARCANE, 1, 15, "35", "Opens a mystical rift directly back to the town of Tristram."),
        DiabloSpell("teleport", 22, "Teleport", SpellSchool.ARCANE, 1, 15, "35", "Bends space to instantaneously relocate to target destination."),
        DiabloSpell("mana_shield", 11, "Mana Shield", SpellSchool.ARCANE, 1, 15, "33", "Envelops caster in an arcane aegis, redirecting physical damage to mana."),
        DiabloSpell("phasing", 10, "Phasing", SpellSchool.ARCANE, 1, 15, "25", "Temporarily blinks through the veil to an unpredictable nearby location."),
        DiabloSpell("stone_curse", 8, "Stone Curse", SpellSchool.ARCANE, 1, 15, "40", "Transmutes demonic flesh into motionless, brittle solid stone."),
        DiabloSpell("identify", 5, "Identify", SpellSchool.ARCANE, 1, 15, "1", "Deciphers ancient runic enchantments on mysterious magical items."),
        DiabloSpell("infravision", 9, "Infravision", SpellSchool.ARCANE, 1, 15, "41", "Allows supernatural perception through dark dungeon corridors."),

        // Forbidden
        DiabloSpell("holy_bolt", 30, "Holy Bolt", SpellSchool.FORBIDDEN, 1, 15, "7", "Dispatches searing sanctified bolts that obliterate undead fiends."),
        DiabloSpell("blood_star", 37, "Blood Star", SpellSchool.FORBIDDEN, 1, 15, "14", "Launches orbs of distilled vitality that penetrate demonic armor."),
        DiabloSpell("bone_spirit", 35, "Bone Spirit", SpellSchool.FORBIDDEN, 1, 15, "24", "Unleashes a hungry vengeful spectre that drains 1/3 of enemy life."),
        DiabloSpell("golem", 38, "Golem", SpellSchool.FORBIDDEN, 1, 15, "100", "Forges an enduring animated clay monstrosity to crush adversaries."),

        // Hellfire
        DiabloSpell("ring_of_fire", 46, "Ring of Fire", SpellSchool.HELLFIRE, 1, 15, "40", "Erects a protective concentric ring of intense demonic flame."),
        DiabloSpell("lightning_wall", 42, "Lightning Wall", SpellSchool.HELLFIRE, 1, 15, "45", "Forms a shimmering barrier of continuous high-voltage crackle."),
        DiabloSpell("warp", 43, "Warp", SpellSchool.HELLFIRE, 1, 15, "25", "Warps through the ethereal labyrinth to a random nearby room."),
        DiabloSpell("reflect", 44, "Reflect", SpellSchool.HELLFIRE, 1, 15, "45", "Rebounds incoming ranged enemy missiles back toward attackers."),
        DiabloSpell("berserk", 45, "Berserk", SpellSchool.HELLFIRE, 1, 15, "50", "Drives lesser monsters into blind homicidal fury against each other."),
        DiabloSpell("search", 47, "Search", SpellSchool.HELLFIRE, 1, 15, "5", "Illuminates hidden treasures and items dropped on dungeon floors."),
        DiabloSpell("jester", 52, "Jester", SpellSchool.HELLFIRE, 1, 15, "30", "Casts a chaotic random spell effect with unpredictable consequences.")
    )
}
