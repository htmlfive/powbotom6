package org.powbot.om6.derangedarch.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.om6.derangedarch.DerangedArchaeologistMagicKiller

class PotionTask(script: DerangedArchaeologistMagicKiller) : Task(script) {
    /**
     * This task now validates by checking the distance to the BOSS_TRIGGER_TILE.
     */
    override fun validate(): Boolean {
        val inFightArea = Players.local().tile().distanceTo(script.BOSS_TRIGGER_TILE) <= 8
        val needsPrayerPot = Prayer.prayerPoints() < 25
        val needsMagicPot = Skills.level(Skill.Magic) <= Skills.realLevel(Skill.Magic) && Inventory.stream().nameContains("Magic potion").isNotEmpty()

        return inFightArea && (needsPrayerPot || needsMagicPot)
    }

    override fun execute() {
        if (Prayer.prayerPoints() < 25) {
            val prayerPotion = Inventory.stream().nameContains("Prayer potion").firstOrNull()
            if (prayerPotion != null && prayerPotion.interact("Drink")) {
                Condition.sleep(1200)
                // Return after drinking one potion to re-evaluate priorities
                return
            }
        }
        if (Skills.level(Skill.Magic) <= Skills.realLevel(Skill.Magic)) {
            val magicPotion = Inventory.stream().nameContains("Magic potion").firstOrNull()
            if (magicPotion != null && magicPotion.interact("Drink")) {
                Condition.sleep(1200)
            }
        }
    }
}