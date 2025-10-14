package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.stalls.StallThiever
import kotlin.random.Random

class ThieveTask(script: StallThiever) : Task(script) {
    private val STEAL_ACTION = "Steal-from"
    private var stall: GameObject = GameObject.Nil

    override fun validate(): Boolean = !Inventory.isFull() &&
            Players.local().tile() == script.THIEVING_TILE &&
            Players.local().animation() == -1

    override fun execute() {
        if (!stall.valid()) {
            script.logger.info("Searching for stall object...")
            stall = Objects.stream()
                .id(script.STALL_ID)
                .within(script.THIEVING_TILE, 3.0)
                .nearest()
                .firstOrNull() ?: GameObject.Nil
        }
        if (!stall.valid()) {
            script.logger.warn("Stall not found within range of thieving tile, waiting...")
            Condition.sleep(600)
            return
        }

        if (!stall.inViewport()) {
            script.logger.info("Stall not in view, turning camera.")
            Camera.turnTo(stall)
        }

        val initialXp = Skills.experience(Skill.Thieving)
        if (stall.interact(STEAL_ACTION)) {
            if (Condition.wait({ Skills.experience(Skill.Thieving) > initialXp }, 150, 20)) {
                script.justStole = true
                Condition.sleep(Random.nextInt(150, 250))
            }
        }
    }
}