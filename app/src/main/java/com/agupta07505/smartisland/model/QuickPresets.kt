/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class PresetPlace { Left, Centre, Right }

data class PresetFit(val widthDp: Float, val xOffsetDp: Float)

data class PresetPlan(val fit: PresetFit, val hole: CutoutArea?)

object QuickPresets {
    const val VALUE_MIN_DP = 28f
    const val EDGE_MARGIN_DP = 8f
    const val MAX_PILL_WIDTH_DP = 180f
    const val MAX_PILL_X_DP = 300f

    fun fit(
        screenWidthDp: Float,
        hole: CutoutArea?,
        baseWidthDp: Float,
        place: PresetPlace,
        maxWidthDp: Float = MAX_PILL_WIDTH_DP,
        maxXDp: Float = MAX_PILL_X_DP
    ): PresetFit {
        fun xFor(width: Float): Float = when (place) {
            PresetPlace.Centre -> 0f
            PresetPlace.Left -> -(screenWidthDp / 2f - width / 2f - EDGE_MARGIN_DP).coerceIn(30f, maxXDp)
            PresetPlace.Right -> (screenWidthDp / 2f - width / 2f - EDGE_MARGIN_DP).coerceIn(30f, maxXDp)
        }
        if (hole == null || hole.widthDp <= 0f) return PresetFit(baseWidthDp, xFor(baseWidthDp))
        var width = baseWidthDp
        while (width < maxWidthDp) {
            val x = xFor(width)
            val left = (screenWidthDp / 2f + x - width / 2f)
                .coerceIn(EDGE_MARGIN_DP, (screenWidthDp - EDGE_MARGIN_DP - width).coerceAtLeast(EDGE_MARGIN_DP))
            val centre = cameraCentreInPill(screenWidthDp / 2f, hole.centreXDp, left + width / 2f, width)
            val room = CollapsedSlots.plan(width, centre, hole.widthDp).valueMaxWidthDp
            if (room == null || room >= VALUE_MIN_DP) break
            width += 1f
        }
        val final = width.coerceAtMost(maxWidthDp)
        return PresetFit(final, xFor(final))
    }

    fun holeCentreFor(place: PresetPlace, screenWidthDp: Float, holeWidthDp: Float): Float {
        if (place == PresetPlace.Centre) return 0f
        val fromEdge = EDGE_MARGIN_DP + SideCamera.bandWidthDp(holeWidthDp) / 2f
        val offset = (screenWidthDp / 2f - fromEdge).coerceIn(0f, MAX_PILL_X_DP)
        return if (place == PresetPlace.Left) -offset else offset
    }

    fun plan(
        screenWidthDp: Float,
        hole: CutoutArea?,
        baseWidthDp: Float,
        place: PresetPlace,
        manualHole: Boolean,
        maxWidthDp: Float = MAX_PILL_WIDTH_DP
    ): PresetPlan {
        val target = if (manualHole && hole != null) hole.copy(centreXDp =
            holeCentreFor(place, screenWidthDp, hole.widthDp)) else hole
        val moved = if (manualHole) target else null
        val side = SideCamera.sideOf(target?.centreXDp)
        if (target == null || side == CameraSide.Centre) {
            return PresetPlan(fit(screenWidthDp, target, baseWidthDp, place, maxWidthDp), moved)
        }
        return PresetPlan(sideFit(screenWidthDp, target, side, baseWidthDp, maxWidthDp), moved)
    }

    private fun sideFit(screenWidthDp: Float, hole: CutoutArea, side: CameraSide, baseWidthDp: Float,
            maxWidthDp: Float): PresetFit {
        var width = baseWidthDp
        while (width < maxWidthDp) {
            val left = SideCamera.desiredLeftDp(side, screenWidthDp, hole.centreXDp, hole.widthDp, width, 0f)
                .coerceIn(EDGE_MARGIN_DP, (screenWidthDp - EDGE_MARGIN_DP - width).coerceAtLeast(EDGE_MARGIN_DP))
            val centre = cameraCentreInPill(screenWidthDp / 2f, hole.centreXDp, left + width / 2f, width)
            val room = centre?.let { SideCamera.zone(side, width, it, hole.widthDp)?.plan?.valueMaxWidthDp }
            if (room == null || room >= VALUE_MIN_DP) break
            width += 1f
        }
        return PresetFit(width.coerceAtMost(maxWidthDp), 0f)
    }
}
