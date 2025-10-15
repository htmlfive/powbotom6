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

    /**
     * SIMPLIFIED: This logic now only looks for a consumable teleport item in the inventory.
     */
    override fun execute() {
        script.logger.warn("EMERGENCY TELEPORT: $reason. Escaping!")
        val teleport = script.teleportOptions[script.config.emergencyTeleportItem]
        if (teleport == null) {
            script.logger.warn("Invalid emergency teleport option: ${script.config.emergencyTeleportItem}. Stopping.")
            ScriptManager.stop(); return
        }

        // Find the teleport item (e.g., house tablet) in the inventory.
        val teleItem = Inventory.stream().nameContains(teleport.itemNameContains).firstOrNull()
        if (teleItem != null) {
            // Interact with the item.
            if (teleItem.interact(teleport.interaction)) {
                Condition.wait(teleport.successCondition, 300, 20)
            }
        } else {
            // If the item isn't found, stop the script for safety.
            script.logger.warn("CRITICAL: No '${teleport.itemNameContains}' found in inventory to escape! Stopping script.")
            ScriptManager.stop()
        }
    }
}