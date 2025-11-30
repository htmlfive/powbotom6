package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Random.nextInt
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.om6.pestcontrol.data.Boat
import org.powbot.om6.pestcontrol.helpers.currentPoints
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth

class CrossGangplank(val boat: Boat): Task {
    override fun name(): String {
        return "Entering boat"
    }

    override fun valid(): Boolean {
        return !Components.currentPoints().visible() && !Components.voidKnightHealth().visible()
    }

    override fun run() {
        Condition.wait({
            Chat.canContinue()
        }, 3, 500)

        if (Chat.canContinue()) {
            if (Random.nextBoolean()) {
                Chat.clickContinue()
                Condition.wait {
                    !Chat.canContinue()
                }
                return
            } else {
                Condition.sleep(nextInt(600, 800))
            }
        }

        val gangplank = Objects.stream(20).name("Gangplank").within(boat.gangplankTile, 4.toDouble()).first()
        if (gangplank.valid()) {
            if (gangplank.interact("Cross")) {
                Condition.wait { Components.currentPoints().visible() }
                return
            } else if (gangplank.tile.distance() > 4) {
                Movement.walkTo(gangplank)
                return
            }
        }
    }
}