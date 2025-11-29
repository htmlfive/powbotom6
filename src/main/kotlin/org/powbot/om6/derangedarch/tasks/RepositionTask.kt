package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class RepositionTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private val repositionTile = Tile(3688, 3705, 0)

    override fun validate(): Boolean {
        val player = Players.local()
        val inFightArea = player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
        val bossIsGone = script.getBoss() == null
        val prayerIsOff = !Prayer.prayerActive(script.REQUIRED_PRAYER)
        val notAtTargetTile = player.tile() != repositionTile
        val notInMotion = !player.inMotion()

        val shouldRun = inFightArea && bossIsGone && prayerIsOff && notAtTargetTile && notInMotion

        if (shouldRun) {
            script.logger.debug("Validate OK: In fight area, boss gone, prayer off, not at reposition tile, and not moving.")
        }

        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing RepositionTask...")
        script.logger.info("Repositioning for next kill...")

        if (Movement.step(repositionTile)) {
            Condition.wait({ Players.local().tile() == repositionTile }, 150, 10)
        } else {
            script.logger.warn("Movement.step() to reposition tile failed.")
        }
    }
}