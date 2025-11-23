package org.powbot.om6.salvager.tasks

import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Widgets
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.salvager.ShipwreckSalvager

class ReadyToTapTask(private val script: ShipwreckSalvager) : Task {

    override fun activate(): Boolean {
        return script.currentPhase == SalvagePhase.READY_TO_TAP
    }

    override fun execute() {
        script.logger.info("TASK: READY_TO_TAP. Initiating screen tap.")
        //checkAndResetZoom()
        if (Camera.yaw() != 0) {
            script.logger.info("Yaw is ${Camera.yaw()}, not North. Attempting to click compass to snap North.")

            val COMPASS_PARENT_WIDGET_ID = 601
            val COMPASS_COMPONENT_INDEX = 33
            val compassWidget = Widgets.widget(COMPASS_PARENT_WIDGET_ID).component(COMPASS_COMPONENT_INDEX)

            if (compassWidget.valid() && compassWidget.click()) {
                script.logger.info("Successfully clicked compass. Waiting for yaw to stabilize.")
                Condition.wait({ Camera.yaw() == 0 }, 100, 10)
                Condition.sleep(Random.nextInt(600,1200))
            } else {
                script.logger.warn("Failed to click compass widget.")
            }
        }

        val currentTile = Players.local().tile()
        if (script.startTile != null && (currentTile.x() != script.startTile!!.x() || currentTile.y() != script.startTile!!.y())) {
            script.logger.warn("Position change detected (X/Y)! Start: ${script.startTile}, Current: $currentTile. Stopping script.")
            ScriptManager.stop()
            return
        }

        script.salvageMessageFound = false

        if (executeCenterClick()) {
            script.logger.info("Tap successful. Starting event-driven wait.")
            script.phaseStartTime = System.currentTimeMillis()
            script.currentPhase = SalvagePhase.WAITING_FOR_ACTION
        } else {
            script.logger.warn("Failed to execute screen tap. Retrying on next poll.")
            Condition.sleep(Random.nextInt(1000, 1500))
        }
    }

    private fun executeCenterClick(): Boolean {
        val dimensions = Game.dimensions()
        val centerX = dimensions.width / 2
        val centerY = dimensions.height / 2

        val randomOffsetX = Random.nextInt(-10, 12)
        val randomOffsetY = Random.nextInt(-12, 9)

        val finalX = centerX + randomOffsetX + 15
        val finalY = centerY + randomOffsetY + 40

        script.logger.info("Tapping screen at randomized point X=$finalX, Y=$finalY (Base: $centerX, $centerY).")

        val clicked = Input.tap(finalX, finalY)

        Condition.sleep(Random.nextInt(300, 500))

        return clicked
    }
//    private fun checkAndResetZoom() {
//        val currentZoom = Camera.zoom.toInt()
//
//        // Powbot's Camera.zoom() returns the current zoom level as a Double (0.0 to 1.0)
//        // We check if the current zoom is NOT equal to the minimum value (0.0)
//        if (currentZoom != 0.0) {
//            // Log the change for debugging
//            println("Camera zoom is $currentZoom. Setting to 0.0 (Minimum Zoom).")
//
//            // Set the zoom level to 0.0 (fully zoomed out)
//            Camera.moveZoomSlider(0.0)
//        } else {
//            println("Camera zoom is already at 0.0.")
//        }
//    }
}