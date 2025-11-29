package org.powbot.om6.derangedarch

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.derangedarch.tasks.*
import org.powbot.om6.derangedarch.utils.ScriptUtils

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
            "Define your full inventory. Must include an axe, some food, prayer pots, ring of dueling, emergency teleport, digsite pendant",
            optionType = OptionType.INVENTORY,
            defaultValue = "{\"1351\":1,\"13123\":1,\"2552\":1,\"11194\":1,\"2434\":5,\"385\":10,\"2446\":1,\"560\":1000,\"554\":5000}"
        ),
        ScriptConfiguration(
            "Food Name", "The name of the food in your inventory setup to eat.",
            optionType = OptionType.STRING, defaultValue = "Shark"
        ),
        ScriptConfiguration(
            "Always Loot", "Comma-separated list of items to loot regardless of value (e.g., Shark, Numulite).",
            optionType = OptionType.STRING,
            defaultValue = "Shark,Numulite"
        ),
        ScriptConfiguration(
            "Minimum Loot Value", "Don't loot items worth less than this (unless in 'Always Loot').",
            optionType = OptionType.INTEGER, defaultValue = "1000"
        ),
        ScriptConfiguration(
            "Eat At %", "What health percentage should the script eat at?",
            optionType = OptionType.INTEGER, defaultValue = "65"
        ),
        ScriptConfiguration(
            "Emergency Teleport HP %", "Teleport out if HP drops below this percentage.",
            optionType = OptionType.INTEGER, defaultValue = "25"
        ),
        ScriptConfiguration(
            "Emergency Teleport Item", "Select the item to use for emergency teleports.",
            optionType = OptionType.STRING,
            defaultValue = "Ardougne cloak",
            allowedValues = ["Teleport to house", "Ectophial", "Ardougne cloak"]
        )
    ]
)
class DerangedArchaeologistMagicKiller : AbstractScript() {

    data class Config(
        val requiredEquipment: Map<Int, Int>,
        val requiredInventory: Map<Int, Int>,
        val foodName: String,
        val alwaysLootItems: List<String>,
        val minLootValue: Int,
        val eatAtPercent: Int,
        val emergencyHpPercent: Int,
        val emergencyTeleportItem: String
    )

    lateinit var config: Config
    lateinit var teleportOptions: Map<String, TeleportOption>
    private var startTime: Long = 0

    // --- Script State ---
    var totalLootValue: Long = 0
    var hasAttemptedPoolDrink: Boolean = true
    var emergencyTeleportJustHappened: Boolean = false

    private var currentTask: String = "Starting..."
    private val tasks: List<Task> = listOf(
        EmergencyTeleportTask(this),
        DeactivatePrayerTask(this),
        WalkToBankAfterEmergencyTask(this),
        GoToBankTask(this),
        EquipItemsTask(this),
        BankTask(this),
        DrinkFromPoolTask(this),
        TravelToBossTask(this),
        DodgeSpecialTask(this),
        PrayerTask(this),
        PoisonTask(this),
        EatTask(this),
        FightTask(this),
        FixPitchTask(this),
        LootTask(this),
        RepositionTask(this)
    )

    data class TeleportOption(
        val itemNameContains: String,
        val interaction: String,
        val successCondition: () -> Boolean
    )

    override fun onStart() {
        startTime = System.currentTimeMillis()
        logger.info("Deranged Archaeologist script started.")

        val alwaysLootString = getOption<String>("Always Loot") ?: ""
        val alwaysLootList = alwaysLootString.split(",").map { it.trim() }.filter { it.isNotBlank() }

        config = Config(
            requiredEquipment = getOption("Required Equipment"),
            requiredInventory = getOption("Required Inventory"),
            foodName = getOption("Food Name"),
            alwaysLootItems = alwaysLootList,
            minLootValue = getOption<Int>("Minimum Loot Value"),
            eatAtPercent = getOption<Int>("Eat At %"),
            emergencyHpPercent = getOption<Int>("Emergency Teleport HP %"),
            emergencyTeleportItem = getOption("Emergency Teleport Item")
        )
        logger.info("Configuration loaded: $config")

        logger.info("Initializing loot price cache...")
        org.powbot.om6.derangedarch.utils.LootPriceCache.initialize()
        logger.info("Price cache initialized.")

        teleportOptions = mapOf(
            "Teleport to house" to TeleportOption(
                itemNameContains = "Teleport to house",
                interaction = "Break",
                successCondition = { Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) > 15 }
            ),
            "Ectophial" to TeleportOption(
                itemNameContains = "Ectophial",
                interaction = "Empty",
                successCondition = { Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) > 15 }
            ),
            "Ardougne cloak" to TeleportOption(
                itemNameContains = "Ardougne cloak",
                interaction = "Monastery Teleport",
                successCondition = { Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) > 15 }
            )
        )

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task:") { currentTask }
            .trackSkill(Skill.Magic)
            .addString("Loot GP:") { ScriptUtils.formatNumber(totalLootValue) }
            .addString("GP/hr:") {
                val runtime = System.currentTimeMillis() - startTime
                val gpPerHour = if (runtime > 0) (totalLootValue * 3600000 / runtime) else 0
                ScriptUtils.formatNumber(gpPerHour)
            }
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
            logger.debug("No valid task found, idling.")
            Condition.sleep(150)
        }
    }

    override fun canBreak(): Boolean {
        val nearBank = Players.local().tile().distanceTo(Constants.FEROX_BANK_AREA.centralTile) < 10
        val needsRestore = needsStatRestore()
        val canBreak = nearBank && !needsRestore
        logger.debug("canBreak check: nearBank=$nearBank, needsRestore=$needsRestore, result=$canBreak")
        return canBreak
    }

    fun equipmentIsCorrect(): Boolean {
        if (config.requiredEquipment.isEmpty()) {
            logger.debug("Equipment check: No required equipment defined, returning true.")
            return true
        }
        for ((requiredId, slotIndex) in config.requiredEquipment) {
            val slot = Equipment.Slot.values()[slotIndex]
            val wornItem = Equipment.itemAt(slot)

            if (ScriptUtils.isDuelingRing(requiredId)) {
                if (!wornItem.name().contains(Constants.DUELING_RING_NAME_CONTAINS)) {
                    logger.debug("Equipment check FAIL: Slot $slot should be a Dueling Ring, but found '${wornItem.name()}'")
                    return false
                }
            } else if (ScriptUtils.isDigsitePendant(requiredId)) {
                if (!wornItem.name().contains(Constants.DIGSITE_PENDANT_NAME_CONTAINS)) {
                    logger.debug("Equipment check FAIL: Slot $slot should be a Digsite Pendant, but found '${wornItem.name()}'")
                    return false
                }
            } else {
                if (wornItem.id() != requiredId) {
                    logger.debug("Equipment check FAIL: Slot $slot requires ID $requiredId, but found ID ${wornItem.id()}")
                    return false
                }
            }
        }
        logger.debug("Equipment check PASS: All items correct.")
        return true
    }

    fun inventoryIsCorrect(): Boolean {
        if (config.requiredInventory.isEmpty()) {
            logger.debug("Inventory check: No required inventory defined, returning true.")
            return true
        }
        for ((id, amount) in config.requiredInventory) {
            val currentCount = if (ScriptUtils.isDuelingRing(id)) {
                Inventory.stream().nameContains(Constants.DUELING_RING_NAME_CONTAINS).count(true)
            } else if (ScriptUtils.isDigsitePendant(id)) {
                Inventory.stream().nameContains(Constants.DIGSITE_PENDANT_NAME_CONTAINS).count(true)
            } else {
                Inventory.stream().id(id).count(true)
            }

            if (currentCount < amount) {
                logger.debug("Inventory check FAIL: Need $amount of ID $id, but have $currentCount")
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
        val noPrayerPotions = Inventory.stream().nameContains(Constants.PRAYER_POTION_NAME_CONTAINS).isEmpty()
        val result = noFood || noPrayerPotions
        logger.debug("needsTripResupply check: noFood=$noFood, noPrayerPotions=$noPrayerPotions, result=$result")
        return result
    }

    fun needsStatRestore(): Boolean {
        if (!Constants.FEROX_BANK_AREA.contains(Players.local())) {
            return false
        }
        val prayerNotFull = Prayer.prayerPoints() < Skills.realLevel(Skill.Prayer)
        val healthNotFull = Combat.healthPercent() < 100
        val result = prayerNotFull || healthNotFull
        logger.debug("needsStatRestore check: prayerNotFull=$prayerNotFull, healthNotFull=$healthNotFull, result=$result")
        return result
    }

    fun getBoss(): Npc? = Npcs.stream().id(Constants.ARCHAEOLOGIST_ID).nearest().firstOrNull()
}