package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.StallThiever
import kotlin.random.Random

class DropTask(script: StallThiever) : Task(script, "Dropping") {
    override fun validate(): Boolean {
        val junkInInventory = Inventory.stream().name(*script.config.itemsToDrop.toTypedArray()).isNotEmpty()
        return (script.config.drop1Mode && script.justStole && junkInInventory) ||
                (Inventory.isFull() && Inventory.stream().all { it.name() in script.config.itemsToDrop })
    }

    override fun execute() {
        script.logger.info("Dropping junk items...")
        val itemsToDrop = Inventory.stream().name(*script.config.itemsToDrop.toTypedArray()).list()
        itemsToDrop.forEach { item ->
            if (item.interact("Drop")) {
                Condition.sleep(Random.nextInt(50, 150))
            }
        }
        Condition.wait({ Inventory.stream().name(*script.config.itemsToDrop.toTypedArray()).isEmpty() }, 200, 15)
        script.justStole = false
    }
}
