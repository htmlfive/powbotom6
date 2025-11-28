package org.powbot.om6.derangedarch

import org.powbot.api.Area
import org.powbot.api.Tile

object Constants {
    // Boss
    const val ARCHAEOLOGIST_ID = 7806
    val BOSS_TRIGGER_TILE = Tile(3683, 3705, 0)
    val DISTANCETOBOSS = 10

    // Areas
    val FEROX_BANK_AREA = Area(Tile(3125, 3625, 0), Tile(3145, 3635, 0))
    val FEROX_POOL_AREA = Area(Tile(3136, 3634, 0), Tile(3138, 3636, 0))
    val FEROX_TELEPORT_TILE = Tile(3150, 3637, 0)
    // Pool
    const val POOL_OF_REFRESHMENT_ID = 39651

    // Special Attack
    const val SPECIAL_ATTACK_PROJECTILE = 1440
    const val SPECIAL_ATTACK_TEXT = "Learn to Read!"

    // Dodge Configuration
    val DODGE_TILES = listOf(
        Tile(3683, 3703, 0),
        Tile(3687, 3706, 0),
        Tile(3683, 3710, 0),
        Tile(3678, 3706, 0)
    )


    const val PROJECTILE_DANGER_DISTANCE = 1.0
    const val MAX_DODGE_ATTEMPTS = 10
    const val MIN_DODGE_ANGLE_DIFFERENCE = 25.0 // Min angle diff to "not walk through boss"
    const val MIN_DODGE_DISTANCE = 5.0 // --- NEW: Minimum distance to dodge ---

    // Repositioning
    val REPOSITION_TILE = Tile(3711, 3357, 0)

    // Camera
    const val MIN_PITCH = 50
    const val MAX_PITCH = 90
    const val TARGET_PITCH = 75

    // Travel - Fossil Island
    const val MAGIC_MUSHTREE_ID = 30920
    const val SECOND_MUSHTREE_ID = 30924
    const val ROCK_FALL_ID = 31085
    val FIRST_MUSHTREE_TILE = Tile(3764, 3879, 1)
    val VINE_OBJECT_TILE = Tile(3680, 3743, 0)
    val POST_VINE_STEP_TILE = Tile(3680, 3725, 0)
    val TRUNK_SAFE_TILE = Tile(3683, 3717, 0)
    const val TRUNK_NAME = "Decaying trunk"
    const val CLIMB_ACTION = "Climb"

    // Widgets
    const val PENDANT_WIDGET_ID = 219
    const val PENDANT_FOSSIL_ISLAND_COMPONENT = 1
    const val PENDANT_FOSSIL_ISLAND_OPTION_INDEX = 2
    const val MUSHTREE_INTERFACE_ID = 608
    const val MUSHTREE_SWAMP_OPTION_COMPONENT = 12

    // Item preservation
    const val PRAYER_POTION_4_ID = 2434
    val KEEP_DUELING_RING_IDS = (2552..2566).toList()
    val KEEP_DIGSITE_PENDANT_IDS = (11190..11194).toList()
    const val DUELING_RING_NAME_CONTAINS = "Ring of dueling"
    const val DIGSITE_PENDANT_NAME_CONTAINS = "Digsite pendant"
    const val PRAYER_POTION_NAME_CONTAINS = "Prayer potion"

    // Antipoison
    val ANTIPOISON_NAMES = listOf(
        "Antipoison(1)", "Antipoison(2)", "Antipoison(3)", "Antipoison(4)",
        "Superantipoison(1)", "Superantipoison(2)", "Superantipoison(3)", "Superantipoison(4)",
        "Antidote+ (1)", "Antidote+ (2)", "Antidote+ (3)", "Antidote+ (4)",
        "Antidote++ (1)", "Antidote++ (2)", "Antidote++ (3)", "Antidote++ (4)",
        "Sanfew serum (1)", "Sanfew serum (2)", "Sanfew serum (3)", "Sanfew serum (4)"
    )

    // Boss loot item IDs for GE price caching
    val BOSS_LOOT_IDS = listOf(
        11978, // Steel ring
        2503,  // Black d'hide body
        1319,  // Rune 2h sword
        1333,  // Rune sword
        555,   // Water rune
        4698,  // Mud rune
        868,   // Rune knife
        11212, // Dragon arrow
        2,     // Cannonball
        5295,  // Ranarr seed
        5300,  // Snapdragon seed
        5304,  // Torstol seed
        5321,  // Watermelon seed
        5313,  // Willow seed
        21488, // Mahogany seed
        5316,  // Maple seed
        21486, // Teak seed
        5315,  // Yew seed
        5288,  // Papaya tree seed
        5319,  // Magic seed
        5289,  // Palm tree seed
        5317,  // Spirit seed
        22877, // Dragonfruit tree seed
        22869, // Celastrus seed
        22871, // Redwood tree seed
        217,   // Grimy dwarf weed
        239,   // White berries
        9431,  // Runite limbs
        1747,  // Black dragonhide
        444,   // Gold ore
        1617,  // Uncut diamond
        6573,  // Onyx bolt tips
        2297,  // Anchovy pizza
        143,   // Prayer potion(3)
        6705,  // Potato with cheese
        385,   // Shark
        989,   // Crystal key
        3123,  // Long bone
        11818, // Numulite
        22124, // Unidentified small fossil
        22126, // Unidentified medium fossil
        22128, // Unidentified large fossil
        22130, // Unidentified rare fossil
        23490, // Brimstone key
        19043  // Clue scroll (elite)
    )
}