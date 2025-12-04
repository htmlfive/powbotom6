package org.powbot.om6.varrockmuseum

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Component
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Widgets

object VarrockMuseumUtils {

    fun inventoryFull(): Boolean {
        return Inventory.isFull()
    }

    fun inventoryContains(itemName: String): Boolean {
        return Inventory.stream().name(itemName).isNotEmpty()
    }

    fun inventoryContainsAny(itemNames: List<String>): Boolean {
        return itemNames.any { inventoryContains(it) }
    }

    fun interactWithObject(objectName: String, action: String): Boolean {
        val obj = Objects.stream().name(objectName).nearest().firstOrNull()
        if (obj != null && obj.valid()) {
            return obj.interact(action)
        }
        return false
    }

    fun handleDialogue(dialogueText: String): Boolean {
        if (Chat.chatting()) {
            val option = Chat.stream()
                .textContains(dialogueText)
                .firstOrNull()

            if (option != null) {
                return option.select()
            }
        }
        return false
    }

    fun dropItems(itemNames: List<String>) {
        for (itemName in itemNames) {
            val items = Inventory.stream().name(itemName).toList()
            for (item in items) {
                item.interact("Drop")
                Condition.sleep(Random.nextInt(100, 400))
            }
        }
    }

    fun waitUntil(condition: () -> Boolean, minWait: Int = 50, maxWait: Int = 150): Boolean {
        return Condition.wait(condition, Random.nextInt(minWait, maxWait), 20)
    }

    fun rubLamp(skillName: String, widgetId: Int): Boolean {
        val lamp = Inventory.stream().name("Antique lamp").firstOrNull()
        if (lamp != null && lamp.valid()) {
            // Step 1: Rub the lamp
            if (lamp.interact("Rub")) {
                // Sleep a bit, then wait for widget to appear or timeout after 1200-1800ms
                Condition.sleep(Random.nextInt(600, 1200))
                val timeout = Random.nextInt(1200, 1800)
                Condition.wait({ Widgets.widget(widgetId).valid() }, 100, timeout / 100)

                // Step 2: Select the skill dynamically by action
                val skillComponent: Component = Components.stream(widgetId).action(skillName).first()

                if (skillComponent.valid() && skillComponent.visible()) {
                    if (skillComponent.click()) {
                        // Sleep a bit after selecting skill
                        Condition.sleep(Random.nextInt(600, 900))

                        // Step 3: Confirm the selection
                        val confirmButton: Component = Components.stream(widgetId).action("Confirm").first()
                        if (confirmButton.valid() && confirmButton.visible()) {
                            Condition.sleep(Random.nextInt(300, 600))
                            return confirmButton.click()
                        }
                    }
                }
            }
        }
        return false
    }

    fun getInventoryCount(): Int {
        return Inventory.stream().count().toInt()
    }
}