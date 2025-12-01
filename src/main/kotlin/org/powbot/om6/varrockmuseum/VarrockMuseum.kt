package org.powbot.om6.varrockmuseum

import org.powbot.api.Condition
import org.powbot.api.Notifications
import org.powbot.api.Random
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.ACTION_ADD_FINDS
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.ACTION_CLEAN
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.ACTION_TAKE
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.ANTIQUE_LAMP
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.CLEAN_FINDS
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.DEFAULT_DROP_LIST
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.DEFAULT_DROP_LIST_STRING
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.DEFAULT_LAMP_SKILL
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.DEFAULT_SPAM_CLICK
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.DIALOGUE_PLACE_ALL
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.DIG_SITE_SPECIMEN_ROCKS
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.LAMP_SKILL_IDS
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.LAMP_SKILL_WIDGET
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.SKILL_NAME_TO_ENUM
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.SPECIMEN_TABLE
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.STORAGE_CRATE
import org.powbot.om6.varrockmuseum.VarrockMuseumConstants.UNCLEANED_FIND
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.dropItems
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.getInventoryCount
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.handleDialogue
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.interactWithObject
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.inventoryContains
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.inventoryContainsAny
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.inventoryFull
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.rubLamp
import org.powbot.om6.varrockmuseum.VarrockMuseumUtils.waitUntil

@ScriptManifest(
    name = "0m6 Varrock Museum",
    description = "Collects and cleans finds at the Varrock Museum. Requires Specimen Brush, Rock pick, Trowel in INV and Leather boots & gloves equipped. Start in the area. Make sure the deposit in crate dialogue is set to Don't ask again.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Minigame
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Spam Click Take",
            description = "Spam click 'Take' on specimen rocks for faster collecting",
            defaultValue = DEFAULT_SPAM_CLICK.toString(),
            optionType = OptionType.BOOLEAN
        ),
        ScriptConfiguration(
            name = "Lamp Skill",
            description = "Skill to use antique lamp XP on",
            defaultValue = DEFAULT_LAMP_SKILL,
            allowedValues = arrayOf("Attack", "Strength", "Ranged", "Magic", "Defense", "Hitpoints", "Prayer", "Agility", "Herblore", "Thieving", "Crafting", "Runecrafting", "Slayer", "Farming", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting", "Fletching", "Construction", "Hunter"),
            optionType = OptionType.STRING
        ),
        ScriptConfiguration(
            name = "Drop List",
            description = "Comma-separated list of items to drop (e.g., Bowl,Pot)",
            defaultValue = DEFAULT_DROP_LIST_STRING,
            optionType = OptionType.STRING
        )
    ]
)
class VarrockMuseum : AbstractScript() {

    private var dropList: List<String> = DEFAULT_DROP_LIST
    private var spamClick: Boolean = DEFAULT_SPAM_CLICK
    private var lampSkill: String = DEFAULT_LAMP_SKILL
    private var findsCollected = 0
    private var lastInventoryCount = 0
    var currentTask: String = "Starting..."

    override fun onStart() {
        // Parse configuration
        spamClick = getOption("Spam Click Take")
        lampSkill = getOption("Lamp Skill")

        val dropListConfig = getOption<String>("Drop List")
        if (dropListConfig.isNotEmpty()) {
            dropList = dropListConfig.split(",").map { it.trim() }
        }

        logger.info("Varrock Museum script started")
        logger.info("Spam click: $spamClick")
        logger.info("Lamp skill: $lampSkill")
        logger.info("Drop list: $dropList")

        // Setup paint
        val paint = PaintBuilder.newBuilder()
            .x(40)
            .y(80)
            .addString("Current Task:") { currentTask }
            .addString("Specimens:") { findsCollected.toString() }
            .trackSkill(SKILL_NAME_TO_ENUM[lampSkill] ?: Skill.Slayer)
            .build()
        addPaint(paint)
    }

    override fun poll(): Unit {
        // Safety checks
        if (!checkLocation()) return
        if (!checkRequiredItems()) return
        if (!checkRequiredEquipment()) return

        // Execute tasks in priority order
        if (shouldRubLamp()) return
        if (shouldAddToCrate()) return
        if (shouldTakeSpecimens()) return
        if (shouldCleanFinds()) return
        if (shouldDropItems()) return
        // Idle
        idleTask()
    }

    private fun checkLocation(): Boolean {
        val playerTile = Players.local().tile()
        val museumTile = org.powbot.api.Tile(3264, 3444, 0)
        if (playerTile.distanceTo(museumTile) > 4) {
            logger.error("Player too far from museum area! Must be within 4 tiles of (3264, 3444)")
            Notifications.showNotification("Player too far from museum area! Must be within 4 tiles of (3264, 3444)")
            ScriptManager.stop()
            return false
        }
        return true
    }

    private fun checkRequiredItems(): Boolean {
        if (!inventoryContains("Rock pick") || !inventoryContains("Specimen brush")|| !inventoryContains("Trowel")) {
            logger.error("Missing required tool: Rock pick or Specimen brush not found in inventory!")
            Notifications.showNotification("Missing required tool: Rock pick, Trowel, or Specimen brush not found in inventory!")
            ScriptManager.stop()
            return false
        }
        return true
    }

    private fun checkRequiredEquipment(): Boolean {
        if (!Equipment.stream().name("Leather gloves").isNotEmpty() || !Equipment.stream().name("Leather boots").isNotEmpty()) {
            logger.error("Missing required equipment: Leather gloves or Leather boots not equipped!")
            Notifications.showNotification("Missing required equipment: Leather gloves or Leather boots not equipped!")
            ScriptManager.stop()
            return false
        }
        return true
    }

    private fun shouldDropItems(): Boolean {
        if (inventoryContainsAny(dropList)) {
            currentTask = "Dropping items"
            logger.info("Dropping unwanted items: $dropList")
            dropItems(dropList)
            return true
        }
        return false
    }

    private fun shouldRubLamp(): Boolean {
        if (inventoryContains(ANTIQUE_LAMP)) {
            currentTask = "Rubbing lamp"
            logger.info("Rubbing antique lamp for $lampSkill XP...")
            if (rubLamp(lampSkill, LAMP_SKILL_WIDGET, LAMP_SKILL_IDS)) {
                Condition.sleep(Random.nextInt(600, 1200))
            }
            return true
        }
        return false
    }

    private fun shouldCleanFinds(): Boolean {
        if (inventoryContains(UNCLEANED_FIND)) {
            currentTask = "Cleaning finds"
            logger.info("Cleaning finds at specimen table...")
            if (interactWithObject(SPECIMEN_TABLE, ACTION_CLEAN)) {
                Condition.wait({ Players.local().animation() == -1 && !inventoryContains(UNCLEANED_FIND) }, 1800, 40)
            }
            return true
        }
        return false
    }

    private fun shouldAddToCrate(): Boolean {
        if (inventoryContainsAny(CLEAN_FINDS)) {
            currentTask = "Adding to crate"
            logger.info("Adding finds to storage crate...")

            handleDialogue(DIALOGUE_PLACE_ALL)

            if (interactWithObject(STORAGE_CRATE, ACTION_ADD_FINDS)) {
                Condition.wait({ Players.local().animation() == -1 && !inventoryContainsAny(CLEAN_FINDS) }, 1800, 28)
                Condition.sleep(Random.nextInt(1200, 1800))
            }
            return true
        }
        return false
    }

    private fun shouldTakeSpecimens(): Boolean {
        if (!inventoryFull() && (!inventoryContainsAny(CLEAN_FINDS)) && (!inventoryContainsAny(dropList)) ) {
            currentTask = "Taking specimens"
            logger.info("Taking specimens from rocks...")

            val inventoryCountBefore = getInventoryCount()

            if (interactWithObject(DIG_SITE_SPECIMEN_ROCKS, ACTION_TAKE)) {
                if (spamClick) {
                    Condition.sleep(Random.nextInt(50, 100))
                } else {
                    waitUntil({ inventoryFull() }, 250, 500)
                }

                val inventoryCountAfter = getInventoryCount()
                val itemsAdded = inventoryCountAfter - inventoryCountBefore
                if (itemsAdded > 0) {
                    findsCollected += itemsAdded
                    logger.info("Added $itemsAdded specimens (Total: $findsCollected)")
                }
            }
            return true
        }
        return false
    }

    private fun idleTask() {
        currentTask = "Idle"
        Condition.sleep(Random.nextInt(600, 1200))
    }
}