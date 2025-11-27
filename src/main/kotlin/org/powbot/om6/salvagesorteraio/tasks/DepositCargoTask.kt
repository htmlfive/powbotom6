package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.SalvagePhase
import org.powbot.api.rt4.Inventory
import org.powbot.api.Condition

class DepositCargoTask(script: SalvageSorter) : Task(script) {
    // Runs in SALVAGING phase if raw salvage is in inventory.
    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return script.currentPhase == SalvagePhase.SALVAGING && hasSalvage
    }

    override fun execute() {
        script.logger.info("ACTION: Depositing in cargo (Empty Task - Placeholder).")
        Condition.sleep(1000) // Placeholder sleep

        // Transition to Sorting: If salvage is still present after an assumed deposit attempt (proxy check).
        val hasFullSalvageInv = Inventory.stream().name(script.SALVAGE_NAME).count() >= 27 // Assumes inventory full check
        if (hasFullSalvageInv) {
            script.currentPhase = SalvagePhase.SETUP_SORTING
            script.logger.info("DEPOSIT: All salvage not deposited (inventory full). Transitioning to ${SalvagePhase.SETUP_SORTING.name}.")
        }
    }
}