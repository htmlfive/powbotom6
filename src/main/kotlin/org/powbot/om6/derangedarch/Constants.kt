package org.powbot.om6.derangedarch

import org.powbot.api.Area
import org.powbot.api.Tile
import org.powbot.api.rt4.Prayer

object Constants {
    // --- NPC & Boss ---
    const val ARCHAEOLOGIST_ID = 7806
    const val BOSS_NAME = "Deranged archaeologist"

    // --- Locations ---
    val BOSS_TRIGGER_TILE = Tile(3683, 3707, 0)
    val FEROX_BANK_AREA = Area(Tile(3123, 3623, 0), Tile(3143, 3643, 0))
    val FEROX_POOL_AREA = Area(Tile(3128, 3637), Tile(3130, 3634))
    val FEROX_ENTRANCE_TILE = Tile(3151, 3635, 0)
    val FEROX_BANK_TILE = Tile(3135, 3631, 0)

    // --- Objects ---
    const val POOL_OF_REFRESHMENT_ID = 39651
    const val MAGIC_MUSHTREE_ID = 30920
    const val SECOND_MUSHTREE_ID = 30924
    const val TRUNK_NAME = "Decaying trunk"
    const val THICK_VINE_NAME = "Thick vine"

    // --- Prayer ---
    val REQUIRED_PRAYER = Prayer.Effect.PROTECT_FROM_MISSILES
    const val LOW_PRAYER_THRESHOLD = 30
    const val CRITICAL_PRAYER_THRESHOLD = 10

    // --- Special Attack ---
    const val SPECIAL_ATTACK_PROJECTILE = 1260
    const val SPECIAL_ATTACK_TEXT = "Learn to Read!"

    // --- Widgets ---
    const val PENDANT_WIDGET_ID = 219
    const val PENDANT_FOSSIL_ISLAND_COMPONENT = 1
    const val PENDANT_FOSSIL_ISLAND_OPTION = 2

    const val MUSHTREE_INTERFACE_ID = 608
    const val MUSHTREE_SWAMP_OPTION_COMPONENT = 12

    // --- Travel Tiles ---
    val FIRST_MUSHTREE_TILE = Tile(3764, 3879, 1)
    val VINE_OBJECT_TILE = Tile(3680, 3743, 0)
    val POST_VINE_STEP_TILE = Tile(3680, 3725, 0)
    val TRUNK_SAFE_TILE = Tile(3683, 3717, 0)
    val REPOSITION_TILE = Tile(3688, 3705, 0)

    // --- Dodge Tiles ---
    val DODGE_TILES = listOf(
        Tile(3683, 3703, 0),
        Tile(3687, 3706, 0),
        Tile(3683, 3710, 0),
        Tile(3678, 3706, 0)
    )

    // --- Distances ---
    const val FIGHT_AREA_DISTANCE = 8
    const val FIGHT_AREA_EXTENDED = 9
    const val BOSS_CLOSE_DISTANCE = 2
    const val PROJECTILE_DANGER_DISTANCE = 1.0
    const val MIN_DODGE_DISTANCE = 5.0
    const val MIN_DODGE_ANGLE_DIFFERENCE = 25.0
    const val MAX_DODGE_ATTEMPTS = 10

    // --- Camera ---
    const val MIN_PITCH = 85
    const val MAX_PITCH = 99
    const val TARGET_PITCH = 92

    // --- Item Ranges ---
    val DUELING_RING_ID_RANGE = 2552..2566
    val KEEP_DUELING_RING_IDS = (2552..2564 step 2).toList()
    val DIGSITE_PENDANT_ID_RANGE = 11190..11194
    val KEEP_DIGSITE_PENDANT_IDS = (11191..11194).toList()

    // --- Item Names ---
    const val DUELING_RING_NAME_CONTAINS = "Ring of dueling"
    const val DIGSITE_PENDANT_NAME_CONTAINS = "Digsite pendant"
    const val PRAYER_POTION_NAME_CONTAINS = "Prayer potion"
    const val AXE_NAME_SUFFIX = "axe"
    const val RUNE_NAME_SUFFIX = " rune"
    const val BONES_NAME = "Bones"

    // --- Specific Item IDs ---
    const val PRAYER_POTION_4_ID = 2434
    const val EMPTY_VIAL_ID = 229

    // --- Antipoison Names ---
    val ANTIPOISON_NAMES = setOf(
        "Antipoison(1)", "Antipoison(2)", "Antipoison(3)", "Antipoison(4)",
        "Superantipoison(1)", "Superantipoison(2)", "Superantipoison(3)", "Superantipoison(4)",
        "Antidote+ (1)", "Antidote+ (2)", "Antidote+ (3)", "Antidote+ (4)",
        "Antidote++ (1)", "Antidote++ (2)", "Antidote++ (3)", "Antidote++ (4)",
        "Sanfew serum (1)", "Sanfew serum (2)", "Sanfew serum (3)", "Sanfew serum (4)"
    )

    // --- Actions ---
    const val CLIMB_ACTION = "Climb"
    const val CHOP_ACTION = "Chop"
    const val USE_ACTION = "Use"
    const val DRINK_ACTION = "Drink"
    const val RUB_ACTION = "Rub"
    const val ATTACK_ACTION = "Attack"
    const val EAT_ACTION = "Eat"
    const val TAKE_ACTION = "Take"

    // --- Timing ---
    const val MIN_RUN_ENERGY = 40
}