package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.om6.stalls.StallThiever

class WalkToStallTask(script: StallThiever) : Task(script, "Walking to Stall") {
    override fun validate(): Boolean = Players.local().tile() != script.config.thievingTile && !Players.local().inMotion()
    override fun execute() {
        script.logger.info("Not at thieving tile, walking back...")
        Movement.walkTo(script.config.thievingTile)
    }
}
