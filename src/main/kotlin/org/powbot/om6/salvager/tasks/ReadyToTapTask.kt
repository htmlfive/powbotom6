package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Players
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvager.*
import org.powbot.api.rt4.Chat


class ReadyToTapTask(script: ShipwreckSalvager) : Task(script) {

    override fun activate(): Boolean {
        val isActive = script.currentPhase == SalvagePhase.READY_TO_TAP
        script.logger.debug("ACTIVATE: Checking if phase is ${SalvagePhase.READY_TO_TAP.name} ($isActive).")
        return isActive
    }

    override fun execute() {
        script.logger.info("TASK: READY_TO_TAP. Initiating screen tap.")

        script.logger.info("ACTION: Snapping camera to required tap direction: ${script.requiredTapDirection.name}.")
        CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

        val currentTile = Players.local().tile()
        if (script.startTile != null && (currentTile.x() != script.startTile!!.x() || currentTile.y() != script.startTile!!.y())) {

            // --- Position Drift Check (as previously modified) ---
            if (script.stopIfMoved) {
                script.logger.error("POSITION DRIFT DETECTED! Start: ${script.startTile}, Current: $currentTile. Stop if Moved is TRUE. Stopping script immediately.")
                ScriptManager.stop()
                return
            } else {
                script.logger.warn("POSITION DRIFT DETECTED! Start: ${script.startTile}, Current: $currentTile. Stop if Moved is FALSE. Continuing...")
            }
        }
        script.logger.info("POSITION CHECK: Player position stable at $currentTile.")

        script.salvageMessageFound = false
        script.logger.debug("LOGIC: Reset salvageMessageFound to false.")

        if (executeCenterClick()) {
            script.logger.info("ACTION: Tap successful. Transitioning to WAITING_FOR_ACTION.")
            script.phaseStartTime = System.currentTimeMillis()
            script.currentPhase = SalvagePhase.WAITING_FOR_ACTION
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name}.")
        } else {
            script.logger.warn("FAIL: Failed to execute screen tap. Retrying on next poll.")
            val sleepTime = Random.nextInt(1000, 1500)
            script.logger.info("SLEEP: Sleeping for $sleepTime ms before next poll attempt.")
            Condition.sleep(sleepTime)
        }
    }

    private fun executeCenterClick(): Boolean {
        val dimensions = Game.dimensions()
        val centerX = 406
        val centerY = 341
        script.logger.debug("CLIENT: Screen dimensions (W x H): ${dimensions.width} x ${dimensions.height}. Center: ($centerX, $centerY).")

        val randomOffsetX = Random.nextInt(-10, 12)
        val randomOffsetY = Random.nextInt(-12, 9)

        val finalX = centerX + randomOffsetX
        val finalY = centerY + randomOffsetY

        script.logger.info("ACTION: Tapping screen at randomized point X=$finalX, Y=$finalY (Offset: X=$randomOffsetX, Y=$randomOffsetY).")

        script.hookCastMessageFound = false // Reset message flag before tap
        val clicked = Input.tap(finalX, finalY)

        if (clicked) {
            val tapSleep = Random.nextInt(300, 500)
            Condition.sleep(tapSleep)
            script.logger.info("SLEEP: Slept for $tapSleep ms after tap, before message check.")

            script.logger.info("CHECK: Waiting for confirmation message.")
            val messageFound = Condition.wait({ script.hookCastMessageFound }, 30, 60) // 10ms polling for max 60 iterations (600ms)

            if (messageFound) {
                script.logger.info("SUCCESS: Action start message received. Continuing execution.")
                return true
            } else {
                if (Chat.canContinue()) {
                    script.logger.info("FAILURE: Action start message NOT received, BUT Chat.canContinue() is TRUE. Assuming dialogue interrupted the action. Continuing to next poll.")
                    return false
                } else {
                    script.logger.info("STOPPING: Action start message NOT received within 600ms AND no dialogue found. Stopping script.")
                    ScriptManager.stop() // STOP SCRIPT if message is not found AND no dialogue is present
                    return false
                }
            }
        }
        return false
    }
}