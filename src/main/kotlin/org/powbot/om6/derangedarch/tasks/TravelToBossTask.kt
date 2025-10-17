package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Constants for travel logic ---
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
    private val TRUNK_SAFE_TILE = Tile(3683, 3717, 0)
    private val TRUNK_NAME = "Decaying trunk"
    private val CLIMB_ACTION = "Climb"

    override fun validate(): Boolean {
        val inFightZone = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 9
        // Run if we are not in the fight zone AND all banking/restoring is complete.
        return !inFightZone && !script.needsFullRestock() && !script.needsStatRestore()
    }

    override fun execute() {
        val player = Players.local()

        if (player.tile().distanceTo(TRUNK_SAFE_TILE) < 5) {
            val trunk = Objects.stream().name(TRUNK_NAME).action(CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null && trunk.interact(CLIMB_ACTION)) {
                Condition.wait({ player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8 }, 600, 15)
            }
            return
        }

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
            // Check if the Mushtree is nearby AND the Mushtree widget is NOT already visible.
            if (firstMushtree != null && firstMushtree.distance() < 20 && !Widgets.widget(MUSHTREE_INTERFACE_ID).valid()) {

                // Only interact with the mushtree if the player is not moving.
                if (!player.inMotion() && firstMushtree.interact("Use")) {
                    if (Condition.wait({ Widgets.widget(MUSHTREE_INTERFACE_ID).valid() }, 600, 15)) {
                        // Added the 600ms delay before clicking the widget.
                        Condition.sleep(600)
                        val swampOption = Widgets.widget(MUSHTREE_INTERFACE_ID).component(MUSHTREE_SWAMP_OPTION_COMPONENT)
                        if (swampOption.valid() && swampOption.click()) {
                            Condition.wait({ Objects.stream().id(SECOND_MUSHTREE_ID).isNotEmpty() }, 600, 10)
                        }
                    }
                }
            } else if (firstMushtree != null && firstMushtree.distance() < 20 && Widgets.widget(MUSHTREE_INTERFACE_ID).valid()) {
                // If the widget IS visible, proceed directly to clicking the option.
                Condition.sleep(600) // Keep the delay before clicking the option
                val swampOption = Widgets.widget(MUSHTREE_INTERFACE_ID).component(MUSHTREE_SWAMP_OPTION_COMPONENT)
                if (swampOption.valid() && swampOption.click()) {
                    Condition.wait({ Objects.stream().id(SECOND_MUSHTREE_ID).isNotEmpty() }, 600, 10)
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
                Condition.sleep(600)
                if (Condition.wait({ Widgets.widget(PENDANT_WIDGET_ID).valid() }, 600, 15)) {
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