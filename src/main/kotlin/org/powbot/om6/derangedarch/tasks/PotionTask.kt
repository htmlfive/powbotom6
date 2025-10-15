package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PotionTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    override fun validate(): Boolean = script.BOSS_AREA.contains(Players.local()) && (Prayer.prayerPoints() < 25 || (Skills.level(Skill.Magic) <= Skills.realLevel(Skill.Magic) && Inventory.stream().nameContains("Magic potion").isNotEmpty()))

    override fun execute() {
        if (Prayer.prayerPoints() < 25) {
            Inventory.stream().nameContains("Prayer potion").firstOrNull()?.interact("Drink"); Condition.sleep(1200)
        }
        if (Skills.level(Skill.Magic) <= Skills.realLevel(Skill.Magic)) {
            Inventory.stream().nameContains("Magic potion").firstOrNull()?.interact("Drink"); Condition.sleep(1200)
        }
    }
}