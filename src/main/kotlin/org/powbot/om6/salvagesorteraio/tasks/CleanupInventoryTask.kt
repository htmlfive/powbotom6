package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.LootConfig
import org.powbot.om6.salvagesorteraio.config.SalvagePhase

class CleanupInventoryTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        if (script.currentPhase != SalvagePhase.SORTING_LOOT) return false
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        if (hasSalvage) return false

        val hasCleanupLoot = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()
        return hasCleanupLoot
    }

    override fun execute() {
        // Phase transition handled by poll() now.

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        executeCleanupLoot(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Phase transition handled by poll() now.
    }
}