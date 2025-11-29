package org.powbot.webwalk

/**
 * Constants for the WebWalker script.
 */
object Constants {
    const val SCRIPT_NAME = "0m6 Webwalker"
    const val SCRIPT_VERSION = "3.1.0"
    const val SCRIPT_AUTHOR = "0m6"
    const val SCRIPT_DESCRIPTION = "Walks to a predefined location or custom tile (X,Y,Z)."

    // Configuration keys
    const val CONFIG_TARGET_LOCATION = "Target Location"

    // Default values
    const val DEFAULT_LOCATION = "Lumbridge Bank"

    // Walking parameters
    const val DESTINATION_DISTANCE_THRESHOLD = 5
    const val WALK_WAIT_TIMEOUT = 150
    const val WALK_WAIT_ITERATIONS = 20
    const val DESTINATION_SLEEP_TIME = 2000

    // Paint configuration
    const val PAINT_X = 10
    const val PAINT_Y = 200

    // Tile parsing
    const val EXPECTED_COORDINATE_PARTS = 3
    const val COORDINATE_DELIMITER = ","

    // Tile validation and search
    const val TILE_SEARCH_RANGE = 10

    /**
     * Predefined locations with their coordinates.
     */
    enum class Location(val displayName: String, val coordinates: String) {
        AL_KHARID_BANK("Al Kharid Bank", "3293,3174,0"),  // Fixed
        ARDOUGNE_SOUTH_BANK("Ardougne South Bank", "2655,3283,0"),  // Looks OK
        CATHERBY_BANK("Catherby Bank", "2813,3447,0"),  // Fixed
        DRAYNOR_BANK("Draynor Bank", "3093,3244,0"),  // OK
        EDGEVILLE_BANK("Edgeville Bank", "3093,3494,0"),  // OK
        FALADOR_EAST_BANK("Falador East Bank", "3013,3355,0"),  // Your current coords
        FALADOR_WEST_BANK("Falador West Bank", "2945,3368,0"),  // Your current coords
        FREMENNIK_DOCKS("Fremennik Docks", "2629,3676,0"),  // OK
        GNOME_STRONGHOLD_BANK("Gnome Stronghold Bank", "2445,3436,1"),  // OK
        GRAND_EXCHANGE("Grand Exchange", "3165,3487,0"),  // OK
        LUMBRIDGE_BANK("Lumbridge Bank", "3208,3220,2"),  // OK
        POLLNIVNEACH_BANK("Pollnivneach", "3359,2970,0"),  // Fixed
        SEERS_VILLAGE_BANK("Seers' Village Bank", "2726,3492,0"),  // Your coords (might need adjustment to 2708)
        SHILO_VILLAGE_BANK("Shilo Village Bank", "2853,2955,0"),  // OK
        TZHAAR_BANK("TzHaar Bank", "2480,5175,0"),  // Fixed (was 2444)
        VARLAMORE_CITY_BANK("Varlamore City Bank", "1647,3115,0"),  // Can't verify
        VARROCK_EAST_BANK("Varrock East Bank", "3253,3420,0"),  // Close enough
        VARROCK_WEST_BANK("Varrock West Bank", "3185,3436,0"),  // OK
        YANILLE_BANK("Yanille Bank", "2606,3093,0"),  // OK ✓
        ZEAH_ARCEUUS_BANK("Zeah Arceuus Bank", "1541,3757,0"),  // Can't verify
        CUSTOM("Custom (enter X,Y,Z below)", "");

        companion object {
            /**
             * Returns array of display names for GUI dropdown.
             */
            fun getDisplayNames(): Array<String> {
                return values().map { it.displayName }.toTypedArray()
            }

            /**
             * Finds a location by its display name.
             */
            fun fromDisplayName(name: String): Location? {
                return values().find { it.displayName == name }
            }
        }
    }
}