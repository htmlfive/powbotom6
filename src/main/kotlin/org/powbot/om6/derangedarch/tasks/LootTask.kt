package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.GroundItem
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean {
        // Validation logic remains the same
        val boss = script.getBoss()
        val bossIsDead = boss == null || boss.healthPercent() == 0
        if (!bossIsDead || Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8) {
            return false
        }
        return GroundItems.stream().within(Players.local(), 15).any {
            it.name() != "Bones" && (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > 1000
        }
    }

    override fun execute() {
        val itemToLoot = GroundItems.stream()
            .within(Players.local(), 15)
            .filter { it.name() != "Bones" && (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > 1000 }
            .minByOrNull { it.distance() }

        if (itemToLoot == null) {
            return
        }

        if (Inventory.isFull()) {
            val food = Inventory.stream().name(script.config.foodName).firstOrNull()
            if (food != null && food.interact("Eat")) {
                Condition.wait({ !Inventory.isFull() }, 150, 10)
            } else {
                return
            }
        }

        if (!Inventory.isFull()) {
            // --- THIS IS THE CORRECTED LOGIC ---
            // 1. Calculate the value of the item *before* trying to loot it.
            val itemValue = (GrandExchange.getItemPrice(itemToLoot.id()) ?: 0) * itemToLoot.stackSize()
            script.logger.info("Looting ${itemToLoot.name()} (Value: $itemValue)")

            val inventoryCount = Inventory.items().size
            if (itemToLoot.interact("Take")) {
                // 2. Wait until the item is actually in the inventory.
                if (Condition.wait({ Inventory.items().size > inventoryCount }, 150, 10)) {
                    // 3. Only after confirming the loot was successful, add the value to the total.
                    script.totalLootValue += itemValue
                    Condition.sleep(600)
                }
            }
        }
    }
}