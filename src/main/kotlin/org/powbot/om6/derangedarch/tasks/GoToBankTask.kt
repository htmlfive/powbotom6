package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs

class GoToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- New constants for the two-step travel ---
    private val FEROX_ENTRANCE_TILE = Tile(3151, 3635, 0) // The arrival point after teleporting
    private val FEROX_BANK_TILE = Tile(3135, 3631, 0)      // The final destination inside the bank

    /**
     * This task is valid if a full restock is needed, but ONLY if we are not
     * already at the bank or in the boss fight area.
     */
    override fun validate(): Boolean {
        val needsFullRestock = script.needsFullRestock()
        val notAtBank = !script.FEROX_BANK_AREA.contains(Players.local())
        val notInFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8
        val shouldRun = needsFullRestock && notAtBank && notInFightArea

        if (shouldRun) {
            script.logger.debug("Validate OK: Needs full restock, not at bank, not in fight area.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing GoToBankTask...")

        // 1. Teleport to Ferox Enclave
        script.logger.info("Teleporting to Ferox Enclave bank.")

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
            script.logger.warn("No Ring of Dueling found in inventory for banking! Please add one to your setup.")
            ScriptManager.stop()
        }
    }
}