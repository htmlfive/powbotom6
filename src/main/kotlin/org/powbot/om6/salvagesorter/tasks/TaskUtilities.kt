package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.om6.salvagesorter.SalvageSorter

// ========================================
// UTILITY FUNCTIONS
// ========================================

/**
 * Taps at coordinates with random offset
 */
fun tapWithOffset(x: Int, y: Int, offsetRange: Int = 3): Boolean {
    val finalX = x + Random.nextInt(-offsetRange, offsetRange + 1)
    val finalY = y + Random.nextInt(-offsetRange, offsetRange + 1)
    return Input.tap(finalX, finalY)
}

/**
 * Taps with offset and sleeps afterward
 */
fun tapWithSleep(x: Int, y: Int, offsetRange: Int = 3, sleepMin: Int, sleepMax: Int): Boolean {
    if (!tapWithOffset(x, y, offsetRange)) return false
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
    return true
}

/**
 * Ensures inventory tab is open
 */
fun ensureInventoryOpen(sleepMin: Int = 200, sleepMax: Int = 400): Boolean {
    if (Inventory.opened()) return true
    if (Inventory.open()) {
        Condition.sleep(Random.nextInt(sleepMin, sleepMax))
        return true
    }
    return false
}

/**
 * Closes open tab with sleep
 */
fun closeTabWithSleep(sleepMin: Int, sleepMax: Int) {
    Game.closeOpenTab()
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
}

/**
 * Handles dialogue if present
 */
fun handleDialogue(sleepMin: Int = 500, sleepMax: Int = 800): Boolean {
    if (!Chat.canContinue()) return false
    Chat.clickContinue()
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
    return true
}

/**
 * Handles multiple dialogue prompts
 */
fun handleMultipleDialogues(maxAttempts: Int = 2, sleepMin: Int = 500, sleepMax: Int = 800): Int {
    var count = 0
    while (count < maxAttempts && handleDialogue(sleepMin, sleepMax)) {
        count++
    }
    return count
}

/**
 * Common assignment sequence setup
 */
fun setupAssignment(script: SalvageSorter, mainWaitMin: Int = 900, mainWaitMax: Int = 1200): Int {
    val mainWait = Random.nextInt(mainWaitMin, mainWaitMax)
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

    if (!ensureInventoryOpen()) {
        script.logger.warn("ASSIGNMENT: Failed to open inventory")
    }

    Condition.sleep(mainWait)
    closeTabWithSleep(mainWait, mainWait)

    return mainWait
}

/**
 * Execute a tap sequence with uniform sleep between taps
 */
fun executeTapSequence(
    script: SalvageSorter,
    coordinates: List<Pair<Int, Int>>,
    offsetRange: Int = 3,
    sleepMin: Int,
    sleepMax: Int,
    logPrefix: String = "TAP"
): Boolean {
    coordinates.forEachIndexed { index, (x, y) ->
        if (!tapWithSleep(x, y, offsetRange, sleepMin, sleepMax)) {
            script.logger.warn("$logPrefix: Failed at tap ${index + 1} of ${coordinates.size}")
            return false
        }
        script.logger.info("$logPrefix ${index + 1}: Complete")
    }
    return true
}