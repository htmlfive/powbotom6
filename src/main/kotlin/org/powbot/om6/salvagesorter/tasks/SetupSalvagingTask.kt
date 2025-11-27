// ========================================
// SetupSalvagingTask.kt
// ========================================
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

        // Walk to salvaging spot
        val success = walkToHook(script)

        if (success) {
            script.logger.info("SETUP: Setup complete. Transitioning to SALVAGING.")
            script.currentPhase = SalvagePhase.SALVAGING
        } else {
            script.logger.warn("SETUP: Setup failed. Will retry.")
            Condition.sleep(1000)
        }
    }
}
