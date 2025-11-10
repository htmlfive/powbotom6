package org.powbot.webwalk

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager

// --- Task Interface ---
interface Task {
    val script: AbstractScript
    fun validate(): Boolean
    fun execute()
}

/**
 * Task to walk to the target tile.
 */
class WalkTask(override val script: WebWalkerScript, private val targetTile: Tile) : Task {
    override fun validate(): Boolean {
        // The task is valid (needs to be executed) if we are not close to the target tile
        return Players.local().distanceTo(targetTile) > 5
    }

    override fun execute() {
        script.currentTask = "Walking to $targetTile"
        // Use Movement.walkTo(tile) which utilizes the web walker
        Movement.walkTo(targetTile)

        // Wait until we are either close to the tile or the script stops moving
        Condition.wait({  Players.local().distanceTo(targetTile) <= 5 }, 150, 20)
    }
}


/**
 * Script that simply walks to a user-configured Tile (x, y, z) entered as a string.
 */
@ScriptManifest(
    name = "0m6 Webwalker",
    description = "Walks to a custom tile (X,Y,Z) entered by the user.",
    version = "2.5.3", // Updated version
    author = "0m6 WebWalker",
    category = ScriptCategory.Other
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Target Location",
            description = "Enter the destination tile as X,Y,Z (e.g., 3208,3220,2).",
            defaultValue = "2657,3657,0", // Lumbridge Bank as a default custom inpu
            optionType = OptionType.STRING // CHANGED: Now a simple STRING input
            // REMOVED: allowedValues = Locations.OPTIONS_ARRAY
        )
    ]
)
class WebWalkerScript : AbstractScript() {

    private var targetTile: Tile = Tile.Nil
    // The target name is now hardcoded since there is no selection
    private var targetName: String = "Custom Location"
    var currentTask: String = "Starting..."

    // Task variable must be initialized in onStart, but declared here
    private lateinit var walkTask: WalkTask

    /**
     * Parses the custom string "X,Y,Z" into a Tile object.
     */
    private fun parseTile(locationString: String): Tile {
        val coordString = locationString.trim()
        targetName = "Custom Tile" // Set name to reflect custom input

        return try {
            val parts = coordString.split(",").map { it.trim() }
            if (parts.size == 3) {
                Tile(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                logger.error("Invalid coordinate format. Must be X,Y,Z. Received: $coordString")
                Tile.Nil
            }
        } catch (e: Exception) {
            logger.error("Failed to parse coordinates from: $locationString", e)
            Tile.Nil
        }
    }

    override fun onStart() {
        // Read the custom X,Y,Z string from the configuration
        val locationString: String = getOption("Target Location")
        targetTile = parseTile(locationString)

        if (targetTile == Tile.Nil) {
            logger.error("Configuration error. Failed to parse target location. Stopping script.")
            ScriptManager.stop()
            return
        }

        walkTask = WalkTask(this, targetTile) // Initialize the task here

        logger.info("Script started. Target: $targetName at ${targetTile.x}, ${targetTile.y}, ${targetTile.floor}")

        // --- Paint Setup ---
        val paint = PaintBuilder.newBuilder()
            .x(10).y(200)
            .addString("Target:") { targetName }
            .addString("Current Task:") { currentTask }
            .addString("Location:") { "X: ${targetTile.x}, Y: ${targetTile.y}, Z: ${targetTile.floor}" }
            .addString("Distance:") { "%.1f tiles".format(Players.local().distanceTo(targetTile)) }
            .build()
        addPaint(paint)
    }

    override fun poll() {
        if (targetTile == Tile.Nil) {
            // Safety check in case onStart failed but script didn't stop
            return
        }

        if (walkTask.validate()) {
            walkTask.execute()
        } else {
            currentTask = "Destination Reached: $targetTile"
            Condition.sleep(2000) // Sleep when finished
            ScriptManager.stop()
        }
    }
}

fun main() {
    val script = WebWalkerScript()
    script.startScript("localhost", "WebWalker", false)
}

// Visual purposes only: The list of locations that were removed.
/**
 * Visual list of locations (REMOVED FROM SCRIPT LOGIC):
 * "Al Kharid Bank (3269,3167,0)",
 * "Ardougne South Bank (2655,3283,0)",
 * "Catherby Bank (2809,3441,0)",
 * "Draynor Bank (3093,3244,0)",
 * "Edgeville Bank (3093,3494,0)",
 * "Falador East Bank (3383,3448,0)",
 * "Falador West Bank (3288,3485,0)",
 * "Fremennik Docks (2629,3676,0)",
 * "Gnome Stronghold Bank (2445,3436,1)",
 * "Grand Exchange (3165,3487,0)",
 * "Lumbridge Bank (3208,3220,2)",
 * "Motherlode Mine Bank (3760,5666,0)",
 * "Pollnivneach Bank (3349,3004,0)",
 * "Seers' Village Bank (2726,3492,0)",
 * "Shilo Village Bank (2853,2955,0)",
 * "TzHaar Bank (2444,5174,0)",
 * "Varlamore City Bank (1647,3115,0)",
 * "Varrock East Bank (3253,3420,0)",
 * "Varrock West Bank (3185,3436,0)",
 * "Yanille Bank (2606,3093,0)",
 * "Zeah Arceuus Bank (1541,3757,0)"
 */