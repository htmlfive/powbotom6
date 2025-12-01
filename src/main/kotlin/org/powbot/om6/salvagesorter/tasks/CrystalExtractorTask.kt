package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase

class CrystalExtractorTask(script: SalvageSorter) : Task(script) {

    override fun activate(): Boolean {
        if (!script.enableExtractor) return false

        // High Priority: Chat Message Override
        if (script.harvesterMessageFound) {
            script.logger.debug("ACTIVATE: Active due to Harvester message override.")
            return true
        }

        // Low Priority: Timer Expiration
        val currentTime = System.currentTimeMillis()
        val timerExpired = currentTime - script.extractorTimer >= script.extractorInterval

        if (timerExpired) {
            script.logger.debug("ACTIVATE: Active due to ${script.extractorInterval / 1000}-second timer expiration.")
            return true
        }

        return false
    }

    override fun execute() {
        val isOverride = script.harvesterMessageFound

        // SAVE the phase BEFORE executing extractor tap
        val phaseBeforeInterrupt = script.currentPhase

        script.logger.info("ACTION: Starting Extractor Tap sequence (Override: $isOverride). Saving phase: $phaseBeforeInterrupt")

        if (executeExtractorTap()) {
            script.harvesterMessageFound = false
            script.extractorTimer = System.currentTimeMillis()
            script.logger.info("SUCCESS: Extractor tap complete. Timer reset.")

            // RESTORE the phase we were in before the interrupt
            script.currentPhase = phaseBeforeInterrupt
            script.logger.info("PHASE: Restored phase to $phaseBeforeInterrupt after extractor interrupt.")

        } else {
            script.logger.warn("FAIL: Extractor tap failed. Timer and message flag NOT reset. Will retry immediately if still active.")
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }

    fun executeExtractorTap(): Boolean {
        try {
            val x = 307
            val y = 231

            CameraSnapper.snapCameraToDirection(script.cameraDirection, script)

            val randomOffsetX = Random.nextInt(-6, 7)
            val randomOffsetY = Random.nextInt(-5, 6)
            val finalX = x + randomOffsetX
            val finalY = y + randomOffsetY
            script.harvesterMessageFound = false

            script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Offset: $randomOffsetX, $randomOffsetY).")

            if (clickAtCoordinates(x,y,"Harvest")) {

                val waitTime = Random.nextInt(2400, 3000)
                script.logger.info("WAIT: Extractor tap successful. Waiting $waitTime ms.")
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

    /**
     * Checks activation and executes the tap sequence if active.
     * Used by other tasks to interrupt their flow.
     */
    fun checkAndExecuteInterrupt(script: SalvageSorter): Boolean {
        if (this.activate()) {
            script.logger.info("INTERRUPT: Crystal Extractor Tap is ACTIVATED during task flow.")
            this.execute()
            return true
        }
        return false
    }
}