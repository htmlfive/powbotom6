// SalvagePhase.kt (Updated)

package org.powbot.om6.salvagesorter.config

// Phases used by the new SalvageSorter.poll() for major state transitions.
enum class SalvagePhase {
    SETUP_SALVAGING,
    SALVAGING,
    SETUP_SORTING,
    SORTING_LOOT,
    WITHDRAWING,
    CLEANING,

    // Kept for backward compatibility:
    IDLE,
    DEPOSITING
}