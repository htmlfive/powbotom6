package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.om6.salvager.ShipwreckSalvager

class RespawnWaitTask(script: ShipwreckSalvager) : Task(script) {

    override fun activate(): Boolean {
        val isActive = script.currentPhase == SalvagePhase.WAITING_FOR_RESPAWN
        script.logger.debug("ACTIVATE: Checking if phase is ${SalvagePhase.WAITING_FOR_RESPAWN.name} ($isActive).")
        return isActive
    }

    override fun execute() {
        val endTime = script.phaseStartTime + script.currentRespawnWait

        if (System.currentTimeMillis() >= endTime) {
            script.logger.info("TASK: Respawn wait complete. Time elapsed: ${(System.currentTimeMillis() - script.phaseStartTime) / 1000.0}s. Transitioning.")

            script.currentPhase = SalvagePhase.READY_TO_TAP
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name}.")
        } else {
            val remaining = (endTime - System.currentTimeMillis()) / 1000L
            val totalSeconds = script.currentRespawnWait / 1000L

            script.logger.info("TASK: Respawn wait active. ${remaining}s remaining (Total: ${totalSeconds}s).")

            val sleepTime = Random.nextInt(500, 1000)
            Condition.sleep(sleepTime)
            script.logger.debug("SLEEP: Slept for $sleepTime ms.")
        }
    }
}