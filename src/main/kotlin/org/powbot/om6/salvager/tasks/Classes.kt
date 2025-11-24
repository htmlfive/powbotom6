package org.powbot.om6.salvager.tasks

enum class SalvagePhase {
    INITIALIZING,
    READY_TO_TAP,
    WAITING_FOR_ACTION,
    DROPPING_SALVAGE,
    WAITING_FOR_RESPAWN
}