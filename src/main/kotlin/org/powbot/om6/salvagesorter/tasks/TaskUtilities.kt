package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.*
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants

// ========================================
// LOGGING UTILITY FUNCTIONS
// ========================================

/**
 * Logs a message at the specified level and shows a notification.
 * @param script The SalvageSorter script instance
 * @param level The log level (e.g., "info", "warn", "error")
 * @param message The message to log and display
 */
fun log(script: SalvageSorter, level: String, message: String) {
    when (level.lowercase()) {
        "info" -> script.logger.info(message)
        "warn" -> script.logger.warn(message)
        "error" -> script.logger.error(message)
        "debug" -> script.logger.debug(message)
        else -> script.logger.info(message)
    }
    Notifications.showNotification(message)
}


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
 *  Checks if the cargo is open
 * @return true if cargo is open, false if closed
 */
fun isCargoOpen(script: SalvageSorter): Boolean {
    if (!Widgets.widget(Constants.ROOT_CARGO_WIDGET).component(Constants.COMPONENT_CARGO_SPACE).visible()) {
        script.logger.info("isCargoOpen: FALSE, cargo is NOT open")
        return false
    }

    // If the component WAS visible (the 'if' condition was false),
    // the function execution reaches here, so return true (cargo IS open).
    script.logger.info("isCargoOpen: TRUE, cargo is open")
    return true
}

/**
 * Clicks an object at specific screen coordinates and selects a menu action
 * @param screenX X coordinate on screen
 * @param screenY Y coordinate on screen
 * @param actions The menu actions to try (e.g., "Deploy", "Take", "Use")
 * @return true if successful, false otherwise
 */

fun clickAtCoordinates(screenX: Int, screenY: Int, vararg actions: String): Boolean {
    val randomX = screenX + Random.nextInt(-3, 3)
    val randomY = screenY + Random.nextInt(-3, 3)
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
        actions.any { action -> cmd.action.contains(action, ignoreCase = true) }
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
                ScriptManager.stop()
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
            // Check if we still have salvage before retrying
            val hasSalvage = Inventory.stream().name(salvageItemName).isNotEmpty()
            if (hasSalvage) {
                script.logger.warn("INTERRUPT: Extractor ran. Re-tapping Sort.")
                if (clickAtCoordinates(tapX, tapY, "Sort-salvage")) {
                    Condition.sleep(postInterruptWaitMs.toInt())
                    attempts = 0
                    continue
                }
            } else {
                script.logger.info("INTERRUPT: Extractor ran but no salvage remains. Skipping re-tap.")
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
            Condition.sleep(delayMs)
        }
    }
    return false
}

// ========================================
// WORLD HOPPING UTILITY FUNCTIONS
// ========================================

/**
 * Hops to a random valid world.
 * @param script The SalvageSorter script instance
 */
fun hopToRandomWorld(script: SalvageSorter) {
    ensureInventoryOpen()
    Condition.sleep(600)
    val currentWorld = Worlds.current()
    script.logger.info("Current world: ${currentWorld.id()}")

    val validWorlds = Worlds.stream()
        .filtered {
            it.type() == World.Type.MEMBERS && it.population < 1000 &&
                    it.server() == World.Server.NORTH_AMERICA &&
                    it.specialty() != World.Specialty.BOUNTY_HUNTER &&
                    it.specialty() != World.Specialty.PVP &&
                    it.specialty() != World.Specialty.TARGET_WORLD &&
                    it.specialty() != World.Specialty.PVP_ARENA &&
                    it.specialty() != World.Specialty.DEAD_MAN &&
                    it.specialty() != World.Specialty.BETA &&
                    it.specialty() != World.Specialty.HIGH_RISK &&
                    it.specialty() != World.Specialty.LEAGUE &&
                    it.specialty() != World.Specialty.SKILL_REQUIREMENT &&
                    it.specialty() != World.Specialty.SPEEDRUNNING &&
                    it.specialty() != World.Specialty.FRESH_START &&
                    it.specialty() != World.Specialty.TRADE

        }
        .toList()
        .shuffled()

    if (validWorlds.isEmpty()) {
        script.logger.warn("No valid worlds found to hop to")
        Condition.sleep(600)
        return
    }

    for (world in validWorlds.take(10)) {
        script.logger.info("Attempting to hop to world: ${world.id()}")
        if (world.hop()) {
            if (Condition.wait({ Worlds.current() != currentWorld }, 1500, 10)) {
                script.logger.info("Successfully hopped to world: ${Worlds.current().id()}")
                script.hops++
                return
            }
        }
        script.logger.warn("Failed to hop to world: ${world.id()}, trying next...")
        Condition.sleep(300)
    }

    script.logger.warn("Failed to hop after 10 attempts")
}

