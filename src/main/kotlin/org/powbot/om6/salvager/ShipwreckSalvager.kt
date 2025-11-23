package org.powbot.om6.salvager

import com.google.common.eventbus.Subscribe
import org.powbot.api.script.ScriptManifest
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.Tile
import org.powbot.om6.salvager.tasks.DropSalvageTask
import org.powbot.om6.salvager.tasks.ReadyToTapTask
import org.powbot.om6.salvager.tasks.RespawnWaitTask
import org.powbot.om6.salvager.tasks.SalvagePhase
import org.powbot.om6.salvager.tasks.Task
import org.powbot.om6.salvager.tasks.WaitingForActionTask



@ScriptManifest(
    name = "0m6 Shipwreck Salvager",
    description = "Salvages Shipwrecks using a task-based system.",
    version = "1.1.0",
    author = "You",
    category = ScriptCategory.Other
)
class ShipwreckSalvager : AbstractScript() {

    // --- Script State (Public/Internal) ---
    @Volatile
    var currentPhase: SalvagePhase = SalvagePhase.READY_TO_TAP
    @Volatile
    var phaseStartTime: Long = 0L
    @Volatile
    var currentRespawnWait: Long = RESPAWN_WAIT_MIN_MILLIS.toLong()
    @Volatile
    var salvageMessageFound = false
    @Volatile
    var startTile: Tile? = null

    // XP Tracking Variables
    @Volatile
    private var initialOverallXp: Long = 0L
    @Volatile
    private var xpTrackStartTime: Long = 0L
    @Volatile
    private var currentGainedXp: Long = 0L // New variable for calculated gain
    @Volatile
    private var currentXpPerHour: Double = 0.0 // New variable for calculated rate


    // --- Constants ---
    companion object {
        const val ACTION_TIMEOUT_MILLIS = 450 * 1000
        const val RESPAWN_WAIT_MIN_MILLIS = 10 * 100
        const val RESPAWN_WAIT_MAX_MILLIS = 20 * 100
        const val DIALOGUE_RESTART_MIN_MILLIS = 15 * 100
        const val DIALOGUE_RESTART_MAX_MILLIS = 20 * 100
        const val SALVAGE_COMPLETE_MESSAGE = "You salvage all you can"
        const val SALVAGE_NAME = "Plundered salvage"
    }

    // --- Task List ---
    private val taskList: List<Task> by lazy {
        listOf(
            DropSalvageTask(this),      // Check for full inventory/drop flag first
            RespawnWaitTask(this),      // Check for wait completion
            ReadyToTapTask(this),       // Initiate action
            WaitingForActionTask(this)  // Wait for completion/handle timeout/dialogue
        )
    }

    // --- Event Handler ---
    @Subscribe
    fun onMessageEvent(change: MessageEvent) {
        if (change.messageType == MessageType.Game && change.message.contains(SALVAGE_COMPLETE_MESSAGE)) {
            logger.info("EVENT: Salvage COMPLETE message detected via EventBus!")
            salvageMessageFound = true
            // Capture tile upon message reception, which happens before the drop phase
            startTile = Players.local().tile()
            logger.info("Updated startTile after dialogue to: $startTile")
        }
    }

    /**
     * Calculates the current XP gained and XP/hr, updating the class member variables.
     */
    private fun updateXpTracking() {
        val currentXp = Skills.experience(Skill.Overall).toLong()
        val gainedXp = currentXp - initialOverallXp
        val elapsedTimeSeconds = (System.currentTimeMillis() - xpTrackStartTime) / 1000.0

        currentGainedXp = gainedXp

        // Calculate XP per hour
        currentXpPerHour = if (elapsedTimeSeconds > 0) {
            (gainedXp / elapsedTimeSeconds) * 3600
        } else {
            0.0
        }
    }

    // --- Main Loop ---
    override fun poll() {
        try {
            // XP Tracking Logic moved to poll() to ensure it updates frequently
            updateXpTracking()

            // Find the task matching the current phase and execute it.
            val activeTask = taskList.firstOrNull { it.activate() }

            if (activeTask != null) {
                logger.info("Executing Task: ${activeTask::class.simpleName} (Phase: $currentPhase)")
                activeTask.execute()
            } else {
                // Should not happen if all phases are covered by tasks
                logger.warn("No active task found for phase: $currentPhase. Sleeping briefly.")
                Thread.sleep(1000)
            }

        } catch (e: Exception) {
            logger.warn("CRASH in poll(): ${e.message}")
            e.printStackTrace()
            // Optional: Transition to a safe state or stop the script
            // currentPhase = SalvagePhase.DROPPING_SALVAGE
        }
    }


    override fun onStart() {
        phaseStartTime = System.currentTimeMillis()
        currentPhase = SalvagePhase.READY_TO_TAP
        salvageMessageFound = false
        startTile = Players.local().tile()

        // Initialize XP tracking
        initialOverallXp = Skills.experience(Skill.Overall).toLong()
        xpTrackStartTime = System.currentTimeMillis()
        logger.info("INITIAL XP: Overall XP started at ${String.format("%,d", initialOverallXp)}")

        // Set up the Paint
        val paint = PaintBuilder.newBuilder()
            .x(40)
            .y(80)
            .addString("Status") {
                val baseStatus = when (currentPhase) {
                    SalvagePhase.READY_TO_TAP -> "Ready to Tap"
                    SalvagePhase.WAITING_FOR_ACTION -> {
                        val remaining = (ACTION_TIMEOUT_MILLIS - (System.currentTimeMillis() - phaseStartTime)) / 1000L
                        if (salvageMessageFound) "Message DETECTED! (Dropping next)"
                        else "Salvaging '$SALVAGE_NAME' (Timeout in: ${if (remaining > 0) "${remaining}s" else "Expired"})"
                    }
                    SalvagePhase.DROPPING_SALVAGE -> "Dropping Salvage"
                    SalvagePhase.WAITING_FOR_RESPAWN -> {
                        val totalSeconds = currentRespawnWait / 1000L
                        val remaining = (phaseStartTime + currentRespawnWait - System.currentTimeMillis()) / 1000L
                        "Respawn Wait (${totalSeconds}s total): ${remaining}s"
                    }
                }
                if (currentPhase != SalvagePhase.WAITING_FOR_ACTION && Chat.canContinue()) {
                    "DIALOGUE BLOCKING - Clicking..." // Show dialogue override even if in other phases for awareness
                } else {
                    baseStatus
                }
            }

//            .addString("(Broken) Starting XP (Overall)") {
//                String.format("%,d", initialOverallXp)
//            }
//            .addString("(Broken) Current XP (Overall)") {
//                // Now directly calling API since calculation is done in poll()
//                String.format("%,d", Skills.experience(Skill.Overall).toLong())
//            }
//            .addString("(Broken) XP Gained (Overall)") {
//                // Read from pre-calculated class properties
//                "Gained: ${String.format("%,d", currentGainedXp)} (XP/Hr: ${String.format("%,.0f", currentXpPerHour)})"
//            }
            .build()
        addPaint(paint)
        logger.info("START: Shipwreck Salvager (Task System Initialized)")

    }
}