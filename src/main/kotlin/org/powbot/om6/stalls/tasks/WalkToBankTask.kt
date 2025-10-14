package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.om6.stalls.StallThiever

class WalkToBankTask(script: StallThiever) : Task(script) {
    override fun validate(): Boolean = Inventory.isFull() &&
            Inventory.stream().name(*script.TARGET_ITEM_NAMES_BANK.toTypedArray()).isNotEmpty() &&
            script.BANK_TILE.distance() > 5

    override fun execute() {
        script.logger.info("Inventory full, walking to bank...")
        if (Bank.opened()) Bank.close()
        Movement.walkTo(script.BANK_TILE)
    }
}