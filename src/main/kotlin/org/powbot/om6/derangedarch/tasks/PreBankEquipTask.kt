package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PreBankEquipTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task now validates ONLY if we need supplies AND there is an item in our inventory
     * that is also a key in the user's "Required Equipment" map.
     * This prevents it from activating for utility items like rings or an axe.
     */
    override fun validate(): Boolean {
        // Don't run if we don't need to bank.
        if (!script.needsSupplies()) return false

        // Get the list of IDs for the gear we are *supposed* to be wearing.
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        // Return true only if an item in our inventory matches one of those required IDs.
        return Inventory.stream().any { it.id() in requiredEquipmentIds }
    }

    /**
     * This task now filters the inventory and ONLY interacts with items
     * that are part of the "Required Equipment" setup before teleporting to the bank.
     */
    override fun execute() {
        script.logger.info("Equipping required combat gear from inventory before banking...")
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        Inventory.stream()
            // IMPORTANT: Filter the inventory to only include items that are part of our defined combat gear.
            .filter { it.id() in requiredEquipmentIds }
            .forEach { itemToEquip ->
                // The action is typically "Wear", but "Wield" is a fallback for weapons.
                if (itemToEquip.interact("Wear") || itemToEquip.interact("Wield")) {
                    // Wait a moment for the equip action to complete before trying the next item.
                    Condition.sleep(250)
                }
            }
    }
}