package org.powbot.om6.moths.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Players
import org.powbot.om6.moths.Constants
import org.powbot.om6.moths.MoonlightMothCatcher
import org.powbot.om6.moths.ScriptUtils

/**
 * Task to catch moonlight moths.
 */
class CatchMothTask(script: MoonlightMothCatcher) : Task(script) {

    override fun activate(): Boolean {
        // Activate when we have jars and are near moths
        return ScriptUtils.hasJars() && ScriptUtils.isNearMoths()
    }

    override fun execute() {
        val moth = ScriptUtils.findValidMoth()

        if (moth == null || !moth.valid()) {
            script.logger.info("CATCH: No valid moth found above Y-axis ${Constants.MINIMUM_Y_AXIS}")
            return
        }

        // Turn camera if moth not in viewport
        if (!moth.inViewport()) {
            script.logger.info("CATCH: Turning camera to moth")
            Camera.turnTo(moth)
            return
        }

        // Catch the moth
        if (moth.interact(Constants.ACTION_CATCH)) {
            script.logger.info("CATCH: Catching moonlight moth")
            script.mothsCaught++

            // Wait for catch to complete
            Condition.wait(
                { !moth.valid() || Players.local().animation() != -1 },
                100,
                30
            )
            Condition.sleep(Random.nextInt(Constants.CATCH_SLEEP_MIN, Constants.CATCH_SLEEP_MAX))
        }
    }

    override fun name(): String = "Catching Moths"
}
