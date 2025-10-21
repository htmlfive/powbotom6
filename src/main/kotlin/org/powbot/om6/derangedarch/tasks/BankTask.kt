package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.rscache.loader.ItemLoader
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    companion object {
        // --- ITEM IDs TO KEEP ---

        // Ring of dueling(8) down to Ring of dueling(2)
        val DUELING_RING_IDS = listOf(
            2552, // (8)
            2554, // (7)
            2556, // (6)
            2558, // (5)
            2560, // (4)
            2562, // (3)
            2564  // (2)
        )

        // Digsite pendant(5) down to Digsite pendant(2)
        val DIGSITE_PENDANT_IDS = listOf(
            11194, // (5)
            11193, // (4)
            11192, // (3)
            11191  // (2)
        )

        // Prayer potion(4)
        val PRAYER_POTION_4_ID = 2434

        // Combine base items to keep
        val BASE_ITEMS_TO_KEEP = DUELING_RING_IDS + DIGSITE_PENDANT_IDS + PRAYER_POTION_4_ID

        // --- HELPER FUNCTIONS ---

        // Check if an ID is any Ring of Dueling
        fun isDuelingRing(id: Int): Boolean = id in 2552..2566

        // Check if an ID is any Digsite Pendant
        fun isDigsitePendant(id: Int): Boolean = id in 11190..11194
    }

    override fun validate(): Boolean {
        val needsRestock = script.needsFullRestock()
        val emergencyTele = script.emergencyTeleportJustHappened
        val inBankArea = script.FEROX_BANK_AREA.contains(Players.local())

        if (inBankArea && (needsRestock || emergencyTele)) {
            script.logger.debug("Validate OK: In bank area, needsRestock=$needsRestock, emergencyTele=$emergencyTele")
            return true
        }
        return false
    }

    /**
     * Helper function to attempt a bank withdrawal with retries.
     * @return true if successful, false if all attempts fail.
     */
    private fun withdrawWithRetries(id: Int, amount: Int, itemName: String = "Item ID $id"): Boolean {
        for (attempt in 1..3) {
            if (Bank.withdraw(id, amount)) {
                script.logger.debug("Bank.withdraw() successful for $itemName (Attempt $attempt/3)")
                return true // Success
            }
            script.logger.warn("Attempt $attempt/3 failed to withdraw $amount of $itemName (ID: $id). Retrying...")
            Condition.sleep(300) // Small delay between failed attempts
        }
        return false // All retries failed
    }

    override fun execute() {
        script.logger.debug("Executing BankTask...")
        if (!Bank.opened()) {
            script.logger.debug("Bank not open, opening...")
            Bank.open()
            return
        }

        // --- PHASE 1: VERIFY AND WITHDRAW MISMATCHED EQUIPMENT ---

        val requiredIds = script.config.requiredEquipment.keys
        val equippedIds = Equipment.stream().map { it.id }.toList()

        // Find which required items are not currently equipped.
        val missingEquipmentIds = requiredIds.filter { it !in equippedIds }
        script.logger.debug("Required equip: $requiredIds, Worn equip: $equippedIds, Missing: $missingEquipmentIds")

        // If there are any missing items, handle them first.
        if (missingEquipmentIds.isNotEmpty()) {
            script.logger.info("Mismatched equipment detected. Withdrawing missing items...")

            missingEquipmentIds.forEach { id ->
                script.logger.info("Withdrawing item ID: $id")

                // Use the retry logic
                if (!withdrawWithRetries(id, 1, "Equip ID $id")) {
                    script.logger.warn("FATAL: Could not withdraw required equipment ID: $id after 3 attempts. Stopping script.")
                    ScriptManager.stop()
                    return // Exit the execute() method entirely
                }
                // Wait for the item to appear in inventory to ensure it was withdrawn successfully
                Condition.wait({ Inventory.stream().id(id).isNotEmpty() }, 250, 10)
            }

            Bank.close()
            return
        }


        // --- PHASE 2: WITHDRAW INVENTORY (ONLY RUNS IF EQUIPMENT IS ALREADY CORRECT) ---

        script.logger.info("Equipment check passed. Withdrawing inventory supplies...")

        // Deposit inventory, keeping specified teleports, food, and prayer pots.
        if (Inventory.isNotEmpty()) {
            script.logger.debug("Inventory not empty, depositing all except keep-items...")

            // Find the IDs of the food currently in the inventory
            val foodIdsToKeep = Inventory.stream().name(script.config.foodName).map { it.id() }.distinct().toList()
            script.logger.debug("Food IDs to keep: $foodIdsToKeep")

            // Find IDs of any axes
            val axeIdsToKeep = Inventory.stream()
                .filter { it.name().endsWith("axe", ignoreCase = true) }
                .map { it.id() }
                .distinct()
                .toList()
            script.logger.debug("Axe IDs to keep: $axeIdsToKeep")

            // Find IDs of the emergency teleport item
            val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains
            var emergencyTeleIdsToKeep = emptyList<Int>()
            if (emergencyTeleportName != null) {
                emergencyTeleIdsToKeep = Inventory.stream()
                    .nameContains(emergencyTeleportName)
                    .map { it.id() }
                    .distinct()
                    .toList()
                script.logger.debug("Emergency Teleport IDs to keep ($emergencyTeleportName): $emergencyTeleIdsToKeep")
            } else {
                script.logger.warn("Could not find emergency teleport option for: ${script.config.emergencyTeleportItem}")
            }


            // Create the final list of all items to keep
            val finalIdsToKeep = (BASE_ITEMS_TO_KEEP + foodIdsToKeep + axeIdsToKeep + emergencyTeleIdsToKeep).distinct()
            script.logger.debug("Final list of IDs to keep: $finalIdsToKeep")


            Bank.depositAllExcept(*finalIdsToKeep.toIntArray())

            // Wait until the only items left are the ones we want to keep
            Condition.wait({
                Inventory.stream().all { it.id() in finalIdsToKeep }
            }, 300, 10)
        }

        // --- INVENTORY WITHDRAWAL LOOP ---

        // Get the emergency teleport name ONCE before the loop
        val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains

        script.config.requiredInventory.forEach { (id, requiredAmount) ->
            if (requiredAmount <= 0) return@forEach // Skip if amount is zero or less

            // Get item name first to use in skip logic
            val itemName = ItemLoader.lookup(id)?.name() ?: ""
            script.logger.debug("Processing required item: $itemName (ID=$id, Amount=$requiredAmount)")


            // --- Skip withdrawing items if we already have any ---
            if (isDuelingRing(id) && Inventory.stream().nameContains("Ring of dueling").isNotEmpty()) {
                script.logger.info("Skipping Ring of Dueling withdrawal, one already in inventory.")
                return@forEach
            }
            if (isDigsitePendant(id) && Inventory.stream().nameContains("Digsite pendant").isNotEmpty()) {
                script.logger.info("Skipping Digsite Pendant withdrawal, one already in inventory.")
                return@forEach
            }
            // Skip Axe
            if (itemName.endsWith("axe", ignoreCase = true) && Inventory.stream().any { it.name().endsWith("axe", ignoreCase = true) }) {
                script.logger.info("Skipping Axe ($itemName) withdrawal, one already in inventory.")
                return@forEach
            }
            // Skip Emergency Teleport
            if (emergencyTeleportName != null && itemName.contains(emergencyTeleportName, ignoreCase = true)) {
                if (Inventory.stream().nameContains(emergencyTeleportName).isNotEmpty()) {
                    script.logger.info("Skipping Emergency Teleport ($itemName) withdrawal, one already in inventory.")
                    return@forEach
                }
            }

            // --- Calculate missing amount for food and prayer pots ---
            var amountToWithdraw = requiredAmount

            // Check if it's the specific prayer pot or the configured food
            if (id == PRAYER_POTION_4_ID || itemName.equals(script.config.foodName, ignoreCase = true)) {
                val currentAmount = Inventory.stream().id(id).count(true).toInt()
                amountToWithdraw = requiredAmount - currentAmount
                script.logger.debug("Item $itemName (ID: $id) needs partial withdraw. Required: $requiredAmount, Have: $currentAmount, Withdrawing: $amountToWithdraw")


                if (amountToWithdraw <= 0) {
                    script.logger.info("Already have enough $itemName (ID: $id). Skipping withdrawal.")
                    return@forEach // Already have enough, skip to next item
                }
            }

            // --- Withdraw the calculated amount ---
            script.logger.debug("Withdrawing $amountToWithdraw of $itemName (ID: $id)")

            // Use the retry logic
            if (!withdrawWithRetries(id, amountToWithdraw, itemName)) {
                script.logger.warn("FATAL: Could not withdraw $amountToWithdraw of $itemName (ID: $id) after 3 attempts. Stopping script.")
                ScriptManager.stop()
                return@forEach // Stop iterating
            }

            // Wait for the correct count to be in the inventory before moving to the next item
            Condition.wait({ Inventory.stream().id(id).count(true) >= requiredAmount }, 250, 12)
        }

        // --- FINAL STEP: CLOSE AND VERIFY ---
        script.logger.debug("Inventory withdrawal loop finished. Closing bank.")
        if (Bank.close()) {
            script.emergencyTeleportJustHappened = false

            // --- FINAL VERIFICATION ---
            // Wait a moment for inventory/equipment to settle after bank close
            Condition.sleep(300)
            script.logger.debug("Bank closed. Running final verification...")

            if (!script.equipmentIsCorrect()) {
                script.logger.warn("FATAL: Equipment is still incorrect after banking. Stopping script.")
                ScriptManager.stop()
                return
            }

            if (!script.inventoryIsCorrect()) {
                script.logger.warn("FATAL: Inventory is incorrect after banking. Stopping script.")
                ScriptManager.stop()
                return
            }

            script.logger.info("Bank task complete. Equipment and inventory verified.")
        }
    }
}