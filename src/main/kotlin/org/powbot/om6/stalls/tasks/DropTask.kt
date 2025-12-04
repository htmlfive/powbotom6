package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class DropTask(script: StallThiever) : Task(script, Constants.TaskNames.DROPPING) {
    override fun validate(): Boolean {
        val itemsToDrop = script.config.itemsToDrop.toTypedArray()
        val junkInInventory = ScriptUtils.hasAnyItem(*itemsToDrop)

        val drop1Condition = script.config.drop1Mode && script.justStole && junkInInventory
        val dropFullCondition = Inventory.isFull() && ScriptUtils.inventoryContainsOnly(script.config.itemsToDrop)

        val result = drop1Condition || dropFullCondition

        script.logger.debug("VALIDATE: ${name}: Drop-1 Check ($drop1Condition) | Drop-Full Check ($dropFullCondition). Result: $result")
        script.logger.debug("VALIDATE: ${name}: Drop-1: (Mode=${script.config.drop1Mode}, Just Stole=${script.justStole}, Junk In Inv=${junkInInventory})")
        script.logger.debug("VALIDATE: ${name}: Drop-Full: (Inv Full=${Inventory.isFull()}, Only Junk=${ScriptUtils.inventoryContainsOnly(script.config.itemsToDrop)})")

        return result
    }

    override fun execute() {
        val itemsToDrop = script.config.itemsToDrop.toTypedArray()
        script.logger.info("EXECUTE: ${name}: Dropping junk items: [${itemsToDrop.joinToString()}]")
        if (!Inventory.opened()){
            Inventory.open()
            script.logger.info("EXECUTE: Opened inventory because it wasnt open.")
        }
        if (ScriptUtils.dropItems(*itemsToDrop)) {
            script.logger.info("EXECUTE: ${name}: Drop actions successfully initiated and items removed.")
        } else {
            script.logger.warn("EXECUTE: ${name}: Failed to complete all drop actions or confirm removal from inventory.")
        }

        script.justStole = false
    }
}