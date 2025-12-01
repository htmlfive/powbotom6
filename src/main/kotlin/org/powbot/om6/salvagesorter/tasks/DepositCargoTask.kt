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
        script.logger.info("DEPOSIT: Initial count: $initialSalvageCount")

        // Tap 1: Open
        tapWithSleep(Constants.HOOK_SALVAGE_2_X, Constants.HOOK_SALVAGE_2_Y,3,600,900)
        Condition.wait{isWidgetVisible(Constants.ROOT_CARGO_WIDGET,Constants.COMPONENT_DEPOSIT_SALVAGE)}

        // Tap 2: Deposit
        clickWidget(Constants.ROOT_CARGO_WIDGET,Constants.COMPONENT_DEPOSIT_SALVAGE)
        Condition.sleep(Random.nextInt(600,900))

        // Tap 3: Close
        clickWidget(Constants.ROOT_CARGO_WIDGET,Constants.COMPONENT_CLOSE,Constants.INDEX_CLOSE)
        Condition.sleep(600)
        Condition.wait{!isWidgetVisible(Constants.ROOT_CARGO_WIDGET,Constants.COMPONENT_WITHDRAW,Constants.INDEX_FIRST_SLOT)}

        val finalSalvageCount = Inventory.stream().name(script.SALVAGE_NAME).count()
        val depositedCount = (initialSalvageCount - finalSalvageCount).toInt()

        if (finalSalvageCount < initialSalvageCount) {
            script.xpMessageCount += depositedCount

            // Flag cargo as full if within 20 of max capacity for earlier transition
            if (script.xpMessageCount >= (script.maxCargoSpace.toLong() - 30L)) {
                script.cargoHoldFull = true
                script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount. Cargo count: ${script.xpMessageCount}. Near capacity (within 20), flagging as full.")
            } else {
                script.cargoHoldFull = false
                script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount. Cargo count: ${script.xpMessageCount}")
            }
            return true
        } else {
            script.cargoHoldFull = true
            script.xpMessageCount = script.maxCargoSpace.toInt()
            script.logger.warn("DEPOSIT: FAILED - Cargo FULL. Set count to ${script.maxCargoSpace}.")
            return false
        }
    }
}
