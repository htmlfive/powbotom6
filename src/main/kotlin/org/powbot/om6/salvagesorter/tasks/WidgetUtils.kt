package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.rt4.Widgets

/**
 * Widget utility functions for extracting and interacting with widget components.
 */
object WidgetUtils {

    /**
     * Gets the text content of a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The text content, or empty string if not available
     */
    fun getText(groupId: Int, componentId: Int): String {
        return Widgets.component(groupId, componentId).text()
    }

    /**
     * Gets the numeric value from a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The parsed integer, or 0 if not available
     */
    fun getNumber(groupId: Int, componentId: Int): Int {
        return Widgets.component(groupId, componentId).text().toIntOrNull() ?: 0
    }

    /**
     * Gets the numeric value from a widget component with a default value.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @param default Default value if parsing fails
     * @return The parsed integer, or default if not available
     */
    fun getNumber(groupId: Int, componentId: Int, default: Int): Int {
        return Widgets.component(groupId, componentId).text().toIntOrNull() ?: default
    }

    /**
     * Checks if a widget component is visible.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return true if the component is visible
     */
    fun isVisible(groupId: Int, componentId: Int): Boolean {
        return Widgets.component(groupId, componentId).visible()
    }

    /**
     * Gets the width of a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The width in pixels
     */
    fun getWidth(groupId: Int, componentId: Int): Int {
        return Widgets.component(groupId, componentId).width()
    }

    /**
     * Gets the height of a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The height in pixels
     */
    fun getHeight(groupId: Int, componentId: Int): Int {
        return Widgets.component(groupId, componentId).height()
    }

    /**
     * Extracts a percentage from a bar-type widget.
     * Useful for progress bars or resource bars.
     * @param groupId The widget group ID
     * @param barComponentId The bar component ID
     * @param fillComponentId The fill component ID
     * @return The percentage (0-100), or null if bar is not visible
     */
    fun getBarPercentage(groupId: Int, barComponentId: Int, fillComponentId: Int): Int? {
        val barComp = Widgets.component(groupId, barComponentId)
        if (barComp.visible()) {
            val barLength = barComp.width()
            val activityBar = Widgets.component(groupId, barComponentId, fillComponentId).width().toDouble()
            return ((activityBar / barLength) * 100).toInt()
        }
        return null
    }

    /**
     * Clicks a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return true if the component was clicked
     */
    fun click(groupId: Int, componentId: Int): Boolean {
        return Widgets.component(groupId, componentId).click()
    }


    /**
     * Gets a child component from a parent widget component.
     * @param groupId The widget group ID
     * @param parentComponentId The parent component ID
     * @param childComponentId The child component ID
     * @return The child component text, or empty string if not available
     */
    fun getChildText(groupId: Int, parentComponentId: Int, childComponentId: Int): String {
        return Widgets.component(groupId, parentComponentId, childComponentId).text()
    }
}