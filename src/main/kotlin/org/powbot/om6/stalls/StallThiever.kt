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

/**
 * A data class to hold all script configuration.
 * This centralizes settings, making them easier to manage and pass around.
 *
 * @param stallId The ID of the target stall.
 * @param stallName The name of the target stall.
 * @param enableHopping If true, hop worlds when another player is nearby.
 * @param drop1Mode If true, drop junk immediately after a successful theft.
 * @param thievingTile The tile to stand on while thieving.
 * @param bankTile The tile to stand on while banking.
 * @param itemsToBank A list of item names to bank.
 * @param itemsToDrop A list of item names to always drop.
 */
data class StallThieverConfig(
    val stallId: Int,
    val stallName: String,
    val enableHopping: Boolean,
    val drop1Mode: Boolean,
    val thievingTile: Tile,
    val bankTile: Tile,
    val itemsToBank: List<String>,
    val itemsToDrop: List<String>
)

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
            defaultValue = "{\"x\": 2655, \"y\":3283, \"floor\": 0}",
            optionType = OptionType.TILE
        )
    ]
)
class StallThiever : AbstractScript() {
    lateinit var config: StallThieverConfig
        private set

    var currentTask: String = "Starting..."
    var justStole: Boolean = false

    private lateinit var tasks: List<Task>

    override fun onStart() {
        val stallTargetEvents = getOption<List<GameObjectActionEvent>>("Stall Target")
        val thievingTile = getOption<Tile>("Thieving Tile")
        val bankTile = getOption<Tile>("Bank Tile")

        if (stallTargetEvents.isEmpty() || thievingTile == Tile.Nil || bankTile == Tile.Nil) {
            logger.warn("Configuration not set correctly. Please restart the script and configure all options.")
            ScriptManager.stop()
            return
        }

        config = StallThieverConfig(
            stallId = stallTargetEvents.first().id,
            stallName = stallTargetEvents.first().name,
            enableHopping = getOption("Enable Hopping"),
            drop1Mode = getOption("Steal 1 Drop 1 Mode"),
            thievingTile = thievingTile,
            bankTile = bankTile,
            itemsToBank = getOption<String>("Target Item Names").split(",").map { it.trim() }.filter { it.isNotEmpty() },
            itemsToDrop = getOption<String>("Items to DROP").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )

        logger.info("Script started. Targeting stall '${config.stallName}' (ID: ${config.stallId}).")

        tasks = listOf(
            HandlePitchTask(this),
            HandleHoppingTask(this),
            BankTask(this),
            WalkToBankTask(this),
            DropTask(this),
            WalkToStallTask(this),
            ThieveTask(this)
        )

        // --- Paint Setup ---
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
        val atBank = Players.local().tile() == config.bankTile
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
