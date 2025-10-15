package org.powbot.om6.derangedarch

data class Config(
    val requiredEquipment: Map<Int, Int>,
    val requiredInventory: Map<Int, Int>,
    val foodName: String,
    val eatAtPercent: Int,
    val emergencyHpPercent: Int,
    val emergencyTeleportItem: String,
    // --- ADDED THIS LINE ---
)