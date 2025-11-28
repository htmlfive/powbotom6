package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DeactivatePrayerTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val prayerActive = Prayer.prayerActive(script.REQUIRED_PRAYER)
        if (!prayerActive) return false

        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.DISTANCETOBOSS
        val bossIsGone = script.getBoss() == null

        return !inFightArea || bossIsGone
    }

    override fun execute() {
        script.logger.info("Deactivating prayer - no longer fighting")
        if (Prayer.prayer(script.REQUIRED_PRAYER, false)) {
            Condition.wait({ !Prayer.prayerActive(script.REQUIRED_PRAYER) }, 100, 10)
        }
    }
}