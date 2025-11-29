package org.powbot.webwalk

import org.powbot.api.Tile
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript

/**
 * Utility functions for the WebWalker script.
 */
object ScriptUtils {

    /**
     * Parses a coordinate string in the format "X,Y,Z" into a Tile object.
     *
     * @param locationString The coordinate string to parse
     * @param script The script instance for logging
     * @return A valid Tile object, or Tile.Nil if parsing fails
     */
    fun parseTileFromString(locationString: String, script: AbstractScript): Tile {
        val coordString = locationString.trim()

        return try {
            val parts = coordString.split(Constants.COORDINATE_DELIMITER).map { it.trim() }

            if (parts.size == Constants.EXPECTED_COORDINATE_PARTS) {
                val x = parts[0].toInt()
                val y = parts[1].toInt()
                val z = parts[2].toInt()

                script.logger.info("Parsed tile: X=$x, Y=$y, Z=$z")
                Tile(x, y, z)
            } else {
                script.logger.error(
                    "Invalid coordinate format. Must be X,Y,Z. Received: $coordString"
                )
                Tile.Nil
            }
        } catch (e: NumberFormatException) {
            script.logger.error(
                "Failed to parse coordinates from: $locationString. Coordinates must be integers.",
                e
            )
            Tile.Nil
        } catch (e: Exception) {
            script.logger.error("Unexpected error parsing coordinates: $locationString", e)
            Tile.Nil
        }
    }

    /**
     * Formats a tile into a readable string.
     *
     * @param tile The tile to format
     * @return Formatted string representation of the tile
     */
    fun formatTile(tile: Tile): String {
        return "(${tile.x}, ${tile.y}, ${tile.floor})"
    }

    /**
     * Validates that a tile is not Tile.Nil and has reasonable coordinates.
     *
     * @param tile The tile to validate
     * @return true if the tile is valid, false otherwise
     */
    fun isValidTile(tile: Tile): Boolean {
        if (tile == Tile.Nil) return false

        return tile.x in 1..25000 &&
                tile.y in 1..25000 &&
                tile.floor in 0..3
    }

    /**
     * Checks if a tile is walkable by checking if it's a valid matrix tile.
     * This is a lightweight check that doesn't require pathfinding.
     *
     * @param tile The tile to check
     * @return true if the tile appears walkable, false otherwise
     */
    fun isTileWalkable(tile: Tile): Boolean {
        return try {
            // Check if the tile matrix exists and is valid
            // A tile with a valid matrix is likely walkable
            tile.matrix().valid()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Searches for a valid walkable tile within a specified range of the target tile.
     * Uses a spiral search pattern starting from the center.
     *
     * @param centerTile The target tile to search around
     * @param range The search range (will search range*2+1 tiles in each direction)
     * @param script The script instance for logging
     * @return A walkable tile if found, or the original tile if none found
     */
    fun findNearestWalkableTile(centerTile: Tile, range: Int, script: AbstractScript): Tile {
        script.logger.info("Searching for walkable tile near ${formatTile(centerTile)} within range $range...")

        // First check if the center tile itself is walkable
        if (isTileWalkable(centerTile)) {
            script.logger.info("Center tile is walkable")
            return centerTile
        }

        // Spiral search pattern: start from center and move outward
        val tilesToCheck = mutableListOf<Tile>()

        // Generate tiles in spiral order
        for (distance in 1..range) {
            for (dx in -distance..distance) {
                for (dy in -distance..distance) {
                    // Only check tiles at the current distance (edge of square)
                    if (Math.abs(dx) == distance || Math.abs(dy) == distance) {
                        val checkTile = Tile(
                            centerTile.x + dx,
                            centerTile.y + dy,
                            centerTile.floor
                        )
                        tilesToCheck.add(checkTile)
                    }
                }
            }
        }

        script.logger.info("Checking ${tilesToCheck.size} tiles for walkability...")

        // Check each tile
        var checkedCount = 0
        for (tile in tilesToCheck) {
            checkedCount++

            if (isValidTile(tile) && isTileWalkable(tile)) {
                val distance = Math.sqrt(
                    Math.pow((tile.x - centerTile.x).toDouble(), 2.0) +
                            Math.pow((tile.y - centerTile.y).toDouble(), 2.0)
                )
                script.logger.info(
                    "Found walkable tile at ${formatTile(tile)} " +
                            "(${String.format("%.1f", distance)} tiles from target, checked $checkedCount tiles)"
                )
                return tile
            }

            // Log progress every 20 tiles
            if (checkedCount % 20 == 0) {
                script.logger.info("Checked $checkedCount/${tilesToCheck.size} tiles...")
            }
        }

        script.logger.warn("No walkable tile found within range $range. Using original tile.")
        return centerTile
    }

    /**
     * Calculates and formats the distance to a tile.
     *
     * @param distance The distance value
     * @return Formatted distance string
     */
    fun formatDistance(distance: Double): String {
        return "%.1f tiles".format(distance)
    }
}