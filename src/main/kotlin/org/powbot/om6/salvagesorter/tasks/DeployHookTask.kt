package org.powbot.om6.salvagesorter.tasks

import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.api.rt4.Inventory
import org.powbot.api.Condition

class DeployHookTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Only activate in SALVAGING phase when:
        // 1. Cargo is not full (otherwise we should be in sorting mode)
        // 2. Inventory is not full (when full, DepositCargoTask takes over)

        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        val inventoryFull = Inventory.isFull()

        // KEY CHANGE: We should keep deploying hook even if we have some salvage,
        // as long as inventory isn't full yet
        val shouldActivate = script.currentPhase == SalvagePhase.SALVAGING &&
                !script.cargoHoldFull &&
                !inventoryFull

        // ALWAYS log when in SALVAGING phase to debug activation issues
        if (script.currentPhase == SalvagePhase.SALVAGING || shouldActivate) {
            script.logger.info("DEPLOY ACTIVATE CHECK: Phase=${script.currentPhase.name}, hasSalvage=$hasSalvage, cargoFull=${script.cargoHoldFull}, invFull=$inventoryFull, RESULT=$shouldActivate")
        }

        return shouldActivate
    }

    override fun execute() {
        script.logger.info("DEPLOY: Starting hook deployment sequence.")

        // Check for extractor interrupt before action
        if (extractorTask.checkAndExecuteInterrupt(script)) {
            script.logger.info("DEPLOY: Extractor interrupted before hook action. Task will retry next poll.")
            return
        }

        // Execute hook action (walkToHook should have been called by SetupSalvagingTask)
        val success = hookSalvage(script)

        // Check for extractor interrupt after action
        if (extractorTask.checkAndExecuteInterrupt(script)) {
            script.logger.info("DEPLOY: Extractor interrupted after hook action. Task will retry next poll.")
            return
        }

        if (success) {
            script.logger.info("DEPLOY: Hook deployed successfully. Waiting for salvage to appear in inventory.")
            // Stay in SALVAGING phase - wait for salvage to arrive in inventory
            // The poll loop will keep calling this task until salvage appears
        } else {
            script.logger.warn("DEPLOY: Hook deployment failed (dialogue/error/interrupt). Ensuring phase is SALVAGING for immediate retry.")
            // CRITICAL: Explicitly set phase to SALVAGING to ensure retry
            script.currentPhase = SalvagePhase.SALVAGING
            Condition.sleep(200) // Very small delay before retry
        }
    }
}