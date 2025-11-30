package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class SortSalvageTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Activate when in SORTING phase and we have salvage to sort
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return script.currentPhase == SalvagePhase.SORTING_LOOT && hasSalvage && script.cargoHoldFull
    }

    override fun execute() {
        script.logger.info("SORT: Starting sort sequence.")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeTapSortSalvage()

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            script.logger.info("SORT: Sort completed successfully.")
            // Inventory should now be empty of salvage
            // Will trigger withdrawal next poll
        } else {
            script.logger.warn("SORT: Sort failed or timed out.")
        }
    }

    /**
     * Executes the sort salvage tap sequence and waits for completion.
     * @return true if sorting completed successfully
     */
    private fun executeTapSortSalvage(): Boolean {
        val salvageItemName = script.SALVAGE_NAME
        CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
        Condition.sleep(Random.nextInt(Constants.SORT_PRE_TAP_MIN, Constants.SORT_PRE_TAP_MAX))

        val randomOffsetX = Random.nextInt(-Constants.SORT_BUTTON_TOLERANCE_X, Constants.SORT_BUTTON_TOLERANCE_X + 13)
        val randomOffsetY = Random.nextInt(-Constants.SORT_BUTTON_TOLERANCE_Y, Constants.SORT_BUTTON_TOLERANCE_Y + 1)
        val finalX = Constants.SORT_BUTTON_X + randomOffsetX
        val finalY = Constants.SORT_BUTTON_Y + randomOffsetY

        val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

        ensureInventoryOpen(Constants.SORT_TAB_OPEN_MIN, Constants.SORT_TAB_OPEN_MAX)
        Condition.sleep(Random.nextInt(Constants.SORT_TAB_CLOSE_MIN, Constants.SORT_TAB_CLOSE_MAX))

        script.logger.info("ACTION: Tapping Sort Salvage at X=$finalX, Y=$finalY. Count: $salvageCountBefore.")
        closeTabWithSleep(Constants.SORT_TAB_CLOSE_MIN, Constants.SORT_TAB_CLOSE_MAX)

        if (tapWithOffset(finalX, finalY, 0)) {
            var elapsed = 0L
            var currentSalvageCount = salvageCountBefore
            var lastSalvageCount = salvageCountBefore
            var retapFailureCount = 0
            val maxRetapFailures = 2

            script.logger.info("RETAP: Starting ${Constants.SORT_INITIAL_WAIT}ms check for active sorting.")

            while (elapsed < Constants.SORT_INITIAL_WAIT) {
                Condition.sleep(Constants.SORT_CHECK_INTERVAL)
                elapsed += Constants.SORT_CHECK_INTERVAL

                currentSalvageCount = Inventory.stream().name(salvageItemName).count()

                if (currentSalvageCount < salvageCountBefore) {
                    script.logger.info("RETAP: Sort started. Items removed: ${salvageCountBefore - currentSalvageCount}.")
                    break
                }

                handleDialogue(Constants.SORT_RETAP_MIN, Constants.SORT_RETAP_MAX)

                if (currentSalvageCount >= lastSalvageCount) {
                    retapFailureCount++

                    if (retapFailureCount > maxRetapFailures) {
                        script.logger.error("FATAL: Sort stalled after $maxRetapFailures retaps. Stopping.")
                        ScriptManager.stop()
                        return true
                    }

                    script.logger.warn("RETAP: Count unchanged. Retapping (Attempt $retapFailureCount).")
                    if (tapWithOffset(finalX, finalY, 0)) {
                        Condition.sleep(Random.nextInt(Constants.SORT_RETAP_MIN, Constants.SORT_RETAP_MAX))
                        lastSalvageCount = currentSalvageCount
                    }
                } else {
                    retapFailureCount = 0
                    lastSalvageCount = currentSalvageCount
                }
            }

            val timeoutTicks = 20
            var waitSuccess = currentSalvageCount.toInt() == 0
            var attempts = 0

            if (!waitSuccess) {
                script.logger.info("POLLING: Waiting for inventory clear.")

                while (attempts < timeoutTicks) {
                    if (extractorTask.checkAndExecuteInterrupt(script)) {
                        script.logger.warn("INTERRUPT: Extractor ran. Re-tapping Sort.")
                        if (tapWithOffset(finalX, finalY, 0)) {
                            Condition.sleep(Constants.SORT_POST_INTERRUPT_WAIT)
                            attempts = 0
                            continue
                        }
                    }

                    if (Inventory.stream().name(salvageItemName).isEmpty()) {
                        waitSuccess = true
                        script.logger.info("POLLING: Inventory cleared.")
                        break
                    }

                    Condition.sleep(Constants.SORT_MAIN_CHECK_INTERVAL)
                    attempts++
                }
            }

            if (waitSuccess) {
                script.logger.info("SUCCESS: Sort complete.")
                Condition.sleep(Random.nextInt(Constants.SORT_SUCCESS_WAIT_MIN, Constants.SORT_SUCCESS_WAIT_MAX))
                return true
            } else {
                script.logger.warn("FAIL: Sort timed out.")
                return false
            }
        }
        return false
    }
}
