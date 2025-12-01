package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
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

        // Setup camera and initial wait
        CameraSnapper.snapCameraToDirection(script.cameraDirection, script)
        Condition.sleep(Random.nextInt(Constants.SORT_PRE_TAP_MIN, Constants.SORT_PRE_TAP_MAX))

        // Calculate tap coordinates with random offset
        val randomOffsetX = Random.nextInt(-Constants.SORT_BUTTON_TOLERANCE_X, Constants.SORT_BUTTON_TOLERANCE_X + 13)
        val randomOffsetY = Random.nextInt(-Constants.SORT_BUTTON_TOLERANCE_Y, Constants.SORT_BUTTON_TOLERANCE_Y + 1)
        val finalX = Constants.SORT_BUTTON_X + randomOffsetX
        val finalY = Constants.SORT_BUTTON_Y + randomOffsetY

        val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

        // Prepare inventory tab
        ensureInventoryOpen(Constants.SORT_TAB_OPEN_MIN, Constants.SORT_TAB_OPEN_MAX)
        Condition.sleep(Random.nextInt(Constants.SORT_TAB_CLOSE_MIN, Constants.SORT_TAB_CLOSE_MAX))

        script.logger.info("ACTION: Tapping Sort Salvage at X=$finalX, Y=$finalY. Count: $salvageCountBefore.")
        closeTabWithSleep(Constants.SORT_TAB_CLOSE_MIN, Constants.SORT_TAB_CLOSE_MAX)

        // Execute initial tap
        if (!clickAtCoordinates(finalX, finalY, Constants.SORT_BUTTON_MENUOPTION)) {
            script.logger.warn("SORT: Initial tap failed.")
            return false
        }

        // Phase 1: Monitor and retap if sorting doesn't start
        val currentCount = monitorAndRetapIfStalled(
            script = script,
            salvageItemName = salvageItemName,
            initialCount = salvageCountBefore,
            tapX = finalX,
            tapY = finalY,
            action = Constants.SORT_BUTTON_MENUOPTION,
            initialWaitMs = Constants.SORT_INITIAL_WAIT.toLong(),
            checkIntervalMs = Constants.SORT_CHECK_INTERVAL.toLong(),
            maxRetapFailures = 2,
            retapSleepMin = Constants.SORT_RETAP_MIN,
            retapSleepMax = Constants.SORT_RETAP_MAX
        )

        // If already cleared during monitoring phase
        if (currentCount.toInt() == 0) {
            script.logger.info("SUCCESS: Sort complete during monitoring phase.")
            Condition.sleep(Random.nextInt(Constants.SORT_SUCCESS_WAIT_MIN, Constants.SORT_SUCCESS_WAIT_MAX))
            return true
        }

        // Phase 2: Wait for inventory to clear
        val waitSuccess = waitForInventoryClear(
            script = script,
            extractorTask = extractorTask,
            salvageItemName = salvageItemName,
            tapX = finalX,
            tapY = finalY,
            maxAttempts = 60,
            checkIntervalMs = Constants.SORT_MAIN_CHECK_INTERVAL.toLong(),
            postInterruptWaitMs = Constants.SORT_POST_INTERRUPT_WAIT.toLong()
        )

        if (waitSuccess) {
            script.logger.info("SUCCESS: Sort complete.")
            Condition.sleep(Random.nextInt(Constants.SORT_SUCCESS_WAIT_MIN, Constants.SORT_SUCCESS_WAIT_MAX))
            return true
        } else {
            script.logger.warn("FAIL: Sort timed out.")
            return false
        }
    }
}