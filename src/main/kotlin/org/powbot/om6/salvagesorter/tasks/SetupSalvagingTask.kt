package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.ScrollHelper
import org.powbot.api.rt4.Widgets
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class SetupSalvagingTask(script: SalvageSorter) : Task(script) {
    private val cleanupTask = CleanupInventoryTask(script)

    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.SETUP_SALVAGING
    }

    override fun execute() {
        script.logger.info("SETUP: Entering Salvaging mode.")

        // CRITICAL: Reset the sort location flag when entering salvaging mode
        script.atSortLocation = false
        script.logger.info("SETUP: Reset atSortLocation flag to false.")

        // Clean up inventory before walking to hook
        if (cleanupTask.activate()) {
            script.logger.info("SETUP: Cleaning up inventory before moving to hook.")
            cleanupTask.execute()
        }

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

        snapCameraAndWait(script, Constants.WALK_WAIT_MIN, Constants.WALK_WAIT_MAX)

        script.logger.info("WALK: Tapping walk-to-hook.")
        val stepSuccess = if (!script.powerSalvageMode) {
            retryAction(Random.nextInt(2,3), 750) {
                clickAtCoordinates(Constants.HOOK_WALK_TO_X, Constants.HOOK_WALK_TO_Y, Constants.HOOK_DEPLOY_MENUOPTION)
            }
        } else {
            CameraSnapper.snapCameraToDirection(script.cameraDirection, script)
            Condition.sleep(Random.nextInt(180,300))
            script.logger.info("WALK: Walking to hook location (Power Mode)")
            
            val tap1 = tapWithOffset(Constants.HOP_X, Constants.HOP_Y, 3)
            if (!tap1) {
                script.logger.warn("WALK: Failed first tap")
            }
            
            Condition.sleep(Random.nextInt(80,120))
            
            val tap2 = tapWithOffset(Constants.HOP_X, Constants.HOP_Y, 3)
            if (!tap2) {
                script.logger.warn("WALK: Failed second tap")
            }
            
            tap1 && tap2
        }
        
        if (!stepSuccess) {
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
    fun assignGhost(): Boolean {
        script.logger.info("ASSIGNMENTS: Starting Ghost assignment sequence.")

        // Open sailing tab
        if (!openSailingTab(script)) {
            return false
        }

        // Scroll to Ghost position
        script.logger.info("ASSIGNMENTS: Scrolling to Ghost position")
        ScrollHelper.scrollTo(
            getItem = { Widgets.component(937, 25, 47) },
            getPane = { Widgets.component(937, 23) },
            getScrollComp = { Widgets.component(937, 32) }
        )
        script.logger.info("ASSIGNMENTS: Scroll complete")

        // Assign Ghost to Slot 1
        if (!assignCrewToSlot(script, Constants.INDEX_ASSIGN_SLOT1, Constants.INDEX_ASSIGNCONFIRM_SLOT1, "Ghost")) {
            return false
        }

        script.logger.info("ASSIGNMENTS: Ghost assignment sequence complete")
        return true
    }
}