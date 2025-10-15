package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.GroundItems
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class LootTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean = !Inventory.isFull() && script.BOSS_AREA.contains(Players.local()) && GroundItems.stream().name("Crystal key", "Odium shard 2", "Malediction shard 2").isNotEmpty() && script.getBoss() == null

    override fun execute() {
        GroundItems.stream().name("Crystal key", "Odium shard 2", "Malediction shard 2").nearest().firstOrNull()?.let {
            val count = Inventory.items().size
            if (it.interact("Take")) Condition.wait({ Inventory.items().size != count }, 150, 10)
        }
    }
}