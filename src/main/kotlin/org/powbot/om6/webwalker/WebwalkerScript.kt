package org.powbot.webwalk

import org.powbot.api.Tile
import org.powbot.api.rt4.Players
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import org.powbot.webwalk.tasks.DestinationReachedTask
import org.powbot.webwalk.tasks.Task
import org.powbot.webwalk.tasks.WalkTask

/**
 * WebWalker script that walks to a user-configured tile.
 *
 * This script allows users to select from predefined locations or input
 * custom coordinates in X,Y,Z format and uses the POWBot web walker
 * to navigate to the specified location.
 */
@ScriptManifest(
    name = Constants.SCRIPT_NAME,
    description = Constants.SCRIPT_DESCRIPTION,
    version = Constants.SCRIPT_VERSION,
    author = Constants.SCRIPT_AUTHOR,
    category = ScriptCategory.Other
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = Constants.CONFIG_TARGET_LOCATION,
            description = "Select a predefined location or choose 'Custom' to enter coordinates.",
            defaultValue = Constants.DEFAULT_LOCATION,
            optionType = OptionType.STRING,
            allowedValues = ["Al Kharid Bank", "Ardougne South Bank", "Catherby Bank",
                "Draynor Bank", "Edgeville Bank", "Falador East Bank", "Falador West Bank",
                "Fremennik Docks", "Gnome Stronghold Bank", "Grand Exchange", "Lumbridge Bank",
                "Motherlode Mine Bank", "Pollnivneach Bank", "Seers' Village Bank",
                "Shilo Village Bank", "TzHaar Bank", "Varlamore City Bank", "Varrock East Bank",
                "Varrock West Bank", "Yanille Bank", "Zeah Arceuus Bank", "Custom (enter X,Y,Z below)"]
        ),
        ScriptConfiguration(
            name = "Custom Coordinates",
            description = "If 'Custom' is selected above, enter coordinates as X,Y,Z (e.g., 3208,3220,2).",
            defaultValue = "3208,3220,2",
            optionType = OptionType.STRING
        )
    ]
)
class WebWalkerScript : AbstractScript() {

    // Script state
    private var targetTile: Tile = Tile.Nil
    private var targetName: String = "Unknown"
    var currentTask: String = "Starting..."
        private set

    // Task list
    private val tasks = mutableListOf<Task>()

    override fun onStart() {
        logger.info("${Constants.SCRIPT_NAME} v${Constants.SCRIPT_VERSION} starting...")

        // Get the selected location
        val selectedLocation: String = getOption(Constants.CONFIG_TARGET_LOCATION)
        val customCoordinates: String = getOption("Custom Coordinates")

        // Determine the target tile based on selection
        val location = Constants.Location.fromDisplayName(selectedLocation)

        if (location != null && location != Constants.Location.CUSTOM) {
            // Use predefined location
            targetName = location.displayName
            targetTile = ScriptUtils.parseTileFromString(location.coordinates, this)
            logger.info("Using predefined location: $targetName")
        } else if (location == Constants.Location.CUSTOM) {
            // Use custom coordinates
            targetName = "Custom Location"
            targetTile = ScriptUtils.parseTileFromString(customCoordinates, this)
            logger.info("Using custom coordinates: $customCoordinates")
        } else {
            // Fallback - shouldn't happen
            logger.error("Invalid location selection: $selectedLocation")
            ScriptManager.stop()
            return
        }

        // Validate the parsed tile
        if (!ScriptUtils.isValidTile(targetTile)) {
            logger.error("Configuration error: Invalid target location")
            logger.error("Coordinates: ${if (location == Constants.Location.CUSTOM) customCoordinates else location?.coordinates}")
            ScriptManager.stop()
            return
        }

        // Check if the tile is walkable, if not search for a nearby walkable tile
        if (!ScriptUtils.isTileWalkable(targetTile)) {
            logger.warn("Target tile ${ScriptUtils.formatTile(targetTile)} is not walkable")
            logger.info("Searching for nearest walkable tile within ${Constants.TILE_SEARCH_RANGE} tile range...")

            val originalTile = targetTile
            targetTile = ScriptUtils.findNearestWalkableTile(
                targetTile,
                Constants.TILE_SEARCH_RANGE,
                this
            )

            if (targetTile != originalTile) {
                logger.info("Adjusted target from ${ScriptUtils.formatTile(originalTile)} to ${ScriptUtils.formatTile(targetTile)}")
                targetName += " (adjusted)"
            } else {
                logger.warn("Could not find walkable tile nearby. Will attempt original tile anyway.")
            }
        } else {
            logger.info("Target tile is walkable")
        }

        // Initialize tasks
        initializeTasks()

        // Setup paint overlay
        setupPaint()

        logger.info("Target: $targetName ${ScriptUtils.formatTile(targetTile)}")
        logger.info("Starting position: ${ScriptUtils.formatTile(Players.local().tile())}")
        logger.info("Script initialized successfully")
    }

    /**
     * Initializes the task list for the script.
     */
    private fun initializeTasks() {
        tasks.clear()
        tasks.add(WalkTask(this, targetTile))
        tasks.add(DestinationReachedTask(this, targetTile))

        logger.info("Initialized ${tasks.size} tasks")
    }

    /**
     * Sets up the paint overlay for the script GUI.
     */
    private fun setupPaint() {
        val paint = PaintBuilder.newBuilder()
            .x(Constants.PAINT_X)
            .y(Constants.PAINT_Y)
            .addString("Script:") { "${Constants.SCRIPT_NAME} v${Constants.SCRIPT_VERSION}" }
            .addString("Target:") { targetName }
            .addString("Location:") {
                "X: ${targetTile.x}, Y: ${targetTile.y}, Z: ${targetTile.floor}"
            }
            .addString("Current Task:") { currentTask }
            .addString("Distance:") {
                ScriptUtils.formatDistance(Players.local().distanceTo(targetTile))
            }
            .addString("Player Pos:") {
                val playerTile = Players.local().tile()
                ScriptUtils.formatTile(playerTile)
            }
            .build()

        addPaint(paint)
        logger.info("Paint overlay configured")
    }

    override fun poll() {
        // Safety check in case onStart failed
        if (targetTile == Tile.Nil) {
            logger.error("Target tile is invalid. Stopping script.")
            ScriptManager.stop()
            return
        }

        // Find and execute the first valid task
        val validTask = tasks.firstOrNull { it.validate() }

        if (validTask != null) {
            currentTask = validTask.getTaskDescription()
            logger.info("Executing task: $currentTask")
            validTask.execute()
        } else {
            // No valid tasks found - this shouldn't happen if tasks are properly configured
            logger.warn("No valid tasks found. Stopping script.")
            ScriptManager.stop()
        }
    }
}

/**
 * Entry point for standalone execution.
 */
fun main() {
    val script = WebWalkerScript()
    script.startScript("localhost", "WebWalker", false)
}