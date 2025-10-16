package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PreBankEquipTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task now validates ONLY if a resupply is needed AND there is an item in the inventory
     * that is also a key in the user's "Required Equipment" map.
     */
    override fun validate(): Boolean {
        // Define the conditions that would trigger a resupply trip.
        val noFood = Inventory.stream().name(script.config.foodName).isEmpty()
        val inventoryFull = Inventory.isFull()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()
        val needsResupply = noFood || inventoryFull || noPrayerPotions

        // Get the list of IDs for the gear we are *supposed* to be wearing.
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        // This task should run if a resupply is needed AND an item that is part of our
        // combat gear is currently in the inventory.
        return needsResupply && Inventory.stream().any { it.id() in requiredEquipmentIds }
    }

    /**
     * This task now filters the inventory and ONLY interacts with items
     * that are part of the "Required Equipment" setup.
     */
    override fun execute() {
        script.logger.info("Equipping required combat gear from inventory before banking...")
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        Inventory.stream()
            // IMPORTANT: Filter the inventory to only include items that are part of our defined combat gear.
            .filter { it.id() in requiredEquipmentIds }
            .forEach { itemToEquip ->
                if (itemToEquip.interact("Wear") || itemToEquip.interact("Wield")) {
                    Condition.sleep(250)
                }
            }
    }
}