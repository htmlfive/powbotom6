package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.om6.stalls.*

class BankTask(script: StallThiever) : Task(script, "Banking") {
    override fun validate(): Boolean = Inventory.isFull() && script.config.bankTile.distance() <= 5

    override fun execute() {
        if (!Bank.opened() && !Bank.open()) {
            script.logger.warn("Failed to open bank.")
            return
        }

        if (Bank.opened()) {
            script.logger.info("Depositing items...")
            for (itemName in script.config.itemsToBank) {
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

