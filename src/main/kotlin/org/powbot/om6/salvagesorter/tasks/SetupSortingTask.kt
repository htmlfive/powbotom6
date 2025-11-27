// ========================================
// SetupSortingTask.kt
// ========================================
package org.powbot.om6.salvagesorter.tasks

import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.api.Condition

class SetupSortingTask(script: SalvageSorter) : Task(script) {
    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.SETUP_SORTING
    }

    override fun execute() {
        script.logger.info("SETUP: Entering Sorting mode.")
        script.atHookLocation = false
        // Assign crew and move to sorting position
        val success = assignBoth(script)

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
}