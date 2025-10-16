package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class RepositionTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private val repositionTile = Tile(3683, 3701, 0)

    /**
     * This task is valid if we are "idle" in the boss area:
     * - The boss is gone.
     * - Our prayer is off.
     * - We are not already at the reposition tile.
     * - We are not moving.
     * The higher-priority LootTask will run first if there is loot, so we don't need to check for it here.
     */
    override fun validate(): Boolean {
        val player = Players.local()
        val inFightArea = player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
        val bossIsGone = script.getBoss() == null
        val prayerIsOff = !Prayer.prayerActive(script.REQUIRED_PRAYER)
        val notAtTargetTile = player.tile() != repositionTile

        return inFightArea && bossIsGone && prayerIsOff && notAtTargetTile && !player.inMotion()
    }

    override fun execute() {
        script.logger.info("Repositioning for next kill...")
        if (Movement.step(repositionTile)) {
            Condition.wait({ Players.local().tile() == repositionTile }, 150, 10)
        }
    }
}