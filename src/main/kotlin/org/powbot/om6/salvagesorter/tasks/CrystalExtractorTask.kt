package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Notifications
import org.powbot.api.Random
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class CrystalExtractorTask(script: SalvageSorter) : Task(script) {

    override fun activate(): Boolean {
        if (!script.enableExtractor) return false

        // Only activate during SORTING_LOOT, SALVAGING, or CLEANING phases (not during WITHDRAWING)
        val isAllowedPhase = script.currentPhase in listOf(
            SalvagePhase.SORTING_LOOT,
            SalvagePhase.SALVAGING,
            SalvagePhase.CLEANING
        )
        if (!isAllowedPhase) {
            return false
        }

        // Don't activate if at withdraw spot during SORTING_LOOT phase
        if (script.currentPhase == SalvagePhase.SORTING_LOOT && script.atWithdrawSpot) {
            script.logger.debug("ACTIVATE: Skipping extractor - at withdraw spot, need to walk back first.")
            return false
        }
        if (isWidgetVisible(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_DEPOSIT_SALVAGE)){
            script.logger.debug("ACTIVATE: Skipping extractor - in menu")
            return false
        }
        // Don't activate if at salvaging spot during but not confirmed salvgaing phase
        if (script.currentPhase == SalvagePhase.SALVAGING && !script.hookingSalvageBool && !script.atWithdrawSpot ) {
            script.logger.debug("ACTIVATE: Skipping extractor - at withdraw spot, need to walk back first.")
            return false
        }

        // High Priority: Chat Message Override
        if (script.harvesterMessageFound) {
            script.logger.debug("ACTIVATE: Active due to Harvester message override during ${script.currentPhase.name}.")
            return true
        }

        // Low Priority: Timer Expiration
        val currentTime = System.currentTimeMillis()
        val timerExpired = currentTime - script.extractorTimer >= script.extractorInterval

        if (timerExpired) {
            script.logger.debug("ACTIVATE: Active due to ${script.extractorInterval / 1000}-second timer expiration during ${script.currentPhase.name}.")
            return true
        }

        return false
    }

    override fun execute() {
        val isOverride = script.harvesterMessageFound

        val phaseBeforeInterrupt = script.currentPhase

        script.logger.info("ACTION: Starting Extractor Tap sequence (Override: $isOverride). Saving phase: $phaseBeforeInterrupt")

        if (executeExtractorTap()) {
            script.harvesterMessageFound = false
            script.extractorTimer = System.currentTimeMillis()
            script.logger.info("SUCCESS: Extractor tap complete. Timer reset.")

            script.currentPhase = phaseBeforeInterrupt
            script.logger.info("PHASE: Restored phase to $phaseBeforeInterrupt after extractor interrupt.")

        } else {
            script.logger.warn("FAIL: Extractor tap failed. Timer and message flag NOT reset. Will retry immediately if still active.")
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }

    fun executeExtractorTap(): Boolean {
        val maxRetries = Random.nextInt(5,7)
        var attempt = 0

        while (attempt < maxRetries) {
            attempt++
            try {
                val (x, y) = if (script.atWithdrawSpot) {
                    Constants.EXTRACTOR_HOOK_X to Constants.EXTRACTOR_HOOK_Y
                } else {
                    Constants.EXTRACTOR_SORT_X to Constants.EXTRACTOR_SORT_Y
                }
                val randomOffsetX = Random.nextInt(-6, 6)
                val randomOffsetY = Random.nextInt(-6, 6)
                val finalX = x + randomOffsetX
                val finalY = y + randomOffsetY
                script.harvesterMessageFound = false

                script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Offset: $randomOffsetX, $randomOffsetY). [Attempt $attempt/$maxRetries]")

                if (clickAtCoordinates(finalX, finalY, "Harvest", "Activate")) {
                    val waitTime = Random.nextInt(2400, 3000)
                    script.logger.info("WAIT: Extractor tap successful. Waiting $waitTime ms.")
                    Condition.sleep(waitTime)
                    return true
                }

                script.logger.warn("FAIL: Failed to execute Input.tap() at ($finalX, $finalY). [Attempt $attempt/$maxRetries]")

                if (attempt < maxRetries) {
                    val retryDelay = Random.nextInt(800, 1200)
                    script.logger.info("RETRY: Waiting $retryDelay ms before retry...")
                    Condition.sleep(retryDelay)
                }
            } catch (e: Exception) {
                script.logger.error("CRASH PROTECTION: Extractor tap sequence failed with exception: ${e.message} [Attempt $attempt/$maxRetries]", e)

                if (attempt < maxRetries) {
                    val retryDelay = Random.nextInt(800, 1200)
                    script.logger.info("RETRY: Waiting $retryDelay ms before retry...")
                    Condition.sleep(retryDelay)
                }
            }
        }

        script.logger.error("FAIL: Extractor tap failed after $maxRetries attempts. Stopping.")
        Notifications.showNotification("FAIL: Extractor tap failed after $maxRetries attempts. Stopping.")
        ScriptManager.stop()

        return false
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