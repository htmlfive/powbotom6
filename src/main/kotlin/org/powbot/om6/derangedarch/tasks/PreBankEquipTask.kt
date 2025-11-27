package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PreBankEquipTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        // Define the conditions that would trigger a resupply trip.
        val noFood = Inventory.stream().name(script.config.foodName).isEmpty()
        val inventoryFull = Inventory.isFull()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()
        val needsResupply = noFood || inventoryFull || noPrayerPotions

        script.logger.debug("PreBankEquip validate: noFood=$noFood, inventoryFull=$inventoryFull, noPrayerPotions=$noPrayerPotions, needsResupply=$needsResupply")

        if (!needsResupply) {
            return false
        }

        // Get the list of IDs for the gear we are *supposed* to be wearing.
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        // This task should run if a resupply is needed AND an item that is part of our
        // combat gear is currently in the inventory.
        val hasGearInInv = Inventory.stream().any { it.id() in requiredEquipmentIds }
        script.logger.debug("PreBankEquip validate: hasGearInInv=$hasGearInInv")

        return hasGearInInv // `needsResupply` is already confirmed to be true here
    }

    override fun execute() {
        script.logger.info("Equipping required combat gear from inventory before banking...")
        script.logger.debug("Executing PreBankEquipTask...")
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        Inventory.stream()
            // Filter the inventory to only include items that are part of our defined combat gear.
            .filter { it.id() in requiredEquipmentIds }
            .forEach { itemToEquip ->

                // --- ADDED CHECK: Skip if the item is already equipped ---
                if (Equipment.stream().id(itemToEquip.id()).isNotEmpty()) {
                    script.logger.debug("Skipping ${itemToEquip.name()}: item is already equipped.")
                    return@forEach // Skips to the next item in the loop
                }
                // --- END ADDED CHECK ---

                script.logger.debug("Attempting to pre-bank equip ${itemToEquip.name()} (ID: ${itemToEquip.id()})")
                val actions = itemToEquip.actions()
                var equipped = false

                // Prioritize "Wield" for weapons
                if ("Wield" in actions) {
                    script.logger.debug("Using action 'Wield' for ${itemToEquip.name()}")
                    if (itemToEquip.interact("Wield")) {
                        equipped = Condition.wait({ Equipment.stream().id(itemToEquip.id()).isNotEmpty() }, 250, 10)
                    }
                    // Fallback to "Wear" for armor, capes, etc.
                } else if ("Wear" in actions) {
                    script.logger.debug("Using action 'Wear' for ${itemToEquip.name()}")
                    if (itemToEquip.interact("Wear")) {
                        equipped = Condition.wait({ Equipment.stream().id(itemToEquip.id()).isNotEmpty() }, 250, 10)
                    }
                    // Fallback for "Equip" for ammo, blessings, etc.
                } else if ("Equip" in actions) {
                    script.logger.debug("Using action 'Equip' for ${itemToEquip.name()}")
                    if (itemToEquip.interact("Equip")) {
                        equipped = Condition.wait({ Equipment.stream().id(itemToEquip.id()).isNotEmpty() }, 250, 10)
                    }
                }

                if (equipped) {
                    script.logger.info("Successfully equipped ${itemToEquip.name()}.")
                } else {
                    script.logger.warn("Failed to equip ${itemToEquip.name()} during pre-bank. Could not find a valid action or wait timed out.")
                }
            }
    }
}
