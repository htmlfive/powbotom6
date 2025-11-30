package org.powbot.om6.pestcontrol.helpers

import org.powbot.api.rt4.Component
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Widgets


fun Components.currentPoints(): Component {
    return Components.stream(407)
        .textContains("Pest Points").first()
}

fun Components.portalHealth(idx: Int): Int {
    val w = Widgets.component(408, idx)
    if (w.valid() && w.text() != "") {
        return w.text().toInt()
    }

    return 0
}

fun Components.activityLevelPercentage(): Int {
    val c = Components.stream(408, 12).filter { it.textColor() == 40704 }.firstOrNull() ?: return 0

    return (c.width() / 141) * 100
}

fun Components.portalHasShield(idx: Int): Boolean {
    return Widgets.component(408, idx).visible()
}


fun Components.voidKnightHealth(): Component {
    return Widgets.component(408, 6)
}