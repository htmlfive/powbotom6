package org.powbot.om6.pestcontrol.helpers

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.om6.pestcontrol.data.Portal

fun Movement.walkToPortal(portal: Portal): Boolean {
    val player = Players.local()

    if (player.tile().distanceTo(portal.tile()) < 2) {
        return true
    }

    if (!Movement.reachable(Players.local().tile(), portal.tile())) {
        val gate = Objects.stream(portal.gate().tile(), 2)
            .name("Gate")
            .action("Open").firstOrNull {
                !it.actions().contains("Repair")
            }

        if (gate != null) {
            if (gate.interact("Open")) {
                return Condition.wait {
                    !gate.valid()
                }
            }

            Input.tap(Game.tileToMap(Movement.closestOnMap(gate.tile())))
            return false
        }
    }

    Input.tap(Game.tileToMap(Movement.closestOnMap(portal.tile())))
    return false
}
