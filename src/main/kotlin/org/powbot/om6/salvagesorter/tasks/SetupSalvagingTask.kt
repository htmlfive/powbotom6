package org.powbot.om6.salvagesorter.tasks

import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.api.Condition

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
        val success = walkToHook(script)

        if (success) {
            script.logger.info("SETUP: Setup complete. Transitioning to SALVAGING.")
            script.atHookLocation = true // Ensure flag is set
            script.currentPhase = SalvagePhase.SALVAGING
        } else {
            script.logger.warn("SETUP: Setup failed. Will retry.")
            Condition.sleep(1000)
        }
    }
}