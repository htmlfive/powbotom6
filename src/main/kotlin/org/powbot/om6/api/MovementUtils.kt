package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players

/**
 * Common movement utilities shared across all scripts.
 */
object MovementUtils {

    // ========================================
    // RUNNING
    // ========================================

    /**
     * Enables running if energy is above threshold.
     * @param minEnergy Minimum energy to enable running (default: 20)
     * @param maxEnergy Maximum threshold for randomization (default: 35)
     * @return true if running is now enabled
     */
    fun enableRunning(minEnergy: Int = 20, maxEnergy: Int = 35): Boolean {
        if (Movement.running()) return true

        val threshold = Random.nextInt(minEnergy, maxEnergy + 1)
        if (Movement.energyLevel() > threshold) {
            return Movement.running(true)
        }
        return false
    }

    /**
     * Checks if running is currently enabled.
     * @return true if running is enabled
     */
    fun isRunning(): Boolean {
        return Movement.running()
    }

    /**
     * Gets the current run energy level.
     * @return Current energy (0-100)
     */
    fun energyLevel(): Int {
        return Movement.energyLevel()
    }

    // ========================================
    // DISTANCE & POSITION
    // ========================================

    /**
     * Gets the distance from the player to a tile.
     * @param tile The target tile
     * @return Distance in tiles
     */
    fun distanceTo(tile: Tile): Double {
        return Players.local().tile().distanceTo(tile)
    }

    /**
     * Checks if player is within range of a tile.
     * @param tile The target tile
     * @param range The maximum distance
     * @return true if within range
     */
    fun isWithinRange(tile: Tile, range: Int): Boolean {
        return distanceTo(tile) <= range
    }

    /**
     * Checks if player is at a specific tile.
     * @param tile The tile to check
     * @return true if player is on that tile
     */
    fun isAt(tile: Tile): Boolean {
        return Players.local().tile() == tile
    }

    /**
     * Checks if player is currently moving.
     * @return true if player is in motion
     */
    fun isMoving(): Boolean {
        return Players.local().inMotion()
    }

    // ========================================
    // WALKING
    // ========================================

    /**
     * Walks to a tile if not already there.
     * @param tile The destination tile
     * @return true if already there or walk initiated
     */
    fun walkTo(tile: Tile): Boolean {
        if (isAt(tile)) return true
        return Movement.walkTo(tile)
    }

    /**
     * Steps to a tile (shorter distance movement).
     * @param tile The destination tile
     * @return true if step initiated
     */
    fun stepTo(tile: Tile): Boolean {
        return Movement.step(tile)
    }

    /**
     * Walks to a tile and waits to arrive.
     * @param tile The destination tile
     * @param arrivalDistance Distance to consider "arrived" (default: 3)
     * @param timeout Maximum wait time in ms (default: 10000)
     * @return true if arrived at destination
     */
    fun walkToAndWait(tile: Tile, arrivalDistance: Int = 3, timeout: Int = 10000): Boolean {
        if (isWithinRange(tile, arrivalDistance)) return true

        if (Movement.walkTo(tile)) {
            return Condition.wait(
                { isWithinRange(tile, arrivalDistance) || !isMoving() },
                100,
                timeout / 100
            )
        }
        return false
    }

    /**
     * Steps to a tile and waits to arrive.
     * @param tile The destination tile
     * @param arrivalDistance Distance to consider "arrived" (default: 2)
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if arrived at destination
     */
    fun stepToAndWait(tile: Tile, arrivalDistance: Int = 2, timeout: Int = 5000): Boolean {
        if (isWithinRange(tile, arrivalDistance)) return true

        if (Movement.step(tile)) {
            return Condition.wait(
                { isWithinRange(tile, arrivalDistance) },
                100,
                timeout / 100
            )
        }
        return false
    }

    // ========================================
    // UTILITY
    // ========================================

    /**
     * Formats distance as a readable string.
     * @param distance The distance value
     * @return Formatted string like "5.2 tiles"
     */
    fun formatDistance(distance: Double): String {
        return "%.1f tiles".format(distance)
    }

    /**
     * Calculates angle between two tiles.
     * @param from Source tile
     * @param to Target tile
     * @return Angle in degrees
     */
    fun angleBetween(from: Tile, to: Tile): Double {
        return Math.toDegrees(
            kotlin.math.atan2(
                (to.y() - from.y()).toDouble(),
                (to.x() - from.x()).toDouble()
            )
        )
    }
}
