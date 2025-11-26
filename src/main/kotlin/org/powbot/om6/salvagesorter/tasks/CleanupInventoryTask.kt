package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.LootConfig
import org.powbot.om6.salvagesorter.config.SalvagePhase

class CleanupInventoryTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        if (hasSalvage) return false

        val hasCleanupLoot = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()
        return hasCleanupLoot
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.CLEANING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 3).")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeCleanupLoot(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        script.currentPhase = if (success) SalvagePhase.IDLE else SalvagePhase.CLEANING
        script.logger.info("PHASE: Cleanup complete/failed. Transitioned to ${script.currentPhase.name}.")



        if (success) {

            script.currentPhase = SalvagePhase.IDLE
        } else {
            script.currentPhase = SalvagePhase.CLEANING
        }
    }
}