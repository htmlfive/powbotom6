package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.Helpers

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val bossIsDead = boss == null || boss.healthPercent() == 0
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= 8

        if (!bossIsDead || !inFightArea) {
            script.logger.debug("LootTask Validate FAIL: Boss not dead ($bossIsDead) or not in fight area ($inFightArea).")
            return false
        }

        val valuableItemExists = GroundItems.stream().within(Players.local(), 15).any { item ->
            val itemName = item.name()
            val gePrice = GrandExchange.getItemPrice(item.id()) ?: 0
            val stackValue = gePrice * item.stackSize()
            val isValuable = itemName != "Bones" && (itemName in script.config.alwaysLootItems || stackValue > script.config.minLootValue)

            if (isValuable) {
                script.logger.debug("LootTask Validate OK: Found valuable item: $itemName (StackValue: $stackValue)")
            }
            isValuable
        }

        return valuableItemExists
    }

    override fun execute() {
        script.logger.debug("Executing LootTask...")

        val itemToLoot = GroundItems.stream()
            .within(Players.local(), 15)
            .filter {
                val itemName = it.name()
                val gePrice = GrandExchange.getItemPrice(it.id()) ?: 0
                val stackValue = gePrice * it.stackSize()
                itemName != "Bones" && (itemName in script.config.alwaysLootItems || stackValue > script.config.minLootValue)
            }
            .minByOrNull { it.distance() }

        if (itemToLoot == null) {
            script.logger.debug("LootTask execute check failed: No item to loot found (it may have despawned).")
            return
        }

        if (Inventory.isFull()) {
            if (!Helpers.makeInventorySpace(script)) {
                script.logger.warn("Inventory full and could not make space. Skipping loot.")
                return
            }
        }

        if (!Inventory.isFull()) {
            val itemValue = (GrandExchange.getItemPrice(itemToLoot.id()) ?: 0) * itemToLoot.stackSize()
            script.logger.info("Looting ${itemToLoot.name()} (Value: $itemValue)")
            script.logger.debug("Attempting to 'Take' ${itemToLoot.name()} at ${itemToLoot.tile()}")

            val inventoryCount = Inventory.items().size
            if (itemToLoot.interact("Take")) {
                if (Condition.wait({ Inventory.items().size > inventoryCount }, 150, 10)) {
                    script.logger.debug("Successfully looted item, new inventory count: ${Inventory.items().size}")
                    script.totalLootValue += itemValue
                    Helpers.sleepRandom(600)
                } else {
                    script.logger.warn("Interaction 'Take' sent, but inventory count did not change.")
                }
            } else {
                script.logger.warn("Failed to interact 'Take' with ${itemToLoot.name()}.")
            }
        }
    }
}
