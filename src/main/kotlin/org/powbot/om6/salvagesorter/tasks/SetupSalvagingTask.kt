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

        val waitTime = Random.nextInt(Constants.WALK_WAIT_MIN, Constants.WALK_WAIT_MAX)
        Condition.sleep(waitTime)
        CameraSnapper.snapCameraToDirection(script.cameraDirection, script)

        script.logger.info("WALK: Tapping walk-to-hook.")
        val stepSuccess = retryAction(Random.nextInt(2,3), 750) { // Use a slightly longer delay for movement
            clickAtCoordinates(Constants.HOOK_WALK_TO_X, Constants.HOOK_WALK_TO_Y, Constants.HOOK_DEPLOY_MENUOPTION)
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

        // Step 1: Open Sailing Tab
        script.logger.info("ASSIGNMENTS: Step 1 - Opening sailing tab")
        if (!clickWidgetWithRetry(Constants.ROOT_SAILINGTAB, Constants.COMPONENT_SAILINGTAB, logPrefix = "ASSIGNMENTS: Step 1", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click sailing tab after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 1 - Sailing tab clicked successfully")

        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))


        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Assign widget did not become visible after opening tab")
            clickWidget(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETBACKBUTTON, Constants.INDEX_ASSIGNCONFIRM_BACKBUTTON)
            Condition.sleep(600)
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 1 - Assign widget confirmed visible")

        // Step 2: Scroll to Ghost position
        script.logger.info("ASSIGNMENTS: Step 2 - Scrolling to Ghost position")
        ScrollHelper.scrollTo(
            getItem = { Widgets.component(937, 25, 47) },
            getPane = { Widgets.component(937, 23) },
            getScrollComp = { Widgets.component(937, 32) }
        )
        script.logger.info("ASSIGNMENTS: Step 2 - Scroll complete")

        // Step 3: Click Ghost slot (Slot 1)
        script.logger.info("ASSIGNMENTS: Step 3 - Clicking Ghost slot (Slot 1)")
        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET, Constants.INDEX_ASSIGN_SLOT1, logPrefix = "ASSIGNMENTS: Step 3", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click Ghost slot after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 3 - Ghost slot clicked successfully")

        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))

        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Confirm widget did not become visible after clicking Ghost slot")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 3 - Confirm widget confirmed visible")

        // Step 4: Confirm Ghost assignment
        script.logger.info("ASSIGNMENTS: Step 4 - Confirming Ghost assignment")
        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM, Constants.INDEX_ASSIGNCONFIRM_SLOT1, logPrefix = "ASSIGNMENTS: Step 4", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click Ghost confirm after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 4 - Ghost assignment confirmed successfully")

        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))

        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Assign widget did not reappear after Ghost confirmation")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 4 - Assign widget reappeared")
//
//        // Step 5: Click Cannon slot
//        script.logger.info("ASSIGNMENTS: Step 5 - Clicking Cannon slot")
//        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET, Constants.INDEX_ASSIGN_CANNON, logPrefix = "ASSIGNMENTS: Step 5", script = script)) {
//            script.logger.warn("ASSIGNMENTS: Failed to click Cannon slot after retries")
//            return false
//        }
//        script.logger.info("ASSIGNMENTS: Step 5 - Cannon slot clicked successfully")
//
//        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))
//
//        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM) }, 100, 30)) {
//            script.logger.warn("ASSIGNMENTS: Confirm widget did not become visible after clicking Cannon slot")
//            return false
//        }
//        script.logger.info("ASSIGNMENTS: Step 5 - Confirm widget confirmed visible")
//
//        // Step 6: Confirm Cannon assignment
//        script.logger.info("ASSIGNMENTS: Step 6 - Confirming Cannon assignment")
//        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM, Constants.INDEX_ASSIGNCONFIRM_SLOT2, logPrefix = "ASSIGNMENTS: Step 6", script = script)) {
//            script.logger.warn("ASSIGNMENTS: Failed to click Cannon confirm after retries")
//            return false
//        }
//        script.logger.info("ASSIGNMENTS: Step 6 - Cannon assignment confirmed successfully")
//
//        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))
//
//        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET) }, 100, 30)) {
//            script.logger.warn("ASSIGNMENTS: Assign widget did not reappear after Cannon confirmation")
//            return false
//        }
//        script.logger.info("ASSIGNMENTS: Step 6 - Assign widget reappeared")

        script.logger.info("ASSIGNMENTS: Ghost assignment sequence complete - all steps validated successfully")
        return true
    }
}