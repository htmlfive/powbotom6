package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class WalkToBankAfterEmergencyTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inBank = Constants.FEROX_BANK_AREA.contains(Players.local())
        val shouldRun = script.emergencyTeleportJustHappened && !inBank

        if (shouldRun) {
            script.logger.debug("Validate OK: Emergency teleport flag is true and not yet at bank.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing WalkToBankAfterEmergencyTask...")
        script.logger.info("Emergency recovery: Using Ring of Dueling to return to bank...")

        if (ScriptUtils.useDuelingRingToFerox(script)) {
            ScriptUtils.walkToFeroxBank(script)

            if (Condition.wait({ Constants.FEROX_BANK_AREA.contains(Players.local()) }, 200, 30)) {
                ScriptUtils.drinkFromPool(script) { script.needsStatRestore() }
            }
        } else {
            ScriptUtils.stopScript("No Ring of Dueling found in inventory for emergency recovery!", script)
        }
    }
}