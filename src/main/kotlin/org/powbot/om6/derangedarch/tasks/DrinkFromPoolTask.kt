package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DrinkFromPoolTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inBankArea = script.FEROX_BANK_AREA.contains(Players.local())
        val needsRestock = script.needsFullRestock()
        val needsRestore = script.needsStatRestore()

        // Run if we are at the bank, don't need a full restock, but do need stats.
        val shouldRun = inBankArea && !needsRestock && needsRestore

        if (shouldRun) {
            script.logger.debug("Validate OK: In bank area, don't need restock, but need stat restore.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing DrinkFromPoolTask...")

        // Safeguard: If stats are somehow already full, just set the flag.
        if (!script.needsStatRestore()) {
            script.logger.info("Stats are already full, skipping pool.")
            script.hasAttemptedPoolDrink = true // Mark as done for this trip
            return
        }

        script.logger.info("Restoring stats at the Pool of Refreshment.")
        val pool = Objects.stream().id(script.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()

        if (pool != null) {
            if (pool.inViewport()) {
                script.logger.debug("Pool is in viewport, interacting 'Drink'...")
                if (pool.interact("Drink")) {
                    // Wait for the stats to actually restore.
                    val restoredSuccessfully = Condition.wait({ !script.needsStatRestore() }, 150, 20)

                    if (restoredSuccessfully) {
                        script.logger.info("Stats restored successfully.")
                        // Short delay after drinking, standard practice.
                        Condition.sleep(1200)
                    } else {
                        script.logger.warn("Interacted with pool, but stats did not restore.")
                    }
                } else {
                    script.logger.warn("Failed to interact 'Drink' with the pool.")
                }
            } else {
                script.logger.debug("Pool not in viewport, walking towards pool area.")
                Movement.walkTo(script.FEROX_POOL_AREA.randomTile)
            }
        } else {
            script.logger.warn("Could not find the Pool of Refreshment object.")
        }

        // After attempting to drink (or skipping/failing), set the flag.
        // This allows TravelToBossTask to run.
        script.hasAttemptedPoolDrink = true
        script.logger.debug("Setting hasAttemptedPoolDrink flag to true.")
    }
}