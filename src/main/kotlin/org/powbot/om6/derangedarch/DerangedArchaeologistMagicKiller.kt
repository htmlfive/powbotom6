package org.powbot.om6.derangedarch

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.om6.derangedarch.tasks.*
import java.util.concurrent.TimeUnit

@ScriptManifest(
    name = "0m6 Deranged Archaeologist (Magic)",
    description = "Kills the Archaeologist with user-defined gear and inventory setups.",
    version = "2.4.1",
    author = "0m6",
    category = ScriptCategory.Combat
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            "Required Equipment", "Define the gear to wear.",
            optionType = OptionType.EQUIPMENT,
            defaultValue = "{\"4089\":0,\"21795\":1,\"12002\":2,\"11907\":3,\"4091\":4,\"25818\":5,\"4093\":7,\"7462\":9,\"4097\":10,\"9104\":12,\"20232\":13}"
        ),
        ScriptConfiguration(
            "Required Inventory",
            "NOTE: Define your full inventory here. You MUST include:\n- Your food\n- Prayer potions\n- Ring of dueling\n- Digsite pendant\n- Your chosen Emergency Teleport item\n- An axe and/or rake if desired.",
            optionType = OptionType.INVENTORY,
            defaultValue = "{\"8013\":1,\"1351\":1,\"5341\":1,\"11194\":1,\"2434\":4,\"385\":15,\"2552\":1}"
        ),
        ScriptConfiguration(
            "Food Name", "The name of the food in your inventory setup to eat.",
            optionType = OptionType.STRING, defaultValue = "Shark"
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
            defaultValue = "Teleport to house",
            allowedValues = ["Teleport to house", "Ectophial"]
        )
    ]
)
class DerangedArchaeologistMagicKiller : AbstractScript() {

    lateinit var config: Config
    lateinit var teleportOptions: Map<String, TeleportOption>
    var hasAttemptedPoolDrink: Boolean = true
    private var startTime: Long = 0
    // --- Constants ---
    val ARCHAEOLOGIST_ID = 7806
    val BOSS_TRIGGER_TILE = Tile(3683, 3707, 0)
    val FIGHT_START_TILE = Tile(3683, 3715, 0)
    var emergencyTeleportJustHappened: Boolean = false
    val FEROX_BANK_AREA = Area(Tile(3123, 3623, 0), Tile(3143, 3643, 0))
    val FEROX_POOL_AREA = Area(Tile(3128, 3637), Tile(3130, 3634))
    val POOL_OF_REFRESHMENT_ID = 39651
    val REQUIRED_PRAYER = Prayer.Effect.PROTECT_FROM_MISSILES
    val SPECIAL_ATTACK_PROJECTILE = 1260
    val SPECIAL_ATTACK_TEXT = "Learn to Read!"
    var totalLootValue: Long = 0

    private var currentTask: String = "Starting..."
    private val tasks: List<Task> = listOf(
        // High priority survival/banking
        EmergencyTeleportTask(this),
        WalkToBankAfterEmergencyTask(this),
        GoToBankTask(this),
        BankTask(this),
        PreBankEquipTask(this),
        EquipItemsTask(this),
        DrinkFromPoolTask(this),

        // Travel
        TravelToBossTask(this),

        // Combat
        DodgeSpecialTask(this),
        DeactivatePrayerTask(this),
        EatTask(this),
        FightTask(this),
        LootTask(this),
        RepositionTask(this), // This was the missing line
    )

    data class TeleportOption(
        val itemNameContains: String,
        val interaction: String,
        val successCondition: () -> Boolean
    )
    // --- NEW: Helper function to format numbers ---
    private fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fm", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fk", number / 1_000.0)
            else -> number.toString()
        }
    }
    override fun onStart() {
        startTime = System.currentTimeMillis()
        config = Config(
            requiredEquipment = getOption("Required Equipment"),
            requiredInventory = getOption("Required Inventory"),
            foodName = getOption("Food Name"),
            eatAtPercent = getOption<Integer>("Eat At %").toInt(),
            emergencyHpPercent = getOption<Integer>("Emergency Teleport HP %").toInt(),
            emergencyTeleportItem = getOption("Emergency Teleport Item")
        )

        teleportOptions = mapOf(
            "Teleport to house" to TeleportOption(
                itemNameContains = "Teleport to house",
                interaction = "Break",
                successCondition = { Players.local().tile().distanceTo(BOSS_TRIGGER_TILE) > 15 }
            )
        )

// --- UPDATED: PaintBuilder now uses the correct formatting function ---
        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task:") { currentTask }
            .trackSkill(Skill.Magic)
            .addString("Loot GP:") { formatNumber(totalLootValue) }
            .addString("GP/hr:") {
                val runtime = System.currentTimeMillis() - startTime
                val gpPerHour = if (runtime > 0) (totalLootValue * 3600000 / runtime) else 0
                formatNumber(gpPerHour)
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
        val nearBank = Players.local().tile().distanceTo(FEROX_BANK_AREA.centralTile) < 10
        return nearBank && !needsStatRestore()
    }
    fun equipmentIsCorrect(): Boolean {
        if (config.requiredEquipment.isEmpty()) { return true }
        for ((requiredId, slotIndex) in config.requiredEquipment) {
            val slot = Equipment.Slot.values()[slotIndex]
            val wornItem = Equipment.itemAt(slot)
            if (isDuelingRing(requiredId)) {
                if (!wornItem.name().contains("Ring of dueling")) return false
            } else if (isDigsitePendant(requiredId)) {
                if (!wornItem.name().contains("Digsite pendant")) return false
            } else {
                if (wornItem.id() != requiredId) return false
            }
        }
        return true
    }
    private fun isDuelingRing(id: Int): Boolean = id in 2552..2566
    private fun isDigsitePendant(id: Int): Boolean = id in 11190..11194
    /**
     * Checks if the current inventory matches the setup defined in the GUI.
     * This is now used directly by banking tasks.
     */
    fun inventoryIsCorrect(): Boolean {
        if (config.requiredInventory.isEmpty()) { return true }
        for ((id, amount) in config.requiredInventory) {
            if (isDuelingRing(id)) {
                if (Inventory.stream().nameContains("Ring of dueling").count(true) < amount) return false
            } else if (isDigsitePendant(id)) {
                if (Inventory.stream().nameContains("Digsite pendant").count(true) < amount) return false
            } else {
                if (Inventory.stream().id(id).count(true) < amount) return false
            }
        }
        return true
    }
    fun needsFullRestock(): Boolean {
        return !equipmentIsCorrect() || !inventoryIsCorrect()
    }

    /**
     * Checks for mid-trip resupply conditions.
     * Returns true only if out of food, out of prayer potions, or full inventory with no food.
     */
    fun needsTripSupplies(): Boolean {
        val noFood = Inventory.stream().name(config.foodName).isEmpty()
        val inventoryFull = Inventory.isFull()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()

        return noPrayerPotions || noFood || (inventoryFull && !noFood)
    }
    fun needsTripResupply(): Boolean {
        val noFood = Inventory.stream().name(config.foodName).isEmpty()
        val noPrayerPotions = Inventory.stream().nameContains("Prayer potion").isEmpty()

        // CORRECTED: The logic is now simple. If you have no food OR no prayer potions, you need to resupply.
        return noFood || noPrayerPotions
    }
    fun needsStatRestore(): Boolean {
        // Only consider restoring stats if we are safely at the bank.
        if (!FEROX_BANK_AREA.contains(Players.local())) {
            return false
        }
        val prayerNotFull = Prayer.prayerPoints() < Skills.realLevel(Skill.Prayer)
        val healthNotFull = Combat.healthPercent() < 100

        return prayerNotFull || healthNotFull
    }    fun getBoss(): Npc? = Npcs.stream().id(ARCHAEOLOGIST_ID).nearest().firstOrNull()
}