package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val bossIsDead = boss == null || boss.healthPercent() == 0
        if (!bossIsDead || Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8) {
            return false
        }

        // --- MODIFIED: Uses 'alwaysLootItems' and 'minLootValue' from the script config ---
        return GroundItems.stream().within(Players.local(), 15).any {
            it.name() != "Bones" && (it.name() in script.config.alwaysLootItems ||
                    (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > script.config.minLootValue)
        }
    }

    override fun execute() {
        // --- MODIFIED: Uses 'alwaysLootItems' and 'minLootValue' from the script config ---
        val itemToLoot = GroundItems.stream()
            .within(Players.local(), 15)
            .filter {
                it.name() != "Bones" && (it.name() in script.config.alwaysLootItems ||
                        (GrandExchange.getItemPrice(it.id()) ?: 0) * it.stackSize() > script.config.minLootValue)
            }
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
            val itemValue = (GrandExchange.getItemPrice(itemToLoot.id()) ?: 0) * itemToLoot.stackSize()
            script.logger.info("Looting ${itemToLoot.name()} (Value: $itemValue)")

            val inventoryCount = Inventory.items().size
            if (itemToLoot.interact("Take")) {
                if (Condition.wait({ Inventory.items().size > inventoryCount }, 150, 10)) {
                    script.totalLootValue += itemValue
                    Condition.sleep(600)
                }
            }
        }
    }
}