package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Notifications
import org.powbot.api.Point
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter

// ========================================
// WIDGET UTILITY FUNCTIONS
// ========================================

/**
 * Clicks a widget component by root and component indices.
 * @param root The root widget index
 * @param component The component index within the root widget
 * @param index Optional sub-component index
 * @param action Optional action to perform (e.g., "Select"). If null, performs a simple click.
 * @return true if the widget was valid and clicked successfully
 */
fun clickWidget(root: Int, component: Int, index: Int? = null, action: String? = null): Boolean {
    Game.setSingleTapToggle(false)
    val widget = if (index != null) {
        Widgets.widget(root).component(component).component(index)
    } else {
        Widgets.widget(root).component(component)
    }
    if (!widget.valid()) return false
    return if (action != null) widget.interact(action) else widget.click()
}

/**
 * Clicks a widget with retry logic.
 * @param root The root widget index
 * @param component The component index within the root widget
 * @param index Optional sub-component index
 * @param action Optional action to perform
 * @param maxAttempts Maximum number of attempts (default: 3)
 * @param retryDelayMs Delay between retries in milliseconds (default: 600)
 * @param logPrefix Optional prefix for logging (if null, no logging)
 * @param script Optional script instance for logging
 * @return true if the widget was clicked successfully within maxAttempts
 */
fun clickWidgetWithRetry(
    root: Int,
    component: Int,
    index: Int? = null,
    action: String? = null,
    maxAttempts: Int = 3,
    retryDelayMs: Int = 600,
    logPrefix: String? = null,
    script: SalvageSorter? = null
): Boolean {
    Game.setSingleTapToggle(false)
    for (attempt in 1..maxAttempts) {
        if (clickWidget(root, component, index, action)) {
            if (logPrefix != null && script != null && attempt > 1) {
                script.logger.info("$logPrefix: Click succeeded on attempt $attempt/$maxAttempts")
            }
            return true
        }

        if (attempt < maxAttempts) {
            if (logPrefix != null && script != null) {
                script.logger.warn("$logPrefix: Click failed on attempt $attempt/$maxAttempts, retrying...")
            }
            Condition.sleep(retryDelayMs)
        }
    }

    if (logPrefix != null && script != null) {
        script.logger.warn("$logPrefix: Click failed after $maxAttempts attempts")
        Notifications.showNotification("$logPrefix: Click failed after $maxAttempts attempts. Stopping script.")
        ScriptManager.stop()
    }
    return false
}

/**
 * Checks if a widget component is visible and valid.
 * @param root The root widget index
 * @param component The component index
 * @param index Optional sub-component index
 * @return true if the widget is valid and visible
 */
fun isWidgetVisible(root: Int, component: Int, index: Int? = null): Boolean {
    Game.setSingleTapToggle(false)
    val widget = if (index != null) {
        Widgets.widget(root).component(component).component(index)
    } else {
        Widgets.widget(root).component(component)
    }
    return widget.valid() && widget.visible()
}

// ========================================
// TAP/CLICK UTILITY FUNCTIONS
// ========================================

/**
 * Taps at coordinates with random offset.
 * @param x The base X coordinate to tap
 * @param y The base Y coordinate to tap
 * @param offsetRange The maximum random offset in pixels (default: 3)
 * @return true if the tap was successful
 */
fun tapWithOffset(x: Int, y: Int, offsetRange: Int = 3): Boolean {
    Game.setSingleTapToggle(false)
    val finalX = x + Random.nextInt(-offsetRange, offsetRange + 1)
    val finalY = y + Random.nextInt(-offsetRange, offsetRange + 1)
    return Input.tap(finalX, finalY)
}

/**
 * Clicks an object at specific screen coordinates and selects a menu action
 * @param screenX X coordinate on screen
 * @param screenY Y coordinate on screen
 * @param action The menu action to select (e.g., "Deploy", "Take", "Use")
 * @return true if successful, false otherwise
 */
fun clickAtCoordinates(screenX: Int, screenY: Int, action: String): Boolean {
    val randomX = screenX + Random.nextInt(-5, 5)
    val randomY = screenY + Random.nextInt(-5, 5)
    val point = Point(randomX, randomY)

    Game.setSingleTapToggle(true)
    Condition.sleep(Random.nextInt(60, 80))
    Input.tap(point)

    // Wait for menu to open
    if (!Condition.wait({ Menu.opened() }, freq = 100, tries = 20)) {
        return false
    }

    // Click the menu action
    return Menu.click { cmd ->
        cmd.action.contains(action, ignoreCase = true)
    }
}

// ========================================
// INVENTORY UTILITY FUNCTIONS
// ========================================

/**
 * Ensures inventory tab is open.
 * @param sleepMin Minimum sleep time in milliseconds after opening (default: 100)
 * @param sleepMax Maximum sleep time in milliseconds after opening (default: 200)
 * @return true if inventory is open or was successfully opened
 */
fun ensureInventoryOpen(sleepMin: Int = 100, sleepMax: Int = 200): Boolean {
    Game.setSingleTapToggle(false)
    if (Inventory.opened()) return true
    if (Inventory.open()) {
        Condition.sleep(Random.nextInt(sleepMin, sleepMax))
        return true
    }
    return false
}

/**
 * Closes open tab with sleep.
 * @param sleepMin Minimum sleep time in milliseconds after closing
 * @param sleepMax Maximum sleep time in milliseconds after closing
 */
fun closeTabWithSleep(sleepMin: Int, sleepMax: Int) {
    Game.closeOpenTab()
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
}

// ========================================
// DIALOGUE UTILITY FUNCTIONS
// ========================================

/**
 * Handles dialogue if present.
 * @param sleepMin Minimum sleep time in milliseconds after clicking continue (default: 500)
 * @param sleepMax Maximum sleep time in milliseconds after clicking continue (default: 800)
 * @return true if dialogue was present and handled
 */
fun handleDialogue(sleepMin: Int = 500, sleepMax: Int = 800): Boolean {
    if (!Chat.canContinue()) return false
    Chat.clickContinue()
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
    return true
}

/**
 * Handles multiple dialogue prompts.
 * @param maxAttempts Maximum number of dialogue prompts to handle (default: 2)
 * @param sleepMin Minimum sleep time in milliseconds between prompts (default: 500)
 * @param sleepMax Maximum sleep time in milliseconds between prompts (default: 800)
 * @return The number of dialogue prompts that were handled
 */
fun handleMultipleDialogues(maxAttempts: Int = 2, sleepMin: Int = 500, sleepMax: Int = 800): Int {
    var count = 0
    while (count < maxAttempts && handleDialogue(sleepMin, sleepMax)) {
        count++
    }
    return count
}

// ========================================
// SORTING UTILITY FUNCTIONS
// ========================================

/**
 * Monitors salvage count and retaps if sorting doesn't start.
 * Handles the initial wait period where we verify the sort action actually started.
 *
 * @param script The SalvageSorter script instance
 * @param salvageItemName Name of the salvage item to monitor
 * @param initialCount Initial salvage count before tap
 * @param tapX X coordinate for retapping
 * @param tapY Y coordinate for retapping
 * @param action Menu action to use when retapping
 * @param initialWaitMs How long to wait for sort to start (default: from Constants)
 * @param checkIntervalMs How often to check count (default: from Constants)
 * @param maxRetapFailures Maximum number of failed retaps before giving up (default: 2)
 * @param retapSleepMin Min sleep after retap (default: from Constants)
 * @param retapSleepMax Max sleep after retap (default: from Constants)
 * @return Current salvage count after monitoring period
 */
fun monitorAndRetapIfStalled(
    script: SalvageSorter,
    salvageItemName: String,
    initialCount: Long,
    tapX: Int,
    tapY: Int,
    action: String,
    initialWaitMs: Long,
    checkIntervalMs: Long,
    maxRetapFailures: Int = 2,
    retapSleepMin: Int,
    retapSleepMax: Int
): Long {
    var elapsed = 0L
    var currentCount = initialCount
    var lastCount = initialCount // The count *before* the last tap/action
    var retapFailureCount = 0

    script.logger.info("RETAP: Starting ${initialWaitMs}ms check for active sorting.")

    while (elapsed < initialWaitMs) {
        Condition.sleep(checkIntervalMs.toInt())
        elapsed += checkIntervalMs

        currentCount = Inventory.stream().name(salvageItemName).count()

        // 1. SUCCESS: Count has dropped, meaning the sort started.
        if (currentCount < initialCount) {
            script.logger.info("RETAP: Sort started. Items removed: ${initialCount - currentCount}.")
            return currentCount // Exit immediately
        }

        // 2. Failure Check: If we are past the initial wait and the count still hasn't dropped,
        // it means the action failed. We now check the retap failure limit.
        if (elapsed >= initialWaitMs || currentCount >= lastCount) {

            // Check the failure count against the limit
            if (retapFailureCount > maxRetapFailures) {
                script.logger.error("FATAL: Sort stalled after $maxRetapFailures retaps. Stopping.")
                Notifications.showNotification("FATAL: Sort stalled after $maxRetapFailures retaps. Stopping.")
                org.powbot.mobile.script.ScriptManager.stop()
                return currentCount
            }

            // Perform the Retap
            script.logger.warn("RETAP: Count unchanged/stalled. Retapping (Attempt ${retapFailureCount + 1}).")
            if (clickAtCoordinates(tapX, tapY, action)) {
                // IMPORTANT: Increment the failure count here, as a retap attempt has now been made
                retapFailureCount++

                // Sleep to allow the action to process
                Condition.sleep(Random.nextInt(retapSleepMin, retapSleepMax))

                // Update lastCount to the count *before* the current retap sleep.
                // The next check will see if the count dropped *after* this retap.
                lastCount = currentCount
            }
        }

        handleDialogue(retapSleepMin, retapSleepMax)
    }

    // After the initial wait loop completes without the count dropping,
    // we return the final count, assuming a failure/stall has been handled
    // by the failure count check inside the loop.
    return currentCount
}

/**
 * Waits for inventory to be cleared of a specific item.
 * Handles extractor interrupts and retaps if needed.
 *
 * @param script The SalvageSorter script instance
 * @param extractorTask The extractor task to check for interrupts
 * @param salvageItemName Name of the item to wait for removal
 * @param tapX X coordinate for retapping after interrupt
 * @param tapY Y coordinate for retapping after interrupt
 * @param maxAttempts Maximum polling attempts (default: 20)
 * @param checkIntervalMs Interval between checks in ms (default: from Constants)
 * @param postInterruptWaitMs Wait time after extractor interrupt (default: from Constants)
 * @return true if inventory was successfully cleared, false if timeout
 */
fun waitForInventoryClear(
    script: SalvageSorter,
    extractorTask: CrystalExtractorTask,
    salvageItemName: String,
    tapX: Int,
    tapY: Int,
    maxAttempts: Int = 20,
    checkIntervalMs: Long,
    postInterruptWaitMs: Long
): Boolean {
    var attempts = 0

    script.logger.info("POLLING: Waiting for inventory clear.")

    while (attempts < maxAttempts) {
        if (extractorTask.checkAndExecuteInterrupt(script)) {
            script.logger.warn("INTERRUPT: Extractor ran. Re-tapping Sort.")
            if (tapWithOffset(tapX, tapY, 0)) {
                Condition.sleep(postInterruptWaitMs.toInt())
                attempts = 0
                continue
            }
        }

        if (Inventory.stream().name(salvageItemName).isEmpty()) {
            script.logger.info("POLLING: Inventory cleared.")
            return true
        }

        Condition.sleep(checkIntervalMs.toInt())
        attempts++
    }

    return false
}

fun retryAction(maxRetries: Int, delayMs: Int, action: () -> Boolean): Boolean {
    for (i in 1..maxRetries) {
        // 1. Execute the action
        if (action()) {
            return true
        }
        if (i < maxRetries) {
            Condition.sleep(600)
        }
    }
    return false
}