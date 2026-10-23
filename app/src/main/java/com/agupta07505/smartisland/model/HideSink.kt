/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

data class SinkHole(val centreXDp: Float, val centreYDp: Float, val widthDp: Float, val heightDp: Float)

data class SinkPivot(val xDp: Float, val yDp: Float, val endScaleX: Float, val endScaleY: Float)

/**
 * The island hiding (auto-hide, hide when idle, the long press on the edge pill, a fullscreen app) sinks into the
 * camera hole and rises out of it again when it comes back. 1 presence value (1 = shown, 0 = hidden) drives it, so a
 * hide that is turned around half way reverses from where it is
 */
object HideSink {
    const val MIN_SCALE = 0.1f

    const val FALLBACK_SCALE = 0.35f

    const val DEFAULT_HOLE_DP = 16f

    /** The island stays solid until it has sunk this far, then fades out over the rest. */
    private const val FADE_START = 0.5f

    fun applies(isLandscape: Boolean, notchMode: Boolean, edgeStyle: Boolean): Boolean =
        !AutoHideHold.applies(isLandscape, notchMode, edgeStyle)

    fun sink(presence: Float): Float = 1f - presence.coerceIn(0f, 1f)

    fun scale(sink: Float, endScale: Float): Float = 1f + (endScale - 1f) * sink.coerceIn(0f, 1f)

    fun fade(presence: Float): Float {
        val t = ((sink(presence) - FADE_START) / (1f - FADE_START)).coerceIn(0f, 1f)
        return 1f - t * t * (3f - 2f * t)
    }

    fun pivot(hole: SinkHole?, pill: SinkHole, windowTopDp: Float): SinkPivot {
        if (hole == null) {
            return SinkPivot(pill.centreXDp, pill.centreYDp - windowTopDp, FALLBACK_SCALE, FALLBACK_SCALE)
        }
        return SinkPivot(
            hole.centreXDp,
            hole.centreYDp - windowTopDp,
            endScale(hole.widthDp, pill.widthDp),
            endScale(hole.heightDp, pill.heightDp)
        )
    }

    /**
     * Portrait, and landscape's notch mode: the pivot for the collapsed pill ([pillWidthDp] by [pillHeightDp], centred
     * [pillCentreXDp] from the window's centre line) sinking into [area], the hole the layout reads. The window sits at
     * the top in notch mode and in landscape, otherwise [yOffsetDp] below it
     */
    fun portraitPivot(
        area: CutoutArea?,
        notchMode: Boolean,
        isLandscape: Boolean,
        yOffsetDp: Float,
        pillWidthDp: Float,
        pillHeightDp: Float,
        pillCentreXDp: Float
    ): SinkPivot {
        val windowTopDp = if (notchMode || isLandscape) 0f else yOffsetDp
        val pill = SinkHole(pillCentreXDp, windowTopDp + pillHeightDp / 2f, pillWidthDp, pillHeightDp)
        val hole = area?.takeIf { it.widthDp > 0f && it.heightDp > 0f }?.let {
            SinkHole(it.centreXDp, it.topDp + it.heightDp / 2f, it.widthDp, it.heightDp)
        }
        return pivot(hole, pill, windowTopDp)
    }

    fun edgePivot(camera: EdgeCamera, pill: EdgeRect): SinkPivot {
        val holeX = if (camera.onLeft) camera.centerFromEdgeDp else camera.screenWidthDp - camera.centerFromEdgeDp
        val hole = SinkHole(
            holeX - camera.screenWidthDp / 2f,
            EdgePillLayoutMath.holeCentreYDp(camera, pill.top, pill.height),
            camera.holeWidthDp.takeIf { it > 0f } ?: DEFAULT_HOLE_DP,
            camera.holeLengthDp.takeIf { it > 0f } ?: DEFAULT_HOLE_DP
        )
        val pillRect = SinkHole(
            pill.left + pill.width / 2f - camera.screenWidthDp / 2f,
            pill.top + pill.height / 2f,
            pill.width,
            pill.height
        )
        return pivot(hole, pillRect, 0f)
    }

    private fun endScale(holeDp: Float, pillDp: Float): Float =
        if (pillDp <= 0f || holeDp <= 0f) FALLBACK_SCALE else (holeDp / pillDp).coerceIn(MIN_SCALE, 1f)
}
