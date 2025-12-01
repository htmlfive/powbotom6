package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.CardinalDirection
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class DepositCargoTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Activate when in SALVAGING phase and inventory has salvage
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return script.currentPhase == SalvagePhase.DEPOSITING && hasSalvage
    }

    override fun execute() {
        script.logger.info("DEPOSIT: Starting deposit sequence.")

        // Check for extractor interrupt
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Execute deposit - this will update script.cargoHoldFull
        val success = depositSalvage()

        // Check for extractor interrupt after action
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            script.logger.info("DEPOSIT: Deposit successful. Cargo hold accepting salvage.")
            // cargoHoldFull = false (set by depositSalvage)
            // Continue salvaging
        } else {
            script.logger.warn("DEPOSIT: Deposit failed - Cargo hold is FULL.")
            // cargoHoldFull = true (set by depositSalvage)
            // State machine will switch to SORTING phase
        }
    }

    /**
     * Deposits salvage to the cargo hold.
     * @return true if deposit was successful
     */
    private fun depositSalvage(): Boolean {
        CameraSnapper.snapCameraToDirection(CardinalDirection.South, script)
        Condition.sleep(Random.nextInt(Constants.DEPOSIT_PRE_WAIT_MIN, Constants.DEPOSIT_PRE_WAIT_MAX))

        val initialSalvageCount = Inventory.stream().name(script.SALVAGE_NAME).count()
        script.logger.info("DEPOSIT: Starting 3-step deposit sequence. Initial salvage count: $initialSalvageCount")

        // Step 1: Open cargo interface
        script.logger.info("DEPOSIT: Step 1 - Opening cargo interface")
        if (!tapWithSleep(Constants.HOOK_CARGO_OPEN_X, Constants.HOOK_CARGO_OPEN_Y, 3, Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX)) {
            script.logger.warn("DEPOSIT: Failed to tap cargo interface")
            return false
        }
        script.logger.info("DEPOSIT: Step 1 - Cargo tap successful")

        if (!Condition.wait({ isWidgetVisible(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_DEPOSIT_SALVAGE) }, 100, 30)) {
            script.logger.warn("DEPOSIT: Deposit widget did not become visible")
            return false
        }
        script.logger.info("DEPOSIT: Step 1 - Deposit widget confirmed visible")

        // Step 2: Click deposit button
        script.logger.info("DEPOSIT: Step 2 - Clicking deposit salvage button")
        if (!clickWidgetWithRetry(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_DEPOSIT_SALVAGE, logPrefix = "DEPOSIT: Step 2", script = script)) {
            script.logger.warn("DEPOSIT: Failed to click deposit button after retries")
            return false
        }
        script.logger.info("DEPOSIT: Step 2 - Deposit button clicked successfully")

        Condition.sleep(Random.nextInt(Constants.WIDGET_INTERACTION_MIN, Constants.WIDGET_INTERACTION_MAX))

        // Step 3: Close cargo interface
        script.logger.info("DEPOSIT: Step 3 - Closing cargo interface")
        if (!clickWidgetWithRetry(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_CLOSE, Constants.INDEX_CLOSE, logPrefix = "DEPOSIT: Step 3", script = script)) {
            script.logger.warn("DEPOSIT: Failed to click close button after retries")
            return false
        }
        script.logger.info("DEPOSIT: Step 3 - Close button clicked successfully")

        Condition.sleep(600)

        if (!Condition.wait({ !isWidgetVisible(Constants.ROOT_CARGO_WIDGET, Constants.COMPONENT_WITHDRAW, Constants.INDEX_FIRST_SLOT) }, 100, 30)) {
            script.logger.warn("DEPOSIT: Cargo widget did not close properly")
            return false
        }
        script.logger.info("DEPOSIT: Step 3 - Cargo widget confirmed closed")

        val finalSalvageCount = Inventory.stream().name(script.SALVAGE_NAME).count()
        val depositedCount = (initialSalvageCount - finalSalvageCount).toInt()

        if (finalSalvageCount < initialSalvageCount) {
            script.xpMessageCount += depositedCount

            // Flag cargo as full if within 30 of max capacity for earlier transition
            if (script.xpMessageCount >= (script.maxCargoSpace.toLong() - 30L)) {
                script.cargoHoldFull = true
                script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount items. Cargo count: ${script.xpMessageCount}. Near capacity (within 30), flagging as full.")
            } else {
                script.cargoHoldFull = false
                script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount items. Cargo count: ${script.xpMessageCount}")
            }
            script.logger.info("DEPOSIT: Deposit sequence complete - all steps validated successfully")
            return true
        } else {
            script.cargoHoldFull = true
            script.xpMessageCount = script.maxCargoSpace.toInt()
            script.logger.warn("DEPOSIT: FAILED - Cargo FULL (no items deposited). Set count to ${script.maxCargoSpace}.")
            return false
        }
    }
}
