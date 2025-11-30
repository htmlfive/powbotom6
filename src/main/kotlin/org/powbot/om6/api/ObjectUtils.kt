package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Tile
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.GameObject
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players

/**
 * Common game object utilities shared across all scripts.
 */
object ObjectUtils {

    // ========================================
    // FINDING OBJECTS
    // ========================================

    /**
     * Finds the nearest game object by name.
     * @param name The object name
     * @return The object or null if not found
     */
    fun findByName(name: String): GameObject? {
        return Objects.stream().name(name).nearest().firstOrNull()
    }

    /**
     * Finds the nearest game object by ID.
     * @param id The object ID
     * @return The object or null if not found
     */
    fun findById(id: Int): GameObject? {
        return Objects.stream().id(id).nearest().firstOrNull()
    }

    /**
     * Finds the nearest game object by name with a specific action.
     * @param name The object name
     * @param action The required action
     * @return The object or null if not found
     */
    fun findByNameAndAction(name: String, action: String): GameObject? {
        return Objects.stream().name(name).action(action).nearest().firstOrNull()
    }

    /**
     * Finds a game object within range of a tile.
     * @param id The object ID
     * @param centerTile The center tile to search from
     * @param range The maximum distance
     * @return The object or null if not found
     */
    fun findWithinRange(id: Int, centerTile: Tile, range: Double): GameObject? {
        return Objects.stream()
            .id(id)
            .within(centerTile, range)
            .nearest()
            .firstOrNull()
    }

    // ========================================
    // INTERACTION
    // ========================================

    /**
     * Ensures an object is in viewport, turning camera if needed.
     * @param obj The game object
     * @return true if object is now in viewport
     */
    fun ensureInViewport(obj: GameObject): Boolean {
        if (!obj.valid()) return false

        if (!obj.inViewport()) {
            Camera.turnTo(obj)
            Condition.sleep(Random.nextInt(200, 400))
        }
        return obj.inViewport()
    }

    /**
     * Interacts with an object by name.
     * @param objectName The object name
     * @param action The action to perform
     * @return true if interaction was successful
     */
    fun interact(objectName: String, action: String): Boolean {
        val obj = findByName(objectName) ?: return false
        if (!obj.valid()) return false

        ensureInViewport(obj)
        return obj.interact(action)
    }

    /**
     * Interacts with an object by ID.
     * @param objectId The object ID
     * @param action The action to perform
     * @return true if interaction was successful
     */
    fun interact(objectId: Int, action: String): Boolean {
        val obj = findById(objectId) ?: return false
        if (!obj.valid()) return false

        ensureInViewport(obj)
        return obj.interact(action)
    }

    /**
     * Interacts with an object and waits for player to stop moving.
     * @param objectName The object name
     * @param action The action to perform
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if interaction completed
     */
    fun interactAndWait(objectName: String, action: String, timeout: Int = 5000): Boolean {
        if (!interact(objectName, action)) return false

        return Condition.wait(
            { !Players.local().inMotion() && Players.local().animation() == -1 },
            100,
            timeout / 100
        )
    }

    /**
     * Interacts with an object and waits for it to become invalid.
     * @param obj The game object
     * @param action The action to perform
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if object became invalid within timeout
     */
    fun interactUntilGone(obj: GameObject, action: String, timeout: Int = 5000): Boolean {
        if (!obj.valid()) return true

        ensureInViewport(obj)
        if (obj.interact(action)) {
            return Condition.wait({ !obj.valid() }, 100, timeout / 100)
        }
        return false
    }

    // ========================================
    // STAIRS / LADDERS
    // ========================================

    /**
     * Climbs stairs in the specified direction.
     * @param direction "Climb-up" or "Climb-down"
     * @param sleepMin Minimum sleep after climbing (default: 1200)
     * @param sleepMax Maximum sleep after climbing (default: 1800)
     * @return true if climb was successful
     */
    fun climbStairs(direction: String, sleepMin: Int = 1200, sleepMax: Int = 1800): Boolean {
        val stairs = Objects.stream()
            .name("Stairs")
            .action(direction)
            .nearest()
            .firstOrNull() ?: return false

        if (!stairs.valid()) return false

        ensureInViewport(stairs)

        // Step closer if needed
        val distance = Players.local().tile().distanceTo(stairs.tile())
        if (distance > 3) {
            org.powbot.api.rt4.Movement.step(stairs.tile())
            Condition.wait(
                { Players.local().tile().distanceTo(stairs.tile()) <= 5 },
                100,
                30
            )
        }

        if (stairs.interact(direction)) {
            Condition.wait(
                { !Players.local().inMotion() && Players.local().animation() == -1 && !stairs.inViewport() },
                100,
                50
            )
            Condition.sleep(Random.nextInt(sleepMin, sleepMax))
            return true
        }

        return false
    }

    /**
     * Climbs a ladder in the specified direction.
     * @param direction "Climb-up" or "Climb-down"
     * @param sleepMin Minimum sleep after climbing (default: 1200)
     * @param sleepMax Maximum sleep after climbing (default: 1800)
     * @return true if climb was successful
     */
    fun climbLadder(direction: String, sleepMin: Int = 1200, sleepMax: Int = 1800): Boolean {
        val ladder = Objects.stream()
            .name("Ladder")
            .action(direction)
            .nearest()
            .firstOrNull() ?: return false

        if (!ladder.valid()) return false

        ensureInViewport(ladder)

        if (ladder.interact(direction)) {
            Condition.wait(
                { !Players.local().inMotion() && Players.local().animation() == -1 },
                100,
                50
            )
            Condition.sleep(Random.nextInt(sleepMin, sleepMax))
            return true
        }

        return false
    }
}
