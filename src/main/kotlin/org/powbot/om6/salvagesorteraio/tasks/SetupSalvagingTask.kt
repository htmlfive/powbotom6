package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.SalvagePhase

class SetupSalvagingTask(script: SalvageSorter) : Task(script) {
    override fun activate(): Boolean = script.currentPhase == SalvagePhase.SETUP_SALVAGING

    override fun execute() {
        script.logger.info("SETUP: Entering Salvaging mode.")
        // 1. Unassign crewmate (Placeholder)
        script.logger.info("ACTION: Unassigning crewmate (Placeholder).")

        // 2. Set camera to Salvaging position (Placeholder)
        script.logger.info("ACTION: Setting camera to Salvaging position (Placeholder).")

        // 3. Walk to spot (Placeholder)
        script.logger.info("ACTION: Walking to salvage spot (Placeholder).")
    }
}