package org.powbot.om6.salvager

// This class holds the configuration for which items to drop and which to high alch.
object LootConfig {
    /**
     * Items that should be dropped to save inventory space.
     */
    val DROP_LIST = arrayOf(
        "Raw lobster",
        "Raw tuna",
        "Raw monkfish",
        "Raw salmon",
        "Mithril ore",
        "Arctic pine logs",
        "Ensouled troll head",
        "Mahogany plank"
    )

    /**
     * High-value items that should be kept for High Alch or Banking.
     */
    val ALCH_LIST = arrayOf(
        "Fremennik helm",
        "Berserker helm",
        "Archer helm",
        "Farseer helm",
        "Warrior helm"
    )

    /**
     * All items the script is configured to discard (either by dropping or alching).
     */
    val DISCARD_OR_ALCH_LIST = DROP_LIST + ALCH_LIST
}