package org.powbot.om6.moths

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.OptionType
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.moths.tasks.*

@ScriptManifest(
    name = Constants.SCRIPT_NAME,
    description = Constants.SCRIPT_DESCRIPTION,
    version = Constants.SCRIPT_VERSION,
    author = Constants.SCRIPT_AUTHOR,
    category = ScriptCategory.Hunter
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Enable World Hopping",
            description = "Hop to a new world when another player is detected nearby",
            defaultValue = "true",
            optionType = OptionType.BOOLEAN
        )
    ]
)
class MoonlightMothCatcher : AbstractScript() {

    // Configuration
    var enableWorldHopping: Boolean = true

    // Statistics
    var mothsCaught: Int = 0
    var currentTask: String = "Starting..."

    // Task list
    private lateinit var tasks: List<Task>

    override fun onStart() {
        logger.info("${Constants.SCRIPT_NAME} v${Constants.SCRIPT_VERSION} starting...")

        // Load configuration
        enableWorldHopping = getOption("Enable World Hopping")
        logger.info("World hopping enabled: $enableWorldHopping")

        // Set camera pitch
        Camera.pitch(Constants.CAMERA_PITCH)

        // Initialize tasks in priority order
        initializeTasks()

        // Setup paint
        setupPaint()

        logger.info("Script initialized successfully")
    }

    /**
     * Initializes the task list in priority order.
     */
    private fun initializeTasks() {
        val taskList = mutableListOf<Task>()

        // Highest priority: Banking (no jars = can't catch)
        taskList.add(BankTask(this))

        // World hop if enabled and player nearby
        if (enableWorldHopping) {
            taskList.add(WorldHopTask(this))
        }

        // Enable running
        taskList.add(EnableRunningTask(this))

        // Travel to moths if not there
        taskList.add(TravelToMothsTask(this))

        // Catch moths
        taskList.add(CatchMothTask(this))

        tasks = taskList
        logger.info("Initialized ${tasks.size} tasks")
    }

    /**
     * Sets up the paint overlay.
     */
    private fun setupPaint() {
        val paint = PaintBuilder.newBuilder()
            .x(Constants.PAINT_X)
            .y(Constants.PAINT_Y)
            .addString("Task") { currentTask }
            .addString("Moths Caught") { mothsCaught.toString() }
            .addString("Jars") { 
                org.powbot.api.rt4.Inventory.stream()
                    .name(Constants.BUTTERFLY_JAR_NAME)
                    .count()
                    .toString() 
            }
            .trackSkill(Skill.Hunter)
            .build()
        addPaint(paint)
    }

    override fun poll() {
        // Enable running check (low overhead, always do this)
        ScriptUtils.enableRunning()

        // Find and execute first valid task
        val task = tasks.firstOrNull { it.activate() }

        if (task != null) {
            currentTask = task.name()
            task.execute()
        } else {
            currentTask = "Idle"
            logger.debug("No valid task found, idling")
        }

        // Small sleep between polls
        Condition.sleep(Random.nextInt(Constants.POLL_SLEEP_MIN, Constants.POLL_SLEEP_MAX))
    }

    override fun onStop() {
        logger.info("${Constants.SCRIPT_NAME} stopped. Moths caught: $mothsCaught")
    }
}
