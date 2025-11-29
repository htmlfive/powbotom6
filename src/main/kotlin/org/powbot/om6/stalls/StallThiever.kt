package org.powbot.om6.stalls

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.event.GameObjectActionEvent
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
    name = Constants.Script.NAME,
    description = Constants.Script.DESCRIPTION,
    version = Constants.Script.VERSION,
    author = Constants.Script.AUTHOR,
    category = ScriptCategory.Thieving
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            Constants.ConfigKeys.STALL_TARGET,
            Constants.ConfigDescriptions.STALL_TARGET,
            defaultValue = Constants.Defaults.DEFAULT_STALL_CONFIG,
            optionType = OptionType.GAMEOBJECT_ACTIONS
        ),
        ScriptConfiguration(
            Constants.ConfigKeys.ENABLE_HOPPING,
            Constants.ConfigDescriptions.ENABLE_HOPPING,
            defaultValue = Constants.Defaults.ENABLE_HOPPING.toString(),
            optionType = OptionType.BOOLEAN
        ),
        ScriptConfiguration(
            Constants.ConfigKeys.DROP_1_MODE,
            Constants.ConfigDescriptions.DROP_1_MODE,
            defaultValue = Constants.Defaults.DROP_1_MODE.toString(),
            optionType = OptionType.BOOLEAN,
            visible = false
        ),
        ScriptConfiguration(
            Constants.ConfigKeys.TARGET_ITEMS,
            Constants.ConfigDescriptions.TARGET_ITEMS,
            defaultValue = Constants.Defaults.TARGET_ITEMS,
            optionType = OptionType.STRING
        ),
        ScriptConfiguration(
            Constants.ConfigKeys.DROP_ITEMS,
            Constants.ConfigDescriptions.DROP_ITEMS,
            defaultValue = Constants.Defaults.DROP_ITEMS,
            optionType = OptionType.STRING
        ),
        ScriptConfiguration(
            Constants.ConfigKeys.THIEVING_TILE,
            Constants.ConfigDescriptions.THIEVING_TILE,
            defaultValue = Constants.Defaults.DEFAULT_THIEVING_TILE,
            optionType = OptionType.TILE
        ),
        ScriptConfiguration(
            Constants.ConfigKeys.BANK_TILE,
            Constants.ConfigDescriptions.BANK_TILE,
            defaultValue = Constants.Defaults.DEFAULT_BANK_TILE,
            optionType = OptionType.TILE
        )
    ]
)
class StallThiever : AbstractScript() {
    lateinit var config: StallThieverConfig
        private set

    var currentTask: String = Constants.TaskNames.STARTING
    var justStole: Boolean = false

    private lateinit var tasks: List<Task>

    override fun onStart() {
        val stallTargetEvents = getOption<List<GameObjectActionEvent>>(Constants.ConfigKeys.STALL_TARGET)
        val thievingTile = getOption<Tile>(Constants.ConfigKeys.THIEVING_TILE)
        val bankTile = getOption<Tile>(Constants.ConfigKeys.BANK_TILE)

        if (!ScriptUtils.isConfigurationValid(thievingTile, bankTile, stallTargetEvents)) {
            logger.warn("Configuration not set correctly. Please restart the script and configure all options.")
            ScriptManager.stop()
            return
        }

        config = StallThieverConfig(
            stallId = stallTargetEvents.first().id,
            stallName = stallTargetEvents.first().name,
            enableHopping = getOption(Constants.ConfigKeys.ENABLE_HOPPING),
            drop1Mode = getOption(Constants.ConfigKeys.DROP_1_MODE),
            thievingTile = thievingTile,
            bankTile = bankTile,
            itemsToBank = ScriptUtils.parseCommaSeparatedList(getOption(Constants.ConfigKeys.TARGET_ITEMS)),
            itemsToDrop = ScriptUtils.parseCommaSeparatedList(getOption(Constants.ConfigKeys.DROP_ITEMS))
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
            .x(Constants.Paint.X_POSITION)
            .y(Constants.Paint.Y_POSITION)
            .addString(Constants.Paint.TASK_LABEL) { currentTask }
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
            currentTask = Constants.TaskNames.IDLE
            Condition.sleep(Constants.Timing.IDLE_SLEEP)
        }
    }

    override fun canBreak(): Boolean {
        return ScriptUtils.canSafelyBreak(config.bankTile)
    }
}

