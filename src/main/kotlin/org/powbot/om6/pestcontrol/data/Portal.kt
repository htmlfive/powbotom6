package org.powbot.om6.pestcontrol.data

import org.powbot.api.Tile
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Players
import org.powbot.om6.pestcontrol.helpers.portalHasShield
import org.powbot.om6.pestcontrol.helpers.portalHealth

enum class Portal(val xOffset: Int, val yOffset: Int, val componentIdx: Int) {
    West(-24, -15, 0), East(24, -18, 1),
    SouthEast(16, -34, 2), SouthWest(-9, -35, 3);

    fun tile(): Tile {
        return PestControlMap.squireTile.tile().derive(xOffset, yOffset)
    }

    fun health(): Int {
        return Components.portalHealth(21 + componentIdx)
    }

    fun hasShield(): Boolean {
        return Components.portalHasShield(25 + (componentIdx * 2))
    }

    fun gate(): Gate {
        return Gate.values().filter { it.portals.contains(this) }.first()
    }

    companion object {
        fun openPortals(): List<Portal> {
            return values().filter { !it.hasShield() && it.health() > 20 }
        }

        fun nearestOpenPortal(): Portal? {
            return openPortals().sortedBy { it.tile().distanceTo(Players.local().tile()) }.firstOrNull()
        }
    }
}