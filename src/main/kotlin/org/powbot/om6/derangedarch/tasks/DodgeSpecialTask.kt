package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private fun isSpecialAttackActive(): Boolean {
        val projectile = Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).isNotEmpty()
        val overheadText = Npcs.stream().name("Deranged archaeologist").any { it.overheadMessage() == script.SPECIAL_ATTACK_TEXT }

        if (projectile) script.logger.debug("Special attack projectile detected.")
        if (overheadText) script.logger.debug("Special attack overhead text detected.")

        return projectile || overheadText
    }

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
        val specialActive = isSpecialAttackActive()

        val shouldRun = specialActive && inFightArea
        if (shouldRun) {
            script.logger.debug("Validate OK: Special attack is active and player is in the fight area.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing DodgeSpecialTask...")
        val player = Players.local()

        // If we are already moving, we are mid-dodge. Do nothing.
        if (player.inMotion()) {
            script.logger.debug("Player is already in motion, assuming mid-dodge.")
            return
        }

        script.logger.info("Special attack active and player is idle. Initiating dodge.")
        val boss = script.getBoss()

        val searchRadius = 10
        val southWestTile = Tile(player.tile().x() - searchRadius, player.tile().y() - searchRadius, player.floor())
        val northEastTile = Tile(player.tile().x() + searchRadius, player.tile().y() + searchRadius, player.floor())
        val searchArea = Area(southWestTile, northEastTile)

        val safeTile = searchArea.tiles.filter { tile ->
            val distanceToPlayer = tile.distanceTo(player.tile())
            val distanceToBoss = if (boss != null) boss.distanceTo(tile) else 99.0

            distanceToPlayer >= 6 && distanceToBoss >= 2 && tile.reachable()
        }.randomOrNull()

        if (safeTile != null) {
            script.logger.debug("Found safe tile: $safeTile. Stepping...")
            if (Movement.step(safeTile)) {
                // Wait until we have stopped moving (arrived at the destination).
                if (Condition.wait({ !player.inMotion() }, 600, 20)) {
                    script.logger.info("Dodge move complete, re-engaging boss...")
                    val currentBoss = script.getBoss() // Get a fresh reference to the boss
                    if (currentBoss != null && currentBoss.valid()) {
                        if (!currentBoss.inViewport()) {
                            script.logger.debug("Turning camera to boss for re-attack.")
                            Camera.turnTo(currentBoss)
                        }
                        if (currentBoss.interact("Attack")) {
                            // Wait briefly to confirm we are interacting.
                            Condition.wait({ player.interacting() == currentBoss }, 600, 10)
                        }
                    } else {
                        script.logger.debug("Boss is null or invalid, cannot re-engage after dodge.")
                    }
                }
            } else {
                script.logger.warn("Movement.step() to safe tile failed.")
            }
        } else {
            script.logger.warn("Could not find a valid safe tile for dodge!")
        }
    }
}