package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Players

/**
 * Common player utilities shared across all scripts.
 */
object PlayerUtils {

    // ========================================
    // STATUS CHECKS
    // ========================================

    /**
     * Checks if the player is idle (not animating and not moving).
     * @return true if player is completely idle
     */
    fun isIdle(): Boolean {
        val player = Players.local()
        return player.animation() == -1 && !player.inMotion()
    }

    /**
     * Checks if the player is animating.
     * @return true if player has an active animation
     */
    fun isAnimating(): Boolean {
        return Players.local().animation() != -1
    }

    /**
     * Checks if the player is moving.
     * @return true if player is in motion
     */
    fun isMoving(): Boolean {
        return Players.local().inMotion()
    }

    /**
     * Checks if the player is in combat.
     * @return true if player is in combat
     */
    fun isInCombat(): Boolean {
        return Players.local().inCombat()
    }

    /**
     * Checks if the player is interacting with something.
     * @return true if player is interacting
     */
    fun isInteracting(): Boolean {
        return Players.local().interacting().valid()
    }

    /**
     * Gets the player's current animation ID.
     * @return Animation ID or -1 if idle
     */
    fun getAnimation(): Int {
        return Players.local().animation()
    }

    // ========================================
    // HEALTH
    // ========================================

    /**
     * Gets the player's current health percentage.
     * @return Health as percentage (0-100)
     */
    fun healthPercent(): Int {
        return org.powbot.api.rt4.Combat.healthPercent()
    }

    /**
     * Gets the player's current health.
     * @return Current health points
     */
    fun currentHealth(): Int {
        return org.powbot.api.rt4.Combat.health()
    }

    /**
     * Checks if player health is below a threshold.
     * @param percent The health percentage threshold
     * @return true if health is below threshold
     */
    fun healthBelow(percent: Int): Boolean {
        return healthPercent() < percent
    }

    /**
     * Checks if player is poisoned.
     * @return true if poisoned
     */
    fun isPoisoned(): Boolean {
        return org.powbot.api.rt4.Combat.isPoisoned()
    }

    // ========================================
    // NEARBY PLAYERS
    // ========================================

    /**
     * Checks if another player is nearby.
     * @param range Distance to check (default: 10)
     * @return true if another player is within range
     */
    fun isPlayerNearby(range: Int = 10): Boolean {
        return Players.stream()
            .within(range)
            .firstOrNull { it != Players.local() } != null
    }

    /**
     * Checks if another player is on the same tile.
     * @return true if another player shares the tile
     */
    fun isPlayerOnTile(): Boolean {
        val localPlayer = Players.local()
        return Players.stream()
            .at(localPlayer.tile())
            .any { it != localPlayer }
    }

    /**
     * Gets the count of nearby players.
     * @param range Distance to check (default: 10)
     * @return Number of other players within range
     */
    fun nearbyPlayerCount(range: Int = 10): Int {
        return Players.stream()
            .within(range)
            .filter { it != Players.local() }
            .count()
            .toInt()
    }

    // ========================================
    // WAITING
    // ========================================

    /**
     * Waits until the player is idle.
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if player became idle within timeout
     */
    fun waitUntilIdle(timeout: Int = 5000): Boolean {
        return Condition.wait({ isIdle() }, 100, timeout / 100)
    }

    /**
     * Waits until the player stops moving.
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if player stopped moving within timeout
     */
    fun waitUntilStationary(timeout: Int = 5000): Boolean {
        return Condition.wait({ !isMoving() }, 100, timeout / 100)
    }

    /**
     * Waits until the player finishes animating.
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if animation finished within timeout
     */
    fun waitUntilNotAnimating(timeout: Int = 5000): Boolean {
        return Condition.wait({ !isAnimating() }, 100, timeout / 100)
    }
}
