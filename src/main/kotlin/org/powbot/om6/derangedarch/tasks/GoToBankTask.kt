package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class GoToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private val FEROX_BANK_TILE = Tile(3130, 3631, 0)

    /**
     * This task is valid if the inventory or equipment is incorrect
     * and we are not already at the bank. This handles the startup check.
     */
    override fun validate(): Boolean {
        val needsBanking = !script.equipmentIsCorrect() || !script.inventoryIsCorrect()
        return needsBanking && !script.FEROX_BANK_AREA.contains(Players.local())
    }

    override fun execute() {
        script.logger.info("Setup is incorrect, webwalking to Ferox Enclave to bank...")
        Movement.walkTo(FEROX_BANK_TILE)
    }
}