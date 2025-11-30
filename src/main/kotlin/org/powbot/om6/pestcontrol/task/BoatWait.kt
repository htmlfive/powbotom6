package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Components
import org.powbot.om6.pestcontrol.PestControl
import org.powbot.om6.pestcontrol.helpers.currentPoints
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth

class BoatWait(val script: PestControl) : Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Waiting for next game"
    }

    override fun valid(): Boolean {
        return !Components.voidKnightHealth().visible() && Components.currentPoints().visible()
    }

    override fun run() {
        if (script.playedGame) {
            script.gamesPlayed++
            script.gamesSinceChangedActivity++
            logger.info("Game completed. Total games: ${script.gamesPlayed}")
            script.playedGame = false
        }

        val currentPointsTxt = Components.currentPoints().text()
        if (currentPointsTxt.startsWith("Pest Points:")) {
            val currPoints = currentPointsTxt.replace("Pest Points: ", "").trim().toInt()
            if (script.initialPoints == null) {
                script.initialPoints = currPoints
                logger.info("Initial points: $currPoints")
            }

            val pointsGained = currPoints - script.initialPoints!!
            if (pointsGained > script.pointsGained) {
                logger.info("Points gained: $pointsGained (Current: $currPoints)")
                script.pointsGained = pointsGained
            }
        }

        Condition.wait {
            !valid()
        }
    }
}