package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class SetupSalvagingTask(script: SalvageSorter) : Task(script) {
    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.SETUP_SALVAGING
    }

    override fun execute() {
        script.logger.info("SETUP: Entering Salvaging mode.")

        // CRITICAL: Reset the sort location flag when entering salvaging mode
        script.atSortLocation = false
        script.logger.info("SETUP: Reset atSortLocation flag to false.")

        // Walk to salvaging spot (this will also set atHookLocation = true)
        val success = walkToHook()

        if (success) {
            script.logger.info("SETUP: Setup complete. Transitioning to SALVAGING.")
            script.atHookLocation = true // Ensure flag is set
            script.currentPhase = SalvagePhase.SALVAGING
        } else {
            script.logger.warn("SETUP: Setup failed. Will retry.")
            Condition.sleep(1000)
        }
    }

    /**
     * Walks to the hook location and assigns crew if needed.
     * @return true if successfully arrived at hook location
     */
    private fun walkToHook(): Boolean {
        if (script.atHookLocation) {
            script.logger.info("WALK: Already at hook location. Skipping movement and assignment.")
            return true
        }

        script.logger.info("WALK: Not at hook location yet. Starting walk and assignment sequence.")

        // Skip Ghost assignment in Power Salvage Mode
        if (!script.powerSalvageMode) {
            if (!assignGhost()) {
                script.logger.warn("WALK: Failed to assign Ghost.")
                return false
            }
        } else {
            script.logger.info("WALK: Power Salvage Mode - Skipping Ghost assignment.")
        }

        val waitTime = Random.nextInt(Constants.WALK_WAIT_MIN, Constants.WALK_WAIT_MAX)
        Condition.sleep(waitTime)
        CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

        script.logger.info("WALK: Tapping walk-to-hook.")

        if (!tapWithSleep(Constants.HOOK_SALVAGE_6_X, Constants.HOOK_SALVAGE_6_Y, 3, waitTime, waitTime)) {
            script.logger.warn("WALK: Failed to tap walk point.")
            return false
        }

        script.atHookLocation = true
        script.logger.info("WALK: Arrived at hook location. Flag set to true.")
        return true
    }

    /**
     * Assigns crew to Ghost position.
     * @return true if assignment was successful
     */
    private fun assignGhost(): Boolean {
        script.logger.info("ASSIGNMENTS: Starting 3-tap ghost sequence.")
        val mainWait = setupAssignment(script, Constants.ASSIGNMENT_MAIN_WAIT_MIN, Constants.ASSIGNMENT_MAIN_WAIT_MAX)
        // Open tab
        clickWidget(Constants.ROOT_SAILINGTAB,Constants.COMPONENT_SAILINGTAB) //Open tab
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET)}
        //Click
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET,Constants.INDEX_ASSIGN_SLOT1) //Assign SLot 1
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM)}
        //Click
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM,Constants.INDEX_ASSIGNCONFIRM_SLOT1) //Confirm
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET)}
        //Click
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET,Constants.INDEX_ASSIGN_CANNON) //Assign Cannon
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM)}
        //Click
        clickWidget(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGETCONFIRM,Constants.INDEX_ASSIGNCONFIRM_SLOT2) //Confirm
        Condition.sleep(Random.nextInt(600,900))
        Condition.wait{isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET,Constants.COMPONENT_ASSIGN_WIDGET)}


//        // Reopen and close inventory
//        ensureInventoryOpen(Constants.ASSIGNMENT_INV_OPEN_MIN, Constants.ASSIGNMENT_INV_OPEN_MAX)
//        Condition.sleep(mainWait)
//        closeTabWithSleep(mainWait, mainWait)

        script.logger.info("ASSIGNMENTS: Ghost complete.")
        return true
    }
}
