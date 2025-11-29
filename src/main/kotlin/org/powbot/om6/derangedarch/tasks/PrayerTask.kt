package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.FIGHT_AREA_EXTENDED
        val boss = script.getBoss()
        val needsResupply = script.needsTripResupply()

        if (boss == null || !inFightArea || needsResupply) {
            return false
        }

        val prayerNotActive = !Prayer.prayerActive(Constants.REQUIRED_PRAYER)
        val prayerLow = Prayer.prayerPoints() < Constants.LOW_PRAYER_THRESHOLD

        return prayerNotActive || prayerLow
    }

    override fun execute() {
        script.logger.debug("Executing PrayerTask...")

        if (!Prayer.prayerActive(Constants.REQUIRED_PRAYER)) {
            script.logger.info("Activating prayer: ${Constants.REQUIRED_PRAYER.name}")
            Prayer.prayer(Constants.REQUIRED_PRAYER, true)
            Condition.wait({ Prayer.prayerActive(Constants.REQUIRED_PRAYER) }, 100, 5)
            return
        }

        if (Prayer.prayerPoints() < Constants.LOW_PRAYER_THRESHOLD) {
            script.logger.info("Prayer points low (${Prayer.prayerPoints()}), drinking potion.")
            val prayerPotion = Inventory.stream().nameContains(Constants.PRAYER_POTION_NAME_CONTAINS).firstOrNull()
            if (prayerPotion != null && prayerPotion.interact(Constants.DRINK_ACTION)) {
                Condition.sleep(1200)
            } else {
                script.logger.warn("Prayer low but no prayer potions found!")
            }
        }
    }
}