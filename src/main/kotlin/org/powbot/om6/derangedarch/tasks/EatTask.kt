package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EatTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean {
        val healthPercent = Combat.healthPercent()
        val eatAtPercent = script.config.eatAtPercent
        val shouldEat = healthPercent < eatAtPercent

        if (shouldEat) {
            script.logger.debug("Validate OK: Health ($healthPercent%) is below threshold ($eatAtPercent%).")
        }
        return shouldEat
    }

    override fun execute() {
        script.logger.debug("Executing EatTask...")
        script.logger.info("Health low, eating ${script.config.foodName}.")

        val food = Inventory.stream().name(script.config.foodName).firstOrNull()

        if (food != null) {
            if (food.interact("Eat")) {
                Condition.wait({ Combat.healthPercent() > script.config.eatAtPercent + 10 || Combat.healthPercent() > script.config.eatAtPercent }, 150, 10)
            } else {
                script.logger.warn("Found food (${food.name()}) but failed to interact 'Eat'.")
            }
        } else {
            script.logger.warn("Attempted to eat but no ${script.config.foodName} found in inventory.")
        }
    }
}
