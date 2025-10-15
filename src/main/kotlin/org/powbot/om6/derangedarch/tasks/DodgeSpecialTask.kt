package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Projectiles
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class DodgeSpecialTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean = Projectiles.stream().id(script.SPECIAL_ATTACK_PROJECTILE).isNotEmpty() ||
            Npcs.stream().name("Deranged archaeologist").any { it.overheadMessage() == script.SPECIAL_ATTACK_TEXT }

    override fun execute() {
        script.logger.info("Special attack detected! Dodging...")
        val playerTile = Players.local().tile()
        val boss = script.getBoss()
        val safeTile = script.BOSS_AREA.tiles.filter { tile ->
            val dP = tile.distanceTo(playerTile); val dB = if (boss != null) tile.distanceTo(boss) else 99.0
            dP >= 9 && dB >= 3 && tile.reachable()
        }.randomOrNull()

        if (safeTile != null) {
            Movement.step(safeTile)
            Condition.wait({ Players.local().tile() == safeTile || !validate() }, 150, 15)
        } else {
            script.logger.warn("Could not find a valid safe tile for dodge!")
        }
    }
}