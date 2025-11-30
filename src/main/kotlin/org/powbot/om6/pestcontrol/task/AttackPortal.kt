package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.om6.pestcontrol.data.Activity
import org.powbot.om6.pestcontrol.data.Portal
import org.powbot.om6.pestcontrol.helpers.nextMonster
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth
import org.powbot.om6.pestcontrol.helpers.walkToPortal

class AttackPortal(val activity: Activity): Task {

    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    var portal: Portal? = null

    override fun name(): String {
        return "Attacking Portal (${portal?.name ?: "-"})"
    }

    override fun valid(): Boolean {
        return activity == Activity.AttackPortal &&  Components.voidKnightHealth().visible() && !Players.local().interacting().valid() && Players.local().animation() == -1
    }

    override fun run() {
        if (portal == null || portal?.health() == 0) {
            portal = Portal.openPortals().randomOrNull() ?:
                    Portal.values().random()
            logger.info("Selected new portal: ${portal?.name}")
            return
        }

        val portalDistance = portal!!.tile().distance()
        if (portalDistance <= 4) {
            if (portal!!.health() > 0 && !portal!!.hasShield() && attackPortal()) {
                logger.info("Attacking portal: ${portal!!.name}, health: ${portal!!.health()}")
                return
            }
        }

        if (fightBrawler()) {
            logger.info("Fighting nearby Brawler")
            return
        }
        if (fightSpinner()) {
            logger.info("Fighting Spinner (3+ nearby)")
            return
        }

        if (portal!!.tile().distance() <= 4) {
            val monster = Npcs.nextMonster(portal!!.tile())
            if (monster.valid()) {
                logger.info("Attacking monster near portal: ${monster.name()}")
                monster.interact("Attack") &&
                        Condition.wait { Players.local().interacting() == monster }
                return
            } else if (portal?.health() == 0) {
                logger.info("Portal destroyed: ${portal?.name}")
                portal = null
            }
            return
        }

        if (portal!!.tile().distance() > 4) {
            if (portal?.health() ?: 0 < 20) {
                logger.info("Portal low health, selecting new target")
                portal = null
                return
            }

            logger.info("Walking to portal: ${portal!!.name}, distance: $portalDistance")
            Movement.walkToPortal(portal!!)
            return
        }
        return
    }

    private fun fightBrawler(): Boolean {
        val brawlers = Npcs.stream().within(3.toDouble()).name("Brawler").nearest().list()
        if (brawlers.isEmpty()) {
            return false
        }

        val brawler = brawlers.random()
        return brawler.valid() && brawler.interact("Attack") &&
                Condition.wait { Players.local().interacting() == brawler }
    }

    private fun fightSpinner(): Boolean {
        val spinners = Npcs.stream()
            .within(7.toDouble()).name("Spinner")
            .nearest()
            .filter { it.healthPercent() >= 20 }
        if (spinners.size <= 2) {
            return false
        }

        val spinner = spinners.random()
        return spinner.valid() && spinner.interact("Attack") &&
                Condition.wait { Players.local().interacting() == spinner }
    }

    private fun attackPortal(): Boolean {
        val portal = Npcs.stream().name("Portal").within(7).nearest().first()
        if (!portal.valid() || !portal.actions().contains("Attack")) {
            return false
        }

        return portal.interact("Attack") &&
                Condition.wait { Players.local().animation() != -1 && !Players.local().inMotion() }
    }
}