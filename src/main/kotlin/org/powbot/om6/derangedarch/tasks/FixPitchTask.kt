package org.powbot.om6.derangedarch.tasks

import org.powbot.api.rt4.Camera
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller
import org.powbot.api.Condition

class FixPitchTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    // Define the acceptable range for the camera pitch
    private val MIN_PITCH = 85
    private val MAX_PITCH = 99

    // Define the target pitch to set if the current pitch is outside the range
    private val TARGET_PITCH = 92 // Midpoint of the range, a safe value to aim for

    /**
     * Checks if the camera pitch is outside the desired range.
     * The task should only run if the pitch is too low OR too high.
     */
    override fun validate(): Boolean {
        val currentPitch = Camera.pitch()
        return currentPitch < MIN_PITCH || currentPitch > MAX_PITCH
    }

    /**
     * Adjusts the camera pitch to the TARGET_PITCH if it is outside the range.
     * Includes a wait condition to ensure the camera movement completes, preventing looping.
     */
    override fun execute() {
        script.logger.info("Camera pitch (${Camera.pitch()}) is outside the acceptable range of $MIN_PITCH-$MAX_PITCH, adjusting to $TARGET_PITCH.")

        // 1. Send the command to set the pitch
        Camera.pitch(TARGET_PITCH)

        // 2. Wait until the pitch is within the target range or the action times out.
        // This is crucial to prevent the task from looping endlessly before the camera animation completes.
        Condition.wait({ Camera.pitch() in MIN_PITCH..MAX_PITCH }, 150, 20)
        // Checks every 150ms for up to 20 attempts (3 seconds total)
    }
}
