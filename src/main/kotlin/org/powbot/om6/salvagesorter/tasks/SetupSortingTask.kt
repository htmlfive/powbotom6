package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class SetupSortingTask(script: SalvageSorter) : Task(script) {
    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.SETUP_SORTING
    }

    override fun execute() {
        script.logger.info("SETUP: Entering Sorting mode.")

        // CRITICAL: Reset the hook location flag when entering sorting mode
        script.atHookLocation = false
        script.logger.info("SETUP: Reset atHookLocation flag to false for next salvaging phase.")

        // Walk to sorting position and assign crew
        val success = walkToSort()

        if (success) {
            script.logger.info("SETUP: Setup complete. Transitioning to WITHDRAWING.")
            script.currentPhase = SalvagePhase.WITHDRAWING
            // Set initial cooldown to 0 so withdrawal happens immediately
            script.currentWithdrawCooldownMs = 0L
            script.lastWithdrawOrCleanupTime = 0L
        } else {
            script.logger.warn("SETUP: Setup failed. Will retry.")
            Condition.sleep(1000)
        }
    }

    /**
     * Walks to the sorting location and assigns crew.
     * @return true if successfully arrived at sort location
     */
    private fun walkToSort(): Boolean {
        if (script.atSortLocation) {
            script.logger.info("WALK_SORT: Already at sort location.")
            return true
        }

        script.logger.info("WALK_SORT: Starting walk and assignment.")

        if (!assignBoth()) {
            script.logger.warn("WALK_SORT: Failed to assign crew.")
            return false
        }

        CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
        Condition.sleep(Random.nextInt(Constants.WALKTOSORT_CAMERA_MIN, Constants.WALKTOSORT_CAMERA_MAX))

        script.logger.info("WALK_SORT: Tapping walk point.")

        if (!tapWithSleep(Constants.WALKTOSORT_TAP_X, Constants.WALKTOSORT_TAP_Y, 3, Constants.WALKTOSORT_WALK_MIN, Constants.WALKTOSORT_WALK_MAX)) {
            script.logger.warn("WALK_SORT: Failed to tap.")
            return false
        }

        script.atSortLocation = true
        script.logger.info("WALK_SORT: Arrived at sort location.")
        return true
    }

    /**
     * Assigns crew to both positions.
     * @return true if assignment was successful
     */
    private fun assignBoth(): Boolean {
        script.logger.info("ASSIGNMENTS: Starting 5-tap sequence.")
        val mainWait = setupAssignment(script, Constants.ASSIGNMENT_MAIN_WAIT_MIN, Constants.ASSIGNMENT_MAIN_WAIT_MAX)

        clickWidget(Constants.ROOT_SAILINGTAB,Constants.COMPONENT_SAILINGTAB) //Open tab
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET)}
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET,Constants.INDEX_ASSIGN_SLOT2) //Assign SLot 1
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM)}
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM,Constants.INDEX_ASSIGNCONFIRM_SLOT1) //Confirm
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET)}
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET,Constants.INDEX_ASSIGN_SLOT1) //Assign Cannon
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM)}
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM,Constants.INDEX_ASSIGNCONFIRM_SLOT2) //Confirm
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET)}

        // Reopen and close inventory
        ensureInventoryOpen(Constants.ASSIGNMENT_INV_OPEN_MIN, Constants.ASSIGNMENT_INV_OPEN_MAX)
        Condition.sleep(mainWait)
        closeTabWithSleep(mainWait, mainWait)

        return true
    }
}
