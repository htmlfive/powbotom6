package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EmergencyTeleportTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private var reason = ""

    override fun validate(): Boolean {
        if (script.FEROX_BANK_AREA.contains(Players.local())) return false
        val lowHp = Combat.healthPercent() < script.config.emergencyHpPercent
        val lowPrayerNoPots = Prayer.prayerPoints() < 10 && Inventory.stream().nameContains("Prayer potion").isEmpty()
        if (lowHp) reason = "HP is critical (${Combat.healthPercent()}%)"
        else if (lowPrayerNoPots) reason = "Prayer is critical (${Prayer.prayerPoints()}) and out of potions"
        return lowHp || lowPrayerNoPots
    }

    override fun execute() {
        script.logger.warn("EMERGENCY TELEPORT: $reason. Escaping!")
        val teleport = script.teleportOptions[script.config.emergencyTeleportItem]
        if (teleport == null) {
            script.logger.warn("Invalid emergency teleport option: ${script.config.emergencyTeleportItem}. Stopping.")
            ScriptManager.stop(); return
        }

        // Check if the teleport item is equippable (like the ring)
        if (teleport.isEquippable) {
            val equippedItem = if (teleport.equipmentSlot != null) Equipment.itemAt(teleport.equipmentSlot) else Item.Nil
            // Check if the correct item is ALREADY equipped
            if (equippedItem.name().contains(teleport.itemNameContains)) {
                if (equippedItem.interact(teleport.interaction)) {
                    Condition.wait(teleport.successCondition, 300, 20)
                }
            } else {
                // If it's not equipped, we can't do anything. Stop the script for safety.
                script.logger.warn("CRITICAL: Emergency teleport '${teleport.itemNameContains}' is not equipped! Stopping script.")
                ScriptManager.stop()
            }
        } else {
            // Logic for non-equippable items (like tablets) remains the same
            val teleItem = Inventory.stream().nameContains(teleport.itemNameContains).firstOrNull()
            if (teleItem != null) {
                if (teleItem.interact(teleport.interaction)) {
                    Condition.wait(teleport.successCondition, 300, 20)
                }
            } else {
                script.logger.warn("CRITICAL: No '${teleport.itemNameContains}' found in inventory to escape! Stopping script.")
                ScriptManager.stop()
            }
        }
    }
}