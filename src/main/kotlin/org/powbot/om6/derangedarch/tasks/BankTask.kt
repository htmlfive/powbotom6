package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean {
        // Validate if we are in the bank area AND we need a restock or just teleported.
        return script.FEROX_BANK_AREA.contains(Players.local()) && (script.needsFullRestock() || script.emergencyTeleportJustHappened)
    }

    override fun execute() {
        if (!Bank.opened()) {
            Bank.open()
            return
        }

        // --- PHASE 1: VERIFY AND WITHDRAW MISMATCHED EQUIPMENT ---

        val requiredIds = script.config.requiredEquipment.keys
        val equippedIds = Equipment.stream().map { it.id }.toList()

        // Find which required items are not currently equipped.
        val missingEquipmentIds = requiredIds.filter { it !in equippedIds }

        // If there are any missing items, handle them first.
        if (missingEquipmentIds.isNotEmpty()) {
            script.logger.info("Mismatched equipment detected. Withdrawing missing items...")

            missingEquipmentIds.forEach { id ->
                script.logger.info("Withdrawing item ID: $id")
                // MODIFIED: If withdraw fails, log a warning and exit the loop instead of stopping the script.
                if (!Bank.withdraw(id, 1)) {
                    script.logger.warn("WARNING: Could not withdraw required equipment ID: $id. Exiting equipment withdrawal.")
                    return@forEach // Exit the loop on failure
                }
                // Wait for the item to appear in inventory to ensure it was withdrawn successfully
                Condition.wait({ Inventory.stream().id(id).isNotEmpty() }, 250, 10)
            }

            // IMPORTANT: Close the bank to allow the EquipItemsTask to run.
            // The banking process will "restart" on the next task loop.
            Bank.close()
            return
        }


        // --- PHASE 2: WITHDRAW INVENTORY (ONLY RUNS IF EQUIPMENT IS ALREADY CORRECT) ---

        script.logger.info("Equipment check passed. Withdrawing inventory supplies...")

        // Deposit inventory for a clean restock.
        if (Inventory.isNotEmpty()) {
            Bank.depositInventory()
            Condition.wait({ Inventory.isEmpty() }, 300, 10)
        }

        script.config.requiredInventory.forEach { (id, amount) ->
            if (amount <= 0) return@forEach // Skip if amount is zero or less

            // MODIFIED: If withdraw fails, log a warning and exit the loop instead of stopping the script.
            if (!Bank.withdraw(id, amount)) {
                script.logger.warn("WARNING: Could not withdraw required inventory item ID: $id. Exiting inventory withdrawal.")
                return@forEach // Exit the loop on failure
            }
            // Wait for the correct count to be in the inventory before moving to the next item
            Condition.wait({ Inventory.stream().id(id).count(true) >= amount }, 250, 12)
        }

        // Final step, close the bank and reset the emergency flag.
        if (Bank.close()) {
            script.emergencyTeleportJustHappened = false
        }
    }
}