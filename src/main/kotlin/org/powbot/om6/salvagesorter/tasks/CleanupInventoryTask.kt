package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Magic
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.LootConfig
import org.powbot.om6.salvagesorter.config.SalvagePhase
import kotlin.random.Random as KotlinRandom

class CleanupInventoryTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()

        // In Power Salvage mode, NEVER clean up salvage here (DropSalvageTask handles it)
        // Only clean up other junk items
        if (script.powerSalvageMode) {
            if (hasSalvage) return false

            val hasJunk = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()
            return hasJunk
        }

        // Normal mode: Don't activate if we still have salvage to process
        if (hasSalvage) return false

        val hasCleanupLoot = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()
        return hasCleanupLoot
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.CLEANING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 3).")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeCleanupLoot()

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // FIX: Always transition to SALVAGING upon success (when not sorting)
        // This ensures the script returns to the primary task loop (DeployHookTask)
        script.currentPhase = if (success) SalvagePhase.SALVAGING else SalvagePhase.CLEANING

        script.logger.info("PHASE: Cleanup complete/failed. Transitioned to ${script.currentPhase.name}.")
    }

    /**
     * Executes cleanup of loot items (alching and dropping).
     * @return true if any items were successfully cleaned up
     */
     private fun executeCleanupLoot(): Boolean {
        Game.setSingleTapToggle(false)
        var successfullyCleaned = false
        CameraSnapper.snapCameraToDirection(script.cameraDirection, script)
        val highAlchSpell = Magic.Spell.HIGH_ALCHEMY
        script.logger.info("CLEANUP: Starting alching loop.")

        LootConfig.ALCH_LIST.forEach { itemName ->
            val item = Inventory.stream().name(itemName).firstOrNull()
            if (item != null && item.valid()) {
                script.logger.info("CLEANUP: Attempting High Alch on $itemName.")
                successfullyCleaned = true

                if (highAlchSpell.cast("Cast") && Condition.wait({ Game.tab() == Game.Tab.INVENTORY }, 125, 12)) {
                    if (item.interact("Cast")) {
                        Condition.wait({ Game.tab() == Game.Tab.MAGIC }, 125, 12)
                        script.logger.info("CLEANUP: Alch successful. Sleeping for animation.")
                        Condition.sleep(Random.nextInt(Constants.CLEANUP_ALCH_MIN, Constants.CLEANUP_ALCH_MAX))
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

        if (!ensureInventoryOpen(Constants.ASSIGNMENT_INV_OPEN_MIN, Constants.ASSIGNMENT_INV_OPEN_MAX)) {
            script.logger.warn("CLEANUP: Failed to open inventory tab.")
        }

        val shuffledDroppableItems = Inventory.stream()
            .filter { item -> item.valid() && item.name() in LootConfig.DROP_LIST }
            .toList()
            .shuffled(KotlinRandom)

        script.logger.info("CLEANUP: Dropping ${shuffledDroppableItems.size} items.")

        shuffledDroppableItems.forEach { itemToDrop ->
            if (itemToDrop.valid()) {
                if (!script.tapToDrop) {
                    Game.setSingleTapToggle(false)
                    itemToDrop.interact("Drop")
                } else {
                    Game.setSingleTapToggle(false)
                    itemToDrop.click()
                }
                Condition.sleep(Random.nextInt(Constants.CLEANUP_DROP_MIN, Constants.CLEANUP_DROP_MAX))
            }
        }

        if (successfullyCleaned) {
            Condition.sleep(Random.nextInt(Constants.CLEANUP_DROP_MIN, Constants.CLEANUP_DROP_MAX))
        }

        return successfullyCleaned
    }
}
