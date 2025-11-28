package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class RepositionTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val player = Players.local()
        val inFightArea = player.tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.DISTANCETOBOSS
        val bossIsGone = script.getBoss() == null
        val prayerIsOff = !Prayer.prayerActive(script.REQUIRED_PRAYER)
        val notAtTargetTile = player.trueTile() != Constants.REPOSITION_TILE
        val notInMotion = !player.inMotion()

        return inFightArea && bossIsGone && prayerIsOff && notAtTargetTile && notInMotion
    }

    override fun execute() {
        script.logger.info("Repositioning for next kill")

        if (Movement.step(Constants.REPOSITION_TILE, 5)) {
            Condition.wait({ Players.local().trueTile() == Constants.REPOSITION_TILE }, 100, 20)
        }
    }
}