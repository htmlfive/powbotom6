package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Chat

/**
 * Common dialogue/chat utilities shared across all scripts.
 */
object DialogueUtils {

    /**
     * Checks if a dialogue is currently open.
     * @return true if chatting/dialogue is active
     */
    fun isDialogueOpen(): Boolean {
        return Chat.chatting()
    }

    /**
     * Checks if a continue option is available.
     * @return true if can click continue
     */
    fun canContinue(): Boolean {
        return Chat.canContinue()
    }

    /**
     * Clicks continue in a dialogue.
     * @param sleepMin Minimum sleep after clicking (default: 500)
     * @param sleepMax Maximum sleep after clicking (default: 800)
     * @return true if continue was clicked
     */
    fun clickContinue(sleepMin: Int = 500, sleepMax: Int = 800): Boolean {
        if (!canContinue()) return false

        if (Chat.clickContinue()) {
            Condition.sleep(Random.nextInt(sleepMin, sleepMax))
            return true
        }
        return false
    }

    /**
     * Handles multiple continue prompts.
     * @param maxAttempts Maximum continues to click (default: 5)
     * @param sleepMin Minimum sleep between clicks
     * @param sleepMax Maximum sleep between clicks
     * @return Number of continues clicked
     */
    fun handleContinues(maxAttempts: Int = 5, sleepMin: Int = 500, sleepMax: Int = 800): Int {
        var count = 0
        while (count < maxAttempts && clickContinue(sleepMin, sleepMax)) {
            count++
        }
        return count
    }

    /**
     * Selects a dialogue option by text content.
     * @param textContains The text to search for
     * @return true if option was selected
     */
    fun selectOption(textContains: String): Boolean {
        if (!isDialogueOpen()) return false

        val option = Chat.stream()
            .textContains(textContains)
            .firstOrNull()

        return option?.select() ?: false
    }

    /**
     * Selects a dialogue option by exact text.
     * @param text The exact text to match
     * @return true if option was selected
     */
    fun selectOptionExact(text: String): Boolean {
        if (!isDialogueOpen()) return false

        val option = Chat.stream()
            .text(text)
            .firstOrNull()

        return option?.select() ?: false
    }

    /**
     * Selects a dialogue option and waits for dialogue to change.
     * @param textContains The text to search for
     * @param timeout Maximum wait time in ms (default: 3000)
     * @return true if option was selected and dialogue changed
     */
    fun selectOptionAndWait(textContains: String, timeout: Int = 3000): Boolean {
        if (!selectOption(textContains)) return false

        // Wait for dialogue to either close or change
        return Condition.wait(
            { !isDialogueOpen() || canContinue() },
            100,
            timeout / 100
        )
    }

    /**
     * Completes an entire dialogue by clicking through all continues.
     * @param maxContinues Maximum continues to click (default: 20)
     * @param sleepMin Minimum sleep between clicks
     * @param sleepMax Maximum sleep between clicks
     * @return true if dialogue completed (no longer chatting)
     */
    fun completeDialogue(maxContinues: Int = 20, sleepMin: Int = 400, sleepMax: Int = 700): Boolean {
        var attempts = 0
        while (isDialogueOpen() && attempts < maxContinues) {
            if (canContinue()) {
                clickContinue(sleepMin, sleepMax)
            } else {
                // No continue available, might be waiting or need to select option
                Condition.sleep(Random.nextInt(200, 400))
            }
            attempts++
        }
        return !isDialogueOpen()
    }

    /**
     * Waits for dialogue to open.
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if dialogue opened within timeout
     */
    fun waitForDialogue(timeout: Int = 5000): Boolean {
        return Condition.wait({ isDialogueOpen() }, 100, timeout / 100)
    }

    /**
     * Waits for dialogue to close.
     * @param timeout Maximum wait time in ms (default: 5000)
     * @return true if dialogue closed within timeout
     */
    fun waitForDialogueClose(timeout: Int = 5000): Boolean {
        return Condition.wait({ !isDialogueOpen() }, 100, timeout / 100)
    }
}
