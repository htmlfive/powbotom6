package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.LootConfig
import org.powbot.om6.salvagesorter.config.SalvagePhase

class WithdrawCargoTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Only activate when in WITHDRAWING phase
        if (script.currentPhase != SalvagePhase.WITHDRAWING) return false

        // Don't activate if we have salvage or junk in inventory
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        val hasJunk = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()

        if (hasSalvage || hasJunk) return false

        // Don't activate if inventory is full
        if (Inventory.isFull()) return false

        // Check cooldown
        val timeElapsedSinceLastWithdraw = System.currentTimeMillis() - script.lastWithdrawOrCleanupTime
        val cooldownExpired = timeElapsedSinceLastWithdraw >= script.currentWithdrawCooldownMs

        script.logger.debug("WITHDRAW CHECK: Clean=${!hasSalvage && !hasJunk}, EmptySlots=${Inventory.emptySlotCount()}, CooldownExpired=$cooldownExpired")

        return cooldownExpired && script.cargoHoldFull
    }

    override fun execute() {
        script.logger.info("WITHDRAW: Starting withdrawal sequence.")

        val emptySlotsBefore = Inventory.emptySlotCount()
        script.logger.info("WITHDRAW: Empty slots before: $emptySlotsBefore.")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Execute withdrawal - returns cooldown time (0L if failed)
        val finalCooldownMs = executeWithdrawCargo(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (finalCooldownMs > 0L) {
            val emptySlotsAfter = Inventory.emptySlotCount()
            val withdrawnCount = emptySlotsBefore - emptySlotsAfter

            // Set cooldown for next withdrawal
            script.currentWithdrawCooldownMs = finalCooldownMs
            script.lastWithdrawOrCleanupTime = System.currentTimeMillis()
            script.logger.info("WITHDRAW: Cooldown set to ${finalCooldownMs / 1000}s.")

            if (withdrawnCount > 0) {
                script.xpMessageCount = (script.xpMessageCount - withdrawnCount).coerceAtLeast(0)
                script.logger.info("WITHDRAW: Withdrew $withdrawnCount items. Remaining proxy count: ${script.xpMessageCount}.")
            } else {
                script.logger.warn("WITHDRAW: No items withdrawn. Cargo may be empty.")
            }
        } else {
            // Withdrawal failed - cargo must be empty, transition back to salvaging
            script.logger.warn("WITHDRAW: Withdrawal failed. Cargo hold is empty. Transitioning to SALVAGING mode.")
            script.cargoHoldFull = false // Cargo is now empty, switch to salvaging
            script.currentWithdrawCooldownMs = 0L

            // Transition to SETUP_SALVAGING to walk back to hook and start salvaging again
            script.currentPhase = SalvagePhase.SETUP_SALVAGING
            script.logger.info("WITHDRAW: Set phase to SETUP_SALVAGING to return to salvaging.")
        }
    }
}