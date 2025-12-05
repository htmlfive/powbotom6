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

        // if under 55 dont do this
//        if (!assignBoth()) {
//            script.logger.warn("WALK_SORT: Failed to assign crew.")
//            return false
//        }
        snapCameraAndWait(script, Constants.WALKTOSORT_CAMERA_MIN, Constants.WALKTOSORT_CAMERA_MAX)

        script.logger.info("WALK_SORT: Tapping walk point.")
        val stepSuccess = retryAction(Random.nextInt(2,3), 750) {
            clickAtCoordinates(Constants.SORT_WALK_TO_X, Constants.SORT_WALK_TO_Y, Constants.SORT_BUTTON_MENUOPTION)
        }

        if (!stepSuccess) {
            script.logger.warn("WALK: Failed to tap walk point.")
            return false
        }

        Condition.sleep(Random.nextInt(1200,1800))

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

        // Open sailing tab
        if (!openSailingTab(script)) {
            return false
        }

        // Scroll to sorting positions
        script.logger.info("ASSIGNMENTS: Scrolling to sorting positions")
        ScrollHelper.scrollTo(
            getItem = { Widgets.component(937, 25, 47) },
            getPane = { Widgets.component(937, 23) },
            getScrollComp = { Widgets.component(937, 32) }
        )
        script.logger.info("ASSIGNMENTS: Scroll complete")

        // Assign first sorting crew to Slot 2
        if (!assignCrewToSlot(script, Constants.INDEX_ASSIGN_SLOT2, Constants.INDEX_ASSIGNCONFIRM_SLOT1, "Sorter 1")) {
            return false
        }

        // Assign second sorting crew to Slot 1
        if (!assignCrewToSlot(script, Constants.INDEX_ASSIGN_SLOT1, Constants.INDEX_ASSIGNCONFIRM_SLOT2, "Sorter 2")) {
            return false
        }

        script.logger.info("ASSIGNMENTS: Sorting crew assignment sequence complete")
        return true
    }
}
