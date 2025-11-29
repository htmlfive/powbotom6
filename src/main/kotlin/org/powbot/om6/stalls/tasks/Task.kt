package org.powbot.om6.stalls.tasks

import org.powbot.om6.stalls.StallThiever
// :)
abstract class Task(val script: StallThiever, val name: String) {
    abstract fun validate(): Boolean
    abstract fun execute()
}
