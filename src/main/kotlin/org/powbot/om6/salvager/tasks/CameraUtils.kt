package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Widgets
import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Defines the required camera yaw and the corresponding compass action to snap to that direction.
 * Mappings: North=0, South=180, East=270, West=90.
 */
sealed class CardinalDirection(val yaw: Int, val action: String) {
    object North : CardinalDirection(0, "Look North")
    object South : CardinalDirection(180, "Look South")
    object East : CardinalDirection(270, "Look East")
    object West : CardinalDirection(90, "Look West")
}

/**
 * Utility object for controlling the camera, specifically snapping to cardinal directions.
 */
object CameraSnapper {
    private const val COMPASS_WIDGET_ID = 601
    private const val COMPASS_COMPONENT_INDEX = 35 // Consistent compass component

    /**
     * Snaps the camera to the specified CardinalDirection using the compass widget action.
     *
     * @param direction The desired cardinal direction (e.g., CardinalDirection.North).
     * @param script The main script instance for logging purposes.
     */
    fun snapCameraToDirection(direction: CardinalDirection, script: ShipwreckSalvager) {
        val yawNeeded = direction.yaw
        val COMPASS_ACTION = direction.action

        if (Camera.yaw() != yawNeeded) {
            script.logger.info("Camera Snapper: Yaw is ${Camera.yaw()}, not $COMPASS_ACTION ($yawNeeded). Attempting snap.")

            val compassComponent = Widgets.widget(COMPASS_WIDGET_ID).component(COMPASS_COMPONENT_INDEX)

            if (compassComponent.valid() && compassComponent.click(COMPASS_ACTION)) {
                script.logger.info("Camera Snapper: Successfully used '$COMPASS_ACTION' on compass. Waiting for yaw to stabilize.")
                // Wait for the camera to snap to the correct yaw
                Condition.wait({ Camera.yaw() == yawNeeded }, 100, 10)
                Condition.sleep(Random.nextInt(600, 1200))
            } else {
                script.logger.warn("Camera Snapper: Failed to find or click compass component $COMPASS_WIDGET_ID:$COMPASS_COMPONENT_INDEX with action '$COMPASS_ACTION'.")
            }
        }
    }
}