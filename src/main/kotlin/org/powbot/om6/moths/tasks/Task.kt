package org.powbot.om6.moths.tasks

import org.powbot.om6.moths.MoonlightMothCatcher

/**
 * Base task interface for the Moonlight Moth Catcher script.
 */
abstract class Task(protected val script: MoonlightMothCatcher) {

    /**
     * Determines if this task should be executed.
     * @return true if the task's conditions are met
     */
    abstract fun activate(): Boolean

    /**
     * Executes the task logic.
     */
    abstract fun execute()

    /**
     * Returns the task name for logging/paint.
     * @return The display name of this task
     */
    open fun name(): String = this::class.simpleName ?: "Unknown"
}
