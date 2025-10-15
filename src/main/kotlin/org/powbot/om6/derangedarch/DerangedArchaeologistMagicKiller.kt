package org.powbot.om6.derangedarch

import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.tasks.*

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

    // --- Constants ---
    val ARCHAEOLOGIST_ID = 7806
    val BOSS_TRIGGER_TILE = Tile(3683, 3707, 0)
    val FIGHT_START_TILE = Tile(3683, 3715, 0)
    var emergencyTeleportJustHappened: Boolean = false
    val FEROX_BANK_AREA = Area(Tile(3128, 3638), Tile(3138, 3628))
    val FEROX_POOL_AREA = Area(Tile(3128, 3637), Tile(3130, 3634))
    val POOL_OF_REFRESHMENT_ID = 39651
    val REQUIRED_PRAYER = Prayer.Effect.PROTECT_FROM_MISSILES
    val SPECIAL_ATTACK_PROJECTILE = 1260
    val SPECIAL_ATTACK_TEXT = "Learn to Read!"

    private var currentTask: String = "Starting..."
    private val tasks: List<Task> = listOf(
        EmergencyTeleportTask(this),
        WalkToBankAfterEmergencyTask(this), // New recovery task
        DodgeSpecialTask(this),
        DeactivatePrayerTask(this),
        EatTask(this), // Moved up
        FightTask(this),
        LootTask(this),
        PreBankEquipTask(this),
        BankTask(this),
        EquipItemsTask(this),
        DrinkFromPoolTask(this),
        TravelToBossTask(this)
    )

    data class TeleportOption(
        val itemNameContains: String,
        val interaction: String,
        val successCondition: () -> Boolean
    )

    override fun onStart() {
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

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task:") { currentTask }
            .trackSkill(Skill.Hitpoints).trackSkill(Skill.Magic).trackSkill(Skill.Prayer)
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


    fun needsStatRestore(): Boolean = Prayer.prayerPoints() < Skills.realLevel(Skill.Prayer) || Combat.healthPercent() < 100
    fun getBoss(): Npc? = Npcs.stream().id(ARCHAEOLOGIST_ID).nearest().firstOrNull()
}