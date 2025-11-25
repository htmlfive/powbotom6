package org.powbot.om6.salvagesorter

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Input
import org.powbot.api.rt4.*
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.ScriptConfiguration.List as ConfigList
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType

enum class SalvagePhase {
    IDLE,
    CLEANING,
    SORTING,
    WITHDRAWING
}

enum class CardinalDirection(val yaw: Int, val action: String) {
    North(0, "Look North"),
    South(180, "Look South"),
    East(270, "Look East"),
    West(90, "Look West")
}

abstract class Task(protected val script: SalvageSorter) {
    abstract fun activate(): Boolean
    abstract fun execute()
}

object CameraSnapper {
    private const val COMPASS_WIDGET_ID = 601
    private const val COMPASS_COMPONENT_INDEX = 35
    private const val PITCH_MIN = 35
    private const val PITCH_MAX = 41

    fun snapCameraToDirection(direction: CardinalDirection, script: SalvageSorter) {
        val yawNeeded = direction.yaw
        val COMPASS_ACTION = direction.action
        script.logger.info("LOGIC: Attempting to snap camera to ${direction.name} (Yaw: $yawNeeded). Current Yaw: ${Camera.yaw()}, Current Pitch: ${Camera.pitch()}")

        val isPitchIncorrect = Camera.pitch() !in PITCH_MIN..PITCH_MAX

        if (Camera.yaw() != yawNeeded || isPitchIncorrect) {
            script.logger.info("CHECK: Yaw is ${Camera.yaw()}, does not match target $COMPASS_ACTION ($yawNeeded).")

            val compassComponent = Widgets.widget(COMPASS_WIDGET_ID).component(COMPASS_COMPONENT_INDEX)
            script.logger.info("WIDGET: Checking compass component $COMPASS_WIDGET_ID:$COMPASS_COMPONENT_INDEX (Valid: ${compassComponent.valid()}).")

            if (compassComponent.valid() && compassComponent.click(COMPASS_ACTION)) {
                script.logger.info("ACTION: Successfully clicked compass with action '$COMPASS_ACTION'. Waiting for yaw stabilization.")
                val snapSuccess = Condition.wait({ Camera.yaw() == yawNeeded }, 100, 10)
                if (snapSuccess) {
                    script.logger.info("SUCCESS: Camera yaw stabilized at $yawNeeded.")
                } else {
                    script.logger.warn("FAIL: Camera yaw failed to stabilize at $yawNeeded after clicking compass.")
                }
                val sleepTime = Random.nextInt(600, 1200)
                script.logger.info("SLEEP: Sleeping for $sleepTime ms after snap attempt.")
                Condition.sleep(sleepTime)
            } else {
                script.logger.warn("FAIL: Failed to find or click compass component $COMPASS_WIDGET_ID:$COMPASS_COMPONENT_INDEX for action '$COMPASS_ACTION'.")
            }
        } else {
            script.logger.info("CHECK: Already facing $COMPASS_ACTION ($yawNeeded). No snap action needed.")
        }
    }
}

object LootConfig {
    val DROP_LIST = arrayOf(
        "Raw lobster", "Raw tuna", "Raw monkfish", "Raw salmon",
        "Mithril ore", "Arctic pine logs", "Ensouled troll head", "Mahogany plank"
    )

    val ALCH_LIST = arrayOf(
        "Fremennik helm", "Berserker helm", "Archer helm", "Farseer helm", "Warrior helm"
    )

    val DISCARD_OR_ALCH_LIST = DROP_LIST + ALCH_LIST
}

class CrystalExtractorTask(script: SalvageSorter) : Task(script) {

    override fun activate(): Boolean {
        if (!script.enableExtractor) return false

        if (script.harvesterMessageFound) {
            script.logger.debug("ACTIVATE: Active due to Harvester message override.")
            return true
        }

        val currentTime = System.currentTimeMillis()
        val timerExpired = currentTime - script.extractorTimer >= script.extractorInterval

        if (timerExpired) {
            script.logger.debug("ACTIVATE: Active due to ${script.extractorInterval / 1000}-second timer expiration.")
            return true
        }

        return false
    }

    override fun execute() {
        val isOverride = script.harvesterMessageFound

        script.logger.info("ACTION: Starting Extractor Tap sequence (Override: $isOverride).")

        if (executeExtractorTap()) {
            script.harvesterMessageFound = false
            script.extractorTimer = System.currentTimeMillis()
            script.logger.info("SUCCESS: Extractor tap complete. Timer reset.")
        } else {
            script.logger.warn("FAIL: Extractor tap failed. Timer and message flag NOT reset. Will retry immediately if still active.")
            Condition.sleep(Random.nextInt(500, 1000))
        }
    }

    fun executeExtractorTap(): Boolean {
        try {
            CameraSnapper.snapCameraToDirection(script.requiredTapDirection, script)

            val x = 289
            val y = 299
            val randomOffsetX = Random.nextInt(-3, 3)
            val randomOffsetY = Random.nextInt(-3, 3)
            val finalX = x + randomOffsetX
            val finalY = y + randomOffsetY
            Condition.sleep(Random.nextInt(600, 1200))

            script.hookCastMessageFound = false
            script.harvesterMessageFound = false

            script.logger.info("ACTION: Executing extractor tap at X=$finalX, Y=$finalY (Offset: $randomOffsetX, $randomOffsetY).")
            val clicked = Input.tap(finalX, finalY)

            if (clicked) {
                val tapSleep = Random.nextInt(150, 250)
                Condition.sleep(tapSleep)

                val waitTime = Random.nextInt(2400, 3000)
                script.logger.info("WAIT: Extractor tap successful. Waiting $waitTime ms.")
                Condition.sleep(waitTime)
                return true
            }
            script.logger.warn("FAIL: Failed to execute Input.tap() at ($finalX, $finalY).")
            return false
        } catch (e: Exception) {
            script.logger.error("CRASH PROTECTION: Extractor tap sequence failed with exception: ${e.message}", e)
            return false
        }
    }

    fun checkAndExecuteInterrupt(script: SalvageSorter): Boolean {
        if (this.activate()) {
            script.logger.info("INTERRUPT: Crystal Extractor Tap is ACTIVATED during task flow.")
            this.execute()
            return true
        }
        return false
    }
}

private fun executeCleanupLoot(script: SalvageSorter): Boolean {
    var successfullyCleaned = false

    val highAlchSpell = Magic.Spell.HIGH_ALCHEMY
    script.logger.info("CLEANUP: Starting alching loop (High Priority Cleanup).")

    LootConfig.ALCH_LIST.forEach { itemName ->
        val item = Inventory.stream().name(itemName).firstOrNull()
        if (item != null && item.valid()) {
            script.logger.info("CLEANUP: Casting High Alch on $itemName.")
            successfullyCleaned = true

            if (highAlchSpell.cast()) {
                if (item.click()) {
                    script.logger.info("CLEANUP: Alch successful. Sleeping for animation/cooldown: 3000-3600ms.")
                    Condition.sleep(Random.nextInt(3000, 3600))
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

    if (!Inventory.opened()) {
        if (Inventory.open()) {
            script.logger.info("Inventory tab opened successfully for dropping.")
            Condition.sleep(Random.nextInt(200, 400))
        } else {
            script.logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
        }
    } else {
        script.logger.info("Inventory tab is already open.")
    }
    script.logger.info("CLEANUP: Starting item drop process for non-alchable items (Low Priority Cleanup).")
    LootConfig.DROP_LIST.forEach { itemName ->
        Inventory.stream().name(itemName).forEach { item ->
            script.logger.info("CLEANUP: Attempting to drop $itemName.")
            successfullyCleaned = true
            if (item.click()) {
                Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 300, 5)
            }
        }
    }

    if (successfullyCleaned) {
        Condition.sleep(Random.nextInt(800, 1500))
    }

    return successfullyCleaned
}

private fun executeWithdrawCargo(script: SalvageSorter): Boolean {
    fun getRandomOffsetLarge() = Random.nextInt(-3, 3)
    val mainWait = Random.nextInt(900, 1200)

    script.logger.info("CARGO: Starting 4-tap cargo withdrawal sequence.")

    val x1 = 364 + getRandomOffsetLarge()
    val y1 = 144 + getRandomOffsetLarge()
    if (!Input.tap(x1, y1)) return false
    script.logger.info("CARGO TAP 1 (Open): Tapped at ($x1, $y1). Waiting for interaction.")
    Condition.sleep(mainWait)
    Condition.sleep(Random.nextInt(1800, 2400))

    val x2 = 143 + getRandomOffsetLarge()
    val y2 = 237 + getRandomOffsetLarge()
    if (!Input.tap(x2, y2)) return false
    script.logger.info("CARGO TAP 2 (Withdraw): Tapped at ($x2, $y2). Waiting $mainWait ms.")
    Condition.sleep(mainWait)

    val x3 = 571 + getRandomOffsetLarge()
    val y3 = 159 + getRandomOffsetLarge()
    if (!Input.tap(x3, y3)) return false
    script.logger.info("CARGO TAP 3 (Close): Tapped at ($x3, $y3). Waiting $mainWait ms.")
    Condition.sleep(mainWait)

    val x4 = 567 + getRandomOffsetLarge()
    val y4 = 460 + getRandomOffsetLarge()
    if (!Input.tap(x4, y4)) return false
    script.logger.info("CARGO TAP 4 (Walk back): Tapped at ($x4, $y4). Waiting for walk back.")
    Condition.sleep(Random.nextInt(1800, 2400))

    return true
}

private fun executeTapSortSalvage(script: SalvageSorter, salvageItemName: String): Boolean {
    val SORT_BUTTON_X = 587
    val SORT_BUTTON_Y = 285
    val SORT_BUTTON_TOLERANCE = 3

    CameraSnapper.snapCameraToDirection(script.requiredDropDirection, script)
    Condition.sleep(Random.nextInt(500, 800))

    val randomOffsetX = Random.nextInt(-SORT_BUTTON_TOLERANCE, SORT_BUTTON_TOLERANCE + 1)
    val randomOffsetY = Random.nextInt(-SORT_BUTTON_TOLERANCE, SORT_BUTTON_TOLERANCE + 1)
    val finalX = SORT_BUTTON_X + randomOffsetX
    val finalY = SORT_BUTTON_Y + randomOffsetY

    val salvageCountBefore = Inventory.stream().name(salvageItemName).count()

    script.logger.info("ACTION: Tapping 'Sort Salvage' button at X=$finalX, Y=$finalY. Count before: $salvageCountBefore.")

    if (Input.tap(finalX, finalY)) {
        Condition.sleep(Random.nextInt(1500, 2500))
        val waitSuccess = Condition.wait({ Inventory.stream().name(salvageItemName).isEmpty() }, 300, 10)

        if (waitSuccess) {
            script.logger.info("SUCCESS: 'Sort Salvage' complete. Extended AFK wait: 5000-8000ms.")
            val sleepBetween = 5
            var count = 0

            while (count < sleepBetween) {
                val sleepTime = Random.nextInt(1000, 2000)
                Condition.sleep(sleepTime)
                script.logger.debug("DIALOGUE: Sleeping $sleepTime ms before next continue click (Count: ${count + 1}).")
                count++
            }
            if (Chat.canContinue()) {
                Chat.clickContinue()
                Condition.sleep(Random.nextInt(1500, 2500))
            }
            return true
        } else {
            script.logger.warn("FAIL: 'Sort Salvage' wait timed out. Salvage still present.")
            return false
        }
    }
    return false
}

class SortSalvageTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        return hasSalvage
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.SORTING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 2).")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeTapSortSalvage(script, script.SALVAGE_NAME)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        script.currentPhase = if (success) SalvagePhase.IDLE else SalvagePhase.SORTING
        script.logger.info("PHASE: Sort complete/failed. Transitioned to ${script.currentPhase.name}.")
    }
}

class CleanupInventoryTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasSalvage = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty()
        if (hasSalvage) return false

        val hasCleanupLoot = Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()
        return hasCleanupLoot
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.CLEANING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 3).")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeCleanupLoot(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        script.currentPhase = if (success) SalvagePhase.IDLE else SalvagePhase.CLEANING
        script.logger.info("PHASE: Cleanup complete/failed. Transitioned to ${script.currentPhase.name}.")
    }
}

class WithdrawCargoTask(script: SalvageSorter) : Task(script) {
    private val extractorTask = CrystalExtractorTask(script)

    override fun activate(): Boolean {
        val hasJunk = Inventory.stream().name(script.SALVAGE_NAME).isNotEmpty() ||
                Inventory.stream().name(*LootConfig.DISCARD_OR_ALCH_LIST).isNotEmpty()

        if (hasJunk) return false

        if (Inventory.isFull()) return false

        val emptySlots = Inventory.emptySlotCount()

        val hasSufficientCargo = script.xpMessageCount >= emptySlots

        script.logger.debug("WITHDRAW CHECK: Clean=$!hasJunk, EmptySlots=$emptySlots, CargoProxy=${script.xpMessageCount}, SufficientCargo=$hasSufficientCargo")

        return hasSufficientCargo
    }

    override fun execute() {
        script.currentPhase = SalvagePhase.WITHDRAWING
        script.logger.info("PHASE: Transitioned to ${script.currentPhase.name} (Priority 4).")

        val emptySlotsBefore = Inventory.emptySlotCount()
        script.logger.info("CARGO: Empty slots before withdrawal: $emptySlotsBefore.")

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        val success = executeWithdrawCargo(script)

        if (extractorTask.checkAndExecuteInterrupt(script)) return

        if (success) {
            val emptySlotsAfter = Inventory.emptySlotCount()
            val withdrawnCount = emptySlotsBefore - emptySlotsAfter

            if (withdrawnCount > 0) {
                script.xpMessageCount = (script.xpMessageCount - withdrawnCount).coerceAtLeast(0)
                script.logger.info("CARGO: Withdrew $withdrawnCount items. Remaining cargo proxy: ${script.xpMessageCount}.")
            } else {
                script.logger.warn("CARGO: Withdrawal was successful, but no items were added (withdrawnCount=$withdrawnCount). Cargo count proxy untouched.")
            }

            script.currentPhase = SalvagePhase.IDLE
        } else {
            script.logger.warn("PHASE: Withdraw failed. Retrying in next cycle.")
            script.currentPhase = SalvagePhase.WITHDRAWING
        }
        script.logger.info("PHASE: Withdraw complete/failed. Transitioned to ${script.currentPhase.name}.")
    }
}

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
    val requiredDropDirectionStr: String get() = getOption<String>("Drop Salvage Direction")
    val SALVAGE_NAME: String get() = getOption<String>("Salvage Item Name")

    val requiredDropDirection: CardinalDirection get() = CardinalDirection.valueOf(requiredDropDirectionStr)

    val enableExtractor: Boolean get() = getOption<Boolean>("Enable Extractor")
    val extractorInterval: Long = 64000L
    val requiredTapDirectionStr: String get() = getOption<String>("Extractor Tap Direction")
    val requiredTapDirection: CardinalDirection get() = CardinalDirection.valueOf(requiredTapDirectionStr)

    @Volatile var harvesterMessageFound: Boolean = false
    @Volatile var hookCastMessageFound: Boolean = false
    @Volatile var extractorTimer: Long = 0L
    @Volatile var currentPhase: SalvagePhase = SalvagePhase.IDLE
    @Volatile var phaseStartTime: Long = 0L

    @Volatile var xpMessageCount: Int = 0

    private val taskList: kotlin.collections.List<Task> by lazy {
        logger.info("INIT: Task list initialized with priority order: Extractor -> Sort -> Cleanup -> Withdraw.")
        listOf(
            CrystalExtractorTask(this),
            SortSalvageTask(this),
            CleanupInventoryTask(this),
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

        if (change.messageType == MessageType.Spam) {
            if (change.message.contains("You gain some experience")) {
                xpMessageCount++
                logger.debug("EVENT: XP message (SPAM) detected! Current count: $xpMessageCount")
            }
        }
    }


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