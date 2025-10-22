package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.rscache.loader.ItemLoader
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    companion object {
        // --- SECTION 1: "DO NOT DEPOSIT" (KEEP) LIST ---
        // Add static item IDs here to keep them when depositing.
        // Dynamic items (food, axes, emergency tele) are handled in execute().

        // Ring of dueling(8) down to Ring of dueling(2)
        private val KEEP_DUELING_RING_IDS = (2552..2564 step 2).toList() // 2552, 2554, ..., 2564
        // Digsite pendant(5) down to Digsite pendant(2)
        private val KEEP_DIGSITE_PENDANT_IDS = (11191..11194).toList() // 11191, 11192, 11193, 11194
        // Prayer potion(4)
        private const val KEEP_PRAYER_POTION_4_ID = 2434
        // Antipoison(1-4)
        private val KEEP_ANTIPOISON_IDS = listOf(2446, 175, 177, 179)

        /**
         * The final, easily editable list of STATIC items to not deposit.
         * These items will be kept by Bank.depositAllExcept().
         */
        val BASE_ITEMS_TO_KEEP = (
                KEEP_DUELING_RING_IDS +
                        KEEP_DIGSITE_PENDANT_IDS +
                        KEEP_ANTIPOISON_IDS +
                        KEEP_PRAYER_POTION_4_ID
                ).distinct()

        // --- SECTION 2: "DO NOT WITHDRAW" (SKIP) LOGIC HELPERS ---
        // These are used to identify item *types* to skip withdrawing if we already have one.

        // All Dueling Rings (1-8)
        private val ALL_DUELING_RING_IDS = 2552..2566
        const val DUELING_RING_NAME_CONTAINS = "Ring of dueling"
        fun isDuelingRing(id: Int): Boolean = id in ALL_DUELING_RING_IDS

        // All Digsite Pendants (1-5)
        private val ALL_DIGSITE_PENDANT_IDS = 11190..11194
        const val DIGSITE_PENDANT_NAME_CONTAINS = "Digsite pendant"
        fun isDigsitePendant(id: Int): Boolean = id in ALL_DIGSITE_PENDANT_IDS

        // All Antidotes (1-4)
        val ALL_ANTIPOISON_IDS = KEEP_ANTIPOISON_IDS // Re-use the list from above
        fun isAntipoison(id: Int): Boolean = id in ALL_ANTIPOISON_IDS

        // Axe identifier
        private const val AXE_NAME_SUFFIX = "axe"
        fun isAxe(name: String): Boolean = name.endsWith(AXE_NAME_SUFFIX, ignoreCase = true)

        // Rune identifier
        private const val RUNE_NAME_SUFFIX = " rune"
        fun isRune(name: String): Boolean = name.endsWith(RUNE_NAME_SUFFIX, ignoreCase = true)

        // Prayer Potion (4) ID for withdrawal logic
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

    /**
     * --- MODIFIED: Centralized "Do Not Withdraw" logic ---
     * Checks if we should skip withdrawing an item because we already have one.
     * This is the "easily editable" place for skip logic.
     * @param script Needs script instance to check config for runes
     */
    private fun shouldSkipWithdrawal(
        id: Int,
        itemName: String,
        emergencyTeleName: String?,
        script: DerangedArchaeologistMagicKiller
    ): Boolean {
        // --- MODIFIED: Removed the specific rune skip logic from here ---
        // It's now handled in the partial withdrawal block

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
        // Skip Antipoison
        if (isAntipoison(id) && Inventory.stream().any { it.id() in ALL_ANTIPOISON_IDS }) {
            script.logger.info("Skipping Antipoison withdrawal, one already in inventory.")
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
        // Did not match any skip rules
        return false
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

        val missingEquipmentIds = requiredIds.filter { it !in equippedIds }
        script.logger.debug("Required equip: $requiredIds, Worn equip: $equippedIds, Missing: $missingEquipmentIds")

        if (missingEquipmentIds.isNotEmpty()) {
            script.logger.info("Mismatched equipment detected. Withdrawing missing items...")

            if (Inventory.isNotEmpty()) {
                script.logger.info("Depositing full inventory to make space for equipment...")
                if (Bank.depositInventory()) {
                    script.logger.debug("Waiting for inventory to be empty after deposit...")
                    Condition.wait({ Inventory.isEmpty() }, 300, 10)
                } else {
                    script.logger.warn("Bank.depositInventory() failed! Proceeding anyway...")
                }
            }

            missingEquipmentIds.forEach { id ->
                script.logger.info("Withdrawing item ID: $id")
                if (!withdrawWithRetries(id, 1, "Equip ID $id")) {
                    script.logger.warn("FATAL: Could not withdraw required equipment ID: $id after 3 attempts. Stopping script.")
                    ScriptManager.stop()
                    return
                }
                Condition.wait({ Inventory.stream().id(id).isNotEmpty() }, 250, 10)
            }

            Bank.close()
            return
        }


        // --- PHASE 2: WITHDRAW INVENTORY (ONLY RUNS IF EQUIPMENT IS ALREADY CORRECT) ---

        script.logger.info("Equipment check passed. Withdrawing inventory supplies...")

        if (Inventory.isNotEmpty()) {
            script.logger.debug("Inventory not empty, depositing all except keep-items...")

            // --- Find dynamic items to keep (food, axe, tele, AND runes) ---
            val foodIdsToKeep = Inventory.stream().name(script.config.foodName).map { it.id() }.distinct().toList()
            script.logger.debug("Food IDs to keep: $foodIdsToKeep")

            val axeIdsToKeep = Inventory.stream()
                .filter { isAxe(it.name()) }
                .map { it.id() }
                .distinct()
                .toList()
            script.logger.debug("Axe IDs to keep: $axeIdsToKeep")

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

            // --- Find rune IDs from the required inventory to keep ---
            val runeIdsToKeep = script.config.requiredInventory.keys.filter {
                val name = ItemLoader.lookup(it)?.name() ?: ""
                isRune(name)
            }
            script.logger.debug("Rune IDs to keep (from GUI): $runeIdsToKeep")


            // Create the final list of all items to keep
            // This combines the static list (BASE_ITEMS_TO_KEEP) with the dynamic ones
            val finalIdsToKeep = (
                    BASE_ITEMS_TO_KEEP +
                            foodIdsToKeep +
                            axeIdsToKeep +
                            emergencyTeleIdsToKeep +
                            runeIdsToKeep // <-- ADDED RUNES
                    ).distinct()
            script.logger.debug("Final list of IDs to keep: $finalIdsToKeep")

            Bank.depositAllExcept(*finalIdsToKeep.toIntArray())

            Condition.wait({
                Inventory.stream().all { it.id() in finalIdsToKeep }
            }, 300, 10)
        }

        // --- INVENTORY WITHDRAWAL LOOP ---

        val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains

        script.config.requiredInventory.forEach { (id, requiredAmount) ->
            if (requiredAmount <= 0) return@forEach

            val itemName = ItemLoader.lookup(id)?.name() ?: ""
            script.logger.debug("Processing required item: $itemName (ID=$id, Amount=$requiredAmount)")


            // --- Centralized skip logic ---
            // Call the new helper function to see if we should skip this item
            if (shouldSkipWithdrawal(id, itemName, emergencyTeleportName, script)) {
                return@forEach // Skip to the next item in the loop
            }


            // --- Calculate missing amount for food, prayer pots, AND RUNES ---
            var amountToWithdraw = requiredAmount

            // --- MODIFIED ---
            // Check if it's a "top-up" item (food, prayer pot, or a configured rune)
            if (id == PRAYER_POTION_4_ID
                || itemName.equals(script.config.foodName, ignoreCase = true)
                || (isRune(itemName) && script.config.requiredInventory.containsKey(id))
            ) {
                // --- END MODIFIED ---
                val currentAmount = Inventory.stream().id(id).count(true).toInt()
                amountToWithdraw = requiredAmount - currentAmount
                script.logger.debug("Item $itemName (ID: $id) needs partial withdraw. Required: $requiredAmount, Have: $currentAmount, Withdrawing: $amountToWithdraw")

                if (amountToWithdraw <= 0) {
                    script.logger.info("Already have enough $itemName (ID: $id). Skipping withdrawal.")
                    return@forEach
                }
            }

            // --- Withdraw the calculated amount ---
            script.logger.debug("Withdrawing $amountToWithdraw of $itemName (ID: $id)")

            if (!withdrawWithRetries(id, amountToWithdraw, itemName)) {
                script.logger.warn("FATAL: Could not withdraw $amountToWithdraw of $itemName (ID: $id) after 3 attempts. Stopping script.")
                ScriptManager.stop()
                return@forEach
            }

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

            if (!script.inventoryIsCorrect()) {
                script.logger.warn("FATAL: Inventory is incorrect after banking. Stopping script.")
                ScriptManager.stop()
                return
            }

            script.logger.info("Bank task complete. Equipment and inventory verified.")
        }
    }
}