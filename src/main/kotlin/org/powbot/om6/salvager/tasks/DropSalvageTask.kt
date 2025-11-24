package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Task responsible for checking inventory and dropping salvage items,
 * then conditionally performing a set of randomized clicks to potentially withdraw cargo.
 */
class DropSalvageTask(
    script: ShipwreckSalvager, // Passed to Task superclass
    private val salvageItemName: String
) : Task(script) {

    // Helper to determine the current drop method based on script state
    private val useTapToDrop: Boolean
        get() = script.tapToDrop && script.isTapToDropEnabled

    override fun activate(): Boolean {
        // Activate if the script is explicitly in the drop phase, or if the inventory is unexpectedly full.
        return script.currentPhase == SalvagePhase.DROPPING_SALVAGE || Inventory.isFull()
    }

    override fun execute() {
        script.logger.info("TASK: DROPPING_SALVAGE. Initiating drop sequence.")
        script.currentPhase = SalvagePhase.DROPPING_SALVAGE // Ensure phase is set correctly

        // 1. Drop the items
        dropSalvageItems()

        // 2. Check config and perform cargo withdrawal if enabled
        if (script.withdrawCargoOnDrop) {
            script.logger.info("CONFIG: Withdraw from Cargo Hold is TRUE. Attempting to withdraw cargo...")
            withdrawCargo()
            dropSalvageItems()
        } else {
            script.logger.info("CONFIG: Withdraw from Cargo Hold is FALSE. Skipping cargo withdrawal.")
        }

        // 3. Transition to the wait phase
        script.logger.info("Drop/Withdraw sequence complete. Transitioning to respawn wait.")
        // --- Phase Transition ---
        script.currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
        // Set up the wait time and starting time for the next phase
        script.currentRespawnWait = Random.nextInt(ShipwreckSalvager.RESPAWN_WAIT_MIN_MILLIS, ShipwreckSalvager.RESPAWN_WAIT_MAX_MILLIS).toLong() // 16-22 seconds
        script.phaseStartTime = System.currentTimeMillis()
    }

    /**
     * Drops all items matching the salvage item name from the inventory.
     */
    private fun dropSalvageItems() {
        // Find all salvage items
        val salvageItems = Inventory.stream().name(salvageItemName).list()

        if (salvageItems.isEmpty()) {
            script.logger.info("No '$salvageItemName' found to drop. Continuing with withdrawCargo (if enabled).")
            return
        }

        val dropMethod = if (useTapToDrop) "Tap-to-drop (enabled)" else "Right-click drop"
        script.logger.info("Dropping ${salvageItems.size} items using $dropMethod...")

        // If tap-to-drop is enabled, we click on all items
        if (useTapToDrop) {
            // Tap-to-drop: just click the item, and it should drop immediately.
            for (item in salvageItems) {
                if (item.click()) {
                    script.logger.info("Tapped item: ${item.name()}")
                    Condition.sleep(Random.nextInt(100, 200)) // Short pause between drops
                } else {
                    script.logger.warn("Failed to tap item: ${item.name()}. Aborting drop sequence.")
                    break
                }
            }
        } else {
            // Traditional right-click drop
            for (item in salvageItems) {
                if (item.interact("Drop")) {
                    script.logger.info("Right-click dropped item: ${item.name()}")
                    Condition.sleep(Random.nextInt(300, 500)) // Longer pause for context menu interaction
                } else {
                    script.logger.warn("Failed to right-click drop item: ${item.name()}. Aborting drop sequence.")
                    break
                }
            }
        }

        // Wait briefly for the inventory to clear
        Condition.wait({ Inventory.stream().name(salvageItemName).isEmpty() }, 500, 5)

        if (Inventory.stream().name(salvageItemName).isNotEmpty()) {
            script.logger.warn("Drop sequence complete, but some items remain. Will try again if necessary.")
        } else {
            script.logger.info("All salvage items successfully dropped.")
        }
    }

    /**
     * Attempts a randomized three-tap sequence to open the cargo hold and withdraw items.
     * These coordinates are relative to the client screen and require the camera to be facing the required direction.
     */
    private fun withdrawCargo() {
        val waitTime = Random.nextInt(900, 1200)

        // Function to get a random offset between -6 and +6 (inclusive)
        fun getRandomOffsetLarge() = Random.nextInt(-6, 7)

        // --- Tap 1: Open Cargo (238, 326) ---
        // Coordinates for the first tap (to open the cargo hold interface)
        val x1 = 238 + getRandomOffsetLarge()
        val y1 = 326 + getRandomOffsetLarge()
        if (Input.tap(x1, y1)) {
            script.logger.info("Cargo Tap 1 (Open) ($x1, $y1) successful. Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Cargo Tap 1 (Open) ($x1, $y1) failed.")
        }

        // --- Tap 2: Withdraw Cargo (143, 237) ---
        // Coordinates for the second tap (to click the "Withdraw" option)
        val x2 = 143 + getRandomOffsetLarge()
        val y2 = 237 + getRandomOffsetLarge()
        if (Input.tap(x2, y2)) {
            script.logger.info("Cargo Tap 2 (Withdraw) ($x2, $y2) successful. Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Cargo Tap 2 (Withdraw) ($x2, $y2) failed.")
        }

        // --- Tap 3: Close Cargo (569, 168) ---
        // Coordinates for the third tap (to close the cargo hold interface, often near the 'X' button)
        val x3 = 569 + getRandomOffsetLarge()
        val y3 = 168 + getRandomOffsetLarge()
        if (Input.tap(x3, y3)) {
            script.logger.info("Cargo Tap 3 (Close) ($x3, $y3) successful. Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("Cargo Tap 3 (Close) ($x3, $y3) failed.")
        }
    }
}