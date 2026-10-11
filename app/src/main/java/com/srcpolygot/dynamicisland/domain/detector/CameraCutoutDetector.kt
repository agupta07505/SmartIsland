/*
 * Realistic Dynamic Island Fork
 * Based on Smart Island (GPL-3.0)
 * Package: com.srcpolygot.dynamicisland
 */

package com.srcpolygot.dynamicisland.domain.detector

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowManager

data class DetectedCutoutInfo(
    val xOffsetDp: Float,
    val yOffsetDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val hasHardwareCutout: Boolean
)

/**
 * Detects the camera punch-hole / notch and calculates ideal island size & position
 * so the collapsed black pill fully covers the ugly cutout (especially on iQOO Z9x).
 */
object CameraCutoutDetector {

    private const val PADDING_WIDTH_DP = 36f
    private const val PADDING_HEIGHT_DP = 16f

    fun calculateCutoutOffset(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        screenWidthPx: Int,
        density: Float
    ): DetectedCutoutInfo {
        if (density <= 0f) {
            return DetectedCutoutInfo(0f, 12f, 120f, 38f, false)
        }

        val cutoutCenterX = (left + right) / 2f
        val screenCenterX = screenWidthPx / 2f
        val xOffsetDp = ((cutoutCenterX - screenCenterX) / density)
        val yOffsetDp = (top.toFloat() / density)

        val widthDp = (right - left).toFloat() / density + PADDING_WIDTH_DP
        val heightDp = (bottom - top).toFloat() / density + PADDING_HEIGHT_DP

        return DetectedCutoutInfo(
            xOffsetDp = xOffsetDp,
            yOffsetDp = yOffsetDp,
            widthDp = widthDp,
            heightDp = heightDp,
            hasHardwareCutout = true
        )
    }

    fun calculateCutoutOffset(
        rect: Rect,
        screenWidthPx: Int,
        density: Float
    ): DetectedCutoutInfo = calculateCutoutOffset(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
        screenWidthPx = screenWidthPx,
        density = density
    )

    fun detect(context: Context): DetectedCutoutInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return fallbackInfo(context)

        val metrics = context.resources.displayMetrics
        val screenWidthPx = metrics.widthPixels
        val density = metrics.density

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val display = runCatching { windowManager.defaultDisplay }.getOrNull()
            val cutout: DisplayCutout? = try {
                display?.cutout
            } catch (e: Throwable) {
                null
            }

            val boundingRects = cutout?.boundingRects
            if (!boundingRects.isNullOrEmpty()) {
                val topCutout = boundingRects.minByOrNull { it.top }
                if (topCutout != null && topCutout.height() > 0) {
                    return calculateCutoutOffset(topCutout, screenWidthPx, density)
                }
            }
        }

        return fallbackInfo(context)
    }

    private fun fallbackInfo(context: Context): DetectedCutoutInfo {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val heightPx = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        val density = context.resources.displayMetrics.density
        val heightDp = if (density > 0f) heightPx / density else 24f
        val yOffsetDp = (heightDp * 0.3f).coerceIn(0f, 30f)

        return DetectedCutoutInfo(
            xOffsetDp = 0f,
            yOffsetDp = yOffsetDp,
            widthDp = 120f,
            heightDp = 38f,
            hasHardwareCutout = false
        )
    }
}
