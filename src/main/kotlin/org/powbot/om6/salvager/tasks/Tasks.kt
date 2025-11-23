package org.powbot.om6.salvager.tasks

import org.powbot.om6.salvager.ShipwreckSalvager

/**
 * Abstract class for all runnable tasks in the script.
 * Stores the reference to the main script instance for state access.
 */
abstract class Task(protected val script: ShipwreckSalvager) {

    /**
     * @return true if this task is currently applicable/active based on the script's state,
     * and should proceed to execute().
     */
    abstract fun activate(): Boolean

    /**
     * Executes the task's logic. Should handle phase transitions internally.
     */
    abstract fun execute()
}