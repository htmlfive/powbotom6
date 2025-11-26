package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs
import kotlin.math.abs
import kotlin.math.atan2

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    private val PROJECTILE_DANGER_DISTANCE = 1.0
    private val MAX_DODGE_ATTEMPTS = 10
    private val MIN_DODGE_ANGLE_DIFFERENCE = 25.0 // Min angle diff to "not walk through boss"
    private val MIN_DODGE_DISTANCE = 5.0 // --- NEW: Minimum distance to dodge ---

    // The 4 specific dodge tiles you requested
    private val DODGE_TILES = listOf(
        Tile(3683, 3703, 0),
        Tile(3687, 3706, 0),
        Tile(3683, 3710, 0),
        Tile(3678, 3706, 0)
    )

    private fun isSpecialAttackActive(): Boolean {
        val projectileExists = Projectiles.stream().id(IDs.SPECIAL_ATTACK_PROJECTILE).isNotEmpty()
        val overheadText = Npcs.stream().name(IDs.DERANGED_ARCHAEOLOGIST_NAME).firstOrNull()?.overheadText()
        val textExists = overheadText != null && overheadText.contains("Rain")

        if (projectileExists || textExists) {
            script.logger.debug("Special attack detected: Projectile: $projectileExists, Text: $textExists")
            return true
        }
        return false
    }

    override fun validate(): Boolean {
        // Do not dodge if we are trying to escape
        if (script.emergencyTeleportJustHappened) return false

        val player = Players.local()
        val inFightArea = player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8

        // Only run if we are in the fight area AND the special is active
        val shouldRun = inFightArea && isSpecialAttackActive()

        if (shouldRun) {
            script.logger.debug("Validate OK: In fight area and special attack is active.")
        }

        return shouldRun
    }

    override fun execute() {
        script.logger.info("Special attack detected, initiating dodge sequence.")
        script.logger.debug("Executing DodgeSpecialTask...")

        val boss = script.getBoss()
        if (boss == null) {
            script.logger.warn("Boss is null, cannot determine best dodge tile. Aborting dodge.")
            return
        }

        val player = Players.local()

        var dodgeSuccess = false
        var dodgeAttempts = 0

        while (!dodgeSuccess && dodgeAttempts < MAX_DODGE_ATTEMPTS) {
            dodgeAttempts++
            script.logger.debug("Dodge attempt #$dodgeAttempts...")

            // 1. Find the current projectile (if it exists)
            val currentProjectile = Projectiles.stream().id(IDs.SPECIAL_ATTACK_PROJECTILE).nearest().firstOrNull()
            if (currentProjectile == null) {
                script.logger.debug("Projectile vanished or not found in time (after $dodgeAttempts attempts). Marking success.")
                dodgeSuccess = true
                break
            }

            // 2. Find the best tile to step to (furthest from projectile and not walking through the boss)
            val projectileTile = currentProjectile.targetTile() ?: currentProjectile.tile()
            val bestDodgeTile = DODGE_TILES
                .filter { Movement.reachable(player.tile(), it) }
                .maxByOrNull { it.distanceTo(projectileTile) }

            if (bestDodgeTile != null) {
                script.logger.debug("Projectiles at $projectileTile. Best dodge tile: $bestDodgeTile (Distance: ${bestDodgeTile.distanceTo(projectileTile)})")

                if (bestDodgeTile.distanceTo(player) < MIN_DODGE_DISTANCE) {
                    script.logger.debug("Best dodge tile is too close to current position, waiting for next tick movement.")
                    Condition.sleep(150)
                    continue
                }

                if (Movement.step(bestDodgeTile)) {
                    script.logger.debug("Stepped to $bestDodgeTile. Waiting for movement to complete/next projectile tick.")

                    // Wait for the character to start moving OR the projectile to land
                    Condition.wait({ player.inMotion() || Projectiles.stream().id(IDs.SPECIAL_ATTACK_PROJECTILE).isEmpty() }, 150, 10)

                    // If we stopped moving OR the projectile is gone, check if we're safe.
                    if (!player.inMotion() || Projectiles.stream().id(IDs.SPECIAL_ATTACK_PROJECTILE).isEmpty()) {
                        // If the projectile is gone, we successfully dodged this wave
                        if (Projectiles.stream().id(IDs.SPECIAL_ATTACK_PROJECTILE).isEmpty()) {
                            script.logger.debug("Projectile is gone. Dodge successful.")
                            dodgeSuccess = true
                            break
                        }
                        // If we stopped moving and projectile is still there, re-evaluate next loop.
                        script.logger.debug("Stopped moving but projectile is still active. Re-evaluating position.")
                    }
                } else {
                    script.logger.warn("Failed to execute movement step to $bestDodgeTile.")
                    Condition.sleep(150) // Short pause before re-trying
                }
            } else {
                script.logger.warn("Could not find a reachable dodge tile! Aborting dodge.")
                // Set success to true to exit the loop and continue with the fight
                dodgeSuccess = true
                break
            }
            Condition.sleep(150) // Don't spam immediately, loop will retry (e.g., if projectiles moved)
        }


        if (dodgeSuccess) {
            script.logger.info("Successfully dodged special attack. Checking safety before re-engaging boss...")

            val finalProjectiles = Projectiles.stream().toList()
            val isSafeFromProjectiles = finalProjectiles.all { player.tile().distanceTo(it.tile()) > PROJECTILE_DANGER_DISTANCE }

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