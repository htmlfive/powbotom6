package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    /**
     * Validates if the boss is dead and if there are valuable items to loot.
     * Valuable items are:
     * 1. Any single item/stack worth > 1000 gp (excluding bones).
     * 2. Numulite, but only if we already have some in our inventory.
     */
    override fun validate(): Boolean {
        // Task is only valid if we are in the fight area and the boss is NOT present.
        if (script.getBoss() != null || Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8) {
            return false
        }

        // Check for any item (not bones) with a GE value > 1000 gp.
        val hasValuableLoot = GroundItems.stream().within(Players.local(), 15).any {
            it.name() != "Bones" && (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > 1000
        }

        // Check if we are in a state to loot Numulite (we have some in inventory and there is some on the ground).
        val canLootNumulite = Inventory.stream().name("Numulite").isNotEmpty() && GroundItems.stream().name("Numulite").isNotEmpty()

        return hasValuableLoot || canLootNumulite
    }

    override fun execute() {
        // Determine if we are in a state where we should be looting Numulite.
        val shouldLootNumulite = Inventory.stream().name("Numulite").isNotEmpty()

        // Find the nearest lootable item based on our rules.
        val itemToLoot = GroundItems.stream()
            .within(Players.local(), 15)
            .filter {
                val itemName = it.name()
                // Condition 1: The item must not be "Bones".
                itemName != "Bones" &&
                        // Condition 2: The item is valuable OR it's Numulite and we have given it permission to be looted.
                        ( (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > 1000 || (itemName == "Numulite" && shouldLootNumulite) )
            }
            .minByOrNull { it.distance() }

        if (itemToLoot == null) {
            script.logger.info("Valuable loot may have disappeared before it could be picked up.")
            return
        }

        // If inventory is full, try to eat food to make space.
        if (Inventory.isFull()) {
            val food = Inventory.stream().name(script.config.foodName).firstOrNull()
            if (food != null) {
                script.logger.info("Inventory full, eating ${food.name()} to make space for loot.")
                if (food.interact("Eat")) {
                    Condition.wait({ !Inventory.isFull() }, 150, 10)
                }
            } else {
                script.logger.warn("Inventory full and no food to eat for space! Skipping loot.")
                return
            }
        }

        // After potentially eating, if we have space, loot the item.
        if (!Inventory.isFull()) {
            script.logger.info("Looting ${itemToLoot.name()} (Value: ${GrandExchange.getItemPrice(itemToLoot.id())})")
            val inventoryCount = Inventory.items().size
            if (itemToLoot.interact("Take")) {
                Condition.wait({ Inventory.items().size > inventoryCount }, 150, 10)
            }
        }
    }
}