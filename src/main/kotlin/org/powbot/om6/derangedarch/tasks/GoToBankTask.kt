package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class GoToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        // Validate if emergency teleport happened and not at bank
        if (script.emergencyTeleportJustHappened && !Constants.FEROX_BANK_AREA.contains(Players.local())) {
            script.logger.debug("Validate OK: Emergency teleport flag is true and not yet at bank.")
            return true
        }

        // Validate if needs full restock
        val needsFullRestock = script.needsFullRestock()
        val notAtBank = !Constants.FEROX_BANK_AREA.contains(Players.local())
        val notInFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) > Constants.FIGHT_AREA_DISTANCE

        val shouldRun = needsFullRestock && notAtBank && notInFightArea
        if (shouldRun) {
            script.logger.debug("Validate OK: Needs restock ($needsFullRestock), not at bank ($notAtBank), not in fight area ($notInFightArea).")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing GoToBankTask...")

        val isEmergency = script.emergencyTeleportJustHappened
        if (isEmergency) {
            script.logger.info("Emergency recovery: Using Ring of Dueling to return to bank...")
        } else {
            script.logger.info("Setup is incorrect, using Ring of Dueling to go to bank...")
        }

        if (ScriptUtils.useDuelingRingToFerox(script)) {
            ScriptUtils.walkToFeroxBank(script)

            // If this was an emergency teleport, drink from pool after arriving
            if (isEmergency && Condition.wait({ Constants.FEROX_BANK_AREA.contains(Players.local()) }, 200, 30)) {
                ScriptUtils.drinkFromPool(script) { script.needsStatRestore() }
            }
        } else {
            val errorMsg = if (isEmergency) {
                "No Ring of Dueling found in inventory for emergency recovery!"
            } else {
                "No Ring of Dueling found in inventory for banking! Please add one to your setup."
            }
            ScriptUtils.stopScript(errorMsg, script)
        }
    }
}