package org.powbot.om6.thieving.stallthiever

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.event.GameObjectActionEvent
import org.powbot.api.rt4.*
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.OptionType
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import kotlin.random.Random

@ScriptManifest(
    name = "0m6 Stalls",
    description = "Stalls and shit",
    version = "2.1.5",
    author = "0m6",
    category = ScriptCategory.Thieving
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            "Stall Target",
            "Click 'Examine' or 'Steal-from' on the stall you want to thieve from.",
            optionType = OptionType.GAMEOBJECT_ACTIONS
        ),
        ScriptConfiguration(
            "Enable Hopping",
            "If enabled, the script will hop worlds if another player is on your thieving tile.",
            defaultValue = "true",
            optionType = OptionType.BOOLEAN
        ),
        ScriptConfiguration(
            "Steal 1 Drop 1 Mode",
            "If enabled, the script will immediately drop all junk items after successfully stealing one item.",
            defaultValue = "true",
            optionType = OptionType.BOOLEAN,
            visible = false
        ),
        ScriptConfiguration(
            "Target Item Names",
            "Comma-separated list of item names to BANK when inventory is full.",
            defaultValue = "Cake",
            optionType = OptionType.STRING
        ),
        ScriptConfiguration(
            "Items to DROP",
            "Comma-separated list of item names to **ALWAYS DROP**.",
            defaultValue = "Chocolate slice, Bread",
            optionType = OptionType.STRING
        ),
        ScriptConfiguration(
            "Thieving Tile",
            "Click the tile you want to stand on while thieving.",
            defaultValue = "{\"x\": 2669, \"y\":3310, \"floor\": 0}",
            optionType = OptionType.TILE
        ),
        ScriptConfiguration(
            "Bank Tile",
            "Click the tile you want to stand on when banking.",
            defaultValue = "{\"x\": 2655, \"y\":3283, \"floor\": 0}",
            optionType = OptionType.TILE
        )
    ]
)
class StallThiever : AbstractScript() {

    // --- GUI Config Accessors ---
    private val stallTargetEvents by lazy { getOption<List<GameObjectActionEvent>>("Stall Target") }
    val STALL_ID by lazy { stallTargetEvents.firstOrNull()?.id ?: -1 }
    val STALL_NAME by lazy { stallTargetEvents.firstOrNull()?.name ?: "" }

    val ENABLE_HOPPING by lazy { getOption<Boolean>("Enable Hopping") }
    val DROP_1_MODE by lazy { getOption<Boolean>("Steal 1 Drop 1 Mode") }
    val THIEVING_TILE by lazy { getOption<Tile>("Thieving Tile") }
    val BANK_TILE by lazy { getOption<Tile>("Bank Tile") }

    val TARGET_ITEM_NAMES_BANK by lazy { getOption<String>("Target Item Names").split(",").map { it.trim() }.filter { it.isNotEmpty() } }
    val TARGET_ITEM_NAMES_DROP by lazy { getOption<String>("Items to DROP").split(",").map { it.trim() }.filter { it.isNotEmpty() } }

    var currentTask: String = "Starting..."
    var justStole: Boolean = false // Flag for S1D1 mode

    // --- Task Framework ---

    abstract inner class Task {
        abstract fun validate(): Boolean
        abstract fun execute()
    }

    private val tasks: List<Task> = listOf(
        HandlePitchTask(),
        HandleHoppingTask(),
        BankTask(),
        WalkToBankTask(),
        DropTask(),
        WalkToStallTask(),
        ThieveTask()
    )

    // --- Core Script Methods ---

    override fun onStart() {
        if (STALL_ID == -1) {
            logger.warn("Stall Target not set! Please restart and click a stall.")
            ScriptManager.stop()
            return
        }
        if (THIEVING_TILE == null || BANK_TILE == null) {
            logger.warn("Thieving or Bank Tile not set. Please restart and select them.")
            ScriptManager.stop()
            return
        }
        logger.info("Script started. Targeting stall '$STALL_NAME' (ID: $STALL_ID).")
        logger.info("Steal 1 Drop 1 Mode is ON (hidden default).")

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task:") { currentTask }
            .trackSkill(org.powbot.api.rt4.walking.model.Skill.Thieving)
            .build()
        addPaint(paint)
    }

    override fun poll() {
        val task = tasks.firstOrNull { it.validate() }
        if (task != null) {
            currentTask = task.javaClass.simpleName
            task.execute()
        } else {
            currentTask = "Idle"
            Condition.sleep(150)
        }
    }

    // --- Task Definitions ---

    inner class HandlePitchTask : Task() {
        private val DESIRED_PITCH = 99
        override fun validate(): Boolean = Camera.pitch() != DESIRED_PITCH
        override fun execute() {
            logger.info("Pitch is incorrect, adjusting to $DESIRED_PITCH.")
            Camera.pitch(DESIRED_PITCH)
        }
    }

    inner class HandleHoppingTask : Task() {
        override fun validate(): Boolean = ENABLE_HOPPING &&
                Players.local().tile() == THIEVING_TILE &&
                Players.stream().at(Players.local().tile()).any { it != Players.local() }

        override fun execute() {
            val randomWorld = Worlds.stream()
                .filtered { it.type() == World.Type.MEMBERS && it.population in 15..350 && it.specialty() == World.Specialty.NONE }
                .toList().randomOrNull()

            if (randomWorld != null) {
                logger.info("Player detected on tile. Hopping to world ${randomWorld.number}.")
                if (randomWorld.hop()) {
                    Condition.wait({ !Players.local().inMotion() }, 300, 20)
                }
            } else {
                logger.warn("Player detected, but no suitable world found for hopping.")
                Condition.sleep(3000)
            }
        }
    }

    inner class BankTask : Task() {
        override fun validate(): Boolean = Inventory.isFull() && BANK_TILE.distance() <= 5
        override fun execute() {
            if (!Bank.opened() && !Bank.open()) {
                logger.warn("Failed to open bank.")
                return
            }

            if (Bank.opened()) {
                logger.info("Depositing items...")
                for (itemName in TARGET_ITEM_NAMES_BANK) {
                    if (Inventory.stream().name(itemName).isNotEmpty()) {
                        if (Bank.deposit(itemName, Bank.Amount.ALL)) {
                            Condition.wait({ Inventory.stream().name(itemName).isEmpty() }, 150, 10)
                        }
                    }
                }
                if (Inventory.isFull()) {
                    Bank.depositInventory()
                }
                Bank.close()
            }
        }
    }

    inner class WalkToBankTask : Task() {
        override fun validate(): Boolean = Inventory.isFull() &&
                Inventory.stream().name(*TARGET_ITEM_NAMES_BANK.toTypedArray()).isNotEmpty() &&
                BANK_TILE.distance() > 5

        override fun execute() {
            logger.info("Inventory full, walking to bank...")
            if (Bank.opened()) Bank.close()
            Movement.walkTo(BANK_TILE)
        }
    }

    inner class DropTask : Task() {
        override fun validate(): Boolean {
            val junkInInventory = Inventory.stream().name(*TARGET_ITEM_NAMES_DROP.toTypedArray()).isNotEmpty()
            return (DROP_1_MODE && justStole && junkInInventory) ||
                    (Inventory.isFull() && Inventory.stream().all { it.name() in TARGET_ITEM_NAMES_DROP })
        }

        override fun execute() {
            logger.info("Dropping junk items...")
            val itemsToDrop = Inventory.stream().name(*TARGET_ITEM_NAMES_DROP.toTypedArray()).list()
            itemsToDrop.forEach { item ->
                if (item.interact("Drop")) {
                    Condition.sleep(Random.nextInt(50, 150))
                }
            }
            Condition.wait({ Inventory.stream().name(*TARGET_ITEM_NAMES_DROP.toTypedArray()).isEmpty() }, 200, 15)
            justStole = false
        }
    }

    inner class WalkToStallTask : Task() {
        override fun validate(): Boolean = Players.local().tile() != THIEVING_TILE && !Players.local().inMotion()
        override fun execute() {
            logger.info("Not at thieving tile, walking back...")
            Movement.walkTo(THIEVING_TILE)
        }
    }

    inner class ThieveTask : Task() {
        private val STEAL_ACTION = "Steal-from"
        override fun validate(): Boolean = !Inventory.isFull() &&
                Players.local().tile() == THIEVING_TILE &&
                Players.local().animation() == -1

        override fun execute() {
            val stall = Objects.stream()
                .id(STALL_ID)
                .within(THIEVING_TILE, 3.0)
                .nearest()
                .firstOrNull()

            if (stall == null) {
                logger.warn("Stall not found within range of thieving tile, waiting...")
                Condition.sleep(2000)
                return
            }

            if (!stall.inViewport()) {
                logger.info("Stall not in view, turning camera.")
                Camera.turnTo(stall)
            }

            val initialXp = Skills.experience(org.powbot.api.rt4.walking.model.Skill.Thieving)
            if (stall.interact(STEAL_ACTION)) {
                if (Condition.wait({ Skills.experience(org.powbot.api.rt4.walking.model.Skill.Thieving) > initialXp }, 150, 20)) {
                    justStole = true
                    Condition.sleep(Random.nextInt(150, 250))
                }
            }
        }
    }
}

fun main() {
    val script = StallThiever()
    script.startScript("localhost", "0m6", false)
}