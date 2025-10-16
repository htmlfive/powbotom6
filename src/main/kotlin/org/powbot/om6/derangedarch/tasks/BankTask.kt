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

        Bank.depositInventory()
        Condition.sleep(600)

        script.logger.info("Withdrawing required equipment...")
        script.config.requiredEquipment.forEach { (id, _) ->
            if (Equipment.stream().id(id).isEmpty()) {
                // If withdrawing fails, stop the script.
                if (!Bank.withdraw(id, 1)) {
                    script.logger.warn("CRITICAL: Could not withdraw required equipment ID: $id. Stopping script.")
                    ScriptManager.stop()
                    return@forEach // Exit the loop
                }
                Condition.sleep(600)
            }
        }

        script.logger.info("Withdrawing required inventory...")
        script.config.requiredInventory.forEach { (id, amount) ->
            // If withdrawing fails, stop the script.
            if (!Bank.withdraw(id, amount)) {
                script.logger.warn("CRITICAL: Could not withdraw required inventory item ID: $id. Stopping script.")
                ScriptManager.stop()
                return@forEach // Exit the loop
            }
            Condition.sleep(600)
        }

        if (Bank.close()) {
            script.emergencyTeleportJustHappened = false
        }
    }
}