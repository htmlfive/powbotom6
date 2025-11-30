package org.powbot.om6.moths.tasks

import org.powbot.om6.moths.Constants
import org.powbot.om6.moths.MoonlightMothCatcher
import org.powbot.om6.moths.ScriptUtils

/**
 * Task to travel to the moth catching location.
 */
class TravelToMothsTask(script: MoonlightMothCatcher) : Task(script) {

    override fun activate(): Boolean {
        // Activate when we have jars but are not near moths
        return ScriptUtils.hasJars() && !ScriptUtils.isNearMoths()
    }

    override fun execute() {
        script.logger.info("TRAVEL: Moving to moth location")

        // Check if we need to climb down stairs
        val stairsDown = ScriptUtils.findStairs(Constants.ACTION_CLIMB_DOWN)
        if (stairsDown != null && stairsDown.valid()) {
            script.logger.info("TRAVEL: Climbing down stairs")
            ScriptUtils.climbStairs(Constants.ACTION_CLIMB_DOWN)
        }

        // Traverse path to moths
        script.logger.info("TRAVEL: Walking to moth area")
        ScriptUtils.traverseToMoths()

        script.logger.info("TRAVEL: Arrived at moth location")
    }

    override fun name(): String = "Traveling to Moths"
}
