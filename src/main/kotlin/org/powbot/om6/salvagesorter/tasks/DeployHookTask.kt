// ========================================
// DeployHookTask.kt
// ========================================
package org.powbot.om6.salvagesorter.tasks

import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.api.rt4.Inventory
import org.powbot.api.Condition

class DeployHookTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        // Only activate in SALVAGING phase when we don't have salvage in inventory
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return script.currentPhase == SalvagePhase.SALVAGING && !hasSalvage && !script.cargoHoldFull
    }

    override fun execute() {
        script.logger.info("DEPLOY: Starting hook deployment sequence.")
        walkToHook(script)
        // Check for extractor interrupt before action
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        // Execute hook action (will return false if interrupted by extractor)
        val success = hookSalvage(script)

        // Check for extractor interrupt after action (required if hookSalvage returned true)
        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            script.logger.info("DEPLOY: Hook deployed successfully. Waiting for salvage.")
            // Stay in SALVAGING phase - wait for salvage to arrive
        } else {
            // FIX: DO NOT transition to IDLE or SETUP_SALVAGING if it failed,
            // as this could be an extractor interrupt or dialogue.
            // Keeping the phase as SALVAGING forces the script to retry hook deployment
            // on the next poll cycle.
            script.logger.warn("DEPLOY: Hook deployment failed or was interrupted (by extractor/dialogue). Will retry immediately.")
            // The phase remains SalvagePhase.SALVAGING, so this task will activate again next poll.
        }
    }
}