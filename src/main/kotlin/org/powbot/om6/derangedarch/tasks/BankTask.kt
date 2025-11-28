package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.rscache.loader.ItemLoader
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.Helpers

class BankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    companion object {
        val BASE_ITEMS_TO_KEEP = (
                Constants.KEEP_DUELING_RING_IDS +
                        Constants.KEEP_DIGSITE_PENDANT_IDS +
                        Constants.PRAYER_POTION_4_ID
                ).distinct()
    }

    override fun validate(): Boolean {
        val needsRestock = script.needsFullRestock()
        val emergencyTele = script.emergencyTeleportJustHappened
        val inBankArea = Constants.FEROX_BANK_AREA.contains(Players.local())

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
                return true
            }
            script.logger.warn("Attempt $attempt/3 failed to withdraw $amount of $itemName (ID: $id). Retrying...")
            Helpers.sleepRandom(300)
        }
        return false
    }

    private fun shouldSkipWithdrawal(
        id: Int,
        itemName: String,
        emergencyTeleName: String?,
        script: DerangedArchaeologistMagicKiller
    ): Boolean {
        if (Helpers.isDuelingRing(id) && Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).isNotEmpty()) {
            script.logger.info("Skipping Ring of Dueling withdrawal, one already in inventory.")
            return true
        }
        if (Helpers.isDigsitePendant(id) && Inventory.stream().nameContains(Constants.DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty()) {
            script.logger.info("Skipping Digsite Pendant withdrawal, one already in inventory.")
            return true
        }
        if (Helpers.isAxe(itemName) && Inventory.stream().any { Helpers.isAxe(it.name()) }) {
            script.logger.info("Skipping Axe ($itemName) withdrawal, one already in inventory.")
            return true
        }
        if (emergencyTeleName != null && itemName.contains(emergencyTeleName, ignoreCase = true)) {
            if (Inventory.stream().nameContains(emergencyTeleName).isNotEmpty()) {
                script.logger.info("Skipping Emergency Teleport ($itemName) withdrawal, one already in inventory.")
                return true
            }
        }
        return false
    }

    private fun isInventorySetupCorrect(): Boolean {
        script.logger.debug("Running custom inventory verification...")
        val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains

        for ((id, requiredAmount) in script.config.requiredInventory) {
            if (requiredAmount <= 0) continue

            val itemName = ItemLoader.lookup(id)?.name() ?: "Item $id"
            var itemFound = false

            if (Helpers.isDuelingRing(id)) {
                if (Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).isNotEmpty()) itemFound = true
            } else if (Helpers.isDigsitePendant(id)) {
                if (Inventory.stream().nameContains(Constants.DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty()) itemFound = true
            } else if (Helpers.isAxe(itemName)) {
                if (Inventory.stream().any { Helpers.isAxe(it.name()) }) itemFound = true
            } else if (emergencyTeleportName != null && itemName.contains(emergencyTeleportName, ignoreCase = true)) {
                if (!Helpers.isDuelingRing(id) && !Helpers.isDigsitePendant(id) &&
                    Inventory.stream().nameContains(emergencyTeleportName).isNotEmpty()) {
                    itemFound = true
                } else if (Helpers.isDuelingRing(id) || Helpers.isDigsitePendant(id)) {
                    if (Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).isNotEmpty() ||
                        Inventory.stream().nameContains(Constants.DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty()) {
                        itemFound = true
                    }
                }
            } else {
                val currentAmount = Inventory.stream().id(id).count(true)
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

        // Phase 1: Verify and withdraw mismatched equipment
        val requiredIds = script.config.requiredEquipment.keys
        val missingEquipmentIds = requiredIds.filter {
            val item = Equipment.itemAt(Equipment.Slot.forIndex(script.config.requiredEquipment[it] ?: -1) ?: return@filter true)
            if (Helpers.isDuelingRing(it)) !item.name().contains(Constants.DUELING_RING_NAME_CONTAINS)
            else if (Helpers.isDigsitePendant(it)) !item.name().contains(Constants.DIGSITE_PENDANT_NAME_CONTAINS)
            else item.id != it
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
                val waitCondition: () -> Boolean = if (Helpers.isDuelingRing(id)) {
                    { Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).isNotEmpty() }
                } else if (Helpers.isDigsitePendant(id)) {
                    { Inventory.stream().nameContains(Constants.DIGSITE_PENDANT_NAME_CONTAINS).isNotEmpty() }
                } else {
                    { Inventory.stream().id(id).isNotEmpty() }
                }
                Condition.wait(waitCondition, 250, 10)
            }
            Bank.close()
            return
        }

        // Phase 2: Withdraw inventory
        script.logger.info("Equipment check passed. Withdrawing inventory supplies...")

        if (Inventory.isNotEmpty()) {
            script.logger.debug("Depositing inventory, keeping essential items...")
            val foodIdsToKeep = Inventory.stream().name(script.config.foodName).map { it.id() }.distinct().toList()
            val axeIdsToKeep = Inventory.stream().filter { Helpers.isAxe(it.name()) }.map { it.id() }.distinct().toList()
            val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains
            var emergencyTeleIdsToKeep = emptyList<Int>()
            if (emergencyTeleportName != null) {
                emergencyTeleIdsToKeep = Inventory.stream().nameContains(emergencyTeleportName).map { it.id() }.distinct().toList()
            }
            val runeIdsToKeep = script.config.requiredInventory.keys.filter { Helpers.isRune(ItemLoader.lookup(it)?.name() ?: "") }

            val finalIdsToKeep = (BASE_ITEMS_TO_KEEP + foodIdsToKeep + axeIdsToKeep + emergencyTeleIdsToKeep + runeIdsToKeep).distinct()
            script.logger.debug("Final list of IDs to keep: $finalIdsToKeep")
            Bank.depositAllExcept(*finalIdsToKeep.toIntArray())
            Condition.wait({ Inventory.stream().all { it.id() in finalIdsToKeep } }, 300, 10)
        }

        // Withdrawal loop
        val emergencyTeleportName = script.teleportOptions[script.config.emergencyTeleportItem]?.itemNameContains
        script.config.requiredInventory.forEach { (id, requiredAmount) ->
            if (requiredAmount <= 0) return@forEach

            val itemName = ItemLoader.lookup(id)?.name() ?: ""
            script.logger.debug("Processing required item: $itemName (ID=$id, Amount=$requiredAmount)")

            if (shouldSkipWithdrawal(id, itemName, emergencyTeleportName, script)) {
                return@forEach
            }

            var amountToWithdraw = requiredAmount
            var isTopUpItem = false

            if (id == Constants.PRAYER_POTION_4_ID ||
                itemName.equals(script.config.foodName, ignoreCase = true) ||
                (Helpers.isRune(itemName) && script.config.requiredInventory.containsKey(id)))
            {
                isTopUpItem = true
                val currentAmount = Inventory.stream().id(id).count(true).toInt()
                amountToWithdraw = requiredAmount - currentAmount
                script.logger.debug("Item $itemName (ID: $id) needs partial withdraw. Required: $requiredAmount, Have: $currentAmount, Withdrawing: $amountToWithdraw")
            }

            if (isTopUpItem && amountToWithdraw <= 0) {
                script.logger.info("Already have enough $itemName (ID: $id). Skipping withdrawal.")
                return@forEach
            }

            if (!isTopUpItem) {
                val currentAmount = Inventory.stream().id(id).count(true).toInt()
                amountToWithdraw = requiredAmount - currentAmount
                if (amountToWithdraw <= 0) {
                    script.logger.info("Already have enough $itemName (ID: $id). Skipping withdrawal.")
                    return@forEach
                }
                script.logger.debug("Standard Item $itemName (ID: $id). Required: $requiredAmount, Have: $currentAmount, Withdrawing: $amountToWithdraw")
            }

            script.logger.debug("Withdrawing $amountToWithdraw of $itemName (ID: $id)")
            if (!withdrawWithRetries(id, amountToWithdraw, itemName)) {
                script.logger.warn("FATAL: Could not withdraw $amountToWithdraw of $itemName (ID: $id) after 3 attempts. Stopping script.")
                ScriptManager.stop()
                return@forEach
            }

            Condition.wait({ Inventory.stream().id(id).count(true) >= requiredAmount }, 250, 12)
        }

        // Final step: Close and verify
        script.logger.debug("Inventory withdrawal loop finished. Closing bank.")
        if (Bank.close()) {
            script.emergencyTeleportJustHappened = false
            Helpers.sleepRandom(300)
            script.logger.debug("Bank closed. Running final verification...")

            if (!script.equipmentIsCorrect()) {
                script.logger.warn("FATAL: Equipment is still incorrect after banking. Stopping script.")
                ScriptManager.stop()
                return
            }

            if (!isInventorySetupCorrect()) {
                script.logger.warn("FATAL: Inventory is incorrect after banking (Smart Check). Stopping script.")
                ScriptManager.stop()
                return
            }

            script.logger.info("Bank task complete. Equipment and inventory verified.")
        }
    }
}
