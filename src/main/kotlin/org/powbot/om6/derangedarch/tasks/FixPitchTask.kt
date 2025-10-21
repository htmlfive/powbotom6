package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Camera
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.api.Condition

class FixPitchTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    // Define the acceptable range for the camera pitch
    private val MIN_PITCH = 85
    private val MAX_PITCH = 99

    // Define the target pitch to set if the current pitch is outside the range
    private val TARGET_PITCH = 92 // Midpoint of the range

    override fun validate(): Boolean {
        val currentPitch = Camera.pitch()
        val shouldRun = currentPitch < MIN_PITCH || currentPitch > MAX_PITCH

        if (shouldRun) {
            script.logger.debug("Validate OK: Current pitch ($currentPitch) is outside the range $MIN_PITCH-$MAX_PITCH.")
        }
        return shouldRun
    }

    override fun execute() {
        script.logger.debug("Executing FixPitchTask...")
        script.logger.info("Camera pitch (${Camera.pitch()}) is outside the acceptable range of $MIN_PITCH-$MAX_PITCH, adjusting to $TARGET_PITCH.")

        // Send the command to set the pitch
        Camera.pitch(TARGET_PITCH)

        // Wait until the pitch is within the target range or the action times out.
        // This prevents the task from looping endlessly before the camera animation completes.
        val result = Condition.wait({ Camera.pitch() in MIN_PITCH..MAX_PITCH }, 150, 20)
        script.logger.debug("Pitch adjustment wait result: $result. Current pitch: ${Camera.pitch()}")
    }
}