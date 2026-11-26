/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/** What the phone's torch can do, read from the camera the torch belongs to. */
data class TorchInfo(
    val cameraId: String,
    /** Highest brightness step the torch accepts, 1 when it only has on and off. */
    val maxLevel: Int,
    val defaultLevel: Int
) {
    /** Adjustable brightness needs Android 13+ and a camera that reports more than 1 step. */
    val supportsBrightness: Boolean get() = maxLevel > 1
}

object TorchCapabilities {
    @Volatile private var cached: TorchInfo? = null
    @Volatile private var checked = false

    /** Null when the phone has no torch (or the camera service cannot be read), cached after the first look. */
    fun detect(context: Context): TorchInfo? {
        if (checked) return cached
        val cm = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val info = cm?.let { read(it) }
        // A failed read is not cached, so a camera service that was not ready yet gets asked again
        if (cm != null && info != null) {
            cached = info
            checked = true
        }
        return info
    }

    /** The torch's current brightness step, null when the phone cannot report one (Android 12 and below). */
    fun currentLevel(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val info = detect(context) ?: return null
        if (!info.supportsBrightness) return null
        val cm = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
        return quietly { cm.getTorchStrengthLevel(info.cameraId) }
    }

    private fun read(cm: CameraManager): TorchInfo? = quietly {
        for (id in cm.cameraIdList) {
            val c = cm.getCameraCharacteristics(id)
            if (c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != true) continue
            // The brightness steps exist from Android 13; below it the torch is plain on/off
            val (max, default) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                levels(
                    Build.VERSION.SDK_INT,
                    c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL),
                    c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL)
                )
            } else {
                1 to 1
            }
            return@quietly TorchInfo(id, max, default)
        }
        null
    }

    /** The reported steps, or plain on/off (1, 1) when the phone is below Android 13 or reports nothing sensible. */
    internal fun levels(sdk: Int, max: Int?, default: Int?): Pair<Int, Int> {
        if (sdk < Build.VERSION_CODES.TIRAMISU || max == null || max < 1) return 1 to 1
        return max to (default ?: max).coerceIn(1, max)
    }
}
