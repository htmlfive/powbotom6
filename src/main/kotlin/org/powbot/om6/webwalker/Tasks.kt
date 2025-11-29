package org.powbot.webwalk.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.script.AbstractScript
import org.powbot.mobile.script.ScriptManager
import org.powbot.webwalk.Constants
import org.powbot.webwalk.ScriptUtils

/**
 * Base interface for all tasks in the WebWalker script.
 */
interface Task {
    val script: AbstractScript

    /**
     * Determines if this task should be executed.
     * @return true if the task is valid and should run
     */
    fun validate(): Boolean

    /**
     * Executes the task's logic.
     */
    fun execute()

    /**
     * Returns a descriptive name for the current task state.
     */
    fun getTaskDescription(): String
}

/**
 * Task responsible for walking to the target tile using the web walker.
 */
class WalkTask(
    override val script: AbstractScript,
    private val targetTile: Tile
) : Task {

    override fun validate(): Boolean {
        // The task is valid if we are not within the destination threshold
        val localPlayer = Players.local()
        return localPlayer.distanceTo(targetTile) > Constants.DESTINATION_DISTANCE_THRESHOLD
    }

    override fun execute() {
        val localPlayer = Players.local()
        val distanceToTarget = localPlayer.distanceTo(targetTile)

        script.logger.info(
            "Walking to ${ScriptUtils.formatTile(targetTile)} " +
                    "(${ScriptUtils.formatDistance(distanceToTarget)} away)"
        )

        // Initiate web walk to the target tile
        if (Movement.walkTo(targetTile)) {
            script.logger.info("Web walk initiated successfully")

            // Wait until we reach the destination or stop moving
            val reached = Condition.wait(
                { Players.local().distanceTo(targetTile) <= Constants.DESTINATION_DISTANCE_THRESHOLD },
                Constants.WALK_WAIT_TIMEOUT,
                Constants.WALK_WAIT_ITERATIONS
            )

            if (reached) {
                script.logger.info("Destination reached!")
            } else {
                script.logger.warn("Walk timeout - may still be moving or encountered obstacle")
            }
        } else {
            script.logger.error("Failed to initiate web walk to target tile")
        }
    }

    override fun getTaskDescription(): String {
        val distance = Players.local().distanceTo(targetTile)
        return "Walking to ${ScriptUtils.formatTile(targetTile)} - ${ScriptUtils.formatDistance(distance)}"
    }
}

/**
 * Task executed when the player has reached the destination.
 */
class DestinationReachedTask(
    override val script: AbstractScript,
    private val targetTile: Tile
) : Task {

    override fun validate(): Boolean {
        // This task is valid when we are at the destination
        val localPlayer = Players.local()
        return localPlayer.distanceTo(targetTile) <= Constants.DESTINATION_DISTANCE_THRESHOLD
    }

    override fun execute() {
        script.logger.info("Destination reached: ${ScriptUtils.formatTile(targetTile)}")
        script.logger.info("Script completed successfully. Stopping...")

        // Brief pause before stopping
        Condition.sleep(Constants.DESTINATION_SLEEP_TIME)

        // Stop the script
        ScriptManager.stop()
    }

    override fun getTaskDescription(): String {
        return "Destination Reached: ${ScriptUtils.formatTile(targetTile)}"
    }
}