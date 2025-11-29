package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class EquipItemsTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean {
        if (Bank.opened()) return false
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        val needsToEquip = Inventory.stream().any { invItem ->
            val requiredId = invItem.id()
            if (requiredId in requiredEquipmentIds) {
                val targetSlotIndex = script.config.requiredEquipment[requiredId]
                if (targetSlotIndex != null) {
                    val targetSlot = Equipment.Slot.values()[targetSlotIndex]
                    val currentlyEquippedItem = Equipment.itemAt(targetSlot)
                    currentlyEquippedItem.id() != requiredId
                } else {
                    script.logger.warn("Item ${invItem.name()} (ID: $requiredId) is required but has no slot mapping in config.")
                    false
                }
            } else {
                false
            }
        }

        if (needsToEquip) {
            script.logger.debug("Validate OK: Bank is closed and inventory contains required equipment not currently worn.")
        }
        return needsToEquip
    }

    override fun execute() {
        script.logger.info("Equipping required gear from inventory...")
        script.logger.debug("Executing EquipItemsTask...")
        val requiredEquipmentMap = script.config.requiredEquipment

        Inventory.stream()
            .filter { it.id() in requiredEquipmentMap.keys }
            .forEach { itemToEquip ->
                val targetSlotIndex = requiredEquipmentMap[itemToEquip.id()]
                if (targetSlotIndex == null) {
                    script.logger.warn("Skipping ${itemToEquip.name()}: Could not find target slot in configuration.")
                    return@forEach
                }
                val targetSlot = Equipment.Slot.values()[targetSlotIndex]
                val currentlyEquippedItem = Equipment.itemAt(targetSlot)

                if (currentlyEquippedItem.id() == itemToEquip.id()) {
                    script.logger.debug("Skipping ${itemToEquip.name()}: Correct item already equipped in slot $targetSlot.")
                    return@forEach
                }

                ScriptUtils.equipItem(itemToEquip, targetSlot, script)
            }
    }
}