package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorteraio.SalvageSorter
import org.powbot.om6.salvagesorteraio.config.LootConfig
import org.powbot.om6.salvagesorteraio.config.SalvagePhase

class WithdrawCargoTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        if (script.currentPhase != SalvagePhase.SORTING_LOOT) return false

        // Must not have raw salvage or junk to clean up first
        val hasJunk = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty() ||
                Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()

        if (hasJunk) return false

        // If inventory is not full and it's clean, we can try to withdraw
        if (Inventory.isFull()) return false

        return true
    }

    override fun execute() {
        script.logger.info("ACTION: Starting Cargo Withdrawal sequence (Priority in loop).")

        val emptySlotsBefore = Inventory.emptySlotCount()

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // MODIFIED: executeWithdrawCargo returns 1L on success (salvage found) or 0L on failure
        val successIndicator = executeWithdrawCargo(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (successIndicator > 0L) {
            val withdrawnCount = emptySlotsBefore - Inventory.emptySlotCount()

            // Update internal count and log success
            script.xpMessageCount = (script.xpMessageCount - withdrawnCount).coerceAtLeast(0)
            script.logger.info("CARGO: Successfully withdrew items. Items withdrawn: $withdrawnCount. Remaining cargo proxy: ${script.xpMessageCount}.")
        } else {
            script.logger.warn("CARGO: Withdrawal sequence failed. Check error logs.")
        }
    }
}