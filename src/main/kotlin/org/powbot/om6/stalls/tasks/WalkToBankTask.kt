package org.powbot.om6.stalls.tasks

import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.om6.stalls.StallThiever

class WalkToBankTask(script: StallThiever) : Task(script, "Walking to Bank") {
    override fun validate(): Boolean = Inventory.isFull() &&
            Inventory.stream().name(*script.config.itemsToBank.toTypedArray()).isNotEmpty() &&
            script.config.bankTile.distance() > 5

    override fun execute() {
        script.logger.info("Inventory full, walking to bank...")
        if (Bank.opened()) Bank.close()
        Movement.walkTo(script.config.bankTile)
    }
}
