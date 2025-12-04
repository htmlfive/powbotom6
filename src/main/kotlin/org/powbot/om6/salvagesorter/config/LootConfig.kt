package org.powbot.om6.salvagesorter.config

object LootConfig {
    val dropList = arrayOf(
        //Barracuda
        "Oak logs", "Teak logs", "Oak plank", "Raw swordfish", "Copper ore", "Teak plank", "Raw shark", "Lead ore", "Teak repair kit", "Steel nails", "Rum", "Rope", "Banana",
        //Large Salvage
        "Oyster pearl", "Steel nails", "Casket", "Oak plank", "Giant seaweed",
        //Plundered
        "Gold ring", "Mithril scimitar", "Rum", "Sapphire ring", "Emerald bracelet", "Oyster pearls", "Casket", "Emerald ring", "Mithril cannonball", "Adamant cannonball",
         //Fremennik
        "Raw lobster", "Raw tuna", "Raw monkfish", "Raw salmon", "Mithril ore", "Arctic pine logs", "Ensouled troll head", "Mahogany plank",
        //Opulent
        "Grimy avantoe", "Grimy ranarr weed", "Grimy snapdragon", "Grimy torstol", "Grimy lantadyme", "Grimy dwarf weed", "Grimy kwuarm", "Grimy cadantine", "Potato cactus seed", "Poison ivy seed", "Cactus seed", "Irit seed", "Belladonna seed", "Lantadyme seed", "Fish offcuts", "Uncut sapphire", "Uncut emerald", "Uncut ruby", "Nature talisman", "Uncut diamond", "Grey wolf fur", "Jug of wine", "Silver ore", "Spice", "Tiara", "Uncut jade", "Uncut opal", "Uncut red topaz", "Silver bar", "Silk"
    )

    val alchList = arrayOf(
        //Large salvage
        "Diamond ring","Sapphire ring", "Emerald ring", "Gold ring", "Teak logs","Oyster pearls",
        //Plundered
        "Diamond ring", "Diamond bracelet","Mahogany repair kit","Ruby bracelet","Rune scimitar",
        //Fremennik
        "Fremennik helm", "Berserker helm", "Archer helm", "Farseer helm", "Warrior helm",
        // Opulent
        "Rune javelin", "Rune spear", "Dragon spear", "Shield left half","Ironwood repair kit"
    )

    val discardOrAlchList = dropList + alchList
    fun getDropListAsString(): String = dropList.joinToString(", ")
    fun getAlchListAsString(): String = alchList.joinToString(", ")
    fun getDiscardOrAlchListAsString(): String = discardOrAlchList.joinToString(", ")
}