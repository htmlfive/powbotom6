package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class ThieveTask(script: StallThiever) : Task(script, Constants.TaskNames.THIEVING) {
    private var stall: GameObject = GameObject.Nil

    override fun validate(): Boolean {
        val inventoryCheck = !Inventory.isFull()
        val tileCheck = ScriptUtils.isAtTile(script.config.thievingTile)
        val idleCheck = ScriptUtils.isPlayerIdle()
        val result = inventoryCheck && tileCheck && idleCheck

        script.logger.debug("VALIDATE: ${name}: Inv Not Full ($inventoryCheck) | At Tile ($tileCheck) | Idle ($idleCheck). Result: $result")

        return result
    }

    override fun execute() {
        if (!stall.valid()) {
            script.logger.info("EXECUTE: ${name}: Searching for stall object (ID: ${script.config.stallId})...")
            stall = ScriptUtils.findGameObject(
                script.config.stallId,
                script.config.thievingTile,
                Constants.Distance.STALL_SEARCH_RANGE
            )
        }

        if (!stall.valid()) {
            script.logger.warn("EXECUTE: ${name}: Stall not found within range of thieving tile. Stall may be down or out of range. Waiting...")
            Condition.sleep(Constants.Timing.STALL_NOT_FOUND_WAIT)
            return
        }

        script.logger.debug("EXECUTE: ${name}: Stall object found: ${stall.name()} at ${stall.tile()}.")

        if (!ScriptUtils.ensureObjectInView(stall)) {
            script.logger.info("EXECUTE: ${name}: Failed to get stall in viewport after camera adjustment. Retrying next loop.")
            return
        }

        script.logger.info("EXECUTE: ${name}: Attempting to steal from stall...")
        if (ScriptUtils.interactAndWaitForXp(stall, Constants.Actions.STEAL_FROM, Skill.Thieving)) {
            script.logger.info("EXECUTE: ${name}: Successfully stole from stall and confirmed XP gain.")
            script.justStole = true
            ScriptUtils.randomDelay(
                Constants.Timing.POST_STEAL_MIN_DELAY,
                Constants.Timing.POST_STEAL_MAX_DELAY
            )
        } else {
            script.logger.warn("EXECUTE: ${name}: Failed to interact with stall or failed to confirm XP gain.")
        }
    }
}