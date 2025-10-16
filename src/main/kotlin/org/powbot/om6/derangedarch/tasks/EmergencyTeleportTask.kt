package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EmergencyTeleportTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private var reason = ""

    override fun validate(): Boolean {
        // --- ADDED THIS CHECK ---
        // If we have already initiated an emergency teleport, do not run this task again.
        // This prevents the script from trying to teleport twice.
        if (script.emergencyTeleportJustHappened) return false

        // Don't trigger if we are already safe at the bank.
        if (script.FEROX_BANK_AREA.contains(Players.local())) return false

        // Only check for resupply needs if we are in the fight area.
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
        if (!inFightArea) return false

        val needsResupply = script.needsTripResupply()
        val lowHp = Combat.healthPercent() < script.config.emergencyHpPercent
        val lowPrayerNoPots = Prayer.prayerPoints() < 10 && Inventory.stream().nameContains("Prayer potion").isEmpty()

        // Determine the reason for teleporting...
        if (lowHp) reason = "HP is critical (${Combat.healthPercent()}%)"
        else if (lowPrayerNoPots) reason = "Prayer is critical and out of potions"
        else if (needsResupply) {
            if (Inventory.stream().nameContains("Prayer potion").isEmpty()) reason = "Out of prayer potions"
            else if (Inventory.stream().name(script.config.foodName).isEmpty()) reason = "Out of food"
        }

        return lowHp || lowPrayerNoPots || needsResupply
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
                // --- MOVED THIS LINE ---
                // Set the flag IMMEDIATELY after initiating the teleport.
                // This "locks" the task and prevents it from running again.
                script.emergencyTeleportJustHappened = true

                // Now, wait for the teleport to complete.
                Condition.wait(teleport.successCondition, 300, 20)
            }
        } else {
            script.logger.warn("CRITICAL: No '${teleport.itemNameContains}' found in inventory to escape! Stopping script.")
            ScriptManager.stop()
        }
    }
}