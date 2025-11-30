package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.om6.pestcontrol.Constants
import org.powbot.om6.pestcontrol.ScriptUtils
import org.powbot.om6.pestcontrol.helpers.Zeal

/**
 *  Attacks nearest NPC if low zeal
 */
class AttackNearestNpc : Task {
    val logger = org.slf4j.LoggerFactory.getLogger("AttackNearestNpc")

    private var nearestNpc: Npc = Npc.Nil

    override fun name(): String {
        return "Gathering activity"
    }

    override fun valid(): Boolean {
        if ((Zeal.percentage() ?: 100) > Constants.LOW_ZEAL_THRESHOLD) {
            return false
        }
        nearestNpc = nearestNpc()
        return nearestNpc != Npc.Nil && ScriptUtils.isPlayerIdle()
    }

    override fun run() {
        logger.info("Low zeal (${Zeal.percentage()}%), attacking nearest NPC: ${nearestNpc.name()}")
        ScriptUtils.attackNpc(nearestNpc)
    }

    private fun nearestNpc(): Npc {
        // Ignore portals since NPCS seem to take damage faster
        return Npcs.stream().within(Constants.NEAREST_NPC_SEARCH_DISTANCE.toDouble())
            .filtered { it.name != Constants.VOID_KNIGHT_NAME && it.name != Constants.PORTAL_NAME }
            .viewable().firstOrNull { it.reachable() } ?: Npc.Nil
    }
}