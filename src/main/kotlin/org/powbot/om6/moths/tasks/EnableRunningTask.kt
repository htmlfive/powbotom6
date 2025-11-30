package org.powbot.om6.moths.tasks

import org.powbot.api.rt4.Movement
import org.powbot.om6.moths.MoonlightMothCatcher
import org.powbot.om6.moths.ScriptUtils

/**
 * Task to enable running when energy is sufficient.
 */
class EnableRunningTask(script: MoonlightMothCatcher) : Task(script) {

    override fun activate(): Boolean {
        // Activate when not running and have enough energy
        return !Movement.running()
    }

    override fun execute() {
        if (ScriptUtils.enableRunning()) {
            script.logger.info("RUN: Enabled running")
        }
    }

    override fun name(): String = "Enabling Run"
}
