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
        val hasSalvage = Inventory.stream().name(script.salvageName).isNotEmpty()
        val hasJunk = Inventory.stream().name(*script.getDiscardOrAlchList()).isNotEmpty()

        if (hasSalvage || hasJunk) return false

        // Don't activate if inventory is full
        if (Inventory.isFull()) return false

        script.logger.debug("WITHDRAW CHECK: Clean=${!hasSalvage && !hasJunk}, EmptySlots=${Inventory.emptySlotCount()}")

        return script.cargoHoldFull
    }

    override fun execute() {
        script.logger.info("WITHDRAW: Starting withdrawal sequence.")
        script.atWithdrawSpot = true
        script.logger.info("WITHDRAW: Set atWithdrawSpot = true")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Execute withdrawal - returns status (0 = failed, -1 = cargo depleted, 1 = success)
        val withdrawStatus = executeWithdrawCargo()

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Handle the three possible return values
        when (withdrawStatus) {
            // Case 1: Cargo depleted (got some salvage but inventory not full)
            -1 -> {
                script.logger.info("WITHDRAW: Cargo depleted (inventory not full). Finishing current sorting then switching to salvaging.")

                // Check if we still have salvage to sort
                val hasSalvageInInventory = Inventory.stream().name(script.salvageName).isNotEmpty()

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
                    script.atWithdrawSpot = false
                    script.currentPhase = SalvagePhase.SETUP_SALVAGING
                }
            }

            // Case 2: Normal successful withdrawal (inventory full)
            1 -> {
                script.logger.info("WITHDRAW: Withdrawal successful. Cargo count updated from widget.")
                //script.atWithdrawSpot = false
                script.logger.info("WITHDRAW: Set atWithdrawSpot = false (walk back complete)")
            }

            // Case 3: Complete failure (no salvage obtained at all)
            else -> {
                script.logger.warn("WITHDRAW: Withdrawal failed. Cargo hold is empty.")

                // Check if we still have salvage to sort
                val hasSalvageInInventory = Inventory.stream().name(script.salvageName).isNotEmpty()

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
                    //script.atWithdrawSpot = false
                    script.currentPhase = SalvagePhase.SETUP_SALVAGING
                }
            }
        }
    }

    /**
     * Executes the 4-step cargo withdrawal sequence.
     * @return Status (0 = failed, -1 = cargo depleted, 1 = success)
     */
    private fun executeWithdrawCargo(): Int {
        val maxRetries = 3
        val delayMs = 750

        snapCameraAndWait(script)
        script.logger.info("WITHDRAW: Starting 4-step cargo withdrawal sequence.")

        // Step 1: Open cargo interface
        if (!openCargoInterfaceAtSort(script)) {
            return 0
        }

        // Step 2: Click withdraw button
        script.logger.info("WITHDRAW: Step 2 - Clicking withdraw salvage button")
        if (!clickWidgetWithRetry(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_WITHDRAW, Constants.INDEX_FIRST_SLOT, logPrefix = "WITHDRAW: Step 2", script = script)) {
            script.logger.warn("WITHDRAW: Failed to click withdraw button after retries")
            return 0
        }
        script.logger.info("WITHDRAW: Step 2 - Withdraw button clicked successfully")

        Condition.sleep(600)

        if (!Condition.wait({ Inventory.stream().name(script.salvageName).isNotEmpty() }, 600,6)) {
            script.logger.warn("WITHDRAW: No salvage appeared in inventory after withdraw")
            // Don't return yet - continue to close the interface
        } else {
            script.logger.info("WITHDRAW: Step 2 - Salvage confirmed in inventory")
        }

        // Read the actual cargo count from widget before closing
        val actualCargoCount = WidgetUtils.getNumber(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_CARGO_SPACE)
        script.logger.info("WITHDRAW: Read cargo count from widget: $actualCargoCount")

        // Step 3: Close cargo interface
        if (!closeCargoInterface(script)) {
            return 0
        }

        // Step 4: Walk back to sorting position
        script.logger.info("WITHDRAW: Step 4 - Walking back to sorting position")
        val step4Success = retryAction(maxRetries, 750) { // Use a slightly longer delay for movement
            clickAtCoordinates(Constants.CARGO_WALKBACK_X, Constants.CARGO_WALKBACK_Y, Constants.CARGO_WALKBACK_MENUOPTION)
        }

        if (!step4Success) {
            script.logger.warn("WITHDRAW: Failed to tap walk back position after retries")
            return 0
        }
        script.logger.info("WITHDRAW: Step 4 - Walk back tap successful")
        Condition.sleep(Random.nextInt(1400,1800))

        // Validate withdrawal results
        val hasSalvage = Inventory.stream().name(script.salvageName).isNotEmpty()

        if (!hasSalvage) {
            script.logger.warn("WITHDRAW: Withdrawal failed - no salvage obtained.")
            script.cargoHoldCount = actualCargoCount // Update to actual value
            script.cargoHoldFull = actualCargoCount == 0
            return 0
        }

        val inventoryFull = Inventory.isFull()

        if (!inventoryFull) {
            script.logger.warn("WITHDRAW: Inventory not full (${Inventory.stream().count()}/28). Cargo depleted.")
            script.cargoHoldCount = actualCargoCount // Update to actual value
            script.cargoHoldFull = actualCargoCount == 0
            return -1
        }

        script.cargoHoldCount = actualCargoCount // Update to actual value from widget
        script.logger.info("WITHDRAW: Inventory full. Cargo count (from widget): ${script.cargoHoldCount}.")
        script.logger.info("WITHDRAW: Withdrawal sequence complete - all steps validated successfully")
        return 1
    }
}