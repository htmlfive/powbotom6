package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.FIGHT_AREA_EXTENDED
        val needsResupply = script.needsTripResupply()

        if (boss != null && inFightArea) {
            val bossTarget = boss.interacting()
            if (bossTarget is Player && bossTarget != Players.local()) {
                script.logger.warn("Another player is fighting the boss. Stopping script to avoid crashing.")
                ScriptManager.stop()
                return false
            }
        }

        val shouldFight = boss != null && inFightArea && !needsResupply
        if (shouldFight) {
            script.logger.debug("Validate OK: Boss is present, in fight area, and no resupply needed.")
        } else if (boss == null) {
            script.logger.debug("Validate FAIL: Boss is null.")
        } else if (!inFightArea) {
            script.logger.debug("Validate FAIL: Not in fight area.")
        } else if (needsResupply) {
            script.logger.debug("Validate FAIL: Needs trip resupply.")
        }
        return shouldFight
    }

    override fun execute() {
        script.logger.debug("Executing FightTask...")

        val boss = script.getBoss() ?: return
        val bossTarget = boss.interacting()
        if (bossTarget is Player && bossTarget != Players.local()) {
            script.logger.warn("Another player detected fighting the boss during execute. Stopping script.")
            ScriptManager.stop()
            return
        }

        if (boss.distance() < Constants.BOSS_CLOSE_DISTANCE) {
            script.logger.debug("Too close to boss, finding a safe spot to step back.")
            val playerTile = Players.local().tile()
            val searchRadius = 5
            val southWestTile = Tile(playerTile.x() - searchRadius, playerTile.y() - searchRadius, playerTile.floor())
            val northEastTile = Tile(playerTile.x() + searchRadius, playerTile.y() + searchRadius, playerTile.floor())
            val searchArea = Area(southWestTile, northEastTile)

            val safeSpot = searchArea.tiles.filter {
                Movement.reachable(playerTile, it) && it.distanceTo(boss) >= Constants.BOSS_CLOSE_DISTANCE
            }.randomOrNull()

            if (safeSpot != null) {
                script.logger.debug("Stepping to safe spot: $safeSpot")
                Movement.step(safeSpot)
                Condition.wait({ boss.valid() && boss.distance() >= Constants.BOSS_CLOSE_DISTANCE }, 100, 10)
                return
            } else {
                script.logger.warn("Could not find a safe spot to step back to.")
            }
        }

        ScriptUtils.attackBoss(boss, script)
    }
}