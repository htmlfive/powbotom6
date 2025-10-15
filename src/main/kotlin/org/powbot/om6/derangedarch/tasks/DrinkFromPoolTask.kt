package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DrinkFromPoolTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task now validates if we are done banking but have NOT yet attempted
     * to drink from the pool on this trip.
     */
    override fun validate(): Boolean {
        return script.FEROX_BANK_AREA.contains(Players.local())
                && !script.needsSupplies()
                && !script.hasAttemptedPoolDrink
    }

    /**
     * Interacts with the pool once, waits briefly, and then sets the flag
     * to indicate the action is complete for this trip.
     */
    override fun execute() {
        script.logger.info("Restoring stats at the Pool of Refreshment (one attempt)...")
        val pool = Objects.stream().id(script.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()

        if (pool != null) {
            if (pool.inViewport()) {
                if (pool.interact("Drink")) {
                    // Wait a short, fixed amount of time for the interaction to occur.
                    Condition.sleep(1500)
                }
            } else {
                Movement.walkTo(script.FEROX_POOL_AREA.randomTile)
            }
        }
        // IMPORTANT: Set the flag to true regardless of success, ensuring we only try once.
        script.hasAttemptedPoolDrink = true
    }
}