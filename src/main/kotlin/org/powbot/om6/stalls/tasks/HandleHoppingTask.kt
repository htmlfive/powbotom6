package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class HandleHoppingTask(script: StallThiever) : Task(script, Constants.TaskNames.HOPPING) {
    override fun validate(): Boolean {
        val hoppingEnabled = script.config.enableHopping
        val atTile = ScriptUtils.isAtTile(script.config.thievingTile)
        val playerOnTile = ScriptUtils.isPlayerOnMyTile()
        val result = hoppingEnabled && atTile && playerOnTile

        script.logger.debug("VALIDATE: ${name}: Hopping Enabled ($hoppingEnabled) | At Thieve Tile ($atTile) | Player On Tile ($playerOnTile). Result: $result")

        return result
    }

    override fun execute() {
        script.logger.info("EXECUTE: ${name}: Player detected on tile. Searching for a new world to hop to.")
        val randomWorld = ScriptUtils.findRandomWorld()

        if (randomWorld != null) {
            script.logger.info("EXECUTE: ${name}: Found world ${randomWorld.number}. Attempting hop...")
            if (ScriptUtils.hopToWorld(randomWorld)) {
                script.logger.info("EXECUTE: ${name}: Successfully hopped to world ${randomWorld.number}.")
            } else {
                script.logger.error("EXECUTE: ${name}: Failed to execute world hop to world ${randomWorld.number}.")
            }
        } else {
            script.logger.warn("EXECUTE: ${name}: Player detected, but no suitable world found for hopping. Waiting and retrying.")
            Condition.sleep(Constants.Timing.HOPPING_FAILED_WAIT)
        }
    }
}