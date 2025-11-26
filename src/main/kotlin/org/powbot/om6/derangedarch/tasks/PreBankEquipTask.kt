package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs

class PreBankEquipTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        // Define the conditions that would trigger a resupply trip.
        val noFood = Inventory.stream().name(script.config.foodName).isEmpty()
        val inventoryFull = Inventory.isFull()
        val noPrayerPotions = Inventory.stream().nameContains(IDs.PRAYER_POTION_NAME_CONTAINS).isEmpty()
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

        return hasGearInInv
    }

    override fun execute() {
        script.logger.debug("Executing PreBankEquipTask...")

        // Find the first required item that is currently in the inventory.
        val itemToEquip = Inventory.stream().filter { it.id() in script.config.requiredEquipment.keys }.firstOrNull()

        if (itemToEquip != null) {
            val actions = itemToEquip.actions()
            var equipped = false
            script.logger.info("Equipping ${itemToEquip.name()} before banking.")

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
        } else {
            script.logger.debug("No combat gear found in inventory, PreBankEquipTask finished.")
        }
    }
}