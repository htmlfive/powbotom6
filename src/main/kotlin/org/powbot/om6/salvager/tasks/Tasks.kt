package org.powbot.om6.salvager.tasks

/**
 * Interface for all runnable tasks in the script.
 * Each task is responsible for a single piece of logic (e.g., clicking, dropping, waiting).
 */
interface Task {

    /**
     * @return true if this task is currently applicable/active based on the script's state,
     * and should proceed to execute().
     */
    fun activate(): Boolean

    /**
     * Executes the task's logic. Should handle phase transitions internally.
     */
    fun execute()
}