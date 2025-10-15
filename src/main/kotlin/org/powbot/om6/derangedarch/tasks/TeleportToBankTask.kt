package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.*
import org.powbot.mobile.script.ScriptManager
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class TeleportToBankTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    // The specific tile to walk to inside Ferox Enclave
    private val bankWalkTile = Tile(3130, 3631, 0)

    override fun validate(): Boolean = script.needsSupplies() && !script.FEROX_BANK_AREA.contains(Players.local())

    override fun execute() {
        script.logger.info("Supplies low, traveling to bank...")

        var duelRing: Item = Equipment.itemAt(Equipment.Slot.RING)
        if (!duelRing.name().contains("Ring of dueling")) {
            duelRing = Inventory.stream().nameContains("Ring of dueling").firstOrNull() ?: Item.Nil
        }

        if (duelRing.valid()) {
            // --- MODIFIED LOGIC ---
            // Instead of teleporting, walk to the specified tile.
            // Note: This assumes you are already close enough to walk,
            // as it replaces the long-distance teleport.
            Movement.walkTo(bankWalkTile)

        } else {
            script.logger.warn("No Ring of Dueling found in equipment or inventory for banking! Stopping script.")
            ScriptManager.stop()
        }
    }
}