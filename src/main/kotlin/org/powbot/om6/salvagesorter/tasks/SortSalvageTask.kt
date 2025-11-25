package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.om6.salvagesorter.tasks.Task

class SortSalvageTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return hasSalvage
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.SORTING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 2).")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeTapSortSalvage(script, script.SALVAGE_NAME)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        script.currentPhase = if (success) SalvagePhase.IDLE else SalvagePhase.SORTING
        script.logger.info("PHASE: Sort complete/failed. Transitioned to ${script.currentPhase.name}.")
    }
}