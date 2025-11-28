package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.dax.teleports.Teleport
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.Helpers

class WalkToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inBank = Constants.FEROX_BANK_AREA.contains(Players.local())
        if (inBank) return false

        val notInFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) > 8
        val needsFullRestock = script.needsFullRestock()
        val emergencyTeleport = script.emergencyTeleportJustHappened

        return (needsFullRestock && notInFightArea) || emergencyTeleport
    }

    override fun execute() {
        val isEmergency = script.emergencyTeleportJustHappened
        script.logger.info(if (isEmergency) "Emergency recovery: teleporting to bank" else "Teleporting to bank")

        if (!Teleport.RING_OF_DUELING_FEROX_ENCLAVE.trigger()) {
            script.logger.warn("FATAL: Failed to teleport to Ferox Enclave. Stopping.")
            ScriptManager.stop()
            return
        }

        Condition.wait({ Constants.FEROX_BANK_AREA.contains(Players.local()) }, 200, 30)

        if (isEmergency) {
            if (Condition.wait({ Constants.FEROX_BANK_AREA.contains(Players.local()) }, 200, 30)) {
                val pool = Objects.stream().id(Constants.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()
                if (pool != null) {
                    if (!pool.inViewport()) {
                        Movement.walkTo(Constants.FEROX_POOL_AREA.randomTile)
                        Condition.wait({ pool.inViewport() }, 150, 10)
                    }
                    if (pool.interact("Drink")) {
                        if (Condition.wait({ !script.needsStatRestore() }, 150, 20)) {
                            script.logger.info("Stats restored")
                            Helpers.sleepRandom(1200)
                        }
                    }
                }
            }
        }
    }
}