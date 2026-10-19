/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowManager
import com.agupta07505.smartisland.data.SmartIslandSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectedCutoutInfo(
    val xOffsetDp: Float,
    val yOffsetDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val hasHardwareCutout: Boolean,
    /** The hole's true width, without the pill padding. 0 when there is no hardware cutout. */
    val rawWidthDp: Float = 0f,
    /** The hole's centre relative to the screen centre, unclamped. 0 when there is no hardware cutout. */
    val rawCenterOffsetDp: Float = 0f
)

/** A cutout bounding rect in screen pixels, free of `android.graphics.Rect` so the maths is unit-testable. */
data class CutoutBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f
}

/** Which bezel the camera sits on once the phone is turned sideways (Rule_Book §9.2). */
enum class LateralSide { Left, Right }

/** Where the island anchors in landscape: the side of the screen and the hole's vertical centre, in px. */
data class LateralAnchor(val side: LateralSide, val centerYPx: Float, val box: CutoutBox)

/**
 * The cutout rects the overlay window last received through `WindowInsetsCompat.displayCutout`
 * (Rule_Book §9.1), in screen pixels, with the display size they were measured against. The overlay service
 * feeds it from an `OnApplyWindowInsetsListener`; [CameraCutoutDetector.detect] prefers it over the display's
 * own cutout while the display size still matches (a rotation invalidates it until new insets arrive).
 */
object CutoutInsets {
    data class Snapshot(val boxes: List<CutoutBox>, val screenWidthPx: Int, val screenHeightPx: Int)

    private val _snapshot = MutableStateFlow<Snapshot?>(null)
    val snapshot: StateFlow<Snapshot?> = _snapshot.asStateFlow()

    /** Records [boxes]; an empty list (a window that does not reach the cutout) keeps what was known. */
    fun update(boxes: List<CutoutBox>, screenWidthPx: Int, screenHeightPx: Int) {
        if (boxes.isEmpty() || screenWidthPx <= 0 || screenHeightPx <= 0) return
        val next = Snapshot(boxes, screenWidthPx, screenHeightPx)
        if (next != _snapshot.value) _snapshot.value = next
    }

    /** The recorded boxes, if they were measured on a display of this size. */
    fun boxesFor(screenWidthPx: Int, screenHeightPx: Int): List<CutoutBox>? =
        _snapshot.value?.takeIf { it.screenWidthPx == screenWidthPx && it.screenHeightPx == screenHeightPx }?.boxes
}

object CameraCutoutDetector {

    /** The camera hole at the top edge (the highest rect with a height), or null when there is none. */
    fun topCutout(boxes: List<CutoutBox>): CutoutBox? =
        boxes.filter { it.height > 0 && it.width > 0 }.minByOrNull { it.top }

    /**
     * Calculates the right edge of the top camera cutout in dp relative to screen center.
     * Returns null if no hardware cutout is detected.
     */
    fun cutoutRightEdgeOffsetDp(boxes: List<CutoutBox>, screenWidthPx: Int, density: Float): Float? {
        if (density <= 0f || screenWidthPx <= 0) return null
        val top = topCutout(boxes) ?: return null
        val screenCenterX = screenWidthPx / 2f
        val rightEdgePx = top.right
        return (rightEdgePx - screenCenterX) / density
    }

    /**
     * In landscape the hole lies on a lateral bezel: the rect touching the left or right quarter of the
     * screen, whichever sits nearer its edge. Null when no rect lies on a side (portrait, or no cutout).
     */
    fun lateralAnchor(boxes: List<CutoutBox>, screenWidthPx: Int): LateralAnchor? {
        if (screenWidthPx <= 0) return null
        val quarter = screenWidthPx / 4
        return boxes.filter { it.height > 0 && it.width > 0 }
            .mapNotNull { box ->
                when {
                    box.right <= quarter -> Triple(box, LateralSide.Left, box.left)
                    box.left >= screenWidthPx - quarter -> Triple(box, LateralSide.Right, screenWidthPx - box.right)
                    else -> null
                }
            }
            .minByOrNull { it.third }
            ?.let { (box, side, _) -> LateralAnchor(side, box.centerY, box) }
    }

    /**
     * Calculates the physical pill position and dimensions matching the device's camera cutout coordinates.
     */
    fun calculateCutoutOffset(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        screenWidthPx: Int,
        density: Float
    ): DetectedCutoutInfo {
        if (density <= 0f) {
            return DetectedCutoutInfo(0f, 12f, 112f, 34f, false)
        }
        val cutoutCenterX = (left + right) / 2f
        val screenCenterX = screenWidthPx / 2f
        val xOffsetPx = cutoutCenterX - screenCenterX
        val xOffsetDp = (xOffsetPx / density).coerceIn(
            SmartIslandSettings.MIN_X_OFFSET,
            SmartIslandSettings.MAX_X_OFFSET
        )

        val yOffsetDp = (top.toFloat() / density).coerceIn(
            SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET
        )

        val paddingW = 24f
        val paddingH = 10f
        val widthDp = ((right - left).toFloat() / density + paddingW).coerceIn(
            SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH
        )
        val heightDp = ((bottom - top).toFloat() / density + paddingH).coerceIn(
            SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT
        )

        return DetectedCutoutInfo(
            xOffsetDp = xOffsetDp,
            yOffsetDp = yOffsetDp,
            widthDp = widthDp,
            heightDp = heightDp,
            hasHardwareCutout = true,
            rawWidthDp = (right - left).toFloat() / density,
            rawCenterOffsetDp = xOffsetPx / density
        )
    }

    /**
     * Calculates the physical pill position and dimensions matching the device's camera cutout rect.
     */
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

    /**
     * The cutout rects for the display as it is now, in screen px: the overlay's insets when current,
     * otherwise the display's own cutout (Android 10+), otherwise none.
     */
    fun currentBoxes(context: Context): List<CutoutBox> {
        val bounds = DisplayBounds.bounds(context)
        CutoutInsets.boxesFor(bounds.width(), bounds.height())?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return emptyList()
        val rects = runCatching {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay?.cutout?.boundingRects
        }.getOrNull().orEmpty()
        return rects.map { CutoutBox(it.left, it.top, it.right, it.bottom) }
    }

    /** Where the island anchors in landscape (the lateral cutout), or null when the hole is not on a side. */
    fun lateralAnchor(context: Context): LateralAnchor? =
        lateralAnchor(currentBoxes(context), DisplayBounds.widthPx(context))

    /**
     * Detects hardware camera cutout on the current device using WindowInsets / DisplayCutout API.
     */
    fun detect(context: Context): DetectedCutoutInfo {
        val metrics = context.resources.displayMetrics
        val bounds = DisplayBounds.bounds(context)
        val screenWidthPx = bounds.width()
        val density = metrics.density

        // The overlay's own WindowInsets are the most precise source when they are current.
        CutoutInsets.boxesFor(bounds.width(), bounds.height())?.let(::topCutout)?.let { box ->
            return calculateCutoutOffset(box.left, box.top, box.right, box.bottom, screenWidthPx, density)
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return fallbackInfo(context)

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
            widthDp = 112f,
            heightDp = 34f,
            hasHardwareCutout = false
        )
    }
}
