/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

data class CutoutSize(val heightDp: Float, val yOffsetDp: Float, val cornerRadiusDp: Float)

data class CutoutFitPlan(
    val place: PresetPlace,
    val widthDp: Float,
    val heightDp: Float,
    val xOffsetDp: Float,
    val yOffsetDp: Float,
    val cornerRadiusDp: Float
)

object CutoutFit {
    const val PAD_W_DP = 24f

    const val CENTRE_BASE_WIDTH_DP = 112f
    const val SIDE_BASE_WIDTH_DP = 90f

    const val MIN_HEIGHT_DP = 24f
    const val MAX_HEIGHT_DP = 60f
    const val MIN_Y_DP = 0f
    const val MAX_Y_DP = 80f
    const val MIN_RADIUS_DP = 8f
    const val MAX_RADIUS_DP = 40f

    /** Fixed values used until the hole size is known. */
    val FALLBACK = CutoutSize(heightDp = 34f, yOffsetDp = 10f, cornerRadiusDp = 20f)

    fun placeOf(hole: CutoutArea): PresetPlace = when (SideCamera.sideOf(hole.centreXDp)) {
        CameraSide.Left -> PresetPlace.Left
        CameraSide.Right -> PresetPlace.Right
        CameraSide.Centre -> PresetPlace.Centre
    }

    fun presetMovesHole(hole: CutoutArea?, place: PresetPlace, manualHole: Boolean): Boolean =
        manualHole || (hole != null && placeOf(hole) != place)

    fun sizeFor(hole: CutoutArea?): CutoutSize {
        if (hole == null || hole.heightDp <= 0f) return FALLBACK
        val height = hole.heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
        val y = (hole.topDp - (height - hole.heightDp) / 2f).coerceIn(MIN_Y_DP, MAX_Y_DP)
        return CutoutSize(height, y, (height / 2f).coerceIn(MIN_RADIUS_DP, MAX_RADIUS_DP))
    }

    fun plan(screenWidthDp: Float, hole: CutoutArea?): CutoutFitPlan? {
        if (hole == null || hole.widthDp <= 0f || hole.heightDp <= 0f) return null
        val place = placeOf(hole)
        val base = maxOf(if (place == PresetPlace.Centre) CENTRE_BASE_WIDTH_DP else SIDE_BASE_WIDTH_DP,
            hole.widthDp + PAD_W_DP)
        val fit = QuickPresets.plan(screenWidthDp, hole, base, place, manualHole = false).fit
        val size = sizeFor(hole)
        val x = if (place == PresetPlace.Centre) hole.centreXDp.coerceIn(-QuickPresets.MAX_PILL_X_DP,
            QuickPresets.MAX_PILL_X_DP) else fit.xOffsetDp
        return CutoutFitPlan(place, fit.widthDp, size.heightDp, x, size.yOffsetDp, size.cornerRadiusDp)
    }

    fun shouldRunOnFirstEntry(welcomeShown: Boolean, fitDone: Boolean, notchMode: Boolean,
            isPortrait: Boolean): Boolean =
        !welcomeShown && !fitDone && !notchMode && isPortrait
}
