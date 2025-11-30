package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Inventory
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.CardinalDirection
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class DeployHookTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // In Power Salvage mode, only activate when:
        // - We're in SALVAGING phase
        // - Inventory is NOT full (when full, DropSalvageTask takes over)
        if (script.powerSalvageMode) {
            val inventoryFull = Inventory.isFull()
            val shouldActivate = script.currentPhase == SalvagePhase.SALVAGING && !inventoryFull

            if (script.currentPhase == SalvagePhase.SALVAGING || shouldActivate) {
                script.logger.info("DEPLOY ACTIVATE CHECK (POWER MODE): Phase=${script.currentPhase.name}, invFull=$inventoryFull, salvageMessageFound=${script.salvageMessageFound}, RESULT=$shouldActivate")
            }

            return shouldActivate
        }

        // Normal mode: Only activate in SALVAGING phase when:
        // 1. Cargo is not full (otherwise we should be in sorting mode)
        // 2. Inventory is not full (when full, DepositCargoTask takes over)

        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        val inventoryFull = Inventory.isFull()

        // KEY CHANGE: We should keep deploying hook even if we have some salvage,
        // as long as inventory isn't full yet
        val shouldActivate = script.currentPhase == SalvagePhase.SALVAGING &&
                !script.cargoHoldFull &&
                !inventoryFull

        // ALWAYS log when in SALVAGING phase to debug activation issues
        if (script.currentPhase == SalvagePhase.SALVAGING || shouldActivate) {
            script.logger.info("DEPLOY ACTIVATE CHECK: Phase=${script.currentPhase.name}, hasSalvage=$hasSalvage, cargoFull=${script.cargoHoldFull}, invFull=$inventoryFull, salvageMessageFound=${script.salvageMessageFound}, RESULT=$shouldActivate")
        }

        return shouldActivate
    }

    override fun execute() {
        script.logger.info("DEPLOY: Starting hook deployment sequence.")

        // Check for extractor interrupt before action
        if (extractorTask.checkAndExecuteInterrupt(script)) {
            script.logger.info("DEPLOY: Extractor interrupted before hook action. Task will retry next poll.")
            return
        }

        // NEW: Check if salvage completion message was detected - reset flag and re-hook
        if (script.salvageMessageFound) {
            script.logger.info("DEPLOY: Salvage completion message detected! Resetting flag and re-hooking immediately.")
            script.salvageMessageFound = false
            script.hookingSalvageBool = false // Ensure hook flag is reset
        }

        // Execute hook action (walkToHook should have been called by SetupSalvagingTask)
        val success = hookSalvage()

        // Check for extractor interrupt after action
        if (extractorTask.checkAndExecuteInterrupt(script)) {
            script.logger.info("DEPLOY: Extractor interrupted after hook action. Task will retry next poll.")
            return
        }

        if (success) {
            script.logger.info("DEPLOY: Hook deployed successfully. Waiting for salvage to appear in inventory.")
            // Stay in SALVAGING phase - wait for salvage to arrive in inventory
            // The poll loop will keep calling this task until salvage appears
        } else {
            script.logger.warn("DEPLOY: Hook deployment failed (dialogue/error/interrupt). Ensuring phase is SALVAGING for immediate retry.")
            // CRITICAL: Explicitly set phase to SALVAGING to ensure retry
            script.currentPhase = SalvagePhase.SALVAGING
            Condition.sleep(200) // Very small delay before retry
        }
    }

    /**
     * Executes the hook salvage action.
     * @return true if hook was deployed successfully
     */
    private fun hookSalvage(): Boolean {
        // Skip deposit logic in Power Salvage Mode - DropSalvageTask handles it
        if (!script.powerSalvageMode) {
            if (Inventory.isFull() && !script.cargoHoldFull) {
                script.logger.info("HOOK: Inventory full. Depositing first.")
                return depositSalvage()
            }

            if (Inventory.isFull() && script.cargoHoldFull) {
                script.logger.error("HOOK: Both full. Stopping.")
                ScriptManager.stop()
                return false
            }
        } else {
            // In Power Salvage Mode, inventory being full is handled by DropSalvageTask
            script.logger.debug("HOOK: Power Salvage Mode - Skipping deposit logic.")
        }

        CameraSnapper.snapCameraToDirection(CardinalDirection.South, script)
        val mainWait = Random.nextInt(Constants.HOOK_MAIN_WAIT_MIN, Constants.HOOK_MAIN_WAIT_MAX)
        Condition.sleep(mainWait)

        script.hookCastMessageFound = false

        // Try tapping hook up to 3 times before giving up
        var messageFound = false
        for (attempt in 1..3) {
            script.logger.info("HOOK: Tapping hook (Attempt $attempt/3).")
            tapWithOffset(Constants.HOOK_SALVAGE_1_X, Constants.HOOK_SALVAGE_1_Y, 3)

            messageFound = Condition.wait({ script.hookCastMessageFound }, 30, 120)

            if (messageFound) {
                script.logger.info("HOOK: Cast message confirmed on attempt $attempt.")
                break
            }

            if (attempt < 3) {
                script.logger.warn("HOOK: No confirmation on attempt $attempt. Retrying...")
                Condition.sleep(Random.nextInt(1200, 1800))
            }
        }

        if (messageFound) {
            script.logger.info("HOOK: Success. Waiting for inventory to fill...")
            script.hookingSalvageBool = true
            script.salvageMessageFound = false

            while (!Inventory.isFull()) {
                // Check for salvage completion message - if found, break loop to re-hook
                if (script.salvageMessageFound) {
                    script.logger.info("HOOK: Salvage completion message detected during wait! Breaking loop to re-hook.")
                    script.salvageMessageFound = false
                    script.hookingSalvageBool = false
                    return false
                }

                if (Chat.canContinue()) {
                    script.logger.warn("HOOK: Dialogue detected. Shipwreck depleted.")
                    handleMultipleDialogues(2, Constants.SORT_RETAP_MIN, Constants.SORT_RETAP_MAX)
                    script.logger.info("HOOK: Transitioning to SORTING.")
                    script.cargoHoldFull = true
                    script.currentPhase = SalvagePhase.SETUP_SORTING
                    script.hookingSalvageBool = false
                    return true
                }

                if (extractorTask.checkAndExecuteInterrupt(script)) {
                    script.logger.info("HOOK: Extractor interrupted. Re-hooking.")
                    script.hookingSalvageBool = false
                    return false
                }

                Condition.sleep(Random.nextInt(Constants.HOOK_WAIT_LOOP_MIN, Constants.HOOK_WAIT_LOOP_MAX))
            }

            script.hookingSalvageBool = false
            script.logger.info("HOOK: Inventory full.")
            return true
        } else {
            script.hookingSalvageBool = false

            if (Chat.canContinue()) {
                script.logger.warn("HOOK: Dialogue before confirmation.")
                handleMultipleDialogues(2, Constants.SORT_RETAP_MIN, Constants.SORT_RETAP_MAX)
                script.logger.info("HOOK: Transitioning to SORTING.")
                script.cargoHoldFull = true
                script.currentPhase = SalvagePhase.SETUP_SORTING
                return true
            } else {
                script.logger.error("HOOK: No confirmation. Stopping.")
                ScriptManager.stop()
                return false
            }
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

        Condition.sleep(Random.nextInt(Constants.DEPOSIT_BETWEEN_TAPS_MIN, Constants.DEPOSIT_BETWEEN_TAPS_MAX))


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
            if (script.xpMessageCount >= (script.maxCargoSpace.toLong() - 20L)) {
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
