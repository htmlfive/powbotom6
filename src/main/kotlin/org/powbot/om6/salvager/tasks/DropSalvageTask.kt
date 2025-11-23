package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Task responsible for checking inventory and dropping salvage items.
 */
class DropSalvageTask(private val script: ShipwreckSalvager) : Task {

    override fun activate(): Boolean {
        // Activate if the script is explicitly in the drop phase, or if the inventory is unexpectedly full.
        return script.currentPhase == SalvagePhase.DROPPING_SALVAGE || Inventory.isFull()
    }

    override fun execute() {
        script.logger.info("TASK: DROPPING_SALVAGE. Initiating drop sequence.")
        script.currentPhase = SalvagePhase.DROPPING_SALVAGE // Ensure phase is set correctly

        if (!Inventory.opened()) {
            if (Inventory.open()) {
                script.logger.info("Inventory tab opened successfully for dropping.")
                Condition.sleep(Random.nextInt(200, 400))
            } else {
                script.logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
                return
            }
        }

        val salvageItems = Inventory.stream().name(ShipwreckSalvager.SALVAGE_NAME).list()

        if (salvageItems.isNotEmpty()) {
            script.logger.info("Dropping ${salvageItems.size} items named '${ShipwreckSalvager.SALVAGE_NAME}'...")

            salvageItems.forEach { item ->
                if (item.valid()) {
                    if (item.interact("Drop")) {
                        Condition.sleep(Random.nextInt(60, 100))
                    } else {
                        script.logger.warn("Failed to click 'Drop' on item ${item.name()}.")
                    }
                }
            }
            Condition.wait({ Inventory.stream().name(ShipwreckSalvager.SALVAGE_NAME).isEmpty() }, 150, 20)
        } else if (Inventory.isFull()) {
            script.logger.warn("Inventory is full but no item named '${ShipwreckSalvager.SALVAGE_NAME}' was found to drop.")
        }

        // --- Phase Transition ---
        script.currentRespawnWait = Random.nextInt(
            ShipwreckSalvager.RESPAWN_WAIT_MIN_MILLIS,
            ShipwreckSalvager.RESPAWN_WAIT_MAX_MILLIS
        ).toLong()

        script.logger.info("Drop sequence complete. Starting randomized respawn wait (${script.currentRespawnWait / 1000L}s).")
        script.phaseStartTime = System.currentTimeMillis()
        script.currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
    }
}