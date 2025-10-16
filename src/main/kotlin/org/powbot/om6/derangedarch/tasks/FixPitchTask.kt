package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Camera
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FixPitchTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    private val DESIRED_PITCH = 99

    /**
     * This task is valid if the camera's pitch is not at the desired top-down angle.
     */
    override fun validate(): Boolean {
        return Camera.pitch() != DESIRED_PITCH
    }

    /**
     * Adjusts the camera pitch to the desired angle.
     */
    override fun execute() {
        script.logger.info("Camera pitch is incorrect, adjusting to $DESIRED_PITCH.")
        Camera.pitch(DESIRED_PITCH)
    }
}