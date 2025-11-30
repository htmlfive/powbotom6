package org.powbot.om6.pestcontrol

import org.powbot.api.Condition
import org.powbot.api.EventFlows
import org.powbot.api.Notifications
import org.powbot.api.Random
import org.powbot.api.event.TickEvent
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptConfiguration
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.paint.PaintFormatters
import org.powbot.api.script.paint.PaintFormatters.round
import org.powbot.mobile.script.ScriptManager
import org.powbot.mobile.service.ScriptUploader
import org.powbot.om6.pestcontrol.data.Activity
import org.powbot.om6.pestcontrol.data.Boat
import org.powbot.om6.pestcontrol.data.PestControlMap
import org.powbot.om6.pestcontrol.helpers.Zeal
import org.powbot.om6.pestcontrol.helpers.squire
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth
import org.powbot.om6.pestcontrol.task.*


fun main(args: Array<String>) {
    ScriptUploader().uploadAndStart("0m6PestControl", "", "localhost", false, useDefaultConfigs = false)
}

@ScriptManifest(
    name = "0m6 PestControl",
    description = "Plays the pest control minigame, start geared up",
    version = "1.0.0",
    category = ScriptCategory.Minigame,
    markdownFileName = "powpestcontrol.md"
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Boat Type",
            description = "Which level boat",
            defaultValue = "Easy",
            allowedValues = [
                "Easy", "Medium", "Hard"
            ]
        ),
        ScriptConfiguration(
            name = "Activity",
            description = "Which activity to take part in",
            defaultValue = "Mix",
            allowedValues = [
                "Defend Knight", "Attack Portal", "Mix"
            ]
        ),
    ]
)
class PowPestControl : AbstractScript() {

    val tasks: MutableList<Task> = mutableListOf<Task>()
    var status: String = "None"

    var boat: Boat? = null

    var initialPoints: Int? = null
    var pointsGained: Int = 0

    var isMix = false
    var activity: Activity? = null

    var gamesPlayed = 0
    var gamesSinceChangedActivity = -1

    var playedGame = false

    var attackPortal: AttackPortal? = null

    var zealPercentage: Int? = null

    override fun onStart() {
        val boatOpt = getOption<String?>("Boat Type")
        if (boatOpt != null) {
            boat = Boat.valueOf(boatOpt.replace(" ", ""))
            logger.info("Boat type selected: ${boat?.name}")
        }
        val activityOpt = getOption<String?>("Activity")
        if (activityOpt != null) {
            activity = Activity.valueOf(activityOpt.replace(" ", ""))
            logger.info("Activity selected: ${activity?.name}")
        }

        if (activity == null) {
            logger.error("No activity set - stopping script")
            Notifications.showNotification("No activity set")
            ScriptManager.stop()
        }

        if (boat == null) {
            logger.error("No boat set - stopping script")
            Notifications.showNotification("No boat set")
            ScriptManager.stop()
        }

        if (activity == Activity.Mix) {
            isMix = true
            logger.info("Mix mode enabled - will alternate between activities")
        }

        EventFlows.collectTicks {
            this.onTick(it)
        }

        val p = PaintBuilder.newBuilder()
            .addString("Status") { status }
            .addString("Activity") { activity?.name ?: "-" }
            .addString(
                "Points"
            ) {
                PaintFormatters.formatAmount((pointsGained).toLong())
            }
            .addString(
                "Games Played"
            ) {
                "${gamesPlayed}"
            }
            .addString(
                "Success Rate"
            ) {
                if (gamesPlayed == 0) {
                    "-"
                } else if (pointsGained == 0) {
                    "0%"
                } else {
                    "${(((pointsGained / boat!!.pointsPerGame).toDouble() / gamesPlayed.toDouble()) * 100).round(2)}%"
                }
            }
            .addString(
                "Fighting"
            ) {
                Players.local().interacting().name
            }
            .addString (
                "Zeal %"
            ) {
                if (zealPercentage != null) "${zealPercentage}%" else "-"
            }
            .trackSkill(Skill.Attack)
            .trackSkill(Skill.Defence)
            .trackSkill(Skill.Strength)
            .trackSkill(Skill.Hitpoints)
            .trackSkill(Skill.Magic)
            .trackSkill(Skill.Ranged)

        initTasks()

        if (attackPortal != null) {
            p.addString("Portal") { attackPortal?.portal?.name ?: "-" }
        }
        addPaint(p.build())
    }

    private fun initTasks() {
        tasks.clear()
        logger.info("Initializing tasks for activity: ${activity?.name}")

        tasks.add(Sleep())
        tasks.add(SetZoom())
        tasks.add(BoatWait(this))
        tasks.add(LeaveBoat(this))
        tasks.add(CrossGangplank(boat!!))
        tasks.add(AttackInteracting())

        when (activity) {
            Activity.DefendKnight -> tasks.add(DefendKnight(activity!!))
            Activity.AttackPortal -> {
                tasks.add(AttackNearestNpc())
                attackPortal = AttackPortal(activity!!)
                tasks.add(attackPortal!!)
            }
            else -> {}
        }
    }

    override fun poll() {
        val squire = Npcs.squire()
        if (squire.valid() && Components.voidKnightHealth().visible()) {
            PestControlMap.update(squire.tile())
        }

        if (isMix && (gamesSinceChangedActivity == -1 || gamesSinceChangedActivity >= Random.nextInt(3, 7))) {
            activity = if (Random.nextBoolean()) Activity.DefendKnight else Activity.AttackPortal
            logger.info("Mix mode: Switching activity to ${activity?.name}")
            initTasks()

            gamesSinceChangedActivity = 0
        }

        if (!Movement.running() && Movement.energyLevel() >= Random.nextInt(5, 10)) {
            Movement.running(true)
        }

        val task = tasks.firstOrNull { it.valid() }
        if (task != null) {
            status = task.name()
            task.run()
            return
        }

        status = "None"
        Condition.sleep(100)
    }

    fun onTick(evt: TickEvent) {
        zealPercentage = Zeal.percentage()
    }
}