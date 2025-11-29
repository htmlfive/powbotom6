package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.utils.ScriptUtils

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val inFightZone = Players.local().tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.FIGHT_AREA_EXTENDED
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

        if (player.tile().distanceTo(Constants.TRUNK_SAFE_TILE) < 5) {
            script.logger.debug("At safe tile, looking for trunk to climb.")
            val trunk = Objects.stream().name(Constants.TRUNK_NAME).action(Constants.CLIMB_ACTION).nearest().firstOrNull()
            if (trunk != null && trunk.interact(Constants.CLIMB_ACTION)) {
                script.logger.debug("Climbing trunk...")
                Condition.wait({ player.tile().distanceTo(Constants.BOSS_TRIGGER_TILE) <= Constants.FIGHT_AREA_EXTENDED }, 200, 15)
            }
            return
        }

        ScriptUtils.enableRunIfNeeded(script)

        val onFossilIsland = Objects.stream().id(Constants.MAGIC_MUSHTREE_ID, Constants.SECOND_MUSHTREE_ID).isNotEmpty() ||
                player.tile().distanceTo(Constants.TRUNK_SAFE_TILE) < 200

        if (onFossilIsland) {
            script.logger.debug("On Fossil Island, navigating...")

            if (player.tile().x < Constants.VINE_OBJECT_TILE.x()) {
                if (player.tile().distanceTo(Constants.POST_VINE_STEP_TILE) < 5) {
                    script.logger.debug("Past vine, walking to trunk safe tile.")
                    Movement.step(Constants.TRUNK_SAFE_TILE)
                } else {
                    script.logger.debug("Past vine, walking to post-vine tile.")
                    Movement.step(Constants.POST_VINE_STEP_TILE)
                }
                return
            }

            if (player.tile().distanceTo(Constants.VINE_OBJECT_TILE) < 5) {
                script.logger.debug("At vine tile, looking for vine to chop.")
                val vine = Objects.stream().name(Constants.THICK_VINE_NAME).within(Constants.VINE_OBJECT_TILE, 1.0).action(Constants.CHOP_ACTION).firstOrNull()
                if (vine != null && vine.valid()) {
                    if (player.animation() == -1 && vine.interact(Constants.CHOP_ACTION)) {
                        script.logger.debug("Chopping vine...")
                        Condition.sleep(1200)
                    }
                } else {
                    script.logger.debug("Vine not found or invalid, walking past.")
                    Movement.step(Constants.POST_VINE_STEP_TILE)
                }
                return
            }

            val firstMushtree = Objects.stream().id(Constants.MAGIC_MUSHTREE_ID).nearest().firstOrNull()
            if (firstMushtree != null && firstMushtree.distance() < 20) {
                script.logger.debug("At first mushtree.")
                if (!player.inMotion() && firstMushtree.interact(Constants.USE_ACTION)) {
                    script.logger.debug("Using mushtree, waiting for interface...")
                    if (Condition.wait({ Widgets.widget(Constants.MUSHTREE_INTERFACE_ID).valid() }, 200, 15)) {
                        script.logger.debug("Mushtree interface open.")
                        Condition.sleep(600)
                        val swampOption = Widgets.widget(Constants.MUSHTREE_INTERFACE_ID).component(Constants.MUSHTREE_SWAMP_OPTION_COMPONENT)
                        if (swampOption.valid() && swampOption.click()) {
                            script.logger.debug("Clicked 'Verdant Valley', waiting for travel...")
                            Condition.wait({ Objects.stream().id(Constants.SECOND_MUSHTREE_ID).isNotEmpty() }, 300, 10)
                        }
                    }
                }
            } else {
                script.logger.debug("Not near any landmarks, walking to vine tile.")
                Movement.step(Constants.VINE_OBJECT_TILE)
            }
        } else {
            script.logger.debug("Not on Fossil Island. Attempting to use Digsite Pendant.")
            val pendant = ScriptUtils.getDigsitePendant()

            if (pendant.valid()) {
                script.logger.debug("Found pendant, interacting 'Rub'.")
                if (pendant.interact(Constants.RUB_ACTION)) {
                    Condition.sleep(600)
                    if (Condition.wait({ Widgets.widget(Constants.PENDANT_WIDGET_ID).valid() }, 200, 15)) {
                        script.logger.debug("Pendant widget open.")
                        val islandOption = Widgets.widget(Constants.PENDANT_WIDGET_ID)
                            .component(Constants.PENDANT_FOSSIL_ISLAND_COMPONENT)
                            .component(Constants.PENDANT_FOSSIL_ISLAND_OPTION)
                        if (islandOption.valid() && islandOption.click()) {
                            script.logger.debug("Clicked 'Fossil Island', waiting for teleport...")
                            Condition.wait({ player.tile().distanceTo(Constants.FIRST_MUSHTREE_TILE) < 20 }, 300, 10)
                        }
                    }
                }
            } else {
                ScriptUtils.stopScript("No Digsite Pendant found! Add one to your setup.", script)
            }
        }
    }
}