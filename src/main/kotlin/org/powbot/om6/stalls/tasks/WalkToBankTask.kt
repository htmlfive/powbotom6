package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class WalkToBankTask(script: StallThiever) : Task(script, Constants.TaskNames.WALKING_TO_BANK) {
    override fun validate(): Boolean =
        Inventory.isFull() &&
                ScriptUtils.hasAnyItem(*script.config.itemsToBank.toTypedArray()) &&
                ScriptUtils.distanceToTile(script.config.bankTile) > Constants.Distance.BANK_INTERACTION_RANGE.toDouble()

    override fun execute() {
        script.logger.info("Inventory full, walking to bank...")
        ScriptUtils.closeBankIfOpen()
        ScriptUtils.walkToTile(script.config.bankTile)
    }
}