// ========================================
// SortSalvageTask.kt
// ========================================
package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase

class SortSalvageTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Activate when in SORTING phase and we have salvage to sort
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return script.currentPhase == SalvagePhase.SORTING_LOOT && hasSalvage && script.cargoHoldFull
    }

    override fun execute() {
        script.logger.info("SORT: Starting sort sequence.")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeTapSortSalvage(script, script.SALVAGE_NAME)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            script.logger.info("SORT: Sort completed successfully.")
            // Inventory should now be empty of salvage
            // Will trigger withdrawal next poll
        } else {
            script.logger.warn("SORT: Sort failed or timed out.")
        }
    }
}
