package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Camera
import org.powbot.api.Condition
import org.powbot.om6.derangedarch.Constants
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class FixPitchTask(script: DerangedArchaeologistMagicKiller) : Task(script) {

    override fun validate(): Boolean {
        val currentPitch = Camera.pitch()
        val shouldRun = currentPitch < Constants.MIN_PITCH || currentPitch > Constants.MAX_PITCH

        if (shouldRun) {
            script.logger.debug("Validate OK: Current pitch ($currentPitch) is outside the range ${Constants.MIN_PITCH}-${Constants.MAX_PITCH}.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing FixPitchTask...")
        script.logger.info("Camera pitch (${Camera.pitch()}) is outside the acceptable range of ${Constants.MIN_PITCH}-${Constants.MAX_PITCH}, adjusting to ${Constants.TARGET_PITCH}.")

        Camera.pitch(Constants.TARGET_PITCH)
        val result = Condition.wait({ Camera.pitch() in Constants.MIN_PITCH..Constants.MAX_PITCH }, 150, 20)
        script.logger.debug("Pitch adjustment wait result: $result. Current pitch: ${Camera.pitch()}")
    }
}