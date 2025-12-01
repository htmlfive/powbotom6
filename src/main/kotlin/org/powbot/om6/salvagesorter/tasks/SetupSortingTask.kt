package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.ScrollHelper
import org.powbot.api.rt4.Widgets
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
        CameraSnapper.snapCameraToDirection(script.cameraDirection, script)
        Condition.sleep(Random.nextInt(Constants.WALKTOSORT_CAMERA_MIN, Constants.WALKTOSORT_CAMERA_MAX))

        script.logger.info("WALK_SORT: Tapping walk point.")

        if (!clickAtCoordinates(Constants.SORT_WALK_TO_X, Constants.SORT_WALK_TO_Y, Constants.SORT_BUTTON_MENUOPTION)) {
            script.logger.warn("WALK_SORT: Failed to tap.")
            return false
        }

        script.atSortLocation = true
        script.logger.info("WALK_SORT: Arrived at sort location.")
        return true
    }

    /**
     * Assigns crew to both sorting positions.
     * @return true if assignment was successful
     */
    private fun assignBoth(): Boolean {
        script.logger.info("ASSIGNMENTS: Starting Sorting crew assignment sequence.")

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
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 1 - Assign widget confirmed visible")

        // Step 2: Scroll to sorting positions
        script.logger.info("ASSIGNMENTS: Step 2 - Scrolling to sorting positions")
        ScrollHelper.scrollTo(
            item = Widgets.component(937, 25, 47),
            pane = Widgets.component(937, 23),
            scrollComp = Widgets.component(937, 32),
        )
        script.logger.info("ASSIGNMENTS: Step 2 - Scroll complete")

        // Step 3: Click first sorting slot (Slot 2)
        script.logger.info("ASSIGNMENTS: Step 3 - Clicking first sorting slot (Slot 2)")
        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET, Constants.INDEX_ASSIGN_SLOT2, logPrefix = "ASSIGNMENTS: Step 3", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click first sorting slot after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 3 - First sorting slot clicked successfully")
        
        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))
        
        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Confirm widget did not become visible after clicking first sorting slot")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 3 - Confirm widget confirmed visible")

        // Step 4: Confirm first sorting assignment
        script.logger.info("ASSIGNMENTS: Step 4 - Confirming first sorting assignment")
        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM, Constants.INDEX_ASSIGNCONFIRM_SLOT1, logPrefix = "ASSIGNMENTS: Step 4", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click first sorting confirm after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 4 - First sorting assignment confirmed successfully")
        
        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))
        
        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Assign widget did not reappear after first sorting confirmation")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 4 - Assign widget reappeared")

        // Step 5: Click second sorting slot (Slot 1)
        script.logger.info("ASSIGNMENTS: Step 5 - Clicking second sorting slot (Slot 1)")
        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET, Constants.INDEX_ASSIGN_SLOT1, logPrefix = "ASSIGNMENTS: Step 5", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click second sorting slot after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 5 - Second sorting slot clicked successfully")
        
        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))
        
        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Confirm widget did not become visible after clicking second sorting slot")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 5 - Confirm widget confirmed visible")

        // Step 6: Confirm second sorting assignment
        script.logger.info("ASSIGNMENTS: Step 6 - Confirming second sorting assignment")
        if (!clickWidgetWithRetry(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGETCONFIRM, Constants.INDEX_ASSIGNCONFIRM_SLOT2, logPrefix = "ASSIGNMENTS: Step 6", script = script)) {
            script.logger.warn("ASSIGNMENTS: Failed to click second sorting confirm after retries")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 6 - Second sorting assignment confirmed successfully")
        
        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))
        
        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_ASSIGN_WIDGET, Constants.COMPONENT_ASSIGN_WIDGET) }, 100, 30)) {
            script.logger.warn("ASSIGNMENTS: Assign widget did not reappear after second sorting confirmation")
            return false
        }
        script.logger.info("ASSIGNMENTS: Step 6 - Assign widget reappeared")

        script.logger.info("ASSIGNMENTS: Sorting crew assignment sequence complete - all steps validated successfully")
        return true
    }
}
