package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.*
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.LootConfig

// --- Cleanup/Alch/Drop Logic ---
fun executeCleanupLoot(script: SalvageSorter): Boolean {
    var successfullyCleaned = false

    val highAlchSpell = Magic.Spell.HIGH_ALCHEMY
    script.logger.info("CLEANUP: Starting alching loop (High Priority Cleanup).")

    LootConfig.ALCH_LIST.forEach { itemName ->
        val item = Inventory.stream().name(itemName).firstOrNull()
        if (item != null && item.valid()) {
            script.logger.info("CLEANUP: Casting High Alch on $itemName.")
            successfullyCleaned = true

            if (highAlchSpell.cast()) {
                if (item.click()) {
                    script.logger.info("CLEANUP: Alch successful. Sleeping for animation/cooldown: 3000-3600ms.")
                    Condition.sleep(Random.nextInt(3000, 3600))
                    Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)
                } else {
                    script.logger.warn("CLEANUP: Failed to click item $itemName.")
                }
            } else {
                script.logger.warn("CLEANUP: Failed to select High Alch spell.")
                return successfullyCleaned
            }
        }
    }

    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully for dropping.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }

    script.logger.info("CLEANUP: Starting item drop process for non-alchable items (Low Priority Cleanup).")
    LootConfig.DROP_LIST.forEach { itemName ->
        Inventory.stream().name(itemName).forEach { item ->
            script.logger.info("CLEANUP: Attempting to drop $itemName.")
            successfullyCleaned = true
            if (item.click()) {
                Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)
            }
        }
    }

    if (successfullyCleaned) {
        Condition.sleep(Random.nextInt(800, 1500))
    }

    return successfullyCleaned
}

// --- Cargo Withdrawal Logic ---
fun executeWithdrawCargo(script: SalvageSorter): Boolean {
    fun getRandomOffsetLarge() = Random.nextInt(-3, 3)
    val mainWait = Random.nextInt(900, 1200)

    script.logger.info("CARGO: Starting 4-tap cargo withdrawal sequence.")

    val x1 = 364 + getRandomOffsetLarge()
    val y1 = 144 + getRandomOffsetLarge()
    if (!Input.tap(x1, y1)) return false
    script.logger.info("CARGO TAP 1 (Open): Tapped at ($x1, $y1). Waiting for interaction.")
    Condition.sleep(mainWait)
    Condition.sleep(Random.nextInt(1800, 2400))

    val x2 = 143 + getRandomOffsetLarge()
    val y2 = 237 + getRandomOffsetLarge()
    if (!Input.tap(x2, y2)) return false
    script.logger.info("CARGO TAP 2 (Withdraw): Tapped at ($x2, $y2). Waiting $mainWait ms.")
    Condition.sleep(mainWait)

    val x3 = 571 + getRandomOffsetLarge()
    val y3 = 159 + getRandomOffsetLarge()
    if (!Input.tap(x3, y3)) return false
    script.logger.info("CARGO TAP 3 (Close): Tapped at ($x3, $y3). Waiting $mainWait ms.")
    Condition.sleep(mainWait)

    val x4 = 567 + getRandomOffsetLarge()
    val y4 = 460 + getRandomOffsetLarge()
    if (!Input.tap(x4, y4)) return false
    script.logger.info("CARGO TAP 4 (Walk back): Tapped at ($x4, $y4). Waiting for walk back.")
    Condition.sleep(Random.nextInt(1800, 2400))

    return true
}

// --- Sort Salvage Logic ---
fun executeTapSortSalvage(script: SalvageSorter, salvageItemName: String): Boolean {
    val SORT_BUTTON_X = 587
    val SORT_BUTTON_Y = 285
    val SORT_BUTTON_TOLERANCE = 3

    CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
    Condition.sleep(Random.nextInt(500, 800))

    val randomOffsetX = Random.nextInt(-SORT_BUTTON_TOLERANCE, SORT_BUTTON_TOLERANCE + 1)
    val randomOffsetY = Random.nextInt(-SORT_BUTTON_TOLERANCE, SORT_BUTTON_TOLERANCE + 1)
    val finalX = SORT_BUTTON_X + randomOffsetX
    val finalY = SORT_BUTTON_Y + randomOffsetY

    val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

    script.logger.info("ACTION: Tapping 'Sort Salvage' button at X=$finalX, Y=$finalY. Count before: $salvageCountBefore.")

    if (Input.tap(finalX, finalY)) {
        val sleepBetween = 5
        var count = 0

        while (count < sleepBetween) {
            val sleepTime = Random.nextInt(1000, 2000)
            Condition.sleep(sleepTime)
            script.logger.debug("DIALOGUE: Sleeping $sleepTime ms before next continue click (Count: ${count + 1}).")
            count++
        }
        if (Chat.canContinue()) {
            Chat.clickContinue()
            Condition.sleep(Random.nextInt(1500, 2500))
        }
        val waitSuccess = Condition.wait({ Inventory.stream().name(salvageItemName).isEmpty() }, 300, 10)

        if (waitSuccess) {
            script.logger.info("SUCCESS: 'Sort Salvage' complete. Extended AFK wait: 5000-8000ms.")
            return true
        } else {
            script.logger.warn("FAIL: 'Sort Salvage' wait timed out. Salvage still present.")
            return false
        }
    }
    return false
}