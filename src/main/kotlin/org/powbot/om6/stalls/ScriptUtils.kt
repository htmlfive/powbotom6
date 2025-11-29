package org.powbot.om6.stalls

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import kotlin.random.Random

/**
 * Utility functions for the stall thieving script.
 * Provides reusable helper methods to reduce code duplication and improve readability.
 */
object ScriptUtils {

    /**
     * Parses a comma-separated string into a list of trimmed, non-empty strings.
     *
     * @param input The comma-separated string to parse
     * @return A list of trimmed strings with empty entries filtered out
     */
    fun parseCommaSeparatedList(input: String): List<String> {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Checks if the player is at a specific tile.
     *
     * @param tile The tile to check
     * @return True if the player is at the specified tile
     */
    fun isAtTile(tile: Tile): Boolean {
        return Players.local().tile() == tile
    }

    /**
     * Checks if there are other players on the local player's tile.
     *
     * @return True if any other player is standing on the same tile
     */
    fun isPlayerOnMyTile(): Boolean {
        val localPlayer = Players.local()
        return Players.stream()
            .at(localPlayer.tile())
            .any { it != localPlayer }
    }

    /**
     * Finds a random suitable world for hopping.
     *
     * @return A random world matching the criteria, or null if none found
     */
    fun findRandomWorld(): World? {
        return Worlds.stream()
            .filtered {
                it.type() == World.Type.MEMBERS &&
                        it.population in Constants.WorldHopping.MIN_POPULATION..Constants.WorldHopping.MAX_POPULATION &&
                        it.specialty() == World.Specialty.NONE
            }
            .toList()
            .randomOrNull()
    }

    /**
     * Hops to a different world with a wait condition.
     *
     * @param world The world to hop to
     * @return True if the hop was successful
     */
    fun hopToWorld(world: World): Boolean {
        return if (world.hop()) {
            Condition.wait(
                { !Players.local().inMotion() },
                Constants.WaitConditions.WORLD_HOP_TIMEOUT,
                Constants.WaitConditions.WORLD_HOP_ATTEMPTS
            )
        } else {
            false
        }
    }

    /**
     * Checks if the inventory contains any items from a given list.
     *
     * @param itemNames The names of items to check for
     * @return True if at least one item from the list is in the inventory
     */
    fun hasAnyItem(vararg itemNames: String): Boolean {
        return Inventory.stream().name(*itemNames).isNotEmpty()
    }

    /**
     * Checks if the inventory contains only items from a given list.
     *
     * @param itemNames The names of items to check
     * @return True if all items in inventory are in the provided list
     */
    fun inventoryContainsOnly(itemNames: List<String>): Boolean {
        return Inventory.stream().all { it.name() in itemNames }
    }

    /**
     * Deposits all items with specified names from the inventory into the bank.
     *
     * @param itemNames The names of items to deposit
     * @return True if all specified items were successfully deposited
     */
    fun depositItems(vararg itemNames: String): Boolean {
        var allDeposited = true
        for (itemName in itemNames) {
            if (hasAnyItem(itemName)) {
                if (Bank.deposit(itemName, Bank.Amount.ALL)) {
                    Condition.wait(
                        { !hasAnyItem(itemName) },
                        Constants.WaitConditions.BANK_DEPOSIT_TIMEOUT,
                        Constants.WaitConditions.BANK_DEPOSIT_ATTEMPTS
                    )
                } else {
                    allDeposited = false
                }
            }
        }
        return allDeposited
    }

    /**
     * Drops all items with specified names from the inventory.
     *
     * @param itemNames The names of items to drop
     * @return True if the drop actions were initiated successfully
     */
    fun dropItems(vararg itemNames: String): Boolean {
        val itemsToDrop = Inventory.stream().name(*itemNames).list()
        if (itemsToDrop.isEmpty()) return true

        itemsToDrop.forEach { item ->
            if (item.interact(Constants.Actions.DROP)) {
                Condition.sleep(Random.nextInt(
                    Constants.Timing.DROP_ITEM_MIN_DELAY,
                    Constants.Timing.DROP_ITEM_MAX_DELAY
                ))
            }
        }

        return Condition.wait(
            { !hasAnyItem(*itemNames) },
            Constants.WaitConditions.DROP_ITEMS_TIMEOUT,
            Constants.WaitConditions.DROP_ITEMS_ATTEMPTS
        )
    }

    /**
     * Finds a game object by ID within a certain range of a tile.
     *
     * @param objectId The ID of the object to find
     * @param centerTile The tile to search around
     * @param range The maximum distance from the center tile
     * @return The found GameObject, or GameObject.Nil if not found
     */
    fun findGameObject(objectId: Int, centerTile: Tile, range: Double): GameObject {
        return Objects.stream()
            .id(objectId)
            .within(centerTile, range)
            .nearest()
            .firstOrNull() ?: GameObject.Nil
    }

    /**
     * Checks if a game object is in the viewport, and turns the camera to it if not.
     *
     * @param obj The GameObject to check and turn to
     * @return True if the object is now in the viewport
     */
    fun ensureObjectInView(obj: GameObject): Boolean {
        if (!obj.valid()) return false

        if (!obj.inViewport()) {
            Camera.turnTo(obj)
            return obj.inViewport()
        }
        return true
    }

    /**
     * Attempts to interact with a game object and waits for XP gain.
     *
     * @param obj The GameObject to interact with
     * @param action The action to perform
     * @param skill The skill to monitor for XP gain
     * @return True if the interaction was successful and XP was gained
     */
    fun interactAndWaitForXp(obj: GameObject, action: String, skill: Skill): Boolean {
        val initialXp = Skills.experience(skill)
        return if (obj.interact(action)) {
            Condition.wait(
                { Skills.experience(skill) > initialXp },
                Constants.WaitConditions.THIEVING_XP_TIMEOUT,
                Constants.WaitConditions.THIEVING_XP_ATTEMPTS
            )
        } else {
            false
        }
    }

    /**
     * Generates a random delay within a specified range.
     *
     * @param min Minimum delay in milliseconds
     * @param max Maximum delay in milliseconds
     */
    fun randomDelay(min: Int, max: Int) {
        Condition.sleep(Random.nextInt(min, max))
    }

    /**
     * Checks if the player can break (safe conditions for stopping the script).
     *
     * @param bankTile The tile where banking occurs
     * @return True if it's safe to break
     */
    fun canSafelyBreak(bankTile: Tile): Boolean {
        val atBank = isAtTile(bankTile)
        val bankClosed = !Bank.opened()
        val inventoryDeposited = !Inventory.isFull()
        val notInCombat = !Players.local().inCombat()

        return atBank && bankClosed && inventoryDeposited && notInCombat
    }

    /**
     * Validates that required configuration is properly set.
     *
     * @param thievingTile The thieving tile to validate
     * @param bankTile The bank tile to validate
     * @param stallEvents The stall target events to validate
     * @return True if configuration is valid
     */
    fun isConfigurationValid(thievingTile: Tile, bankTile: Tile, stallEvents: List<*>): Boolean {
        return stallEvents.isNotEmpty() &&
                thievingTile != Tile.Nil &&
                bankTile != Tile.Nil
    }

    /**
     * Safely closes the bank if it's open.
     */
    fun closeBankIfOpen() {
        if (Bank.opened()) {
            Bank.close()
        }
    }

    /**
     * Checks if the player is idle (not in motion and not animating).
     *
     * @return True if the player is completely idle
     */
    fun isPlayerIdle(): Boolean {
        val player = Players.local()
        return player.animation() == -1 && !player.inMotion()
    }

    /**
     * Walks to a tile if not already there.
     *
     * @param targetTile The tile to walk to
     * @return True if already at the tile or movement was initiated
     */
    fun walkToTile(targetTile: Tile): Boolean {
        return if (isAtTile(targetTile)) {
            true
        } else {
            Movement.walkTo(targetTile)
        }
    }

    /**
     * Gets the distance to a tile from the local player.
     *
     * @param tile The tile to measure distance to
     * @return The distance in tiles
     */
    fun distanceToTile(tile: Tile): Double {
        return tile.distance().toDouble()
    }
}