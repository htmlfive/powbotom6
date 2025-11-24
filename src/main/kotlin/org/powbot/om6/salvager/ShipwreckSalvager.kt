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
import org.powbot.om6.salvager.tasks.CardinalDirection
import org.powbot.om6.salvager.tasks.DropSalvageTask
import org.powbot.om6.salvager.tasks.ReadyToTapTask
import org.powbot.om6.salvager.tasks.RespawnWaitTask
import org.powbot.om6.salvager.tasks.SalvagePhase
import org.powbot.om6.salvager.tasks.Task
import org.powbot.om6.salvager.tasks.WaitingForActionTask
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptConfiguration.List as ConfigList
import org.powbot.api.script.OptionType
import org.powbot.om6.salvager.tasks.TapToDropTask

@ScriptManifest(
    name = "0m6 Shipwreck Salvager",
    description = "Salvages Shipwrecks using a task-based system.",
    version = "1.2.5",
    author = "You",
    category = ScriptCategory.Other
)
@ConfigList(
    [
        ScriptConfiguration(
            "Withdraw Cargo",
            "Will withdraw from cargo hold and drop items if true. Requires camera to be set to EAST.",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Ready-to-Tap Direction",
            "The camera direction (e.g., North, East) required to reliably click the salvage spot.",
            optionType = OptionType.STRING, defaultValue = "East",
            allowedValues = ["North", "East", "South", "West"]
        ),
        ScriptConfiguration(
            "Drop Salvage Direction",
            "The camera direction (e.g., North, East) required for fixed-screen tap locations during the drop phase.",
            optionType = OptionType.STRING, defaultValue = "East",
            allowedValues = ["North", "East", "South", "West"]
        ),
        ScriptConfiguration(
            "Salvage Item Name",
            "The exact name of the item dropped after salvaging the shipwreck (e.g., 'Plundered salvage').",
            optionType = OptionType.STRING,
            defaultValue = "Plundered salvage",
            allowedValues = ["Small salvage", "Fishy salvage", "Barracuda salvage", "Large salvage", "Plundered salvage", "Martial salvage", "Fremennik salvage", "Opulent salvage"]
        )
    ]
)
class ShipwreckSalvager : AbstractScript() {

    @Volatile
    var currentPhase: SalvagePhase = SalvagePhase.INITIALIZING
    @Volatile
    var phaseStartTime: Long = 0L
    @Volatile
    var currentRespawnWait: Long = RESPAWN_WAIT_MIN_MILLIS.toLong()
    @Volatile
    var salvageMessageFound = false
    @Volatile
    var startTile: Tile? = null
    @Volatile
    var isTapToDropEnabled: Boolean = false

    @Volatile
    private var initialOverallXp: Long = 0L
    @Volatile
    private var xpTrackStartTime: Long = 0L
    @Volatile
    private var currentGainedXp: Long = 0L
    @Volatile
    private var currentXpPerHour: Double = 0.0

    @ScriptConfiguration(
        "Tap to Drop",
        "If true, uses Shift-Click (Tap-to-Drop) for faster inventory dropping. Requires Shift-Click Drop to be enabled in game settings.",
        optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false
    )
    var tapToDrop: Boolean = true

    @Volatile
    @ScriptConfiguration("Ready-to-Tap Direction", "Description")
    var requiredTapDirectionStr: String = "East"

    @Volatile
    @ScriptConfiguration("Drop Salvage Direction", "Description")
    var requiredDropDirectionStr: String = "East"

    @Volatile
    @ScriptConfiguration("Salvage Item Name", "Description")
    var SALVAGE_NAME: String = "Plundered salvage"

    val requiredTapDirection: CardinalDirection
        get() = CardinalDirection.valueOf(requiredTapDirectionStr).also { logger.info("ACCESS: requiredTapDirection returned $it") }

    val requiredDropDirection: CardinalDirection
        get() = CardinalDirection.valueOf(requiredDropDirectionStr).also { logger.info("ACCESS: requiredDropDirection returned $it") }

    val withdrawCargoOnDrop: Boolean get() = getOption<Boolean>("Withdraw Cargo").also { logger.info("ACCESS: withdrawCargoOnDrop returned $it") } ?: false

    companion object {
        const val ACTION_TIMEOUT_MILLIS = 450 * 1000
        const val RESPAWN_WAIT_MIN_MILLIS = 5 * 100
        const val RESPAWN_WAIT_MAX_MILLIS = 8 * 100
        const val DIALOGUE_RESTART_MIN_MILLIS = 15 * 100
        const val DIALOGUE_RESTART_MAX_MILLIS = 20 * 100
        const val SALVAGE_COMPLETE_MESSAGE = "You salvage all you can"
        const val SALVAGE_SUCCESS_MESSAGE = "You find some salvage"

        const val TAP_TO_DROP_ENABLED_MSG = "Tap-to-drop enabled!"
        const val TAP_TO_DROP_DISABLED_MSG = "Tap-to-drop disabled!"
    }

    private val taskList: kotlin.collections.List<Task> by lazy {
        logger.info("INIT: Task list initialized.")
        listOf(
            TapToDropTask(this),
            DropSalvageTask(this, SALVAGE_NAME),
            RespawnWaitTask(this),
            ReadyToTapTask(this),
            WaitingForActionTask(this)
        )
    }

    @Subscribe
    fun onMessageEvent(change: MessageEvent) {
        if (change.messageType == MessageType.Game) {
            logger.info("EVENT: Game Message received: ${change.message}")
            if (change.message.contains(SALVAGE_COMPLETE_MESSAGE) || change.message.contains(SALVAGE_SUCCESS_MESSAGE)) {
                logger.info("EVENT: Salvage SUCCESS message detected via EventBus! Message: ${change.message}")
                salvageMessageFound = true
                startTile = Players.local().tile()
                logger.info("LOGIC: Updated startTile after dialogue to: $startTile")

                currentPhase = SalvagePhase.DROPPING_SALVAGE
                phaseStartTime = System.currentTimeMillis()
                logger.info("PHASE CHANGE: Transitioned to ${currentPhase.name}")
            }

            if (change.message.contains(TAP_TO_DROP_ENABLED_MSG)) {
                logger.info("EVENT: CONFIRMATION - Tap-to-drop is now ENABLED.")
                isTapToDropEnabled = true
            } else if (change.message.contains(TAP_TO_DROP_DISABLED_MSG)) {
                logger.info("EVENT: CONFIRMATION - Tap-to-drop is now DISABLED.")
                isTapToDropEnabled = false
            }
        }
    }

    private fun updateXpTracking() {
        val currentXp = Skills.experience(Skill.Overall).toLong()
        if (initialOverallXp == 0L) {
            initialOverallXp = currentXp
            logger.info("XP TRACKING: Initial Overall XP set to ${String.format("%,d", initialOverallXp)}")
        }

        val gainedXp = currentXp - initialOverallXp
        val elapsedTimeSeconds = (System.currentTimeMillis() - xpTrackStartTime) / 1000.0

        currentGainedXp = gainedXp

        currentXpPerHour = if (elapsedTimeSeconds > 0) {
            (gainedXp / elapsedTimeSeconds) * 3600
        } else {
            0.0
        }
        logger.debug("XP TRACKING: Gained XP: ${String.format("%,d", currentGainedXp)}, XP/hr: ${String.format("%,.0f", currentXpPerHour)}")
    }

    override fun poll() {
        try {
            updateXpTracking()

            val activeTask = taskList.firstOrNull { it.activate().also { isActive -> logger.debug("TASK CHECK: ${it::class.simpleName} activate() returned $isActive") } }

            if (activeTask != null) {
                logger.info("EXECUTING: Task: ${activeTask::class.simpleName} (Phase: $currentPhase, TapEnabled: $isTapToDropEnabled, TapConfig: $tapToDrop)")
                activeTask.execute()
            } else {
                logger.warn("POLL: No active task found for phase: $currentPhase. Sleeping briefly.")
                Thread.sleep(1000)
            }

        } catch (e: Exception) {
            logger.error("CRASH in poll(): ${e.message}", e)
            Thread.sleep(1000)
        }
    }


    override fun onStart() {
        logger.info("SCRIPT START: Initializing Shipwreck Salvager...")
        phaseStartTime = System.currentTimeMillis()
        salvageMessageFound = false
        startTile = Players.local().tile()
        logger.info("LOGIC: Initial startTile set to $startTile")

        isTapToDropEnabled = false
        logger.info("LOGIC: isTapToDropEnabled reset to false to force initial check.")

        currentPhase = SalvagePhase.INITIALIZING
        logger.info("PHASE CHANGE: Starting in ${currentPhase.name}. Tap-to-Drop configured: $tapToDrop.")

        initialOverallXp = Skills.experience(Skill.Overall).toLong()
        xpTrackStartTime = System.currentTimeMillis()
        logger.info("XP TRACKING: Overall XP started at ${String.format("%,d", initialOverallXp)}")

        val paint = PaintBuilder.newBuilder()
            .x(40)
            .y(80)
            .addString("Drop Mode") {
                if (tapToDrop && isTapToDropEnabled) "Tap-to-Drop (FAST)"
                else "Right-Click (SAFE)"
            }
            .addString("Withdraw Cargo") { if (withdrawCargoOnDrop) "YES" else "NO" }
            .addString("Tap Dir") { requiredTapDirection.toString() }
            .addString("Drop Dir") { requiredDropDirection.toString() }
            .addString("Salvage Item") { SALVAGE_NAME }
            .addString("Status") {
                val baseStatus = when (currentPhase) {
                    SalvagePhase.INITIALIZING -> {
                        val desiredState = if(tapToDrop) "ENABLE" else "DISABLE"
                        "Synchronizing Tap-to-Drop (Goal: $desiredState)..."
                    }
                    SalvagePhase.READY_TO_TAP -> "Ready to Tap"
                    SalvagePhase.WAITING_FOR_ACTION -> {
                        val remaining = (ACTION_TIMEOUT_MILLIS - (System.currentTimeMillis() - phaseStartTime)) / 1000L
                        if (salvageMessageFound) "Message DETECTED! (Dropping next)"
                        else "Salvaging (Timeout in: ${if (remaining > 0) "${remaining}s" else "Expired"})"
                    }
                    SalvagePhase.DROPPING_SALVAGE -> {
                        val dropStatus = if (tapToDrop && !isTapToDropEnabled) "ENABLING Tap-to-Drop..."
                        else "Dropping Salvage (Cargo Withdraw: ${if (withdrawCargoOnDrop) "YES" else "NO"})"
                        dropStatus
                    }
                    SalvagePhase.WAITING_FOR_RESPAWN -> {
                        val totalSeconds = currentRespawnWait / 1000L
                        val remaining = (phaseStartTime + currentRespawnWait - System.currentTimeMillis()) / 1000L
                        "Respawn Wait (${totalSeconds}s total): ${remaining}s"
                    }
                }
                if (currentPhase != SalvagePhase.WAITING_FOR_ACTION && Chat.canContinue()) {
                    "DIALOGUE BLOCKING - Clicking..."
                } else {
                    baseStatus
                }
            }
            .addString("XP Gained") { String.format("%,d", currentGainedXp) }
            .addString("XP/Hr (Overall)") { String.format("%,.0f", currentXpPerHour) }
            .build()
        addPaint(paint)
        logger.info("START: Shipwreck Salvager (Task System Initialized and Paint Added)")
    }
}