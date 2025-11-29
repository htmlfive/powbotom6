package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.CardinalDirection
import org.powbot.om6.salvagesorter.config.LootConfig
import kotlin.random.Random as KotlinRandom

// ========================================
// SLEEP TIMINGS (Easy Configuration)
// ========================================

// Assignment Sleeps
private const val ASSIGNMENT_MAIN_WAIT_MIN = 900
private const val ASSIGNMENT_MAIN_WAIT_MAX = 1200
private const val ASSIGNMENT_INV_OPEN_MIN = 200
private const val ASSIGNMENT_INV_OPEN_MAX = 400

// Walk Sleeps
private const val WALK_WAIT_MIN = 1800
private const val WALK_WAIT_MAX = 2400

// Hook Sleeps
private const val HOOK_MAIN_WAIT_MIN = 900
private const val HOOK_MAIN_WAIT_MAX = 1200
private const val HOOK_TAB_CLOSE_MIN = 600
private const val HOOK_TAB_CLOSE_MAX = 1200
private const val HOOK_TAB_OPEN_MIN = 200
private const val HOOK_TAB_OPEN_MAX = 400
private const val HOOK_WAIT_LOOP_MIN = 1000
private const val HOOK_WAIT_LOOP_MAX = 3000

// Deposit Sleeps
private const val DEPOSIT_PRE_WAIT_MIN = 700
private const val DEPOSIT_PRE_WAIT_MAX = 1100
private const val DEPOSIT_BETWEEN_TAPS_MIN = 1200
private const val DEPOSIT_BETWEEN_TAPS_MAX = 1800

// Cargo Withdraw Sleeps
private const val CARGO_MAIN_WAIT_MIN = 600
private const val CARGO_MAIN_WAIT_MAX = 900
private const val CARGO_TAP1_WAIT_MIN = 1800
private const val CARGO_TAP1_WAIT_MAX = 2400
private const val CARGO_TAP4_WAIT_MIN = 1800
private const val CARGO_TAP4_WAIT_MAX = 2400

// Sort Sleeps
private const val SORT_PRE_TAP_MIN = 500
private const val SORT_PRE_TAP_MAX = 800
private const val SORT_TAB_OPEN_MIN = 200
private const val SORT_TAB_OPEN_MAX = 400
private const val SORT_TAB_CLOSE_MIN = 600
private const val SORT_TAB_CLOSE_MAX = 1200
private const val SORT_SUCCESS_WAIT_MIN = 5000
private const val SORT_SUCCESS_WAIT_MAX = 8000
private const val SORT_CHECK_INTERVAL = 2400
private const val SORT_INITIAL_WAIT = 7200L
private const val SORT_RETAP_MIN = 500
private const val SORT_RETAP_MAX = 800
private const val SORT_MAIN_CHECK_INTERVAL = 1800
private const val SORT_POST_INTERRUPT_WAIT = 600

// Cleanup Sleeps
private const val CLEANUP_ALCH_MIN = 3000
private const val CLEANUP_ALCH_MAX = 3600
private const val CLEANUP_DROP_MIN = 300
private const val CLEANUP_DROP_MAX = 500

// WalkToSort Sleeps
private const val WALKTOSORT_CAMERA_MIN = 600
private const val WALKTOSORT_CAMERA_MAX = 1200
private const val WALKTOSORT_WALK_MIN = 1800
private const val WALKTOSORT_WALK_MAX = 2400

// ========================================
// TAP COORDINATES
// ========================================

// executeWithdrawCargo (4-Tap Sequence)
private const val CARGO_TAP_1_X = 584
private const val CARGO_TAP_1_Y = 148
private const val CARGO_TAP_2_X = 143
private const val CARGO_TAP_2_Y = 237
private const val CARGO_TAP_3_X = 571
private const val CARGO_TAP_3_Y = 159
private const val CARGO_TAP_4_X = 432
private const val CARGO_TAP_4_Y = 490

// executeAssign
private const val ASSIGN_BOTH_1_X = 818 //OPEN TAB
private const val ASSIGN_BOTH_1_Y = 394
private const val ASSIGN_BOTH_2_X = 747 //FIRST SLOT
private const val ASSIGN_BOTH_2_Y = 435
private const val ASSIGN_BOTH_3_X = 690 //SIAD
private const val ASSIGN_BOTH_3_Y = 403
private const val ASSIGN_BOTH_4_X = 747 //SECOND SLOT
private const val ASSIGN_BOTH_4_Y = 469
private const val ASSIGN_BOTH_5_X = 684 //GHOST
private const val ASSIGN_BOTH_5_Y = 370
private const val ASSIGN_CANNON_X = 748 //CANNON SELECT
private const val ASSIGN_CANNON_Y = 401
private const val ASSIGN_BOTH_SCROLL_X = 773 //SCROLL (CLICK X3)
private const val ASSIGN_BOTH_SCROLL_Y = 477

// executeTapSortSalvage (Sort Button)
private const val SORT_BUTTON_X = 574
private const val SORT_BUTTON_Y = 359
private const val SORT_BUTTON_TOLERANCEX = 10
private const val SORT_BUTTON_TOLERANCEY = 10

// hookSalvage and depositSalvage
private const val HOOK_SALVAGE_1_X = 525
private const val HOOK_SALVAGE_1_Y = 406
private const val HOOK_SALVAGE_2_X = 337
private const val HOOK_SALVAGE_2_Y = 350
private const val HOOK_SALVAGE_3_X = 551
private const val HOOK_SALVAGE_3_Y = 308
private const val HOOK_SALVAGE_4_X = 570
private const val HOOK_SALVAGE_4_Y = 165
private const val HOOK_SALVAGE_6_X = 791
private const val HOOK_SALVAGE_6_Y = 63

// ========================================
// CLEANUP FUNCTIONS
// ========================================

fun executeCleanupLoot(script: SalvageSorter): Boolean {
    var successfullyCleaned = false
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    val highAlchSpell = Magic.Spell.HIGH_ALCHEMY
    script.logger.info("CLEANUP: Starting alching loop.")

    LootConfig.ALCH_LIST.forEach { itemName ->
        val item = Inventory.stream().name(itemName).firstOrNull()
        if (item != null && item.valid()) {
            script.logger.info("CLEANUP: Attempting High Alch on $itemName.")
            successfullyCleaned = true

            if (highAlchSpell.cast("Cast") && Condition.wait({Game.tab() == Game.Tab.INVENTORY}, 125, 12)) {
                if(item.interact("Cast")) {
                    Condition.wait({ Game.tab() == Game.Tab.MAGIC }, 125, 12 )
                    script.logger.info("CLEANUP: Alch successful. Sleeping for animation.")
                    Condition.sleep(Random.nextInt(CLEANUP_ALCH_MIN, CLEANUP_ALCH_MAX))
                    Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)
                } else {
                    script.logger.warn("CLEANUP: Failed to click item $itemName.")
                }
            } else {
                script.logger.warn("CLEANUP: Failed to select High Alch spell.")
                return successfullyCleaned
            }
        }
    }

    if (!ensureInventoryOpen(ASSIGNMENT_INV_OPEN_MIN, ASSIGNMENT_INV_OPEN_MAX)) {
        script.logger.warn("CLEANUP: Failed to open inventory tab.")
    }

    val shuffledDroppableItems = Inventory.stream()
        .filter { item -> item.valid() && item.name() in LootConfig.DROP_LIST }
        .toList()
        .shuffled(KotlinRandom)

    script.logger.info("CLEANUP: Dropping ${shuffledDroppableItems.size} items.")

    shuffledDroppableItems.forEach { itemToDrop ->
        if (itemToDrop.valid()) {
            itemToDrop.interact("Drop")
            Condition.sleep(Random.nextInt(CLEANUP_DROP_MIN, CLEANUP_DROP_MAX))
        }
    }

    if (successfullyCleaned) {
        Condition.sleep(Random.nextInt(CLEANUP_DROP_MIN, CLEANUP_DROP_MAX))
    }

    return successfullyCleaned
}

// ========================================
// CARGO WITHDRAW FUNCTIONS
// ========================================

fun executeWithdrawCargo(script: SalvageSorter): Long {
    val mainWait = Random.nextInt(CARGO_MAIN_WAIT_MIN, CARGO_MAIN_WAIT_MAX)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    script.logger.info("CARGO: Starting 4-tap cargo withdrawal sequence.")

    val invCountBefore = Inventory.stream().count()

    // Tap 1: Open cargo
    if (!tapWithSleep(CARGO_TAP_1_X, CARGO_TAP_1_Y, 3, CARGO_TAP1_WAIT_MIN, CARGO_TAP1_WAIT_MAX)) {
        script.logger.warn("CARGO: Failed at tap 1")
        return 0L
    }
    script.logger.info("CARGO TAP 1 (Open): Complete")

    // Tap 2: Withdraw
    if (!tapWithSleep(CARGO_TAP_2_X, CARGO_TAP_2_Y, 3, mainWait, mainWait)) {
        script.logger.warn("CARGO: Failed at tap 2")
        return 0L
    }
    script.logger.info("CARGO TAP 2 (Withdraw): Complete")

    // Tap 3: Close
    if (!tapWithSleep(CARGO_TAP_3_X, CARGO_TAP_3_Y, 3, mainWait, mainWait)) {
        script.logger.warn("CARGO: Failed at tap 3")
        return 0L
    }
    script.logger.info("CARGO TAP 3 (Close): Complete")

    // Tap 4: Walk back
    if (!tapWithSleep(CARGO_TAP_4_X, CARGO_TAP_4_Y, 3, CARGO_TAP4_WAIT_MIN, CARGO_TAP4_WAIT_MAX)) {
        script.logger.warn("CARGO: Failed at tap 4")
        return 0L
    }
    script.logger.info("CARGO TAP 4 (Walk back): Complete")

    val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
    if (!hasSalvage) {
        script.logger.warn("CARGO: Withdrawal failed - no salvage obtained.")
        script.cargoHoldFull = false
        return 0L
    }

    val invCountAfter = Inventory.stream().count()
    val inventoryFull = Inventory.isFull()

    if (!inventoryFull) {
        script.logger.warn("CARGO: Inventory not full ($invCountAfter/28). Cargo depleted.")
        return -1L
    }

    val baseCooldownMs = script.randomWithdrawCooldownMs
    script.logger.info("CARGO: Inventory full. Cooldown: ${baseCooldownMs / 1000}s.")
    return baseCooldownMs
}

// ========================================
// SORTING FUNCTIONS
// ========================================

fun executeTapSortSalvage(script: SalvageSorter, salvageItemName: String): Boolean {
    CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
    Condition.sleep(Random.nextInt(SORT_PRE_TAP_MIN, SORT_PRE_TAP_MAX))

    val randomOffsetX = Random.nextInt(-SORT_BUTTON_TOLERANCEX, SORT_BUTTON_TOLERANCEX + 13)
    val randomOffsetY = Random.nextInt(-SORT_BUTTON_TOLERANCEY, SORT_BUTTON_TOLERANCEY + 1)
    val finalX = SORT_BUTTON_X + randomOffsetX
    val finalY = SORT_BUTTON_Y + randomOffsetY

    val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

    ensureInventoryOpen(SORT_TAB_OPEN_MIN, SORT_TAB_OPEN_MAX)
    Condition.sleep(Random.nextInt(SORT_TAB_CLOSE_MIN, SORT_TAB_CLOSE_MAX))

    script.logger.info("ACTION: Tapping Sort Salvage at X=$finalX, Y=$finalY. Count: $salvageCountBefore.")
    closeTabWithSleep(SORT_TAB_CLOSE_MIN, SORT_TAB_CLOSE_MAX)

    if (tapWithOffset(finalX, finalY, 0)) {
        var elapsed = 0L
        var currentSalvageCount = salvageCountBefore
        var lastSalvageCount = salvageCountBefore
        var retapFailureCount = 0
        val MAX_RETAP_FAILURES = 2

        script.logger.info("RETAP: Starting ${SORT_INITIAL_WAIT}ms check for active sorting.")

        while (elapsed < SORT_INITIAL_WAIT) {
            Condition.sleep(SORT_CHECK_INTERVAL)
            elapsed += SORT_CHECK_INTERVAL

            currentSalvageCount = Inventory.stream().name(salvageItemName).count()

            if (currentSalvageCount < salvageCountBefore) {
                script.logger.info("RETAP: Sort started. Items removed: ${salvageCountBefore - currentSalvageCount}.")
                break
            }

            handleDialogue(SORT_RETAP_MIN, SORT_RETAP_MAX)

            if (currentSalvageCount >= lastSalvageCount) {
                retapFailureCount++

                if (retapFailureCount > MAX_RETAP_FAILURES) {
                    script.logger.error("FATAL: Sort stalled after $MAX_RETAP_FAILURES retaps. Stopping.")
                    ScriptManager.stop()
                    return true
                }

                script.logger.warn("RETAP: Count unchanged. Retapping (Attempt $retapFailureCount).")
                if (tapWithOffset(finalX, finalY, 0)) {
                    Condition.sleep(Random.nextInt(SORT_RETAP_MIN, SORT_RETAP_MAX))
                    lastSalvageCount = currentSalvageCount
                }
            } else {
                retapFailureCount = 0
                lastSalvageCount = currentSalvageCount
            }
        }

        val timeoutTicks = 20
        var waitSuccess = currentSalvageCount.toInt() == 0
        var attempts = 0

        if (!waitSuccess) {
            script.logger.info("POLLING: Waiting for inventory clear.")

            while (attempts < timeoutTicks) {
                if (script.extractorTask.checkAndExecuteInterrupt(script)) {
                    script.logger.warn("INTERRUPT: Extractor ran. Re-tapping Sort.")
                    if (tapWithOffset(finalX, finalY, 0)) {
                        Condition.sleep(SORT_POST_INTERRUPT_WAIT)
                        attempts = 0
                        continue
                    }
                }

                if (Inventory.stream().name(salvageItemName).isEmpty()) {
                    waitSuccess = true
                    script.logger.info("POLLING: Inventory cleared.")
                    break
                }

                Condition.sleep(SORT_MAIN_CHECK_INTERVAL)
                attempts++
            }
        }

        if (waitSuccess) {
            script.logger.info("SUCCESS: Sort complete.")
            Condition.sleep(Random.nextInt(SORT_SUCCESS_WAIT_MIN, SORT_SUCCESS_WAIT_MAX))
            return true
        } else {
            script.logger.warn("FAIL: Sort timed out.")
            return false
        }
    }
    return false
}

// ========================================
// ASSIGNMENT FUNCTIONS
// ========================================

fun assignBoth(script: SalvageSorter): Boolean {
    script.logger.info("ASSIGNMENTS: Starting 5-tap sequence.")
    val mainWait = setupAssignment(script, ASSIGNMENT_MAIN_WAIT_MIN, ASSIGNMENT_MAIN_WAIT_MAX)

    val taps = listOf(
        ASSIGN_BOTH_1_X to ASSIGN_BOTH_1_Y,
//        ASSIGN_BOTH_SCROLL_X to ASSIGN_BOTH_SCROLL_Y,
//        ASSIGN_BOTH_SCROLL_X to ASSIGN_BOTH_SCROLL_Y,
//        ASSIGN_BOTH_SCROLL_X to ASSIGN_BOTH_SCROLL_Y,
        ASSIGN_BOTH_2_X to ASSIGN_BOTH_2_Y,
        ASSIGN_BOTH_3_X to ASSIGN_BOTH_3_Y,
        ASSIGN_BOTH_4_X to ASSIGN_BOTH_4_Y,
        ASSIGN_BOTH_5_X to ASSIGN_BOTH_5_Y
    )

    val success = executeTapSequence(script, taps, 3, mainWait, mainWait, "ASSIGN")

    if (success) {
        script.logger.info("ASSIGNMENTS: Complete.")
    }

    return success
}

fun assignGhost(script: SalvageSorter): Boolean {
    script.logger.info("ASSIGNMENTS: Starting 3-tap ghost sequence.")
    val mainWait = setupAssignment(script, ASSIGNMENT_MAIN_WAIT_MIN, ASSIGNMENT_MAIN_WAIT_MAX)

    val taps = listOf(
        ASSIGN_BOTH_1_X to ASSIGN_BOTH_1_Y,
//        ASSIGN_BOTH_SCROLL_X to ASSIGN_BOTH_SCROLL_Y,
//        ASSIGN_BOTH_SCROLL_X to ASSIGN_BOTH_SCROLL_Y,
//        ASSIGN_BOTH_SCROLL_X to ASSIGN_BOTH_SCROLL_Y,
        ASSIGN_BOTH_2_X to ASSIGN_BOTH_2_Y,
        ASSIGN_BOTH_5_X to ASSIGN_BOTH_5_Y,
        ASSIGN_CANNON_X to ASSIGN_CANNON_Y,
        ASSIGN_BOTH_3_X to ASSIGN_BOTH_3_Y

    )

    if (!executeTapSequence(script, taps, 3, mainWait, mainWait, "GHOST")) {
        return false
    }

    // Reopen and close inventory
    ensureInventoryOpen(ASSIGNMENT_INV_OPEN_MIN, ASSIGNMENT_INV_OPEN_MAX)
    Condition.sleep(mainWait)
    closeTabWithSleep(mainWait, mainWait)

    script.logger.info("ASSIGNMENTS: Ghost complete.")
    return true
}

// ========================================
// SALVAGING FUNCTIONS
// ========================================

fun walkToHook(script: SalvageSorter): Boolean {
    if (script.atHookLocation) {
        script.logger.info("WALK: Already at hook location. Skipping movement and assignment.")
        return true
    }

    script.logger.info("WALK: Not at hook location yet. Starting walk and assignment sequence.")

    // Skip Ghost assignment in Power Salvage Mode
    if (!script.powerSalvageMode) {
        if (!assignGhost(script)) {
            script.logger.warn("WALK: Failed to assign Ghost.")
            return false
        }
    } else {
        script.logger.info("WALK: Power Salvage Mode - Skipping Ghost assignment.")
    }

    val waitTime = Random.nextInt(WALK_WAIT_MIN, WALK_WAIT_MAX)
    Condition.sleep(waitTime)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

    script.logger.info("WALK: Tapping walk-to-hook.")

    if (!tapWithSleep(HOOK_SALVAGE_6_X, HOOK_SALVAGE_6_Y, 3, waitTime, waitTime)) {
        script.logger.warn("WALK: Failed to tap walk point.")
        return false
    }

    script.atHookLocation = true
    script.logger.info("WALK: Arrived at hook location. Flag set to true.")
    return true
}

fun hookSalvage(script: SalvageSorter): Boolean {
    // Skip deposit logic in Power Salvage Mode - DropSalvageTask handles it
    if (!script.powerSalvageMode) {
        if (Inventory.isFull() && !script.cargoHoldFull) {
            script.logger.info("HOOK: Inventory full. Depositing first.")
            return depositSalvage(script)
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
    val mainWait = Random.nextInt(HOOK_MAIN_WAIT_MIN, HOOK_MAIN_WAIT_MAX)
    Condition.sleep(mainWait)

    script.hookCastMessageFound = false

    // Try tapping hook up to 3 times before giving up
    var messageFound = false
    for (attempt in 1..3) {
        script.logger.info("HOOK: Tapping hook (Attempt $attempt/3).")
        tapWithOffset(HOOK_SALVAGE_1_X, HOOK_SALVAGE_1_Y, 3)

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
        val extractorTask = CrystalExtractorTask(script)
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
                handleMultipleDialogues(2, SORT_RETAP_MIN, SORT_RETAP_MAX)
                script.logger.info("HOOK: Transitioning to SORTING.")
                script.cargoHoldFull = true
                script.currentPhase = org.powbot.om6.salvagesorter.config.SalvagePhase.SETUP_SORTING
                script.hookingSalvageBool = false
                return true
            }

            if (extractorTask.checkAndExecuteInterrupt(script)) {
                script.logger.info("HOOK: Extractor interrupted. Re-hooking.")
                script.hookingSalvageBool = false
                return false
            }

            Condition.sleep(Random.nextInt(HOOK_WAIT_LOOP_MIN, HOOK_WAIT_LOOP_MAX))
        }

        script.hookingSalvageBool = false
        script.logger.info("HOOK: Inventory full.")
        return true
    } else {
        script.hookingSalvageBool = false

        if (Chat.canContinue()) {
            script.logger.warn("HOOK: Dialogue before confirmation.")
            handleMultipleDialogues(2, SORT_RETAP_MIN, SORT_RETAP_MAX)
            script.logger.info("HOOK: Transitioning to SORTING.")
            script.cargoHoldFull = true
            script.currentPhase = org.powbot.om6.salvagesorter.config.SalvagePhase.SETUP_SORTING
            return true
        } else {
            script.logger.error("HOOK: No confirmation. Stopping.")
            ScriptManager.stop()
            return false
        }
    }
}

fun depositSalvage(script: SalvageSorter): Boolean {
    CameraSnapper.snapCameraToDirection(CardinalDirection.South, script)
    Condition.sleep(Random.nextInt(DEPOSIT_PRE_WAIT_MIN, DEPOSIT_PRE_WAIT_MAX))

    val initialSalvageCount = Inventory.stream().name(script.SALVAGE_NAME).count()
    script.logger.info("DEPOSIT: Initial count: $initialSalvageCount")

    Condition.sleep(Random.nextInt(DEPOSIT_BETWEEN_TAPS_MIN, DEPOSIT_BETWEEN_TAPS_MAX))

    val depositTaps = listOf(
        HOOK_SALVAGE_2_X to HOOK_SALVAGE_2_Y,
        HOOK_SALVAGE_3_X to HOOK_SALVAGE_3_Y,
        HOOK_SALVAGE_4_X to HOOK_SALVAGE_4_Y
    )

    executeTapSequence(script, depositTaps, 3, DEPOSIT_BETWEEN_TAPS_MIN, DEPOSIT_BETWEEN_TAPS_MAX, "DEPOSIT")

    val finalSalvageCount = Inventory.stream().name(script.SALVAGE_NAME).count()
    val depositedCount = (initialSalvageCount - finalSalvageCount).toInt()

    if (finalSalvageCount < initialSalvageCount) {
        script.cargoHoldFull = false
        script.xpMessageCount += depositedCount
        script.logger.info("DEPOSIT: SUCCESS - Deposited $depositedCount. Cargo count: ${script.xpMessageCount}")
        return true
    } else {
        script.cargoHoldFull = true
        script.xpMessageCount = script.maxCargoSpace.toInt()
        script.logger.warn("DEPOSIT: FAILED - Cargo FULL. Set count to 120.")
        return false
    }
}

fun walkToSort(script: SalvageSorter): Boolean {
    if (script.atSortLocation) {
        script.logger.info("WALK_SORT: Already at sort location.")
        return true
    }

    script.logger.info("WALK_SORT: Starting walk and assignment.")

    if (!assignBoth(script)) {
        script.logger.warn("WALK_SORT: Failed to assign crew.")
        return false
    }

    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    Condition.sleep(Random.nextInt(WALKTOSORT_CAMERA_MIN, WALKTOSORT_CAMERA_MAX))

    script.logger.info("WALK_SORT: Tapping walk point.")

    if (!tapWithSleep(580, 482, 3, WALKTOSORT_WALK_MIN, WALKTOSORT_WALK_MAX)) {
        script.logger.warn("WALK_SORT: Failed to tap.")
        return false
    }

    script.atSortLocation = true
    script.logger.info("WALK_SORT: Arrived at sort location.")
    return true
}