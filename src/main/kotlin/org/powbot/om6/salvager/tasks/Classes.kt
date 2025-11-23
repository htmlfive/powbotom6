package org.powbot.om6.salvager.tasks

// Defines the current state of the bot's activities.
enum class SalvagePhase {
    INITIALIZING,
    READY_TO_TAP,
    WAITING_FOR_ACTION,
    DROPPING_SALVAGE,
    WAITING_FOR_RESPAWN
}