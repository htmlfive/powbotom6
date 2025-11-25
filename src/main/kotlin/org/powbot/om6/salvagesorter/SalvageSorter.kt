package org.powbot.om6.salvagesorter

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.ScriptConfiguration.List as ConfigList
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType
import org.powbot.om6.salvagesorter.config.CardinalDirection
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.om6.salvagesorter.tasks.*

private const val HARVESTER_MESSAGE = "Your crystal extractor has harvested"

@ScriptManifest(
    name = "0m6 Shipwreck Sorter",
    description = "Automates salvage sorting (High Prio), loot cleanup (Med Prio), cargo withdrawal (Low Prio), and crystal extractor taps (Highest Prio).",
    version = "1.2.2",
    author = "You",
    category = ScriptCategory.Other
)
@ConfigList(
    [
        ScriptConfiguration(
            "Drop Salvage Direction",
            "The camera direction required for fixed-screen tap locations during the sort phase.",
            optionType = OptionType.STRING, defaultValue = "West",
            allowedValues = ["North", "East", "South", "West"]
        ),
        ScriptConfiguration(
            "Salvage Item Name",
            "The exact name of the item dropped after salvaging the shipwreck.",
            optionType = OptionType.STRING,
            defaultValue = "Fremennik salvage",
            allowedValues = ["Small salvage", "Fishy salvage", "Barracuda salvage", "Large salvage", "Plundered salvage", "Martial salvage", "Fremennik salvage", "Opulent salvage"]
        ),

        ScriptConfiguration(
            "Enable Extractor",
            "If true, enables the automatic tapping of the Crystal Extractor every ~64 seconds.",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),

        ScriptConfiguration(
            "Extractor Tap Direction",
            "The camera direction required for fixed-screen tap locations during the extractor tap.",
            optionType = OptionType.STRING, defaultValue = "West",
            allowedValues = ["North", "East", "South", "West"]
        )
    ]
)
class SalvageSorter : AbstractScript() {
    // --- Configuration Accessors ---
    val requiredDropDirectionStr: String get() = getOption<String>("Drop Salvage Direction")
    val SALVAGE_NAME: String get() = getOption<String>("Salvage Item Name")
    val requiredDropDirection: CardinalDirection get() = CardinalDirection.valueOf(requiredDropDirectionStr)

    val enableExtractor: Boolean get() = getOption<Boolean>("Enable Extractor")
    val extractorInterval: Long = 64000L
    val requiredTapDirectionStr: String get() = getOption<String>("Extractor Tap Direction")
    val requiredTapDirection: CardinalDirection get() = CardinalDirection.valueOf(requiredTapDirectionStr)

    // --- State Variables ---
    @Volatile var harvesterMessageFound: Boolean = false
    @Volatile var extractorTimer: Long = 0L
    @Volatile var currentPhase: SalvagePhase = SalvagePhase.IDLE
    @Volatile var phaseStartTime: Long = 0L
    @Volatile var xpMessageCount: Int = 0 // Proxy for number of items in cargo hold

    // --- Task List (Initialized with lazy to ensure proper setup) ---
    private val taskList: kotlin.collections.List<Task> by lazy {
        logger.info("INIT: Task list initialized with priority order: Extractor -> Sort -> Cleanup -> Withdraw.")
        listOf(
            CrystalExtractorTask(this),
            SortSalvageTask(this),
            CleanupInventoryTask(this),
            WithdrawCargoTask(this)
        )
    }

    // --- Event Handlers ---
    @Subscribe
    fun onMessageEvent(change: MessageEvent) {
        if (change.messageType == MessageType.Game) {
            if (change.message.contains(HARVESTER_MESSAGE)) {
                logger.info("EVENT: HARVESTER MESSAGE DETECTED! Message: ${change.message}")
                harvesterMessageFound = true
            }
        }

        if (change.messageType == MessageType.Spam) {
            if (change.message.contains("You gain some experience")) {
                xpMessageCount++
                logger.debug("EVENT: XP message (SPAM) detected! Current count: $xpMessageCount")
            }
        }
    }

    // --- Lifecycle Methods ---
    override fun onStart() {
        logger.info("SCRIPT START: Initializing Shipwreck Sorter...")

        extractorTimer = 0L
        phaseStartTime = System.currentTimeMillis()
        currentPhase = SalvagePhase.IDLE

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Status") { currentPhase.name }
            .addString("Extractor Tap") {
                if (enableExtractor) {
                    val timeElapsed = System.currentTimeMillis() - extractorTimer
                    val remainingSeconds = ((extractorInterval - timeElapsed) / 1000L).coerceAtLeast(0)
                    "Next in: ${remainingSeconds}s"
                } else {
                    "Disabled (GUI)"
                }
            }
            .addString("Salvage in Cargo Hold") { xpMessageCount.toString() }
            .build()
        addPaint(paint)
    }

    override fun poll() {
        try {
            val task = taskList.firstOrNull { it.activate() }

            if (task != null) {
                logger.info("POLL: Executing task: ${task::class.simpleName}. Current Phase: ${currentPhase.name}")
                task.execute()
            } else {
                logger.debug("POLL: No task currently active. Sleeping.")
                currentPhase = SalvagePhase.IDLE
                Condition.sleep(300)
            }
        } catch (e: Exception) {
            logger.error("CRASH in poll(): ${e.message}", e)
            Condition.sleep(1000)
        }
    }
}