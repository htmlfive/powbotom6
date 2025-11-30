package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Players

class Sleep : Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)

    override fun name(): String {
        return "Waiting till idle"
    }

    override fun valid(): Boolean {
        val player = Players.local()
        if (player.inMotion()) {
            return true
        }

        if (player.interacting().name() == "") {
            return false
        }

        // If using range we don't need to check if we are nearby
        val weapon = Equipment.itemAt(Equipment.Slot.MAIN_HAND).name().lowercase()
        if (weapon.contains("staff") || weapon.contains("bow")) {
            return true
        }

        // If we are close then its all dandy
        return player.interacting().tile().distanceTo(Players.local()) <= 1
    }

    override fun run() {
        val weapon = Equipment.itemAt(Equipment.Slot.MAIN_HAND).name()
        logger.info("Waiting for combat action (Weapon: $weapon, Target: ${Players.local().interacting().name()})")
        Condition.wait {
            !valid()
        }
    }
}