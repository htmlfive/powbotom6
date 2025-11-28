package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class EmergencyTeleportTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private var reason = ""

    override fun validate(): Boolean {
        if (script.emergencyTeleportJustHappened) {
            script.logger.debug("Validate FAIL: Emergency teleport flag is already set.")
            return false
        }

        if (Constants.FEROX_BANK_AREA.contains(Players.local())) {
            script.logger.debug("Validate FAIL: Already in Ferox bank area.")
            return false
        }

        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= 8
        if (!inFightArea) {
            script.logger.debug("Validate FAIL: Not in fight area.")
            return false
        }

        val needsResupply = script.needsTripResupply()
        val lowHp = Combat.healthPercent() < script.config.emergencyHpPercent
        val lowPrayerNoPots = Prayer.prayerPoints() < 10 && Inventory.stream().nameContains("Prayer potion").isEmpty()

        script.logger.debug("EmergencyCheck: lowHp=$lowHp (${Combat.healthPercent()}% < ${script.config.emergencyHpPercent}%), lowPrayerNoPots=$lowPrayerNoPots, needsResupply=$needsResupply")

        reason = ""
        if (lowHp) reason = "HP is critical (${Combat.healthPercent()}%)"
        else if (lowPrayerNoPots) reason = "Prayer is critical and out of potions"
        else if (needsResupply) {
            if (Inventory.stream().nameContains("Prayer potion").isEmpty()) reason = "Out of prayer potions"
            else if (Inventory.stream().name(script.config.foodName).isEmpty()) reason = "Out of food"
            else reason = "Needs resupply (unknown)"
        }

        val shouldTeleport = lowHp || lowPrayerNoPots || needsResupply
        if (shouldTeleport) {
            script.logger.debug("Validate OK: Triggering emergency teleport. Reason: $reason")
        }

        return shouldTeleport
    }

    override fun execute() {
        script.logger.debug("Executing EmergencyTeleportTask...")
        script.logger.warn("EMERGENCY TELEPORT: $reason. Escaping!")

        val teleport = script.teleportOptions[script.config.emergencyTeleportItem]
        if (teleport == null) {
            script.logger.warn("FATAL: Invalid emergency teleport option configured: ${script.config.emergencyTeleportItem}. Stopping.")
            ScriptManager.stop()
            return
        }

        script.logger.debug("Using teleport item: ${teleport.itemNameContains}, Interaction: ${teleport.interaction}")
        val teleItem = Inventory.stream().nameContains(teleport.itemNameContains).firstOrNull()

        if (teleItem != null) {
            if (teleItem.interact(teleport.interaction)) {
                script.logger.debug("Teleport interaction sent. Setting emergency flag to TRUE.")
                script.emergencyTeleportJustHappened = true

                script.logger.debug("Waiting for teleport success condition...")
                Condition.wait(teleport.successCondition, 300, 20)
            } else {
                script.logger.warn("Failed to interact with teleport item: ${teleport.itemNameContains}")
            }
        } else {
            script.logger.warn("FATAL: No '${teleport.itemNameContains}' found in inventory to escape! Stopping script.")
            ScriptManager.stop()
        }
    }
}
