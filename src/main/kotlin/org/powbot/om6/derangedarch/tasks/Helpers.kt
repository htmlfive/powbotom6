package org.powbot.om6.derangedarch

import org.powbot.api.Condition
import org.powbot.api.rt4.GrandExchange
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import java.text.NumberFormat
import java.util.*
import java.util.logging.Logger

object Helpers {

    private val DUELING_RING_IDS = (2552..2566).toList()
    private val DIGSITE_PENDANT_IDS = (11190..11194).toList()
    private val AXE_NAMES = setOf("Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe", "Adamant axe", "Rune axe", "Dragon axe", "Infernal axe", "Crystal axe")
    private val RUNE_NAMES = setOf("Air rune", "Water rune", "Earth rune", "Fire rune", "Mind rune", "Body rune", "Cosmic rune", "Chaos rune", "Nature rune", "Law rune", "Death rune", "Astral rune", "Blood rune", "Soul rune", "Wrath rune")

    fun isDuelingRing(id: Int): Boolean = id in DUELING_RING_IDS
    fun isDigsitePendant(id: Int): Boolean = id in DIGSITE_PENDANT_IDS
    fun isAxe(name: String): Boolean = name in AXE_NAMES
    fun isRune(name: String): Boolean = name in RUNE_NAMES

    fun formatNumber(value: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    fun sleepRandom(baseMs: Int) {
        val variance = (baseMs * 0.3).toInt()
        val sleep = baseMs + Random().nextInt(variance * 2) - variance
        Condition.sleep(sleep)
    }

    fun equipItem(item: Item, logger: Logger, verifyCondition: () -> Boolean) {
        val itemName = item.name()
        val actions = item.actions()
        val actionUsed = listOf("Wield", "Wear", "Equip").find { it in actions }

        if (actionUsed != null && item.interact(actionUsed)) {
            val success = Condition.wait(verifyCondition, 250, 10)
            if (success) {
                logger.info("Equipped: $itemName")
            } else {
                logger.info("Equip failed verification: $itemName")
            }
        } else {
            logger.info("Failed to equip: $itemName")
        }
    }

    fun makeInventorySpace(script: DerangedArchaeologistMagicKiller): Boolean {
        val logger = script.logger

        val food = Inventory.stream()
            .name(script.config.foodName)
            .firstOrNull()

        if (food != null) {
            if (food.interact("Drop")) {
                Condition.wait({ !Inventory.isFull() }, 150, 8)
                if (!Inventory.isFull()) {
                    logger.info("Dropped food for space")
                    return true
                }
            }
        }

        val itemToDrop = Inventory.stream()
            .filter { item ->
                val name = item.name()
                name != "Prayer potion(4)" &&
                        !name.contains("Ring of dueling") &&
                        !name.contains("Digsite pendant") &&
                        !isAxe(name) &&
                        name != script.config.foodName &&
                        !isRune(name)
            }
            .minByOrNull { GrandExchange.getItemPrice(it.id()) ?: 0 }

        if (itemToDrop != null) {
            if (itemToDrop.interact("Drop")) {
                Condition.wait({ !Inventory.isFull() }, 150, 8)
                if (!Inventory.isFull()) {
                    logger.info("Dropped ${itemToDrop.name()} for space")
                    return true
                }
            }
        }

        logger.warn("Could not make inventory space")
        return false
    }
}