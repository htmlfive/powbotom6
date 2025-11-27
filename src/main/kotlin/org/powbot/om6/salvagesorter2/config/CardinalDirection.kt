package org.powbot.om6.salvagesorter2.config

enum class CardinalDirection(val yaw: Int, val action: String) {
    North(0, "Look North"),
    South(180, "Look South"),
    East(270, "Look East"),
    West(90, "Look West")
}