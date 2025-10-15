package org.powbot.om6.derangedarch.tasks

import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

abstract class Task(protected val script: DerangedArchaeologistMagicKiller) {
    abstract fun validate(): Boolean
    abstract fun execute()
}