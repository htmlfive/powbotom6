package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class PreBankEquipTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val noFood = Inventory.stream().name(script.config.foodName).isEmpty()
        val inventoryFull = Inventory.isFull()
        val noPrayerPotions = Inventory.stream().nameContains(org.powbot.om6.derangedarch.Constants.PRAYER_POTION_NAME_CONTAINS).isEmpty()
        val needsResupply = noFood || inventoryFull || noPrayerPotions

        script.logger.debug("PreBankEquip validate: noFood=$noFood, inventoryFull=$inventoryFull, noPrayerPotions=$noPrayerPotions, needsResupply=$needsResupply")

        if (!needsResupply) return false

        val requiredEquipmentIds = script.config.requiredEquipment.keys
        val hasGearInInv = Inventory.stream().any { it.id() in requiredEquipmentIds }
        script.logger.debug("PreBankEquip validate: hasGearInInv=$hasGearInInv")

        return hasGearInInv
    }

    override fun execute() {
        script.logger.info("Equipping required combat gear from inventory before banking...")
        script.logger.debug("Executing PreBankEquipTask...")
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        Inventory.stream()
            .filter { it.id() in requiredEquipmentIds }
            .forEach { itemToEquip ->
                if (Equipment.stream().id(itemToEquip.id()).isNotEmpty()) {
                    script.logger.debug("Skipping ${itemToEquip.name()}: item is already equipped.")
                    return@forEach
                }

                val targetSlotIndex = script.config.requiredEquipment[itemToEquip.id()] ?: return@forEach
                val targetSlot = Equipment.Slot.values()[targetSlotIndex]
                ScriptUtils.equipItem(itemToEquip, targetSlot, script)
            }
    }
}