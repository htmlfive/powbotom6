package org.powbot.om6.moths

import org.powbot.api.Tile

/**
 * Constants used throughout the Moonlight Moth Catcher script.
 */
object Constants {

    // ========================================
    // SCRIPT INFO
    // ========================================

    const val SCRIPT_NAME = "0m6 Moonlight Moth Catcher"
    const val SCRIPT_DESCRIPTION = "Catches Moonlight Moths with banking and world hopping support"
    const val SCRIPT_VERSION = "2.0.0"
    const val SCRIPT_AUTHOR = "0m6"

    // ========================================
    // NPC & ITEM CONSTANTS
    // ========================================

    const val MOONLIGHT_MOTH_ID = 12771
    const val MOONLIGHT_MOTH_NAME = "Moonlight moth"
    const val BUTTERFLY_JAR_NAME = "Butterfly jar"

    // ========================================
    // LOCATION CONSTANTS
    // ========================================

    /** Minimum Y-axis value - do not catch moths below this */
    const val MINIMUM_Y_AXIS = 9437

    /** Main moth catching location */
    val MOTH_LOCATION = Tile(1574, 9446)

    /** Path to traverse to moths */
    val PATH_TO_MOTHS = arrayOf(
        Tile(1574, 9446)
    )

    // ========================================
    // OBJECT CONSTANTS
    // ========================================

    const val STAIRS_NAME = "Stairs"
    const val ACTION_CLIMB_UP = "Climb-up"
    const val ACTION_CLIMB_DOWN = "Climb-down"
    const val ACTION_CATCH = "Catch"

    // ========================================
    // DISTANCE THRESHOLDS
    // ========================================

    /** Distance to consider "near" moths */
    const val NEAR_MOTHS_DISTANCE = 15

    /** Distance to trigger world hop when player nearby */
    const val PLAYER_DETECTION_DISTANCE = 10

    /** Distance to consider "near" bank */
    const val NEAR_BANK_DISTANCE = 10

    /** Distance threshold for stepping to stairs */
    const val STAIRS_STEP_DISTANCE = 3

    /** Distance threshold for close to tile */
    const val CLOSE_TO_TILE_DISTANCE = 5

    // ========================================
    // ENERGY THRESHOLDS
    // ========================================

    const val MIN_RUN_ENERGY = 20
    const val MAX_RUN_ENERGY = 36

    // ========================================
    // WORLD HOP SETTINGS
    // ========================================

    const val MIN_WORLD_POPULATION = 15

    // ========================================
    // SLEEP TIMINGS
    // ========================================

    const val CATCH_SLEEP_MIN = 350
    const val CATCH_SLEEP_MAX = 550

    const val POLL_SLEEP_MIN = 80
    const val POLL_SLEEP_MAX = 220

    const val STAIRS_SLEEP_MIN = 1200
    const val STAIRS_SLEEP_MAX = 1800

    const val BANK_WAIT_TIMEOUT = 5000

    // ========================================
    // PAINT SETTINGS
    // ========================================

    const val PAINT_X = 40
    const val PAINT_Y = 80

    // ========================================
    // CAMERA SETTINGS
    // ========================================

    const val CAMERA_PITCH = 99
}
