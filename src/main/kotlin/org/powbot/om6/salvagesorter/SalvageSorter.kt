package org.powbot.om6.salvagesorter

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorter.config.CardinalDirection
import org.powbot.om6.salvagesorter.config.Constants
import org.powbot.om6.salvagesorter.config.SalvagePhase
import org.powbot.om6.salvagesorter.tasks.*
import kotlin.random.Random
import org.powbot.api.script.ScriptConfiguration.List as ConfigList

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
            "Power Salvage Mode",
            "If true, skips sorting entirely and simply drops all salvage when inventory is full. Useful for Raft/Skiff salvaging below level 50 without Salvaging station.",
            optionType = OptionType.BOOLEAN, defaultValue = "false"
        ),
        ScriptConfiguration(
            "Enable Extractor",
            "If true, enables the automatic tapping of the Crystal Extractor every ~64 seconds.",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Salvage Item Name",
            "The exact name of the item dropped after salvaging the shipwreck.",
            optionType = OptionType.STRING,
            defaultValue = "Opulent salvage",
            allowedValues = ["Small salvage", "Fishy salvage", "Barracuda salvage", "Large salvage", "Plundered salvage", "Martial salvage", "Fremennik salvage", "Opulent salvage"]
        ),
        ScriptConfiguration(
            "Start Sorting",
            "If true, starts sorting instead of salvaging.",
            optionType = OptionType.BOOLEAN, defaultValue = "false"
        ),
        ScriptConfiguration(
            "Tap-to-drop",
            "If true, enabled tap-to-drop before starting",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Max Cargo Space",
            "The maximum cargo space you can hold.",
            optionType = OptionType.STRING,
            defaultValue = "210"
        ),
        ScriptConfiguration(
            "Camera Direction",
            "The camera direction required for fixed-screen tap locations during salvaging, sorting, and extractor tapping. Req. Camera Vertical Setting in OSRS Settings. Set zoom to max all the way in",
            optionType = OptionType.STRING, defaultValue = "North",
            allowedValues = ["North", "East", "South", "West"]
        )
    ]
)
class SalvageSorter : AbstractScript() {
    val maxCargoSpace: String get() = getOption<String>("Max Cargo Space")
    val powerSalvageMode: Boolean get() = getOption<Boolean>("Power Salvage Mode")
    var salvageMessageFound = false
    var atHookLocation = false
    var atSortLocation = false
    val tapToDrop: Boolean get() = getOption<Boolean>("Tap-to-drop")
    val SALVAGE_NAME: String get() = getOption<String>("Salvage Item Name")
    var hookingSalvageBool = false
    val startSorting: Boolean get() = getOption<Boolean>("Start Sorting")

    val enableExtractor: Boolean get() = getOption<Boolean>("Enable Extractor")
    val extractorInterval: Long = 64000L
    val cameraDirectionStr: String get() = getOption<String>("Camera Direction")
    val cameraDirection: CardinalDirection get() = CardinalDirection.valueOf(cameraDirectionStr)

    @Volatile var cargoMessageFound: Boolean = false
    @Volatile var harvesterMessageFound: Boolean = false
    @Volatile var hookCastMessageFound = false
    @Volatile var extractorTimer: Long = 0L
    @Volatile var currentPhase: SalvagePhase = SalvagePhase.IDLE
    @Volatile var phaseStartTime: Long = 0L
    @Volatile var cargoHoldCount: Int = 0
    @Volatile var initialCoinCount: Long = 0L
    @Volatile var cargoHoldFull: Boolean = true

    private val currentCoinCount: Long
        get() {
            val invCoins = Inventory.stream().id(Constants.COINS_ID).firstOrNull()?.stackSize() ?: 0
            return invCoins.toLong()
        }

    private val allTasks: kotlin.collections.List<Task> by lazy {
        logger.info("INIT: Comprehensive Task list initialized.")
        listOf(
            // HIGHEST PRIORITY
            SetZoomTask(this),
            CrystalExtractorTask(this),
            CleanupInventoryTask(this),

            // POWER SALVAGE MODE TASK (High priority when enabled)
            DropSalvageTask(this),

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
            if (change.message.contains(Constants.HARVESTER_MESSAGE)) {
                logger.info("EVENT: HARVESTER MESSAGE DETECTED! Message: ${change.message}")
                harvesterMessageFound = true
            }
        }

        if (change.message.contains(Constants.HOOK_CAST_MESSAGE_1) || change.message.contains(Constants.HOOK_CAST_MESSAGE_2)) {
            logger.info("EVENT: CONFIRMATION - Action start message detected: ${change.message}")
            hookCastMessageFound = true
        }

        if (change.messageType == MessageType.Game) {
            if (change.message.contains(Constants.CARGO_MESSAGE)) {
                logger.info("EVENT: CARGO MESSAGE DETECTED! Message: ${change.message}")
                cargoMessageFound = true
                cargoHoldFull = true
            }
        }
        if (change.messageType == MessageType.Game) {
            if (change.message.contains(Constants.SALVAGE_COMPLETE_MESSAGE) || change.message.contains(Constants.SALVAGE_SUCCESS_MESSAGE)) {
                logger.info("EVENT: Salvage ending message detected via EventBus! Message: ${change.message}")
                salvageMessageFound = true
            }
        }
        if (change.messageType == MessageType.Spam) {
            if (change.message.contains("You gain some experience by watching your crew work.")) {
                cargoHoldCount++
                logger.debug("EVENT: XP message (SPAM) detected! Current count: $cargoHoldCount")
            }
        }
    }

    override fun onStart() {
        logger.info("SCRIPT START: Initializing Shipwreck Sorter...")
        Condition.sleep(Random.nextInt(600, 1200))
        // Set zoom to target level if needed
        if (Camera.zoom != Constants.TARGET_ZOOM_LEVEL) {
            logger.info("INIT: Setting camera zoom to ${Constants.TARGET_ZOOM_LEVEL}")
            Camera.moveZoomSlider(Constants.TARGET_ZOOM_LEVEL.toDouble())
            Condition.wait({ Camera.zoom == Constants.TARGET_ZOOM_LEVEL }, 100, 20)
            Condition.sleep(Random.nextInt(600, 1200))
        }

        initialCoinCount = currentCoinCount
        logger.info("INIT: Tracking coins. Initial total GP (Inventory Only): $initialCoinCount")
        extractorTimer = 5L
        phaseStartTime = System.currentTimeMillis()
        currentPhase = SalvagePhase.IDLE

        // Check if the user wants to start in SORTING mode (or if inventory is full)
        val startInSortingMode = startSorting

        // Power Salvage Mode: Always start in salvaging mode
        if (powerSalvageMode) {
            this.logger.info("INIT: POWER SALVAGE MODE enabled - Starting in SALVAGING mode only.")
            this.cargoHoldFull = false
            this.currentPhase = SalvagePhase.SETUP_SALVAGING
        } else if (startInSortingMode) {
            this.logger.info("INIT: Forcing initial phase to SETUP_SORTING (User request).")
            this.cargoHoldFull = true
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
            .addString("Salvage in Cargo (Approx)") { cargoHoldCount.toString() }
            .build()
        addPaint(paint)
    }

    override fun poll() {
        try {
            logger.info("POLL START: phase=$currentPhase, cargoHoldFull=$cargoHoldFull, atSortLocation=$atSortLocation, atHookLocation=$atHookLocation")

            // --- 1. HIGHEST PRIORITY INTERRUPTS ---

            // Zoom Check (Highest priority - ensure camera is set correctly)
            val zoomTask = allTasks.firstOrNull { it is SetZoomTask && it.activate() }
            if (zoomTask != null) {
                logger.info("POLL: Executing INTERRUPT: Set Zoom")
                zoomTask.execute()
                return
            }

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

            // --- POWER SALVAGE MODE CHECK ---
            if (powerSalvageMode) {
                val dropTask = allTasks.firstOrNull { it is DropSalvageTask && it.activate() }
                if (dropTask != null) {
                    logger.info("POLL: POWER SALVAGE MODE - Dropping salvage")
                    dropTask.execute()
                    return
                }

                val setupTask = allTasks.firstOrNull { it is SetupSalvagingTask && it.activate() }
                if (setupTask != null) {
                    logger.info("POLL: POWER SALVAGE MODE - Setup salvaging")
                    setupTask.execute()
                    return
                }

                val deployTask = allTasks.firstOrNull { it is DeployHookTask }
                if (deployTask != null && deployTask.activate()) {
                    logger.info("POLL: POWER SALVAGE MODE - Deploying hook")
                    deployTask.execute()
                    return
                } else {
                    if (currentPhase != SalvagePhase.SALVAGING && currentPhase != SalvagePhase.SETUP_SALVAGING) {
                        currentPhase = SalvagePhase.SETUP_SALVAGING
                        logger.info("POLL: POWER SALVAGE MODE - Reset to SETUP_SALVAGING")
                    }
                    Condition.sleep(300)
                    return
                }
            }

            // --- 2. DETERMINE CURRENT STATE AND SELECT TASK ---

            val hasSalvageInInventory = Inventory.stream().name(SALVAGE_NAME).isNotEmpty()
            val inventoryFull = Inventory.isFull()

            logger.info("POLL: State Check - cargoFull=$cargoHoldFull, hasSalvage=$hasSalvageInInventory, invFull=$inventoryFull, phase=$currentPhase")

            val nextTask: Task? = when {
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
            if (!Game.loggedIn()) {
                logger.error("POLL: Logged out. Stopping script.")
                ScriptManager.stop()
            }

        } catch (e: Exception) {
            logger.error("CRASH in poll(): ${e.message}", e)
            Condition.sleep(1000)
        }
    }
}