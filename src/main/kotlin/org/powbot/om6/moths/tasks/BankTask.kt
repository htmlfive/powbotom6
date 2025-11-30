package org.powbot.om6.moths.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Players
import org.powbot.om6.moths.Constants
import org.powbot.om6.moths.MoonlightMothCatcher
import org.powbot.om6.moths.ScriptUtils

/**
 * Task to handle banking - depositing moths and withdrawing jars.
 */
class BankTask(script: MoonlightMothCatcher) : Task(script) {

    override fun activate(): Boolean {
        // Activate when we have no jars left
        return !ScriptUtils.hasJars()
    }

    override fun execute() {
        script.logger.info("BANK: Starting banking sequence")

        // Climb up stairs if needed
        if (Players.local().tile().distanceTo(Bank.nearest().tile()) > Constants.NEAR_BANK_DISTANCE) {
            script.logger.info("BANK: Need to climb stairs to reach bank")
            if (!ScriptUtils.climbStairs(Constants.ACTION_CLIMB_UP)) {
                script.logger.warn("BANK: Failed to climb stairs")
                return
            }
        }

        // Wait for bank to be in viewport or close
        if (!Bank.opened()) {
            script.logger.info("BANK: Walking to bank")
            Condition.wait(
                { Bank.inViewport() || Players.local().tile().distanceTo(Bank.nearest()) < 6 },
                100,
                50
            )
        }

        // Open bank
        if (!Bank.open()) {
            script.logger.warn("BANK: Failed to open bank")
            return
        }

        Condition.wait({ Bank.opened() }, 100, 30)

        // Deposit all moths
        if (ScriptUtils.hasCaughtMoths()) {
            script.logger.info("BANK: Depositing caught moths")
            Bank.depositInventory()
            Condition.wait({ !ScriptUtils.hasCaughtMoths() }, 100, 30)
        }

        // Withdraw butterfly jars
        if (!ScriptUtils.hasJars()) {
            script.logger.info("BANK: Withdrawing butterfly jars")
            Bank.withdraw(Constants.BUTTERFLY_JAR_NAME, Bank.Amount.ALL)
            Condition.wait({ ScriptUtils.hasJars() }, 100, 30)
        }

        // Close bank
        Bank.close()
        Condition.wait({ !Bank.opened() }, 100, 20)

        script.logger.info("BANK: Banking complete")
    }

    override fun name(): String = "Banking"
}
