package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // Constants for the travel logic
    private val TRUNK_SAFE_TILE = Tile(3682, 3717, 0)
    private val TRUNK_NAME = "Decaying trunk"
    private val CLIMB_ACTION = "Climb"

    override fun validate(): Boolean = !script.BOSS_AREA.contains(Players.local()) && !script.needsSupplies() && !script.needsStatRestore()

    override fun execute() {
        if (!Movement.running() && Movement.energyLevel() > 40) Movement.running(true)

        // Step 1: Check if we are on Fossil Island. If not, use the pendant to get there.
        if (Npcs.stream().name("Row boat").isEmpty()) {
            script.logger.info("Not on Fossil Island, using Digsite Pendant...")
            val pendant = Equipment.itemAt(Equipment.Slot.NECK)
            if (pendant.valid() && pendant.name().contains("Digsite pendant")) {
                // Use the configured left-click option (should be Mushtree)
                if (pendant.interact(pendant.actions().first())) {
                    Condition.wait({ Npcs.stream().name("Row boat").isNotEmpty() }, 300, 20)
                }
            } else {
                script.logger.warn("Digsite pendant not equipped! Add it to your equipment setup.")
            }
            return // End execution for this cycle to re-evaluate state
        }

        // Step 2: We are on Fossil Island. Check if we are near the trunk's safe spot.
        if (Players.local().tile().distanceTo(TRUNK_SAFE_TILE) > 5) {
            // If we are far from the trunk, walk to the safe tile.
            script.logger.info("Walking to the decaying trunk safe spot...")
            Movement.walkTo(TRUNK_SAFE_TILE)
        } else {
            // If we are near the trunk, find it and climb it.
            script.logger.info("At the safe spot, attempting to climb the trunk...")
            val trunk = Objects.stream().name(TRUNK_NAME).action(CLIMB_ACTION).nearest().firstOrNull()

            if (trunk != null) {
                if (!trunk.inViewport()) {
                    Camera.turnTo(trunk)
                }
                if (trunk.interact(CLIMB_ACTION)) {
                    // The success condition is entering the boss area.
                    Condition.wait({ script.BOSS_AREA.contains(Players.local()) }, 200, 15)
                }
            } else {
                script.logger.warn("Can't find the '$TRUNK_NAME' to climb! Make sure you are at the right spot.")
                // Move a little closer just in case we're on the edge of the distance check
                Movement.walkTo(TRUNK_SAFE_TILE)
            }
        }
    }
}