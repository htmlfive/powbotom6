package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.SalvagePhase

class SetupSortingTask(script: SalvageSorter) : Task(script) {
    override fun activate(): Boolean = script.currentPhase == SalvagePhase.SETUP_SORTING

    override fun execute() {
        script.logger.info("SETUP: Entering Sorting mode.")
        // 1. Assign crewmate (Placeholder)
        script.logger.info("ACTION: Assigning crewmate (Placeholder).")

        // 2. Set camera to Sorting position (Placeholder)
        script.logger.info("ACTION: Setting camera to Sorting position (Placeholder).")

        // 3. Walk to spot (Placeholder)
        script.logger.info("ACTION: Walking to sorting spot (Placeholder).")
    }
}