package org.powbot.om6.salvagesorter2.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter2.SalvageSorter
import org.powbot.om6.salvagesorter2.config.LootConfig
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

// executeAssignBoth
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
private const val ASSIGN_BOTH_6_X = 678 // UNASSIGN
private const val ASSIGN_BOTH_6_Y = 334

// executeTapSortSalvage (Sort Button)
private const val SORT_BUTTON_X = 574
private const val SORT_BUTTON_Y = 359
private const val SORT_BUTTON_TOLERANCEX = 10
private const val SORT_BUTTON_TOLERANCEY = 10

// ----------------------------------------

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
                    Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)

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

fun executeWithdrawCargo(script: SalvageSorter): Long {
    fun getRandomOffsetLarge() = Random.nextInt(-3, 3)
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
        script.logger.warn("STOP: Withdrawal failed to provide salvage item (${script.SALVAGE_NAME}). Stopping script.")
        //ScriptManager.stop()
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

fun executeTapSortSalvage(script: SalvageSorter, salvageItemName: String): Boolean {

    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    Condition.sleep(Random.nextInt(500, 800))

    val randomOffsetX = Random.nextInt(-SORT_BUTTON_TOLERANCEX, SORT_BUTTON_TOLERANCEX + 13)
    val randomOffsetY = Random.nextInt(-SORT_BUTTON_TOLERANCEY, SORT_BUTTON_TOLERANCEY + 1)
    val finalX = SORT_BUTTON_X + randomOffsetX
    val finalY = SORT_BUTTON_Y + randomOffsetY

    val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

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
        val MAX_RETAP_FAILURES = 2 // FIXED: Max retap attempts is 2 (initial check + 2 retries = 3 total checks)

        script.logger.info("RETAP: Starting $initialWaitTime ms check for active sorting (1800ms check interval).")

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

                if (retapFailureCount > MAX_RETAP_FAILURES) { // Using '>' here to respect 2 retries (0, 1, 2)
                    // The logic here is triggered after the 3rd check (initial + 2 retries)
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

fun assignBoth(script: SalvageSorter): Boolean {
    fun getRandomOffsetLarge() = Random.nextInt(-3, 3)
    val mainWait = Random.nextInt(900, 1200)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    script.logger.info("ASSIGNMENTS: Starting 5-tap assignment sequence.")
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

    // Tap 6 (UNASSIGN) is defined in constants but not used in the function body, assuming 5 taps complete the sequence for "Assign Both".

    script.logger.info("ASSIGNMENTS: Assignment sequence complete.")
    return true
}

fun assignGhost(script: SalvageSorter): Boolean {
    fun getRandomOffsetLarge() = Random.nextInt(-3, 3)
    val mainWait = Random.nextInt(900, 1200)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)
    script.logger.info("ASSIGNMENTS: Starting 5-tap assignment sequence.")
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

    script.logger.info("ASSIGNMENTS: Tap 5 (Ghost) complete. Waiting ${mainWait}ms.")
    Condition.sleep(mainWait)

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
    Condition.sleep(mainWait)
    Game.closeOpenTab()
    Condition.sleep(mainWait)

    script.logger.info("ASSIGNMENTS: Assignment sequence complete.")
    return true
}