package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EquipItemsTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean {
        if (Bank.opened()) return false
        val requiredEquipmentIds = script.config.requiredEquipment.keys
        val needsToEquip = Inventory.stream().any { it.id() in requiredEquipmentIds }

        if (needsToEquip) {
            script.logger.debug("Validate OK: Bank is closed and inventory contains required equipment.")
        }
        return needsToEquip
    }

    override fun execute() {
        script.logger.info("Equipping required gear from inventory...")
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        Inventory.stream()
            .filter { it.id() in requiredEquipmentIds }
            .forEach { itemToEquip ->
                script.logger.debug("Attempting to equip ${itemToEquip.name()} (ID: ${itemToEquip.id()})")
                val actions = itemToEquip.actions()
                var equipped = false

                // Prioritize "Wield" for weapons
                if ("Wield" in actions) {
                    script.logger.debug("Using action 'Wield' for ${itemToEquip.name()}")
                    if (itemToEquip.interact("Wield")) {
                        // Wait for the item to actually be equipped
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
                    script.logger.warn("Failed to equip ${itemToEquip.name()}. Could not find a valid action or wait timed out.")
                }
            }
    }
}