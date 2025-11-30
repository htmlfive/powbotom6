package org.powbot.om6.pestcontrol.task

import org.powbot.api.rt4.Camera

class SetZoom: Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)
    override fun valid(): Boolean {
        return Camera.zoom > 5
    }

    override fun run() {
        logger.info("Setting camera zoom to minimum")
        Camera.moveZoomSlider(0.0)
    }

    override fun name(): String {
        return "Setting zoom"
    }
}