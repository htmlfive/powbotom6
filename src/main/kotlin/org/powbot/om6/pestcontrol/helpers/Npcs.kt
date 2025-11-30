package org.powbot.om6.pestcontrol.helpers

import org.powbot.api.Locatable
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs

private val monsters = listOf(
    "Spinner", "Torcher","Ravager","Defiler","Brawler","Splatter","Shifter"
   // "Portal",
)

fun Npcs.voidKnight(): Npc {
    return Npcs.stream().name("Void Knight").first()
}

fun Npcs.squire(): Npc {
    return Npcs.stream().name("Squire").first()
}

fun Npcs.nextMonster(locatable: Locatable): Npc {
    val brawler = Npcs.stream().within(6.toDouble())
        .name("Brawler").nearest().viewable().filter { it.healthPercent() > 20 }.firstOrNull()

    if (brawler?.valid() == true) {
        return brawler
    }

    val monster = Npcs.stream().name(*monsters.toTypedArray())
        .viewable()
        .filter { it.healthPercent() == -1 || it.healthPercent() > 10 }
        .filter { it.tile().distanceTo(locatable) < 12 }
        .sortedBy {
            it.tile().distanceTo(locatable)
        }.firstOrNull()

    if (monster?.valid() == true) {
        return monster
    }

    return Npc.Nil
}