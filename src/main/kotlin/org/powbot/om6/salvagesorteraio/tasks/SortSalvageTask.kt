package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.SalvagePhase

class SortSalvageTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        if (script.currentPhase != SalvagePhase.SORTING_LOOT) return false
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return hasSalvage
    }

    override fun execute() {
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // FIXED: Passing extractorTask instance
       // executeTapSortSalvage(script, script.SALVAGE_NAME, extractorTask)

        if (extractorTask.checkAndExecuteInterrupt(script)) return
    }
}