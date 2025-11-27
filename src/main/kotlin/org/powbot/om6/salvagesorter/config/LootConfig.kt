package org.powbot.om6.salvagesorter.config

object LootConfig {
    val DROP_LIST = arrayOf(
        //Fremennik
        "Raw lobster", "Raw tuna", "Raw monkfish", "Raw salmon",
        "Mithril ore", "Arctic pine logs", "Ensouled troll head", "Mahogany plank",
        //Opulent
        "Grimy avantoe", "Grimy ranarr weed", "Grimy snapdragon", "Grimy torstol",
        "Grimy lantadyme", "Grimy dwarf weed", "Grimy kwuarm", "Grimy cadantine",
        "Potato cactus seed", "Poison ivy seed", "Cactus seed",
        "Irit seed", "Belladonna seed", "Lantadyme seed",
        "Fish offcuts",
        "Uncut sapphire", "Uncut emerald", "Uncut ruby", "Nature talisman", "Uncut diamond",
        "Grey wolf fur", "Jug of wine", "Silver ore", "Spice", "Tiara",
        "Uncut jade", "Uncut opal", "Uncut red topaz", "Silver bar", "Silk"
    )

    val ALCH_LIST = arrayOf(
        //Fremennik
        "Fremennik helm", "Berserker helm", "Archer helm", "Farseer helm", "Warrior helm",
        // Opulent
        "Rune javelin", "Rune spear", "Dragon spear", "Shield left half","Ironwood repair kit"
    )

    val DISCARD_OR_ALCH_LIST = DROP_LIST + ALCH_LIST
}