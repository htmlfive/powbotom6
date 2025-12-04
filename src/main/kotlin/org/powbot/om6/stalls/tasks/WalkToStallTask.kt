package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Players
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class WalkToStallTask(script: StallThiever) : Task(script, Constants.TaskNames.WALKING_TO_STALL) {
    override fun validate(): Boolean {
        val notAtTile = !ScriptUtils.isAtTile(script.config.thievingTile)
        val notInMotion = !Players.local().inMotion()
        val result = notAtTile && notInMotion

        script.logger.debug("VALIDATE: ${name}: Not at Thieving Tile ($notAtTile) | Not in Motion ($notInMotion). Result: $result")

        return result
    }

    override fun execute() {
        script.logger.info("EXECUTE: ${name}: Not at thieving tile. Walking back to: ${script.config.thievingTile}")
        if (ScriptUtils.walkToTile(script.config.thievingTile)) {
            script.logger.debug("EXECUTE: ${name}: Movement to stall initiated.")
        } else {
            script.logger.warn("EXECUTE: ${name}: Failed to initiate movement to thieving tile.")
        }
    }
}