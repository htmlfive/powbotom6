package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.Helpers

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

        script.logger.info("Restoring stats at the Pool of Refreshment.")
        val pool = Objects.stream().id(Constants.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()

        if (pool != null) {
            if (pool.inViewport()) {
                script.logger.debug("Pool is in viewport, interacting 'Drink'...")
                if (pool.interact("Drink")) {
                    val restoredSuccessfully = Condition.wait({ !script.needsStatRestore() }, 150, 20)

                    if (restoredSuccessfully) {
                        script.logger.info("Stats restored successfully.")
                        Helpers.sleepRandom(2400)
                    } else {
                        script.logger.warn("Interacted with pool, but stats did not restore.")
                    }
                } else {
                    script.logger.warn("Failed to interact 'Drink' with the pool.")
                }
            } else {
                script.logger.debug("Pool not in viewport, walking towards pool area.")
                Movement.walkTo(Constants.FEROX_POOL_AREA.randomTile)
            }
        } else {
            script.logger.warn("Could not find the Pool of Refreshment object.")
        }

        script.hasAttemptedPoolDrink = true
        script.logger.debug("Setting hasAttemptedPoolDrink flag to true.")
    }
}
