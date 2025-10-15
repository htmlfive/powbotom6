package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Constants for travel logic ---
    private val TRUNK_SAFE_TILE = Tile(3683, 3717, 0)
    private val TRUNK_NAME = "Decaying trunk"
    private val CLIMB_ACTION = "Climb"
    // ... other constants ...
    private val PENDANT_WIDGET_ID = 219
    private val PENDANT_FOSSIL_ISLAND_COMPONENT = 1
    private val PENDANT_FOSSIL_ISLAND_OPTION_INDEX = 2
    private val FIRST_MUSHTREE_TILE = Tile(3764, 3879, 1)
    private val MAGIC_MUSHTREE_ID = 30920
    private val MUSHTREE_INTERFACE_ID = 608
    private val MUSHTREE_SWAMP_OPTION_COMPONENT = 12
    private val SECOND_MUSHTREE_ID = 30924
    private val VINE_OBJECT_TILE = Tile(3680, 3743, 0)
    private val POST_VINE_STEP_TILE = Tile(3680, 3725, 0)

    /**
     * This task is valid as long as we are more than 8 tiles away from the trigger spot.
     */
    override fun validate(): Boolean = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) > 8
            && !script.needsSupplies() && script.hasAttemptedPoolDrink

    override fun execute() {
        val player = Players.local()

        // The final step is to climb the trunk.
        if (player.tile().distanceTo(TRUNK_SAFE_TILE) < 5) {
            val trunk = Objects.stream().name(TRUNK_NAME).action(CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null && trunk.interact(CLIMB_ACTION)) {
                // Wait until we arrive within 8 tiles of our destination.
                Condition.wait({ player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8 }, 200, 15)
            }
            return
        }

        // ... rest of the travel logic to get to the trunk remains the same ...
        if (!Movement.running() && Movement.energyLevel() > 40) Movement.running(true)
        val onFossilIsland = Objects.stream().id(MAGIC_MUSHTREE_ID, SECOND_MUSHTREE_ID).isNotEmpty() || player.tile().distanceTo(TRUNK_SAFE_TILE) < 200
        if (onFossilIsland) {
            if (player.tile().x < VINE_OBJECT_TILE.x()) {
                if (player.tile().distanceTo(POST_VINE_STEP_TILE) < 5) {
                    Movement.step(TRUNK_SAFE_TILE)
                } else {
                    Movement.step(POST_VINE_STEP_TILE)
                }
                return
            }
            if (player.tile().distanceTo(VINE_OBJECT_TILE) < 5) {
                val vine = Objects.stream().name("Thick vine").within(VINE_OBJECT_TILE, 1.0).action("Chop").firstOrNull()
                if (vine != null && vine.valid()) {
                    if (player.animation() == -1 && vine.interact("Chop")) {
                        Condition.sleep(1200)
                    }
                } else {
                    Movement.step(POST_VINE_STEP_TILE)
                }
                return
            }
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