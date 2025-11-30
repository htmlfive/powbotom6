package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players

/**
 * Common NPC utilities shared across all scripts.
 */
object NpcUtils {

    // ========================================
    // FINDING NPCS
    // ========================================

    /**
     * Finds the nearest NPC by name.
     * @param name The NPC name
     * @return The NPC or null if not found
     */
    fun findByName(name: String): Npc? {
        return Npcs.stream().name(name).nearest().firstOrNull()
    }

    /**
     * Finds the nearest NPC by ID.
     * @param id The NPC ID
     * @return The NPC or null if not found
     */
    fun findById(id: Int): Npc? {
        return Npcs.stream().id(id).nearest().firstOrNull()
    }

    /**
     * Finds the nearest NPC by name with a specific action.
     * @param name The NPC name
     * @param action The required action
     * @return The NPC or null if not found
     */
    fun findByNameAndAction(name: String, action: String): Npc? {
        return Npcs.stream().name(name).action(action).nearest().firstOrNull()
    }

    /**
     * Finds NPCs within range.
     * @param range The maximum distance
     * @return List of NPCs within range
     */
    fun findWithinRange(range: Int): List<Npc> {
        return Npcs.stream().within(range).toList()
    }

    /**
     * Finds NPCs by ID within range.
     * @param id The NPC ID
     * @param range The maximum distance
     * @return List of matching NPCs
     */
    fun findByIdWithinRange(id: Int, range: Int): List<Npc> {
        return Npcs.stream().id(id).within(range).toList()
    }

    // ========================================
    // INTERACTION
    // ========================================

    /**
     * Ensures an NPC is in viewport, turning camera if needed.
     * @param npc The NPC
     * @return true if NPC is now in viewport
     */
    fun ensureInViewport(npc: Npc): Boolean {
        if (!npc.valid()) return false

        if (!npc.inViewport()) {
            Camera.turnTo(npc)
            Condition.sleep(Random.nextInt(200, 400))
        }
        return npc.inViewport()
    }

    /**
     * Interacts with an NPC by name.
     * @param npcName The NPC name
     * @param action The action to perform
     * @return true if interaction was successful
     */
    fun interact(npcName: String, action: String): Boolean {
        val npc = findByName(npcName) ?: return false
        if (!npc.valid()) return false

        ensureInViewport(npc)
        return npc.interact(action)
    }

    /**
     * Interacts with an NPC by ID.
     * @param npcId The NPC ID
     * @param action The action to perform
     * @return true if interaction was successful
     */
    fun interact(npcId: Int, action: String): Boolean {
        val npc = findById(npcId) ?: return false
        if (!npc.valid()) return false

        ensureInViewport(npc)
        return npc.interact(action)
    }

    /**
     * Attacks an NPC and waits for interaction.
     * @param npc The NPC to attack
     * @param timeout Maximum wait time in ms (default: 3000)
     * @return true if now attacking the NPC
     */
    fun attack(npc: Npc, timeout: Int = 3000): Boolean {
        if (!npc.valid()) return false

        ensureInViewport(npc)
        if (npc.interact("Attack")) {
            return Condition.wait(
                { Players.local().interacting() == npc },
                100,
                timeout / 100
            )
        }
        return false
    }

    /**
     * Interacts with an NPC and waits for dialogue.
     * @param npcName The NPC name
     * @param action The action to perform (default: "Talk-to")
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if dialogue opened
     */
    fun talkTo(npcName: String, action: String = "Talk-to", timeout: Int = 5000): Boolean {
        if (!interact(npcName, action)) return false

        return Condition.wait(
            { org.powbot.api.rt4.Chat.chatting() },
            100,
            timeout / 100
        )
    }

    // ========================================
    // STATUS CHECKS
    // ========================================

    /**
     * Checks if player is currently interacting with an NPC.
     * @param npc The NPC to check
     * @return true if player is interacting with the NPC
     */
    fun isInteractingWith(npc: Npc): Boolean {
        return Players.local().interacting() == npc
    }

    /**
     * Checks if player is currently interacting with any NPC.
     * @return true if player is interacting with an NPC
     */
    fun isInteractingWithAny(): Boolean {
        val interacting = Players.local().interacting()
        return interacting.valid() && interacting is Npc
    }

    /**
     * Gets the NPC the player is currently interacting with.
     * @return The NPC or null if not interacting with an NPC
     */
    fun getInteracting(): Npc? {
        val interacting = Players.local().interacting()
        return if (interacting.valid() && interacting is Npc) interacting else null
    }
}
