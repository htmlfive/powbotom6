package org.powbot.om6.stalls

/**
 * Central location for all constants used throughout the stall thieving script.
 * This improves maintainability and makes it easier to adjust timing and thresholds.
 */
object Constants {

    // Camera settings
    object Camera {
        const val MIN_PITCH = 80
        const val MAX_PITCH = 99
    }

    // Timing constants (in milliseconds)
    object Timing {
        const val IDLE_SLEEP = 150
        const val STALL_NOT_FOUND_WAIT = 600
        const val HOPPING_FAILED_WAIT = 3000
        const val POST_STEAL_MIN_DELAY = 150
        const val POST_STEAL_MAX_DELAY = 250
        const val DROP_ITEM_MIN_DELAY = 50
        const val DROP_ITEM_MAX_DELAY = 150
    }

    // Condition wait parameters
    object WaitConditions {
        const val BANK_DEPOSIT_TIMEOUT = 150
        const val BANK_DEPOSIT_ATTEMPTS = 10
        const val DROP_ITEMS_TIMEOUT = 200
        const val DROP_ITEMS_ATTEMPTS = 15
        const val THIEVING_XP_TIMEOUT = 150
        const val THIEVING_XP_ATTEMPTS = 20
        const val WORLD_HOP_TIMEOUT = 300
        const val WORLD_HOP_ATTEMPTS = 20
    }

    // Distance thresholds
    object Distance {
        const val BANK_INTERACTION_RANGE = 5
        const val STALL_SEARCH_RANGE = 3.0
    }

    // World hopping criteria
    object WorldHopping {
        const val MIN_POPULATION = 15
        const val MAX_POPULATION = 1000
    }

    // Game actions
    object Actions {
        const val STEAL_FROM = "Steal-from"
        const val DROP = "Drop"
    }

    // Default configuration values
    object Defaults {
        const val STALL_ID = 11730
        const val STALL_NAME = "Baker's stall"
        const val STALL_INTERACTION = "Steal-from"
        const val THIEVING_TILE_X = 2669
        const val THIEVING_TILE_Y = 3310
        const val THIEVING_TILE_FLOOR = 0
        const val BANK_TILE_X = 2655
        const val BANK_TILE_Y = 3283
        const val BANK_TILE_FLOOR = 0
        const val TARGET_ITEMS = "Cake"
        const val DROP_ITEMS = "Chocolate slice, Bread"
        const val ENABLE_HOPPING = true
        const val DROP_1_MODE = true

        const val DEFAULT_STALL_CONFIG = "[{\"id\":$STALL_ID, \"name\":\"$STALL_NAME\", \"interaction\":\"$STALL_INTERACTION\", \"tile\":{\"floor\":$THIEVING_TILE_FLOOR, \"x\":2667, \"y\":3310}}]"
        const val DEFAULT_THIEVING_TILE = "{\"x\": $THIEVING_TILE_X, \"y\":$THIEVING_TILE_Y, \"floor\": $THIEVING_TILE_FLOOR}"
        const val DEFAULT_BANK_TILE = "{\"x\": $BANK_TILE_X, \"y\":$BANK_TILE_Y, \"floor\": $BANK_TILE_FLOOR}"
    }

    // Script metadata
    object Script {
        const val NAME = "0m6 Stalls"
        const val DESCRIPTION = "Steals from stalls."
        const val VERSION = "2.2.1"
        const val AUTHOR = "0m6"
    }

    // Paint configuration
    object Paint {
        const val X_POSITION = 40
        const val Y_POSITION = 80
        const val TASK_LABEL = "Current Task:"
    }

    // Configuration keys
    object ConfigKeys {
        const val STALL_TARGET = "Stall Target"
        const val ENABLE_HOPPING = "Enable Hopping"
        const val DROP_1_MODE = "Steal 1 Drop 1 Mode"
        const val TARGET_ITEMS = "Target Item Names"
        const val DROP_ITEMS = "Items to DROP"
        const val THIEVING_TILE = "Thieving Tile"
        const val BANK_TILE = "Bank Tile"
    }

    // Configuration descriptions
    object ConfigDescriptions {
        const val STALL_TARGET = "Click 'Examine' or 'Steal-from' on the stall you want to thieve from."
        const val ENABLE_HOPPING = "If enabled, the script will hop worlds if another player is on your thieving tile."
        const val DROP_1_MODE = "If enabled, the script will immediately drop all junk items after successfully stealing one item."
        const val TARGET_ITEMS = "Comma-separated list of item names to BANK when inventory is full."
        const val DROP_ITEMS = "Comma-separated list of item names to **ALWAYS DROP**."
        const val THIEVING_TILE = "Click the tile you want to stand on while thieving."
        const val BANK_TILE = "Click the tile you want to stand on when banking."
    }

    // Task names
    object TaskNames {
        const val STARTING = "Starting..."
        const val IDLE = "Idle"
        const val BANKING = "Banking"
        const val DROPPING = "Dropping"
        const val HOPPING = "Hopping"
        const val HANDLING_PITCH = "Handling Pitch"
        const val THIEVING = "Thieving"
        const val WALKING_TO_BANK = "Walking to Bank"
        const val WALKING_TO_STALL = "Walking to Stall"
    }
}