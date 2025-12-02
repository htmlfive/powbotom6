package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Notifications
import org.powbot.api.Random
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.ScrollHelper
import org.powbot.api.rt4.Widgets
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase

class DeployHookTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)
    private val setupTask = SetupSalvagingTask(script)

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
                Notifications.showNotification("HOOK: Both full. Stopping.")

                ScriptManager.stop()
                return false
            }
        } else {
            // In Power Salvage Mode, inventory being full is handled by DropSalvageTask
            script.logger.debug("HOOK: Power Salvage Mode - Skipping deposit logic.")
        }

        CameraSnapper.snapCameraToDirection(script.cameraDirection, script)
        val mainWait = Random.nextInt(Constants.HOOK_MAIN_WAIT_MIN, Constants.HOOK_MAIN_WAIT_MAX)
        Condition.sleep(mainWait)

        script.hookCastMessageFound = false

        // Try tapping hook up to 3 times before giving up
        var messageFound = false
        for (attempt in 1..Random.nextInt(2,3)) {
            script.logger.info("HOOK: Attempt $attempt/3 - Tapping hook")

            if (!clickAtCoordinates(Constants.HOOK_DEPLOY_X, Constants.HOOK_DEPLOY_Y, Constants.HOOK_DEPLOY_MENUOPTION)) {
                script.logger.warn("HOOK: Failed to execute tap on attempt $attempt")
                continue
            }
            script.logger.info("HOOK: Attempt $attempt/3 - Tap executed, waiting for cast message")

            messageFound = Condition.wait({ script.hookCastMessageFound }, 30, 120)

            if (messageFound) {
                script.atWithdrawSpot = true
                script.logger.info("HOOK: Cast message confirmed on attempt $attempt")
                break
            }

            if (Chat.canContinue()) {
                return handleDepletedShipwreck()
            }
            if (attempt < 3) {
                script.logger.warn("HOOK: No confirmation on attempt $attempt. Retrying...")
                Condition.sleep(Random.nextInt(1200, 1800))
            }
        }

        if (messageFound) {
            script.logger.info("HOOK: Success. Waiting for inventory to fill...")
            script.hookingSalvageBool = true
            script.atWithdrawSpot = true
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
                    return handleDepletedShipwreck()
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
                return handleDepletedShipwreck()
            } else {
                script.logger.error("HOOK: No confirmation after 3 attempts. Stopping.")
                Notifications.showNotification("HOOK: No confirmation after 3 attempts. Stopping.")
                ScriptManager.stop()
                return false
            }
        }
    }

    /**
     * Handles depleted shipwreck dialogue and world hopping logic.
     * @return true after handling
     */
    private fun handleDepletedShipwreck(): Boolean {
        script.logger.warn("HOOK: Dialogue detected. Shipwreck depleted.")
        
        // Step 1: Handle dialogues
        val dialoguesHandled = handleMultipleDialogues(2, Constants.SORT_RETAP_MIN, Constants.SORT_RETAP_MAX)
        script.logger.info("DEPLETED: Handled $dialoguesHandled dialogue(s)")
        
        if (script.hopWorlds) {
            script.logger.info("DEPLETED: Starting world hop sequence")
            
            // Step 2: Hop to random world
            hopToRandomWorld(script)
            if (!Condition.wait({ Game.loggedIn() }, 1500, 10)) {
                script.logger.error("DEPLETED: Failed to verify login after world hop")
                return false
            }
            script.logger.info("DEPLETED: World hop successful, logged in confirmed")
            Condition.sleep(Random.nextInt(1200,1800))

            // Step 3: Walk to Hook
            script.logger.info("DEPLETED: Walking to hook location")
            if (!tapWithOffset(Constants.HOP_X, Constants.HOP_Y, 4)) {
                script.logger.warn("DEPLETED: Failed to tap walk-to-hook location")
                return false
            }
            script.logger.info("DEPLETED: Walk tap successful")
            

            // Step 4: Enable tap-to-drop if configured
            if (script.tapToDrop) {
                Game.setMouseActionToggled(true)
                script.logger.info("DEPLETED: Tap-to-drop enabled")
            }
            
            // Step 5: Assign Ghost
            script.logger.info("DEPLETED: Assigning Ghost")
            if (!setupTask.assignGhost()) {
                script.logger.warn("DEPLETED: Failed to assign Ghost after world hop")
                return false
            }
            script.logger.info("DEPLETED: Ghost assignment successful")
            
            // Step 6: Activate Extractor (if enabled)
            if (script.enableExtractor) {
                script.logger.info("DEPLETED: Activating extractor")
                if (clickAtCoordinates(Constants.hopEXTRACTORX, Constants.hopEXTRACTORY, "Harvest", "Activate")) {
                    val waitTime = Random.nextInt(2400, 3000)
                    script.logger.info("DEPLETED: Extractor activated. Waiting $waitTime ms")
                    Condition.sleep(waitTime)
                } else {
                    script.logger.warn("DEPLETED: Failed to activate extractor, continuing anyway")
                }
            }

            // Step 7: Set phase and flags
            script.logger.info("DEPLETED: World hop sequence complete")
            script.currentPhase = SalvagePhase.SALVAGING
            script.hookingSalvageBool = false
            script.atHookLocation = true
            return true
        }
        
        // No world hopping - transition to sorting
        script.logger.info("DEPLETED: Hop worlds disabled. Transitioning to SORTING")
        script.cargoHoldFull = true
        script.currentPhase = SalvagePhase.SETUP_SORTING
        script.hookingSalvageBool = false
        return true
    }

    /**
     * Deposits salvage to the cargo hold (embedded in DeployHookTask for normal mode).
     * @return true if deposit was successful
     */
    private fun depositSalvage(): Boolean {
        CameraSnapper.snapCameraToDirection(script.cameraDirection, script)
        Condition.sleep(Random.nextInt(Constants.DEPOSIT_PRE_WAIT_MIN, Constants.DEPOSIT_PRE_WAIT_MAX))

        val initialSalvageCount = Inventory.stream().name(script.SALVAGE_NAME).count()
        script.logger.info("DEPOSIT: Starting 3-step deposit sequence. Initial salvage count: $initialSalvageCount")

        Condition.sleep(Random.nextInt(Constants.DEPOSIT_BETWEEN_TAPS_MIN, Constants.DEPOSIT_BETWEEN_TAPS_MAX))

        // Step 1: Open cargo interface
        script.logger.info("DEPOSIT: Step 1 - Opening cargo interface")
        if (!clickAtCoordinates(Constants.HOOK_CARGO_OPEN_X, Constants.HOOK_CARGO_OPEN_Y, Constants.HOOK_CARGO_MENUOPTION)) {
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
            script.cargoHoldCount += depositedCount

            // Check if we still have salvage left in inventory after deposit (partial deposit = cargo full)
            if (finalSalvageCount > 0) {
                script.cargoHoldFull = true
                script.cargoHoldCount = script.maxCargoSpace.toInt()
                script.logger.info("DEPOSIT: PARTIAL - Deposited $depositedCount items, but $finalSalvageCount remain. Cargo is FULL. Set count to ${script.maxCargoSpace}.")
            }
            // Flag cargo as full if within 20 of max capacity for earlier transition
            else if (script.cargoHoldCount >= (script.maxCargoSpace.toLong() - 20L)) {
                script.cargoHoldFull = true
                script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount items. Cargo count: ${script.cargoHoldCount}. Near capacity (within 20), flagging as full.")
            } else {
                script.cargoHoldFull = false
                script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount items. Cargo count: ${script.cargoHoldCount}")
            }
            script.logger.info("DEPOSIT: Deposit sequence complete - all steps validated successfully")
            return true
        } else {
            script.cargoHoldFull = true
            script.cargoHoldCount = script.maxCargoSpace.toInt()
            script.logger.warn("DEPOSIT: FAILED - Cargo FULL (no items deposited). Set count to ${script.maxCargoSpace}.")
            return false
        }
    }
}