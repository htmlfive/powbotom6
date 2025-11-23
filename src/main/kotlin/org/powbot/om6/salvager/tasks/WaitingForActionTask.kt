package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Task responsible for waiting for the salvage completion message or handling dialogue.
 * Handles both dialogue interruption and the main action timeout.
 */
class WaitingForActionTask(private val script: ShipwreckSalvager) : Task {

    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.WAITING_FOR_ACTION
    }

    override fun execute() {
        // 1. Dialogue Check (Highest Priority Interruption)
        if (handleDialogueCheck()) {
            script.logger.info("TASK: Dialogue cleared. Restarting action.")
            // --- Phase Transition ---
            script.currentPhase = SalvagePhase.READY_TO_TAP
            script.phaseStartTime = System.currentTimeMillis()
            return
        }

        // 2. Message Event Check (Successful Completion)
        if (script.salvageMessageFound) {
            script.logger.info("TASK: Salvage message detected via event flow. Moving to drop phase.")
            script.salvageMessageFound = false
            // --- Phase Transition ---
            script.currentPhase = SalvagePhase.DROPPING_SALVAGE
            Condition.sleep(Random.nextInt(2000, 4000))
            return
        }

        // 3. Action Timeout Check
        val elapsedTime = System.currentTimeMillis() - script.phaseStartTime
        if (elapsedTime > ShipwreckSalvager.ACTION_TIMEOUT_MILLIS) {
            val timeoutSeconds = ShipwreckSalvager.ACTION_TIMEOUT_MILLIS / 1000L
            script.logger.warn("TASK: Salvage action timed out after $timeoutSeconds seconds. Moving to drop phase for safety.")
            // --- Phase Transition ---
            script.currentPhase = SalvagePhase.DROPPING_SALVAGE
            Condition.sleep(Random.nextInt(2000, 4000))
            return
        }

        // 4. Continue Waiting
        val remaining = (ShipwreckSalvager.ACTION_TIMEOUT_MILLIS - elapsedTime) / 1000L
        script.logger.info("TASK: WAITING_FOR_ACTION. Timeout in ${remaining}s.")
        Condition.sleep(Random.nextInt(500, 1000))
    }

    private fun handleDialogueCheck(): Boolean {
        if (Chat.canContinue()) {
            script.logger.info("DIALOGUE DETECTED: Clicking continue...")

            var count = 0
            val sleepBetween = 3

            while (count < sleepBetween) {
                Condition.sleep(Random.nextInt(1000, 2000))
                count++
            }

            if (Chat.clickContinue()) {
                script.startTile = Players.local().tile()
                script.logger.info("Updated startTile after dialogue to: ${script.startTile}")

                Condition.sleep(
                    Random.nextInt(
                        ShipwreckSalvager.DIALOGUE_RESTART_MIN_MILLIS,
                        ShipwreckSalvager.DIALOGUE_RESTART_MAX_MILLIS
                    )
                )
                return true
            }
        }
        return false
    }
}