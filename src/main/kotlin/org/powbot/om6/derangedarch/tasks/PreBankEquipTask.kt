package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PreBankEquipTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task now validates by checking for resupply conditions directly,
     * since the needsSupplies() helper function was removed.
     */
    override fun validate(): Boolean {
        // Define the conditions that would trigger a resupply trip.
        val noFood = Inventory.stream().name(script.config.foodName).isEmpty()
        val inventoryFull = Inventory.isFull()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()
        val needsResupply = noFood || inventoryFull || noPrayerPotions

        // This task should run if a resupply is needed AND there are wearable items in the inventory.
        return needsResupply && Inventory.stream().any { it.actions().contains("Wear") || it.actions().contains("Wield") }
    }

    override fun execute() {
        script.logger.info("Equipping items from inventory before banking...")
        Inventory.stream().filter { it.actions().contains("Wear") || it.actions().contains("Wield") }.forEach {
            it.interact("Wear")
            Condition.sleep(250)
        }
    }
}