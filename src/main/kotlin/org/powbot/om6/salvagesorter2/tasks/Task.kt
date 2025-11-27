package org.powbot.om6.salvagesorter2.tasks

import org.powbot.om6.salvagesorter2.SalvageSorter

abstract class Task(protected val script: SalvageSorter) {
    abstract fun activate(): Boolean
    abstract fun execute()
}