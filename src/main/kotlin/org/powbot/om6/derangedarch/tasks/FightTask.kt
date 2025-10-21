package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val boss = script.getBoss()
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 9
        val needsResupply = script.needsTripResupply()

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
        script.logger.debug("Executing FightTask...")

        // --- Prayer Activation ---
        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) {
            script.logger.info("Activating prayer: ${script.REQUIRED_PRAYER.name}")
            Prayer.prayer(script.REQUIRED_PRAYER, true)
            Condition.wait({ Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 5)
            return
        }

        // The validation already confirms the boss exists, so we can safely use it.
        val boss = script.getBoss() ?: return

        // --- Prayer Potion Management ---
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

        // --- Positioning ---
        if (boss.distance() < 2) {
            script.logger.debug("Too close to boss, finding a safe spot to step back.")
            val playerTile = Players.local().tile()
            val searchRadius = 5
            val southWestTile = Tile(playerTile.x() - searchRadius, playerTile.y() - searchRadius, playerTile.floor())
            val northEastTile = Tile(playerTile.x() + searchRadius, playerTile.y() + searchRadius, playerTile.floor())
            val searchArea = Area(southWestTile, northEastTile)

            // Find a random tile in the area that is at least 2 tiles away and reachable
            val safeSpot = searchArea.tiles.filter { it.distanceTo(boss) >= 2 && it.reachable() }.randomOrNull()

            if (safeSpot != null) {
                script.logger.debug("Stepping to safe spot: $safeSpot")
                Movement.step(safeSpot)
                Condition.wait({ boss.valid() && boss.distance() >= 2 }, 100, 10)
                return
            } else {
                script.logger.warn("Could not find a safe spot to step back to.")
            }
        }

        // --- Attacking ---
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