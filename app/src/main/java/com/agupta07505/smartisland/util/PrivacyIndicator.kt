/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/** The two hardware sensors the privacy indicator reports (Rule_Book §4.15, §4.16). */
enum class PrivacySensor { Camera, Microphone }

/**
 * Who is using the camera and the microphone right now. A user is a package name, or [UNKNOWN_USER] when the
 * source cannot tell (the unprivileged fallback sees that a sensor is busy, not by whom).
 */
data class PrivacyUsage(
    val camera: Set<String> = emptySet(),
    val microphone: Set<String> = emptySet()
) {
    val isIdle: Boolean get() = camera.isEmpty() && microphone.isEmpty()

    fun users(sensor: PrivacySensor): Set<String> = if (sensor == PrivacySensor.Camera) camera else microphone

    /** [packageName] started ([active]) or stopped using [sensor]. */
    fun with(sensor: PrivacySensor, packageName: String, active: Boolean): PrivacyUsage {
        val current = users(sensor)
        val next = if (active) current + packageName else current - packageName
        return if (sensor == PrivacySensor.Camera) copy(camera = next) else copy(microphone = next)
    }

    companion object {
        const val UNKNOWN_USER = ""
    }
}

/**
 * Turns [PrivacyUsage] into the island's one merged privacy alert and back. Camera and microphone never take
 * both slots: a single [IslandMode.PrivacyIndicator] alert carries which sensors are on in its category
 * ([CATEGORY_CAMERA], [CATEGORY_MICROPHONE] or [CATEGORY_BOTH]); the small state then shows a green dot, an
 * orange dot or the split green/orange pill. Pure, so it is unit-testable.
 */
object PrivacyIndicatorAlert {
    /** "system_" makes it an internal tool (Rule_Book §7.1): it stays when notification access goes. */
    const val KEY = "system_privacy_indicator"
    const val CATEGORY_CAMERA = "privacy_camera"
    const val CATEGORY_MICROPHONE = "privacy_microphone"
    const val CATEGORY_BOTH = "privacy_camera_microphone"

    data class Sensors(val camera: Boolean, val microphone: Boolean) {
        val both: Boolean get() = camera && microphone
    }

    fun sensorsOf(notification: IslandNotification?): Sensors = when (notification?.category) {
        CATEGORY_BOTH -> Sensors(camera = true, microphone = true)
        CATEGORY_CAMERA -> Sensors(camera = true, microphone = false)
        CATEGORY_MICROPHONE -> Sensors(camera = false, microphone = true)
        else -> Sensors(camera = false, microphone = false)
    }

    /**
     * The alert for [usage], or null when nothing is in use (the alert then leaves the island). [ownPackage] is
     * never reported. [labelOf] names a package ("Instagram"): the title names the camera's user and the text
     * the microphone's, each empty when unknown. [startedAtMs] keeps the original start across updates.
     */
    fun build(
        usage: PrivacyUsage,
        ownPackage: String,
        labelOf: (String) -> String,
        nowMs: Long,
        startedAtMs: Long? = null
    ): IslandNotification? {
        val camera = usage.camera - ownPackage
        val microphone = usage.microphone - ownPackage
        if (camera.isEmpty() && microphone.isEmpty()) return null
        val category = when {
            camera.isNotEmpty() && microphone.isNotEmpty() -> CATEGORY_BOTH
            camera.isNotEmpty() -> CATEGORY_CAMERA
            else -> CATEGORY_MICROPHONE
        }
        fun known(users: Set<String>) = users.firstOrNull { it != PrivacyUsage.UNKNOWN_USER }
        val cameraUser = known(camera)
        val microphoneUser = known(microphone)
        // The camera user is named first: it is almost always the app on screen.
        val user = cameraUser ?: microphoneUser
        return IslandNotification(
            key = KEY,
            // Always the island's own package: opening the camera app must not clear its own indicator
            // (the overlay drops an app's alerts when that app comes to the foreground).
            packageName = ownPackage,
            appName = user?.let(labelOf).orEmpty(),
            // Title: who uses the camera; text: who uses the microphone ("" = unknown or not in use).
            title = cameraUser?.let(labelOf).orEmpty(),
            text = microphoneUser?.let(labelOf).orEmpty(),
            timeMillis = startedAtMs ?: nowMs,
            category = category,
            mode = IslandMode.PrivacyIndicator
        )
    }
}
