package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Antipoison Logic ---
    private val ANTIPOISON_NAMES = setOf(
        "Antipoison (1)",
        "Antipoison (2)",
        "Antipoison (3)",
        "Antipoison (4)"
    )

    /**
     * Checks if the player is poisoned.
     */
    private fun isPoisoned(): Boolean {
        return Combat.isPoisoned()
    }

    /**
     * Finds the first available antipoison potion in the inventory.
     */
    private fun getAntipoison(): Item {
        return Inventory.stream().name(*ANTIPOISON_NAMES.toTypedArray()).first()
    }
    // --- End Antipoison Logic ---

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 9
        val needsResupply = script.needsTripResupply()

        if (boss != null && inFightArea) {
            val bossTarget = boss.interacting()
            if (bossTarget is Player && bossTarget != Players.local()) {
                script.logger.warn("Another player is fighting the boss. Stopping script to avoid crashing.")
                ScriptManager.stop()
                return false
            }
        }

        val shouldFight = boss != null && inFightArea && !needsResupply
        if (shouldFight) {
            script.logger.debug("Validate OK: Boss is present, in fight area, and no resupply needed.")
        } else if (boss == null) {
            script.logger.debug("Validate FAIL: Boss is null.")
        } else if (!inFightArea) {
            script.logger.debug("Validate FAIL: Not in fight area.")
        } else if (needsResupply) {
            script.logger.debug("Validate FAIL: Needs trip resupply.")
        }
        return shouldFight
    }

    override fun execute() {
        // --- POISON CHECK ---
        if (isPoisoned()) {
            script.logger.info("Player is poisoned. Looking for antipoison...")
            val antipoison = getAntipoison()

            if (antipoison.valid()) {
                script.logger.info("Found ${antipoison.name()}. Drinking...")

                if (antipoison.interact("Drink")) {
                    val waitSuccess = Condition.wait({ !isPoisoned() }, 300, 10)
                    if (waitSuccess) {
                        script.logger.info("Successfully cured poison.")
                    } else {
                        script.logger.warn("Drank antipoison but still poisoned (or wait timed out).")
                    }
                } else {
                    script.logger.warn("Failed to interact 'Drink' with ${antipoison.name()}.")
                }
                return // Return after attempting to drink
            } else {
                script.logger.warn("Player is poisoned but no antipoison found!")
            }
        }
        // --- END POISON CHECK ---

        val boss = script.getBoss() ?: return
        val bossTarget = boss.interacting()
        if (bossTarget is Player && bossTarget != Players.local()) {
            script.logger.warn("Another player detected fighting the boss during execute. Stopping script.")
            ScriptManager.stop()
            return
        }

        script.logger.debug("Executing FightTask...")

        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) {
            script.logger.info("Activating prayer: ${script.REQUIRED_PRAYER.name}")
            Prayer.prayer(script.REQUIRED_PRAYER, true)
            Condition.wait({ Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 5)
            return
        }

        if (Prayer.prayerPoints() < 30) {
            script.logger.info("Prayer points low (${Prayer.prayerPoints()}), drinking potion.")
            val prayerPotion = Inventory.stream().nameContains("Prayer potion").firstOrNull()
            if (prayerPotion != null && prayerPotion.interact("Drink")) {
                Condition.sleep(1200)
                return
            } else {
                script.logger.warn("Prayer low but no prayer potions found!")
            }
        }

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

        if (Players.local().interacting() != boss) {
            script.logger.info("Not interacting with boss, attempting to attack.")
            if (!boss.inViewport()) {
                script.logger.debug("Boss not in viewport, turning camera.")
                Camera.turnTo(boss)
            }
            if (boss.interact("Attack")) {
                Condition.wait({ Players.local().interacting() == boss }, 150, 10)
            }
        }
    }
}