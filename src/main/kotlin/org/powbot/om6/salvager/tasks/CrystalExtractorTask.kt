package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.om6.salvager.*

class CrystalExtractorTask(script: ShipwreckSalvager) : Task(script) {

    override fun activate(): Boolean {
        // Only activate if the feature is enabled in the GUI
        if (!script.enableExtractor) {
            return false
        }

        // This task is highest priority when the message flag is set
        if (script.harvesterMessageFound) {
            script.logger.debug("ACTIVATE: Active due to Harvester message override.")
            return true
        }

        // Check if the timer is up (and we are in a phase where we can interrupt)
        val currentTime = System.currentTimeMillis()
        val timerExpired = currentTime - script.extractorTimer >= script.extractorInterval

        if (timerExpired) {
            script.logger.debug("ACTIVATE: Active due to 61-second timer expiration.")
            return true
        }

        return false
    }

    override fun execute() {
        // Capture the override state for logging
        val isOverride = script.harvesterMessageFound

        script.logger.info("ACTION: Starting Extractor Tap sequence (Override: $isOverride).")

        if (executeExtractorTap()) {
            // FIX: Only reset the message flag upon confirmed success
            script.harvesterMessageFound = false
            script.logger.info("SUCCESS: Extractor tap complete. Message flag reset. Transitioning back to READY_TO_TAP.")

            // Go back to ReadyToTap after the successful extractor tap and wait
            script.currentPhase = SalvagePhase.READY_TO_TAP
            script.phaseStartTime = System.currentTimeMillis()
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name} after extractor tap.")
        } else {
            script.logger.warn("FAIL: Extractor tap failed. Timer and message flag NOT reset. Will retry immediately if message is still active.")
            // Wait a short duration to prevent spamming the tap
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }

    /**
     * Checks if the tap is needed and, if so, executes the tap sequence.
     * This is designed for other tasks (e.g., DropSalvageTask) to call to interrupt their flow.
     * @return true if the task was activated and executed successfully, false otherwise.
     */
    fun checkAndExecuteInterrupt(): Boolean {
        if (activate()) {
            // Log the interrupt and call the execute logic which handles the tap, phase change, and resets
            script.logger.info("INTERRUPT: Crystal Extractor Tap is ACTIVATED during execution flow.")
            execute()
            return true
        }
        return false
    }

    /**
     * Executes the specific extractor tap and handles the post-tap wait.
     * @return true if the tap succeeded, false otherwise.
     */
    private fun executeExtractorTap(): Boolean {
        try {
            // 1. Snap Camera
            CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

            // 2. Tap Logic (571, 294 with +-3 randomization)
            val x = 571
            val y = 294
            val randomOffsetX = Random.nextInt(-3, 3)
            val randomOffsetY = Random.nextInt(-3, 3)
            val finalX = x + randomOffsetX
            val finalY = y + randomOffsetY
            Condition.sleep(Random.nextInt(600, 1200))
            script.hookCastMessageFound = false
            script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Offset: $randomOffsetX, $randomOffsetY).")
            val clicked = Input.tap(finalX, finalY)

            if (clicked) {
                // 3. Reset Timer and Wait
                script.extractorTimer = System.currentTimeMillis() // Timer reset on successful click execution
                val tapSleep = Random.nextInt(150, 250)
                Condition.sleep(tapSleep)

                val waitTime = Random.nextInt(2400, 3000)
                script.logger.info("WAIT: Extractor tap successful. Waiting $waitTime ms. Timer restarted.")
                Condition.sleep(waitTime)
                return true
            }
            script.logger.warn("FAIL: Failed to execute Input.tap() at ($finalX, $finalY).")
            return false
        } catch (e: Exception) {
            script.logger.error("CRASH PROTECTION: Extractor tap sequence failed with exception: ${e.message}", e)
            return false
        }
    }
}