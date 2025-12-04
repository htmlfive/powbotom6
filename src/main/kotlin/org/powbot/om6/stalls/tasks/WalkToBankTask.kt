package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.ScriptUtils
import org.powbot.om6.stalls.StallThiever

class WalkToBankTask(script: StallThiever) : Task(script, Constants.TaskNames.WALKING_TO_BANK) {
    override fun validate(): Boolean {
        val fullCheck = Inventory.isFull()
        val itemsToBankCheck = ScriptUtils.hasAnyItem(*script.config.itemsToBank.toTypedArray())
        val distanceCheck = ScriptUtils.distanceToTile(script.config.bankTile) > Constants.Distance.BANK_INTERACTION_RANGE.toDouble()
        val result = fullCheck && itemsToBankCheck && distanceCheck

        script.logger.debug("VALIDATE: ${name}: Inv Full ($fullCheck) | Items To Bank ($itemsToBankCheck) | Too Far From Bank ($distanceCheck). Result: $result")

        return result
    }

    override fun execute() {
        script.logger.info("EXECUTE: ${name}: Inventory full and far from bank. Walking to bank tile: ${script.config.bankTile}")

        ScriptUtils.closeBankIfOpen()

        if (ScriptUtils.walkToTile(script.config.bankTile)) {
            script.logger.debug("EXECUTE: ${name}: Movement to bank initiated.")
        } else {
            script.logger.warn("EXECUTE: ${name}: Failed to initiate movement to bank tile.")
        }
    }
}