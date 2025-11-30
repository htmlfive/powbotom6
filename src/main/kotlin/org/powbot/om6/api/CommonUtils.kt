package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Tile

/**
 * Common utility functions shared across all scripts.
 */
object CommonUtils {

    // ========================================
    // SLEEPING
    // ========================================

    /**
     * Sleeps for a random duration within range.
     * @param min Minimum sleep time in ms
     * @param max Maximum sleep time in ms
     */
    fun sleep(min: Int, max: Int) {
        Condition.sleep(Random.nextInt(min, max))
    }

    /**
     * Sleeps for a random duration around a base value (±offset).
     * @param baseMs Base sleep time in ms
     * @param offset Maximum offset in ms (default: 200)
     */
    fun sleepRandom(baseMs: Int, offset: Int = 200) {
        val sleepTime = (baseMs + Random.nextInt(-offset, offset + 1)).coerceAtLeast(0)
        Condition.sleep(sleepTime)
    }

    /**
     * Sleeps for a Gaussian-distributed duration.
     * @param mean Mean sleep time in ms
     * @param stdDev Standard deviation in ms
     */
    fun sleepGaussian(mean: Int, stdDev: Int) {
        val gaussian = java.util.Random().nextGaussian()
        val sleepTime = (mean + gaussian * stdDev).toInt().coerceAtLeast(0)
        Condition.sleep(sleepTime)
    }

    // ========================================
    // PARSING
    // ========================================

    /**
     * Parses a comma-separated string into a list.
     * @param input The comma-separated string
     * @return List of trimmed, non-empty strings
     */
    fun parseCommaSeparated(input: String): List<String> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Parses coordinates in "X,Y,Z" format into a Tile.
     * @param coords The coordinate string
     * @return The Tile or Tile.Nil if parsing fails
     */
    fun parseTile(coords: String): Tile {
        return try {
            val parts = coords.split(",").map { it.trim().toInt() }
            if (parts.size == 3) {
                Tile(parts[0], parts[1], parts[2])
            } else if (parts.size == 2) {
                Tile(parts[0], parts[1], 0)
            } else {
                Tile.Nil
            }
        } catch (e: Exception) {
            Tile.Nil
        }
    }

    // ========================================
    // FORMATTING
    // ========================================

    /**
     * Formats a number with K/M suffixes.
     * @param number The number to format
     * @return Formatted string (e.g., "1.5m", "500k", "999")
     */
    fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fm", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fk", number / 1_000.0)
            else -> number.toString()
        }
    }

    /**
     * Formats a tile as a readable string.
     * @param tile The tile to format
     * @return Formatted string "(X, Y, Z)"
     */
    fun formatTile(tile: Tile): String {
        return "(${tile.x}, ${tile.y}, ${tile.floor})"
    }

    /**
     * Formats milliseconds as a time string.
     * @param ms Milliseconds to format
     * @return Formatted string "HH:MM:SS"
     */
    fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // ========================================
    // VALIDATION
    // ========================================

    /**
     * Checks if a tile is valid (not Nil and has reasonable coordinates).
     * @param tile The tile to validate
     * @return true if tile is valid
     */
    fun isValidTile(tile: Tile): Boolean {
        if (tile == Tile.Nil) return false
        return tile.x in 1..25000 &&
               tile.y in 1..25000 &&
               tile.floor in 0..3
    }

    // ========================================
    // RANDOM
    // ========================================

    /**
     * Gets a random integer in range (inclusive).
     * @param min Minimum value
     * @param max Maximum value
     * @return Random integer
     */
    fun random(min: Int, max: Int): Int {
        return Random.nextInt(min, max + 1)
    }

    /**
     * Gets a random boolean.
     * @return Random true or false
     */
    fun randomBoolean(): Boolean {
        return Random.nextBoolean()
    }

    /**
     * Gets a random boolean with weighted probability.
     * @param trueChance Chance of true (0.0 to 1.0)
     * @return Weighted random boolean
     */
    fun randomBoolean(trueChance: Double): Boolean {
        return Random.nextDouble() < trueChance
    }
}
