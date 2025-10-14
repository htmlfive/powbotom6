package org.powbot.om6.stalls

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.event.GameObjectActionEvent
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.stalls.tasks.*

@ScriptManifest(
    name = "0m6 Stalls",
    description = "Stalls and shit",
    version = "2.2.1",
    author = "0m6",
    category = ScriptCategory.Thieving
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            "Stall Target",
            "Click 'Examine' or 'Steal-from' on the stall you want to thieve from.",
            defaultValue = "[{\"id\":11730, \"name\":\"Baker's stall\", \"interaction\":\"Steal-from\", \"tile\":{\"floor\":0, \"x\":2667, \"y\":3310}}]",
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
            defaultValue = "{\"x\": 255, \"y\":3283, \"floor\": 0}",
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
    private val tasks: List<Task> = listOf(
        HandlePitchTask(this),
        HandleHoppingTask(this),
        BankTask(this),
        WalkToBankTask(this),
        DropTask(this),
        WalkToStallTask(this),
        ThieveTask(this)
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

    override fun canBreak(): Boolean {
        val atBank = Players.local().tile() == BANK_TILE
        val bankClosed = !Bank.opened()
        val inventoryDeposited = !Inventory.isFull()
        val notInCombat = !Players.local().inCombat()

        return atBank && bankClosed && inventoryDeposited && notInCombat
    }
}

fun main() {
    val script = StallThiever()
    script.startScript("localhost", "0m6", false)
}