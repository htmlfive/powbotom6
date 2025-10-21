package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import kotlin.math.abs
import kotlin.math.atan2

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private val PROJECTILE_DANGER_DISTANCE = 1.0
    private val MAX_DODGE_ATTEMPTS = 10
    private val MIN_DODGE_ANGLE_DIFFERENCE = 30.0

    private fun isSpecialAttackActive(): Boolean {
        val projectileExists = Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).isNotEmpty()
        val overheadText = Npcs.stream().name("Deranged archaeologist").any { it.overheadMessage() == script.SPECIAL_ATTACK_TEXT }

        if (projectileExists) script.logger.debug("Active special attack projectile detected.")
        if (overheadText) script.logger.debug("Special attack overhead text detected.")

        return projectileExists || overheadText
    }

    override fun validate(): Boolean {
        if (Players.local().inMotion()) return false

        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
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

    private fun findPotentialSafeTiles(playerTile: Tile, boss: Npc?, currentProjectiles: List<Projectile>): List<Tile> {
        val searchRadius = 14
        val southWestTile = Tile(playerTile.x() - searchRadius, playerTile.y() - searchRadius, playerTile.floor())
        val northEastTile = Tile(playerTile.x() + searchRadius, playerTile.y() + searchRadius, playerTile.floor())
        val searchArea = Area(southWestTile, northEastTile)
        val projectileTiles = currentProjectiles.map { it.tile() }

        return searchArea.tiles.filter { tile ->
            val distanceToPlayer = tile.distanceTo(playerTile)
            val distanceToBoss = if (boss != null) boss.distanceTo(tile) else 99.0
            val isAwayFromProjectiles = projectileTiles.all { projTile -> tile.distanceTo(projTile) > PROJECTILE_DANGER_DISTANCE }

            val avoidsBossPath = if (boss != null && boss.valid()) {
                val angleToBoss = angleBetween(playerTile, boss.tile())
                val angleToTile = angleBetween(playerTile, tile)
                var angleDiff = abs(angleToBoss - angleToTile)
                if (angleDiff > 180) {
                    angleDiff = 360 - angleDiff
                }
                angleDiff > MIN_DODGE_ANGLE_DIFFERENCE
            } else {
                true
            }

            distanceToPlayer.toInt() in 6..14 &&
                    distanceToBoss >= 2 &&
                    isAwayFromProjectiles &&
                    avoidsBossPath &&
                    Movement.reachable(playerTile, tile)
        }.shuffled()
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

        while (dodgeAttempts < MAX_DODGE_ATTEMPTS && !dodgeSuccess) {
            dodgeAttempts++
            script.logger.debug("Dodge attempt $dodgeAttempts / $MAX_DODGE_ATTEMPTS")

            val currentBoss = script.getBoss()
            val currentProjectiles = Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).toList()
            val playerTile = player.tile()

            val potentialSafeTiles = findPotentialSafeTiles(playerTile, currentBoss, currentProjectiles)
            script.logger.debug("Found ${potentialSafeTiles.size} potential safe tiles for attempt $dodgeAttempts.")

            var stepInitiatedForThisAttempt = false
            for (safeTile in potentialSafeTiles.take(10)) {
                script.logger.debug("Attempting step to potentially reachable safe tile: $safeTile")
                if (Movement.step(safeTile)) {
                    stepInitiatedForThisAttempt = true
                    script.logger.debug("Step initiated towards $safeTile. Monitoring movement...")

                    // MODIFIED: Increased check frequency for faster reaction time.
                    val waitResult = Condition.wait({
                        val currentProjsDuringWait = Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).toList()
                        val tooClose = currentProjsDuringWait.any { player.tile().distanceTo(it.tile()) <= PROJECTILE_DANGER_DISTANCE }

                        if (tooClose) {
                            script.logger.warn("Dodge interrupted! Too close to a projectile during movement.")
                            return@wait false
                        }
                        !player.inMotion()
                    }, 200, 60) // Check every 200ms for up to 12 seconds

                    if (waitResult) {
                        dodgeSuccess = true
                        script.logger.info("Dodge move complete to $safeTile.")
                    } else {
                        script.logger.warn("Wait failed for step to $safeTile (Interrupted or Timeout).")
                    }
                    break
                } else {
                    script.logger.warn("Movement.step() to $safeTile failed immediately.")
                }
            }

            if (!stepInitiatedForThisAttempt) {
                script.logger.warn("Could not initiate step to any potential safe tile for attempt $dodgeAttempts.")
                break
            }
        }


        if (dodgeSuccess) {
            script.logger.info("Successfully dodged special attack. Re-engaging boss...")
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
            script.logger.warn("Failed to complete dodge sequence after $MAX_DODGE_ATTEMPTS attempts!")
        }
    }
}

