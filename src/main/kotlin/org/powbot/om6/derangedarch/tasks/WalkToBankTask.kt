package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
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

        // Don't interrupt if we're on Fossil Island traveling to boss
        val onFossilIsland = Objects.stream().id(Constants.MAGIC_MUSHTREE_ID, Constants.SECOND_MUSHTREE_ID).isNotEmpty() ||
                Players.local().tile().distanceTo(Constants.TRUNK_SAFE_TILE) < 200
        if (onFossilIsland) return false

        val notInFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) > Constants.DISTANCETOBOSS
        val needsFullRestock = script.needsFullRestock()
        val emergencyTeleport = script.emergencyTeleportJustHappened

        return (needsFullRestock && notInFightArea) || emergencyTeleport
    }

    override fun execute() {
        val isEmergency = script.emergencyTeleportJustHappened
        script.logger.info(if (isEmergency) "Emergency recovery: teleporting to bank" else "Teleporting to bank")
        script.logger.debug("Current position: ${Players.local().tile()}")

        script.logger.debug("Triggering Ring of Dueling teleport")
        if (!Teleport.RING_OF_DUELING_FEROX_ENCLAVE.trigger()) {
            script.logger.warn("Teleport.RING_OF_DUELING_FEROX_ENCLAVE.trigger() returned false")
            return
        }

        script.logger.debug("Teleport trigger successful, waiting to reach bank area")

        val arrived = Condition.wait({
            val currentPos = Players.local().tile()
            val inBankArea = Constants.FEROX_BANK_AREA.contains(Players.local())

            if (!inBankArea) {
                script.logger.debug("Not in bank area yet. Position: $currentPos, Distance to ${Constants.FEROX_TELEPORT_TILE}: ${currentPos.distanceTo(Constants.FEROX_TELEPORT_TILE)}")

                val bankChest = Objects.stream().name("Bank chest").nearest().firstOrNull()
                if (bankChest != null) {
                    val distance = bankChest.distance()
                    script.logger.debug("Bank chest found at distance: $distance")

                    if (distance > 5) {
                        script.logger.debug("Walking to bank chest")
                        Movement.walkTo(bankChest)
                    }
                } else {
                    script.logger.warn("Bank chest not found")
                }
            } else {
                script.logger.debug("Arrived in bank area")
            }

            inBankArea
        }, 200, 30)

        script.logger.debug("Wait finished. Arrived: $arrived, Position: ${Players.local().tile()}")

        if (isEmergency) {
            script.logger.info("Emergency teleport - restoring stats at pool")

            val pool = Objects.stream().id(Constants.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()
            if (pool != null) {
                script.logger.debug("Pool found at distance: ${pool.distance()}")

                if (!pool.inViewport()) {
                    script.logger.debug("Pool not in viewport, walking to pool area")
                    Movement.walkTo(Constants.FEROX_POOL_AREA.randomTile)
                    Condition.wait({ pool.inViewport() }, 150, 10)
                }

                script.logger.debug("Interacting with pool")
                if (pool.interact("Drink")) {
                    script.logger.debug("Drink interaction sent, waiting for stats to restore")

                    if (Condition.wait({ !script.needsStatRestore() }, 150, 20)) {
                        script.logger.info("Stats restored successfully")
                        Helpers.sleepRandom(1200)
                    } else {
                        script.logger.warn("Stats did not restore (timeout)")
                    }
                } else {
                    script.logger.warn("Failed to interact with pool")
                }
            } else {
                script.logger.warn("Pool of Refreshment not found")
            }
        }

        script.logger.debug("WalkToBankTask complete")
    }
}