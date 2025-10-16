package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class WalkToBankAfterEmergencyTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Constants for the Ring of Dueling widget interaction ---
    private val DUELING_RING_WIDGET_ID = 219
    private val OPTIONS_CONTAINER_COMPONENT = 1
    private val FEROX_ENCLAVE_OPTION_INDEX = 3

    // --- New constants for the two-step travel ---
    private val FEROX_ENTRANCE_TILE = Tile(3151, 3635, 0) // The arrival point after teleporting
    private val FEROX_BANK_TILE = Tile(3135, 3631, 0)      // The final destination inside the bank

    /**
     * This task is valid only if we have just emergency teleported and are not yet at the bank.
     */
    override fun validate(): Boolean {
        return script.emergencyTeleportJustHappened && !script.FEROX_BANK_AREA.contains(Players.local())
    }

    /**
     * The action is now to use the Ring of Dueling to teleport, mirroring the GoToBankTask logic.
     */
    override fun execute() {
        script.logger.info("Emergency recovery: Using Ring of Dueling to return to bank...")

        val duelRing = Inventory.stream().nameContains("Ring of dueling").firstOrNull()

        if (duelRing != null && duelRing.valid()) {
            if (duelRing.interact("Rub")) {
                if (Condition.wait({ Widgets.widget(DUELING_RING_WIDGET_ID).valid() }, 200, 15)) {
                    val enclaveOption = Widgets.widget(DUELING_RING_WIDGET_ID)
                        .component(OPTIONS_CONTAINER_COMPONENT)
                        .component(FEROX_ENCLAVE_OPTION_INDEX)

                    if (enclaveOption.valid() && enclaveOption.click()) {
                        // Step 1: Wait until we land near the Ferox Enclave entrance.
                        if (Condition.wait({ Players.local().tile().distanceTo(FEROX_ENTRANCE_TILE) < 6 }, 300, 15)) {
                            // Step 2: Once we've arrived, walk the rest of the way to the bank.
                            script.logger.info("Arrived at Ferox Enclave, walking to bank chest...")
                            Movement.walkTo(FEROX_BANK_TILE)
                        }
                    }
                }
            }
        } else {
            script.logger.warn("No Ring of Dueling found in inventory for emergency recovery! Stopping script.")
            ScriptManager.stop()
        }
    }
}