package org.powbot.om6.salvagesorter

import org.powbot.om6.salvagesorter.SalvageSorter

fun main() {
    val script = SalvageSorter()
    script.startScript("localhost", "0m6", false)
}