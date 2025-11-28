package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
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

        val duelRing = Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).firstOrNull()

        if (duelRing == null || !duelRing.valid()) {
            script.logger.warn("FATAL: No Ring of Dueling found. Stopping.")
            ScriptManager.stop()
            return
        }

        if (!duelRing.interact("Rub")) {
            script.logger.warn("Failed to rub Ring of Dueling")
            return
        }

        if (!Condition.wait({ Widgets.widget(Constants.DUELING_RING_WIDGET_ID).valid() }, 200, 15)) {
            script.logger.warn("Dueling Ring widget did not appear")
            return
        }

        val enclaveOption = Widgets.widget(Constants.DUELING_RING_WIDGET_ID)
            .component(Constants.OPTIONS_CONTAINER_COMPONENT)
            .component(Constants.FEROX_ENCLAVE_OPTION_INDEX)

        if (!enclaveOption.valid() || !enclaveOption.click()) {
            script.logger.warn("Could not click Ferox Enclave option")
            return
        }

        if (!Condition.wait({ Players.local().tile().distanceTo(Constants.FEROX_ENTRANCE_TILE) < 6 }, 300, 15)) {
            script.logger.warn("Did not arrive at Ferox Enclave")
            return
        }

        script.logger.info("Arrived at Ferox, walking to bank")
        Movement.walkTo(Constants.FEROX_BANK_TILE)

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