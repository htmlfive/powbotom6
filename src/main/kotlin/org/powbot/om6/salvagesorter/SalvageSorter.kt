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
import kotlin.random.Random
import org.powbot.api.rt4.Inventory
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvager.ShipwreckSalvager.Companion.HOOK_CAST_MESSAGE_1
import org.powbot.om6.salvager.ShipwreckSalvager.Companion.HOOK_CAST_MESSAGE_2

private const val CARGO_MESSAGE = "Your crewmate on the salvaging hook cannot salvage as the cargo hold is full."
private const val HARVESTER_MESSAGE = "Your crystal extractor has harvested"
private const val COINS_ID = 995
private const val SALVAGE_COMPLETE_MESSAGE = "You salvage all you can"
private const val SALVAGE_SUCCESS_MESSAGE = "You cast out" // Assumed definition based on context

@ScriptManifest(
    name = "0m6 Shipwreck Sorter",
    description = "Automates salvage sorting (High Prio), loot cleanup (Med Prio), cargo withdrawal (Low Prio), and crystal extractor taps (Highest Prio).",
    version = "1.3.0",
    author = "0m6",
    category = ScriptCategory.Other
)
@ConfigList(
    [
        ScriptConfiguration(
            "Salvage Item Name",
            "The exact name of the item dropped after salvaging the shipwreck.",
            optionType = OptionType.STRING,
            defaultValue = "Opulent salvage",
            allowedValues = ["Small salvage", "Fishy salvage", "Barracuda salvage", "Large salvage", "Plundered salvage", "Martial salvage", "Fremennik salvage", "Opulent salvage"]
        ),
        ScriptConfiguration(
            "Min Withdraw Cooldown (s)",
            "The minimum random time (in seconds) the script waits after cleanup/withdraw before trying again.",
            optionType = OptionType.STRING,
            defaultValue = "1"
        ),
        ScriptConfiguration(
            "Max Withdraw Cooldown (s)",
            "The maximum random time (in seconds) the script waits after cleanup/withdraw before trying again.",
            optionType = OptionType.STRING,
            defaultValue = "1"
        ),
        ScriptConfiguration(
            "Enable Extractor",
            "If true, enables the automatic tapping of the Crystal Extractor every ~64 seconds.",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Extractor Tap Direction",
            "The camera direction required for fixed-screen tap locations during the extractor tap.",
            optionType = OptionType.STRING, defaultValue = "North",
            allowedValues = ["North", "East", "South", "West"]
        ),
        ScriptConfiguration(
            "Drop Salvage Direction",
            "The camera direction required for fixed-screen tap locations during the sort phase. Req. Camera Vertical Setting in OSRS Settings. Set zoom to max all the way in",
            optionType = OptionType.STRING, defaultValue = "North",
            allowedValues = ["North", "East", "South", "West"]
        )
    ]
)
class SalvageSorter : AbstractScript() {
    var salvageMessageFound = false
    var atHookLocation = false // ADDED: New flag to track if the player is at the salvaging spot
    var atSortLocation = false // NEW FLAG: Track if at sorting spot
    val extractorTask = CrystalExtractorTask(this)
    val requiredDropDirectionStr: String get() = getOption<String>("Drop Salvage Direction")
    val SALVAGE_NAME: String get() = getOption<String>("Salvage Item Name")
    val requiredDropDirection: CardinalDirection get() = CardinalDirection.valueOf(requiredDropDirectionStr)
    var hookingSalvageBool = false
    val enableExtractor: Boolean get() = getOption<Boolean>("Enable Extractor")
    val extractorInterval: Long = 64000L
    val requiredTapDirectionStr: String get() = getOption<String>("Extractor Tap Direction")
    val requiredTapDirection: CardinalDirection get() = CardinalDirection.valueOf(requiredTapDirectionStr)

    private val MIN_COOLDOWN_SECONDS: Int get() = getOption<String>("Min Withdraw Cooldown (s)").toInt()
    private val MAX_COOLDOWN_SECONDS: Int get() = getOption<String>("Max Withdraw Cooldown (s)").toInt()
    @Volatile var cargoMessageFound: Boolean = false
    @Volatile var harvesterMessageFound: Boolean = false
    @Volatile var hookCastMessageFound = false
    @Volatile var extractorTimer: Long = 0L
    @Volatile var currentPhase: SalvagePhase = SalvagePhase.IDLE
    @Volatile var phaseStartTime: Long = 0L
    @Volatile var xpMessageCount: Int = 0
    @Volatile var initialCoinCount: Long = 0L
    @Volatile var cargoHoldFull: Boolean = true // Core state flag: true = SORTING phase, false = SALVAGING phase

    private val currentCoinCount: Long
        get() {
            val invCoins = Inventory.stream().id(COINS_ID).firstOrNull()?.stackSize() ?: 0
            return invCoins.toLong()
        }

    val randomWithdrawCooldownMs: Long
        get() {
            val min = MIN_COOLDOWN_SECONDS.toLong()
            val max = MAX_COOLDOWN_SECONDS.toLong()
            return Random.nextLong(min, max + 1) * 1000L
        }

    @Volatile var currentWithdrawCooldownMs: Long = 0L
    @Volatile var lastWithdrawOrCleanupTime: Long = 0L

    private val allTasks: kotlin.collections.List<Task> by lazy {
        logger.info("INIT: Comprehensive Task list initialized.")
        listOf(
            // HIGHEST PRIORITY
            CrystalExtractorTask(this),
            CleanupInventoryTask(this),

            // CORE SALVAGING TASKS
            DepositCargoTask(this),
            DeployHookTask(this),

            // CORE SORTING TASK
            SortSalvageTask(this),

            // SETUP / TRANSITION TASKS
            SetupSalvagingTask(this),
            SetupSortingTask(this),

            // LOW PRIORITY UTILITY
            WithdrawCargoTask(this)
        )
    }

    @Subscribe
    fun onMessageEvent(change: MessageEvent) {
        if (change.messageType == MessageType.Game) {
            if (change.message.contains(HARVESTER_MESSAGE)) {
                logger.info("EVENT: HARVESTER MESSAGE DETECTED! Message: ${change.message}")
                harvesterMessageFound = true
            }
        }

        if (change.message.contains(HOOK_CAST_MESSAGE_1) || change.message.contains(HOOK_CAST_MESSAGE_2)) {
            logger.info("EVENT: CONFIRMATION - Action start message detected: ${change.message}")
            hookCastMessageFound = true
        }

        if (change.messageType == MessageType.Game) {
            if (change.message.contains(CARGO_MESSAGE)) {
                logger.info("EVENT: CARGO MESSAGE DETECTED! Message: ${change.message}")
                cargoMessageFound = true
                cargoHoldFull = true
            }
        }
        if (change.messageType == MessageType.Game) {
            if (change.message.contains(SALVAGE_COMPLETE_MESSAGE) || change.message.contains(SALVAGE_SUCCESS_MESSAGE)) {
                logger.info("EVENT: Salvage SUCCESS message detected via EventBus! Message: ${change.message}")
                salvageMessageFound = true
            }
        }
        if (change.messageType == MessageType.Spam) {
            if (change.message.contains("You gain some experience by watching your crew work.")) {
                xpMessageCount++
                logger.debug("EVENT: XP message (SPAM) detected! Current count: $xpMessageCount")
            }
        }
    }

    override fun onStart() {
        logger.info("SCRIPT START: Initializing Shipwreck Sorter...")
        initialCoinCount = currentCoinCount
        logger.info("INIT: Tracking coins. Initial total GP (Inventory Only): $initialCoinCount")
        extractorTimer = 0L
        phaseStartTime = System.currentTimeMillis()
        currentPhase = SalvagePhase.IDLE

// Check if the user wants to start in SORTING mode (or if inventory is full)
        val startInSortingMode = true // <-- You can make this a configuration option later

        if (startInSortingMode) {
            this.logger.info("INIT: Forcing initial phase to SETUP_SORTING (User request).")
            // 1. Force cargoHoldFull to TRUE to signal the script to enter the SORTING loop
            this.cargoHoldFull = true
            // 2. Set the initial phase to start the sorting flow
            this.currentPhase = SalvagePhase.SETUP_SORTING
        } else {
            this.logger.info("INIT: Starting in default SALVAGING mode.")
            this.cargoHoldFull = false
            this.currentPhase = SalvagePhase.SETUP_SALVAGING
        }

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Status") { currentPhase.name }
            .addString("Mode") { if (cargoHoldFull) "SORTING" else "SALVAGING" }
            .addString("Extractor Tap") {
                if (enableExtractor) {
                    val timeElapsed = System.currentTimeMillis() - extractorTimer
                    val remainingSeconds = ((extractorInterval - timeElapsed) / 1000L).coerceAtLeast(0)
                    "Next in: ${remainingSeconds}s"
                } else {
                    "Disabled (GUI)"
                }
            }
            .addString("Coins Gained") {
                val gain = currentCoinCount - initialCoinCount
                String.format("%,d GP", gain)
            }
            .addString("Withdraw Cooldown") {
                val maxCooldown = currentWithdrawCooldownMs
                val timeElapsed = System.currentTimeMillis() - lastWithdrawOrCleanupTime
                val remainingSeconds = ((maxCooldown - timeElapsed) / 1000L).coerceAtLeast(0)

                if (remainingSeconds > 0) "Next in: ${remainingSeconds}s" else "Ready"
            }
            .addString("Salvage in Cargo (Approx)") { xpMessageCount.toString() }
            .build()
        addPaint(paint)

//        if (assignBoth(this)) {
//            logger.info("Initial assignments successful.")
//        } else {
//            logger.error("Initial assignments FAILED. Check log for tap errors.")
//        }
    }

    override fun poll() {
        try {
            // --- 1. HIGHEST PRIORITY INTERRUPTS ---

            // Extractor Check (Always highest priority)
            val extractorTask = allTasks.firstOrNull { it is CrystalExtractorTask && it.activate() }
            if (extractorTask != null) {
                logger.info("POLL: Executing INTERRUPT: Crystal Extractor")
                extractorTask.execute()
                return
            }

            // Cleanup Check (High priority - remove junk before any core action)
            val cleanupTask = allTasks.firstOrNull { it is CleanupInventoryTask && it.activate() }
            if (cleanupTask != null) {
                logger.info("POLL: Executing INTERRUPT: Cleanup Inventory")
                cleanupTask.execute()
                return
            }

            // --- 2. DETERMINE CURRENT STATE AND SELECT TASK ---

            val hasSalvageInInventory = Inventory.stream().name(SALVAGE_NAME).isNotEmpty()
            val inventoryFull = Inventory.isFull()

            logger.debug("POLL: State Check - cargoFull=$cargoHoldFull, hasSalvage=$hasSalvageInInventory, invFull=$inventoryFull, phase=$currentPhase")

            val nextTask: Task? = when {
                // Replace your poll() STATE A (SORTING PHASE) section with this:

// STATE A: SORTING PHASE (Cargo Hold is Full OR in process of emptying)
                cargoHoldFull -> {
                    logger.debug("STATE: SORTING PHASE (cargoHoldFull=true)")

                    when {
                        // Priority 1: Setup sorting FIRST if not at sort location
                        !atSortLocation -> {
                            logger.debug("STATE: Not at sort location, need setup")
                            currentPhase = SalvagePhase.SETUP_SORTING
                            allTasks.firstOrNull { it is SetupSortingTask }
                        }

                        // Priority 2: Sort any salvage currently in inventory
                        hasSalvageInInventory -> {
                            logger.debug("STATE: At sort location with salvage, sorting")
                            currentPhase = SalvagePhase.SORTING_LOOT
                            allTasks.firstOrNull { it is SortSalvageTask }
                        }

                        // Priority 3: Withdraw more salvage from cargo to continue sorting
                        else -> {
                            logger.debug("STATE: At sort location, no salvage, withdrawing")
                            currentPhase = SalvagePhase.WITHDRAWING
                            allTasks.firstOrNull { it is WithdrawCargoTask }
                        }
                    }
                }

// STATE B: SALVAGING PHASE (Cargo Hold is Not Full - ready to salvage)
                else -> {
                    logger.debug("STATE: SALVAGING PHASE (cargoHoldFull=false)")

                    when {
                        // Priority 1: If we're at sort location but cargo is empty, need to transition to salvaging
                        atSortLocation -> {
                            logger.debug("STATE: At sort location but cargo empty, transitioning to SETUP_SALVAGING")
                            currentPhase = SalvagePhase.SETUP_SALVAGING
                            allTasks.firstOrNull { it is SetupSalvagingTask }
                        }

                        // Priority 2: Deposit if inventory is full
                        inventoryFull -> {
                            logger.debug("STATE: Inventory full, selecting DepositCargoTask")
                            currentPhase = SalvagePhase.DEPOSITING
                            allTasks.firstOrNull { it is DepositCargoTask }
                        }

                        // Priority 3: Setup camera/position if needed before deploying hook
                        else -> {
                            val setupTask = allTasks.firstOrNull { it is SetupSalvagingTask && it.activate() }
                            if (setupTask != null) {
                                logger.debug("STATE: Setup needed, selecting SetupSalvagingTask")
                                setupTask
                            } else {
                                // Priority 4: Deploy hook and salvage
                                logger.debug("STATE: Ready to salvage, selecting DeployHookTask")
                                currentPhase = SalvagePhase.SALVAGING
                                allTasks.firstOrNull { it is DeployHookTask }
                            }
                        }
                    }

                }
            }

            // --- 3. EXECUTE SELECTED TASK ---

            if (nextTask != null) {
                val canActivate = nextTask.activate()
                logger.debug("POLL: Selected ${nextTask::class.simpleName}, canActivate=$canActivate")

                if (canActivate) {
                    logger.info("POLL: Executing ${nextTask::class.simpleName}. Phase: ${currentPhase.name}, Mode: ${if (cargoHoldFull) "SORTING" else "SALVAGING"}")
                    nextTask.execute()
                } else {
                    logger.warn("POLL: Task ${nextTask::class.simpleName} selected but activation failed. Phase=$currentPhase, Mode=${if (cargoHoldFull) "SORTING" else "SALVAGING"}")
                    currentPhase = SalvagePhase.IDLE
                    Condition.sleep(300)
                }
            } else {
                logger.warn("POLL: No task selected. Phase=$currentPhase, cargoFull=$cargoHoldFull, hasSalvage=$hasSalvageInInventory")
                currentPhase = SalvagePhase.IDLE
                Condition.sleep(300)
            }

            // --- 4. SAFETY CHECKS ---

            // Safety: Force deposit if somehow stuck in IDLE with full inventory
            if (currentPhase == SalvagePhase.IDLE && Inventory.isFull() && !cargoHoldFull) {
                logger.error("SAFETY: IDLE with full inventory in SALVAGING mode. Forcing deposit.")
                currentPhase = SalvagePhase.DEPOSITING
                val forceDeposit = allTasks.firstOrNull { it is DepositCargoTask }
                forceDeposit?.execute()
            }

            // Safety: Stop if logged out
            if (!org.powbot.api.rt4.Game.loggedIn()) {
                logger.error("POLL: Logged out. Stopping script.")
                ScriptManager.stop()
            }

        } catch (e: Exception) {
            logger.error("CRASH in poll(): ${e.message}", e)
            Condition.sleep(1000)
        }
    }
}