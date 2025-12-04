package org.powbot.om6.salvagesorter.config

object LootConfig {

    // Small Salvage
    private val smallDropList = arrayOf<String>()
    private val smallAlchList = arrayOf<String>()
    const val SMALL_DROP_LIST_STRING = ""
    const val SMALL_ALCH_LIST_STRING = ""

    // Fishy Salvage
    private val fishyDropList = arrayOf<String>()
    private val fishyAlchList = arrayOf<String>()
    const val FISHY_DROP_LIST_STRING = ""
    const val FISHY_ALCH_LIST_STRING = ""

    // Barracuda Salvage
    private val barracudaDropList = arrayOf(
        "Oak logs", "Teak logs", "Oak plank", "Raw swordfish", "Copper ore", 
        "Teak plank", "Raw shark", "Lead ore", "Teak repair kit", "Steel nails", 
        "Rum", "Rope", "Banana"
    )
    private val barracudaAlchList = arrayOf<String>()
    const val BARRACUDA_DROP_LIST_STRING = "Oak logs, Teak logs, Oak plank, Raw swordfish, Copper ore, Teak plank, Raw shark, Lead ore, Teak repair kit, Steel nails, Rum, Rope, Banana"
    const val BARRACUDA_ALCH_LIST_STRING = ""

    // Large Salvage
    private val largeDropList = arrayOf(
        "Oyster pearl", "Steel nails", "Casket", "Oak plank", "Giant seaweed"
    )
    private val largeAlchList = arrayOf(
        "Diamond ring", "Sapphire ring", "Emerald ring", "Gold ring", "Teak logs", "Oyster pearls"
    )
    const val LARGE_DROP_LIST_STRING = "Oyster pearl, Steel nails, Casket, Oak plank, Giant seaweed"
    const val LARGE_ALCH_LIST_STRING = "Diamond ring, Sapphire ring, Emerald ring, Gold ring, Teak logs, Oyster pearls"

    // Plundered Salvage
    private val plunderedDropList = arrayOf(
        "Gold ring", "Mithril scimitar", "Rum", "Sapphire ring", "Emerald bracelet", 
        "Oyster pearls", "Casket", "Emerald ring", "Mithril cannonball", "Adamant cannonball"
    )
    private val plunderedAlchList = arrayOf(
        "Diamond ring", "Diamond bracelet", "Mahogany repair kit", "Ruby bracelet", "Rune scimitar"
    )
    const val PLUNDERED_DROP_LIST_STRING = "Gold ring, Mithril scimitar, Rum, Sapphire ring, Emerald bracelet, Oyster pearls, Casket, Emerald ring, Mithril cannonball, Adamant cannonball"
    const val PLUNDERED_ALCH_LIST_STRING = "Diamond ring, Diamond bracelet, Mahogany repair kit, Ruby bracelet, Rune scimitar"

    // Martial Salvage
    private val martialDropList = arrayOf<String>()
    private val martialAlchList = arrayOf<String>()
    const val MARTIAL_DROP_LIST_STRING = ""
    const val MARTIAL_ALCH_LIST_STRING = ""

    // Fremennik Salvage
    private val fremennikDropList = arrayOf(
        "Raw lobster", "Raw tuna", "Raw monkfish", "Raw salmon", "Mithril ore", 
        "Arctic pine logs", "Ensouled troll head", "Mahogany plank"
    )
    private val fremennikAlchList = arrayOf(
        "Fremennik helm", "Berserker helm", "Archer helm", "Farseer helm", "Warrior helm"
    )
    const val FREMENNIK_DROP_LIST_STRING = "Raw lobster, Raw tuna, Raw monkfish, Raw salmon, Mithril ore, Arctic pine logs, Ensouled troll head, Mahogany plank"
    const val FREMENNIK_ALCH_LIST_STRING = "Fremennik helm, Berserker helm, Archer helm, Farseer helm, Warrior helm"

    // Opulent Salvage
    private val opulentDropList = arrayOf(
        "Grimy avantoe", "Grimy ranarr weed", "Grimy snapdragon", "Grimy torstol", 
        "Grimy lantadyme", "Grimy dwarf weed", "Grimy kwuarm", "Grimy cadantine", 
        "Potato cactus seed", "Poison ivy seed", "Cactus seed", "Irit seed", 
        "Belladonna seed", "Lantadyme seed", "Fish offcuts", "Uncut sapphire", 
        "Uncut emerald", "Uncut ruby", "Nature talisman", "Uncut diamond", 
        "Grey wolf fur", "Jug of wine", "Silver ore", "Spice", "Tiara", 
        "Uncut jade", "Uncut opal", "Uncut red topaz", "Silver bar", "Silk"
    )
    private val opulentAlchList = arrayOf(
        "Rune javelin", "Rune spear", "Dragon spear", "Shield left half", "Ironwood repair kit"
    )
    const val OPULENT_DROP_LIST_STRING = "Grimy avantoe, Grimy ranarr weed, Grimy snapdragon, Grimy torstol, Grimy lantadyme, Grimy dwarf weed, Grimy kwuarm, Grimy cadantine, Potato cactus seed, Poison ivy seed, Cactus seed, Irit seed, Belladonna seed, Lantadyme seed, Fish offcuts, Uncut sapphire, Uncut emerald, Uncut ruby, Nature talisman, Uncut diamond, Grey wolf fur, Jug of wine, Silver ore, Spice, Tiara, Uncut jade, Uncut opal, Uncut red topaz, Silver bar, Silk"
    const val OPULENT_ALCH_LIST_STRING = "Rune javelin, Rune spear, Dragon spear, Shield left half, Ironwood repair kit"

    // Map salvage names to their lists
    private val salvageDropLists = mapOf(
        "Small salvage" to smallDropList,
        "Fishy salvage" to fishyDropList,
        "Barracuda salvage" to barracudaDropList,
        "Large salvage" to largeDropList,
        "Plundered salvage" to plunderedDropList,
        "Martial salvage" to martialDropList,
        "Fremennik salvage" to fremennikDropList,
        "Opulent salvage" to opulentDropList
    )

    private val salvageAlchLists = mapOf(
        "Small salvage" to smallAlchList,
        "Fishy salvage" to fishyAlchList,
        "Barracuda salvage" to barracudaAlchList,
        "Large salvage" to largeAlchList,
        "Plundered salvage" to plunderedAlchList,
        "Martial salvage" to martialAlchList,
        "Fremennik salvage" to fremennikAlchList,
        "Opulent salvage" to opulentAlchList
    )

    /**
     * Get drop list for specific salvage type
     */
    fun getDropList(salvageName: String): Array<String> {
        return salvageDropLists[salvageName] ?: emptyArray()
    }

    /**
     * Get alch list for specific salvage type
     */
    fun getAlchList(salvageName: String): Array<String> {
        return salvageAlchLists[salvageName] ?: emptyArray()
    }

    /**
     * Get combined drop and alch list for specific salvage type
     */
    fun getDiscardOrAlchList(salvageName: String): Array<String> {
        return getDropList(salvageName) + getAlchList(salvageName)
    }

    /**
     * Get formatted drop list as comma-separated string for GUI
     */
    fun getDropListString(salvageName: String): String {
        return getDropList(salvageName).joinToString(", ")
    }

    /**
     * Get formatted alch list as comma-separated string for GUI
     */
    fun getAlchListString(salvageName: String): String {
        return getAlchList(salvageName).joinToString(", ")
    }

    // Legacy properties for backward compatibility (deprecated)
    @Deprecated("Use getDropList(salvageName) instead")
    val dropList = barracudaDropList

    @Deprecated("Use getAlchList(salvageName) instead")
    val alchList = barracudaAlchList

    @Deprecated("Use getDiscardOrAlchList(salvageName) instead")
    val discardOrAlchList = dropList + alchList
}
