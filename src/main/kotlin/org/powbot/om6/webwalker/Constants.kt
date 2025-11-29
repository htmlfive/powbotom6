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
        AL_KHARID_BANK("Al Kharid Bank", "3269,3166,0"),  //Good
        ARDOUGNE_SOUTH_BANK("Ardougne South Bank", "2655,3283,0"),  //Good
        CATHERBY_BANK("Catherby Bank", "2808,3440,0"),  //Good
        DIGSITE("Digsite", "3360,3445,0"),  //Good
        DRAYNOR_BANK("Draynor Bank", "3093,3244,0"),  //Good
        EDGEVILLE_BANK("Edgeville Bank", "3093,3490,0"),  //Good
        FALADOR_EAST_BANK("Falador East Bank", "3013,3355,0"),  //Good
        FALADOR_WEST_BANK("Falador West Bank", "2945,3368,0"),  //Good
        FREMENNIK_DOCKS("Fremennik Docks", "2629,3680,0"),  //Good
        GNOME_STRONGHOLD_BANK("Gnome Stronghold Bank", "2449,3481,1"),  //Good
        GRAND_EXCHANGE("Grand Exchange", "3165,3487,0"),  //Good
        LUMBRIDGE_BANK("Lumbridge Bank", "3208,3220,2"),  //Good
        POLLNIVNEACH_BANK("Pollnivneach", "3359,2970,0"),  //Good
        PORT_SARIM("Port Sarim", "3027,3027,0"),  //Good
        SEERS_VILLAGE_BANK("Seers' Village Bank", "2726,3492,0"),  //Good
        SHILO_VILLAGE_BANK("Shilo Village Bank", "2850,2955,0"),  //Good
        TZHAAR_BANK("TzHaar Bank", "2445,5178,0"),  //Good
        VARLAMORE_CITY_BANK("Varlamore City Bank", "1647,3115,0"),  //Good
        VARROCK_EAST_BANK("Varrock East Bank", "3253,3420,0"),  //Good
        VARROCK_WEST_BANK("Varrock West Bank", "3182,3440,0"),  //Good
        YANILLE_BANK("Yanille Bank", "2610,3092,0"),  //Good
        ZEAH_ARCEUUS_BANK("Zeah Arceuus Bank", "1629,3752,0"),  //Good

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