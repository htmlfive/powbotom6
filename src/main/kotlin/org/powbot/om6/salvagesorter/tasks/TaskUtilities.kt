package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
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
    val widget = Widgets.widget(root).component(component).component(index)
    if (!widget.valid()) return false
    return if (action != null) widget.interact(action) else widget.click()
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
    if (!tapWithOffset(x, y, offsetRange)) return false
    Condition.sleep(Random.nextInt(sleepMin, sleepMax))
    return true
}

// ========================================
// INVENTORY UTILITY FUNCTIONS
// ========================================

/**
 * Ensures inventory tab is open.
 * @param sleepMin Minimum sleep time in milliseconds after opening (default: 200)
 * @param sleepMax Maximum sleep time in milliseconds after opening (default: 400)
 * @return true if inventory is open or was successfully opened
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
    CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

    if (!ensureInventoryOpen()) {
        script.logger.warn("ASSIGNMENT: Failed to open inventory")
    }

    Condition.sleep(mainWait)
    closeTabWithSleep(mainWait, mainWait)

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
