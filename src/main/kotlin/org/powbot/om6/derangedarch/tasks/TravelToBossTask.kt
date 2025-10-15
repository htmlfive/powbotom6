package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Constants for the travel logic ---
    private val PENDANT_WIDGET_ID = 219
    private val PENDANT_FOSSIL_ISLAND_COMPONENT = 1
    private val PENDANT_FOSSIL_ISLAND_OPTION_INDEX = 2

    private val FIRST_MUSHTREE_TILE = Tile(3764, 3879, 1)
    private val MAGIC_MUSHTREE_ID = 30920
    private val MUSHTREE_INTERFACE_ID = 608
    private val MUSHTREE_SWAMP_OPTION_COMPONENT = 12
    private val SECOND_MUSHTREE_ID = 30924

    private val VINE_OBJECT_TILE = Tile(3680, 3743, 0)
    private val VINE_NAME = "Thick vine"
    private val CHOP_ACTION = "Chop"

    private val POST_VINE_STEP_TILE = Tile(3680, 3725, 0)

    /**
     * This task now stops running once the player is at the FIGHT_START_TILE.
     */
    override fun validate(): Boolean = Players.local().tile() != script.FIGHT_START_TILE && !script.needsSupplies() && script.hasAttemptedPoolDrink

    override fun execute() {
        if (!Movement.running() && Movement.energyLevel() > 40) Movement.running(true)
        val player = Players.local()

        val onFossilIsland = Objects.stream().id(MAGIC_MUSHTREE_ID, SECOND_MUSHTREE_ID).isNotEmpty() || player.tile().distanceTo(script.FIGHT_START_TILE) < 200

        if (onFossilIsland) {
            // We are on Fossil Island, proceed with local travel steps.

            // Are we at the intermediate tile past the vine?
            if (player.tile().distanceTo(POST_VINE_STEP_TILE) < 5) {
                Movement.step(script.FIGHT_START_TILE)
                return
            }

            // Have we just passed the vine?
            if (player.tile().x < VINE_OBJECT_TILE.x()) {
                Movement.step(POST_VINE_STEP_TILE)
                return
            }

            // Are we near the vine and need to chop it?
            if (player.tile().distanceTo(VINE_OBJECT_TILE) < 5) {
                val vine = Objects.stream().name(VINE_NAME).within(VINE_OBJECT_TILE, 1.0).action(CHOP_ACTION).firstOrNull()
                if (vine != null && vine.valid()) {
                    if (player.animation() == -1 && vine.interact(CHOP_ACTION)) {
                        Condition.sleep(1200)
                    }
                } else {
                    Movement.step(POST_VINE_STEP_TILE)
                }
                return
            }

            // If none of the above, we must be somewhere else on the island. Navigate towards the vine.
            val firstMushtree = Objects.stream().id(MAGIC_MUSHTREE_ID).nearest().firstOrNull()
            if (firstMushtree != null && firstMushtree.distance() < 20) {
                if (firstMushtree.interact("Use")) {
                    if (Condition.wait({ Widgets.widget(MUSHTREE_INTERFACE_ID).valid() }, 200, 15)) {
                        val swampOption = Widgets.widget(MUSHTREE_INTERFACE_ID).component(MUSHTREE_SWAMP_OPTION_COMPONENT)
                        if (swampOption.valid() && swampOption.click()) {
                            Condition.wait({ Objects.stream().id(SECOND_MUSHTREE_ID).isNotEmpty() }, 300, 10)
                        }
                    }
                }
            } else {
                Movement.step(VINE_OBJECT_TILE)
            }

        } else {
            // We are NOT on Fossil Island. Use the pendant.
            var pendant: Item = Equipment.itemAt(Equipment.Slot.NECK)
            if (!pendant.name().contains("Digsite pendant")) {
                pendant = Inventory.stream().nameContains("Digsite pendant").firstOrNull() ?: Item.Nil
            }
            if (pendant.valid() && pendant.interact("Rub")) {
                if (Condition.wait({ Widgets.widget(PENDANT_WIDGET_ID).valid() }, 200, 15)) {
                    val islandOption = Widgets.widget(PENDANT_WIDGET_ID).component(PENDANT_FOSSIL_ISLAND_COMPONENT).component(PENDANT_FOSSIL_ISLAND_OPTION_INDEX)
                    if (islandOption.valid() && islandOption.click()) {
                        Condition.wait({ player.tile().distanceTo(FIRST_MUSHTREE_TILE) < 20 }, 300, 10)
                    }
                }
            } else {
                script.logger.warn("No Digsite Pendant found! Add one to your setup.")
                ScriptManager.stop()
            }
        }
    }
}