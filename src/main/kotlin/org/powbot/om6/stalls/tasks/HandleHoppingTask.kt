package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class HandleHoppingTask(script: StallThiever) : Task(script, Constants.TaskNames.HOPPING) {
    override fun validate(): Boolean =
        script.config.enableHopping &&
                ScriptUtils.isAtTile(script.config.thievingTile) &&
                ScriptUtils.isPlayerOnMyTile()

    override fun execute() {
        val randomWorld = ScriptUtils.findRandomWorld()

        if (randomWorld != null) {
            script.logger.info("Player detected on tile. Hopping to world ${randomWorld.number}.")
            ScriptUtils.hopToWorld(randomWorld)
        } else {
            script.logger.warn("Player detected, but no suitable world found for hopping.")
            Condition.sleep(Constants.Timing.HOPPING_FAILED_WAIT)
        }
    }
}