package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class ThieveTask(script: StallThiever) : Task(script, Constants.TaskNames.THIEVING) {
    private var stall: GameObject = GameObject.Nil

    override fun validate(): Boolean =
        !Inventory.isFull() &&
                ScriptUtils.isAtTile(script.config.thievingTile) &&
                ScriptUtils.isPlayerIdle()

    override fun execute() {
        if (!stall.valid()) {
            script.logger.info("Searching for stall object...")
            stall = ScriptUtils.findGameObject(
                script.config.stallId,
                script.config.thievingTile,
                Constants.Distance.STALL_SEARCH_RANGE
            )
        }

        if (!stall.valid()) {
            script.logger.warn("Stall not found within range of thieving tile, waiting...")
            Condition.sleep(Constants.Timing.STALL_NOT_FOUND_WAIT)
            return
        }

        if (!ScriptUtils.ensureObjectInView(stall)) {
            script.logger.info("Failed to get stall in viewport.")
            return
        }

        if (ScriptUtils.interactAndWaitForXp(stall, Constants.Actions.STEAL_FROM, Skill.Thieving)) {
            script.justStole = true
            ScriptUtils.randomDelay(
                Constants.Timing.POST_STEAL_MIN_DELAY,
                Constants.Timing.POST_STEAL_MAX_DELAY
            )
        }
    }
}