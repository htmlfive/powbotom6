package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class DepositCargoTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Activate when in SALVAGING phase and inventory has salvage
        val hasSalvage = Inventory.stream().name(script.salvageName).isNotEmpty()
        return script.currentPhase == SalvagePhase.DEPOSITING && hasSalvage
    }

    override fun execute() {
        script.logger.info("DEPOSIT: Starting deposit sequence.")

        // Check for extractor interrupt
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Execute deposit - this will update script.cargoHoldFull
        val success = depositSalvageToCargoHold(script)

        // Check for extractor interrupt after action
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            script.logger.info("DEPOSIT: Deposit successful. Cargo hold accepting salvage.")
            // cargoHoldFull = false (set by utility function)
            // Continue salvaging
        } else {
            script.logger.warn("DEPOSIT: Deposit failed - Cargo hold is FULL.")
            // cargoHoldFull = true (set by utility function)
            // State machine will switch to SORTING phase
        }
    }
}
