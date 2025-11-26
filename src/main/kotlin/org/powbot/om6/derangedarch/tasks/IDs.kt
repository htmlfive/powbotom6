package org.powbot.om6.derangedarch

import org.powbot.api.rt4.Prayer

/**
 * Centralized container for all non-configurable IDs, Names, Widgets,
 * and other static constants used across the script's tasks.
 */
object IDs {
    // --- NPC IDs ---
    const val DERANGED_ARCHAEOLOGIST_ID = 6618 // Boss ID

    // --- Object IDs ---
    const val POOL_OF_REFRESHMENT_ID = 29241 // Ferox Enclave Pool
    const val MAGIC_MUSHTREE_ID = 30920 // First Mushtree (House on the Hill)
    const val SECOND_MUSHTREE_ID = 30924 // Second Mushtree (Swamp crossing)
    const val ROCK_FALL_ID = 31085 // Obstacle object on the way to the boss

    // --- Projectile IDs ---
    const val SPECIAL_ATTACK_PROJECTILE = 1440 // ID for the special attack

    // --- Widget/Interface/Component IDs ---
    const val DUELING_RING_WIDGET_ID = 219 // Also used for Digsite Pendant
    const val WIDGET_OPTIONS_CONTAINER = 1 // Component container for options list on teleport widgets
    const val FEROX_ENCLAVE_OPTION_INDEX = 3 // Index for 'Ferox Enclave' on Dueling Ring widget
    const val PENDANT_FOSSIL_ISLAND_OPTION_INDEX = 2 // Index for 'Fossil Island' on Digsite Pendant widget
    const val MUSHTREE_INTERFACE_ID = 608
    const val MUSHTREE_SWAMP_OPTION_COMPONENT = 12 // Component for 'Fossil Island Swamp'

    // --- Item Names/Name Contains for Lookups ---
    const val RING_OF_DUELING_NAME = "Dueling ring" // Used for Inventory.stream().nameContains
    const val DIGSITE_PENDANT_NAME_CONTAINS = "Digsite pendant" // Used for Inventory.stream().nameContains
    const val BONES_NAME = "Bones" // Item name to ignore for looting
    const val PRAYER_POTION_NAME_CONTAINS = "Prayer potion" // Used for resupply check nameContains

    // --- NPC/Object Names ---
    const val DERANGED_ARCHAEOLOGIST_NAME = "Deranged Archaeologist"
    const val DECAYING_TRUNK_NAME = "Decaying trunk" // Object name for deadfall trap

    // --- Antipoison Potions (Names) ---
    val ANTIPOISON_NAMES = setOf(
        "Antipoison(1)", "Antipoison(2)", "Antipoison(3)", "Antipoison(4)",
        "Superantipoison(1)", "Superantipoison(2)", "Superantipoison(3)", "Superantipoison(4)",
        "Antidote+ (1)", "Antidote+ (2)", "Antidote+ (3)", "Antidote+ (4)",
        "Antidote++ (1)", "Antidote++ (2)", "Antidote++ (3)", "Antidote++ (4)",
        "Sanfew serum (1)", "Sanfew serum (2)", "Sanfew serum (3)", "Sanfew serum (4)"
    )
}