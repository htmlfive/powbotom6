package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Players
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class WalkToStallTask(script: StallThiever) : Task(script, Constants.TaskNames.WALKING_TO_STALL) {
    override fun validate(): Boolean =
        !ScriptUtils.isAtTile(script.config.thievingTile) &&
                !Players.local().inMotion()

    override fun execute() {
        script.logger.info("Not at thieving tile, walking back...")
        ScriptUtils.walkToTile(script.config.thievingTile)
    }
}