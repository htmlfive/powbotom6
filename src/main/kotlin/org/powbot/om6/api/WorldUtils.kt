package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.World
import org.powbot.api.rt4.Worlds

/**
 * Common world hopping utilities shared across all scripts.
 */
object WorldUtils {

    /** Blocked world specialties that should never be hopped to */
    private val BLOCKED_SPECIALTIES = setOf(
        World.Specialty.BOUNTY_HUNTER,
        World.Specialty.PVP,
        World.Specialty.TARGET_WORLD,
        World.Specialty.PVP_ARENA,
        World.Specialty.DEAD_MAN,
        World.Specialty.BETA,
        World.Specialty.HIGH_RISK,
        World.Specialty.LEAGUE,
        World.Specialty.SKILL_REQUIREMENT,
        World.Specialty.SPEEDRUNNING,
        World.Specialty.FRESH_START,
        World.Specialty.TRADE
    )

    /**
     * Finds a random suitable members world for hopping.
     * @param minPopulation Minimum world population (default: 15)
     * @param maxPopulation Maximum world population (default: Int.MAX_VALUE)
     * @param allowSpecialWorlds If true, allows non-blocked specialty worlds (default: false)
     * @return A suitable world or null if none found
     */
    fun findRandomWorld(
        minPopulation: Int = 15,
        maxPopulation: Int = Int.MAX_VALUE,
        allowSpecialWorlds: Boolean = false
    ): World? {
        return Worlds.stream()
            .filtered { world ->
                world.type() == World.Type.MEMBERS &&
                world.population >= minPopulation &&
                world.population <= maxPopulation &&
                world.specialty() !in BLOCKED_SPECIALTIES &&
                (allowSpecialWorlds || world.specialty() == World.Specialty.NONE)
            }
            .toList()
            .randomOrNull()
    }

    /**
     * Finds a random suitable F2P world for hopping.
     * @param minPopulation Minimum world population (default: 15)
     * @param maxPopulation Maximum world population (default: Int.MAX_VALUE)
     * @return A suitable world or null if none found
     */
    fun findRandomF2PWorld(
        minPopulation: Int = 15,
        maxPopulation: Int = Int.MAX_VALUE
    ): World? {
        return Worlds.stream()
            .filtered { world ->
                world.type() == World.Type.FREE &&
                world.population >= minPopulation &&
                world.population <= maxPopulation &&
                world.specialty() !in BLOCKED_SPECIALTIES
            }
            .toList()
            .randomOrNull()
    }

    /**
     * Hops to a specific world.
     * @param world The world to hop to
     * @param timeout Maximum wait time in ms (default: 10000)
     * @return true if hop was successful
     */
    fun hopTo(world: World, timeout: Int = 10000): Boolean {
        if (world.hop()) {
            return Condition.wait({ !Players.local().inMotion() }, 100, timeout / 100)
        }
        return false
    }

    /**
     * Hops to a random suitable world.
     * @param minPopulation Minimum world population
     * @param maxPopulation Maximum world population
     * @return true if hop was successful
     */
    fun hopToRandom(minPopulation: Int = 15, maxPopulation: Int = Int.MAX_VALUE): Boolean {
        val world = findRandomWorld(minPopulation, maxPopulation) ?: return false
        return hopTo(world)
    }

    /**
     * Gets the current world number.
     * @return Current world number
     */
    fun currentWorld(): Int {
        return Worlds.current()?.number ?: -1
    }

    /**
     * Gets the current world population.
     * @return Current world population or -1 if unknown
     */
    fun currentPopulation(): Int {
        return Worlds.current()?.population ?: -1
    }
}
