package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.om6.stalls.StallThiever

class WalkToStallTask(script: StallThiever) : Task(script) {
    override fun validate(): Boolean = Players.local().tile() != script.THIEVING_TILE && !Players.local().inMotion()
    override fun execute() {
        script.logger.info("Not at thieving tile, walking back...")
        Movement.walkTo(script.THIEVING_TILE)
    }
}