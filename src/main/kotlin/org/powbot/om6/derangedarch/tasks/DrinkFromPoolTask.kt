package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DrinkFromPoolTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task is valid if we are at the bank, have finished banking, and have NOT
     * yet attempted to drink from the pool on this trip.
     */
    override fun validate(): Boolean {
        return script.initialCheckCompleted
                && script.FEROX_BANK_AREA.contains(Players.local())
                && !script.hasAttemptedPoolDrink
    }

    override fun execute() {
        // First, check if we even need to restore stats. If not, we can skip this and proceed.
        if (!script.needsStatRestore()) {
            script.logger.info("Stats are already full, skipping pool.")
            script.hasAttemptedPoolDrink = true
            return
        }

        script.logger.info("Restoring stats at the Pool of Refreshment.")
        val pool = Objects.stream().id(script.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()

        if (pool != null) {
            if (pool.inViewport()) {
                if (pool.interact("Drink")) {
                    Condition.wait({ !script.needsStatRestore() }, 150, 20)
                }
            } else {
                Movement.walkTo(script.FEROX_POOL_AREA.randomTile)
            }
        }

        // After attempting to drink (or skipping), set the flag to true.
        // This signals that this step is complete and the TravelToBossTask can now run.
        script.hasAttemptedPoolDrink = true
    }
}