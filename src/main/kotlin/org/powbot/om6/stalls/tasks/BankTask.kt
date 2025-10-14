package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.StallThiever

class BankTask(script: StallThiever) : Task(script) {
    override fun validate(): Boolean = Inventory.isFull() && script.BANK_TILE.distance() <= 5
    override fun execute() {
        if (!Bank.opened() && !Bank.open()) {
            script.logger.warn("Failed to open bank.")
            return
        }

        if (Bank.opened()) {
            script.logger.info("Depositing items...")
            for (itemName in script.TARGET_ITEM_NAMES_BANK) {
                if (Inventory.stream().name(itemName).isNotEmpty()) {
                    if (Bank.deposit(itemName, Bank.Amount.ALL)) {
                        Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 150, 10)
                    }
                }
            }
            if (Inventory.isFull()) {
                Bank.depositInventory()
            }
            Bank.close()
        }
    }
}