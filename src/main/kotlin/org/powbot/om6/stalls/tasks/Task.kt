package org.powbot.om6.stalls.tasks

import org.powbot.om6.stalls.StallThiever

// The abstract class that all other tasks will extend from.
abstract class Task(protected val script: StallThiever) {
    /**
     * Determines if this task should be executed.
     * @return `true` if the task's conditions are met, `false` otherwise.
     */
    abstract fun validate(): Boolean

    /**
     * The logic to be performed when the task is executed.
     */
    abstract fun execute()
}