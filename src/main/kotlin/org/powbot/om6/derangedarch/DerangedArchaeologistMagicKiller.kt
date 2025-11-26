package org.powbot.om6.derangedarch

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.derangedarch.tasks.*

@ScriptManifest(
    name = "0m6 Deranged Archaeologist (BETA)",
    description = "Must have ring of dueling in inventory to start.",
    version = "2.4.2",
    author = "0m6",
    category = ScriptCategory.Combat
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            "Required Equipment", "Define the gear to wear.",
            optionType = OptionType.EQUIPMENT,
            defaultValue = "{\"4089\":0,\"21795\":1,\"12002\":2,\"12658\":3,\"4091\":4,\"25818\":5,\"4093\":7,\"7462\":9,\"4097\":10,\"30895\":12,\"20232\":13}"
        ),
        ScriptConfiguration(
            "Required Inventory",
            "Define your full inventory. Must include an axe, some food, prayer pots, ring of dueling, emergency teleport, and digsite pendant.",
            optionType = OptionType.INVENTORY,
            defaultValue = "{\"1387\":\"1\",\"3144\":\"10\",\"3024\":\"4\",\"2552\":\"1\",\"12793\":\"1\",\"30895\":\"1\"}"
        ),
        ScriptConfiguration(
            "Food Name", "The name of the food to eat.",
            optionType = OptionType.STRING,
            defaultValue = "Manta ray"
        ),
        ScriptConfiguration(
            "Eat at Health Percent", "The health percentage to eat at.",
            optionType = OptionType.INTEGER,
            defaultValue = "60"
        ),
        ScriptConfiguration(
            "Emergency Teleport Item", "The item to use for emergency teleport (Dueling Ring or Royal Seed Pod are recommended).",
            optionType = OptionType.STRING,
            defaultValue = "Ring of Dueling"
        ),
        ScriptConfiguration(
            "Emergency HP Percent", "The health percentage to trigger an emergency teleport at.",
            optionType = OptionType.INTEGER,
            defaultValue = "25"
        ),
        ScriptConfiguration(
            "Min Loot Value", "The minimum GE value of an item stack to pick up.",
            optionType = OptionType.INTEGER,
            defaultValue = "15000"
        ),
        ScriptConfiguration(
            "Always Loot Items", "A comma-separated list of item names that should always be looted, regardless of value.",
            optionType = OptionType.STRING,
            defaultValue = "Runes,Grinding"
        ),
        ScriptConfiguration(
            "Disable Looting", "Disable the Loot task.",
            optionType = OptionType.BOOLEAN,
            defaultValue = "false"
        )
    ]
)
class DerangedArchaeologistMagicKiller : AbstractScript() {
    val config: DerangedArchaeologistConfig = DerangedArchaeologistConfig.instance(this)
    var totalLootValue: Long = 0L

    // Flag set by EmergencyTeleportTask to ensure other tasks wait until we are safe
    var emergencyTeleportJustHappened: Boolean = false

    // Flag set by DrinkFromPoolTask to skip the task if stats are full on arrival
    var hasAttemptedPoolDrink: Boolean = false

    // --- Area and Tile Constants ---
    val FEROX_BANK_TILE = Tile(3135, 3631, 0)
    val FEROX_BANK_AREA = Area(Tile(3125, 3624, 0), Tile(3143, 3639, 0))
    val FEROX_POOL_AREA = Area(Tile(3144, 3623, 0), Tile(3156, 3646, 0))
    val BOSS_TRIGGER_TILE = Tile(3683, 3706, 0)

    // --- Prayer Constant ---
    val REQUIRED_PRAYER = Prayer.ProtectFromMagic

    // --- Teleport Configuration ---
    data class TeleportOption(
        val itemNameContains: String,
        val interaction: String,
        val successCondition: () -> Boolean
    )

    val teleportOptions = mapOf(
        "Ring of Dueling" to TeleportOption(
            itemNameContains = IDs.RING_OF_DUELING_NAME,
            interaction = "Rub",
            successCondition = { FEROX_BANK_AREA.contains(Players.local()) }
        ),
        "Royal Seed Pod" to TeleportOption(
            itemNameContains = "Royal seed pod",
            interaction = "Commune",
            successCondition = { FEROX_BANK_AREA.contains(Players.local()) }
        )
        // Add other teleports here if needed
    )

    override fun onStart() {
        val tasks = listOf(
            WalkToBankAfterEmergencyTask(this),
            EmergencyTeleportTask(this),
            GoToBankTask(this),
            DrinkFromPoolTask(this),
            EquipItemsTask(this),
            PreBankEquipTask(this), // Must run before TravelToBossTask to ensure correct items are equipped
            TravelToBossTask(this),
            RepositionTask(this),
            FixPitchTask(this),
            DeactivatePrayerTask(this),
            DodgeSpecialTask(this),
            FightTask(this),
            EatTask(this),
            if (!config.disableLooting) LootTask(this) else null
        ).filterNotNull()

        taskExecutors.addAll(tasks)

        paint = PaintBuilder.builder()
            .addString("Status", { lastTask.get()?.name ?: "Idle" })
            .addString("Loot Value", { totalLootValue.toString() })
            .build()

        logger.info("Script started with ${taskExecutors.size} tasks.")
        logger.info("Looting is ${if (config.disableLooting) "DISABLED" else "ENABLED (Min value: ${config.minLootValue}gp)"}.")
    }

    override fun onStop() {
        logger.info("Total profit: ${totalLootValue} gp")
    }

    /**
     * Helper function to get the boss NPC.
     */
    fun getBoss(): Npc? {
        // Use the centralized ID
        return Npcs.stream().id(IDs.DERANGED_ARCHAEOLOGIST_ID).nearest().firstOrNull()
    }

    /**
     * Checks if all required items are equipped in the correct slots.
     */
    fun equipmentIsCorrect(): Boolean {
        for ((requiredId, slotIndex) in config.requiredEquipment) {
            val targetSlot = Equipment.Slot.values()[slotIndex]
            val equippedItem = Equipment.itemAt(targetSlot)
            if (equippedItem.id() != requiredId) {
                logger.debug("Equipment check FAIL: Missing item with ID $requiredId in slot ${targetSlot.name}. Found ${equippedItem.name()} (ID: ${equippedItem.id()}).")
                return false
            }
        }
        logger.debug("Equipment check PASS: All required items are equipped correctly.")
        return true
    }

    /**
     * Checks if the inventory contains the correct number of items.
     * Note: This does not check for items that should NOT be in the inventory (e.g., extra junk).
     */
    fun inventoryIsCorrect(): Boolean {
        for ((requiredItemName, requiredCountString) in config.requiredInventory) {
            val requiredCount = requiredCountString.toIntOrNull() ?: 0
            val actualCount = Inventory.stream().name(requiredItemName).count()

            if (actualCount < requiredCount) {
                logger.debug("Inventory check FAIL: Missing $requiredItemName. Expected $requiredCount, found $actualCount.")
                return false
            }
        }
        logger.debug("Inventory check PASS: All items and counts correct.")
        return true
    }

    fun needsFullRestock(): Boolean {
        val equipCorrect = equipmentIsCorrect()
        val invCorrect = inventoryIsCorrect()
        val result = !equipCorrect || !invCorrect
        logger.debug("needsFullRestock check: equipCorrect=$equipCorrect, invCorrect=$invCorrect, result=$result")
        return result
    }

    fun needsTripResupply(): Boolean {
        val noFood = Inventory.stream().name(config.foodName).isEmpty()
        val noPrayerPotions = Inventory.stream().nameContains(IDs.PRAYER_POTION_NAME_CONTAINS).isEmpty()
        val result = noFood || noPrayerPotions
        logger.debug("needsTripResupply check: noFood=$noFood, noPrayerPotions=$noPrayerPotions, result=$result")
        return result
    }

    fun needsStatRestore(): Boolean {
        if (!FEROX_BANK_AREA.contains(Players.local())) {
            return false // Only need to restore stats when at the bank.
        }
        val prayerNotFull = Prayer.prayerPoints() < Skills.realLevel(Skill.Prayer)
        val healthNotFull = Combat.healthPercent() < 100
        val result = prayerNotFull || healthNotFull
        logger.debug("needsStatRestore check: prayerNotFull=$prayerNotFull, healthNotFull=$healthNotFull, result=$result")
        return result
    }
}