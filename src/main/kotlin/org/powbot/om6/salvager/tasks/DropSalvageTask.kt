package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item // Import Item to allow explicit typing
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Task responsible for checking inventory and dropping salvage items,
 * then performing a set of randomized clicks to potentially withdraw cargo.
 */
class DropSalvageTask(
    script: ShipwreckSalvager, // Passed to Task superclass
    private val salvageItemName: String
) : Task(script) { // Correct inheritance

    // NEW: Property to determine the current drop method based on script state
    private val useTapToDrop: Boolean
        // Read user preference AND confirmed state dynamically from the main script
        get() = script.tapToDrop && script.isTapToDropEnabled

    override fun activate(): Boolean {
        // Activate if the script is explicitly in the drop phase, or if the inventory is unexpectedly full.
        return script.currentPhase == SalvagePhase.DROPPING_SALVAGE || Inventory.isFull()
    }

    override fun execute() {
        script.logger.info("TASK: DROPPING_SALVAGE. Initiating drop sequence.")
        script.currentPhase = SalvagePhase.DROPPING_SALVAGE // Ensure phase is set correctly

        dropSalvageItems()

        // Assuming CameraSnapper and requiredDropDirection are defined elsewhere and accessible
        CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)

        withdrawCargo()

        dropSalvageItems()

        // --- Phase Transition ---
        script.currentRespawnWait = Random.nextInt(
            ShipwreckSalvager.RESPAWN_WAIT_MIN_MILLIS,
            ShipwreckSalvager.RESPAWN_WAIT_MAX_MILLIS
        ).toLong()
        script.phaseStartTime = System.currentTimeMillis()
        script.currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
        script.logger.info("Transitioning to WAITING_FOR_RESPAWN for ${script.currentRespawnWait / 1000L} seconds.")
    }

    private fun dropSalvageItems() {
        if (!Inventory.opened()) {
            if (Inventory.open()) {
                script.logger.info("Inventory tab opened successfully for dropping.")
                Condition.sleep(Random.nextInt(200, 400))
            } else {
                script.logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
                return
            }
        } else {
            script.logger.info("Inventory tab is already open.")
        }
        // Use the injected salvageItemName property
        val salvageItems = Inventory.stream().name(salvageItemName).list()

        if (salvageItems.isEmpty()) {
            script.logger.info("No '$salvageItemName' items found to drop.")
            return
        }

        script.logger.info("Dropping ${salvageItems.size} x '$salvageItemName'. Drop Mode: ${if (useTapToDrop) "Tap/Shift-Drop" else "Right-Click"}.")

        if (useTapToDrop) {
            // Fast Shift-Drop Method
            salvageItems.forEach { item: Item ->
                if (item.valid()) {
                    if (item.click()) {
                        Condition.sleep(Random.nextInt(100, 200))
                    } else {
                        script.logger.warn("Failed to shift-drop item at slot.")
                    }
                }
            }
        } else {
            // Safe Right-Click Drop Method
            salvageItems.forEach { item: Item ->
                if (item.valid()) {
                    if (item.interact("Drop")) {
                        Condition.wait({ !item.valid() }, 50, 10)
                    } else {
                        script.logger.warn("Failed to right-click drop item at slot .")
                    }
                }
            }
        }

        // Wait until all items are dropped
        Condition.wait({ Inventory.stream().name(salvageItemName).isEmpty() }, 100, 25)
        script.logger.info("Finished dropping all '$salvageItemName' salvage items.")
    }


    /**
     * Executes randomized taps to simulate the cargo withdrawal sequence.
     * Hard-coded coordinates based on fixed screen size/UI, requires correct camera direction.
     */
    private fun withdrawCargo() {
        val waitTime = Random.nextInt(900, 1200)

        // Function to get a random offset between -6 and +6 (inclusive)
        fun getRandomOffsetLarge() = Random.nextInt(-6, 7)

        // --- Tap 1: Open Cargo (238, 326) ---
        val x1 = 238 + getRandomOffsetLarge()
        val y1 = 326 + getRandomOffsetLarge()
        if (Input.tap(x1, y1)) {
            script.logger.info("Click 1 ($x1, $y1) successful. Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Click 1 ($x1, $y1) failed.")
        }

        // --- Tap 2: Withdraw Cargo (143, 237) ---
        val x2 = 143 + getRandomOffsetLarge()
        val y2 = 237 + getRandomOffsetLarge()
        if (Input.tap(x2, y2)) {
            script.logger.info("Click 2 ($x2, $y2) successful. Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Click 2 ($x2, $y2) failed.")
        }

        // --- Tap 3: Close Cargo (569, 168) ---
        val x3 = 569 + getRandomOffsetLarge()
        val y3 = 168 + getRandomOffsetLarge()
        if (Input.tap(x3, y3)) {
            script.logger.info("Click 3 ($x3, $y3) successful. Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Click 3 ($x3, $y3) failed.")
        }

        Condition.sleep(Random.nextInt(500, 1000))
    }
}