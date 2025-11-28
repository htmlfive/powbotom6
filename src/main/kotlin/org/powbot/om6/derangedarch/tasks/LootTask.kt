package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
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

        if (!bossIsDead || !inFightArea) return false

        return GroundItems.stream().within(Players.local(), 15).any { item ->
            val itemName = item.name()
            if (itemName == "Bones") return@any false
            if (itemName in script.config.alwaysLootItems) return@any true

            val gePrice = script.getCachedPrice(item.id())
            val stackValue = gePrice * item.stackSize()
            stackValue > script.config.minLootValue
        }
    }

    override fun execute() {
        val itemToLoot = GroundItems.stream()
            .within(Players.local(), 15)
            .filter {
                val itemName = it.name()
                if (itemName == "Bones") return@filter false
                if (itemName in script.config.alwaysLootItems) return@filter true

                val gePrice = script.getCachedPrice(it.id())
                val stackValue = gePrice * it.stackSize()
                stackValue > script.config.minLootValue
            }
            .minByOrNull { it.distance() }

        if (itemToLoot == null) return

        if (Inventory.isFull()) {
            if (!Helpers.makeInventorySpace(script)) {
                script.logger.warn("Inventory full, cannot loot")
                return
            }
        }

        if (!Inventory.isFull()) {
            val itemValue = script.getCachedPrice(itemToLoot.id()) * itemToLoot.stackSize()
            script.logger.info("Looting ${itemToLoot.name()} (${itemValue}gp)")

            val inventoryCount = Inventory.items().size
            if (itemToLoot.interact("Take")) {
                if (Condition.wait({ Inventory.items().size > inventoryCount }, 150, 10)) {
                    script.totalLootValue += itemValue
                    Helpers.sleepRandom(600)
                }
            }
        }
    }
}