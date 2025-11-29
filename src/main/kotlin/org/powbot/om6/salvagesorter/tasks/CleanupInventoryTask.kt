package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.LootConfig
import org.powbot.om6.salvagesorter.config.SalvagePhase

class CleanupInventoryTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()

        // In Power Salvage mode, NEVER clean up salvage here (DropSalvageTask handles it)
        // Only clean up other junk items
        if (script.powerSalvageMode) {
            if (hasSalvage) return false

            val hasJunk = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()
            return hasJunk
        }

        // Normal mode: Don't activate if we still have salvage to process
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

        // FIX: Always transition to SALVAGING upon success (when not sorting)
        // This ensures the script returns to the primary task loop (DeployHookTask)
        script.currentPhase = if (success) SalvagePhase.SALVAGING else SalvagePhase.CLEANING

        script.logger.info("PHASE: Cleanup complete/failed. Transitioned to ${script.currentPhase.name}.")
    }

}