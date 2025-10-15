package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean = script.FEROX_BANK_AREA.contains(Players.local()) && script.needsSupplies()

    override fun execute() {
        // --- ADDED THIS LINE ---
        // Reset the pool drink flag at the start of every bank trip.
        script.hasAttemptedPoolDrink = false

        if (!Bank.opened()) { Bank.open(); return }

        Bank.depositInventory()
        Condition.sleep(600)

        script.logger.info("Withdrawing required equipment...")
        script.config.requiredEquipment.forEach { (id, amount) ->
            if (Equipment.stream().id(id).isEmpty()) {
                if (!Bank.withdraw(id, amount)) {
                    script.logger.warn("Could not withdraw equipment ID: $id. Stopping."); ScriptManager.stop(); return@forEach
                }
            }
        }
        script.logger.info("Withdrawing required inventory...")
        script.config.requiredInventory.forEach { (id, amount) ->
            if (!Bank.withdraw(id, amount)) {
                script.logger.warn("Could not withdraw inventory item ID: $id.")
            }
        }
        Bank.close()
    }
}