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

    // CORRECTED: Use the parent widget ID and specific component index from your image.
    private val MUSHTREE_INTERFACE_ID = 608
    private val MUSHTREE_SWAMP_OPTION_COMPONENT = 12 // This corresponds to the widget with UID 39845900

    private val STICKY_SWAMP_TILE = Tile(3676, 3871, 0)
    private val VINE_TILE = Tile(3681, 3869, 0)
    private val VINE_NAME = "Thick vine"
    private val CHOP_ACTION = "Chop"

    private val TRUNK_SAFE_TILE = Tile(3682, 3717, 0)
    private val TRUNK_NAME = "Decaying trunk"
    private val CLIMB_ACTION = "Climb"

    override fun validate(): Boolean = !script.BOSS_AREA.contains(Players.local()) && !script.needsSupplies() && script.hasAttemptedPoolDrink

    override fun execute() {
        if (!Movement.running() && Movement.energyLevel() > 40) Movement.running(true)
        val player = Players.local()

        // State 4: Past the vine, at the trunk safe spot.
        if (player.tile().distanceTo(TRUNK_SAFE_TILE) < 5) {
            val trunk = Objects.stream().name(TRUNK_NAME).action(CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null) {
                if (!trunk.inViewport()) Camera.turnTo(trunk)
                if (trunk.interact(CLIMB_ACTION)) {
                    Condition.wait({ script.BOSS_AREA.contains(player) }, 200, 15)
                }
            }
            return
        }

        // State 3: Arrived in Sticky Swamp.
        if (player.tile().distanceTo(STICKY_SWAMP_TILE) < 30) {
            val vine = Objects.stream().name(VINE_NAME).at(VINE_TILE).action(CHOP_ACTION).firstOrNull()
            if (vine != null && vine.valid()) {
                if (vine.interact(CHOP_ACTION)) {
                    Condition.wait({ player.tile().x < VINE_TILE.x() }, 200, 15)
                }
            } else {
                Movement.step(TRUNK_SAFE_TILE)
            }
            return
        }

        // State 2: Arrived at the first mushtree.
        if (player.tile().distanceTo(FIRST_MUSHTREE_TILE) < 20) {
            val mushtree = Objects.stream().id(MAGIC_MUSHTREE_ID).nearest().firstOrNull()
            if (mushtree != null && mushtree.interact("Use")) {
                if (Condition.wait({ Widgets.widget(MUSHTREE_INTERFACE_ID).valid() }, 200, 15)) {
                    // CORRECTED: Target the specific component by its parent ID and index.
                    val swampOption = Widgets.widget(MUSHTREE_INTERFACE_ID).component(MUSHTREE_SWAMP_OPTION_COMPONENT)
                    if (swampOption.valid() && swampOption.click()) {
                        Condition.wait({ player.tile().distanceTo(STICKY_SWAMP_TILE) < 10 }, 300, 10)
                    }
                }
            }
            return
        }

        // State 1: Not on Fossil Island. Use the pendant.
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