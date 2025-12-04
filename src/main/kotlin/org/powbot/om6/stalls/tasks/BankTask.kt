package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.*

class BankTask(script: StallThiever) : Task(script, Constants.TaskNames.BANKING) {
    override fun validate(): Boolean {
        val fullCheck = Inventory.isFull()
        val distanceCheck = ScriptUtils.distanceToTile(script.config.bankTile) <= Constants.Distance.BANK_INTERACTION_RANGE.toDouble()
        val result = fullCheck && distanceCheck

        script.logger.debug("VALIDATE: ${name}: Inv Full ($fullCheck) | Near Bank ($distanceCheck). Result: $result")

        return result
    }

    override fun execute() {
        if (!Bank.opened()) {
            script.logger.info("EXECUTE: ${name}: Bank is closed, attempting to open...")
            if (!Bank.open()) {
                script.logger.warn("EXECUTE: ${name}: Failed to open bank.")
                return
            }
        }

        if (Bank.opened()) {
            script.logger.info("EXECUTE: ${name}: Bank opened. Depositing configured items: [${script.config.itemsToBank.joinToString()}]")

            if (ScriptUtils.depositItems(*script.config.itemsToBank.toTypedArray())) {
                script.logger.info("EXECUTE: ${name}: Configured items successfully deposited.")
            } else {
                script.logger.error("EXECUTE: ${name}: Failed to deposit all configured items.")
            }

            if (Inventory.isFull()) {
                script.logger.warn("EXECUTE: ${name}: Inventory is still full. Depositing entire inventory as a failsafe.")
                Bank.depositInventory()
            }

            script.logger.info("EXECUTE: ${name}: Closing bank.")
            Bank.close()
        }
    }
}