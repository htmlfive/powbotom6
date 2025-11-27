package org.powbot.om6.salvagesorteraio

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.ScriptConfiguration.List as ConfigList
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType
import org.powbot.om6.salvagesorteraio.config.CardinalDirection
import org.powbot.om6.salvagesorteraio.config.SalvagePhase
import org.powbot.om6.salvagesorteraio.tasks.*
import org.powbot.api.rt4.Inventory
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvagesorteraio.config.LootConfig

private const val CARGO_MESSAGE = "Your crewmate on the salvaging hook cannot salvage as the cargo hold is full."
private const val HARVESTER_MESSAGE = "Your crystal extractor has harvested"
private const val COINS_ID = 995

@ScriptManifest(
    name = "0m6 Shipwreck Sorter",
    description = "Automates salvage sorting and salvaging via a two-state machine.",
    version = "2.0.0", // Updated version to reflect major change
    author = "0m6",
    category = ScriptCategory.Other
)
@ConfigList(
    [
        ScriptConfiguration(name = "Salvage Item Name", description = "The name of the raw salvage item.", defaultValue = "Fremennik salvage", visible = false),
        ScriptConfiguration(name = "Enable Extractor", description = "Toggle crystal extractor tapping.", defaultValue = "true"),
        ScriptConfiguration(name = "Extractor Interval", description = "Interval in milliseconds to check the extractor (e.g. 64000ms = 64s).", defaultValue = "64000", visible = false),
        // ADDED: Configuration for sorting direction
        ScriptConfiguration(name = "Sorting Direction", description = "The cardinal direction the camera must face for sorting (North, East, South, West).", defaultValue = "East")
    ]
)
class SalvageSorter : AbstractScript() {

    // Core state and variables
    var currentPhase = SalvagePhase.IDLE
    var SALVAGE_NAME = "Fremennik salvage" // Initial value overridden in onStart
    var enableExtractor = true
    var extractorInterval = 64000L
    var extractorTimer = 0L
    var harvesterMessageFound = false
    var currentCoinCount = 0
    var initialCoinCount = 0
    var xpMessageCount = 0
    var cargoMessageFound = false
    // ADDED: Variable for required sorting direction
    var requiredSortDirection: CardinalDirection = CardinalDirection.East

    // NEW task lists for the state machine
    private val salvageTasks = listOf(DeployHookTask(this), DepositCargoTask(this))
    private val sortingTasks = listOf(WithdrawCargoTask(this), SortSalvageTask(this), CleanupInventoryTask(this))

    override fun onStart() {
        // NOTE: If you want to use the config, you must change these manual settings
        // to use the framework's get<T> method. Example:
        // val directionStr = get<String>("Sorting Direction") ?: "East"
        // requiredSortDirection = CardinalDirection.valueOf(directionStr.capitalize())

        SALVAGE_NAME = "Opulent salvage"
        enableExtractor = true
        extractorInterval = 64000L
        initialCoinCount = Inventory.stream().id(COINS_ID).firstOrNull()?.stackSize() ?: 0
        currentCoinCount = initialCoinCount
        extractorTimer = System.currentTimeMillis()

        // Initializing with default 'East' as per the new config's default value.
        requiredSortDirection = CardinalDirection.East

        setupPaint()
        logger.info("Script started. Initial Coin Count: $initialCoinCount, Sorting Direction: ${requiredSortDirection.name}")
    }

    private fun setupPaint() {
        val paint = PaintBuilder.newBuilder()
            .addString("Phase") { currentPhase.name }
            .addString("Salvage in Cargo Hold Approx") { xpMessageCount.toString() }
            .build()
        addPaint(paint)
    }

    override fun poll() {
        try {
            // Highest Priority: Crystal Extractor Interrupt
            val extractorTask = CrystalExtractorTask(this)
            if (extractorTask.activate()) {
                extractorTask.execute()
                return
            }

            when (currentPhase) {
                SalvagePhase.IDLE -> {
                    // Determine initial state. Start by sorting to clear inventory first if needed.
                    val isInventoryFullOrHasSalvage = Inventory.stream().name(SALVAGE_NAME).isNotEmpty() || Inventory.emptySlotCount() < 10
                    currentPhase = if (isInventoryFullOrHasSalvage) {
                        SalvagePhase.SETUP_SORTING
                    } else {
                        SalvagePhase.SETUP_SALVAGING
                    }
                    logger.info("POLL: Initializing phase to ${currentPhase.name}.")
                }

                // --- SETUP PHASES (Run once, then transition to loop) ---
                SalvagePhase.SETUP_SALVAGING -> {
                    SetupSalvagingTask(this).execute()
                    currentPhase = SalvagePhase.SALVAGING
                    Condition.sleep(1000)
                }
                SalvagePhase.SETUP_SORTING -> {
                    SetupSortingTask(this).execute()
                    currentPhase = SalvagePhase.SORTING_LOOT
                    Condition.sleep(1000)
                }

                // --- MAIN LOOP PHASES ---
                SalvagePhase.SALVAGING -> {
                    // Loop: Deploy hook -> Deposit in cargo
                    val task = salvageTasks.firstOrNull { it.activate() }

                    if (task != null) {
                        logger.info("POLL: Executing SALVAGING task: ${task::class.simpleName}")
                        task.execute()
                    } else {
                        logger.debug("POLL: No salvaging task currently active (inventory empty of salvage). Sleeping.")
                        Condition.sleep(300)
                    }

                    // Transition to Sorting: Safety check for inventory full of raw salvage.
                    val hasFullSalvageInv = Inventory.stream().name(SALVAGE_NAME).count() >= 27
                    if (hasFullSalvageInv || cargoMessageFound) {
                        logger.info("POLL: Inventory full or cargo full message received. Transitioning to ${SalvagePhase.SETUP_SORTING.name}.")
                        cargoMessageFound = false // Reset flag
                        currentPhase = SalvagePhase.SETUP_SORTING
                    }
                }

                SalvagePhase.SORTING_LOOT -> {
                    // Loop: Withdraw cargo -> Sort Salvage -> Alch/Drop
                    val task = sortingTasks.firstOrNull { it.activate() }

                    if (task != null) {
                        logger.info("POLL: Executing SORTING task: ${task::class.simpleName}")
                        task.execute()
                    } else {
                        // Transition to Salvaging: If all sorting tasks are done (i.e., inventory is clean and there is no more cargo to withdraw).
                        val isInventoryClean = Inventory.stream().name(SALVAGE_NAME).isEmpty() &&
                                Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isEmpty()

                        if (isInventoryClean) {
                            logger.info("POLL: Inventory is clean. Transitioning to ${SalvagePhase.SETUP_SALVAGING.name}.")
                            currentPhase = SalvagePhase.SETUP_SALVAGING
                        } else {
                            logger.debug("POLL: No sorting task active, but inventory is not clean. Sleeping.")
                            Condition.sleep(300)
                        }
                    }
                }
            }

            if (!org.powbot.api.rt4.Game.loggedIn()) {
                ScriptManager.stop()
            }
        } catch (e: Exception) {
            logger.error("CRASH PROTECTION: An error occurred during poll: ${e.message}", e)
            Condition.sleep(1000)
        }
    }

    @Subscribe
    fun onMessageEvent(change: MessageEvent) {

        if (change.messageType == MessageType.Game) {
            when {
                change.message.contains(HARVESTER_MESSAGE) -> {
                    logger.info("EVENT: HARVESTER MESSAGE DETECTED! Message: ${change.message}")
                    harvesterMessageFound = true
                }
                change.message.contains(CARGO_MESSAGE) -> {
                    logger.info("EVENT: CARGO MESSAGE DETECTED! Message: ${change.message}")
                    cargoMessageFound = true
                }
            }
        }

        if (change.messageType == MessageType.Spam) {
            if (change.message.contains("You gain some experience by watching your crew work.")) {
                xpMessageCount++
                logger.debug("EVENT: XP message (SPAM) detected! Current count: $xpMessageCount")
            }
        }
    }
}