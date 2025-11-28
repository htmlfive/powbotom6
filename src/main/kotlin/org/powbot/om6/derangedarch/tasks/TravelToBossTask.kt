package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightZone = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.DISTANCETOBOSS
        val needsRestock = script.needsFullRestock()
        val needsRestore = script.needsStatRestore()

        return !inFightZone && !needsRestock && !needsRestore
    }

    override fun execute() {
        val player = Players.local()

        if (player.tile().distanceTo(Constants.TRUNK_SAFE_TILE) < 5) {
            val trunk = Objects.stream().name(Constants.TRUNK_NAME).action(Constants.CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null && trunk.interact(Constants.CLIMB_ACTION)) {
                Condition.wait({ player.tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= 9 }, 200, 15)
            }
            return
        }

        if (!Movement.running() && Movement.energyLevel() > 40) {
            Movement.running(true)
        }

        val onFossilIsland = Objects.stream().id(Constants.MAGIC_MUSHTREE_ID, Constants.SECOND_MUSHTREE_ID).isNotEmpty() ||
                player.tile().distanceTo(Constants.TRUNK_SAFE_TILE) < 200

        if (onFossilIsland) {
            if (player.tile().x < Constants.VINE_OBJECT_TILE.x()) {
                if (player.tile().distanceTo(Constants.POST_VINE_STEP_TILE) < 5) {
                    Movement.step(Constants.TRUNK_SAFE_TILE)
                } else {
                    Movement.step(Constants.POST_VINE_STEP_TILE)
                }
                return
            }

            if (player.tile().distanceTo(Constants.VINE_OBJECT_TILE) < 5) {
                val vine = Objects.stream().name("Thick vine").within(Constants.VINE_OBJECT_TILE, 1.0).action("Chop").firstOrNull()
                if (vine != null && vine.valid()) {
                    if (player.animation() == -1 && vine.interact("Chop")) {
                        Condition.sleep(1200)
                    }
                } else {
                    Movement.step(Constants.POST_VINE_STEP_TILE)
                }
                return
            }

            val firstMushtree = Objects.stream().id(Constants.MAGIC_MUSHTREE_ID).nearest().firstOrNull()
            if (firstMushtree != null && firstMushtree.distance() < 20) {
                if (!player.inMotion() && firstMushtree.interact("Use")) {
                    if (Condition.wait({ Widgets.widget(Constants.MUSHTREE_INTERFACE_ID).valid() }, 200, 15)) {
                        Condition.sleep(600)
                        val swampOption = Widgets.widget(Constants.MUSHTREE_INTERFACE_ID).component(Constants.MUSHTREE_SWAMP_OPTION_COMPONENT)
                        if (swampOption.valid() && swampOption.click()) {
                            Condition.wait({ Objects.stream().id(Constants.SECOND_MUSHTREE_ID).isNotEmpty() }, 300, 10)
                        }
                    }
                }
            } else {
                Movement.step(Constants.VINE_OBJECT_TILE)
            }
        } else {
            var pendant: Item = Equipment.itemAt(Equipment.Slot.NECK)
            if (!pendant.name().contains("Digsite pendant")) {
                pendant = Inventory.stream().nameContains("Digsite pendant").firstOrNull() ?: Item.Nil
            }

            if (pendant.valid()) {
                if (pendant.interact("Rub")) {
                    Condition.sleep(600)
                    if (Condition.wait({ Widgets.widget(Constants.PENDANT_WIDGET_ID).valid() }, 200, 15)) {
                        val islandOption = Widgets.widget(Constants.PENDANT_WIDGET_ID)
                            .component(Constants.PENDANT_FOSSIL_ISLAND_COMPONENT)
                            .component(Constants.PENDANT_FOSSIL_ISLAND_OPTION_INDEX)
                        if (islandOption.valid() && islandOption.click()) {
                            Condition.wait({ player.tile().distanceTo(Constants.FIRST_MUSHTREE_TILE) < 20 }, 300, 10)
                        }
                    }
                }
            } else {
                script.logger.warn("FATAL: No Digsite Pendant found. Stopping.")
                ScriptManager.stop()
            }
        }
    }
}