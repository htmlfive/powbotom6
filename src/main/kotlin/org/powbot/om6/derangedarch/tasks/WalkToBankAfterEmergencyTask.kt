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

    // --- Constants for the two-step travel ---
    private val FEROX_ENTRANCE_TILE = Tile(3151, 3635, 0) // The arrival point after teleporting
    private val FEROX_BANK_TILE = Tile(3135, 3631, 0)      // The final destination inside the bank

    override fun validate(): Boolean {
        val inBank = script.FEROX_BANK_AREA.contains(Players.local())
        val shouldRun = script.emergencyTeleportJustHappened && !inBank

        if (shouldRun) {
            script.logger.debug("Validate OK: Emergency teleport flag is true and not yet at bank.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing WalkToBankAfterEmergencyTask...")
        script.logger.info("Emergency recovery: Using Ring of Dueling to return to bank...")

        val duelRing = Inventory.stream().nameContains("Ring of dueling").firstOrNull()

        if (duelRing != null && duelRing.valid()) {
            script.logger.debug("Found valid Ring of Dueling, interacting 'Rub'...")
            if (duelRing.interact("Rub")) {
                script.logger.debug("Waiting for Dueling Ring widget ($DUELING_RING_WIDGET_ID)...")
                if (Condition.wait({ Widgets.widget(DUELING_RING_WIDGET_ID).valid() }, 200, 15)) {
                    script.logger.debug("Widget found. Clicking Ferox Enclave option.")
                    val enclaveOption = Widgets.widget(DUELING_RING_WIDGET_ID)
                        .component(OPTIONS_CONTAINER_COMPONENT)
                        .component(FEROX_ENCLAVE_OPTION_INDEX)

                    if (enclaveOption.valid() && enclaveOption.click()) {
                        script.logger.debug("Clicked, waiting to arrive at $FEROX_ENTRANCE_TILE...")
                        // Step 1: Wait until we land near the Ferox Enclave entrance.
                        if (Condition.wait({ Players.local().tile().distanceTo(FEROX_ENTRANCE_TILE) < 6 }, 300, 15)) {
                            // Step 2: Once we've arrived, walk the rest of the way to the bank.
                            script.logger.info("Arrived at Ferox Enclave, walking to bank chest...")
                            script.logger.debug("Walking to $FEROX_BANK_TILE.")
                            Movement.walkTo(FEROX_BANK_TILE)
                        } else {
                            script.logger.warn("Teleport click succeeded but did not arrive at Ferox Enclave.")
                        }
                    } else {
                        script.logger.warn("Could not find or click Ferox Enclave option on widget.")
                    }
                } else {
                    script.logger.warn("Dueling Ring widget did not appear after 'Rub'.")
                }
            } else {
                script.logger.warn("Failed to interact 'Rub' with Ring of Dueling.")
            }
        } else {
            script.logger.warn("FATAL: No Ring of Dueling found in inventory for emergency recovery! Stopping script.")
            ScriptManager.stop()
        }
    }
}