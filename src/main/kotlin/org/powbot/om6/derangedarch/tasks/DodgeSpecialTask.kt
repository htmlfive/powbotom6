package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private fun isSpecialAttackActive(): Boolean {
        return Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).isNotEmpty() ||
                Npcs.stream().name("Deranged archaeologist").any { it.overheadMessage() == script.SPECIAL_ATTACK_TEXT }
    }

    override fun validate(): Boolean {
        return isSpecialAttackActive() && Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
    }

    override fun execute() {
        val player = Players.local()

        // If we are already moving, we are mid-dodge. Do nothing.
        if (player.inMotion()) {
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
            if (Movement.step(safeTile)) {
                // Wait until we have stopped moving (arrived at the destination).
                if (Condition.wait({ !player.inMotion() }, 150, 20)) {

                    // --- NEW LOGIC: Immediately re-attack after dodging ---
                    script.logger.info("Dodge move complete, re-engaging boss...")
                    val currentBoss = script.getBoss() // Get a fresh reference to the boss
                    if (currentBoss != null && currentBoss.valid()) {
                        if (!currentBoss.inViewport()) {
                            Camera.turnTo(currentBoss)
                        }
                        if (currentBoss.interact("Attack")) {
                            // Wait briefly to confirm we are interacting.
                            Condition.wait({ player.interacting() == currentBoss }, 150, 10)
                        }
                    }
                }
            }
        } else {
            script.logger.warn("Could not find a valid safe tile for dodge!")
        }
    }
}