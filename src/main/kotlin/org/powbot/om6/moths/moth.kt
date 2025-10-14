package org.powbot.om6.moth

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.paint.PaintBuilder
import kotlin.random.Random
import kotlin.random.nextInt


@ScriptManifest(
    name = "0m6 Moonlight Moth Catcher",
    description = "Catches Moonlight Moths and manages banking",
    version = "1.0.7",
    author = "0m6",
    category = ScriptCategory.Hunter
)
class MoonlightMothCatcher : AbstractScript() {

    private val MOONLIGHT_MOTH_ID = 12771
    private val BUTTERFLY_JAR_NAME = "Butterfly jar"
    private val MOONLIGHT_MOTH_NAME = "Moonlight moth"
    private val MINIMUM_Y_AXIS = 9437 // Do not catch moths below this Y axis
    private var lastActionTime = System.currentTimeMillis() // To track the time of the last action

    // Define the tile to return to where moths are located (Adjust this to your desired location)
    private val mothLocation = Tile(1574, 9446)
    private val pathToMoths = arrayOf(
        Tile(1574, 9446)   // Final tile near the moths
    )


    override fun poll() {

        val randomEnergyThreshold = Random.nextInt(20, 36)  // Randomize between 20 and 35

        if (Players.local().tile().distanceTo(mothLocation) > 15 &&
            Inventory.stream().name(BUTTERFLY_JAR_NAME).isNotEmpty()) {
            logger.info("Not near moths and have jars, traversing to moths.")
            moveToMothLocation()
            return
        }
        if (Movement.energyLevel() > randomEnergyThreshold && !Movement.running()) {
            logger.info("Enabling running as energy level is above $randomEnergyThreshold%.")
            Movement.running(true)  // Enable running
        }

        // If another player is nearby and you're near the moths, hop worlds
        if (playerNearby() && Players.local().tile().distanceTo(mothLocation) < 10) {
            logger.info("Player detected nearby, hopping to a random world.")
            hopToRandomWorld()
            return
        }

        // Check if the inventory contains a Butterfly jar, if not, go to the bank
        if (!Inventory.stream().name(BUTTERFLY_JAR_NAME).isNotEmpty()) {
            logger.info("No Butterfly jar found. Going to bank.")
            goToBank()
            return
        }


        // Find the nearest valid Moonlight Moth NPC by its ID
        val moonlightMoth = Npcs.stream().id(MOONLIGHT_MOTH_ID).nearest().filter {
            it.tile().y() >= MINIMUM_Y_AXIS // Filter out moths below the Y-axis
        }.firstOrNull() // Use firstOrNull() to avoid exception when list is empty

        if (moonlightMoth == null || !moonlightMoth.valid()) {
            logger.info("No valid Moonlight Moth found above Y-axis $MINIMUM_Y_AXIS.")
            // If no valid moths, move to the designated moth location
            moveToMothLocation()
            return
        }

        if (!moonlightMoth.inViewport()) {
            // Turn the camera to the Moonlight Moth if it's not in view
            Camera.turnTo(moonlightMoth)

        } else {
            // Interact with the Moonlight Moth using the "Catch" action
            if (moonlightMoth.interact("Catch")) {
                logger.info("Catching a Moonlight Moth.")
                lastActionTime = System.currentTimeMillis() // Update the time of the last action
                // Wait until the moth is caught or no longer valid
                Condition.wait { !moonlightMoth.valid() || Players.local().animation() != -1  }
                Condition.sleep(Random.nextInt(350,550))

            }


        }
        Condition.sleep(Random.nextInt(80,220))
    }

    private fun playerNearby(): Boolean {
        val nearbyPlayer = Players.stream()
            .within(10) // Detect players within 10 tiles
            .firstOrNull { it != Players.local() }
        return nearbyPlayer != null
    }
    private fun hopToRandomWorld() {
        val randomWorld = Worlds.stream()
            .filtered {
                it.type() == World.Type.MEMBERS && it.population >= 15 &&
                        it.specialty() != World.Specialty.BOUNTY_HUNTER &&
                        it.specialty() != World.Specialty.PVP &&
                        it.specialty() != World.Specialty.TARGET_WORLD &&
                        it.specialty() != World.Specialty.PVP_ARENA &&
                        it.specialty() != World.Specialty.DEAD_MAN &&
                        it.specialty() != World.Specialty.BETA &&
                        it.specialty() != World.Specialty.HIGH_RISK &&
                        it.specialty() != World.Specialty.LEAGUE &&
                        it.specialty() != World.Specialty.SKILL_REQUIREMENT &&
                        it.specialty() != World.Specialty.SPEEDRUNNING &&
                        it.specialty() != World.Specialty.FRESH_START &&
                        it.specialty() != World.Specialty.TRADE
            }
            .toList()
            .randomOrNull()

        if (randomWorld != null) {
            logger.info("Hopping to world ${randomWorld.number}.")
            randomWorld.hop()
            Condition.wait { !Players.local().inMotion() } // Wait for the hop to complete
        } else {
            logger.info("No suitable world found for hopping.")
        }
    }
    private fun climbDownStairs() {

        val stairsDown = Objects.stream().name("Stairs").action("Climb-down").nearest().first()

        if (stairsDown.valid()) {
            logger.info("Stairs detected. Attempting to climb down.")

            // Keep attempting to climb down until the action is successful
            if (!stairsDown.inViewport()) {
                // Turn the camera towards the stairs if not in view
                logger.info("Turning camera to stairs.")
                Camera.turnTo(stairsDown)
            }

            // Ensure we step directly to the stairs instead of using minimap
            if (Players.local().tile().distanceTo(stairsDown.tile()) > 3) {
                logger.info("Stepping directly to the stairs.")
                Movement.step(stairsDown.tile()) // Move directly to the stairs tile
                Condition.wait { Players.local().tile().distanceTo(stairsDown.tile()) <= 5 }
            }

            // Try interacting with the stairs to "Climb-down"
            if (stairsDown.interact("Climb-down")) {
                logger.info("Climbing down the stairs.")
                // Wait for the action to be performed, i.e., the player stops moving
                Condition.wait { !Players.local().inMotion() && Players.local().animation() == -1 && !stairsDown.inViewport() }
                Condition.sleep(Random.nextInt(1200,1800))
                traverseToMoths()

                return // Exit the function if the action is successful
            } else {
                // If the interaction fails, wait a bit and retry
                logger.info("Failed to climb down. Retrying...")
                Condition.sleep(1) // Optional sleep before retrying
            }
        } else {
            logger.info("No stairs detected to climb down.")
        }
    }
    private fun climbUpStairs() {
        val stairsUp = Objects.stream().name("Stairs").action("Climb-up").nearest().first()

        if (stairsUp.valid()) {
            logger.info("Stairs detected. Attempting to climb up.")

            // Keep attempting to climb up until the action is successful

            if (!stairsUp.inViewport()) {
                // Turn the camera towards the stairs if not in view
                logger.info("Turning camera to stairs.")
                Camera.turnTo(stairsUp)
            }

            // Ensure we step directly to the stairs instead of using minimap
            if (Players.local().tile().distanceTo(stairsUp.tile()) > 1) {
                logger.info("Stepping directly to the stairs.")
                Movement.step(stairsUp.tile()) // Move directly to the stairs tile
                Condition.wait { Players.local().tile().distanceTo(stairsUp.tile()) <= 3 }
            }

            // Try interacting with the stairs to "Climb-up"
            if (stairsUp.interact("Climb-up")) {
                logger.info("Climbing up the stairs.")
                // Wait for the action to be performed, i.e., the player stops moving
                Condition.wait { !Players.local().inMotion() && Players.local().animation() == -1 && !stairsUp.inViewport() }
                Condition.sleep(Random.nextInt(1200,1800))
                return // Exit the function if the action is successful
            } else {
                // If the interaction fails, wait a bit and retry
                logger.info("Failed to climb up. Retrying...")

            }
        } else {
            logger.info("No stairs detected to climb up.")
        }
    }
    private fun goToBank() {
        // Check if the player is near the bank, skip climbing the stairs if so
        if (Players.local().tile().distanceTo(Bank.nearest().tile()) > 10) {
            // Only climb up the stairs if the player is far from the bank
            climbUpStairs()
        } else {
            logger.info("Already near the bank, skipping stairs.")
        }

        // Proceed to the bank after climbing the stairs
        if (!Bank.opened()) {
            logger.info("Walking to the nearest bank.")
            //Movement.moveTo(Bank.nearest().tile())
            Condition.wait { Bank.inViewport() || Players.local().tile().distanceTo(Bank.nearest()) < 6 }
        }

        if (Bank.open()) {
            // Ensure all Moonlight Moths are deposited
            while (Inventory.stream().name(MOONLIGHT_MOTH_NAME).isNotEmpty()) {
                logger.info("Depositing all Moonlight moths.")
                Bank.depositInventory()
                Condition.wait { Inventory.stream().name(MOONLIGHT_MOTH_NAME).isEmpty() }
            }

            // Ensure Butterfly Jars are withdrawn
            while (!Inventory.stream().name(BUTTERFLY_JAR_NAME).isNotEmpty()) {
                logger.info("Withdrawing Butterfly jars.")
                Bank.withdraw(BUTTERFLY_JAR_NAME, Bank.Amount.ALL)
                Condition.wait { Inventory.stream().name(BUTTERFLY_JAR_NAME).isNotEmpty() }
            }

            // Close the bank once all actions are completed
            Bank.close()

            // Proceed to the moth location after banking and climbing down
            moveToMothLocation()
        }
    }
    private fun traverseToMoths() {
        for (tile in pathToMoths) {
            // If the player is not already moving, step to the next tile
            if (!Players.local().inMotion() && Players.local().tile().distanceTo(tile) > 1) {
                logger.info("Walking to tile $tile")
                Movement.step(tile)
                // Wait until the player reaches the tile or is very close
                Condition.wait { Players.local().tile().distanceTo(tile) < 5 }
            } else {
                logger.info("Already moving or close to the tile $tile, skipping step.")
            }
        }

    }
    private fun moveToMothLocation() {
        logger.info("Moving to the moth location.")
        val stairsDown = Objects.stream().name("Stairs").action("Climb-down").nearest().first()

        if (stairsDown.valid()) { climbDownStairs() }

        traverseToMoths()  // Traverse the path to the moth location
    }

    override fun onStart() {
        Camera.pitch(99) // Set the camera pitch to a high angle
        val paint = PaintBuilder.newBuilder()
            .x(40)
            .y(80)
            .trackSkill(org.powbot.api.rt4.walking.model.Skill.Hunter) // Assuming catching moths trains Hunter
            .build()
        addPaint(paint)
        logger.info("Moonlight Moth Catcher script started.")
    }

    override fun onStop() {
        logger.info("Moonlight Moth Catcher script stopped.")
    }
}

fun main() {
    val script = MoonlightMothCatcher()
    script.startScript("localhost", "0m6", false)
}