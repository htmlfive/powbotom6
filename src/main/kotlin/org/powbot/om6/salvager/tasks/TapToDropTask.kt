package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Widgets
import org.powbot.om6.salvager.ShipwreckSalvager

class TapToDropTask(script: ShipwreckSalvager) : Task(script) {

    private companion object {
        const val WIDGET_ID = 892
        const val COMPONENT_ID = 18
        const val ACTION = "Tap-to-drop"
        const val MAX_TOGGLE_ATTEMPTS = 3
    }

    private var toggleAttempts = 0

    override fun activate(): Boolean {
        val needsToggle = script.tapToDrop != script.isTapToDropEnabled
        script.logger.debug("ACTIVATE: Tap-to-drop Check - Desired: ${script.tapToDrop}, Current: ${script.isTapToDropEnabled}. Needs Toggle: $needsToggle.")

        if (!needsToggle) {
            return false
        }

        if (script.currentPhase == SalvagePhase.INITIALIZING) {
            script.logger.debug("ACTIVATE: Task active in INITIALIZING phase (Mismatch detected).")
            return true
        }

        if (script.currentPhase == SalvagePhase.DROPPING_SALVAGE) {
            val activateInDrop = script.tapToDrop // Only run if we need to enable it during drop
            script.logger.debug("ACTIVATE: Task active in DROPPING_SALVAGE phase (Tap-to-drop is desired: $activateInDrop).")
            return activateInDrop
        }

        return false
    }

    override fun execute() {
        val isInitializing = script.currentPhase == SalvagePhase.INITIALIZING
        val desiredState = script.tapToDrop
        val actionDescription = if (desiredState) "ENABLE" else "DISABLE"

        if (toggleAttempts >= MAX_TOGGLE_ATTEMPTS) {
            val failureAction = if (desiredState) "enabled" else "disabled"
            script.logger.info("FAILURE: Failed to ensure Tap-to-drop is $failureAction after $MAX_TOGGLE_ATTEMPTS attempts. Continuing with safe drop.")

            if (isInitializing) {
                script.currentPhase = SalvagePhase.READY_TO_TAP
                script.logger.info("PHASE CHANGE: Initial check failed. Transitioning to ${SalvagePhase.READY_TO_TAP.name}.")
            }
            toggleAttempts = 0
            return
        }

        val phase = if (isInitializing) "INITIALIZING" else "DROPPING_SALVAGE"
        script.logger.info("ACTION: Attempting to $actionDescription Tap-to-drop (Phase: $phase, Attempt: ${toggleAttempts + 1})")

        val settingComponent = Widgets.widget(WIDGET_ID).component(COMPONENT_ID)
        script.logger.debug("WIDGET: Checking widget $WIDGET_ID:$COMPONENT_ID (Valid: ${settingComponent.valid()}).")


        if (!settingComponent.valid()) {
            script.logger.warn("WIDGET FAIL: Widget $WIDGET_ID:$COMPONENT_ID not valid/visible. Cannot check/toggle setting. Sleeping.")
            Condition.sleep(1000)
            toggleAttempts++
            return
        }

        if (settingComponent.interact(ACTION)) {
            script.logger.info("INTERACT: Clicked '$ACTION' widget component. Waiting for chat confirmation...")

            val waitSuccess = Condition.wait({ script.isTapToDropEnabled == desiredState }, 500, 3)

            if (waitSuccess) {
                script.logger.info("SUCCESS: Tap-to-drop confirmed $actionDescription. State matches desired: $desiredState.")

                if (isInitializing) {
                    script.currentPhase = SalvagePhase.READY_TO_TAP
                    script.logger.info("PHASE CHANGE: Initial check complete. Transitioning to ${SalvagePhase.READY_TO_TAP.name}.")
                }

                toggleAttempts = 0
            } else {
                script.logger.warn("WAIT FAIL: Chat confirmation of Tap-to-drop $actionDescription FAILED to arrive after click. Retrying. Current state: ${script.isTapToDropEnabled}")
            }
        } else {
            script.logger.warn("INTERACT FAIL: Failed to interact with widget component for action '$ACTION'. Retrying.")
            Condition.sleep(Random.nextInt(800, 1500))
        }

        toggleAttempts++
    }
}