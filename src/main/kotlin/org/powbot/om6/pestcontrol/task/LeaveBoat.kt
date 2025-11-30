package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.om6.pestcontrol.PowPestControl
import org.powbot.om6.pestcontrol.data.PestControlMap
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth
import org.powbot.om6.pestcontrol.helpers.squire
import kotlin.random.Random

class LeaveBoat(val script: PowPestControl): Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Leaving boat"
    }

    override fun valid(): Boolean {
        return Components.voidKnightHealth().visible() && PestControlMap.boatArea.contains(Players.local()) &&
                Npcs.squire().tile().distance() > 1
    }

    override fun run() {
        script.playedGame = true
        logger.info("Leaving boat, game starting")
        Input.tap(Game.tileToMap(Npcs.squire().tile().derive(Random.nextInt(2, 4), Random.nextInt(-12, -7))))
        Condition.wait { !valid() }
    }
}