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

        // CRITICAL: Reset the hook location flag when entering sorting mode
        script.atHookLocation = false
        script.logger.info("SETUP: Reset atHookLocation flag to false for next salvaging phase.")

        // Walk to sorting position and assign crew
        val success = walkToSort(script)

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