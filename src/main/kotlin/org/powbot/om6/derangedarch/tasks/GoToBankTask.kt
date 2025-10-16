package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class GoToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Constants for the Ring of Dueling widget interaction ---
    private val DUELING_RING_WIDGET_ID = 219
    private val OPTIONS_CONTAINER_COMPONENT = 1
    private val FEROX_ENCLAVE_OPTION_INDEX = 3

    // --- New constants for the two-step travel ---
    private val FEROX_ENTRANCE_TILE = Tile(3151, 3635, 0) // The arrival point after teleporting
    private val FEROX_BANK_TILE = Tile(3135, 3631, 0)      // The final destination inside the bank

    override fun validate(): Boolean {
        val needsBanking = !script.initialCheckCompleted && script.needsFullRestock()
        val notAtBank = !script.FEROX_BANK_AREA.contains(Players.local())
        // NEW: Check if we are outside the boss fight area.
        val notInFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8

        // This task now only runs on startup if a restock is needed AND we are not at the bank OR the boss area.
        return needsBanking && notAtBank && notInFightArea
    }

    override fun execute() {
        script.logger.info("Setup is incorrect, using Ring of Dueling to go to bank...")

        val duelRing = Inventory.stream().nameContains("Ring of dueling").firstOrNull()

        if (duelRing != null && duelRing.valid()) {
            if (duelRing.interact("Rub")) {
                if (Condition.wait({ Widgets.widget(DUELING_RING_WIDGET_ID).valid() }, 200, 15)) {
                    val enclaveOption = Widgets.widget(DUELING_RING_WIDGET_ID)
                        .component(OPTIONS_CONTAINER_COMPONENT)
                        .component(FEROX_ENCLAVE_OPTION_INDEX)

                    if (enclaveOption.valid() && enclaveOption.click()) {
                        // --- UPDATED LOGIC ---
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
            script.logger.warn("No Ring of Dueling found in inventory for banking! Please add one to your setup.")
            ScriptManager.stop()
        }
    }
}