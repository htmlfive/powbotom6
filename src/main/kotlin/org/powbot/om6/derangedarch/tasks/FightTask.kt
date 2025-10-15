package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FightTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private val TRUNK_NAME = "Decaying trunk"
    private val CLIMB_ACTION = "Climb"

    override fun validate(): Boolean {
        // This task is now valid if we are at the fight start tile OR already in the boss area.
        return (Players.local().tile() == script.FIGHT_START_TILE || script.BOSS_AREA.contains(Players.local())) && !script.needsSupplies()
    }

    override fun execute() {
        // --- NEW LOGIC: Entry Step ---
        // If we are not yet in the boss area, our first job is to climb the trunk.
        if (!script.BOSS_AREA.contains(Players.local())) {
            script.logger.info("At final safe spot, climbing trunk...")
            val trunk = Objects.stream().name(TRUNK_NAME).action(CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null && trunk.interact(CLIMB_ACTION)) {
                Condition.wait({ script.BOSS_AREA.contains(Players.local()) }, 200, 15)
            }
            return // End the task for this cycle to re-evaluate our position.
        }

        // --- Standard Combat Logic ---
        val boss = script.getBoss()
        if (boss == null || !boss.valid()) {
            script.logger.info("Waiting for boss to spawn...")
            Condition.sleep(1000)
            return
        }

        // 1. Prayer Potion Management
        if (Prayer.prayerPoints() < 30) {
            val prayerPotion = Inventory.stream().nameContains("Prayer potion").firstOrNull()
            if (prayerPotion != null && prayerPotion.interact("Drink")) {
                Condition.sleep(1200)
                return
            }
        }

        // 2. Prayer Activation
        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) {
            Prayer.prayer(script.REQUIRED_PRAYER, true)
        }

        // 3. Positioning
        if (boss.distance() < 3) {
            val safeSpot = script.BOSS_AREA.tiles.filter { it.distanceTo(boss) >= 3 && it.reachable() }.randomOrNull()
            if (safeSpot != null) {
                Movement.step(safeSpot)
                Condition.wait({ boss.valid() && boss.distance() >= 3 }, 100, 10)
                return
            }
        }

        // 4. Attacking
        if (Players.local().interacting() != boss) {
            if (boss.interact("Attack")) {
                Condition.wait({ Players.local().interacting() == boss }, 150, 10)
            }
        }
    }
}