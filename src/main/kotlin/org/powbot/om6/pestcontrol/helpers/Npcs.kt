package org.powbot.om6.pestcontrol.helpers

import org.powbot.api.Locatable
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.om6.pestcontrol.Constants

fun Npcs.voidKnight(): Npc {
    return Npcs.stream().name(Constants.VOID_KNIGHT_NAME).first()
}

fun Npcs.squire(): Npc {
    return Npcs.stream().name(Constants.SQUIRE_NAME).first()
}

fun Npcs.nextMonster(locatable: Locatable): Npc {
    val brawler = Npcs.stream().within(Constants.BRAWLER_SEARCH_DISTANCE.toDouble())
        .name(Constants.BRAWLER_NAME).nearest().viewable()
        .filter { it.healthPercent() > Constants.BRAWLER_HEALTH_THRESHOLD }.firstOrNull()

    if (brawler?.valid() == true) {
        return brawler
    }

    val monster = Npcs.stream().name(*Constants.MONSTER_NAMES.toTypedArray())
        .viewable()
        .filter { it.healthPercent() == Constants.IDLE_ANIMATION || it.healthPercent() > Constants.MONSTER_HEALTH_THRESHOLD }
        .filter { it.tile().distanceTo(locatable) < Constants.MONSTER_SEARCH_DISTANCE }
        .sortedBy {
            it.tile().distanceTo(locatable)
        }.firstOrNull()

    if (monster?.valid() == true) {
        return monster
    }

    return Npc.Nil
}