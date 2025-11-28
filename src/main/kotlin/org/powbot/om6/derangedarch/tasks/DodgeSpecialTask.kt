package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.Constants
import kotlin.math.abs
import kotlin.math.atan2

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private fun isSpecialAttackActive(): Boolean {
        val projectileExists = Projectiles.stream().id(Constants.SPECIAL_ATTACK_PROJECTILE).isNotEmpty()

        // Debug: log ALL overhead messages from the boss
        val boss = Npcs.stream().name("Deranged archaeologist").firstOrNull()
        if (boss != null) {
            val overhead = boss.overheadMessage() ?: ""
            if (overhead.isNotEmpty()) {
                script.logger.debug("Boss overhead message: '$overhead' (comparing to '${Constants.SPECIAL_ATTACK_TEXT}')")
            }
        }

        val overheadText = Npcs.stream().name("Deranged archaeologist").any { it.overheadMessage() == Constants.SPECIAL_ATTACK_TEXT }

        if (projectileExists) script.logger.debug("Active special attack projectile detected.")
        if (overheadText) script.logger.debug("Special attack overhead text detected.")

        return projectileExists || overheadText
    }

    override fun validate(): Boolean {
        if (Players.local().inMotion()) return false

        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= 10
        val specialActive = isSpecialAttackActive()

        val shouldRun = specialActive && inFightArea
        if (shouldRun) {
            script.logger.debug("Validate OK: Special attack is active, player is idle and in the fight area.")
        }
        return shouldRun
    }

    private fun angleBetween(from: Tile, to: Tile): Double {
        return Math.toDegrees(atan2((to.y() - from.y()).toDouble(), (to.x() - from.x()).toDouble()))
    }

    /**
     * Filters the fixed DODGE_TILES list based on safety and reachability.
     * Returns a list of safe tiles in a randomized order.
     */
    private fun findPotentialSafeTiles(playerTile: Tile, boss: Npc?, allProjectiles: List<Projectile>): List<Tile> {
        val projectileTiles = allProjectiles.map { it.tile() }

        // Get angle to boss for filtering
        val angleToBoss = if (boss != null && boss.valid()) angleBetween(playerTile, boss.tile()) else null

        return Constants.DODGE_TILES.filter { tile ->
            // 1. Don't try to dodge to the tile we are already on
            if (tile == playerTile) return@filter false

            // 2. Check distance from player (must be at least MIN_DODGE_DISTANCE)
            val distanceToPlayer = tile.distanceTo(playerTile)
            if (distanceToPlayer < Constants.MIN_DODGE_DISTANCE) {
                script.logger.debug("Tile $tile is too close (Dist: $distanceToPlayer). Needs to be >= ${Constants.MIN_DODGE_DISTANCE}.")
                return@filter false
            }

            // 3. Check distance to projectiles
            val isAwayFromProjectiles = projectileTiles.all { projTile -> tile.distanceTo(projTile) > Constants.PROJECTILE_DANGER_DISTANCE }
            if (!isAwayFromProjectiles) {
                script.logger.debug("Tile $tile is on or too close to a projectile.")
                return@filter false
            }

            // 4. Check "avoidsBossPath" (don't walk through the boss)
            val avoidsBossPath = if (angleToBoss != null) {
                val angleToTile = angleBetween(playerTile, tile)
                var angleDiff = abs(angleToBoss - angleToTile)
                if (angleDiff > 180) {
                    angleDiff = 360 - angleDiff
                }
                angleDiff > Constants.MIN_DODGE_ANGLE_DIFFERENCE
            } else {
                true // If boss isn't valid, any tile is fine
            }
            if (!avoidsBossPath) {
                script.logger.debug("Tile $tile is in the same direction as the boss.")
                return@filter false
            }

            // 5. Check reachability (most expensive check, do it last)
            val isReachable = Movement.reachable(playerTile, tile)
            if (!isReachable) {
                script.logger.debug("Tile $tile is not reachable.")
                return@filter false
            }

            true // Tile passed all checks

        }.shuffled() // Randomize the order of valid safe tiles
    }


    override fun execute() {
        script.logger.debug("Executing DodgeSpecialTask...")
        val player = Players.local()

        if (player.inMotion() || !isSpecialAttackActive()) {
            script.logger.debug("Execute exit: Player in motion or special attack ended.")
            return
        }

        script.logger.info("Special attack active and player is idle. Initiating dodge sequence.")

        var dodgeSuccess = false
        var dodgeAttempts = 0

        while (dodgeAttempts < Constants.MAX_DODGE_ATTEMPTS && !dodgeSuccess) {
            dodgeAttempts++
            script.logger.debug("Dodge attempt $dodgeAttempts / ${Constants.MAX_DODGE_ATTEMPTS}")

            val currentBoss = script.getBoss()
            val allCurrentProjectiles = Projectiles.stream().toList()
            script.logger.debug("Checking against ${allCurrentProjectiles.size} total projectiles.")
            val playerTile = player.tile()

            // This function now returns your 4 tiles, filtered (by 5+ dist), and SHUFFLED.
            val potentialSafeTiles = findPotentialSafeTiles(playerTile, currentBoss, allCurrentProjectiles)
            script.logger.debug("Found ${potentialSafeTiles.size} potential safe tiles for attempt $dodgeAttempts.")

            // Try the first (now random) safe tile from the list
            val bestSafeTile = potentialSafeTiles.firstOrNull()

            if (bestSafeTile != null) {
                script.logger.debug("Attempting step to best (random) safe tile: $bestSafeTile")
                if (Movement.step(bestSafeTile)) {
                    script.logger.debug("Step initiated towards $bestSafeTile. Monitoring movement...")

                    val waitResult = Condition.wait({
                        val currentProjsDuringWait = Projectiles.stream().toList()
                        val tooClose = currentProjsDuringWait.any { player.tile().distanceTo(it.tile()) <= Constants.PROJECTILE_DANGER_DISTANCE }

                        if (tooClose) {
                            script.logger.warn("Dodge interrupted! Too close to a projectile (any type) during movement.")
                            return@wait false
                        }
                        !player.inMotion()
                    }, 200, 60)

                    if (waitResult) {
                        dodgeSuccess = true
                        script.logger.info("Dodge move complete to $bestSafeTile.")
                    } else {
                        script.logger.warn("Wait failed for step to $bestSafeTile (Interrupted or Timeout).")
                    }

                } else {
                    script.logger.warn("Movement.step() to $bestSafeTile failed immediately.")
                }
            } else {
                script.logger.warn("Could not find any valid safe tile from the list for attempt $dodgeAttempts.")
                // No point in retrying if the list is empty, so break the loop
                break
            }

            // If dodge succeeded, the outer loop will break
            if (dodgeSuccess) break

            // If step failed immediately, loop will retry (e.g., if projectiles moved)
        }


        if (dodgeSuccess) {
            script.logger.info("Successfully dodged special attack. Checking safety before re-engaging boss...")

            val finalProjectiles = Projectiles.stream().toList()
            val isSafeFromProjectiles = finalProjectiles.all { player.tile().distanceTo(it.tile()) > Constants.PROJECTILE_DANGER_DISTANCE }

            if (isSafeFromProjectiles) {
                script.logger.debug("Player is in a safe position. Re-engaging boss...")
                val finalBoss = script.getBoss()
                if (finalBoss != null && finalBoss.valid()) {
                    if (!finalBoss.inViewport()) {
                        script.logger.debug("Turning camera to boss for re-attack.")
                        Camera.turnTo(finalBoss)
                    }
                    if (finalBoss.interact("Attack")) {
                        Condition.wait({ player.interacting() == finalBoss }, 600, 10)
                    }
                } else {
                    script.logger.debug("Boss is null or invalid after dodging, cannot re-engage.")
                }
            } else {
                script.logger.warn("Dodge complete, but still too close to a projectile (any type). Holding position before re-engaging.")
            }

        } else {
            script.logger.warn("Failed to complete dodge sequence after $dodgeAttempts attempts!")
        }
    }
}