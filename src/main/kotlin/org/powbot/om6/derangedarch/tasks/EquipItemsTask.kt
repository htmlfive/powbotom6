package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EquipItemsTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task now validates ONLY if there's an item in the inventory
     * that is also a key in the user's "Required Equipment" map.
     * This prevents it from activating for items like rings or tools.
     */
    override fun validate(): Boolean {
        // Don't run if the bank is open.
        if (Bank.opened()) return false

        // Get the list of IDs for the gear we are *supposed* to be wearing.
        val requiredEquipmentIds = script.config.requiredEquipment.keys

        // Return true only if an item in our inventory matches one of those required IDs.
        return Inventory.stream().any { it.id() in requiredEquipmentIds }
    }

    /**
     * This task now filters the inventory and ONLY interacts with items
     * that are part of the "Required Equipment" setup.
     */
    override fun execute() {
        script.logger.info("Equipping required combat gear from inventory...")
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