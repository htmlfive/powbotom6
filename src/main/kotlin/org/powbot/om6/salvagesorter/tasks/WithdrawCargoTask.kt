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

        // Execute withdrawal - returns cooldown time (0L = failed, -1L = cargo depleted, >0L = success)
        val finalCooldownMs = executeWithdrawCargo(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Handle the three possible return values
        when {
            // Case 1: Cargo depleted (got some salvage but inventory not full)
            finalCooldownMs == -1L -> {
                script.logger.info("WITHDRAW: Cargo depleted (inventory not full). Finishing current sorting then switching to salvaging.")

                // Check if we still have salvage to sort
                val hasSalvageInInventory = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()

                if (hasSalvageInInventory) {
                    // Still have salvage to sort - go back to sorting what we have
                    script.logger.info("WITHDRAW: Still have salvage. Going to SORTING_LOOT to finish.")
                    script.currentPhase = SalvagePhase.SORTING_LOOT
                    // Keep cargoHoldFull = true so we stay in sorting loop until inventory is clear
                } else {
                    // No salvage left - transition back to salvaging immediately
                    script.logger.info("WITHDRAW: No salvage left. Transitioning to SETUP_SALVAGING.")
                    script.cargoHoldFull = false
                    script.atSortLocation = false // Reset flag when leaving sorting area
                    script.currentPhase = SalvagePhase.SETUP_SALVAGING
                }

                script.currentWithdrawCooldownMs = 0L
            }

            // Case 2: Normal successful withdrawal (inventory full)
            finalCooldownMs > 0L -> {
                val emptySlotsAfter = Inventory.emptySlotCount()
                val withdrawnCount = emptySlotsBefore - emptySlotsAfter

                // Set cooldown for next withdrawal
                script.currentWithdrawCooldownMs = finalCooldownMs
                script.lastWithdrawOrCleanupTime = System.currentTimeMillis()
                script.logger.info("WITHDRAW: Cooldown set to ${finalCooldownMs / 1000}s.")

                if (withdrawnCount > 0) {
                    script.xpMessageCount = (script.xpMessageCount - withdrawnCount).coerceAtLeast(0)
                    script.logger.info("WITHDRAW: Withdrew $withdrawnCount items. Remaining proxy count: ${script.xpMessageCount}.")
                }
            }

            // Case 3: Complete failure (no salvage obtained at all)
            else -> {
                script.logger.warn("WITHDRAW: Withdrawal failed. Cargo hold is empty.")

                // Check if we still have salvage to sort
                val hasSalvageInInventory = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()

                if (hasSalvageInInventory) {
                    // Still have salvage to sort - stay in sorting mode
                    script.logger.info("WITHDRAW: Still have salvage in inventory. Staying in SORTING mode to finish sorting.")
                    script.cargoHoldFull = true // Keep flag true so we stay in sorting loop
                    script.currentPhase = SalvagePhase.SORTING_LOOT
                } else {
                    // No salvage left - transition back to salvaging
                    script.logger.info("WITHDRAW: No salvage in inventory. Transitioning to SALVAGING mode.")
                    script.cargoHoldFull = false
                    script.atSortLocation = false // Reset flag when leaving sorting area
                    script.currentWithdrawCooldownMs = 0L
                    script.currentPhase = SalvagePhase.SETUP_SALVAGING
                }
            }
        }
    }
}