package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

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
        val needsRestock = script.needsFullRestock()
        val needsRestore = script.needsStatRestore()

        val shouldRun = !inFightZone && !needsRestock && !needsRestore
        if (shouldRun) {
            script.logger.debug("Validate OK: Not in fight zone, not needing restock or restore.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing TravelToBossTask...")
        val player = Players.local()

        if (player.tile().distanceTo(TRUNK_SAFE_TILE) < 5) {
            script.logger.debug("At safe tile, looking for trunk to climb.")
            val trunk = Objects.stream().name(TRUNK_NAME).action(CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null && trunk.interact(CLIMB_ACTION)) {
                script.logger.debug("Climbing trunk...")
                // MODIFIED: Increased distance check to 9 to match validate()
                Condition.wait({ player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 9 }, 200, 15)
            }
            return
        }

        if (!Movement.running() && Movement.energyLevel() > 40) {
            script.logger.debug("Enabling run energy.")
            Movement.running(true)
        }

        val onFossilIsland = Objects.stream().id(MAGIC_MUSHTREE_ID, SECOND_MUSHTREE_ID).isNotEmpty() || player.tile().distanceTo(TRUNK_SAFE_TILE) < 200

        if (onFossilIsland) {
            script.logger.debug("On Fossil Island, navigating...")

            if (player.tile().x < VINE_OBJECT_TILE.x()) {
                if (player.tile().distanceTo(POST_VINE_STEP_TILE) < 5) {
                    script.logger.debug("Past vine, walking to trunk safe tile.")
                    Movement.step(TRUNK_SAFE_TILE)
                } else {
                    script.logger.debug("Past vine, walking to post-vine tile.")
                    Movement.step(POST_VINE_STEP_TILE)
                }
                return
            }

            if (player.tile().distanceTo(VINE_OBJECT_TILE) < 5) {
                script.logger.debug("At vine tile, looking for vine to chop.")
                val vine = Objects.stream().name("Thick vine").within(VINE_OBJECT_TILE, 1.0).action("Chop").firstOrNull()
                if (vine != null && vine.valid()) {
                    if (player.animation() == -1 && vine.interact("Chop")) {
                        script.logger.debug("Chopping vine...")
                        Condition.sleep(1200)
                    }
                } else {
                    script.logger.debug("Vine not found or invalid, walking past.")
                    Movement.step(POST_VINE_STEP_TILE)
                }
                return
            }

            val firstMushtree = Objects.stream().id(MAGIC_MUSHTREE_ID).nearest().firstOrNull()
            if (firstMushtree != null && firstMushtree.distance() < 20) {
                script.logger.debug("At first mushtree.")
                if (!player.inMotion() && firstMushtree.interact("Use")) {
                    script.logger.debug("Using mushtree, waiting for interface...")
                    if (Condition.wait({ Widgets.widget(MUSHTREE_INTERFACE_ID).valid() }, 200, 15)) {
                        script.logger.debug("Mushtree interface open.")
                        Condition.sleep(600) // Delay for interface
                        val swampOption = Widgets.widget(MUSHTREE_INTERFACE_ID).component(MUSHTREE_SWAMP_OPTION_COMPONENT)
                        if (swampOption.valid() && swampOption.click()) {
                            script.logger.debug("Clicked 'Verdant Valley', waiting for travel...")
                            Condition.wait({ Objects.stream().id(SECOND_MUSHTREE_ID).isNotEmpty() }, 300, 10)
                        }
                    }
                }
            } else {
                script.logger.debug("Not near any landmarks, walking to vine tile.")
                Movement.step(VINE_OBJECT_TILE)
            }
        } else {
            script.logger.debug("Not on Fossil Island. Attempting to use Digsite Pendant.")
            var pendant: Item = Equipment.itemAt(Equipment.Slot.NECK)
            if (!pendant.name().contains("Digsite pendant")) {
                script.logger.debug("Pendant not equipped, checking inventory.")
                pendant = Inventory.stream().nameContains("Digsite pendant").firstOrNull() ?: Item.Nil
            }

            if (pendant.valid()) {
                script.logger.debug("Found pendant, interacting 'Rub'.")
                if (pendant.interact("Rub")) {
                    Condition.sleep(600)
                    if (Condition.wait({ Widgets.widget(PENDANT_WIDGET_ID).valid() }, 200, 15)) {
                        script.logger.debug("Pendant widget open.")
                        val islandOption = Widgets.widget(PENDANT_WIDGET_ID).component(PENDANT_FOSSIL_ISLAND_COMPONENT).component(PENDANT_FOSSIL_ISLAND_OPTION_INDEX)
                        if (islandOption.valid() && islandOption.click()) {
                            script.logger.debug("Clicked 'Fossil Island', waiting for teleport...")
                            Condition.wait({ player.tile().distanceTo(FIRST_MUSHTREE_TILE) < 20 }, 300, 10)
                        }
                    }
                }
            } else {
                script.logger.warn("FATAL: No Digsite Pendant found! Add one to your setup. Stopping script.")
                ScriptManager.stop()
            }
        }
    }
}
