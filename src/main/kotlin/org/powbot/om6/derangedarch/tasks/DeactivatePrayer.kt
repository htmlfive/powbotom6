package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DeactivatePrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task is now valid if prayer is active and EITHER:
     * 1. The boss is gone.
     * 2. We are no longer in the boss fight area.
     */
    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
        val bossIsGone = script.getBoss() == null

        // Trigger if prayer is active and we are either out of the fight area, or the boss is dead.
        return Prayer.prayerActive(script.REQUIRED_PRAYER) && (!inFightArea || bossIsGone)
    }

    override fun execute() {
        script.logger.info("No longer fighting, deactivating prayer.")
        if (Prayer.prayer(script.REQUIRED_PRAYER, false)) {
            Condition.wait({ !Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 10)
        }
    }
}