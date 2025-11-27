package org.powbot.om6.salvagesorter.config

enum class SalvagePhase {
    // --- Neutral/Initial State ---
    IDLE,

    // --- State 1: Setup Phases (Highest-level control) ---

    /**
     * Initial setup or recovery state to get the character to the salvaging spot.
     */
    SETUP_SALVAGING,

    /**
     * Pre-sorting state where the character moves to the cargo hold to begin withdrawing/sorting.
     */
    SETUP_SORTING,

    // --- State 2: Main Salvaging Loop ---

    /**
     * General state encompassing the entire salvaging process (walking, hooking, depositing).
     */
    SALVAGING, // Re-introduced for compatibility with high-level tasks

    /**
     * Moving to the salvage location or re-deploying the hook (action within SALVAGING).
     */
    WALKING,
    SORTING_LOOT,
    /**
     * Actively salvaging items with the hook (action within SALVAGING).
     */
    HOOKING,

    /**
     * Inventory is full of raw salvage, depositing items into the cargo hold (action within SALVAGING).
     */
    DEPOSITING,

    // --- State 3: Main Sorting Loop ---

    /**
     * Refilling the inventory with raw salvage from the cargo hold to continue the sorting process.
     */
    WITHDRAWING,

    /**
     * Actively sorting salvage items from the inventory to clear them.
     */
    SORTING,

    // --- High Priority ---
    /**
     * Cleaning up non-salvage items (alching/dropping).
     */
    CLEANING
}