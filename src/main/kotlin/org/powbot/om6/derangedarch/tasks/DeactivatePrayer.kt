package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DeactivatePrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.FIGHT_AREA_EXTENDED
        val bossIsGone = script.getBoss() == null
        val prayerActive = Prayer.prayerActive(Constants.REQUIRED_PRAYER)

        val shouldDeactivate = prayerActive && (!inFightArea || bossIsGone)

        if (shouldDeactivate) {
            script.logger.debug("Validate OK: Prayer active ($prayerActive), not in fight area ($inFightArea) or boss is gone ($bossIsGone).")
        }

        return shouldDeactivate
    }

    override fun execute() {
        script.logger.info("No longer fighting, deactivating prayer.")
        script.logger.debug("Executing DeactivatePrayerTask...")
        if (Prayer.prayer(Constants.REQUIRED_PRAYER, false)) {
            Condition.wait({ !Prayer.prayerActive(Constants.REQUIRED_PRAYER) }, 100, 10)
        }
    }
}