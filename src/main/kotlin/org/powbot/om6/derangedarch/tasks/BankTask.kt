package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean {
        return script.FEROX_BANK_AREA.contains(Players.local()) && (script.needsFullRestock() || script.emergencyTeleportJustHappened)
    }

    override fun execute() {
        if (!Bank.opened()) {
            Bank.open()
            return
        }
        script.hasAttemptedPoolDrink = false
        // Deposit inventory
        Bank.depositInventory()
        // Wait for the inventory to clear, then add the delay
        if (Condition.wait({ Inventory.isEmpty() }, 150, 15)) {
            Condition.sleep(300)
        }


        script.logger.info("Withdrawing required equipment...")
        script.config.requiredEquipment.forEach { (id, _) ->
            if (Equipment.stream().id(id).isEmpty()) {
                if (Bank.withdraw(id, 1)) {
                    // Add delay after withdrawing an item
                    Condition.sleep(300)
                } else {
                    script.logger.warn("Could not withdraw equipment ID: $id. Stopping.")
                    ScriptManager.stop()
                    return@forEach
                }
            }
        }

        script.logger.info("Withdrawing required inventory...")
        script.config.requiredInventory.forEach { (id, amount) ->
            if (Bank.withdraw(id, amount)) {
                // Add delay after withdrawing an item
                Condition.sleep(300)
            } else {
                script.logger.warn("Could not withdraw inventory item ID: $id.")
            }
        }

        if (Bank.close()) {
            script.emergencyTeleportJustHappened = false
            script.initialCheckCompleted = true
        }
    }
}