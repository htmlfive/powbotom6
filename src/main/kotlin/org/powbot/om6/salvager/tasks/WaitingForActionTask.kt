package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Players
import org.powbot.om6.salvager.ShipwreckSalvager



class WaitingForActionTask(script: ShipwreckSalvager) : Task(script) {

    override fun activate(): Boolean {
        val isActive = script.currentPhase == SalvagePhase.WAITING_FOR_ACTION
        script.logger.debug("ACTIVATE: Checking if phase is ${SalvagePhase.WAITING_FOR_ACTION.name} ($isActive).")
        return isActive
    }

    override fun execute() {

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

    private fun handleDialogueCheck(): Boolean {
        if (Chat.canContinue()) {
            script.logger.info("DIALOGUE DETECTED: Clicking continue...")

            val sleepBetween = script.sleepLevel.toInt()
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