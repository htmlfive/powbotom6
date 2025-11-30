package org.powbot.om6.pestcontrol

/**
 * Entry point for standalone execution.
 */
fun main() {
    val script = PestControl()
    script.startScript("localhost", "WebWalker", false)
}