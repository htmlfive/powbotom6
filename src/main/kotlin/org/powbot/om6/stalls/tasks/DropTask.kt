package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.StallThiever
import kotlin.random.Random

class DropTask(script: StallThiever) : Task(script) {
    override fun validate(): Boolean {
        val junkInInventory = Inventory.stream().name(*script.TARGET_ITEM_NAMES_DROP.toTypedArray()).isNotEmpty()
        return (script.DROP_1_MODE && script.justStole && junkInInventory) ||
                (Inventory.isFull() && Inventory.stream().all { it.name() in script.TARGET_ITEM_NAMES_DROP })
    }

    override fun execute() {
        script.logger.info("Dropping junk items...")
        val itemsToDrop = Inventory.stream().name(*script.TARGET_ITEM_NAMES_DROP.toTypedArray()).list()
        itemsToDrop.forEach { item ->
            if (item.interact("Drop")) {
                Condition.sleep(Random.nextInt(50, 150))
            }
        }
        Condition.wait({ Inventory.stream().name(*script.TARGET_ITEM_NAMES_DROP.toTypedArray()).isEmpty() }, 200, 15)
        script.justStole = false
    }
}