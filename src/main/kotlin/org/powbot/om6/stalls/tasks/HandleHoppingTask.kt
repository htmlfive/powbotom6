package org.powbot.om6.stalls.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.World
import org.powbot.api.rt4.Worlds
import org.powbot.om6.stalls.StallThiever

class HandleHoppingTask(script: StallThiever) : Task(script) {
    override fun validate(): Boolean = script.ENABLE_HOPPING &&
            Players.local().tile() == script.THIEVING_TILE &&
            Players.stream().at(Players.local().tile()).any { it != Players.local() }

    override fun execute() {
        val randomWorld = Worlds.stream()
            .filtered { it.type() == World.Type.MEMBERS && it.population in 15..350 && it.specialty() == World.Specialty.NONE }
            .toList().randomOrNull()

        if (randomWorld != null) {
            script.logger.info("Player detected on tile. Hopping to world ${randomWorld.number}.")
            if (randomWorld.hop()) {
                Condition.wait({ !Players.local().inMotion() }, 300, 20)
            }
        } else {
            script.logger.warn("Player detected, but no suitable world found for hopping.")
            Condition.sleep(3000)
        }
    }
}