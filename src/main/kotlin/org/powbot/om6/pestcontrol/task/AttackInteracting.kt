package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players

class AttackInteracting: Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Attacks interacting npc"
    }

    override fun valid(): Boolean {
        return Npcs.stream().interactingWithMe().isNotEmpty() && !Players.local().interacting().valid() && Players.local().animation() == -1
    }

    override fun run() {
        val npc = Npcs.stream().interactingWithMe().first()
        if (npc.valid()) {
            logger.info("Attacking interacting NPC: ${npc.name()}")
            npc.interact("Attack")
            Condition.wait {
                Players.local().interacting().valid()
            }
        }
    }
}