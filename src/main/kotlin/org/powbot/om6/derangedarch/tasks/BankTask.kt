package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.rscache.loader.ItemLoader
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    companion object {
        // --- SECTION 1: "DO NOT DEPOSIT" (KEEP) LIST ---
        // Antipoison IDs removed
        private val KEEP_DUELING_RING_IDS = (2552..2564 step 2).toList()
        private val KEEP_DIGSITE_PENDANT_IDS = (11191..11194).toList()
        private const val KEEP_PRAYER_POTION_4_ID = 2434
        // private val KEEP_ANTIPOISON_IDS = listOf(2446, 175, 177, 179) // REMOVED

        val BASE_ITEMS_TO_KEEP = (
                KEEP_DUELING_RING_IDS +
                        KEEP_DIGSITE_PENDANT_IDS +
                        // KEEP_ANTIPOISON_IDS + // REMOVED
                        KEEP_PRAYER_POTION_4_ID
                ).distinct()

        // --- SECTION 2: "DO NOT WITHDRAW" (SKIP) LOGIC HELPERS ---
        private val ALL_DUELING_RING_IDS = 2552..2566
        const val DUELING_RING_NAME_CONTAINS = "Ring of dueling"
        fun isDuelingRing(id: Int): Boolean = id in ALL_DUELING_RING_IDS

        private val ALL_DIGSITE_PENDANT_IDS = 11190..11194
        const val DIGSITE_PENDANT_NAME_CONTAINS = "Digsite pendant"
        fun isDigsitePendant(id: Int): Boolean = id in ALL_DIGSITE_PENDANT_IDS

        // val ALL_ANTIPOISON_IDS = KEEP_ANTIPOISON_IDS // REMOVED
        // fun isAntipoison(id: Int): Boolean = id in ALL_ANTIPOISON_IDS // REMOVED

        private const val AXE_NAME_SUFFIX = "axe"
        fun isAxe(name: String): Boolean = name.endsWith(AXE_NAME_SUFFIX, ignoreCase = true)

        private const val RUNE_NAME_SUFFIX = " rune"
        fun isRune(name: String): Boolean = name.endsWith(RUNE_NAME_SUFFIX, ignoreCase = true)

        const val PRAYER_POTION_4_ID = KEEP_PRAYER_POTION_4_ID
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

    /**
     * Centralized "Do Not Withdraw" logic for items where having *any* means we skip.
     */
    private fun shouldSkipWithdrawal(
        id: Int,
        itemName: String,
        emergencyTeleName: String?,
        script: DerangedArchaeologistMagicKiller
    ): Boolean {
        // Skip Dueling Ring
        if (isDuelingRing(id) && Inventory.stream().nameContains(DUELING_RING_NAME_CONTAINS).isNotEmpty()) {
            script.logger.info("Skipping Ring of Dueling withdrawal, one already in inventory.")
            return true
        }
        // Skip Digsite Pendant
        if (isDigsitePendant(id) && Inventory.stream().nameContains(DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty()) {
            script.logger.info("Skipping Digsite Pendant withdrawal, one already in inventory.")
            return true
        }
        // Skip Axe
        if (isAxe(itemName) && Inventory.stream().any { isAxe(it.name()) }) {
            script.logger.info("Skipping Axe ($itemName) withdrawal, one already in inventory.")
            return true
        }
        // Skip Emergency Teleport
        if (emergencyTeleName != null && itemName.contains(emergencyTeleName, ignoreCase = true)) {
            if (Inventory.stream().nameContains(emergencyTeleName).isNotEmpty()) {
                script.logger.info("Skipping Emergency Teleport ($itemName) withdrawal, one already in inventory.")
                return true
            }
        }
        return false
    }

    /**
     * Custom verification function that's "smart" about item types.
     * REMOVED smart antipoison check.
     */
    private fun isInventorySetupCorrect(): Boolean {
        script.logger.debug("Running custom inventory verification...")
        val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains

        for ((id, requiredAmount) in script.config.requiredInventory) {
            if (requiredAmount <= 0) continue // Skip items with 0 requirement

            val itemName = ItemLoader.lookup(id)?.name() ?: "Item $id"
            var itemFound = false

            // --- Smart Checks for item "groups" ---
            // Antipoison check removed
            if (isDuelingRing(id)) {
                if (Inventory.stream().nameContains(DUELING_RING_NAME_CONTAINS).isNotEmpty()) itemFound = true
            } else if (isDigsitePendant(id)) {
                if (Inventory.stream().nameContains(DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty()) itemFound = true
            } else if (isAxe(itemName)) {
                if (Inventory.stream().any { isAxe(it.name()) }) itemFound = true
            } else if (emergencyTeleportName != null && itemName.contains(emergencyTeleportName, ignoreCase = true)) {
                // Avoid double-checking if tele is ring/pendant
                if (!isDuelingRing(id) && !isDigsitePendant(id) &&
                    Inventory.stream().nameContains(emergencyTeleportName).isNotEmpty()) {
                    itemFound = true
                } else if (isDuelingRing(id) || isDigsitePendant(id)) {
                    // Already handled above, mark as found if present
                    if (Inventory.stream().nameContains(DUELING_RING_NAME_CONTAINS).isNotEmpty() ||
                        Inventory.stream().nameContains(DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty()) {
                        itemFound = true
                    }
                }
            }
            // --- Standard Check ---
            else {
                val currentAmount = Inventory.stream().id(id).count(true)
                // Use >= for items where having more is okay (like runes, food, pots)
                if (currentAmount >= requiredAmount) itemFound = true
            }

            if (!itemFound) {
                script.logger.warn("Smart Inventory check FAIL: Item $itemName (ID: $id) not found or insufficient amount (Need >= $requiredAmount).")
                return false
            }
        }
        script.logger.debug("Custom inventory verification PASS.")
        return true
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
        val missingEquipmentIds = requiredIds.filter {
            val item = Equipment.itemAt(Equipment.Slot.forIndex(script.config.requiredEquipment[it] ?: -1) ?: return@filter true) // Basic check
            // Smart check for rings/pendants before exact ID check
            if (isDuelingRing(it)) !item.name().contains(DUELING_RING_NAME_CONTAINS)
            else if (isDigsitePendant(it)) !item.name().contains(DIGSITE_PENDANT_NAME_CONTAINS)
            else item.id != it // Standard ID check
        }

        if (missingEquipmentIds.isNotEmpty()) {
            script.logger.info("Mismatched equipment detected. Withdrawing missing items...")
            if (Inventory.isNotEmpty() && Bank.depositInventory()) {
                Condition.wait({ Inventory.isEmpty() }, 300, 10)
            } else if (Inventory.isNotEmpty()) {
                script.logger.warn("Bank.depositInventory() failed! Proceeding anyway...")
            }

            missingEquipmentIds.forEach { id ->
                script.logger.info("Withdrawing item ID: $id")
                if (!withdrawWithRetries(id, 1, "Equip ID $id")) {
                    script.logger.warn("FATAL: Could not withdraw required equipment ID: $id after 3 attempts. Stopping script.")
                    ScriptManager.stop()
                    return
                }
                // Wait for *any* variant if it's a ring/pendant
                val waitCondition: () -> Boolean = if (isDuelingRing(id)) {
                    { Inventory.stream().nameContains(DUELING_RING_NAME_CONTAINS).isNotEmpty() }
                } else if (isDigsitePendant(id)) {
                    { Inventory.stream().nameContains(DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty() }
                } else {
                    { Inventory.stream().id(id).isNotEmpty() }
                }
                Condition.wait(waitCondition, 250, 10)
            }
            Bank.close()
            return
        }

        // --- PHASE 2: WITHDRAW INVENTORY ---
        script.logger.info("Equipment check passed. Withdrawing inventory supplies...")

        // --- Deposit Except ---
        if (Inventory.isNotEmpty()) {
            script.logger.debug("Depositing inventory, keeping essential items...")
            val foodIdsToKeep = Inventory.stream().name(script.config.foodName).map { it.id() }.distinct().toList()
            val axeIdsToKeep = Inventory.stream().filter { isAxe(it.name()) }.map { it.id() }.distinct().toList()
            val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains
            var emergencyTeleIdsToKeep = emptyList<Int>()
            if (emergencyTeleportName != null) {
                emergencyTeleIdsToKeep = Inventory.stream().nameContains(emergencyTeleportName).map { it.id() }.distinct().toList()
            }
            val runeIdsToKeep = script.config.requiredInventory.keys.filter { isRune(ItemLoader.lookup(it)?.name() ?: "") }

            // BASE_ITEMS_TO_KEEP no longer includes antipoison
            val finalIdsToKeep = (BASE_ITEMS_TO_KEEP + foodIdsToKeep + axeIdsToKeep + emergencyTeleIdsToKeep + runeIdsToKeep).distinct()
            script.logger.debug("Final list of IDs to keep: $finalIdsToKeep")
            Bank.depositAllExcept(*finalIdsToKeep.toIntArray())
            Condition.wait({ Inventory.stream().all { it.id() in finalIdsToKeep } }, 300, 10)
        }

        // --- Withdrawal Loop ---
        val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains
        script.config.requiredInventory.forEach { (id, requiredAmount) ->
            if (requiredAmount <= 0) return@forEach

            val itemName = ItemLoader.lookup(id)?.name() ?: ""
            script.logger.debug("Processing required item: $itemName (ID=$id, Amount=$requiredAmount)")

            // Skip items where having *any* is enough (Rings, Pendants, Axes, Emergency Tele)
            if (shouldSkipWithdrawal(id, itemName, emergencyTeleportName, script)) {
                return@forEach
            }

            // Calculate missing amount for "top-up" items (Food, Prayer Potions, Runes)
            // Antipoison is now handled like a standard item below
            var amountToWithdraw = requiredAmount
            var isTopUpItem = false

            if (id == PRAYER_POTION_4_ID ||
                itemName.equals(script.config.foodName, ignoreCase = true) ||
                (isRune(itemName) && script.config.requiredInventory.containsKey(id)))
            {
                isTopUpItem = true
                val currentAmount = Inventory.stream().id(id).count(true).toInt()
                amountToWithdraw = requiredAmount - currentAmount
                script.logger.debug("Item $itemName (ID: $id) needs partial withdraw. Required: $requiredAmount, Have: $currentAmount, Withdrawing: $amountToWithdraw")
            }
            // REMOVED Antipoison specific block

            // Check if we need to skip withdrawal based on calculated amount
            if (isTopUpItem && amountToWithdraw <= 0) {
                script.logger.info("Already have enough $itemName (ID: $id). Skipping withdrawal.")
                return@forEach
            }

            // If it wasn't a top-up item, we withdraw the full required amount (unless skipped above)
            // This now includes Antipoison specified in the config
            if (!isTopUpItem) {
                val currentAmount = Inventory.stream().id(id).count(true).toInt()
                amountToWithdraw = requiredAmount - currentAmount // Calculate needed amount
                if (amountToWithdraw <= 0) {
                    script.logger.info("Already have enough $itemName (ID: $id). Skipping withdrawal.")
                    return@forEach
                }
                script.logger.debug("Standard Item $itemName (ID: $id). Required: $requiredAmount, Have: $currentAmount, Withdrawing: $amountToWithdraw")
            }


            // Withdraw the calculated amount
            script.logger.debug("Withdrawing $amountToWithdraw of $itemName (ID: $id)")
            if (!withdrawWithRetries(id, amountToWithdraw, itemName)) {
                script.logger.warn("FATAL: Could not withdraw $amountToWithdraw of $itemName (ID: $id) after 3 attempts. Stopping script.")
                ScriptManager.stop()
                return@forEach
            }

            // Standard wait condition for specific item/amount
            // Removed smart antipoison wait
            Condition.wait({ Inventory.stream().id(id).count(true) >= requiredAmount }, 250, 12)
        }

        // --- FINAL STEP: CLOSE AND VERIFY ---
        script.logger.debug("Inventory withdrawal loop finished. Closing bank.")
        if (Bank.close()) {
            script.emergencyTeleportJustHappened = false
            Condition.sleep(300)
            script.logger.debug("Bank closed. Running final verification...")

            if (!script.equipmentIsCorrect()) {
                script.logger.warn("FATAL: Equipment is still incorrect after banking. Stopping script.")
                ScriptManager.stop()
                return
            }

            // Use the smart check (which no longer has special antipoison logic)
            if (!isInventorySetupCorrect()) {
                script.logger.warn("FATAL: Inventory is incorrect after banking (Smart Check). Stopping script.")
                ScriptManager.stop()
                return
            }

            script.logger.info("Bank task complete. Equipment and inventory verified.")
        }
    }
}