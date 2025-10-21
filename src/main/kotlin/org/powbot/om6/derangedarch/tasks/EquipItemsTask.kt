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
        // Check if any required item is in the inventory *and* not currently equipped correctly
        val needsToEquip = Inventory.stream().any { invItem ->
            val requiredId = invItem.id()
            if (requiredId in requiredEquipmentIds) {
                // Find the slot this item belongs to based on config
                val targetSlotIndex = script.config.requiredEquipment[requiredId]
                if (targetSlotIndex != null) {
                    val targetSlot = Equipment.Slot.values()[targetSlotIndex]
                    val currentlyEquippedItem = Equipment.itemAt(targetSlot)
                    // Needs equip if the slot is empty or has the wrong item
                    currentlyEquippedItem.id() != requiredId
                } else {
                    // Item in inventory is in required list but has no slot mapping? Should not happen.
                    script.logger.warn("Item ${invItem.name()} (ID: $requiredId) is required but has no slot mapping in config.")
                    false // Don't try to equip if slot is unknown
                }
            } else {
                false // Not a required item
            }
        }


        if (needsToEquip) {
            script.logger.debug("Validate OK: Bank is closed and inventory contains required equipment not currently worn.")
        }
        return needsToEquip
    }

    override fun execute() {
        script.logger.info("Equipping required gear from inventory...")
        val requiredEquipmentMap = script.config.requiredEquipment // Get ID -> SlotIndex map

        Inventory.stream()
            // Only consider items that are part of the required equipment setup
            .filter { it.id() in requiredEquipmentMap.keys }
            .forEach { itemToEquip ->

                // --- ADDED CHECK: See if the item is already equipped ---
                val targetSlotIndex = requiredEquipmentMap[itemToEquip.id()]
                if (targetSlotIndex == null) {
                    script.logger.warn("Skipping ${itemToEquip.name()}: Could not find target slot in configuration.")
                    return@forEach // Skip this item if slot is unknown
                }
                val targetSlot = Equipment.Slot.values()[targetSlotIndex]
                val currentlyEquippedItem = Equipment.itemAt(targetSlot)

                if (currentlyEquippedItem.id() == itemToEquip.id()) {
                    script.logger.debug("Skipping ${itemToEquip.name()}: Correct item already equipped in slot $targetSlot.")
                    return@forEach // Skip if the exact item is already worn
                }
                // --- END ADDED CHECK ---


                script.logger.debug("Attempting to equip ${itemToEquip.name()} (ID: ${itemToEquip.id()}) into slot $targetSlot")
                val actions = itemToEquip.actions()
                var equipped = false
                var actionUsed = "None"

                // Prioritize "Wield"
                if ("Wield" in actions) {
                    actionUsed = "Wield"
                    script.logger.debug("Using action '$actionUsed' for ${itemToEquip.name()}")
                    if (itemToEquip.interact(actionUsed)) {
                        equipped = Condition.wait({ Equipment.itemAt(targetSlot).id() == itemToEquip.id() }, 250, 10)
                    }
                    // Fallback to "Wear"
                } else if ("Wear" in actions) {
                    actionUsed = "Wear"
                    script.logger.debug("Using action '$actionUsed' for ${itemToEquip.name()}")
                    if (itemToEquip.interact(actionUsed)) {
                        equipped = Condition.wait({ Equipment.itemAt(targetSlot).id() == itemToEquip.id() }, 250, 10)
                    }
                    // Fallback for "Equip"
                } else if ("Equip" in actions) {
                    actionUsed = "Equip"
                    script.logger.debug("Using action '$actionUsed' for ${itemToEquip.name()}")
                    if (itemToEquip.interact(actionUsed)) {
                        equipped = Condition.wait({ Equipment.itemAt(targetSlot).id() == itemToEquip.id() }, 250, 10)
                    }
                }

                if (equipped) {
                    script.logger.info("Successfully equipped ${itemToEquip.name()}.")
                } else {
                    script.logger.warn("Failed to equip ${itemToEquip.name()} using action '$actionUsed'. Could not find a valid action or wait timed out.")
                }
            }
    }
}