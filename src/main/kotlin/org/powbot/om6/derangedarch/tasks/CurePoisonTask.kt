package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class CurePoisonTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= 9
        val isPoisoned = Combat.isPoisoned()
        
        val shouldRun = inFightArea && isPoisoned
        
        if (shouldRun) {
            script.logger.debug("Validate OK: Player is poisoned in fight area")
        }
        
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing CurePoisonTask...")
        script.logger.info("Player is poisoned. Looking for antipoison...")
        
        val antipoison = Inventory.stream().name(*Constants.ANTIPOISON_NAMES.toTypedArray()).first()
        
        if (antipoison.valid()) {
            script.logger.info("Found ${antipoison.name()}. Drinking...")
            
            if (antipoison.interact("Drink")) {
                val waitSuccess = Condition.wait({ !Combat.isPoisoned() }, 300, 10)
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
