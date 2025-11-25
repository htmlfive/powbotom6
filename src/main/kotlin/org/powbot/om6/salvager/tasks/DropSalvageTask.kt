package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvager.ShipwreckSalvager

class DropSalvageTask(
    script: ShipwreckSalvager,
    private val salvageItemName: String
) : Task(script) {

    private val useTapToDrop: Boolean
        get() = script.tapToDrop && script.isTapToDropEnabled

    override fun activate(): Boolean {
        script.logger.debug("ACTIVATE: Checking if phase is ${SalvagePhase.DROPPING_SALVAGE.name} (${script.currentPhase == SalvagePhase.DROPPING_SALVAGE}) or Inventory is full (${Inventory.isFull()}).")
        return script.currentPhase == SalvagePhase.DROPPING_SALVAGE || Inventory.isFull()
    }

    override fun execute() {
        script.logger.info("TASK: DROPPING_SALVAGE. Initiating drop sequence.")
        script.currentPhase = SalvagePhase.DROPPING_SALVAGE

        dropSalvageItems()

        if (CrystalExtractorTask(script).checkAndExecuteInterrupt()) { return }

        if (script.withdrawCargoOnDrop) {
            script.logger.info("CONFIG: Withdraw from Cargo Hold is TRUE. Attempting to withdraw cargo...")
            CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
            withdrawCargo()

            if (CrystalExtractorTask(script).checkAndExecuteInterrupt()) { return }

            dropSalvageItems()
        }

        if (!Inventory.isFull()) {
            script.logger.info("TASK: Drop/Withdraw complete. Inventory is no longer full. Transitioning to respawn wait.")
            script.currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
            script.currentRespawnWait = Random.nextInt(
                ShipwreckSalvager.RESPAWN_WAIT_MIN_MILLIS,
                ShipwreckSalvager.RESPAWN_WAIT_MAX_MILLIS
            ).toLong()
            script.phaseStartTime = System.currentTimeMillis()
            script.logger.info("PHASE CHANGE: Transitioned to ${script.currentPhase.name} for ${script.currentRespawnWait / 1000L}s.")
        } else {
            script.logger.warn("TASK: Drop attempt failed. Inventory is still full. Retrying.")
            Condition.sleep(Random.nextInt(500, 1000))
        }
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

        val salvageItems = Inventory.stream().name(salvageItemName).list()

        if (salvageItems.isEmpty()) {
            script.logger.info("LOGIC: No '$salvageItemName' found to drop. Returning.")
            return
        }

        val dropMethod = if (useTapToDrop) "Tap-to-drop (enabled)" else "Right-click drop"
        script.logger.info("ACTION: Dropping ${salvageItems.size} items using $dropMethod...")

        if (useTapToDrop) {
            for (item in salvageItems) {
                if (item.click()) {
                    script.logger.debug("Tapped item: ${item.name()}")
                    Condition.sleep(Random.nextInt(100, 200))
                } else {
                    script.logger.warn("FAIL: Failed to tap item: ${item.name()}. Aborting tap sequence.")
                    break
                }
            }
        } else {
            for (item in salvageItems) {
                if (item.interact("Drop")) {
                    script.logger.debug("Right-click dropped item: ${item.name()}")
                    Condition.sleep(Random.nextInt(300, 500))
                } else {
                    script.logger.warn("FAIL: Failed to right-click drop item: ${item.name()}. Aborting drop sequence.")
                    break
                }
            }
        }

        val waitSuccess = Condition.wait({ Inventory.stream().name(salvageItemName).isEmpty() }, 500, 5)

        if (Inventory.stream().name(salvageItemName).isNotEmpty()) {
            script.logger.warn("DROP CHECK: Drop sequence complete, but some items remain. Remaining count: ${Inventory.stream().name(salvageItemName).count()}")
        } else {
            script.logger.info("DROP CHECK: All salvage items successfully dropped. Success: $waitSuccess")
        }
    }

    private fun withdrawCargo() {
        val waitTime = Random.nextInt(900, 1200)

        fun getRandomOffsetLarge() = Random.nextInt(-6, 7)
        script.logger.info("CARGO: Starting 3-tap cargo withdrawal sequence.")

        // Tap 1: Open Cargo (302, 308)
        val x1 = 302 + getRandomOffsetLarge()
        val y1 = 308 + getRandomOffsetLarge()
        if (Input.tap(x1, y1)) {
            script.logger.info("CARGO TAP 1 (Open): Tapped at ($x1, $y1). Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("CARGO TAP 1 (Open): Tap at ($x1, $y1) failed.")
        }

        // Tap 2: Withdraw Cargo (143, 237)
        val x2 = 143 + getRandomOffsetLarge()
        val y2 = 237 + getRandomOffsetLarge()
        if (Input.tap(x2, y2)) {
            script.logger.info("CARGO TAP 2 (Withdraw): Tapped at ($x2, $y2). Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("CARGO TAP 2 (Withdraw): Tap at ($x2, $y2) failed.")
        }

        // Tap 3: Close Cargo (569, 168)
        val x3 = 569 + getRandomOffsetLarge()
        val y3 = 168 + getRandomOffsetLarge()
        if (Input.tap(x3, y3)) {
            script.logger.info("CARGO TAP 3 (Close): Tapped at ($x3, $y3). Waiting $waitTime ms.")
            Condition.sleep(waitTime)
        } else {
            script.logger.warn("CARGO TAP 3 (Close): Tap at ($x3, $y3) failed.")
        }
    }
}