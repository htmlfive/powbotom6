package org.powbot.om6.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Random.nextInt
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.om6.pestcontrol.Constants
import org.powbot.om6.pestcontrol.data.Boat
import org.powbot.om6.pestcontrol.helpers.currentPoints
import org.powbot.om6.pestcontrol.helpers.voidKnightHealth

class CrossGangplank(val boat: Boat): Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun name(): String {
        return "Entering boat"
    }

    override fun valid(): Boolean {
        return !Components.currentPoints().visible() && !Components.voidKnightHealth().visible()
    }

    override fun run() {
        Condition.wait({
            Chat.canContinue()
        }, Constants.CHAT_WAIT_ATTEMPTS, Constants.CHAT_WAIT_INTERVAL)

        if (Chat.canContinue()) {
            logger.info("Closing chat dialog")
            if (Random.nextBoolean()) {
                Chat.clickContinue()
                Condition.wait {
                    !Chat.canContinue()
                }
                return
            } else {
                Condition.sleep(nextInt(Constants.CHAT_CLOSE_MIN_DELAY, Constants.CHAT_CLOSE_MAX_DELAY))
            }
        }

        val gangplank = Objects.stream(Constants.OBJECT_SEARCH_DISTANCE)
            .name(Constants.GANGPLANK_NAME)
            .within(boat.gangplankTile, Constants.GANGPLANK_SEARCH_DISTANCE).first()
        if (gangplank.valid()) {
            if (gangplank.interact(Constants.CROSS_ACTION)) {
                logger.info("Crossing gangplank for ${boat.name} boat")
                Condition.wait { Components.currentPoints().visible() }
                return
            } else if (gangplank.tile.distance() > Constants.GANGPLANK_SEARCH_DISTANCE) {
                logger.info("Walking to gangplank")
                Movement.walkTo(gangplank)
                return
            }
        }
    }
}