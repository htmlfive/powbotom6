package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EmergencyTeleportTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private var reason = ""

    override fun validate(): Boolean {
        // Don't trigger if we are already safe at the bank.
        if (script.FEROX_BANK_AREA.contains(Players.local())) return false

        // Only check for resupply needs if we are in the fight area.
        if (Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8) return false

        val lowHp = Combat.healthPercent() < script.config.emergencyHpPercent
        val lowPrayer = Prayer.prayerPoints() < 10
        val noFood = Inventory.stream().name(script.config.foodName).isEmpty()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()
        val inventoryFull = Inventory.isFull()

        // Determine the reason for teleporting
        if (lowHp) reason = "HP is critical (${Combat.healthPercent()}%)"
        else if (lowPrayer && noPrayerPotions) reason = "Prayer is critical (${Prayer.prayerPoints()}) and out of potions"
        else if (noFood) reason = "Out of food"
        else if (noPrayerPotions) reason = "Out of prayer potions"
        else if (inventoryFull) reason = "Inventory is full"

        return lowHp || (lowPrayer && noPrayerPotions) || noFood || noPrayerPotions || inventoryFull
    }

    override fun execute() {
        script.logger.warn("RESUPPLY TELEPORT: $reason. Escaping!")
        val teleport = script.teleportOptions[script.config.emergencyTeleportItem]
        if (teleport == null) {
            script.logger.warn("Invalid emergency teleport option: ${script.config.emergencyTeleportItem}. Stopping.")
            ScriptManager.stop(); return
        }

        val teleItem = Inventory.stream().nameContains(teleport.itemNameContains).firstOrNull()
        if (teleItem != null) {
            if (teleItem.interact(teleport.interaction)) {
                if (Condition.wait(teleport.successCondition, 300, 20)) {
                    // Set the flag to trigger the walk-back and re-banking sequence.
                    script.emergencyTeleportJustHappened = true
                }
            }
        } else {
            script.logger.warn("CRITICAL: No '${teleport.itemNameContains}' found in inventory to escape! Stopping script.")
            ScriptManager.stop()
        }
    }
}