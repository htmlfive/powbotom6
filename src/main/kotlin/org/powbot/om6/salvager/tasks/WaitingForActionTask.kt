package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Players
import org.powbot.api.Input // Required for tapping
import org.powbot.om6.salvager.ShipwreckSalvager
import org.powbot.om6.salvager.* // Assuming SalvagePhase is imported or defined here

class WaitingForActionTask(script: ShipwreckSalvager) : Task(script) {

    override fun activate(): Boolean {
        val isActive = script.currentPhase == SalvagePhase.WAITING_FOR_ACTION
        script.logger.debug("ACTIVATE: Checking if phase is ${SalvagePhase.WAITING_FOR_ACTION.name} ($isActive).")
        return isActive
    }

    override fun execute() {
        // --- 1. EXTRACTOR TAP CHECK (Highest Priority) ---
        if (executeExtractorTapCheck()) {
            script.logger.info("TASK: Extractor tap completed during WAITING_FOR_ACTION. Transitioning back to READY_TO_TAP.")

            // Go back to ReadyToTap after the successful extractor tap and wait
            script.currentPhase = SalvagePhase.READY_TO_TAP
            script.phaseStartTime = System.currentTimeMillis()
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name} after extractor tap.")
            return
        }
        // --- END EXTRACTOR TAP CHECK ---

        if (handleDialogueCheck()) {
            script.logger.info("TASK: Dialogue cleared. Restarting action.")

            script.currentPhase = SalvagePhase.READY_TO_TAP
            script.phaseStartTime = System.currentTimeMillis()
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name} after dialogue clear.")
            return
        }

        if (script.salvageMessageFound) {
            script.logger.info("TASK: Salvage message detected via event flow. Moving to drop phase.")
            script.salvageMessageFound = false

            script.currentPhase = SalvagePhase.DROPPING_SALVAGE
            val sleepTime = Random.nextInt(2000, 4000)
            Condition.sleep(sleepTime)
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name}. Slept for $sleepTime ms.")
            return
        }

        val elapsedTime = System.currentTimeMillis() - script.phaseStartTime
        if (elapsedTime > ShipwreckSalvager.ACTION_TIMEOUT_MILLIS) {
            val timeoutSeconds = ShipwreckSalvager.ACTION_TIMEOUT_MILLIS / 1000L
            script.logger.warn("TASK: Salvage action timed out after $timeoutSeconds seconds. Moving to drop phase for safety.")

            script.currentPhase = SalvagePhase.DROPPING_SALVAGE
            val sleepTime = Random.nextInt(2000, 4000)
            Condition.sleep(sleepTime)
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name} due to timeout. Slept for $sleepTime ms.")
            return
        }

        val remaining = (ShipwreckSalvager.ACTION_TIMEOUT_MILLIS - elapsedTime) / 1000L
        script.logger.info("TASK: WAITING_FOR_ACTION. Timeout in ${remaining}s.")
        val sleepTime = Random.nextInt(500, 1000)
        Condition.sleep(sleepTime)
        script.logger.debug("SLEEP: Slept for $sleepTime ms.")
    }

    /**
     * Checks the extractor timer and executes the specific tap if the interval has passed.
     * Logic duplicated from ReadyToTapTask to allow execution during WAITING_FOR_ACTION.
     * @return true if the tap was executed and succeeded, false otherwise.
     */
    private fun executeExtractorTapCheck(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Check if 61 seconds (or more) have passed since the last extractor tap
        if (currentTime - script.extractorTimer >= script.extractorInterval) {
            script.logger.info("EXTRACTOR TIMER: 61-second timer finished. Executing specific tap at (571, 294) with randomization.")

            // Execute the specific tap logic (x=571, y=294) with +-5 randomization
            val x = 571
            val y = 294
            val randomOffsetX = Random.nextInt(-5, 6)
            val randomOffsetY = Random.nextInt(-5, 6)
            val finalX = x + randomOffsetX
            val finalY = y + randomOffsetY

            script.hookCastMessageFound = false
            script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Base: $x, $y | Offset: $randomOffsetX, $randomOffsetY).")
            val clicked = Input.tap(finalX, finalY)

            if (clicked) {
                // Success actions: Restart timer and wait 1800-2400 ms
                script.extractorTimer = currentTime
                val waitTime = Random.nextInt(1800, 2400)
                script.logger.info("WAIT: Extractor tap successful. Waiting $waitTime ms. Timer restarted.")
                Condition.sleep(waitTime)
                return true
            } else {
                script.logger.warn("FAIL: Failed to execute extractor tap at ($finalX, $finalY).")
                return false
            }
        }
        return false
    }

    private fun handleDialogueCheck(): Boolean {
        if (Chat.canContinue()) {
            script.logger.info("DIALOGUE DETECTED: Clicking continue...")

            val sleepBetween = 3
            var count = 0

            while (count < sleepBetween) {
                val sleepTime = Random.nextInt(1000, 2000)
                Condition.sleep(sleepTime)
                script.logger.debug("DIALOGUE: Sleeping $sleepTime ms before next continue click (Count: ${count + 1}).")
                count++
            }

            if (Chat.clickContinue()) {
                script.startTile = Players.local().tile()
                script.logger.info("DIALOGUE: Successfully clicked continue. Updated startTile to: ${script.startTile}")

                val restartWait = Random.nextInt(
                    ShipwreckSalvager.DIALOGUE_RESTART_MIN_MILLIS,
                    ShipwreckSalvager.DIALOGUE_RESTART_MAX_MILLIS
                )
                script.logger.info("DIALOGUE: Sleeping for $restartWait ms before phase transition.")
                Condition.sleep(restartWait)
                return true
            } else {
                script.logger.warn("DIALOGUE: Failed to click continue.")
            }
        }
        return false
    }
}