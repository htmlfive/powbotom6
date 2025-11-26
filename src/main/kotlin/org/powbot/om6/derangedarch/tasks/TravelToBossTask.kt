package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.om6.derangedarch.IDs

class TravelToBossTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    // --- Hardcoded Tiles (These are fine to keep here as they are map-specific coordinates) ---
    private val FIRST_MUSHTREE_TILE = Tile(3764, 3879, 1)
    private val VINE_OBJECT_TILE = Tile(3680, 3743, 0)
    private val POST_VINE_STEP_TILE = Tile(3680, 3725, 0)
    private val TRUNK_SAFE_TILE = Tile(3683, 3717, 0)
    private val ROCK_FALL_PRE_TILE = Tile(3692, 3708, 0)

    override fun validate(): Boolean {
        val needsFullRestock = script.needsFullRestock()
        val inBankArea = script.FEROX_BANK_AREA.contains(Players.local())
        val bossIsGone = script.getBoss() == null

        // This task should run if:
        // 1. We don't need a full restock (inventory/gear is correct)
        // 2. We are not currently in the bank area (or we've finished drinking from the pool)
        // 3. The boss is not present (needs to be traveled to/spawned)
        val shouldRun = !needsFullRestock && (!inBankArea || script.hasAttemptedPoolDrink) && bossIsGone

        if (shouldRun) {
            script.logger.debug("Validate OK: Needs travel (Restock: $needsFullRestock, BankArea: $inBankArea, Pool: ${script.hasAttemptedPoolDrink}, BossGone: $bossIsGone).")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing TravelToBossTask...")
        val player = Players.local()

        // 1. Teleport to Fossil Island if not on Fossil Island (World 1: Zeah/Islands)
        if (player.tile().plane() != 0 || player.tile().x() < 3000) { // Simple check for Fossil Island location
            script.logger.debug("Not on Fossil Island. Attempting to use Digsite Pendant.")

            var pendant: Item = Equipment.itemAt(Equipment.Slot.NECK)
            if (!pendant.name().contains(IDs.DIGSITE_PENDANT_NAME_CONTAINS)) {
                script.logger.debug("Pendant not equipped, checking inventory.")
                pendant = Inventory.stream().nameContains(IDs.DIGSITE_PENDANT_NAME_CONTAINS).firstOrNull() ?: Item.Nil
            }

            if (pendant.valid()) {
                script.logger.debug("Found pendant, interacting 'Rub'.")
                if (pendant.interact("Rub")) {
                    Condition.sleep(600)
                    if (Condition.wait({ Widgets.widget(IDs.DUELING_RING_WIDGET_ID).valid() }, 200, 15)) {
                        script.logger.debug("Pendant widget open.")
                        val islandOption = Widgets.widget(IDs.DUELING_RING_WIDGET_ID).component(IDs.WIDGET_OPTIONS_CONTAINER).component(IDs.PENDANT_FOSSIL_ISLAND_OPTION_INDEX)
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
            return
        }

        // 2. Travel from Mushroom Tree (if on first floor)
        if (player.tile().floor() == 1) {
            script.logger.info("On first floor of mushroom tree, clicking tree to descend.")
            val tree = Objects.stream().id(IDs.MAGIC_MUSHTREE_ID).nearest().firstOrNull()

            if (tree != null) {
                if (!tree.inViewport()) {
                    Camera.turnTo(tree)
                }
                if (tree.interact("Climb-down")) {
                    // Wait for Mushtree UI to appear
                    if (Condition.wait({ Widgets.widget(IDs.MUSHTREE_INTERFACE_ID).valid() }, 200, 15)) {
                        script.logger.debug("Mushtree UI open.")
                        val swampOption = Widgets.widget(IDs.MUSHTREE_INTERFACE_ID).component(IDs.MUSHTREE_SWAMP_OPTION_COMPONENT)
                        if (swampOption.valid() && swampOption.click()) {
                            script.logger.debug("Clicked 'Fossil Island Swamp', waiting for travel...")
                            Condition.wait({ player.tile().distanceTo(script.BOSS_TRIGGER_TILE) < 100 }, 300, 10)
                        }
                    }
                }
            } else {
                script.logger.warn("Could not find the Magic Mushtree.")
                Movement.walkTo(FIRST_MUSHTREE_TILE)
            }
            return
        }

        // 3. Walk to the second mushtree (if not near boss area)
        if (player.tile().distanceTo(script.BOSS_TRIGGER_TILE) > 50) {
            script.logger.info("Walking from second mushtree towards boss area.")
            val tree = Objects.stream().id(IDs.SECOND_MUSHTREE_ID).nearest().firstOrNull()
            if (tree != null) {
                // We're at the swamp tree, walk south
                Movement.walkTo(VINE_OBJECT_TILE)
                script.logger.debug("Walking towards the vines.")
            } else {
                // If the second tree isn't there (maybe we took a wrong path), just walk towards the vines/boss area.
                Movement.walkTo(VINE_OBJECT_TILE)
                script.logger.debug("Mushtree not found, walking towards the vines.")
            }
            Condition.wait({ player.inMotion() || player.tile().distanceTo(VINE_OBJECT_TILE) < 5 }, 150, 20)
            return
        }

        // 4. Climb the vine (if near the vine)
        if (player.tile().distanceTo(VINE_OBJECT_TILE) < 5) {
            script.logger.info("Climbing down the vine.")
            val vine = Objects.stream().at(VINE_OBJECT_TILE).firstOrNull()

            if (vine != null) {
                if (vine.interact("Climb-down")) {
                    Condition.wait({ player.tile().distanceTo(POST_VINE_STEP_TILE) < 5 }, 600, 10)
                }
            } else {
                script.logger.warn("Could not find the vine object. Attempting to walk past.")
                Movement.walkTo(POST_VINE_STEP_TILE)
            }
            return
        }

        // 5. Pass the decaying trunk (deadfall trap)
        if (player.tile().distanceTo(TRUNK_SAFE_TILE) > 5) {
            script.logger.info("Walking to the decaying trunk area.")
            Movement.walkTo(TRUNK_SAFE_TILE)
            Condition.wait({ player.tile().distanceTo(TRUNK_SAFE_TILE) < 5 }, 150, 20)
            return
        } else if (player.tile().distanceTo(script.BOSS_TRIGGER_TILE) > 10) {
            script.logger.info("Attempting to pass the decaying trunk.")
            val trunk = Objects.stream().name(IDs.DECAYING_TRUNK_NAME).nearest().firstOrNull()

            if (trunk != null) {
                if (trunk.interact("Pass")) {
                    Condition.wait({ player.tile().distanceTo(TRUNK_SAFE_TILE) > 5 }, 150, 20)
                }
            } else {
                script.logger.warn("Could not find the Decaying trunk.")
            }
            return
        }

        // 6. Pass the rock fall (if near the rock fall)
        if (player.tile().distanceTo(ROCK_FALL_PRE_TILE) < 5) {
            script.logger.info("Walking to the rock fall area.")
            val rockFall = Objects.stream().id(IDs.ROCK_FALL_ID).nearest().firstOrNull()

            if (rockFall != null) {
                if (rockFall.interact("Pass")) {
                    Condition.wait({ player.tile().distanceTo(script.BOSS_TRIGGER_TILE) < 5 }, 600, 10)
                }
            } else {
                script.logger.warn("Could not find the Rock fall object.")
            }
            return
        }

        // 7. Final step to boss tile (if not there yet)
        if (player.tile().distanceTo(script.BOSS_TRIGGER_TILE) > 3) {
            script.logger.info("Final step to boss trigger tile: ${script.BOSS_TRIGGER_TILE}.")
            Movement.walkTo(script.BOSS_TRIGGER_TILE)
            Condition.wait({ player.inMotion() || player.tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 3 }, 150, 20)
        }
    }
}