package org.powbot.om6.derangedarch

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.derangedarch.tasks.*

@ScriptManifest(
    name = "0m6 Deranged Archaeologist (BETA)",
    description = "Must have ring of dueling in inventory to start.",
    version = "2.5.1",
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

    val REQUIRED_PRAYER = Prayer.Effect.PROTECT_FROM_MISSILES
    val SPECIAL_ATTACK_PROJECTILE = Constants.SPECIAL_ATTACK_PROJECTILE
    val SPECIAL_ATTACK_TEXT = Constants.SPECIAL_ATTACK_TEXT

    var totalLootValue: Long = 0
    var hasAttemptedPoolDrink: Boolean = true
    var emergencyTeleportJustHappened: Boolean = false

    // Cached GE prices for boss loot
    val lootPriceCache = mutableMapOf<Int, Int>()

    private var currentTask: String = "Starting..."
    private val tasks: List<Task> = listOf(
        EmergencyTeleportTask(this),
        DeactivatePrayerTask(this),
        WalkToBankTask(this),
        EquipItemsTask(this),
        BankTask(this),
        DrinkFromPoolTask(this),
        TravelToBossTask(this),
        DodgeSpecialTask(this),
        PrayerTask(this),
        CurePoisonTask(this),
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

        // Cache GE prices for common boss loot
        logger.info("Caching GE prices for boss loot...")
        Constants.BOSS_LOOT_IDS.forEach { itemId ->
            val price = GrandExchange.getItemPrice(itemId) ?: 0
            lootPriceCache[itemId] = price
        }
        logger.info("Cached ${lootPriceCache.size} item prices")

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task:") { currentTask }
            .trackSkill(Skill.Magic)
            .addString("Loot GP:") { Helpers.formatNumber(totalLootValue) }
            .addString("GP/hr:") {
                val runtime = System.currentTimeMillis() - startTime
                val gpPerHour = if (runtime > 0) (totalLootValue * 3600000 / runtime) else 0
                Helpers.formatNumber(gpPerHour)
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
            Condition.sleep(150)
        }
    }

    override fun canBreak(): Boolean {
        val nearBank = Players.local().tile().distanceTo(Constants.FEROX_BANK_AREA.centralTile) < 10
        val needsRestore = needsStatRestore()
        return nearBank && !needsRestore
    }

    fun equipmentIsCorrect(): Boolean {
        if (config.requiredEquipment.isEmpty()) return true

        for ((requiredId, slotIndex) in config.requiredEquipment) {
            val slot = Equipment.Slot.values()[slotIndex]
            val wornItem = Equipment.itemAt(slot)

            if (Helpers.isDuelingRing(requiredId)) {
                if (!wornItem.name().contains("Ring of dueling")) return false
            } else if (Helpers.isDigsitePendant(requiredId)) {
                if (!wornItem.name().contains("Digsite pendant")) return false
            } else {
                if (wornItem.id() != requiredId) return false
            }
        }
        return true
    }

    fun inventoryIsCorrect(): Boolean {
        if (config.requiredInventory.isEmpty()) return true

        for ((id, amount) in config.requiredInventory) {
            val currentCount = if (Helpers.isDuelingRing(id)) {
                Inventory.stream().nameContains("Ring of dueling").count(true)
            } else if (Helpers.isDigsitePendant(id)) {
                Inventory.stream().nameContains("Digsite pendant").count(true)
            } else {
                Inventory.stream().id(id).count(true)
            }

            if (currentCount < amount) return false
        }
        return true
    }

    fun needsFullRestock(): Boolean = !equipmentIsCorrect() || !inventoryIsCorrect()

    fun needsTripResupply(): Boolean {
        val noFood = Inventory.stream().name(config.foodName).isEmpty()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()
        return noFood || noPrayerPotions
    }

    fun needsStatRestore(): Boolean {
        if (!Constants.FEROX_BANK_AREA.contains(Players.local())) return false

        val prayerNotFull = Prayer.prayerPoints() < Skills.realLevel(Skill.Prayer)
        val healthNotFull = Combat.healthPercent() < 100
        return prayerNotFull || healthNotFull
    }

    fun getBoss(): Npc? = Npcs.stream().id(Constants.ARCHAEOLOGIST_ID).nearest().firstOrNull()

    fun getCachedPrice(itemId: Int): Int = lootPriceCache.getOrPut(itemId) {
        GrandExchange.getItemPrice(itemId) ?: 0
    }
}