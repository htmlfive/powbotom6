package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Widgets
import org.powbot.om6.salvager.*

/**
 * Ensures 'Tap-to-drop' is either enabled (true) or disabled (false) based on the script configuration,
 * by clicking the widget and confirming the state change via chat messages handled by the main script.
 */
class TapToDropTask(script: ShipwreckSalvager) : Task(script) {

    // Constants for the widget component (892 is often the Settings widget)
    private companion object {
        const val WIDGET_ID = 892
        const val COMPONENT_ID = 18
        const val ACTION = "Tap-to-drop"
        const val MAX_TOGGLE_ATTEMPTS = 3
    }

    private var toggleAttempts = 0

    /**
     * Activates if the in-game state (`script.isTapToDropEnabled`) does NOT match the desired config (`script.tapToDrop`).
     */
    override fun activate(): Boolean {
        // This is the core synchronization check: desired state (config) != current state (in-game)
        val needsToggle = script.tapToDrop != script.isTapToDropEnabled

        if (!needsToggle) {
            return false // State already matches config.
        }

        // A. Initializing Phase: Always run if there's a mismatch during startup (to ENABLE or DISABLE).
        if (script.currentPhase == SalvagePhase.INITIALIZING) {
            return true
        }

        // B. Dropping Phase: Only run if the user wants it ENABLED AND it is currently disabled.
        // If the user wants it disabled (script.tapToDrop == false), we rely on the initial check.
        if (script.currentPhase == SalvagePhase.DROPPING_SALVAGE) {
            return script.tapToDrop
        }

        return false
    }

    override fun execute() {
        val isInitializing = script.currentPhase == SalvagePhase.INITIALIZING
        // Desired state comes directly from the GUI boolean
        val desiredState = script.tapToDrop // true to enable, false to disable
        val actionDescription = if (desiredState) "ENABLE" else "DISABLE"

        // 1. Check for failure condition
        if (toggleAttempts >= MAX_TOGGLE_ATTEMPTS) {
            val failureAction = if (desiredState) "enabled" else "disabled"
            script.logger.info("Failed to ensure Tap-to-drop is $failureAction after $MAX_TOGGLE_ATTEMPTS attempts. Continuing with safe drop.")

            if (isInitializing) {
                script.currentPhase = SalvagePhase.READY_TO_TAP
                script.logger.info("Initial check failed. Transitioning to READY_TO_TAP.")
            }
            toggleAttempts = 0
            return
        }

        // 2. Perform the interaction attempt
        val phase = if (isInitializing) "INITIALIZING" else "DROPPING_SALVAGE"
        script.logger.info("Attempting to $actionDescription Tap-to-drop (Phase: $phase, Attempt: ${toggleAttempts + 1})")

        val settingComponent = Widgets.widget(WIDGET_ID).component(COMPONENT_ID)

        if (!settingComponent.valid()) {
            script.logger.warn("Widget $WIDGET_ID:$COMPONENT_ID not valid/visible. Cannot check/toggle setting. Make sure the settings menu is open or loaded in the client.")
            Condition.sleep(1000)
            toggleAttempts++
            return
        }

        if (settingComponent.interact(ACTION)) {
            script.logger.info("Clicked '$ACTION' widget component. Waiting for chat confirmation...")

            // Wait for the script state (updated by EventBus) to match the desired state
            Condition.wait({ script.isTapToDropEnabled == desiredState }, 500, 3)

            // 3. Handle success or failure post-wait
            if (script.isTapToDropEnabled == desiredState) {
                script.logger.info("Tap-to-drop confirmed $actionDescription.")

                // If we were initializing, now we transition to the main loop state
                if (isInitializing) {
                    script.currentPhase = SalvagePhase.READY_TO_TAP
                    script.logger.info("Initial check complete. Transitioning to READY_TO_TAP.")
                }

                toggleAttempts = 0 // Reset for future checks
            } else {
                script.logger.warn("Chat confirmation of Tap-to-drop $actionDescription FAILED to arrive after click. Retrying.")
            }
        } else {
            script.logger.warn("Failed to interact with widget component for action '$ACTION'. Retrying.")
            Condition.sleep(Random.nextInt(800, 1500))
        }

        toggleAttempts++ // Count the attempt
    }
}