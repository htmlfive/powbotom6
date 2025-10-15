package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TeleportToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    // A tile safely inside the Ferox Enclave bank area.
    private val FEROX_BANK_TILE = Tile(3135, 3631, 0)

    override fun validate(): Boolean = script.needsSupplies() && !script.FEROX_BANK_AREA.contains(Players.local())

    override fun execute() {
        script.logger.info("Supplies are low, walking to Ferox Enclave bank...")
        Movement.walkTo(FEROX_BANK_TILE)
    }
}