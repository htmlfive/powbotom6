package org.powbot.om6.stalls

import org.powbot.api.Condition
import org.powbot.api.Notifications
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
    version = "2.2.2",
    author = Constants.Script.AUTHOR,
    scriptId = "5a9626a1-3db0-4270-b447-c2d1dc98af66",
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
        logger.info("--- StallThiever Script Initialization ---")

        val stallTargetEvents = getOption<List<GameObjectActionEvent>>(Constants.ConfigKeys.STALL_TARGET)
        val thievingTile = getOption<Tile>(Constants.ConfigKeys.THIEVING_TILE)
        val bankTile = getOption<Tile>(Constants.ConfigKeys.BANK_TILE)

        logger.info("1. Retrieving Configuration Values:")
        logger.info("   Stall Target Events: ${stallTargetEvents?.size} entries.")
        logger.info("   Thieving Tile: $thievingTile")
        logger.info("   Bank Tile: $bankTile")

        // NEW VALIDATION: Ensure exactly one stall is selected
        if (stallTargetEvents != null && stallTargetEvents.size > 1) {
            logger.warn("Configuration validation FAILED. More than one GameObject was selected for the stall target.")
            Notifications.showNotification("Please select only ONE stall object as the target for thieving.")
            ScriptManager.stop()
            return
        }

        if (!ScriptUtils.isConfigurationValid(thievingTile, bankTile, stallTargetEvents)) {
            logger.warn("2. Configuration validation FAILED. One or more required options (Tiles/Stall Target) are missing or invalid.")
            Notifications.showNotification("Configuration not set correctly. Please restart the script and configure all options.")
            ScriptManager.stop()
            return
        }

        logger.info("2. Configuration validation SUCCESSFUL.")

        // Retrieve remaining options
        val enableHopping = getOption<Boolean>(Constants.ConfigKeys.ENABLE_HOPPING)
        val drop1Mode = getOption<Boolean>(Constants.ConfigKeys.DROP_1_MODE)
        val itemsToBankStr = getOption<String>(Constants.ConfigKeys.TARGET_ITEMS)
        val itemsToDropStr = getOption<String>(Constants.ConfigKeys.DROP_ITEMS)

        config = StallThieverConfig(
            stallId = stallTargetEvents!!.first().id,
            stallName = stallTargetEvents.first().name,
            enableHopping = enableHopping,
            drop1Mode = drop1Mode,
            thievingTile = thievingTile!!,
            bankTile = bankTile!!,
            itemsToBank = ScriptUtils.parseCommaSeparatedList(itemsToBankStr).map { it.lowercase() },
            itemsToDrop = ScriptUtils.parseCommaSeparatedList(itemsToDropStr).map { it.lowercase() }
        )

        logger.info("3. StallThieverConfig Built:")
        logger.info("  -> Stall: ${config.stallName} (ID: ${config.stallId})")
        logger.info("  -> Thieving Tile: ${config.thievingTile}")
        logger.info("  -> Bank Tile: ${config.bankTile}")
        logger.info("  -> Enable Hopping: ${config.enableHopping}")
        logger.info("  -> Drop 1 Mode: ${config.drop1Mode}")
        logger.info("  -> Items to Bank: [${config.itemsToBank.joinToString()}]")
        logger.info("  -> Items to Drop: [${config.itemsToDrop.joinToString()}]")

        logger.info("4. Initializing task list...")
        tasks = listOf(
            HandlePitchTask(this),
            HandleHoppingTask(this),
            BankTask(this),
            WalkToBankTask(this),
            DropTask(this),
            WalkToStallTask(this),
            ThieveTask(this)
        )
        logger.info("   Tasks initialized: ${tasks.size} tasks loaded.")

        // --- Paint Setup ---
        val paint = PaintBuilder.newBuilder()
            .x(Constants.Paint.X_POSITION)
            .y(Constants.Paint.Y_POSITION)
            .addString(Constants.Paint.TASK_LABEL) { currentTask }
            .trackSkill(org.powbot.api.rt4.walking.model.Skill.Thieving)
            .build()
        addPaint(paint)
        logger.info("5. Paint successfully added. Script is ready to poll.")
    }

    override fun poll() {
        logger.info("--- Poll Cycle Start (Current Task: $currentTask) ---")

        val task = tasks.firstOrNull {
            val validated = it.validate()
            logger.info("Attempting validation for ${it.name}: Result = $validated")
            validated
        }

        if (task != null) {
            currentTask = task.name
            logger.info("--- Task Selected --- Selected task: $currentTask. Executing...")
            task.execute()
            logger.info("Task $currentTask execution finished.")
        } else {
            currentTask = Constants.TaskNames.IDLE
            logger.info("No tasks validated in this cycle. Entering IDLE state.")
            Condition.sleep(Constants.Timing.IDLE_SLEEP)
            logger.info("IDLE state complete. Sleeping for ${Constants.Timing.IDLE_SLEEP}ms finished.")
        }
    }

    override fun canBreak(): Boolean {
        val safeBreak = ScriptUtils.canSafelyBreak(config.bankTile)
        logger.info("Checking if safe to break: $safeBreak (Based on being near bank tile: ${config.bankTile})")
        return safeBreak
    }
}