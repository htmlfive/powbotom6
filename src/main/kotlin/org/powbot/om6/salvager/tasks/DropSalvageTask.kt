package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Task responsible for checking inventory and dropping salvage items,
 * then performing a set of randomized clicks to potentially withdraw cargo.
 */
class DropSalvageTask(
    private val script: ShipwreckSalvager,
    private val tapToDrop: Boolean // Added tapToDrop property
) : Task {

    override fun activate(): Boolean {
        // Activate if the script is explicitly in the drop phase, or if the inventory is unexpectedly full.
        return script.currentPhase == SalvagePhase.DROPPING_SALVAGE || Inventory.isFull()
    }

    override fun execute() {
        script.logger.info("TASK: DROPPING_SALVAGE. Initiating drop sequence.")
        script.currentPhase = SalvagePhase.DROPPING_SALVAGE // Ensure phase is set correctly

        dropSalvageItems()

        // Assuming CameraSnapper and requiredDropDirection are defined elsewhere and accessible
        // CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)

        withdrawCargo()

        dropSalvageItems()

        // --- Phase Transition ---
        script.currentRespawnWait = Random.nextInt(
            ShipwreckSalvager.RESPAWN_WAIT_MIN_MILLIS,
            ShipwreckSalvager.RESPAWN_WAIT_MAX_MILLIS
        ).toLong()

        script.logger.info("Drop/Withdraw sequence complete. Starting randomized respawn wait (${script.currentRespawnWait / 1000L}s).")
        script.phaseStartTime = System.currentTimeMillis()
        script.currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
    }

    /**
     * Handles the logic for opening the inventory and dropping all items named 'Plundered salvage'.
     * @return Boolean indicating if the process successfully completed the drop attempt.
     */
    private fun dropSalvageItems(): Boolean {
        if (!Inventory.opened()) {
            if (Inventory.open()) {
                script.logger.info("Inventory tab opened successfully for dropping.")
                Condition.sleep(Random.nextInt(200, 400))
            } else {
                script.logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
                return false
            }
        }

        val salvageItems = Inventory.stream().name(ShipwreckSalvager.SALVAGE_NAME).list()

        if (salvageItems.isNotEmpty()) {
            script.logger.info("Dropping ${salvageItems.size} items named '${ShipwreckSalvager.SALVAGE_NAME}' (TapToDrop: $tapToDrop)...")

            if (tapToDrop) {
                // If tapToDrop is TRUE, use standard click (often used for Shift-Drop)
                salvageItems.forEach { item ->
                    if (item.valid()) {
                        if (item.click()) { // Standard click (assumes shift is held or tap logic is desired)
                            Condition.sleep(Random.nextInt(90, 180))
                        } else {
                            script.logger.warn("Failed to click (tap) on item ${item.name()}.")
                        }
                    }
                }
            } else {
                // If tapToDrop is FALSE, use right-click -> "Drop"
                salvageItems.forEach { item ->
                    if (item.valid()) {
                        // Explicitly click the "Drop" option
                        if (item.click("Drop")) {
                            Condition.sleep(Random.nextInt(90, 180))
                        } else {
                            script.logger.warn("Failed to click 'Drop' on item ${item.name()}.")
                        }
                    }
                }
            }

            // Wait until inventory is clear of the salvage item
            Condition.wait({ Inventory.stream().name(ShipwreckSalvager.SALVAGE_NAME).isEmpty() }, 150, 20)
        } else if (Inventory.isFull()) {
            script.logger.warn("Inventory is full but no item named '${ShipwreckSalvager.SALVAGE_NAME}' was found to drop.")
        }
        return true
    }

    /**
     * Performs a series of fixed-coordinate clicks with randomization,
     * likely to interact with a bank or deposit/withdraw interface.
     */
    private fun withdrawCargo() {
        script.logger.info("Executing WithdrawCargo sequence (Bank/Interface interaction) with randomization.")
        val waitTime = Random.nextInt(900, 1200)

        // Function to get a random offset between -3 and +3 (inclusive)
        fun getRandomOffsetSmall() = Random.nextInt(-3, 4)
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
        val x3 = 569 + getRandomOffsetSmall()
        val y3 = 168 + getRandomOffsetSmall()
        if (Input.tap(x3, y3)) {
            script.logger.info("Click 3 ($x3, $y3) successful.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Click 3 ($x3, $y3) failed.")
        }

        script.logger.info("WithdrawCargo sequence finished.")
    }
}