package org.powbot.om6.pestcontrol

import org.powbot.api.Condition
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Players

object ScriptUtils {
    
    /**
     * Checks if the player is idle (not interacting and not animating)
     */
    fun isPlayerIdle(): Boolean {
        return !Players.local().interacting().valid() && Players.local().animation() == Constants.IDLE_ANIMATION
    }

    /**
     * Attacks an NPC and waits for the player to start interacting with it
     */
    fun attackNpc(npc: Npc): Boolean {
        return npc.interact(Constants.ATTACK_ACTION) && 
               Condition.wait { Players.local().interacting() == npc }
    }

    /**
     * Checks if a player is currently attacking or in combat animation
     */
    fun isInCombat(): Boolean {
        return Players.local().animation() != Constants.IDLE_ANIMATION || 
               Players.local().inMotion()
    }
}
