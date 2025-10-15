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

        // --- NEW: High-priority health check ---
        // Before doing anything else, check if we need to eat.
        if (Combat.healthPercent() < script.config.eatAtPercent) {
            script.logger.info("Health is low during special attack, eating first!")
            val food = Inventory.stream().name(script.config.foodName).firstOrNull()
            if (food != null && food.interact("Eat")) {
                // Wait for health to increase before proceeding.
                Condition.wait({ Combat.healthPercent() > script.config.eatAtPercent + 10 }, 150, 5)
            }
            // End the task for this cycle to prioritize eating.
            // The task will re-validate and run the dodge logic on the next tick.
            return
        }

        // --- Standard Dodge Logic ---
        // If we are already moving, we are in the process of dodging. Do nothing.
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
                Condition.wait({ player.inMotion() }, 150, 5)
            }
        } else {
            script.logger.warn("Could not find a valid safe tile for dodge!")
        }
    }
}