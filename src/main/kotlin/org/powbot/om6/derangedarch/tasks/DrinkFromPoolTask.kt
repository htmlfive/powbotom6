package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class DrinkFromPoolTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inBankArea = Constants.FEROX_BANK_AREA.contains(Players.local())
        val needsRestock = script.needsFullRestock()
        val needsRestore = script.needsStatRestore()

        val shouldRun = inBankArea && !needsRestock && needsRestore

        if (shouldRun) {
            script.logger.debug("Validate OK: In bank area, don't need restock, but need stat restore.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing DrinkFromPoolTask...")

        if (!script.needsStatRestore()) {
            script.logger.info("Stats are already full, skipping pool.")
            script.hasAttemptedPoolDrink = true
            return
        }

        ScriptUtils.drinkFromPool(script) { script.needsStatRestore() }
        script.hasAttemptedPoolDrink = true
        script.logger.debug("Setting hasAttemptedPoolDrink flag to true.")
    }
}