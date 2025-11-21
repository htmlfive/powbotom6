package org.powbot.om6.salvager

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.Input
import org.powbot.api.Random
import org.powbot.api.Tile
import org.powbot.api.event.MessageEvent
import org.powbot.api.event.MessageType
import org.powbot.api.rt4.*
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.mobile.script.ScriptManager
import kotlin.math.abs

private enum class SalvagePhase {
    READY_TO_TAP,
    WAITING_FOR_ACTION,
    DROPPING_SALVAGE,
    WAITING_FOR_RESPAWN
}

@ScriptManifest(
    name = "Shipwreck Salvager",
    description = "Salvages Shipwrecks",
    version = "1.0.0",
    author = "You",
    category = ScriptCategory.Other
)
class ShipwreckSalvager : AbstractScript() {


    private val ACTION_TIMEOUT_MILLIS = 450 * 1000
    private val RESPAWN_WAIT_MIN_MILLIS = 25 * 100
    private val RESPAWN_WAIT_MAX_MILLIS = 35 * 100
    private val DIALOGUE_RESTART_MIN_MILLIS = 15 * 100
    private val DIALOGUE_RESTART_MAX_MILLIS = 20 * 100

    private val SALVAGE_COMPLETE_MESSAGE = "You salvage all you can"
    private val SALVAGE_NAME = "Barracuda salvage"
    private var currentPhase: SalvagePhase = SalvagePhase.READY_TO_TAP
    private var phaseStartTime: Long = 0L
    private var currentRespawnWait: Long = RESPAWN_WAIT_MIN_MILLIS.toLong()
    private var salvageMessageFound = false
    private var startTile: Tile? = null

    @Subscribe
    fun onMessageEvent(change: MessageEvent) {
        if (change.messageType == MessageType.Game && change.message.contains(SALVAGE_COMPLETE_MESSAGE)) {
            logger.info("EVENT: Salvage COMPLETE message detected via EventBus!")
            salvageMessageFound = true
            startTile = Players.local().tile()
            logger.info("Updated startTile after dialogue to: $startTile")
            Condition.sleep(100)
        }
    }

    private fun handleDialogueCheck(): Boolean {
        if (Chat.canContinue()) {
            logger.info("DIALOGUE DETECTED: Clicking continue...")

            var count = 0
            val sleepBetween = 15

            while (count < sleepBetween) {
                Condition.sleep(Random.nextInt(1000, 2000))
                count++
            }

            if (Chat.clickContinue()) {

                startTile = Players.local().tile()
                logger.info("Updated startTile after dialogue to: $startTile")

                Condition.sleep(Random.nextInt(DIALOGUE_RESTART_MIN_MILLIS, DIALOGUE_RESTART_MAX_MILLIS))
                return true
            }
        }
        return false
    }

    override fun poll() {
        try {

            if (Inventory.isFull()) {
                if (currentPhase != SalvagePhase.DROPPING_SALVAGE) {
                    logger.info("SAFETY TRIGGER: Inventory full. Jumping to drop phase.")
                }
                currentPhase = SalvagePhase.DROPPING_SALVAGE
                handleDrop()
                return
            }

            when (currentPhase) {
                SalvagePhase.READY_TO_TAP -> {
                    handleScreenClickAction()
                }

                SalvagePhase.WAITING_FOR_ACTION -> {
                    val timeoutSeconds = ACTION_TIMEOUT_MILLIS / 1000L

                    if (handleDialogueCheck()) {
                        val waitTime = Random.nextInt(DIALOGUE_RESTART_MIN_MILLIS, DIALOGUE_RESTART_MAX_MILLIS)
                        logger.info("Dialogue cleared. Waiting ${waitTime / 1000L}s and restarting action.")
                        Condition.sleep(waitTime)

                        currentPhase = SalvagePhase.READY_TO_TAP
                        phaseStartTime = System.currentTimeMillis()
                        return
                    }

                    if (salvageMessageFound) {
                        logger.info("Salvage message detected via event flow. Moving to drop phase.")
                        salvageMessageFound = false
                        currentPhase = SalvagePhase.DROPPING_SALVAGE
                        Condition.sleep(Random.nextInt(2000, 4000))
                        handleDrop()
                    }

                    else if (System.currentTimeMillis() - phaseStartTime > ACTION_TIMEOUT_MILLIS) {
                        logger.warn("Salvage action timed out after $timeoutSeconds seconds. Moving to drop phase for safety.")
                        currentPhase = SalvagePhase.DROPPING_SALVAGE
                        Condition.sleep(Random.nextInt(2000, 4000))
                        handleDrop()
                    }

                    else {
                        val elapsedTime = System.currentTimeMillis() - phaseStartTime
                        val remaining = (ACTION_TIMEOUT_MILLIS - elapsedTime) / 1000L
                        logger.info("Waiting for MessageEvent. Timeout in ${remaining}s.")
                        Condition.sleep(Random.nextInt(500, 1000))
                    }
                }

                SalvagePhase.DROPPING_SALVAGE -> {
                    handleDrop()
                }

                SalvagePhase.WAITING_FOR_RESPAWN -> {
                    if (System.currentTimeMillis() >= phaseStartTime + currentRespawnWait) {
                        logger.info("Randomized respawn wait complete (Wait: ${currentRespawnWait / 1000L}s). Ready to tap again.")
                        currentPhase = SalvagePhase.READY_TO_TAP
                    } else {
                        val remaining = (phaseStartTime + currentRespawnWait - System.currentTimeMillis()) / 1000L
                        logger.info("Respawn wait active. ${remaining}s remaining (Total: ${currentRespawnWait / 1000L}s).")
                        Condition.sleep(Random.nextInt(500, 1000))
                    }
                }
            }

        } catch (e: Exception) {
            logger.warn("CRASH in poll(): ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleScreenClickAction() {
        logger.info("PHASE: READY_TO_TAP. Initiating screen tap.")

        salvageMessageFound = false

        val currentTile = Players.local().tile()
        if (startTile != null && (currentTile.x() != startTile!!.x() || currentTile.y() != startTile!!.y())) {
            logger.warn("Position change detected (X/Y)! Start: $startTile, Current: $currentTile. Stopping script.")
            ScriptManager.stop()
            return
        }
        val currentYaw = Camera.yaw()
        val currentPitch = Camera.pitch()

        val targetYaw = 0
        val isYawCorrect = currentYaw in 0..2
        val targetPitch = 0
        val isPitchCorrect = abs(currentPitch - targetPitch) <= 2


        if (!isYawCorrect || !isPitchCorrect) {
            logger.info("Adjusting camera. Correcting to Yaw: $targetYaw, Pitch: $targetPitch. Current Yaw: $currentYaw, Current Pitch: $currentPitch.")

            Camera.angle(targetYaw)
            Camera.pitch(targetPitch)

            Condition.sleep(Random.nextInt(800, 1500))
        } else {
            logger.info("Camera correctly positioned (Yaw: $currentYaw, Pitch: $currentPitch). Proceeding with tap.")
        }

        if (executeCenterClick()) {
            logger.info("Tap successful. Starting event-driven wait for message: '$SALVAGE_COMPLETE_MESSAGE'.")
            phaseStartTime = System.currentTimeMillis()
            currentPhase = SalvagePhase.WAITING_FOR_ACTION
        } else {
            logger.warn("Failed to execute screen tap. Retrying on next poll.")
            Condition.sleep(Random.nextInt(1000, 1500))
        }
    }

    private fun handleDrop() {
        logger.info("PHASE: DROPPING_SALVAGE. Initiating drop sequence.")

        dropSalvage()

        currentRespawnWait = Random.nextInt(RESPAWN_WAIT_MIN_MILLIS, RESPAWN_WAIT_MAX_MILLIS).toLong()

        logger.info("Drop sequence complete. Starting randomized respawn wait (${currentRespawnWait / 1000L}s).")
        phaseStartTime = System.currentTimeMillis()
        currentPhase = SalvagePhase.WAITING_FOR_RESPAWN
    }

    private fun dropSalvage() {
        if (!Inventory.opened()) {
            if (Inventory.open()) {
                logger.info("Inventory tab opened successfully for dropping.")
                Condition.sleep(Random.nextInt(200, 400))
            } else {
                logger.warn("Failed to open the inventory tab. Aborting drop sequence.")
                return
            }
        } else {
            logger.info("Inventory tab is already open.")
        }

        val salvageItems = Inventory.stream().name(SALVAGE_NAME).list()

        if (salvageItems.isNotEmpty()) {
            logger.info("Dropping ${salvageItems.size} items named '$SALVAGE_NAME'...")

            salvageItems.forEach { item ->
                if (item.valid()) {
                    if (item.interact("Drop")) {
                        Condition.sleep(Random.nextInt(60, 100))
                    } else {
                        logger.warn("Failed to click 'Drop' on item ${item.name()}.")
                    }
                }
            }

            Condition.wait({ Inventory.stream().name(SALVAGE_NAME).isEmpty() }, 150, 20)
        } else if (Inventory.isFull()) {
            logger.warn("Inventory is full but no item named '$SALVAGE_NAME' was found to drop.")
        }
    }

    private fun executeCenterClick(): Boolean {
        //val dimensions = Game.dimensions()
        //val centerX = dimensions.width / 2
        //val centerY = dimensions.height / 2
        //val finalX = centerX + randomOffsetX + 15
        //val finalY = centerY + randomOffsetY + 40
        val randomOffsetX = Random.nextInt(-10, 12)
        val randomOffsetY = Random.nextInt(-12, 9)

        val centerX = 517
        val centerY = 322
        val finalX = centerX + randomOffsetX
        val finalY = centerY + randomOffsetY


        logger.info("Tapping screen at randomized point X=$finalX, Y=$finalY (Base: $centerX, $centerY).")

        val clicked = Input.tap(finalX, finalY)

        Condition.sleep(Random.nextInt(300, 500))

        return clicked
    }

    override fun onStart() {
        phaseStartTime = System.currentTimeMillis()
        currentPhase = SalvagePhase.READY_TO_TAP
        salvageMessageFound = false

        startTile = Players.local().tile()

        val paint = PaintBuilder.newBuilder()
            .x(40)
            .y(80)
            .addString("Status") {
                val elapsedTime = System.currentTimeMillis() - phaseStartTime

                when (currentPhase) {
                    SalvagePhase.READY_TO_TAP -> "Ready to Tap"
                    SalvagePhase.WAITING_FOR_ACTION -> {
                        val remaining = (ACTION_TIMEOUT_MILLIS - elapsedTime) / 1000L
                        val status = if (salvageMessageFound) "Message DETECTED! (Dropping next)"
                        else "Salvaging '$SALVAGE_NAME' (Timeout in: ${if (remaining > 0) "${remaining}s" else "Expired"})"

                        if (Chat.canContinue()) {
                            "DIALOGUE BLOCKING - Clicking..."
                        } else {
                            status
                        }
                    }
                    SalvagePhase.DROPPING_SALVAGE -> "Dropping Salvage"
                    SalvagePhase.WAITING_FOR_RESPAWN -> {
                        val totalSeconds = currentRespawnWait / 1000L
                        val remaining = (phaseStartTime + currentRespawnWait - System.currentTimeMillis()) / 1000L
                        "Respawn Wait (${totalSeconds}s total): ${remaining}s"
                    }
                }
            }
            .build()
        addPaint(paint)
        logger.info("START: Shipwreck Salvager")
    }
}