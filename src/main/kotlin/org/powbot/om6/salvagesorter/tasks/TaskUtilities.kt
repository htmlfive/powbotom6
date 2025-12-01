package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Point
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.om6.salvagesorter.SalvageSorter

// ========================================
// WIDGET UTILITY FUNCTIONS
// ========================================

/**
 * Clicks a widget component by root and component indices.
 * @param root The root widget index
 * @param component The component index within the root widget
 * @param action Optional action to perform (e.g., "Select"). If null, performs a simple click.
 * @return true if the widget was valid and clicked successfully
 */
fun clickWidget(root: Int, component: Int, action: String? = null): Boolean {
    Game.setSingleTapToggle(false)
    val widget = Widgets.widget(root).component(component)
    if (!widget.valid()) return false
    return if (action != null) widget.interact(action) else widget.click()
}

/**
 * Clicks a widget component with a sub-index (for nested components).
 * @param root The root widget index
 * @param component The component index within the root widget
 * @param index The sub-component index
 * @param action Optional action to perform. If null, performs a simple click.
 * @return true if the widget was valid and clicked successfully
 */
fun clickWidget(root: Int, component: Int, index: Int, action: String? = null): Boolean {
    Game.setSingleTapToggle(false)
    val widget = Widgets.widget(root).component(component).component(index)
    if (!widget.valid()) return false
    return if (action != null) widget.interact(action) else widget.click()
}

/**
 * Clicks a widget with retry logic.
 * @param root The root widget index
 * @param component The component index within the root widget
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
    action: String? = null,
    maxAttempts: Int = 3,
    retryDelayMs: Int = 600,
    logPrefix: String? = null,
    script: SalvageSorter? = null
): Boolean {
    Game.setSingleTapToggle(false)
    for (attempt in 1..maxAttempts) {
        if (clickWidget(root, component, action)) {
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
    }
    return false
}

/**
 * Clicks a widget with sub-index and retry logic.
 * @param root The root widget index
 * @param component The component index within the root widget
 * @param index The sub-component index
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
    index: Int,
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
    }
    return false
}

/**
 * Clicks a widget and waits for a condition.
 * @param root The root widget index
 * @param component The component index
 * @param condition The condition to wait for after clicking
 * @param timeout Maximum time to wait in milliseconds (default: 2400)
 * @param action Optional action to perform
 * @return true if clicked and condition was met
 */
fun clickWidgetAndWait(
    root: Int,
    component: Int,
    condition: () -> Boolean,
    timeout: Int = 2400,
    action: String? = null
): Boolean {
    Game.setSingleTapToggle(false)
    if (!clickWidget(root, component, action)) return false
    return Condition.wait(condition, 100, timeout / 100)
}

/**
 * Clicks a widget with sub-index and waits for a condition.
 * @param root The root widget index
 * @param component The component index
 * @param index The sub-component index
 * @param condition The condition to wait for after clicking
 * @param timeout Maximum time to wait in milliseconds (default: 2400)
 * @param action Optional action to perform
 * @return true if clicked and condition was met
 */
fun clickWidgetAndWait(
    root: Int,
    component: Int,
    index: Int,
    condition: () -> Boolean,
    timeout: Int = 2400,
    action: String? = null
): Boolean {
    Game.setSingleTapToggle(false)
    if (!clickWidget(root, component, index, action)) return false
    return Condition.wait(condition, 100, timeout / 100)
}

/**
 * Checks if a widget component is visible and valid.
 * @param root The root widget index
 * @param component The component index
 * @return true if the widget is valid and visible
 */
fun isWidgetVisible(root: Int, component: Int): Boolean {
    Game.setSingleTapToggle(false)
    val widget = Widgets.widget(root).component(component)
    return widget.valid() && widget.visible()
}

/**
 * Checks if a widget component with sub-index is visible and valid.
 * @param root The root widget index
 * @param component The component index
 * @param index The sub-component index
 * @return true if the widget is valid and visible
 */
fun isWidgetVisible(root: Int, component: Int, index: Int): Boolean {
    Game.setSingleTapToggle(false)
    val widget = Widgets.widget(root).component(component).component(index)
    return widget.valid() && widget.visible()
}

// ========================================
// TAP UTILITY FUNCTIONS
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
 * Taps with offset and sleeps afterward.
 * @param x The base X coordinate to tap
 * @param y The base Y coordinate to tap
 * @param offsetRange The maximum random offset in pixels (default: 3)
 * @param sleepMin Minimum sleep time in milliseconds after tap
 * @param sleepMax Maximum sleep time in milliseconds after tap
 * @return true if the tap was successful
 */
fun tapWithSleep(x: Int, y: Int, offsetRange: Int = 3, sleepMin: Int, sleepMax: Int): Boolean {
    Game.setSingleTapToggle(false)
    if (!tapWithOffset(x, y, offsetRange)) return false
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
    return true
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
// ASSIGNMENT UTILITY FUNCTIONS
// ========================================

/**
 * Common assignment sequence setup.
 * @param script The SalvageSorter script instance
 * @param mainWaitMin Minimum wait time in milliseconds (default: 900)
 * @param mainWaitMax Maximum wait time in milliseconds (default: 1200)
 * @return The calculated main wait time used
 */
fun setupAssignment(script: SalvageSorter, mainWaitMin: Int = 900, mainWaitMax: Int = 1200): Int {
    val mainWait = Random.nextInt(mainWaitMin, mainWaitMax)
    CameraSnapper.snapCameraToDirection(script.cameraDirection, script)

    if (!ensureInventoryOpen()) {
        script.logger.warn("ASSIGNMENT: Failed to open inventory")
    }

    Condition.sleep(mainWait)
    closeTabWithSleep(100, 200)

    return mainWait
}

/**
 * Execute a tap sequence with uniform sleep between taps.
 * @param script The SalvageSorter script instance for logging
 * @param coordinates List of (x, y) coordinate pairs to tap in sequence
 * @param offsetRange The maximum random offset in pixels for each tap (default: 3)
 * @param sleepMin Minimum sleep time in milliseconds between taps
 * @param sleepMax Maximum sleep time in milliseconds between taps
 * @param logPrefix Prefix for log messages (default: "TAP")
 * @return true if all taps in the sequence were successful
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

/**
 * Clicks an object at specific screen coordinates and selects a menu action
 * @param screenX X coordinate on screen
 * @param screenY Y coordinate on screen
 * @param action The menu action to select (e.g., "Deploy", "Take", "Use")
 * @return true if successful, false otherwise
 */
fun clickAtCoordinates(
    screenX: Int,
    screenY: Int,
    action: String,

): Boolean {
    val randomX = screenX + Random.nextInt(-5, 5)
    val randomY = screenY + Random.nextInt(-5, 5)
    val point = Point(randomX, randomY)

    Game.setSingleTapToggle(true)
    Condition.sleep(Random.nextInt(60,80))
    Input.tap(point)


    // Wait for menu to open
    if (!Condition.wait({ Menu.opened() }, freq = 100, tries = 20)) {
        return false
    }

    // Click the menu action
    val success = Menu.click { cmd ->
        cmd.action.contains(action, ignoreCase = true)
    }
    //Game.setSingleTapToggle(false)
    return success
}