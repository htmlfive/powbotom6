package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Players
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvager.ShipwreckSalvager

class ReadyToTapTask(private val script: ShipwreckSalvager) : Task {

    // Removed: private val requiredDirection property is now read from the main script.

    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.READY_TO_TAP
    }

    override fun execute() {
        script.logger.info("TASK: READY_TO_TAP. Initiating screen tap.")

        // 1. Ensure the camera is facing the required direction using the shared utility
        // Now using the direction configured in the main script (e.g., CardinalDirection.East).
        CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

        // 2. Check for position drift and stop if detected
        val currentTile = Players.local().tile()
        if (script.startTile != null && (currentTile.x() != script.startTile!!.x() || currentTile.y() != script.startTile!!.y())) {
            script.logger.warn("Position change detected (X/Y)! Start: ${script.startTile}, Current: $currentTile. Stopping script.")
            ScriptManager.stop()
            return
        }

        script.salvageMessageFound = false

        // 3. Execute the center click action
        if (executeCenterClick()) {
            script.logger.info("Tap successful. Starting event-driven wait.")
            script.phaseStartTime = System.currentTimeMillis()
            script.currentPhase = SalvagePhase.WAITING_FOR_ACTION
        } else {
            script.logger.warn("Failed to execute screen tap. Retrying on next poll.")
            Condition.sleep(Random.nextInt(1000, 1500))
        }
    }

    private fun executeCenterClick(): Boolean {
        val dimensions = Game.dimensions()
        val centerX = dimensions.width / 2
        val centerY = dimensions.height / 2

        val randomOffsetX = Random.nextInt(-10, 12)
        val randomOffsetY = Random.nextInt(-12, 9)

        // Using the user's custom offsets: -35 on X, +50 on Y
        val finalX = centerX + randomOffsetX - 35
        val finalY = centerY + randomOffsetY + 50

        script.logger.info("Tapping screen at randomized point X=$finalX, Y=$finalY (Base: $centerX, $centerY).")

        val clicked = Input.tap(finalX, finalY)

        Condition.sleep(Random.nextInt(300, 500))

        return clicked
    }
}