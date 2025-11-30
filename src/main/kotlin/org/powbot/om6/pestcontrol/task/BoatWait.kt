package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Components
import org.powbot.om6.pestcontrol.PowPestControl
import org.powbot.om6.pestcontrol.helpers.currentPoints
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth

class BoatWait(val script: PowPestControl) : Task {
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

            script.playedGame = false
        }

        val currentPointsTxt = Components.currentPoints().text()
        if (currentPointsTxt.startsWith("Pest Points:")) {
            val currPoints = currentPointsTxt.replace("Pest Points: ", "").trim().toInt()
            if (script.initialPoints == null) {
                script.initialPoints = currPoints
            }

            val pointsGained = currPoints - script.initialPoints!!
            if (pointsGained > script.pointsGained) {
                script.pointsGained = pointsGained
            }
        }

        Condition.wait {
            !valid()
        }
    }
}