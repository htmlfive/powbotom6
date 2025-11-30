package org.powbot.om6.moths

import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.om6.api.*

/**
 * Script-specific utilities for the Moonlight Moth Catcher.
 * Uses shared API utilities where possible.
 */
object ScriptUtils {

    /**
     * Checks if another player is nearby within detection range.
     */
    fun isPlayerNearby(range: Int = Constants.PLAYER_DETECTION_DISTANCE): Boolean {
        return PlayerUtils.isPlayerNearby(range)
    }

    /**
     * Checks if the player is near the moth catching location.
     */
    fun isNearMoths(): Boolean {
        return MovementUtils.isWithinRange(Constants.MOTH_LOCATION, Constants.NEAR_MOTHS_DISTANCE)
    }

    /**
     * Checks if the player is at the moth catching location (close enough to hop).
     */
    fun isAtMothLocation(): Boolean {
        return MovementUtils.isWithinRange(Constants.MOTH_LOCATION, Constants.PLAYER_DETECTION_DISTANCE)
    }

    /**
     * Checks if player has butterfly jars in inventory.
     */
    fun hasJars(): Boolean {
        return InventoryUtils.contains(Constants.BUTTERFLY_JAR_NAME)
    }

    /**
     * Checks if player has caught moths in inventory.
     */
    fun hasCaughtMoths(): Boolean {
        return InventoryUtils.contains(Constants.MOONLIGHT_MOTH_NAME)
    }

    /**
     * Enables running if energy is above threshold.
     */
    fun enableRunning(): Boolean {
        return MovementUtils.enableRunning(Constants.MIN_RUN_ENERGY, Constants.MAX_RUN_ENERGY)
    }

    /**
     * Finds a valid moonlight moth to catch.
     */
    fun findValidMoth(): Npc? {
        return Npcs.stream()
            .id(Constants.MOONLIGHT_MOTH_ID)
            .nearest().firstOrNull { it.tile().y() >= Constants.MINIMUM_Y_AXIS }
    }

    /**
     * Climbs stairs in the specified direction.
     */
    fun climbStairs(action: String): Boolean {
        return ObjectUtils.climbStairs(action, Constants.STAIRS_SLEEP_MIN, Constants.STAIRS_SLEEP_MAX)
    }

    /**
     * Traverses the path to moth location.
     */
    fun traverseToMoths() {
        for (tile in Constants.PATH_TO_MOTHS) {
            MovementUtils.stepToAndWait(tile, Constants.CLOSE_TO_TILE_DISTANCE)
        }
    }

    /**
     * Gets a random valid world for hopping.
     */
    fun getValidWorld() = WorldUtils.findRandomWorld(Constants.MIN_WORLD_POPULATION)
}
