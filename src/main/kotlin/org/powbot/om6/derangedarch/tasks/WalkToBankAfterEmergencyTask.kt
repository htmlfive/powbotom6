package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs

class WalkToBankAfterEmergencyTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // Constants for the two-step travel
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
        script.logger.info("Successfully escaped boss. Returning to bank.")

        // 1. Teleport to Ferox Enclave
        val ring = Inventory.stream().nameContains(IDs.RING_OF_DUELING_NAME).firstOrNull()

        if (ring != null) {
            script.logger.debug("Found Ring of Dueling, attempting to 'Rub'.")
            if (ring.interact("Rub")) {
                Condition.sleep(600) // Wait for item delay

                if (Condition.wait({ Widgets.widget(IDs.DUELING_RING_WIDGET_ID).valid() }, 200, 15)) {
                    script.logger.debug("Dueling Ring widget open.")
                    val enclaveOption = Widgets.widget(IDs.DUELING_RING_WIDGET_ID)
                        .component(IDs.WIDGET_OPTIONS_CONTAINER)
                        .component(IDs.FEROX_ENCLAVE_OPTION_INDEX)

                    if (enclaveOption.valid() && enclaveOption.click()) {
                        script.logger.debug("Clicked, waiting to arrive at $FEROX_ENTRANCE_TILE...")
                        // Step 1: Wait until we land near the Ferox Enclave entrance.
                        if (Condition.wait({ Players.local().tile().distanceTo(FEROX_ENTRANCE_TILE) < 6 }, 300, 15)) {
                            // Step 2: Once we've arrived, walk the rest of the way to the bank.
                            script.logger.info("Arrived at Ferox Enclave, walking to bank chest...")
                            script.logger.debug("Walking to $FEROX_BANK_TILE.")
                            Movement.walkTo(FEROX_BANK_TILE)

                            // --- ADDED LOGIC: Drink from pool if we need stat restore
                            if (Condition.wait({ script.FEROX_BANK_AREA.contains(Players.local()) }, 150, 10)) {
                                script.logger.debug("Arrived at bank area. Checking for stat restore need.")
                                if (script.needsStatRestore()) {
                                    script.logger.info("Stats need restoration after emergency teleport, heading to pool.")
                                    val pool = Objects.stream().id(IDs.POOL_OF_REFRESHMENT_ID).nearest().firstOrNull()

                                    if (pool != null) {
                                        if (pool.inViewport()) {
                                            script.logger.debug("Pool is in viewport, interacting 'Drink'...")
                                            if (pool.interact("Drink")) {
                                                // Wait for the stats to actually restore.
                                                Condition.wait({ !script.needsStatRestore() }, 150, 20)
                                                Condition.sleep(1200)
                                            }
                                        } else {
                                            script.logger.debug("Pool not in viewport, walking to it.")
                                            Movement.walkTo(script.FEROX_POOL_AREA.randomTile)
                                        }
                                    } else {
                                        script.logger.warn("Could not find Pool of Refreshment after arriving at bank.")
                                    }
                                }
                            }
                            // --- END ADDED LOGIC ---

                            // Clear the flag after safely arriving at the bank area
                            script.logger.info("Successfully returned to bank area. Clearing emergency flag.")
                            script.emergencyTeleportJustHappened = false


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