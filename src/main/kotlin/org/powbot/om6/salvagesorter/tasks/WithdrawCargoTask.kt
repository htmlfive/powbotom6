package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
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
        val finalCooldownMs = executeWithdrawCargo()

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
                    // CRITICAL: Set cargoHoldFull = false so after sorting we transition to salvaging
                    script.cargoHoldFull = false
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

    /**
     * Executes the 4-step cargo withdrawal sequence.
     * @return Cooldown time in ms (0L = failed, -1L = cargo depleted, >0L = success)
     */
    private fun executeWithdrawCargo(): Long {
        CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
        script.logger.info("WITHDRAW: Starting 4-step cargo withdrawal sequence.")

        val invCountBefore = 28 - Inventory.stream().count()
        val salvageCountBefore = Inventory.stream().name(script.SALVAGE_NAME).count()

        // Step 1: Open cargo interface
        script.logger.info("WITHDRAW: Step 1 - Opening cargo interface")
        if (!tapWithSleep(Constants.CARGO_OPEN_X, Constants.CARGO_OPEN_Y, 3, Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX * 2)) {
            script.logger.warn("WITHDRAW: Failed to tap cargo interface")
            return 0L
        }
        script.logger.info("WITHDRAW: Step 1 - Cargo tap successful")

        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_WITHDRAW, Constants.INDEX_FIRST_SLOT) }, 100, 30)) {
            script.logger.warn("WITHDRAW: Withdraw widget did not become visible")
            return 0L
        }
        script.logger.info("WITHDRAW: Step 1 - Withdraw widget confirmed visible")

        // Step 2: Click withdraw button
        script.logger.info("WITHDRAW: Step 2 - Clicking withdraw salvage button")
        if (!clickWidgetWithRetry(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_WITHDRAW, Constants.INDEX_FIRST_SLOT, logPrefix = "WITHDRAW: Step 2", script = script)) {
            script.logger.warn("WITHDRAW: Failed to click withdraw button after retries")
            return 0L
        }
        script.logger.info("WITHDRAW: Step 2 - Withdraw button clicked successfully")

        Condition.sleep(600)

        if (!Condition.wait({ Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty() }, 100, 30)) {
            script.logger.warn("WITHDRAW: No salvage appeared in inventory after withdraw")
            // Don't return yet - continue to close the interface
        } else {
            script.logger.info("WITHDRAW: Step 2 - Salvage confirmed in inventory")
        }

        // Step 3: Close cargo interface
        script.logger.info("WITHDRAW: Step 3 - Closing cargo interface")
        if (!clickWidgetWithRetry(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_CLOSE, Constants.INDEX_CLOSE, logPrefix = "WITHDRAW: Step 3", script = script)) {
            script.logger.warn("WITHDRAW: Failed to click close button after retries")
            return 0L
        }
        script.logger.info("WITHDRAW: Step 3 - Close button clicked successfully")

        Condition.sleep(600)

        if (!Condition.wait({ !isWidgetVisible(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_WITHDRAW, Constants.INDEX_FIRST_SLOT) }, 100, 30)) {
            script.logger.warn("WITHDRAW: Cargo widget did not close properly")
            return 0L
        }
        script.logger.info("WITHDRAW: Step 3 - Cargo widget confirmed closed")

        // Step 4: Walk back to sorting position
        script.logger.info("WITHDRAW: Step 4 - Walking back to sorting position")
        if (!tapWithSleep(Constants.CARGO_WALKBACK_X, Constants.CARGO_WALKBACK_Y, 3, Constants.CARGO_WALKBACK_WAIT_MIN, Constants.CARGO_WALKBACK_WAIT_MAX)) {
            script.logger.warn("WITHDRAW: Failed to tap walk back position")
            return 0L
        }
        script.logger.info("WITHDRAW: Step 4 - Walk back tap successful")

        // Validate withdrawal results
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()

        if (!hasSalvage) {
            script.logger.warn("WITHDRAW: Withdrawal failed - no salvage obtained.")
            script.cargoHoldFull = false
            return 0L
        }

        val invCountAfter = Inventory.stream().count()
        val salvageCountAfter = Inventory.stream().name(script.SALVAGE_NAME).count()
        val inventoryFull = Inventory.isFull()
        val salvageWithdrawn = (salvageCountAfter - salvageCountBefore).toInt()

        if (!inventoryFull) {
            script.logger.warn("WITHDRAW: Inventory not full ($invCountAfter/28). Cargo depleted.")
            script.xpMessageCount = 0
            script.cargoHoldFull = false
            return -1L
        }

        val baseCooldownMs = script.randomWithdrawCooldownMs
        script.xpMessageCount -= salvageWithdrawn // Track actual withdrawal
        script.logger.info("WITHDRAW: Inventory full. Withdrew $salvageWithdrawn items. Cargo count now: ${script.xpMessageCount}. Cooldown: ${baseCooldownMs / 1000}s.")
        script.logger.info("WITHDRAW: Withdrawal sequence complete - all steps validated successfully")
        return baseCooldownMs
    }
}
