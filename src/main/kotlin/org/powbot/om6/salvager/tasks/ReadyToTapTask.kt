package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Players
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvager.* // Assuming this includes ShipwreckSalvager, SalvagePhase, CameraSnapper, and Task
import org.powbot.api.rt4.Chat


class ReadyToTapTask(script: ShipwreckSalvager) : Task(script) {

    override fun activate(): Boolean {
        val isActive = script.currentPhase == SalvagePhase.READY_TO_TAP
        script.logger.debug("ACTIVATE: Checking if phase is ${SalvagePhase.READY_TO_TAP.name} ($isActive).")
        return isActive
    }

    override fun execute() {
        script.logger.info("TASK: READY_TO_TAP. Initiating screen tap.")

        // --- SPECIFIC, TIMED TAP CHECK (The Extractor Tap) ---
        val currentTime = System.currentTimeMillis()

        // Check if 61 seconds (or more) have passed since the last extractor tap
        if (currentTime - script.extractorTimer >= script.extractorInterval) {
            script.logger.info("EXTRACTOR TIMER: 61-second extractor timer has finished. Executing specific tap at (571, 294) with randomization.")

            if (executeSpecificTap(571, 294)) {
                // SUCCESS: Update timer and wait
                script.extractorTimer = currentTime // Restart the extractor timer
                val waitTime = Random.nextInt(1800, 2400)
                script.logger.info("WAIT: Extractor tap successful. Waiting $waitTime ms as requested. Extractor timer restarted.")
                Condition.sleep(waitTime)

                script.logger.info("CONTINUE: Finished extractor tap cycle. Continuing script.")
                return // Exit execute() after the timed tap and wait
            } else {
                // FAILURE: Log and continue to next poll
                script.logger.warn("FAIL: Failed to execute extractor tap at (571, 294). Retrying on next poll.")
                return
            }
        }
        // --- END EXTRACTOR TAP CHECK ---


        // --- ORIGINAL GAME-SPECIFIC LOGIC (Runs if the extractor timer is NOT finished) ---
        script.logger.info("LOGIC: Extractor timer not finished. Proceeding with regular game logic.")

        script.logger.info("ACTION: Snapping camera to required tap direction: ${script.requiredTapDirection.name}.")
        CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

        val currentTile = Players.local().tile()
        if (script.startTile != null && (currentTile.x() != script.startTile!!.x() || currentTile.y() != script.startTile!!.y())) {

            // --- Position Drift Check ---
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
            script.logger.info("ACTION: Regular tap successful. Transitioning to WAITING_FOR_ACTION.")
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

    /**
     * Executes the specific, timed extractor tap with a random offset of +- 5.
     */
    private fun executeSpecificTap(x: Int, y: Int): Boolean {
        // Generate random offsets between -5 and +5 (inclusive)
        val randomOffsetX = Random.nextInt(-3, 3)
        val randomOffsetY = Random.nextInt(-3, 3)

        val finalX = x + randomOffsetX
        val finalY = y + randomOffsetY

        script.hookCastMessageFound = false // Reset message flag
        script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Base: $x, $y | Offset: $randomOffsetX, $randomOffsetY).")
        val clicked = Input.tap(finalX, finalY)
        Condition.sleep(Random.nextInt(600,1800))

        if (clicked) {
            val tapSleep = Random.nextInt(1800, 2400) // A short, realistic sleep after input
            Condition.sleep(tapSleep)
            script.logger.debug("SLEEP: Slept for $tapSleep ms after extractor tap.")
            return true
        }
        return false
    }

    private fun executeCenterClick(): Boolean {
        // Function for the original, main task center tap logic (with message/dialogue checks)
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
            // 10ms polling for max 60 iterations (600ms)
            val messageFound = Condition.wait({ script.hookCastMessageFound }, 30, 60)

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