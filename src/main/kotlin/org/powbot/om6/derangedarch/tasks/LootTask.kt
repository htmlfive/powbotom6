package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val bossIsDead = boss == null || boss.healthPercent() == 0
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8

        if (!bossIsDead || !inFightArea) {
            script.logger.debug("LootTask Validate FAIL: Boss not dead ($bossIsDead) or not in fight area ($inFightArea).")
            return false
        }

        // Check for valuable items
        val valuableItemExists = GroundItems.stream().within(Players.local(), 15).any { item ->
            val itemName = item.name()
            val gePrice = GrandExchange.getItemPrice(item.id()) ?: 0
            val stackValue = gePrice * item.stackSize()
            val isValuable = itemName != IDs.BONES_NAME && (itemName in script.config.alwaysLootItems || stackValue > script.config.minLootValue)
            isValuable
        }

        val shouldRun = valuableItemExists
        if (shouldRun) {
            script.logger.debug("Validate OK: Boss is dead and valuable item exists nearby.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing LootTask...")

        // 1. Find the best item to loot
        val itemToLoot = GroundItems.stream().within(Players.local(), 15)
            .sortedByDescending { item ->
                val itemName = item.name()
                val gePrice = GrandExchange.getItemPrice(item.id()) ?: 0
                val stackValue = gePrice * item.stackSize()
                val isValuable = itemName != IDs.BONES_NAME && (itemName in script.config.alwaysLootItems || stackValue > script.config.minLootValue)
                if (isValuable) stackValue else 0
            }
            .firstOrNull()

        if (itemToLoot == null) {
            script.logger.debug("No valuable items found after re-check. Looting complete for now.")
            return
        }

        // 2. Make space if inventory is full
        if (Inventory.isFull()) {
            script.logger.warn("Inventory full! Attempting to eat food to make space.")
            val food = Inventory.stream().name(script.config.foodName).firstOrNull()
            if (food != null && food.interact("Eat")) {
                Condition.wait({ !Inventory.isFull() }, 150, 10)
            } else {
                script.logger.warn("Inventory full, but no food found to eat. Skipping loot.")
                return
            }
        }

        // 3. Loot the item
        if (!Inventory.isFull()) {
            val itemValue = (GrandExchange.getItemPrice(itemToLoot.id()) ?: 0) * itemToLoot.stackSize()
            script.logger.info("Looting ${itemToLoot.name()} (Value: $itemValue)")
            script.logger.debug("Attempting to 'Take' ${itemToLoot.name()} at ${itemToLoot.tile()}")

            val inventoryCount = Inventory.items().size
            if (itemToLoot.interact("Take")) {
                if (Condition.wait({ Inventory.items().size > inventoryCount }, 150, 10)) {
                    script.logger.debug("Successfully looted item, new inventory count: ${Inventory.items().size}")
                    script.totalLootValue += itemValue
                    Condition.sleep(600) // Small delay after looting to prevent mis-clicks
                } else {
                    script.logger.warn("Interaction 'Take' sent, but inventory count did not change.")
                }
            } else {
                script.logger.warn("Failed to interact 'Take' with ${itemToLoot.name()}.")
            }
        }
    }
}