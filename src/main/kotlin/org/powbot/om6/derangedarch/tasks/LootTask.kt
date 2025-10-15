package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.GroundItem
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    /**
     * Validates if the boss is dead and if there is at least one item nearby (excluding bones)
     * with a GE value > 1000 gp.
     */
    override fun validate(): Boolean {
        // Task is only valid if we are in the fight area and the boss is NOT present.
        if (script.getBoss() != null || Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8) {
            return false
        }

        // Check if there is ANY valuable item on the ground.
        return GroundItems.stream().within(Players.local(), 15).any {
            it.name() != "Bones" && (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > 1000
        }
    }

    override fun execute() {
        // Find the nearest valuable item that is NOT bones.
        val itemToLoot = GroundItems.stream()
            .within(Players.local(), 15)
            .filter { it.name() != "Bones" && (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > 1000 }
            .minByOrNull { it.distance() }

        if (itemToLoot == null) {
            script.logger.info("Valuable loot disappeared before it could be picked up.")
            return
        }

        // If inventory is full, try to eat food to make space.
        if (Inventory.isFull()) {
            val food = Inventory.stream().name(script.config.foodName).firstOrNull()
            if (food != null) {
                script.logger.info("Inventory full, eating ${food.name()} to make space for loot.")
                if (food.interact("Eat")) {
                    // Wait until a space is free.
                    Condition.wait({ !Inventory.isFull() }, 150, 10)
                }
            } else {
                script.logger.warn("Inventory full and no food to eat for space! Skipping loot.")
                return // Can't loot, so stop.
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