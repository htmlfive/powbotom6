package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Antipoison Logic ---

    /**
     * Checks if the player is poisoned.
     */
    private fun isPoisoned(): Boolean {
        // Status.stream().nameContains("Poison").isNotEmpty()
        return Combat.poisoned() || Combat.venomed()
    }

    /**
     * Finds and uses an antipoison dose from the inventory.
     */
    private fun curePoison(): Boolean {
        if (!isPoisoned()) {
            return true
        }

        script.logger.info("Poisoned/Venomed! Attempting to drink antipoison.")

        val antipoison = Inventory.stream().name(*IDs.ANTIPOISON_NAMES.toTypedArray()).firstOrNull()

        if (antipoison != null) {
            if (antipoison.interact("Drink")) {
                script.logger.debug("Drank antipoison: ${antipoison.name()}. Waiting for poison to be cured.")
                // Wait for poison status to clear
                return Condition.wait({ !isPoisoned() }, 150, 10)
            } else {
                script.logger.warn("Failed to interact 'Drink' with antipoison: ${antipoison.name()}.")
            }
        } else {
            script.logger.warn("No antipoison found in inventory while poisoned/venomed!")
        }
        return false
    }

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8

        // Only run if a boss is present and we are in the fight area
        val shouldRun = boss != null && inFightArea

        if (shouldRun) {
            script.logger.debug("Validate OK: Boss is present and in fight area.")
        }

        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing FightTask...")
        val boss = script.getBoss()
        if (boss == null || boss.healthPercent() == 0) {
            script.logger.debug("Boss is dead or null, stopping fight task.")
            return
        }

        // 1. Check/Cure Poison
        if (isPoisoned()) {
            curePoison()
        }

        // 2. Activate Prayer
        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) {
            script.logger.info("Activating ${script.REQUIRED_PRAYER.name} prayer.")
            if (Prayer.prayer(script.REQUIRED_PRAYER, true)) {
                Condition.wait({ Prayer.prayerActive(script.REQUIRED_PRAYER) }, 150, 10)
            } else {
                script.logger.warn("Failed to activate prayer: ${script.REQUIRED_PRAYER.name}.")
            }
            // Let the task run again on the next loop to ensure prayer activates
            return
        }

        // 3. Keep distance (The boss's melee attack is deadly)
        if (boss.distance() < 2) {
            script.logger.debug("Too close to boss, finding a safe spot to step back.")
            val playerTile = Players.local().tile()
            val searchRadius = 5
            val southWestTile = Tile(playerTile.x() - searchRadius, playerTile.y() - searchRadius, playerTile.floor())
            val northEastTile = Tile(playerTile.x() + searchRadius, playerTile.y() + searchRadius, playerTile.floor())
            val searchArea = Area(southWestTile, northEastTile)

            val safeSpot = searchArea.tiles.filter { Movement.reachable(playerTile, it) && it.distanceTo(boss) >= 2 }.randomOrNull()

            if (safeSpot != null) {
                script.logger.debug("Stepping to safe spot: $safeSpot")
                Movement.step(safeSpot)
                Condition.wait({ boss.valid() && boss.distance() >= 2 }, 100, 10)
                return
            } else {
                script.logger.warn("Could not find a safe spot to step back to.")
            }
        }

        // 4. Attack
        if (Players.local().interacting() != boss) {
            script.logger.info("Not interacting with boss, attempting to attack.")
            if (!boss.inViewport()) {
                script.logger.debug("Boss not in viewport, turning camera.")
                Camera.turnTo(boss)
            }
            if (boss.interact("Attack")) {
                Condition.wait({ Players.local().interacting() == boss }, 600, 10)
            } else {
                script.logger.warn("Failed to interact 'Attack' with boss.")
            }
        }
    }
}