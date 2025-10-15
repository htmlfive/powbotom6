package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EatTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean = Combat.healthPercent() < script.config.eatAtPercent

    override fun execute() {
        val food = Inventory.stream().name(script.config.foodName).firstOrNull()
        if (food != null && food.interact("Eat")) {
            Condition.wait({ Combat.healthPercent() > script.config.eatAtPercent + 10 }, 150, 5)
        }
    }
}