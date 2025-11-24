package org.powbot.om6.salvager.tasks

import org.powbot.om6.salvager.ShipwreckSalvager

abstract class Task(protected val script: ShipwreckSalvager) {
    abstract fun activate(): Boolean
    abstract fun execute()
}