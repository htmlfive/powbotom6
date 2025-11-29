package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.*

class BankTask(script: StallThiever) : Task(script, Constants.TaskNames.BANKING) {
    override fun validate(): Boolean =
        Inventory.isFull() &&
                ScriptUtils.distanceToTile(script.config.bankTile) <= Constants.Distance.BANK_INTERACTION_RANGE.toDouble()

    override fun execute() {
        if (!Bank.opened() && !Bank.open()) {
            script.logger.warn("Failed to open bank.")
            return
        }

        if (Bank.opened()) {
            script.logger.info("Depositing items...")
            ScriptUtils.depositItems(*script.config.itemsToBank.toTypedArray())

            if (Inventory.isFull()) {
                Bank.depositInventory()
            }
            Bank.close()
        }
    }
}