package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class WalkToBankAfterEmergencyTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private val FEROX_BANK_TILE = Tile(3128, 3638, 0)

    /**
     * This task is valid only if we have just emergency teleported and are not yet at the bank.
     */
    override fun validate(): Boolean {
        return script.emergencyTeleportJustHappened && !script.FEROX_BANK_AREA.contains(Players.local())
    }

    override fun execute() {
        script.logger.info("Emergency recovery: Walking to Ferox Enclave to re-bank.")
        Movement.walkTo(FEROX_BANK_TILE)
    }
}