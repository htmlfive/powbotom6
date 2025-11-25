package org.powbot.om6.salvager

import com.google.common.eventbus.Subscribe
import org.powbot.api.Tile
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.salvager.tasks.*
import org.powbot.api.script.ScriptConfiguration.List as ConfigList

@ScriptManifest(
    name = "0m6 Shipwreck Salvager",
    description = "Start zoomed in all the way, with camera all the way down",
    version = "1.2.8",
    author = "You",
    category = ScriptCategory.Other
)
@ConfigList(
    [
        ScriptConfiguration(
            "Enable Extractor",
            "If true, the script will perform the timed (61s) or message-triggered tap for the Extractor device.",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Withdraw Cargo",
            "Will withdraw from cargo hold and drop items if true. Requires you to be using the sloop and salvaging hook next to cargo hold",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Stop if Moved",
            "If true, the script will stop immediately if player position drifts from the starting tile during the READY_TO_TAP phase.",
            optionType = OptionType.BOOLEAN, defaultValue = "false"
        ),
        ScriptConfiguration(
            "Sleep Level (1000-2000ms x Level)",
            "The camera direction (e.g., North, East) required to reliably click the salvage spot.",
            optionType = OptionType.STRING, defaultValue = "5"
        ),
        ScriptConfiguration(
            "Ready-to-Tap Direction",
            "The camera direction (e.g., North, East) required to reliably click the salvage spot.",
            optionType = OptionType.STRING, defaultValue = "North",
            allowedValues = ["North", "East", "South", "West"]
        ),
        ScriptConfiguration(
            "Drop Salvage Direction",
            "The camera direction (e.g., North, East) required for fixed-screen tap locations during the drop phase.",
            optionType = OptionType.STRING, defaultValue = "North",
            allowedValues = ["North", "East", "South", "West"]
        ),
        ScriptConfiguration(
            "Salvage Item Name",
            "The exact name of the item dropped after salvaging the shipwreck (e.g., 'Plundered salvage').",
            optionType = OptionType.STRING,
            defaultValue = "Fremennik salvage",
            allowedValues = ["Small salvage", "Fishy salvage", "Barracuda salvage", "Large salvage", "Plundered salvage", "Martial salvage", "Fremennik salvage", "Opulent salvage"]
        ),
        ScriptConfiguration(
            "Tap to Drop",
            "If true, uses Shift-Click (Tap-to-Drop) for faster inventory dropping. Requires Shift-Click Drop to be enabled in game settings.",
            optionType = OptionType.BOOLEAN, defaultValue = "true", visible = false
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

    val extractorInterval: Long = 64 * 1000L
    @Volatile
    var extractorTimer: Long = 0L

    @Volatile
    var harvesterMessageFound = false

    @Volatile
    private var initialOverallXp: Long = 0L
    @Volatile
    private var xpTrackStartTime: Long = 0L
    @Volatile
    private var currentGainedXp: Long = 0L
    @Volatile
    private var currentXpPerHour: Double = 0.0
    @Volatile
    var hookCastMessageFound = false

    val tapToDrop: Boolean get() = getOption<Boolean>("Tap to Drop")

    val enableExtractor: Boolean get() = getOption<Boolean>("Enable Extractor")
    val sleepLevel: String get() = getOption<String>("Sleep Level (1000-2000ms x Level)")
    val requiredTapDirectionStr: String get() = getOption<String>("Ready-to-Tap Direction")
    val requiredDropDirectionStr: String get() = getOption<String>("Drop Salvage Direction")
    val SALVAGE_NAME: String get() = getOption<String>("Salvage Item Name")
    val withdrawCargoOnDrop: Boolean get() = getOption<Boolean>("Withdraw Cargo")
    val stopIfMoved: Boolean get() = getOption<Boolean>("Stop if Moved")


    val requiredTapDirection: CardinalDirection
        get() = CardinalDirection.valueOf(requiredTapDirectionStr)

    val requiredDropDirection: CardinalDirection
        get() = CardinalDirection.valueOf(requiredDropDirectionStr)

    companion object {
        const val ACTION_TIMEOUT_MILLIS = 450 * 1000
        const val RESPAWN_WAIT_MIN_MILLIS = 5 * 100
        const val RESPAWN_WAIT_MAX_MILLIS = 8 * 100
        const val DIALOGUE_RESTART_MIN_MILLIS = 15 * 100
        const val DIALOGUE_RESTART_MAX_MILLIS = 20 * 100
        const val SALVAGE_COMPLETE_MESSAGE = "You salvage all you can"
        const val SALVAGE_SUCCESS_MESSAGE = "You find some salvage"
        const val HOOK_CAST_MESSAGE_1 = "You cast out your salvaging hook"
        const val HOOK_CAST_MESSAGE_2 = "You start operating"
        const val HARVESTER_MESSAGE = "Your crystal extractor has harvested"
        const val TAP_TO_DROP_ENABLED_MSG = "Tap-to-drop enabled!"
        const val TAP_TO_DROP_DISABLED_MSG = "Tap-to-drop disabled!"
    }

    private val taskList: kotlin.collections.List<Task> by lazy {
        logger.info("INIT: Task list initialized.")
        listOf(
            TapToDropTask(this),
            DropSalvageTask(this, SALVAGE_NAME),
            RespawnWaitTask(this),
            CrystalExtractorTask(this),
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
            if (change.message.contains(HOOK_CAST_MESSAGE_1) || change.message.contains(HOOK_CAST_MESSAGE_2)) {
                logger.info("EVENT: CONFIRMATION - Action start message detected: ${change.message}")
                hookCastMessageFound = true
            }
            if (change.message.contains(HARVESTER_MESSAGE)) {
                logger.info("EVENT: HARVESTER MESSAGE DETECTED! Message: ${change.message}")
                harvesterMessageFound = true
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
                logger.info("EXECUTING: Task: ${activeTask::class.simpleName} (Phase: $currentPhase, TapEnabled: $isTapToDropEnabled, Extractor: $enableExtractor)")
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

        if (enableExtractor) {
            extractorTimer = System.currentTimeMillis() - extractorInterval
            logger.info("LOGIC: Extractor enabled. Timer initialized to $extractorInterval ms in the past to trigger immediate tap.")
        } else {
            extractorTimer = System.currentTimeMillis()
            logger.info("LOGIC: Extractor disabled. Timer initialized to current time (will not trigger).")
        }

        currentPhase = SalvagePhase.INITIALIZING
        logger.info("PHASE CHANGE: Starting in ${currentPhase.name}. Tap-to-Drop configured: $tapToDrop. Extractor enabled: $enableExtractor.")

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
            .addString("Extractor Tap") {
                if (enableExtractor) {
                    val timeElapsed = System.currentTimeMillis() - extractorTimer
                    val remainingSeconds = ((extractorInterval - timeElapsed) / 1000L).coerceAtLeast(0)
                    "Next in: ${remainingSeconds}s"
                } else {
                    "Disabled (GUI)"
                }
            }
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
            .addString("XP/Hr") { String.format("%,.0f", currentXpPerHour) }
            .build()
        addPaint(paint)
        logger.info("START: Shipwreck Salvager (Task System Initialized and Paint Added)")
    }
}