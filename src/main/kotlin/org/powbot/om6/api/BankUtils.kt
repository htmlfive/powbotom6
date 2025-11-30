package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players

/**
 * Common banking utilities shared across all scripts.
 */
object BankUtils {

    /**
     * Opens the bank if not already open.
     * @param timeout Maximum time to wait in ms (default: 5000)
     * @return true if bank is now open
     */
    fun open(timeout: Int = 5000): Boolean {
        if (Bank.opened()) return true

        if (Bank.open()) {
            return Condition.wait({ Bank.opened() }, 100, timeout / 100)
        }
        return false
    }

    /**
     * Closes the bank if open.
     * @return true if bank is now closed
     */
    fun close(): Boolean {
        if (!Bank.opened()) return true

        if (Bank.close()) {
            return Condition.wait({ !Bank.opened() }, 100, 20)
        }
        return false
    }

    /**
     * Closes the bank if it's currently open (no return value).
     */
    fun closeIfOpen() {
        if (Bank.opened()) {
            Bank.close()
        }
    }

    /**
     * Checks if near a bank.
     * @return true if within 10 tiles of nearest bank
     */
    fun isNearBank(): Boolean {
        return Players.local().tile().distanceTo(Bank.nearest().tile()) <= 10
    }

    /**
     * Deposits all items with the specified name.
     * @param itemName The name of items to deposit
     * @return true if items were deposited
     */
    fun deposit(itemName: String): Boolean {
        if (!InventoryUtils.contains(itemName)) return true

        if (Bank.deposit(itemName, Bank.Amount.ALL)) {
            return Condition.wait({ !InventoryUtils.contains(itemName) }, 100, 30)
        }
        return false
    }

    /**
     * Deposits all items with the specified names.
     * @param itemNames The names of items to deposit
     * @return true if all items were deposited
     */
    fun depositAll(vararg itemNames: String): Boolean {
        var success = true
        itemNames.forEach { name ->
            if (!deposit(name)) {
                success = false
            }
        }
        return success
    }

    /**
     * Deposits entire inventory.
     * @return true if inventory is now empty
     */
    fun depositInventory(): Boolean {
        if (Inventory.stream().isEmpty()) return true

        if (Bank.depositInventory()) {
            return Condition.wait({ Inventory.stream().isEmpty() }, 100, 30)
        }
        return false
    }

    /**
     * Withdraws items by name.
     * @param itemName The name of items to withdraw
     * @param amount The amount to withdraw (use Bank.Amount constants)
     * @return true if items were withdrawn
     */
    fun withdraw(itemName: String, amount: Bank.Amount = Bank.Amount.ALL): Boolean {
        if (Bank.withdraw(itemName, amount)) {
            return Condition.wait({ InventoryUtils.contains(itemName) }, 100, 30)
        }
        return false
    }

    /**
     * Withdraws items by ID.
     * @param itemId The ID of items to withdraw
     * @param amount The amount to withdraw
     * @return true if items were withdrawn
     */
    fun withdraw(itemId: Int, amount: Int): Boolean {
        if (Bank.withdraw(itemId, amount)) {
            return Condition.wait({ InventoryUtils.contains(itemId) }, 100, 30)
        }
        return false
    }

    /**
     * Withdraws items by ID with retry logic.
     * @param itemId The ID of items to withdraw
     * @param amount The amount to withdraw
     * @param maxRetries Maximum number of attempts (default: 3)
     * @return true if items were withdrawn
     */
    fun withdrawWithRetry(itemId: Int, amount: Int, maxRetries: Int = 3): Boolean {
        for (attempt in 1..maxRetries) {
            if (Bank.withdraw(itemId, amount)) {
                if (Condition.wait({ InventoryUtils.contains(itemId) }, 100, 30)) {
                    return true
                }
            }
            Condition.sleep(Random.nextInt(200, 400))
        }
        return false
    }

    /**
     * Checks if the bank contains an item.
     * @param itemName The name of item to check
     * @return true if bank contains the item
     */
    fun bankContains(itemName: String): Boolean {
        return Bank.stream().name(itemName).isNotEmpty()
    }

    /**
     * Checks if the bank contains an item by ID.
     * @param itemId The ID of item to check
     * @return true if bank contains the item
     */
    fun bankContains(itemId: Int): Boolean {
        return Bank.stream().id(itemId).isNotEmpty()
    }
}
