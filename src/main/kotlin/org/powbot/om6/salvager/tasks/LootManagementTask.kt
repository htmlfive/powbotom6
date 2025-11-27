package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Input
import org.powbot.api.rt4.*
import org.powbot.om6.salvager.LootConfig
import org.powbot.om6.salvager.*

class LootManagementTask(script: ShipwreckSalvager, val salvageItemName: String) : Task(script) {

    // Coordinates for the "Sort Salvage" button relative to the game window.
    // NOTE: These fixed coordinates are only used when 'Sort Salvage Only' mode is active,
    // which relies on a specific camera angle and window layout.
    private val SORT_BUTTON_X = 587 // Reverting to typical location, or using a reasonable default
    private val SORT_BUTTON_Y = 285
    private val SORT_BUTTON_TOLERANCE = 3
    private val SORT_BUTTON_CHECK_WAIT = 1000 // Time to wait for the button to appear

    // Helper to get the option directly
    private val sortSalvageOnly: Boolean get() = script.getOption<Boolean>("Sort Salvage Only") ?: false

    private val useTapToDrop: Boolean
        get() = script.tapToDrop && script.isTapToDropEnabled

    override fun activate(): Boolean {
        // Activate only in the DROPPING_SALVAGE phase
        return script.currentPhase == SalvagePhase.DROPPING_SALVAGE
    }

    /**
     * Executes a 4-tap sequence for quick Cargo Hold withdrawal using fixed screen coordinates.
     */
    private fun withdrawCargo() {
        // Randomize the main wait time slightly
        val mainWait = Random.nextInt(900, 1200)

        fun getRandomOffsetLarge() = Random.nextInt(-6, 7)
        script.logger.info("CARGO: Starting 4-tap cargo withdrawal sequence.")

        // Tap 1: Open Cargo (364, 144)
        val x1 = 364 + getRandomOffsetLarge()
        val y1 = 144 + getRandomOffsetLarge()
        if (Input.tap(x1, y1)) {
            script.logger.info("CARGO TAP 1 (Open): Tapped at ($x1, $y1). Waiting $mainWait ms.")
            Condition.sleep(mainWait)
            Condition.sleep(Random.nextInt(2400, 3000))
        } else {
            script.logger.warn("CARGO TAP 1 (Open): Tap at ($x1, $y1) failed.")
        }

        // Tap 2: Withdraw Cargo (143, 237)
        val x2 = 143 + getRandomOffsetLarge()
        val y2 = 237 + getRandomOffsetLarge()
        if (Input.tap(x2, y2)) {
            script.logger.info("CARGO TAP 2 (Withdraw): Tapped at ($x2, $y2). Waiting $mainWait ms.")
            Condition.sleep(mainWait)
        } else {
            script.logger.warn("CARGO TAP 2 (Withdraw): Tap at ($x2, $y2) failed.")
        }

        // Tap 3: Close (338, 60)
        val x3 = 338 + getRandomOffsetLarge()
        val y3 = 60 + getRandomOffsetLarge()
        if (Input.tap(x3, y3)) {
            script.logger.info("CARGO TAP 3 (Close): Tapped at ($x3, $y3). Waiting $mainWait ms.")
            Condition.sleep(mainWait)
        } else {
            script.logger.warn("CARGO TAP 3 (Close): Tap at ($x3, $y3) failed.")
        }

        // Tap 4: Walk back (567, 460)
        val x4 = 567 + getRandomOffsetLarge()
        val y4 = 460 + getRandomOffsetLarge()
        if (Input.tap(x4, y4)) {
            script.logger.info("CARGO TAP 4 (Walk back): Tapped at ($x4, $y4). Waiting $mainWait ms.")
            Condition.sleep(mainWait)
            Condition.sleep(Random.nextInt(2400, 3000))
        } else {
            script.logger.warn("CARGO TAP 4 (Walk back): Tap at ($x4, $y4) failed.")
        }
    }

    /**
     * Executes the single tap on the fixed-screen "Sort Salvage" button.
     */
    private fun tapSortSalvage(): Boolean {
        CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
        Condition.sleep(Random.nextInt(500, 800)) // Wait for camera snap

        val randomOffsetX = Random.nextInt(-SORT_BUTTON_TOLERANCE, SORT_BUTTON_TOLERANCE + 1)
        val randomOffsetY = Random.nextInt(-SORT_BUTTON_TOLERANCE, SORT_BUTTON_TOLERANCE + 1)
        val finalX = SORT_BUTTON_X + randomOffsetX
        val finalY = SORT_BUTTON_Y + randomOffsetY

        val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

        script.logger.info("ACTION: Tapping 'Sort Salvage' button at X=$finalX, Y=$finalY (AFK mode). Count before: $salvageCountBefore.")

        if (Input.tap(finalX, finalY)) {
            // Wait for the sorting animation/delay
            Condition.sleep(Random.nextInt(1500, 2500))

            // Wait until the salvage item is no longer in the inventory
            val waitSuccess = Condition.wait({ Inventory.stream().name("Fremennik salvage").isEmpty() }, 300, 10)

            if (waitSuccess) {
                script.logger.info("SUCCESS: 'Sort Salvage' complete. Inventory is clear of $salvageItemName.")
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))
                Condition.sleep(Random.nextInt(2000, 3000))

                return true
            } else {
                val salvageCountAfter = Inventory.stream().name(salvageItemName).count()
                script.logger.warn("FAIL: 'Sort Salvage' wait timed out. $salvageItemName still present (Count: $salvageCountAfter).")
                return false
            }
        }
        return false
    }

    /**
     * Executes the fast drop logic using the inventory.
     */
    private fun dropSalvageItems() {
        val salvageItem = Inventory.stream().name(salvageItemName).first()

        if (salvageItem.valid()) {
            val itemName = salvageItem.name()
            val itemCountBefore = Inventory.stream().name(itemName).count()
            script.logger.info("DROP: Found salvage item: $itemName. Dropping (Count before: $itemCountBefore)...")

            var success = false
            // Fast drop using Shift-Click/Tap-to-Drop
            if (script.tapToDrop) {
                script.logger.info("DROP: Using Tap-to-Drop for fast inventory removal.")
                if (salvageItem.click()) {
                    success = true
                }
            }
            // Standard right-click drop
            else {
                script.logger.info("DROP: Using Right-Click Drop for safe inventory removal.")
                if (salvageItem.interact("Drop")) {
                    success = true
                }
            }

            if (success) {
                // Wait up to 2.4 seconds (8 * 300ms) for the item count to decrease.
                val waitSuccess = Condition.wait({ Inventory.stream().name(itemName).count() < itemCountBefore }, 300, 8)

                if (waitSuccess) {
                    script.logger.info("DROP SUCCESS: Item $itemName removed. Count: $itemCountBefore -> ${Inventory.stream().name(itemName).count()}.")
                } else {
                    script.logger.warn("DROP FAIL: Item $itemName failed to disappear from inventory after click/drop within 2.4s. Retrying loop.")
                }
            } else {
                script.logger.warn("DROP FAIL: Failed to perform the click/interact action on $itemName.")
            }
        }
    }

    override fun execute() {
        script.logger.info("TASK: DROPPING_SALVAGE/LootManagement. Initiating sort/drop/alch sequence.")

        // 1. Check for Extractor interrupt before the main action
        if (CrystalExtractorTask(script).checkAndExecuteInterrupt()) { return }

        // 2. Withdraw Cargo (if enabled)
        if (script.withdrawCargoOnDrop) {
            script.logger.info("LOGIC: Withdrawing cargo before dropping.")
            withdrawCargo()
        }

        // 3. Drop/Sort the Main Salvage Item
        var dropSuccess = false

        if (sortSalvageOnly) {
            script.logger.info("LOGIC: Using AFK 'Sort Salvage Only' mode.")
            dropSuccess = tapSortSalvage()
        } else {
            script.logger.info("LOGIC: Using fast inventory drop mode.")

            // Ensure camera is snapped for the tap-to-drop action if the mouse needs to move
            CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)

            // Loop until salvage is gone
            val salvageBefore = Inventory.stream().name(salvageItemName).count()
            while (Inventory.stream().name(salvageItemName).isNotEmpty()) {
                dropSalvageItems()
                Condition.sleep(Random.nextInt(100, 300)) // Small delay between drops
            }
            val salvageAfter = Inventory.stream().name(salvageItemName).count()
            // The drop is successful if the count went to 0 and there was something to begin with.
            dropSuccess = salvageAfter == 0L && salvageBefore > 0L
            // FIX for line 196: Ensure comparisons here are Long/Long. Inventory.stream().count() returns Long.

            if (dropSuccess) {
                script.logger.info("DROP: Successfully dropped/removed all salvage items ($salvageBefore -> $salvageAfter).")
            } else {
                script.logger.warn("DROP: Salvage item count did not reach zero. Drop failed or item was not present to begin with.")
            }
        }


        // 4. Drop/Alch Logic (Only proceeds if the main drop/sort step was considered successful)
        if (dropSuccess || sortSalvageOnly) {

            // Drop non-alchable items
            script.logger.info("LootManagement: Starting item drop process for non-alchable items.")
            LootConfig.DROP_LIST.forEach { itemName ->
                Inventory.stream().name(itemName).forEach { item ->
                    script.logger.info("LootManagement: Attempting to drop $itemName.")
                    if (item.interact("Drop")) {
                        // Wait for item to disappear
                        Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)
                    }
                }
            }

            // High Alch the high-value items
            val highAlchSpell = Magic.Spell.HIGH_ALCHEMY
            script.logger.info("LootManagement: Starting alching loop (Ignoring canCast check).")
            // FIX for line 224: The previous code block that contained 'canCast' was removed.
            // The code below proceeds with alching attempts without the conditional check, as requested.
            LootConfig.ALCH_LIST.forEach { itemName ->
                val item = Inventory.stream().name(itemName).firstOrNull()
                if (item != null && item.valid()) {
                    script.logger.info("LootManagement: Casting High Alch on $itemName.")

                    // 1. Select the spell
                    if (highAlchSpell.cast()) {
                        // 2. Click the item to cast on it
                        if (item.click()) {
                            script.logger.info("LootManagement: Alch successful. Sleeping for animation/cooldown: 3000-3600ms.")
                            // Wait for the animation/cooldown time
                            Condition.sleep(Random.nextInt(3000, 3600))

                            // Short wait to confirm item removal
                            Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)
                        } else {
                            script.logger.warn("LootManagement: Failed to click item $itemName.")
                        }
                    } else {
                        script.logger.warn("LootManagement: Failed to select High Alch spell. Stopping alching loop.")
                        return@forEach // Breaks out of the forEach loop for the next item
                    }
                }
            }


            // --- PHASE TRANSITION ---
            if (sortSalvageOnly) {
                // If Sort Salvage Only is ON, we LOOP back to DROPPING_SALVAGE.
                script.logger.info("PHASE CHANGE: Sort Salvage Only is ON. Looping task immediately.")
                script.currentPhase = SalvagePhase.DROPPING_SALVAGE
            } else {
                // Standard behavior (Wait for Respawn)
                script.currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
                script.phaseStartTime = System.currentTimeMillis()
                script.salvageMessageFound = false



                script.logger.info("PHASE CHANGE: Looting complete. Transitioned to ${script.currentPhase.name} (Wait: ${script.currentRespawnWait}ms).")
            }

        } else {
            script.logger.warn("LootManagement: Drop/Sort logic failed. Retrying in next poll.")
        }
    }
}