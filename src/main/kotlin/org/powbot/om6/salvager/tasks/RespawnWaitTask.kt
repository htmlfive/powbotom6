package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Task responsible for waiting a randomized time for the shipwreck to respawn.
 */
class RespawnWaitTask(private val script: ShipwreckSalvager) : Task {

    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.WAITING_FOR_RESPAWN
    }

    override fun execute() {
        if (System.currentTimeMillis() >= script.phaseStartTime + script.currentRespawnWait) {
            script.logger.info("TASK: Respawn wait complete. Ready to tap again.")
            // --- Phase Transition ---
            script.currentPhase = SalvagePhase.READY_TO_TAP
        } else {
            val remaining = (script.phaseStartTime + script.currentRespawnWait - System.currentTimeMillis()) / 1000L
            script.logger.info("TASK: Respawn wait active. ${remaining}s remaining (Total: ${script.currentRespawnWait / 1000L}s).")
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }
}