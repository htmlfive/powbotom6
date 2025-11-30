package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Tile
import org.powbot.api.rt4.*

/**
 * Common inventory utilities shared across all scripts.
 */
object InventoryUtils {

    /**
     * Checks if the inventory contains an item by name.
     * @param itemName The name of the item to check
     * @return true if at least one item with the name exists
     */
    fun contains(itemName: String): Boolean {
        return Inventory.stream().name(itemName).isNotEmpty()
    }

    /**
     * Checks if the inventory contains an item by ID.
     * @param itemId The ID of the item to check
     * @return true if at least one item with the ID exists
     */
    fun contains(itemId: Int): Boolean {
        return Inventory.stream().id(itemId).isNotEmpty()
    }

    /**
     * Checks if the inventory contains any items from the given names.
     * @param itemNames The names of items to check
     * @return true if at least one item from the list exists
     */
    fun containsAny(vararg itemNames: String): Boolean {
        return Inventory.stream().name(*itemNames).isNotEmpty()
    }

    /**
     * Checks if the inventory contains any items from the given list.
     * @param itemNames List of item names to check
     * @return true if at least one item from the list exists
     */
    fun containsAny(itemNames: List<String>): Boolean {
        return itemNames.any { contains(it) }
    }

    /**
     * Checks if the inventory contains all items from the given names.
     * @param itemNames The names of items to check
     * @return true if all items exist in inventory
     */
    fun containsAll(vararg itemNames: String): Boolean {
        return itemNames.all { contains(it) }
    }

    /**
     * Gets the count of an item by name.
     * @param itemName The name of the item
     * @param includeStacks If true, counts stack sizes
     * @return The count of items
     */
    fun count(itemName: String, includeStacks: Boolean = false): Long {
        return Inventory.stream().name(itemName).count(includeStacks)
    }

    /**
     * Gets the total count of items in inventory.
     * @return The number of occupied inventory slots
     */
    fun totalCount(): Int {
        return Inventory.stream().count().toInt()
    }

    /**
     * Gets the number of empty slots.
     * @return The count of empty inventory slots
     */
    fun emptySlots(): Int {
        return Inventory.emptySlotCount()
    }

    /**
     * Checks if the inventory is full.
     * @return true if no empty slots remain
     */
    fun isFull(): Boolean {
        return Inventory.isFull()
    }

    /**
     * Checks if the inventory is empty.
     * @return true if all slots are empty
     */
    fun isEmpty(): Boolean {
        return Inventory.stream().isEmpty()
    }

    /**
     * Drops all items with the specified names.
     * @param itemNames The names of items to drop
     * @param sleepMin Minimum sleep between drops (default: 80)
     * @param sleepMax Maximum sleep between drops (default: 150)
     * @return true if all items were dropped
     */
    fun dropAll(vararg itemNames: String, sleepMin: Int = 80, sleepMax: Int = 150): Boolean {
        val items = Inventory.stream().name(*itemNames).toList()
        if (items.isEmpty()) return true

        items.forEach { item ->
            if (item.valid()) {
                item.interact("Drop")
                Condition.sleep(Random.nextInt(sleepMin, sleepMax))
            }
        }

        return Condition.wait({ !containsAny(*itemNames) }, 100, 30)
    }

    /**
     * Drops all items from a list of names.
     * @param itemNames List of item names to drop
     * @param sleepMin Minimum sleep between drops
     * @param sleepMax Maximum sleep between drops
     * @return true if all items were dropped
     */
    fun dropAll(itemNames: List<String>, sleepMin: Int = 80, sleepMax: Int = 150): Boolean {
        return dropAll(*itemNames.toTypedArray(), sleepMin = sleepMin, sleepMax = sleepMax)
    }

    /**
     * Gets the first item matching the name.
     * @param itemName The name to search for
     * @return The item or null if not found
     */
    fun getItem(itemName: String): Item? {
        return Inventory.stream().name(itemName).firstOrNull()
    }

    /**
     * Gets the first item matching the ID.
     * @param itemId The ID to search for
     * @return The item or null if not found
     */
    fun getItem(itemId: Int): Item? {
        return Inventory.stream().id(itemId).firstOrNull()
    }

    /**
     * Gets the first item matching any of the names.
     * @param itemNames The names to search for
     * @return The item or null if not found
     */
    fun getItemAny(vararg itemNames: String): Item? {
        return Inventory.stream().name(*itemNames).firstOrNull()
    }

    /**
     * Gets the first item containing the name substring.
     * @param nameContains The substring to search for
     * @return The item or null if not found
     */
    fun getItemContaining(nameContains: String): Item? {
        return Inventory.stream().nameContains(nameContains).firstOrNull()
    }
}
