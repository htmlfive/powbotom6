package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.Helpers

class PrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.DISTANCETOBOSS
        if (!inFightArea) return false

        val needsResupply = script.needsTripResupply()
        if (needsResupply) return false

        val prayerNotActive = !Prayer.prayerActive(script.REQUIRED_PRAYER)
        val prayerLow = Prayer.prayerPoints() < 30

        return prayerNotActive || prayerLow
    }

    override fun execute() {
        // Activate prayer if not active AND boss is up
        if (!Prayer.prayerActive(script.REQUIRED_PRAYER)) {
            val boss = script.getBoss()
            if (boss != null) {
                script.logger.info("Activating prayer: ${script.REQUIRED_PRAYER.name}")
                Prayer.prayer(script.REQUIRED_PRAYER, true)
                Condition.wait({ Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 5)
            } else {
                script.logger.debug("Boss not present, skipping prayer activation")
            }
            return
        }

        // Drink prayer potion if low
        if (Prayer.prayerPoints() < 30) {
            script.logger.info("Prayer points low (${Prayer.prayerPoints()}), drinking potion")
            val prayerPotion = Inventory.stream().nameContains(Constants.PRAYER_POTION_NAME_CONTAINS).firstOrNull()
            if (prayerPotion != null && prayerPotion.interact("Drink")) {
                Helpers.sleepRandom(1200)
            } else {
                script.logger.warn("Prayer low but no prayer potions found")
            }
        }
    }
}