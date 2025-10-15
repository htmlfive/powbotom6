package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DeactivatePrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task is valid only when prayer is active BUT the boss is no longer present.
     */
    override fun validate(): Boolean {
        return Prayer.prayerActive(script.REQUIRED_PRAYER)
                && script.getBoss() == null
                && Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
    }

    override fun execute() {
        script.logger.info("Boss is gone, deactivating prayer.")
        if (Prayer.prayer(script.REQUIRED_PRAYER, false)) {
            Condition.wait({ !Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 10)
        }
    }
}