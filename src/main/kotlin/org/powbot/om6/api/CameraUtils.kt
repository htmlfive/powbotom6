package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera

/**
 * Common camera utilities shared across all scripts.
 */
object CameraUtils {

    // ========================================
    // PITCH
    // ========================================

    /**
     * Sets the camera pitch.
     * @param pitch The pitch value (0-99, where 99 is highest)
     * @return true if pitch was set
     */
    fun setPitch(pitch: Int): Boolean {
        return Camera.pitch(pitch)
    }

    /**
     * Sets the camera to maximum pitch (bird's eye view).
     * @return true if pitch was set
     */
    fun setMaxPitch(): Boolean {
        return Camera.pitch(99)
    }

    /**
     * Sets the camera to minimum pitch (ground level).
     * @return true if pitch was set
     */
    fun setMinPitch(): Boolean {
        return Camera.pitch(0)
    }

    /**
     * Gets the current camera pitch.
     * @return Current pitch value
     */
    fun getPitch(): Int {
        return Camera.pitch()
    }

    // ========================================
    // YAW (ROTATION)
    // ========================================

    /**
     * Sets the camera yaw (rotation).
     * @param yaw The yaw value (0-359 degrees)
     * @return true if yaw was set
     */
    fun setYaw(yaw: Int): Boolean {
        return Camera.angle(yaw)
    }

    /**
     * Gets the current camera yaw.
     * @return Current yaw value in degrees
     */
    fun getYaw(): Int {
        return Camera.yaw()
    }

    /**
     * Rotates the camera to face a cardinal direction.
     * @param direction "North", "East", "South", or "West"
     * @return true if rotation was successful
     */
    fun faceDirection(direction: String): Boolean {
        val yaw = when (direction.lowercase()) {
            "north" -> 0
            "east" -> 90
            "south" -> 180
            "west" -> 270
            else -> return false
        }
        return setYaw(yaw)
    }

    // ========================================
    // ZOOM
    // ========================================

    /**
     * Sets the camera zoom level.
     * @param zoom Zoom level (0-100, where 100 is fully zoomed out)
     * @return true if zoom was set
     */
    fun setZoom(zoom: Int): Boolean {
        Camera.moveZoomSlider(zoom.toDouble())
        return Condition.wait({ Camera.zoom == zoom }, 100, 20)
    }

    /**
     * Gets the current camera zoom.
     * @return Current zoom level
     */
    fun getZoom(): Int {
        return Camera.zoom
    }

    /**
     * Zooms camera to maximum (fully zoomed out).
     * @return true if zoom was set
     */
    fun zoomOut(): Boolean {
        return setZoom(100)
    }

    /**
     * Zooms camera to minimum (fully zoomed in).
     * @return true if zoom was set
     */
    fun zoomIn(): Boolean {
        return setZoom(0)
    }

    // ========================================
    // TURN TO
    // ========================================

    /**
     * Turns the camera to face a locatable (NPC, Object, Tile, etc).
     * @param locatable The target to face
     * @return true if camera was turned
     */
    fun turnTo(locatable: org.powbot.api.Locatable): Boolean {
        Camera.turnTo(locatable)
        Condition.sleep(Random.nextInt(200, 400))
        return true
    }

    // ========================================
    // COMPOUND ACTIONS
    // ========================================

    /**
     * Sets up camera for optimal viewing (max pitch, specific zoom).
     * @param zoom The zoom level (default: 100)
     * @return true if camera was set up
     */
    fun setupCamera(zoom: Int = 100): Boolean {
        val pitchSet = setMaxPitch()
        val zoomSet = setZoom(zoom)
        return pitchSet && zoomSet
    }

    /**
     * Sets up camera with specific pitch, yaw, and zoom.
     * @param pitch The pitch value
     * @param yaw The yaw value (degrees)
     * @param zoom The zoom level
     * @return true if all settings were applied
     */
    fun setupCamera(pitch: Int, yaw: Int, zoom: Int): Boolean {
        val pitchSet = setPitch(pitch)
        val yawSet = setYaw(yaw)
        val zoomSet = setZoom(zoom)
        return pitchSet && yawSet && zoomSet
    }
}
