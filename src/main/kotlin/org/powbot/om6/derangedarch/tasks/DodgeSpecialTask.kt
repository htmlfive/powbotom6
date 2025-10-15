package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    /**
     * This task is now the sole authority for dodging. It validates whenever the special attack is active.
     */
    override fun validate(): Boolean {
        return (Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).isNotEmpty() ||
                Npcs.stream().name("Deranged archaeologist").any { it.overheadMessage() == script.SPECIAL_ATTACK_TEXT })
                && Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
    }

    override fun execute() {
        // --- NEW LOGIC: Only move if we are NOT already moving. ---
        // This is the most reliable way to ensure we only dodge once per attack.
        if (Players.local().inMotion()) {
            return // We are already dodging, do nothing.
        }

        script.logger.info("Special attack detected! Dodging...")
        val playerTile = Players.local().tile()
        val boss = script.getBoss()

        val searchRadius = 10
        val southWestTile = Tile(playerTile.x() - searchRadius, playerTile.y() - searchRadius, playerTile.floor())
        val northEastTile = Tile(playerTile.x() + searchRadius, playerTile.y() + searchRadius, playerTile.floor())
        val searchArea = Area(southWestTile, northEastTile)

        val safeTile = searchArea.tiles.filter { tile ->
            val distanceToPlayer = tile.distanceTo(playerTile)
            val distanceToBoss = if (boss != null) boss.distanceTo(tile) else 99.0

            distanceToPlayer >= 6 && distanceToBoss >= 2 && tile.reachable()
        }.randomOrNull()

        if (safeTile != null) {
            // Perform the single step action.
            Movement.step(safeTile)
            // Wait until we start moving. After this, the inMotion() check will prevent further steps.
            Condition.wait({ Players.local().inMotion() }, 150, 5)
        } else {
            script.logger.warn("Could not find a valid safe tile for special attack dodge!")
        }
    }
}