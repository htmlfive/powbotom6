package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.CardinalDirection
import org.powbot.om6.salvagesorter.config.LootConfig
import org.powbot.om6.salvagesorter.config.SalvagePhase
import kotlin.random.Random as KotlinRandom

// --- X,Y Tap Coordinates and Constants ---

// executeWithdrawCargo (4-Tap Sequence)
private const val CARGO_TAP_1_X = 577 // Open/Interact
private const val CARGO_TAP_1_Y = 161
private const val CARGO_TAP_2_X = 143 // Withdraw
private const val CARGO_TAP_2_Y = 237
private const val CARGO_TAP_3_X = 571 // Close
private const val CARGO_TAP_3_Y = 159
private const val CARGO_TAP_4_X = 432 // Walk back
private const val CARGO_TAP_4_Y = 490

// executeAssign
private const val ASSIGN_BOTH_1_X = 818 // OPEN TAB
private const val ASSIGN_BOTH_1_Y = 394
private const val ASSIGN_BOTH_2_X = 747 // FIRST ASSIGN
private const val ASSIGN_BOTH_2_Y = 435
private const val ASSIGN_BOTH_3_X = 690 // SIAD
private const val ASSIGN_BOTH_3_Y = 403
private const val ASSIGN_BOTH_4_X = 747 // SECOND ASSIGN
private const val ASSIGN_BOTH_4_Y = 469
private const val ASSIGN_BOTH_5_X = 684 // GHOST
private const val ASSIGN_BOTH_5_Y = 370

// executeTapSortSalvage (Sort Button)
private const val SORT_BUTTON_X = 574
private const val SORT_BUTTON_Y = 359
private const val SORT_BUTTON_TOLERANCEX = 10
private const val SORT_BUTTON_TOLERANCEY = 10

// hookSalvage and depositSalvage
private const val HOOK_SALVAGE_1_X = 524 // HOOK
private const val HOOK_SALVAGE_1_Y = 340
private const val HOOK_SALVAGE_2_X = 385 // DEPOSIT STEP 1
private const val HOOK_SALVAGE_2_Y = 365
private const val HOOK_SALVAGE_3_X = 551 // DEPOSIT STEP 2
private const val HOOK_SALVAGE_3_Y = 308
private const val HOOK_SALVAGE_4_X = 570 // DEPOSIT STEP 3
private const val HOOK_SALVAGE_4_Y = 165
private const val HOOK_SALVAGE_6_X = 791 // walk to hook
private const val HOOK_SALVAGE_6_Y = 63

// ----------------------------------------

// Helper function to add slight randomness to the tap location
fun getRandomOffsetSmall() = Random.nextInt(-3, 3)
fun getRandomOffsetLarge() = Random.nextInt(-3, 3)

// ========================================
// CLEANUP FUNCTIONS
// ========================================

fun executeCleanupLoot(script: SalvageSorter): Boolean {
    var successfullyCleaned = false
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    val highAlchSpell = Magic.Spell.HIGH_ALCHEMY
    script.logger.info("CLEANUP: Starting alching loop (High Priority Cleanup).")

    LootConfig.ALCH_LIST.forEach { itemName ->
        val item = Inventory.stream().name(itemName).firstOrNull()
        if (item != null && item.valid()) {
            script.logger.info("CLEANUP: Attempting High Alch on $itemName with custom tab logic.")
            successfullyCleaned = true

            if (highAlchSpell.cast("Cast") && Condition.wait({Game.tab() == Game.Tab.INVENTORY}, 125, 12)) {
                if(item.interact("Cast")) {
                    Condition.wait({ Game.tab() == Game.Tab.MAGIC }, 125, 12 )
                    script.logger.info("CLEANUP: Alch successful. Sleeping for animation/cooldown: 3000-3600ms.")
                    Condition.sleep(Random.nextInt(3000, 3600))
                } else {
                    script.logger.warn("CLEANUP: Failed to click item $itemName.")
                }
            } else {
                script.logger.warn("CLEANUP: Failed to select High Alch spell or failed tab check.")
                return successfullyCleaned
            }
        }
    }

    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully for dropping.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }

    val droppableItems = Inventory.stream()
        .filter { item -> item.valid() && item.name() in LootConfig.DROP_LIST }
        .toList()

    val shuffledDroppableItems = droppableItems.shuffled(KotlinRandom)

    script.logger.info("CLEANUP: Starting random item drop process. Items found: ${shuffledDroppableItems.size}")

    shuffledDroppableItems.forEach { itemToDrop ->
        val itemName = itemToDrop.name()

        if (itemToDrop.valid()) {
            script.logger.info("CLEANUP: Attempting to randomly drop $itemName (Slot: ${itemToDrop.inventoryIndex()}).")
            successfullyCleaned = true

            if (itemToDrop.click()) {
                Condition.wait({ Inventory.stream().name(itemName).none { it.inventoryIndex() == itemToDrop.inventoryIndex() } }, 300, 5)
            }
        }
    }

    if (successfullyCleaned) {
        Condition.sleep(Random.nextInt(800, 1500))
    }

    return successfullyCleaned
}

// ========================================
// CARGO WITHDRAW FUNCTIONS
// ========================================

fun executeWithdrawCargo(script: SalvageSorter): Long {
    val mainWait = Random.nextInt(600, 900)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    script.logger.info("CARGO: Starting 4-tap cargo withdrawal sequence.")

    if (!Input.tap(CARGO_TAP_1_X + getRandomOffsetLarge(), CARGO_TAP_1_Y + getRandomOffsetLarge())) return 0L
    script.logger.info("CARGO TAP 1 (Open): Tapped. Waiting for interaction.")
    Condition.sleep(Random.nextInt(1800, 2400))

    if (!Input.tap(CARGO_TAP_2_X + getRandomOffsetLarge(), CARGO_TAP_2_Y + getRandomOffsetLarge())) return 0L
    script.logger.info("CARGO TAP 2 (Withdraw): Tapped. Waiting $mainWait ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(CARGO_TAP_3_X + getRandomOffsetLarge(), CARGO_TAP_3_Y + getRandomOffsetLarge())) return 0L
    script.logger.info("CARGO TAP 3 (Close): Tapped. Waiting $mainWait ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(CARGO_TAP_4_X + getRandomOffsetLarge(), CARGO_TAP_4_Y + getRandomOffsetLarge())) return 0L
    script.logger.info("CARGO TAP 4 (Walk back): Tapped. Waiting for walk back.")
    Condition.sleep(Random.nextInt(1800, 2400))

    val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
    if (!hasSalvage) {
        script.logger.warn("CARGO: Withdrawal failed - no salvage item (${script.SALVAGE_NAME}) in inventory after withdrawal.")
        // Cargo hold must be empty now
        script.cargoHoldFull = false
        return 0L
    }

    val baseCooldownMs = script.randomWithdrawCooldownMs
    val occupiedSlots = Inventory.stream().count()
    val emptySlots = 28 - occupiedSlots
    val penaltyPerSlotMs = 20000L
    val penaltyMs = emptySlots * penaltyPerSlotMs
    val finalCooldownMs = baseCooldownMs + penaltyMs

    script.logger.info("COOLDOWN ADJUST: Base: ${baseCooldownMs / 1000}s. Empty Slots: $emptySlots. Penalty: ${penaltyMs / 1000}s. Final Cooldown: ${finalCooldownMs / 1000}s.")

    return finalCooldownMs
}
//Walk to sort
// Add this to TaskExecution.kt

fun walkToSort(script: SalvageSorter): Boolean {
    // Check if we're already at the sort location
    if (script.atSortLocation) {
        script.logger.info("WALK_SORT: Already at sort location. Skipping movement and assignment.")
        return true
    }

    script.logger.info("WALK_SORT: Not at sort location yet. Starting walk and assignment sequence.")

    // 1. Setup camera
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    Condition.sleep(Random.nextInt(600, 1200))

    // 2. Walk back to sorting area
    val x = 580 + getRandomOffsetLarge()
    val y = 482 + getRandomOffsetLarge()

    script.logger.info("WALK_SORT: Tapping walk-to-sort point at ($x, $y).")

    if (!Input.tap(x, y)) {
        script.logger.warn("WALK_SORT: Failed to tap walk-to-sort point.")
        return false
    }

    script.logger.info("WALK_SORT: Tapped. Waiting for walk back.")
    Condition.sleep(Random.nextInt(1800, 2400))

    // 3. Assign both crew members
    if (!assignBoth(script)) {
        script.logger.warn("WALK_SORT: Failed to assign crew.")
        return false
    }

    // 4. Set the flag to indicate we're now at the sort location
    script.atSortLocation = true
    script.logger.info("WALK_SORT: Arrived at sort location. Flag set to true.")

    return true
}
// ========================================
// SORTING FUNCTIONS
// ========================================

fun executeTapSortSalvage(script: SalvageSorter, salvageItemName: String): Boolean {
    CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
    Condition.sleep(Random.nextInt(500, 800))

    val randomOffsetX = Random.nextInt(-SORT_BUTTON_TOLERANCEX, SORT_BUTTON_TOLERANCEX + 13)
    val randomOffsetY = Random.nextInt(-SORT_BUTTON_TOLERANCEY, SORT_BUTTON_TOLERANCEY + 1)
    val finalX = SORT_BUTTON_X + randomOffsetX
    val finalY = SORT_BUTTON_Y + randomOffsetY

    val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully for sorting.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab. Aborting sort sequence.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }

    Condition.sleep(Random.nextInt(600, 1200))
    script.logger.info("ACTION: Tapping 'Sort Salvage' button at X=$finalX, Y=$finalY. Count before: $salvageCountBefore.")
    Game.closeOpenTab()
    Condition.sleep(Random.nextInt(600, 1200))

    if (Input.tap(finalX, finalY)) {
        val checkInterval = 2400
        val initialWaitTime = 7200L
        var elapsed = 0L
        var currentSalvageCount = salvageCountBefore
        var lastSalvageCount = salvageCountBefore

        var retapFailureCount = 0
        val MAX_RETAP_FAILURES = 2

        script.logger.info("RETAP: Starting $initialWaitTime ms check for active sorting (2400ms check interval).")

        while (elapsed < initialWaitTime) {
            Condition.sleep(checkInterval)
            elapsed += checkInterval

            currentSalvageCount = Inventory.stream().name(salvageItemName).count()

            if (currentSalvageCount < salvageCountBefore) {
                script.logger.info("RETAP: Sort started successfully. Items removed: ${salvageCountBefore - currentSalvageCount}.")
                break
            }

            if (Chat.canContinue()) {
                Chat.clickContinue()
                Condition.sleep(Random.nextInt(500, 800))
            }

            if (currentSalvageCount >= lastSalvageCount) {
                retapFailureCount++

                if (retapFailureCount > MAX_RETAP_FAILURES) {
                    script.logger.error("FATAL ERROR: Sort stalled after $MAX_RETAP_FAILURES retap attempts. Stopping script.")
                    ScriptManager.stop()
                    return true
                }

                script.logger.warn("RETAP: Count (${currentSalvageCount}) has not decreased. Retapping Sort Salvage (Attempt $retapFailureCount).")

                if (Input.tap(finalX, finalY)) {
                    Condition.sleep(Random.nextInt(500, 800))
                    lastSalvageCount = currentSalvageCount
                } else {
                    script.logger.warn("RETAP FAILED: Could not retap. Proceeding to next check.")
                }
            } else {
                retapFailureCount = 0
                lastSalvageCount = currentSalvageCount
            }
        }

        val timeoutTicks = 20
        val mainCheckInterval = 1800
        var waitSuccess = currentSalvageCount.toInt() == 0
        var attempts = 0
        val POST_INTERRUPT_WAIT = 600

        if (!waitSuccess) {
            script.logger.info("POLLING: Starting non-blocking wait for remaining inventory clear.")

            while (attempts < timeoutTicks) {
                if (script.extractorTask.checkAndExecuteInterrupt(script)) {
                    script.logger.warn("INTERRUPT: Extractor ran. Re-tapping Sort Salvage and restarting wait loop.")

                    if (Input.tap(finalX, finalY)) {
                        script.logger.info("RETAP SUCCESS: Re-tapped Sort Salvage after interrupt. Waiting ${POST_INTERRUPT_WAIT}ms.")
                        Condition.sleep(POST_INTERRUPT_WAIT)
                        attempts = 0
                        continue
                    } else {
                        script.logger.warn("RETAP FAILED: Could not re-tap after interrupt. Continuing check loop.")
                    }
                }

                if (Inventory.stream().name(salvageItemName).isEmpty()) {
                    waitSuccess = true
                    script.logger.info("POLLING: Salvage inventory cleared successfully.")
                    break
                }

                Condition.sleep(mainCheckInterval)
                attempts++
                script.logger.debug("POLLING: Attempt $attempts of $timeoutTicks. Salvage still present.")
            }
        }

        if (waitSuccess) {
            script.logger.info("SUCCESS: 'Sort Salvage' complete. Extended AFK wait: 5000-8000ms.")
            Condition.sleep(Random.nextInt(5000, 8000))
            return true
        } else {
            script.logger.warn("FAIL: 'Sort Salvage' wait timed out. Salvage still present.")
            return false
        }
    }
    return false
}

// ========================================
// ASSIGNMENT FUNCTIONS
// ========================================

fun assignBoth(script: SalvageSorter): Boolean {
    val mainWait = Random.nextInt(900, 1200)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    script.logger.info("ASSIGNMENTS: Starting 5-tap assignment sequence.")

    // Open and close inventory to clear game tab
    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }
    Condition.sleep(mainWait)
    Game.closeOpenTab()
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_1_X + getRandomOffsetLarge(), ASSIGN_BOTH_1_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 1 (Open Sailing Tab).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 1 (Open Sailing Tab) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_2_X + getRandomOffsetLarge(), ASSIGN_BOTH_2_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 2 (First Assign).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 2 (First Assign) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_3_X + getRandomOffsetLarge(), ASSIGN_BOTH_3_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 3 (SIAD).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 3 (SIAD) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_4_X + getRandomOffsetLarge(), ASSIGN_BOTH_4_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 4 (Second Assign).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 4 (Second Assign) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_5_X + getRandomOffsetLarge(), ASSIGN_BOTH_5_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 5 (Ghost).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 5 (Ghost) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    script.logger.info("ASSIGNMENTS: Assignment sequence complete.")
    return true
}

fun assignGhost(script: SalvageSorter): Boolean {
    val mainWait = Random.nextInt(900, 1200)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    script.logger.info("ASSIGNMENTS: Starting 3-tap ghost assignment sequence.")

    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }
    Condition.sleep(mainWait)
    Game.closeOpenTab()
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_1_X + getRandomOffsetLarge(), ASSIGN_BOTH_1_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 1 (Open Tab).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 1 (Open Tab) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_2_X + getRandomOffsetLarge(), ASSIGN_BOTH_2_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 2 (First Assign).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 2 (First Assign) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Input.tap(ASSIGN_BOTH_5_X + getRandomOffsetLarge(), ASSIGN_BOTH_5_Y + getRandomOffsetLarge())) {
        script.logger.warn("ASSIGNMENTS: Failed Tap 3 (Ghost).")
        return false
    }
    script.logger.info("ASSIGNMENTS: Tap 3 (Ghost) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }
    Condition.sleep(mainWait)
    Game.closeOpenTab()
    Condition.sleep(mainWait)

    script.logger.info("ASSIGNMENTS: Ghost assignment sequence complete.")
    return true
}

// ========================================
// SALVAGING FUNCTIONS (Hook & Deposit)
// ========================================

fun walkToHook(script: SalvageSorter): Boolean {
    // Check if we're already at the hook location
    if (script.atHookLocation) {
        script.logger.info("WALK: Already at hook location. Skipping movement and assignment.")
        return true
    }

    script.logger.info("WALK: Not at hook location yet. Starting walk and assignment sequence.")

    // 1. Assign Ghost ONCE before walking
    if (!assignGhost(script)) {
        script.logger.warn("WALK: Failed to assign Ghost.")
        return false
    }

    val waitTime = Random.nextInt(1800, 2400)

    // 2. Wait and Camera Setup
    Condition.sleep(waitTime)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

    // 3. Perform Tap to Walk to Hook
    val x1 = HOOK_SALVAGE_6_X + getRandomOffsetSmall()
    val y1 = HOOK_SALVAGE_6_Y + getRandomOffsetSmall()

    script.logger.info("WALK: Tapping Walk-to-Hook point at ($x1, $y1).")

    if (!Input.tap(x1, y1)) {
        script.logger.warn("WALK: Failed to tap walk-to-hook point.")
        return false
    }

    // Wait for the walking action to complete
    Condition.sleep(waitTime)

    // 4. Set the flag to indicate we're now at the hook location
    script.atHookLocation = true
    script.logger.info("WALK: Arrived at hook location. Flag set to true.")

    return true
}

fun hookSalvage(script: SalvageSorter): Boolean {
    // 1. Check if we need to deposit first
    if (Inventory.isFull() && !script.cargoHoldFull) {
        script.logger.info("HOOK: Inventory full. Attempting deposit before hooking.")
        return depositSalvage(script)
    }

    // 2. Check if both inventory AND cargo are full (cannot proceed)
    if (Inventory.isFull() && script.cargoHoldFull) {
        script.logger.error("HOOK: Both inventory and cargo are full. Cannot proceed. Stopping.")
        ScriptManager.stop()
        return false
    }

    // 3. Setup camera for hook action
    CameraSnapper.snapCameraToDirection(CardinalDirection.South, script)

    val mainWait = Random.nextInt(900, 1200)
    Condition.sleep(mainWait)

    // 4. Reset message flag before tap
    script.hookCastMessageFound = false

    // 5. Perform hook tap
    val x = HOOK_SALVAGE_1_X + getRandomOffsetSmall()
    val y = HOOK_SALVAGE_1_Y + getRandomOffsetSmall()

    script.logger.info("HOOK: Tapping Hook Salvage at ($x, $y).")
    Input.tap(x, y)

    // 6. Wait for confirmation message (up to 1.8 seconds)
    val messageFound = Condition.wait({ script.hookCastMessageFound }, 30, 120)

    // 7. Handle result
    if (messageFound) {
        script.logger.info("HOOK: Success - action start message received.")

        // NEW LOGIC: Set flag - we are now in the active 'hooking' state
        script.hookingSalvageBool = true

        // Instantiate the extractor task for interrupt checking during the wait
        val extractorTask = CrystalExtractorTask(script)

        // Reset the completion flag before waiting
        script.salvageMessageFound = false

        // Wait until salvage message is found, inventory is full, OR extractor interrupts
        script.logger.info("HOOK: Waiting for salvage complete message, inventory to fill, or dialogue.")
        while (!script.salvageMessageFound && !Inventory.isFull()) {

            // CRITICAL: Check for canContinue FIRST (shipwreck depleted dialogue)
            if (Chat.canContinue()) {
                script.logger.warn("HOOK: Dialogue detected (Chat.canContinue()). Shipwreck likely depleted.")

                // Click through the dialogue
                Chat.clickContinue()
                Condition.sleep(Random.nextInt(500, 800))

                // Check again in case there are multiple dialogue boxes
                if (Chat.canContinue()) {
                    Chat.clickContinue()
                    Condition.sleep(Random.nextInt(500, 800))
                }

                // TRANSITION TO SORTING PHASE
                script.logger.info("HOOK: Transitioning to SORTING phase (shipwreck depleted).")
                script.cargoHoldFull = true // Signal that we need to sort now
                script.currentPhase = SalvagePhase.SETUP_SORTING
                script.hookingSalvageBool = false

                // Return true since we handled the situation
                return true
            }

            // Crystal Harvester Interrupt Check (tap and reset timer if active)
            if (extractorTask.checkAndExecuteInterrupt(script)) {
                script.logger.info("HOOK: Extractor interrupt executed during hook wait. Returning false to force a re-hook.")

                // Reset flag on interrupt
                script.hookingSalvageBool = false

                // Returning false breaks the hook action and the script will retry DeployHookTask
                return false
            }

            Condition.sleep(Random.nextInt(1000, 3000))
        }

        // Reset flag after wait is complete
        script.hookingSalvageBool = false

        script.logger.info("HOOK: Wait condition met (Salvage complete or Inventory full). Returning success.")
        return true
    } else {
        // Ensure flag is reset in the failure path too (though it shouldn't be set yet)
        script.hookingSalvageBool = false

        if (Chat.canContinue()) {
            // Dialogue appeared before hook even started - could be shipwreck depleted
            script.logger.warn("HOOK: Dialogue appeared before hook confirmation. Checking if shipwreck depleted.")

            // Click through dialogue
            Chat.clickContinue()
            Condition.sleep(Random.nextInt(500, 800))

            if (Chat.canContinue()) {
                Chat.clickContinue()
                Condition.sleep(Random.nextInt(500, 800))
            }

            // TRANSITION TO SORTING PHASE
            script.logger.info("HOOK: Transitioning to SORTING phase (dialogue before hook start).")
            script.cargoHoldFull = true
            script.currentPhase = SalvagePhase.SETUP_SORTING

            return true
        } else {
            // No message and no dialogue - critical failure
            script.logger.error("HOOK: No confirmation message received and no dialogue found. Stopping.")
            ScriptManager.stop()
            return false
        }
    }
}

fun depositSalvage(script: SalvageSorter): Boolean {
    val waitTime = Random.nextInt(400, 600)
    CameraSnapper.snapCameraToDirection(CardinalDirection.South, script)
    Condition.sleep(Random.nextInt(700, 1100))
    // 1. Get initial inventory count before deposit attempt
    val initialSalvageCount = Inventory.stream()
        .name(script.SALVAGE_NAME)
        .count()
    script.logger.info("DEPOSIT: Initial salvage count: $initialSalvageCount")

    Condition.sleep(Random.nextInt(1200, 1800))
    // 2. Tap DEPOSIT STEP 1
    val x1 = HOOK_SALVAGE_2_X + getRandomOffsetSmall()
    val y1 = HOOK_SALVAGE_2_Y + getRandomOffsetSmall()
    script.logger.info("DEPOSIT: Tapping Deposit Step 1 at ($x1, $y1).")
    Input.tap(x1, y1)
    Condition.sleep(Random.nextInt(1200, 1800))

    // 3. Tap DEPOSIT STEP 2
    val x2 = HOOK_SALVAGE_3_X + getRandomOffsetSmall()
    val y2 = HOOK_SALVAGE_3_Y + getRandomOffsetSmall()
    script.logger.info("DEPOSIT: Tapping Deposit Step 2 at ($x2, $y2).")
    Input.tap(x2, y2)
    Condition.sleep(Random.nextInt(1200, 1800))


    // 3. Tap DEPOSIT STEP 3
    val x3 = HOOK_SALVAGE_4_X + getRandomOffsetSmall()
    val y3 = HOOK_SALVAGE_4_Y + getRandomOffsetSmall()
    script.logger.info("DEPOSIT: Tapping Deposit Step 2 at ($x3, $y3).")
    Input.tap(x3, y3)
    Condition.sleep(Random.nextInt(1200, 1800))


    // 4. Get final inventory count after deposit attempt
    val finalSalvageCount = Inventory.stream()
        .name(script.SALVAGE_NAME)
        .count()
    script.logger.info("DEPOSIT: Final salvage count: $finalSalvageCount")


    // 5. Determine if deposit was successful and update state
    if (finalSalvageCount < initialSalvageCount) {
        // SUCCESS: Inventory decreased
        script.cargoHoldFull = false
        script.logger.info("DEPOSIT: SUCCESS - Salvage deposited. Cargo hold is not full.")
        return true
    } else {
        // FAILURE: Inventory did not decrease - cargo must be full
        script.cargoHoldFull = true
        script.logger.warn("DEPOSIT: FAILED - Inventory unchanged. Cargo hold is FULL.")
        return false
    }
}