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
    version = "2.1.1",
    author = "0m6",
    category = ScriptCategory.Combat
)
@ScriptConfiguration.List(
    [
        // --- Setup Tab ---
        ScriptConfiguration(
            "Required Equipment", "Define the gear to wear.",
            optionType = OptionType.EQUIPMENT,
            defaultValue = "{\"4089\":0,\"21795\":1,\"12002\":2,\"11907\":3,\"4091\":4,\"25818\":5,\"4093\":7,\"7462\":9,\"4097\":10,\"9104\":12,\"20232\":13}"
        ),
        ScriptConfiguration(
            "Required Inventory",
            "NOTE: Define your full inventory here. You MUST include:\n- Your food\n- Prayer potions\n- Ring of dueling (for banking)\n- Digsite pendant (for travel, must 'Swap' to Mushtree)\n- Your chosen Emergency Teleport item\n- An axe and/or rake if desired.",
            optionType = OptionType.INVENTORY,
            defaultValue = "{\"8013\":1,\"1351\":1,\"5341\":1,\"2552\":1,\"11194\":1,\"2434\":4,\"385\":15}"
        ),

        // --- Combat & Food Tab ---
        ScriptConfiguration(
            "Food Name", "The name of the food in your inventory setup to eat.",
            optionType = OptionType.STRING, defaultValue = "Shark"
        ),
        ScriptConfiguration(
            "Eat At %", "What health percentage should the script eat at?",
            optionType = OptionType.INTEGER, defaultValue = "65"
        ),

        // --- Safety Tab ---
        ScriptConfiguration(
            "Emergency Teleport HP %", "Teleport out if HP drops below this percentage.",
            optionType = OptionType.INTEGER, defaultValue = "25"
        ),
        ScriptConfiguration(
            "Emergency Teleport Item", "Select the item to use for emergency teleports.",
            optionType = OptionType.STRING,
            defaultValue = "Teleport to house",
            allowedValues = ["Teleport to house", "Ring of dueling", "Ectophial"]
        )
    ]
)
class DerangedArchaeologistMagicKiller : AbstractScript() {

    lateinit var config: Config
    lateinit var teleportOptions: Map<String, TeleportOption>

    // --- Constants ---
    var hasAttemptedPoolDrink: Boolean = true
    val ARCHAEOLOGIST_ID = 7807
    val BOSS_AREA = Area(Tile(3736, 3823), Tile(3761, 3801))
    // CORRECTED: Expanded the bank area to include the bank chests/booths.
    val FEROX_BANK_AREA = Area(Tile(3128, 3638), Tile(3138, 3628))
    val FEROX_POOL_AREA = Area(Tile(3128, 3637), Tile(3130, 3634))
    val POOL_OF_REFRESHMENT_ID = 39651
    val REQUIRED_PRAYER = Prayer.Effect.PROTECT_FROM_MISSILES

    val SPECIAL_ATTACK_PROJECTILE = 1260
    val SPECIAL_ATTACK_TEXT = "Learn to Read!"

    private var currentTask: String = "Starting..."
    private val tasks: List<Task> = listOf(
        EmergencyTeleportTask(this), DodgeSpecialTask(this), FightTask(this), LootTask(this),
        EatTask(this), PotionTask(this), PreBankEquipTask(this), TeleportToBankTask(this),
        BankTask(this), EquipItemsTask(this), DrinkFromPoolTask(this), TravelToBossTask(this)
    )

    data class TeleportOption(
        val itemNameContains: String,
        val interaction: String,
        val isEquippable: Boolean = false,
        val equipmentSlot: Equipment.Slot? = null,
        val successCondition: () -> Boolean
    )

    override fun onStart() {
        // Initialize config from GUI
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
                successCondition = { !BOSS_AREA.contains(Players.local()) }
            ),
            "Ring of dueling" to TeleportOption(
                itemNameContains = "Ring of dueling",
                interaction = "Ferox Enclave",
                isEquippable = true,
                equipmentSlot = Equipment.Slot.RING,
                successCondition = { FEROX_BANK_AREA.contains(Players.local()) }
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
        tasks.firstOrNull { it.validate() }?.let {
            currentTask = it.javaClass.simpleName
            it.execute()
        } ?: run {
            currentTask = "Idle"
            Condition.sleep(150)
        }
    }

    override fun canBreak(): Boolean = FEROX_BANK_AREA.contains(Players.local()) && !needsSupplies() && !needsStatRestore()

    // --- NEW HELPER METHODS ---

    /**
     * Checks if the currently worn equipment matches the setup defined in the GUI.
     */
    // --- NEW HELPER METHODS ---

    /**
     * Checks if the currently worn equipment matches the setup defined in the GUI.
     * This is the CORRECTED version.
     */
    /**
     * Checks if the currently worn equipment matches the setup defined in the GUI.
     * This is the CORRECTED version.
     */
    /**
     * Checks if the currently worn equipment matches the setup defined in the GUI.
     * This is the final, correct version.
     */
    private fun equipmentIsCorrect(): Boolean {
        if (config.requiredEquipment.isEmpty()) {
            logger.warn("Required Equipment setup is empty. Skipping check.")
            return true
        }

        config.requiredEquipment.forEach { (requiredId, slotIndex) ->
            // CORRECTED: The method is values()[slotIndex].
            val slot = Equipment.Slot.values()[slotIndex]
            val wornItem = Equipment.itemAt(slot)

            if (wornItem.id() != requiredId) {
                logger.warn("Equipment check failed: Slot ${slot.name} (requires ID $requiredId) has item ${wornItem.id()} ('${wornItem.name()}').")
                return false
            }
        }

        logger.info("Equipment check passed successfully.")
        return true
    }

    /**
     * Checks if the current inventory matches the setup defined in the GUI.
     */
    private fun inventoryIsCorrect(): Boolean {
        if (config.requiredInventory.isEmpty()) {
            logger.warn("Required Inventory setup is empty. Skipping check.")
            return true
        }

        config.requiredInventory.forEach { (id, amount) ->
            if (Inventory.stream().id(id).count(true) < amount) {
                logger.info("Inventory check failed: Missing item ID $id")
                return false
            }
        }
        logger.info("Inventory check passed successfully.")
        return true
    }

    /**
     * Master "readiness" check. Returns true if banking is needed.
     * Banking is needed if either the equipment OR the inventory is incorrect.
     */
    fun needsSupplies(): Boolean {
        return !equipmentIsCorrect() || !inventoryIsCorrect()
    }
// --- In your main script file, replace the old helper method with this corrected version ---
fun needsStatRestore(): Boolean {
    // CORRECTED: Compare current prayer points to the base prayer level.
    val prayerNotFull = Prayer.prayerPoints() < Skills.realLevel(Skill.Prayer)

    // Health check remains the same as it correctly handles HP boosts.
    val healthNotFull = Combat.healthPercent() < 100

    return prayerNotFull || healthNotFull
}
    fun getBoss(): Npc? = Npcs.stream().id(ARCHAEOLOGIST_ID).nearest().firstOrNull()
}