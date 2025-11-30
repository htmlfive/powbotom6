package org.powbot.om6.salvagesorter.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.om6.salvagesorter.SalvageSorter
import org.powbot.om6.salvagesorter.config.Constants

/**
 * Task to ensure camera zoom is set to the target level.
 * Activates when the current zoom level does not match the target.
 *
 * @param script The SalvageSorter script instance
 */
class SetZoomTask(script: SalvageSorter) : Task(script) {

    /**
     * Activates when camera zoom is not at the target level.
     * @return true if zoom adjustment is needed
     */
    override fun activate(): Boolean {
        return Camera.zoom != Constants.TARGET_ZOOM_LEVEL
    }

    /**
     * Sets the camera zoom to the target level.
     */
    override fun execute() {
        script.logger.info("Setting camera zoom to ${Constants.TARGET_ZOOM_LEVEL}")
        Camera.moveZoomSlider(Constants.TARGET_ZOOM_LEVEL.toDouble())
        Condition.wait({ Camera.zoom == Constants.TARGET_ZOOM_LEVEL }, Random.nextInt(100, 150), 20)
    }
}
