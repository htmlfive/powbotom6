package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DeactivatePrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 9
        val bossIsGone = script.getBoss() == null
        val prayerActive = Prayer.prayerActive(script.REQUIRED_PRAYER)

        // Trigger if prayer is active and we are either out of the fight area, or the boss is dead.
        val shouldDeactivate = prayerActive && (!inFightArea || bossIsGone)

        if (shouldDeactivate) {
            script.logger.debug("Validate OK: Prayer active ($prayerActive), not in fight area ($inFightArea) or boss is gone ($bossIsGone).")
        }

        return shouldDeactivate
    }

    override fun execute() {
        script.logger.info("No longer fighting, deactivating prayer.")
        script.logger.debug("Executing DeactivatePrayerTask...")
        if (Prayer.prayer(script.REQUIRED_PRAYER, false)) {
            Condition.wait({ !Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 10)
        }
    }
}