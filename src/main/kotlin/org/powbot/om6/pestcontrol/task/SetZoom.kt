package org.powbot.om6.pestcontrol.task

import org.powbot.api.rt4.Camera

class SetZoom: Task {
    override fun valid(): Boolean {
        return Camera.zoom > 5
    }

    override fun run() {
        Camera.moveZoomSlider(0.0)
    }

    override fun name(): String {
        return "Setting zoom"
    }
}