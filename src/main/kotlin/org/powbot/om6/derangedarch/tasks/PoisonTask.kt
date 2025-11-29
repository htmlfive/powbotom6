package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class PoisonTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.FIGHT_AREA_EXTENDED
        val boss = script.getBoss()
        val needsResupply = script.needsTripResupply()

        if (boss == null || !inFightArea || needsResupply) {
            return false
        }

        if (!ScriptUtils.isPoisoned()) {
            return false
        }

        val antipoison = ScriptUtils.getAntipoison()
        return antipoison.valid()
    }

    override fun execute() {
        script.logger.debug("Executing PoisonTask...")
        script.logger.info("Player is poisoned. Looking for antipoison...")

        val antipoison = ScriptUtils.getAntipoison()

        if (antipoison.valid()) {
            script.logger.info("Found ${antipoison.name()}. Drinking...")
            if (antipoison.interact(Constants.DRINK_ACTION)) {
                val waitSuccess = Condition.wait({ !ScriptUtils.isPoisoned() }, 300, 10)
                if (waitSuccess) {
                    script.logger.info("Successfully cured poison.")
                } else {
                    script.logger.warn("Drank antipoison but still poisoned (or wait timed out).")
                }
            } else {
                script.logger.warn("Failed to interact 'Drink' with ${antipoison.name()}.")
            }
        } else {
            script.logger.warn("Player is poisoned but no antipoison found!")
        }
    }
}