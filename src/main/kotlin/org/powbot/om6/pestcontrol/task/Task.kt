package org.powbot.om6.pestcontrol.task

interface Task {

    fun name(): String

    fun valid(): Boolean

    fun run()
}