package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.om6.stalls.Constants
import org.powbot.om6.stalls.StallThiever

class HandlePitchTask(script: StallThiever) : Task(script, Constants.TaskNames.HANDLING_PITCH) {
    private var desiredPitch: Int = Random.nextInt(Constants.Camera.MIN_PITCH, Constants.Camera.MAX_PITCH + 1)

    override fun validate(): Boolean {
        val currentPitch = Camera.pitch()
        return currentPitch < Constants.Camera.MIN_PITCH || currentPitch > Constants.Camera.MAX_PITCH
    }

    override fun execute() {
        script.logger.info("Pitch is outside acceptable range (${Constants.Camera.MIN_PITCH}-${Constants.Camera.MAX_PITCH}), adjusting to $desiredPitch.")
        Camera.pitch(desiredPitch)
        Condition.sleep(Random.nextInt(1200, 1800))
        // Pick a new random pitch for next time
        desiredPitch = Random.nextInt(Constants.Camera.MIN_PITCH, Constants.Camera.MAX_PITCH + 1)
    }
}