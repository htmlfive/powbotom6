package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.tasks.Task

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

        script.logger.info("ACTION: Starting Extractor Tap sequence (Override: $isOverride).")

        if (executeExtractorTap()) {
            script.harvesterMessageFound = false
            script.extractorTimer = System.currentTimeMillis()
            script.logger.info("SUCCESS: Extractor tap complete. Timer reset.")
        } else {
            script.logger.warn("FAIL: Extractor tap failed. Timer and message flag NOT reset. Will retry immediately if still active.")
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }

    fun executeExtractorTap(): Boolean {
        try {
            // Requires CameraSnapper object
            CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

            val x = 289
            val y = 299
            val randomOffsetX = Random.nextInt(-3, 3)
            val randomOffsetY = Random.nextInt(-3, 3)
            val finalX = x + randomOffsetX
            val finalY = y + randomOffsetY
            Condition.sleep(Random.nextInt(600, 1200))

            script.harvesterMessageFound = false

            script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Offset: $randomOffsetX, $randomOffsetY).")
            val clicked = Input.tap(finalX, finalY)

            if (clicked) {
                val tapSleep = Random.nextInt(150, 250)
                Condition.sleep(tapSleep)

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