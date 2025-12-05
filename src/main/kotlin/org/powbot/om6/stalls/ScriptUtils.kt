package org.powbot.om6.stalls

import org.powbot.api.AppManager.logger
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.bank.Quantity
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
        logger.info("UTILS: Parsing comma-separated input: \"$input\"")
        val result = input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        logger.info("UTILS: Parsed list result: $result")
        return result
    }


    /**
     * Checks if there are other players on the local player's tile.
     *
     * @return True if any other player is standing on the same tile
     */
    fun isPlayerOnMyTile(): Boolean {
        val localPlayer = Players.local()
        val playersOnTile = Players.stream()
            .at(localPlayer.tile())
            .filter { it != localPlayer }
            .count()
        val isOccupied = playersOnTile > 0
        logger.info("UTILS: Checking for other players on tile ${localPlayer.tile()}. Count found: $playersOnTile. Result: $isOccupied")
        return isOccupied
    }

    /**
     * Finds a random suitable world for hopping.
     *
     * @return A random world matching the criteria, or null if none found
     */
    fun findRandomWorld(): World? {
        logger.info("UTILS: Searching for random suitable world (Pop: ${Constants.WorldHopping.MIN_POPULATION}-${Constants.WorldHopping.MAX_POPULATION}, Type: MEMBERS, Specialty: NONE)...")
        val suitableWorlds = Worlds.stream()
            .filtered {
                it.type() == World.Type.MEMBERS &&
                        it.population in Constants.WorldHopping.MIN_POPULATION..Constants.WorldHopping.MAX_POPULATION &&
                        it.specialty() == World.Specialty.NONE
            }
            .toList()

        val selectedWorld = suitableWorlds.randomOrNull()
        if (selectedWorld != null) {
            logger.info("UTILS: Found and selected world ${selectedWorld}.")
        } else {
            logger.warn("UTILS: Failed to find a suitable world for hopping.")
        }
        return selectedWorld
    }

    /**
     * Hops to a different world with a wait condition.
     *
     * @param world The world to hop to
     * @return True if the hop was successful
     */
    fun hopToWorld(world: World): Boolean {
        logger.info("UTILS: Attempting to hop to world ${world}...")
        return if (world.hop()) {
            logger.info("UTILS: World hop requested. Waiting for player to stop moving (Max ${Constants.WaitConditions.WORLD_HOP_ATTEMPTS} attempts)...")
            val result = Condition.wait(
                { !Players.local().inMotion() },
                Constants.WaitConditions.WORLD_HOP_TIMEOUT,
                Constants.WaitConditions.WORLD_HOP_ATTEMPTS
            )
            logger.info("UTILS: Hop completed and player is stationary: $result")
            result
        } else {
            logger.error("UTILS: Failed to initiate world hop request for world ${world}.")
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
        val names = itemNames.joinToString()
        val count = Inventory.stream().name(*itemNames).count()
        val result = count > 0
        logger.info("UTILS: Checking for any of items [$names]. Found $count items. Result: $result")
        return result
    }

    /**
     * Checks if the inventory contains only items from a given list.
     *
     * @param itemNames The names of items to check
     * @return True if all items in inventory are in the provided list
     */
    fun inventoryContainsOnly(itemNames: List<String>): Boolean {
        val totalInvCount = Inventory.stream().count()
        val itemsInListCount = Inventory.stream().name(*itemNames.toTypedArray()).count()
        val result = totalInvCount == itemsInListCount
        logger.info("UTILS: Checking if inventory only contains items from [${itemNames.joinToString()}]. Total Inv: $totalInvCount, Matching Inv: $itemsInListCount. Result: $result")
        return result
    }

    fun depositItems(vararg itemNames: String): Boolean {
        logger.info("UTILS: Attempting to deposit items: [${itemNames.joinToString()}]")
        var allDeposited = true

        for ((index, itemName) in itemNames.withIndex()) {
            if (hasAnyItem(itemName)) {
                logger.info("UTILS: Depositing all instances of '$itemName'.")
                if (Bank.deposit().item(itemName, Quantity.of(Bank.Amount.ALL.value)).submit()) {
                    val isLastItem = index == itemNames.size - 1

                    if (isLastItem) {
                        val deposited = Condition.wait(
                            { !hasAnyItem(itemName) },
                            Constants.WaitConditions.BANK_DEPOSIT_TIMEOUT,
                            Constants.WaitConditions.BANK_DEPOSIT_ATTEMPTS
                        )
                        if (!deposited) {
                            logger.error("UTILS: Failed to confirm deposit of '$itemName' within timeout.")
                            allDeposited = false
                        }
                    } else {
                        val delay = Random.nextInt(180, 400)
                        logger.debug("UTILS: Sleeping ${delay}ms before next deposit.")
                        Condition.sleep(delay)
                    }
                } else {
                    logger.error("UTILS: Bank.deposit() failed for '$itemName'.")
                    allDeposited = false
                }
            } else {
                logger.info("UTILS: Item '$itemName' not found in inventory, skipping deposit.")
            }
        }
        logger.info("UTILS: Overall deposit operation finished. Success: $allDeposited")
        return allDeposited
    }

    /**
     * Drops all items with specified names from the inventory.
     *
     * @param itemNames The names of items to drop
     * @return True if the drop actions were initiated successfully
     */
    fun dropItems(vararg itemNames: String): Boolean {
        logger.info("UTILS: Attempting to drop items: [${itemNames.joinToString()}]")
        val itemsToDrop = Inventory.stream().name(*itemNames).list()
        if (itemsToDrop.isEmpty()) {
            logger.info("UTILS: No items to drop found. Returning true.")
            return true
        }

        logger.info("UTILS: Found ${itemsToDrop.size} items to drop. Initiating drops...")
        itemsToDrop.forEach { item ->
            if (item.interact(Constants.Actions.DROP)) {
                val delay = Random.nextInt(Constants.Timing.DROP_ITEM_MIN_DELAY, Constants.Timing.DROP_ITEM_MAX_DELAY)
                logger.debug("UTILS: Interacted to drop ${item.name()}. Sleeping for ${delay}ms.")
                Condition.sleep(delay)
            } else {
                logger.warn("UTILS: Failed to interact (DROP) with item ${item.name()}.")
            }
        }

        val dropped = Condition.wait(
            { !hasAnyItem(*itemNames) },
            Constants.WaitConditions.DROP_ITEMS_TIMEOUT,
            Constants.WaitConditions.DROP_ITEMS_ATTEMPTS
        )
        logger.info("UTILS: Drop actions completed. Confirmed all items dropped: $dropped")
        return dropped
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
        logger.info("UTILS: Searching for object ID $objectId around $centerTile with range $range.")
        val obj = Objects.stream()
            .id(objectId)
            .within(centerTile, range)
            .nearest()
            .firstOrNull() ?: GameObject.Nil

        if (obj.valid()) {
            logger.info("UTILS: Found object: ${obj.name()} at ${obj.tile()}.")
        } else {
            logger.warn("UTILS: Object ID $objectId not found.")
        }
        return obj
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
        if (!obj.valid()) {
            logger.error("UTILS: Cannot interact: Object is not valid.")
            return false
        }

        val initialXp = Skills.experience(skill)
        logger.info("UTILS: Interacting with ${obj.name()} via '$action'. Initial ${skill.name} XP: $initialXp.")

        return if (obj.interact(action)) {
            logger.info("UTILS: Interaction successful. Waiting for XP gain...")
            val gainedXp = Condition.wait(
                { Skills.experience(skill) > initialXp },
                Constants.WaitConditions.THIEVING_XP_TIMEOUT,
                Constants.WaitConditions.THIEVING_XP_ATTEMPTS
            )
            if (gainedXp) {
                logger.info("UTILS: XP gained confirmed. New ${skill.name} XP: ${Skills.experience(skill)}.")
            } else {
                logger.warn("UTILS: Failed to confirm XP gain within timeout.")
            }
            gainedXp
        } else {
            logger.error("UTILS: Failed to execute interaction '$action' on ${obj.name()}.")
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
        val delay = Random.nextInt(min, max)
        logger.debug("UTILS: Applying random sleep delay of ${delay}ms.")
        Condition.sleep(delay)
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
        val inventoryDeposited = Inventory.stream().isNotEmpty() // This is likely inverted, should check if we have valuable items left. Assuming the intent is 'not full' or 'empty of valuables'. Let's check if full.
        val invFull = Inventory.isFull()
        val notInCombat = !Players.local().inCombat()

        // Revised logic for safety: Must be near bank, bank closed, not full/in combat.
        val safe = atBank && bankClosed && !invFull && notInCombat
        logger.info("UTILS: Checking safe break conditions: At Bank=$atBank, Bank Closed=$bankClosed, Inv Not Full=${!invFull}, Not in Combat=$notInCombat. Overall Safe: $safe")
        return safe
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
        val validEvents = stallEvents.isNotEmpty()
        val validThievingTile = thievingTile != Tile.Nil
        val validBankTile = bankTile != Tile.Nil
        val isValid = validEvents && validThievingTile && validBankTile

        logger.info("UTILS: Validating Configuration: Stall Events=$validEvents, Thieving Tile=$validThievingTile, Bank Tile=$validBankTile. Result: $isValid")
        return isValid
    }

    /**
     * Safely closes the bank if it's open.
     */
    fun closeBankIfOpen() {
        if (Bank.opened()) {
            logger.info("UTILS: Bank is open. Attempting to close it.")
            Bank.close()
            Condition.wait({ !Bank.opened() }, 500, 5)
        } else {
            logger.debug("UTILS: Bank is already closed.")
        }
    }

    /**
     * Checks if the player is idle (not in motion and not animating).
     *
     * @return True if the player is completely idle
     */
    fun isPlayerIdle(): Boolean {
        val player = Players.local()
        val isIdle = player.animation() == -1 && !player.inMotion()
        logger.debug("UTILS: Checking player idle status: Animation=${player.animation()}, InMotion=${player.inMotion()}. Result: $isIdle")
        return isIdle
    }
    /**
     * Checks if the player is at a specific tile.
     *
     * @param tile The tile to check
     * @return True if the player is at the specified tile
     */
    fun isAtTile(tile: Tile): Boolean {
        val localTile = Players.local().tile()
        val isAt = localTile == tile
        logger.info("UTILS: Checking if player is at target tile ($tile). Current tile: $localTile. Result: $isAt")
        return isAt
    }


    /**
     * Walks to a tile if not already there with interaction timeout and fallback.
     *
     * @param targetTile The tile to walk to
     * @param stallTile The stall tile for finding nearby fallback tiles
     * @param interactionTimeout Time in ms to wait before finding fallback tile (default 10000ms)
     * @return True if reached target or fallback tile
     */
    fun walkToTile(targetTile: Tile, stallTile: Tile? = null, interactionTimeout: Long = 10000): Boolean {
        if (isAtTile(targetTile)) {
            logger.info("UTILS: Already at target tile $targetTile.")
            return true
        }

        logger.info("UTILS: Walking to tile $targetTile with ${interactionTimeout}ms interaction timeout...")

        val startTime = System.currentTimeMillis()
        var lastInteractionTime = startTime
        var currentTarget = targetTile
        var fallbackAttempted = false
        var retryCount = 0
        val maxRetries = 3

        val walked = Condition.wait({
            val elapsedTime = System.currentTimeMillis() - startTime
            val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime

            if (elapsedTime >= 10000) {
                if (retryCount < maxRetries && stallTile != null) {
                    logger.info("UTILS: Timeout after 30 seconds. Retrying with new fallback tile (${retryCount + 1}/$maxRetries)...")
                    val newFallback = findNearbyValidTile(stallTile)
                    if (newFallback != null && newFallback != currentTarget) {
                        currentTarget = newFallback
                        retryCount++
                        lastInteractionTime = System.currentTimeMillis()
                        logger.info("UTILS: New fallback tile selected: $newFallback")
                        return@wait false
                    }
                }
                logger.info("UTILS: Max retries reached or no new tiles found. Stopping.")
                return@wait true
            }

            if (isAtTile(currentTarget)) {
                logger.info("UTILS: Reached target tile $currentTarget.")
                if (currentTarget != targetTile) {
                    Movement.step(targetTile)
                    Condition.sleep(200)
                }
                return@wait true
            }

            if (timeSinceInteraction >= interactionTimeout && !fallbackAttempted && stallTile != null) {
                logger.info("UTILS: No interaction for ${interactionTimeout}ms. Finding fallback tile near stall $stallTile...")
                val fallbackTile = findNearbyValidTile(stallTile)
                if (fallbackTile != null) {
                    currentTarget = fallbackTile
                    fallbackAttempted = true
                    logger.info("UTILS: Fallback tile selected: $fallbackTile")
                } else {
                    logger.warn("UTILS: No valid fallback tile found near stall.")
                }
            }

            val distance = currentTarget.distance()

            if (distance <= 5 && currentTarget.reachable()) {
                logger.info("UTILS: Target tile is $distance tiles away (<= 5) AND reachable. Using Movement.step.")
                Movement.step(currentTarget)
                lastInteractionTime = System.currentTimeMillis()
                Condition.sleep(200)
            } else {
                logger.info("UTILS: Target tile is $distance tiles away. Using Movement.walkTo.")
                Movement.walkTo(currentTarget)
                lastInteractionTime = System.currentTimeMillis()
            }

            false
        }, 200, 25)

        logger.info("UTILS: Movement completed. Success: $walked")
        return walked
    }

    /**
     * Finds a nearby valid tile within 1 tile of the stall.
     *
     * @param stallTile The stall tile to search around
     * @return A valid reachable tile or null
     */
    private fun findNearbyValidTile(stallTile: Tile): Tile? {
        val nearby = listOf(
            stallTile,
            stallTile.derive(1, 0),
            stallTile.derive(-1, 0),
            stallTile.derive(0, 1),
            stallTile.derive(0, -1),
            stallTile.derive(1, 1),
            stallTile.derive(1, -1),
            stallTile.derive(-1, 1),
            stallTile.derive(-1, -1)
        )

        return nearby.find { it.reachable() }.also {
            logger.debug("UTILS: Valid fallback tile found: $it")
        }
    }

    /**
     * Gets the distance to a tile from the local player.
     *
     * @param tile The tile to measure distance to
     * @return The distance in tiles
     */
    fun distanceToTile(tile: Tile): Double {
        val dist = tile.distance().toDouble()
        logger.debug("UTILS: Distance to tile $tile is $dist.")
        return dist
    }
}