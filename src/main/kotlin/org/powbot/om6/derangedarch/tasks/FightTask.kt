package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    /**
     * This task is now strictly valid only if the boss is present and we are in the fight area.
     */
    override fun validate(): Boolean {
        return script.getBoss() != null
                && Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
                && !script.needsSupplies()
    }

    override fun execute() {
        // Prayer activation is now the first priority.
        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) {
            Prayer.prayer(script.REQUIRED_PRAYER, true)
            Condition.wait({ Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 5)
            return
        }

        // Get the boss, but if it disappears mid-task, the validation will handle it next cycle.
        val boss = script.getBoss() ?: return

        // Prayer Potion Management
        if (Prayer.prayerPoints() < 30) {
            val prayerPotion = Inventory.stream().nameContains("Prayer potion").firstOrNull()
            if (prayerPotion != null && prayerPotion.interact("Drink")) {
                Condition.sleep(1200)
                return
            }
        }

        // Positioning
        if (boss.distance() < 2) {
            val playerTile = Players.local().tile()
            val searchRadius = 5
            val southWestTile = Tile(playerTile.x() - searchRadius, playerTile.y() - searchRadius, playerTile.floor())
            val northEastTile = Tile(playerTile.x() + searchRadius, playerTile.y() + searchRadius, playerTile.floor())
            val searchArea = Area(southWestTile, northEastTile)
            val safeSpot = searchArea.tiles.filter { it.distanceTo(boss) >= 2 && it.reachable() }.randomOrNull()

            if (safeSpot != null) {
                Movement.step(safeSpot)
                Condition.wait({ boss.valid() && boss.distance() >= 2 }, 100, 10)
                return
            }
        }

        // Attacking
        if (Players.local().interacting() != boss) {
            if (boss.interact("Attack")) {
                Condition.wait({ Players.local().interacting() == boss }, 150, 10)
            }
        }
    }
}