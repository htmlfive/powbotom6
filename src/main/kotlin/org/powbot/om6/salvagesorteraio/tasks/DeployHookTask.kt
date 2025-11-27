package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.SalvagePhase
import org.powbot.api.rt4.Inventory
import org.powbot.api.Condition

class DeployHookTask(script: SalvageSorter) : Task(script) {
    // Runs in SALVAGING phase if no raw salvage is currently in inventory.
    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return script.currentPhase == SalvagePhase.SALVAGING && !hasSalvage
    }

    override fun execute() {
        script.logger.info("ACTION: Deploying hook (Empty Task - Placeholder).")
        Condition.sleep(1000) // Placeholder sleep
    }
}