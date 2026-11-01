/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.SideCamera
import kotlin.math.abs

data class CutoutRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val opacity: Float,
    /** Flatness of each side (1 = both corners on that side square, 0 = round). */
    val flatTop: Float = 0f,
    val flatRight: Float = 0f,
    val flatBottom: Float = 0f,
    val flatLeft: Float = 0f
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    val radiusTopLeft: Float get() = radius * (1f - maxOf(flatTop, flatLeft))
    val radiusTopRight: Float get() = radius * (1f - maxOf(flatTop, flatRight))
    val radiusBottomRight: Float get() = radius * (1f - maxOf(flatBottom, flatRight))
    val radiusBottomLeft: Float get() = radius * (1f - maxOf(flatBottom, flatLeft))
}

object RotationMorph {
    /** Window stays grown for the morph at most this long (ms), whatever happens. */
    const val MAX_ACTIVE_MS: Long = 900L

    const val SETTLE_WINDOW_MS: Long = 700L

    /** Base length (ms) of the fade from the morph surface to the real island. */
    const val HAND_OVER_MS: Int = 140

    /** Morph surface is nearly home from here on (spring progress). */
    const val HAND_OVER_FROM: Float = 0.8f

    /** Display rotation the state stands for, in quarter turns. */
    fun displayRotation(state: OrientationState): Int =
        if (!state.isLandscape) 0 else if (state.reversed) 3 else 1

    /** Quarter turns the phone went through from [prev] to [now]. */
    fun quarterTurns(prev: OrientationState, now: OrientationState): Int =
        ((displayRotation(now) - displayRotation(prev)) % 4 + 4) % 4

    /** Whether a read after a turn can be the morph's target: a camera found, or the settled read (a camera-less phone). */
    fun targetReady(state: OrientationState, settled: Boolean): Boolean =
        settled || !state.isLandscape || state.camera != null

    fun turned(prev: OrientationState, now: OrientationState): Boolean =
        prev.screenWidthDp > 0f && now.screenWidthDp > 0f && quarterTurns(prev, now) != 0

    /** Size (width, height in dp) of the screen in the orientation [prev] stood for. */
    fun oldFrame(prev: OrientationState, now: OrientationState): Pair<Float, Float> =
        if (prev.isLandscape) prev.screenWidthDp to prev.screenHeightDp else prev.screenWidthDp to now.screenWidthDp

    /** [rect], in a screen [oldWidth] x [oldHeight] dp. */
    fun rotate(rect: CutoutRect, oldWidth: Float, oldHeight: Float, turns: Int): CutoutRect =
            when (((turns % 4) + 4) % 4) {
        1 -> rect.copy(
            left = rect.top, top = oldWidth - rect.right, width = rect.height, height = rect.width,
            flatLeft = rect.flatTop, flatBottom = rect.flatLeft, flatRight = rect.flatBottom, flatTop = rect.flatRight
        )
        2 -> rect.copy(
            left = oldWidth - rect.right, top = oldHeight - rect.bottom,
            flatTop = rect.flatBottom, flatBottom = rect.flatTop, flatLeft = rect.flatRight, flatRight = rect.flatLeft
        )
        3 -> rect.copy(
            left = oldHeight - rect.bottom, top = rect.left, width = rect.height, height = rect.width,
            flatTop = rect.flatLeft, flatLeft = rect.flatBottom, flatBottom = rect.flatRight, flatRight = rect.flatTop
        )
        else -> rect
    }

    fun surfaceAlpha(opacity: Float, handOver: Float): Float {
        if (handOver >= 1f || opacity <= 0f) return 0f
        val h = handOver.coerceAtLeast(0f)
        return (opacity * (1f - h) / (1f - h * opacity)).coerceIn(0f, 1f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Rectangle [t] of the way from [a] to [b] (0 = a, 1 = b). */
    fun lerp(a: CutoutRect, b: CutoutRect, t: Float): CutoutRect {
        val w = lerp(a.width, b.width, t)
        val h = lerp(a.height, b.height, t)
        return CutoutRect(
            left = lerp(a.left, b.left, t),
            top = lerp(a.top, b.top, t),
            width = w,
            height = h,
            radius = lerp(a.radius, b.radius, t).coerceAtMost(minOf(w, h) / 2f).coerceAtLeast(0f),
            opacity = lerp(a.opacity, b.opacity, t),
            flatTop = lerp(a.flatTop, b.flatTop, t),
            flatRight = lerp(a.flatRight, b.flatRight, t),
            flatBottom = lerp(a.flatBottom, b.flatBottom, t),
            flatLeft = lerp(a.flatLeft, b.flatLeft, t)
        )
    }

    fun nearlySame(a: CutoutRect, b: CutoutRect, tolDp: Float = 2f): Boolean =
        abs(a.left - b.left) <= tolDp && abs(a.top - b.top) <= tolDp &&
            abs(a.width - b.width) <= tolDp && abs(a.height - b.height) <= tolDp &&
            abs(a.radius - b.radius) <= tolDp && abs(a.opacity - b.opacity) <= 0.05f &&
            abs(a.flatTop - b.flatTop) <= 0.1f && abs(a.flatRight - b.flatRight) <= 0.1f &&
            abs(a.flatBottom - b.flatBottom) <= 0.1f && abs(a.flatLeft - b.flatLeft) <= 0.1f

    /** Cutout reshaping from [a] to [b] on the camera. */
    fun lerpInPlace(a: CutoutRect, b: CutoutRect, t: Float): CutoutRect = lerp(a, b, t)

    fun collapsedRect(
        raw: SmartIslandSettings,
        state: OrientationState,
        hasContent: Boolean,
        hasCards: Boolean,
        edgeShown: Boolean,
        drawn: Boolean = true,
        /** Portrait: the camera hole (null in notch mode or without one). */
        cameraArea: CutoutArea? = null,
        /** Second cutout (the companion circle) sits beside the pill. */
        hasCompanion: Boolean = false
    ): CutoutRect? {
        if (!drawn || state.screenWidthDp <= 0f) return null
        if (state.isLandscape && edgeShown && LandscapeLayoutStyle.isEdge(raw.landscapeLayoutStyle)) {
            val camera = state.edgeCamera(raw.edgePill, withContent = hasCards) ?: return null
            val pill = EdgePillLayoutMath.compute(camera, hasSecondary = false).pill
            val tuning = camera.tuning
            val flat = if (EdgePillRules.flatCameraSide(tuning.flatEdge, tuning.xOffsetDp)) 1f else 0f
            return CutoutRect(
                pill.left, pill.top, pill.width, pill.height,
                tuning.cornerRadiusDp.coerceAtMost(minOf(pill.width, pill.height) / 2f), tuning.opacity,
                flatLeft = if (camera.onLeft) flat else 0f,
                flatRight = if (camera.onLeft) 0f else flat
            )
        }
        val eff = raw.forOrientation(state, hasContent, hasCards)
        val w = eff.width
        val h = eff.height
        if (w <= 0f || h <= 0f) return null
        // The window keeps 8 dp from the screen sides, except landscape's cutouts
        val edgePad = if (state.isLandscape) 0f else 8f
        val side = if (state.isLandscape) CameraSide.Centre else SideCamera.sideOf(cameraArea?.centreXDp)
        val wanted = if (side == CameraSide.Centre || cameraArea == null) {
            state.screenWidthDp / 2f + eff.xOffset
        } else {
            // the real island's own placement (IslandLayout.desiredCollapsedLeft)
            SideCamera.desiredLeftDp(
                side, state.screenWidthDp, cameraArea.centreXDp, cameraArea.widthDp, w, eff.xOffset
            ) + w / 2f
        }
        // the real island's clamp (IslandLayout minMainLeft / maxMainLeft)
        val gap = 8f
        val circle = minOf(w, h)
        val circleLeft = SideCamera.circleLeft(side, eff.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT)
        val maxClear = state.screenWidthDp - edgePad
        val minLeft = if (hasCompanion && circleLeft) edgePad + circle + gap else edgePad
        val maxLeft = if (hasCompanion && !circleLeft) maxClear - (w + gap + circle) else maxClear - w
        val left = (wanted - w / 2f).coerceIn(minLeft, maxLeft.coerceAtLeast(minLeft))
        val centre = left + w / 2f
        // notch mode: the cutout hangs from the screen's top edge (square top corners)
        val notch = !state.isLandscape && eff.enableNotchMode
        val top = if (state.isLandscape) eff.yOffset.coerceAtLeast(0f) else if (notch) 0f else eff.yOffset
        return CutoutRect(
            centre - w / 2f, top, w, h, eff.cornerRadius.coerceAtMost(minOf(w, h) / 2f), eff.opacity,
            flatTop = if (notch) 1f else 0f
        )
    }
}

class RotationMorphTracker {
    private var state: OrientationState? = null
    private var rect: CutoutRect? = null
    private var lastTurnAtMs = Long.MIN_VALUE / 2

    /** Counts the morphs started; 0 before the first. */
    var token: Int = 0
        private set

    var skips: Int = 0
        private set

    /** Where the morph starts, in the new orientation's screen (dp). */
    var from: CutoutRect? = null
        private set

    fun observe(now: OrientationState, rectNow: CutoutRect?, enabled: Boolean, nowMs: Long) {
        val prev = state
        val prevRect = rect
        if (prev != null && prevRect != null && RotationMorph.turned(prev, now)) {
            // a turn right after another is the orientation settling
            val settling = nowMs - lastTurnAtMs < RotationMorph.SETTLE_WINDOW_MS
            val (oldW, oldH) = RotationMorph.oldFrame(prev, now)
            if (enabled && !settling && oldW > 0f && oldH > 0f) {
                val carried = RotationMorph.rotate(prevRect, oldW, oldH, RotationMorph.quarterTurns(prev, now))
                if (rectNow != null && RotationMorph.nearlySame(carried, rectNow)) {
                    skips++
                } else {
                    from = carried
                    token++
                }
            }
            lastTurnAtMs = nowMs
        }
        state = now
        // a known screen with no cutout (nothing drawn) clears the rectangle
        if (rectNow != null || now.screenWidthDp > 0f) rect = rectNow
    }
}
