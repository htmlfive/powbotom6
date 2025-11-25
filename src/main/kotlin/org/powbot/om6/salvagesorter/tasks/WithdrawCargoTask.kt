package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.LootConfig
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.om6.salvagesorter.tasks.Task

class WithdrawCargoTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasJunk = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty() ||
                Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()

        if (hasJunk) return false

        if (Inventory.isFull()) return false

        val emptySlots = Inventory.emptySlotCount()

        val hasSufficientCargo = script.xpMessageCount >= emptySlots

        script.logger.debug("WITHDRAW CHECK: Clean=$!hasJunk, EmptySlots=$emptySlots, CargoProxy=${script.xpMessageCount}, SufficientCargo=$hasSufficientCargo")

        return hasSufficientCargo
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.WITHDRAWING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 4).")

        val emptySlotsBefore = Inventory.emptySlotCount()
        script.logger.info("CARGO: Empty slots before withdrawal: $emptySlotsBefore.")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeWithdrawCargo(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            val emptySlotsAfter = Inventory.emptySlotCount()
            val withdrawnCount = emptySlotsBefore - emptySlotsAfter

            if (withdrawnCount > 0) {
                script.xpMessageCount = (script.xpMessageCount - withdrawnCount).coerceAtLeast(0)
                script.logger.info("CARGO: Withdrew $withdrawnCount items. Remaining cargo proxy: ${script.xpMessageCount}.")
            } else {
                script.logger.warn("CARGO: Withdrawal was successful, but no items were added (withdrawnCount=$withdrawnCount). Cargo count proxy untouched.")
            }

            script.currentPhase = SalvagePhase.IDLE
        } else {
            script.logger.warn("PHASE: Withdraw failed. Retrying in next cycle.")
            script.currentPhase = SalvagePhase.WITHDRAWING
        }
        script.logger.info("PHASE: Withdraw complete/failed. Transitioned to ${script.currentPhase.name}.")
    }
}