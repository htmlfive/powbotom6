package org.powbot.om6.moths.tasks

import org.powbot.api.Condition
import org.powbot.om6.moths.Constants
import org.powbot.om6.moths.MoonlightMothCatcher
import org.powbot.om6.moths.ScriptUtils

/**
 * Task to hop worlds when another player is detected nearby.
 */
class WorldHopTask(script: MoonlightMothCatcher) : Task(script) {

    override fun activate(): Boolean {
        // Only hop if we're at the moth location and another player is nearby
        return ScriptUtils.isAtMothLocation() && ScriptUtils.isPlayerNearby()
    }

    override fun execute() {
        script.logger.info("HOP: Player detected nearby, hopping worlds")

        val world = ScriptUtils.getValidWorld()
        if (world != null) {
            script.logger.info("HOP: Hopping to world ${world.number}")
            world.hop()
            Condition.wait({ !org.powbot.api.rt4.Players.local().inMotion() }, 100, 50)
            script.logger.info("HOP: World hop complete")
        } else {
            script.logger.warn("HOP: No suitable world found for hopping")
        }
    }

    override fun name(): String = "World Hopping"
}
