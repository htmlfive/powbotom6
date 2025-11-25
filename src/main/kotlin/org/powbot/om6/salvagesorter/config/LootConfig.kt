package org.powbot.om6.salvagesorter.config

object LootConfig {
    val DROP_LIST = arrayOf(
        "Raw lobster", "Raw tuna", "Raw monkfish", "Raw salmon",
        "Mithril ore", "Arctic pine logs", "Ensouled troll head", "Mahogany plank"
    )

    val ALCH_LIST = arrayOf(
        "Fremennik helm", "Berserker helm", "Archer helm", "Farseer helm", "Warrior helm"
    )

    val DISCARD_OR_ALCH_LIST = DROP_LIST + ALCH_LIST
}