package org.powbot.om6.salvagesorteraio.tasks

import org.powbot.om6.salvagesorteraio.SalvageSorter

abstract class Task(protected val script: SalvageSorter) {
    abstract fun activate(): Boolean
    abstract fun execute()
}