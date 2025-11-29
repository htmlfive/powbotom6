package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class DropTask(script: StallThiever) : Task(script, Constants.TaskNames.DROPPING) {
    override fun validate(): Boolean {
        val junkInInventory = ScriptUtils.hasAnyItem(*script.config.itemsToDrop.toTypedArray())
        return (script.config.drop1Mode && script.justStole && junkInInventory) ||
                (Inventory.isFull() && ScriptUtils.inventoryContainsOnly(script.config.itemsToDrop))
    }

    override fun execute() {
        script.logger.info("Dropping junk items...")
        ScriptUtils.dropItems(*script.config.itemsToDrop.toTypedArray())
        script.justStole = false
    }
}