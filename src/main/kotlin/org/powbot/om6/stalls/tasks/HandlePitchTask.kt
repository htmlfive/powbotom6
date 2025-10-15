package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Camera
import org.powbot.om6.stalls.StallThiever

class HandlePitchTask(script: StallThiever) : Task(script, "Handling Pitch") {
    private val DESIRED_PITCH = 99
    override fun validate(): Boolean = Camera.pitch() != DESIRED_PITCH
    override fun execute() {
        script.logger.info("Pitch is incorrect, adjusting to $DESIRED_PITCH.")
        Camera.pitch(DESIRED_PITCH)
    }
}
