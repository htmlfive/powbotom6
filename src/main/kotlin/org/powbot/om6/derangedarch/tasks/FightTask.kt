package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean = script.BOSS_AREA.contains(Players.local()) && script.getBoss() != null && !script.needsSupplies()

    override fun execute() {
        val boss = script.getBoss() ?: return
        if (boss.distance() < 3) {
            val safeSpot = script.BOSS_AREA.tiles.filter { it.distanceTo(boss) >= 3 && it.reachable() }.randomOrNull()
            if (safeSpot != null) {
                script.logger.info("Too close, stepping back.")
                Movement.step(safeSpot); Condition.wait({ Players.local().distanceTo(boss) >= 3 }, 100, 10); return
            }
        }
        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) Prayer.prayer(script.REQUIRED_PRAYER, true)
        if (Players.local().interacting() != boss) {
            if (boss.interact("Attack")) Condition.wait({ Players.local().interacting() == boss }, 150, 10)
        }
    }
}