package org.powbot.om6.salvagesorter.tasks

import org.powbot.om6.salvagesorter.SalvageSorter

abstract class Task(protected val script: SalvageSorter) {
    abstract fun activate(): Boolean
    abstract fun execute()
}