package org.powbot.om6.derangedarch

import org.powbot.api.Area
import org.powbot.api.Tile

object Constants {
    // Boss
    const val ARCHAEOLOGIST_ID = 11338
    val BOSS_TRIGGER_TILE = Tile(3711, 3357, 0)

    // Areas
    val FEROX_BANK_AREA = Area(Tile(3125, 3625, 0), Tile(3145, 3635, 0))
    val FEROX_POOL_AREA = Area(Tile(3136, 3634, 0), Tile(3138, 3636, 0))

    // Pool
    const val POOL_OF_REFRESHMENT_ID = 29241

    // Special Attack
    const val SPECIAL_ATTACK_PROJECTILE = 1259
    const val SPECIAL_ATTACK_TEXT = "Rain of Knowledge!"

    // Dodge Configuration
    val DODGE_TILES = listOf(
        Tile(3709, 3357, 0),
        Tile(3710, 3357, 0),
        Tile(3711, 3357, 0),
        Tile(3712, 3357, 0),
        Tile(3713, 3357, 0),
        Tile(3709, 3358, 0),
        Tile(3710, 3358, 0),
        Tile(3711, 3358, 0),
        Tile(3712, 3358, 0),
        Tile(3713, 3358, 0),
        Tile(3709, 3359, 0),
        Tile(3710, 3359, 0),
        Tile(3711, 3359, 0),
        Tile(3712, 3359, 0),
        Tile(3713, 3359, 0)
    )
    const val MIN_DODGE_DISTANCE = 2
    const val PROJECTILE_DANGER_DISTANCE = 1
    const val MIN_DODGE_ANGLE_DIFFERENCE = 30.0
    const val MAX_DODGE_ATTEMPTS = 3

    // Repositioning
    val REPOSITION_TILE = Tile(3711, 3357, 0)

    // Camera
    const val MIN_PITCH = 50
    const val MAX_PITCH = 90
    const val TARGET_PITCH = 75

    // Travel - Fossil Island
    const val MAGIC_MUSHTREE_ID = 30922
    const val SECOND_MUSHTREE_ID = 30920
    val FIRST_MUSHTREE_TILE = Tile(3676, 3871, 0)
    val VINE_OBJECT_TILE = Tile(3732, 3810, 0)
    val POST_VINE_STEP_TILE = Tile(3729, 3810, 0)
    val TRUNK_SAFE_TILE = Tile(3719, 3803, 0)
    const val TRUNK_NAME = "Rope"
    const val CLIMB_ACTION = "Climb-down"

    // Widgets
    const val PENDANT_WIDGET_ID = 187
    const val PENDANT_FOSSIL_ISLAND_COMPONENT = 3
    const val PENDANT_FOSSIL_ISLAND_OPTION_INDEX = 4
    const val MUSHTREE_INTERFACE_ID = 608
    const val MUSHTREE_SWAMP_OPTION_COMPONENT = 4
    const val DUELING_RING_WIDGET_ID = 219
    const val OPTIONS_CONTAINER_COMPONENT = 1
    const val FEROX_ENCLAVE_OPTION_INDEX = 3

    // Bank teleport tiles
    val FEROX_ENTRANCE_TILE = Tile(3151, 3635, 0)
    val FEROX_BANK_TILE = Tile(3135, 3631, 0)

    // Item preservation
    const val PRAYER_POTION_4_ID = 2434
    val KEEP_DUELING_RING_IDS = (2552..2566).toList()
    val KEEP_DIGSITE_PENDANT_IDS = (11190..11194).toList()
    const val DUELING_RING_NAME_CONTAINS = "Ring of dueling"
    const val DIGSITE_PENDANT_NAME_CONTAINS = "Digsite pendant"
    const val PRAYER_POTION_NAME_CONTAINS = "Prayer potion"

    // Antipoison
    val ANTIPOISON_NAMES = listOf(
        "Antipoison(4)", "Antipoison(3)", "Antipoison(2)", "Antipoison(1)",
        "Superantipoison(4)", "Superantipoison(3)", "Superantipoison(2)", "Superantipoison(1)",
        "Antidote++(4)", "Antidote++(3)", "Antidote++(2)", "Antidote++(1)",
        "Antidote+(4)", "Antidote+(3)", "Antidote+(2)", "Antidote+(1)"
    )
}