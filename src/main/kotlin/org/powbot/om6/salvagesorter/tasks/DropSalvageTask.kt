package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase

class DropSalvageTask(script: SalvageSorter) : Task(script) {
    override fun activate(): Boolean {
        // Only activate in Power Salvage mode when inventory is full
        if (!script.powerSalvageMode) return false

        val hasSalvage = Inventory.stream().name(script.salvageName).isNotEmpty()
        val inventoryFull = Inventory.isFull()

        return hasSalvage && inventoryFull
    }

    override fun execute() {
        script.logger.info("POWER SALVAGE: Dropping all salvage from inventory.")
        script.currentPhase = SalvagePhase.CLEANING

        // Drop all salvage items
        val success = dropAllSalvage(script)

        if (success) {
            script.logger.info("POWER SALVAGE: Successfully dropped all salvage. Returning to salvaging.")
            script.currentPhase = SalvagePhase.SALVAGING
        } else {
            script.logger.warn("POWER SALVAGE: Failed to drop salvage. Will retry.")
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }

    private fun dropAllSalvage(script: SalvageSorter): Boolean {
        val salvageItems = Inventory.stream().name(script.salvageName).toList()

        if (salvageItems.isEmpty()) {
            script.logger.info("POWER SALVAGE: No salvage to drop.")
            return true
        }

        script.logger.info("POWER SALVAGE: Dropping ${salvageItems.size} salvage items.")

        // Drop each item with a short delay between drops
        salvageItems.forEach { item ->
            if (item.valid()) {
                if (!script.tapToDrop) {
                    Game.setSingleTapToggle(false)
                    item.interact("Drop")
                    Condition.sleep(Random.nextInt(300, 500))
                } else {
                    Game.setSingleTapToggle(false)
                    item.click()
                    Condition.sleep(Random.nextInt(300, 500))
                }
            }
        }

        // Wait a moment for all drops to complete
        Condition.sleep(Random.nextInt(300, 500))

        // Verify all salvage was dropped
        val remainingSalvage = Inventory.stream().name(script.salvageName).count()

        return remainingSalvage == 0L
    }
}