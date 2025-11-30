package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item

/**
 * Common equipment and combat utilities shared across all scripts.
 */
object EquipmentUtils {

    // ========================================
    // EQUIPMENT CHECKS
    // ========================================

    /**
     * Checks if an item is equipped by name.
     * @param itemName The item name
     * @return true if item is equipped
     */
    fun isEquipped(itemName: String): Boolean {
        return Equipment.stream().name(itemName).isNotEmpty()
    }

    /**
     * Checks if an item is equipped by ID.
     * @param itemId The item ID
     * @return true if item is equipped
     */
    fun isEquipped(itemId: Int): Boolean {
        return Equipment.stream().id(itemId).isNotEmpty()
    }

    /**
     * Checks if an item containing text is equipped.
     * @param nameContains Text the item name should contain
     * @return true if matching item is equipped
     */
    fun isEquippedContaining(nameContains: String): Boolean {
        return Equipment.stream().nameContains(nameContains).isNotEmpty()
    }

    /**
     * Gets the item in a specific equipment slot.
     * @param slot The equipment slot
     * @return The item or Item.Nil if empty
     */
    fun getItemAt(slot: Equipment.Slot): Item {
        return Equipment.itemAt(slot)
    }

    // ========================================
    // EQUIPPING
    // ========================================

    /**
     * Equips an item from inventory.
     * @param itemName The item name
     * @return true if item was equipped
     */
    fun equip(itemName: String): Boolean {
        val item = Inventory.stream().name(itemName).firstOrNull() ?: return false
        return equipItem(item)
    }

    /**
     * Equips an item from inventory by ID.
     * @param itemId The item ID
     * @return true if item was equipped
     */
    fun equip(itemId: Int): Boolean {
        val item = Inventory.stream().id(itemId).firstOrNull() ?: return false
        return equipItem(item)
    }

    /**
     * Equips an item with the appropriate action.
     * @param item The item to equip
     * @return true if item was equipped
     */
    private fun equipItem(item: Item): Boolean {
        if (!item.valid()) return false

        val actions = item.actions()
        val action = listOf("Wield", "Wear", "Equip").find { it in actions } ?: return false

        if (item.interact(action)) {
            return Condition.wait({ isEquipped(item.id()) }, 100, 20)
        }
        return false
    }

    // ========================================
    // COMBAT STATUS
    // ========================================

    /**
     * Gets current health as percentage.
     * @return Health percentage (0-100)
     */
    fun healthPercent(): Int {
        return Combat.healthPercent()
    }

    /**
     * Gets current health points.
     * @return Current HP
     */
    fun currentHealth(): Int {
        return Combat.health()
    }

    /**
     * Checks if health is below threshold.
     * @param percent The threshold percentage
     * @return true if health is below threshold
     */
    fun healthBelow(percent: Int): Boolean {
        return healthPercent() < percent
    }

    /**
     * Checks if player is poisoned.
     * @return true if poisoned
     */
    fun isPoisoned(): Boolean {
        return Combat.isPoisoned()
    }

    /**
     * Gets the current special attack energy.
     * @return Special attack percentage (0-100)
     */
    fun specialEnergy(): Int {
        return Combat.specialPercentage()
    }

    /**
     * Checks if special attack is enabled.
     * @return true if special is active
     */
    fun isSpecialEnabled(): Boolean {
        return Combat.specialAttack()
    }

    /**
     * Toggles special attack.
     * @param enable true to enable, false to disable
     * @return true if toggled successfully
     */
    fun setSpecialAttack(enable: Boolean): Boolean {
        if (Combat.specialAttack() == enable) return true
        return Combat.specialAttack(enable)
    }

    // ========================================
    // EATING
    // ========================================

    /**
     * Eats food by name.
     * @param foodName The food name
     * @return true if food was eaten
     */
    fun eat(foodName: String): Boolean {
        val food = Inventory.stream().name(foodName).firstOrNull() ?: return false
        return eatItem(food)
    }

    /**
     * Eats the first available food item.
     * @param foodNames List of food names to try
     * @return true if food was eaten
     */
    fun eatAny(vararg foodNames: String): Boolean {
        val food = Inventory.stream().name(*foodNames).firstOrNull() ?: return false
        return eatItem(food)
    }

    /**
     * Eats a food item.
     * @param food The food item
     * @return true if eaten
     */
    private fun eatItem(food: Item): Boolean {
        if (!food.valid()) return false

        val healthBefore = currentHealth()
        if (food.interact("Eat")) {
            Condition.wait({ currentHealth() > healthBefore }, 100, 30)
            Condition.sleep(Random.nextInt(200, 400))
            return true
        }
        return false
    }

    /**
     * Drinks a potion by name.
     * @param potionName The potion name (or partial name)
     * @return true if potion was drunk
     */
    fun drink(potionName: String): Boolean {
        val potion = Inventory.stream().nameContains(potionName).firstOrNull() ?: return false
        if (potion.interact("Drink")) {
            Condition.sleep(Random.nextInt(200, 400))
            return true
        }
        return false
    }
}
