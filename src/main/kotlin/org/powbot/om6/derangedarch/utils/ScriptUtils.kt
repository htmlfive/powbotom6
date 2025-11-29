package org.powbot.om6.derangedarch.utils

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.dax.teleports.Teleport
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

object ScriptUtils {

    // --- Item Type Checks ---

    fun isDuelingRing(id: Int): Boolean = id in Constants.DUELING_RING_ID_RANGE

    fun isDigsitePendant(id: Int): Boolean = id in Constants.DIGSITE_PENDANT_ID_RANGE

    fun isAxe(name: String): Boolean = name.endsWith(Constants.AXE_NAME_SUFFIX, ignoreCase = true)

    fun isRune(name: String): Boolean = name.endsWith(Constants.RUNE_NAME_SUFFIX, ignoreCase = true)

    // --- Item Finders ---

    fun getAntipoison(): Item {
        return Inventory.stream().name(*Constants.ANTIPOISON_NAMES.toTypedArray()).first()
    }

    fun getDuelingRing(): Item? {
        return Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).firstOrNull()
    }

    fun getDigsitePendant(): Item {
        var pendant: Item = Equipment.itemAt(Equipment.Slot.NECK)
        if (!pendant.name().contains(Constants.DIGSITE_PENDANT_NAME_CONTAINS)) {
            pendant = Inventory.stream().nameContains(Constants.DIGSITE_PENDANT_NAME_CONTAINS).firstOrNull() ?: Item.Nil
        }
        return pendant
    }

    // --- Status Checks ---

    fun isPoisoned(): Boolean = Combat.isPoisoned()

    // --- Banking Utilities ---

    fun withdrawWithRetries(id: Int, amount: Int, itemName: String = "Item ID $id", script: DerangedArchaeologistMagicKiller): Boolean {
        for (attempt in 1..3) {
            if (Bank.withdraw(id, amount)) {
                script.logger.debug("Bank.withdraw() successful for $itemName (Attempt $attempt/3)")
                return true
            }
            script.logger.warn("Attempt $attempt/3 failed to withdraw $amount of $itemName (ID: $id). Retrying...")
            Condition.sleep(300)
        }
        return false
    }

    // --- Widget Interactions ---

    fun useDuelingRingToFerox(script: DerangedArchaeologistMagicKiller): Boolean {
        script.logger.debug("Using Ring of Dueling to teleport to Ferox Enclave...")

        // Verify ring exists
        val ring = getDuelingRing()
        if (ring == null || ring == Item.Nil) {
            script.logger.warn("No Ring of Dueling found in inventory for teleport.")
            return false
        }

        // Try triggering teleport (retry up to 5 times if it fails)
        for (attempt in 1..5) {
            if (Teleport.RING_OF_DUELING_FEROX_ENCLAVE.trigger()) {
                script.logger.debug("Teleport triggered (attempt $attempt), waiting for teleport to complete...")
                randomSleep(5000)
                script.logger.debug("Checking if arrived at ${Constants.FEROX_ENTRANCE_TILE}...")
                return Condition.wait({ Players.local().tile().distanceTo(Constants.FEROX_ENTRANCE_TILE) < 6 }, 300, 20)
            }

            if (attempt < 5) {
                script.logger.warn("Teleport attempt $attempt failed, retrying...")
                Condition.sleep(600)
            }
        }

        script.logger.warn("Failed to trigger Ring of Dueling teleport after 5 attempts.")
        return false
    }

    fun walkToFeroxBank(script: DerangedArchaeologistMagicKiller) {
        script.logger.info("Arrived at Ferox Enclave, walking to bank chest...")
        script.logger.debug("Walking to ${Constants.FEROX_BANK_TILE}.")
        Movement.walkTo(Constants.FEROX_BANK_TILE)
    }

    fun drinkFromPool(script: DerangedArchaeologistMagicKiller, needsStatRestoreCheck: () -> Boolean): Boolean {
        script.logger.info("Restoring stats at the Pool of Refreshment.")
        val pool = Objects.stream().id(Constants.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()

        if (pool != null) {
            if (pool.inViewport()) {
                if (pool.interact(Constants.DRINK_ACTION)) {
                    val restoredSuccessfully = Condition.wait({ !needsStatRestoreCheck() }, 150, 20)
                    if (restoredSuccessfully) {
                        script.logger.info("Stats restored successfully.")
                        randomSleep(1200)
                        return true
                    } else {
                        script.logger.warn("Interacted with pool, but stats did not restore.")
                    }
                }
            } else {
                script.logger.debug("Pool not in viewport, walking to it.")
                Movement.walkTo(Constants.FEROX_POOL_AREA.randomTile)
            }
        } else {
            script.logger.warn("Could not find Pool of Refreshment.")
        }
        return false
    }

    // --- Equipment Helpers ---

    fun equipItem(item: Item, targetSlot: Equipment.Slot, script: DerangedArchaeologistMagicKiller): Boolean {
        script.logger.debug("Attempting to equip ${item.name()} (ID: ${item.id()}) into slot $targetSlot")
        val actions = item.actions()
        val actionUsed = listOf("Wield", "Wear", "Equip").find { it in actions }

        if (actionUsed == null) {
            script.logger.warn("Failed to equip ${item.name()}: no valid equip action found.")
            return false
        }

        script.logger.debug("Using action '$actionUsed' for ${item.name()}")
        val equipped = if (item.interact(actionUsed)) {
            Condition.wait({ Equipment.itemAt(targetSlot).id() == item.id() }, 250, 10)
        } else {
            false
        }

        if (equipped) {
            script.logger.info("Successfully equipped ${item.name()}.")
        } else {
            script.logger.warn("Failed to equip ${item.name()} using action '$actionUsed'.")
        }

        return equipped
    }

    // --- Combat Helpers ---

    fun attackBoss(boss: Npc, script: DerangedArchaeologistMagicKiller): Boolean {
        if (Players.local().interacting() != boss) {
            script.logger.info("Not interacting with boss, attempting to attack.")
            if (!boss.inViewport()) {
                script.logger.debug("Boss not in viewport, turning camera.")
                Camera.turnTo(boss)
            }
            if (boss.interact(Constants.ATTACK_ACTION)) {
                return Condition.wait({ Players.local().interacting() == boss }, 150, 10)
            }
        }
        return false
    }

    // --- Movement Helpers ---

    fun enableRunIfNeeded(script: DerangedArchaeologistMagicKiller) {
        if (!Movement.running() && Movement.energyLevel() > Constants.MIN_RUN_ENERGY) {
            script.logger.debug("Enabling run energy.")
            Movement.running(true)
        }
    }

    // --- Timing Helpers ---

    /**
     * Sleep for a randomized duration within ±200ms of the base value.
     */
    fun randomSleep(baseMs: Int) {
        val randomOffset = (-200..200).random()
        val sleepTime = (baseMs + randomOffset).coerceAtLeast(0)
        Condition.sleep(sleepTime)
    }

    // --- Geometry Helpers ---

    fun angleBetween(from: org.powbot.api.Tile, to: org.powbot.api.Tile): Double {
        return Math.toDegrees(kotlin.math.atan2((to.y() - from.y()).toDouble(), (to.x() - from.x()).toDouble()))
    }

    // --- Formatting ---

    fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fm", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fk", number / 1_000.0)
            else -> number.toString()
        }
    }

    // --- Emergency Stop ---

    fun stopScript(reason: String, script: DerangedArchaeologistMagicKiller) {
        script.logger.warn("FATAL: $reason Stopping script.")
        ScriptManager.stop()
    }
}