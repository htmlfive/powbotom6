package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class WalkToBankAfterEmergencyTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Constants for the Ring of Dueling widget interaction ---
    private val DUELING_RING_WIDGET_ID = 219
    private val OPTIONS_CONTAINER_COMPONENT = 1
    private val FEROX_ENCLAVE_OPTION_INDEX = 3

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
                        Condition.wait({ script.FEROX_BANK_AREA.contains(Players.local()) }, 300, 20)
                    }
                }
            }
        } else {
            script.logger.warn("No Ring of Dueling found in inventory for emergency recovery! Stopping script.")
            ScriptManager.stop()
        }
    }
}