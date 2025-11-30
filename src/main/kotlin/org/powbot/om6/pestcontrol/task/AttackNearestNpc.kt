package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
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
        if ((Zeal.percentage() ?: 100) > 30) {
            return false
        }
        nearestNpc = nearestNpc()
        return nearestNpc != Npc.Nil && !Players.local().interacting().valid() && Players.local().animation() == -1
    }

    override fun run() {
        logger.info("Low zeal (${Zeal.percentage()}%), attacking nearest NPC: ${nearestNpc.name()}")
        nearestNpc.interact("Attack") && Condition.wait { Players.local().interacting() == nearestNpc }
    }

    private fun nearestNpc(): Npc {
        // Ignore portals since NPCS seem to take damage faster
        return Npcs.stream().within(7.toDouble()).filtered { it.name != "Void Knight" && it.name != "Portal" }
            .viewable().firstOrNull { it.reachable() } ?: Npc.Nil
    }
}