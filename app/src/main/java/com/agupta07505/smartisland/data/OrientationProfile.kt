/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillTuning
import com.agupta07505.smartisland.model.FoldPosture
import com.agupta07505.smartisland.model.LandscapeClassicPulse
import com.agupta07505.smartisland.model.LandscapeLayoutStyle

data class LandscapeCamera(
    val xOffsetDp: Float,
    val centerYDp: Float,
    /** Width of the reported cutout rectangle in dp; 0 when unknown. */
    val widthDp: Float = 0f,
    val lengthDp: Float = 0f
)

data class OrientationState(
    val isLandscape: Boolean = false,
    val screenWidthDp: Float = 0f,
    /** Landscape height (the short side) in dp; 0 when unknown. */
    val screenHeightDp: Float = 0f,
    val camera: LandscapeCamera? = null,
    /** Portrait: width of the detected camera hole in dp (0 = none detected). */
    val portraitCameraWidthDp: Float = 0f,
    /** Side the camera sits on when no cutout was reported. */
    val rotationSide: String = SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
    /** Reverse landscape (display rotation 270, the camera on the right). */
    val reversed: Boolean = false,
    val posture: FoldPosture = FoldPosture.Standard
)

fun SmartIslandSettings.forOrientation(
    state: OrientationState,
    hasContent: Boolean = false,
    hasCards: Boolean = hasContent
): SmartIslandSettings {
    if (!state.isLandscape) return if (hasContent) withContentPillWidth(state.screenWidthDp) else this

    val base = copy(
        disabledEvents = disabledEventsLandscape,
        noAutoExpandEvents = noAutoExpandEventsLandscape,
        enableNotchMode = false,
        // The overlay reads one `mergeStyle`; in landscape it is the landscape choice
        mergeStyle = mergeStyleLandscape,
        // The idle widgets' switches are per orientation too
        idleWidgetsEnabled = idleWidgetsEnabledLandscape,
        idleOffAfterTapClose = idleOffAfterTapCloseLandscape,
        idleOffAfterLongPress = idleOffAfterLongPressLandscape,
        idleOffWhenStored = idleOffWhenStoredLandscape
    )

    if (hasContent) {
        // Edge pill layout with a card on the island
        val edge = if (hasCards && LandscapeLayoutStyle.isEdge(landscapeLayoutStyle)) {
            state.edgeCamera(edgePill, withContent = true)
        } else null
        return if (edge != null) base.withEdgePill(edge) else base.landscapeContentPillFor(this, state, hasCards)
    }

    // Idle cutout
    val flip = if (state.reversed) -1f else 1f
    val size = landscapeCircleSize
    val halfScreen = state.screenWidthDp / 2f
    val camera = state.camera
    val cornerX = (halfScreen - landscapeCircleEdgeInset - size / 2f).coerceAtLeast(0f)

    val baseX: Float
    val baseY: Float
    val side: String
    when (mirrorLandscapeSide(landscapeCirclePosition, state.reversed)) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> {
            side = SmartIslandSettings.LANDSCAPE_POSITION_LEFT
            baseX = -cornerX
            baseY = landscapeCircleYOffset
        }
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> {
            side = SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            baseX = cornerX
            baseY = landscapeCircleYOffset
        }
        SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE -> {
            side = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE
            baseX = 0f
            baseY = landscapeCircleYOffset
        }
        else -> if (camera != null) {
            side = if (camera.xOffsetDp < 0f) SmartIslandSettings.LANDSCAPE_POSITION_LEFT
            else SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            baseX = camera.xOffsetDp
            baseY = camera.centerYDp - size / 2f
        } else {
            side = state.rotationSide
            val sign = if (side == SmartIslandSettings.LANDSCAPE_POSITION_LEFT) -1f else 1f
            baseX = sign * cornerX
            baseY = if (state.screenHeightDp > 0f) state.screenHeightDp / 2f - size / 2f else landscapeCircleYOffset
        }
    }

    // a second notification opens its companion circle on the inner side
    val innerSide = innerCircleSide(side, mirrorCirclePosition(circlePosition, state.reversed))

    return base.copy(
        width = size,
        height = size,
        xOffset = (baseX + flip * landscapeCircleXAdjust)
            .coerceIn(-SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET),
        yOffset = (baseY + landscapeCircleYAdjust)
            .coerceIn(SmartIslandSettings.MIN_Y_OFFSET, maxOf(SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET,
                state.screenHeightDp - size)),
        cornerRadius = size / 2f,
        opacity = landscapeCircleOpacity,
        circlePosition = innerSide
    )
}

/**
 * Whether the collapsed pill pulses in landscape: classic style,
 * wherever the pill sits. Inside [LandscapeClassicPulse.CENTRE_BAND_DP] of the screen centre it takes the centre
 * layout, beyond it the left or right layout ([LandscapeClassicPulse.sideOf])
 */
fun SmartIslandSettings.landscapeClassicPulses(state: OrientationState): Boolean =
    state.isLandscape && !LandscapeLayoutStyle.isEdge(landscapeLayoutStyle)

/** Whether the edge pill pulses in landscape: every camera position, in the notch and compact looks too. */
fun SmartIslandSettings.edgePulses(state: OrientationState): Boolean =
    state.isLandscape && LandscapeLayoutStyle.isEdge(landscapeLayoutStyle)

/** Whether the collapsed pill pulses at all now: always in portrait, see [landscapeClassicPulses] and [edgePulses] in landscape. */
fun SmartIslandSettings.collapsedPulses(state: OrientationState): Boolean =
    !state.isLandscape || landscapeClassicPulses(state) || edgePulses(state)

fun SmartIslandSettings.withContentPillWidth(screenWidthDp: Float): SmartIslandSettings {
    val stretched = contentPillWidthDp(screenWidthDp)
    return if (stretched == width) this else copy(width = stretched)
}

fun SmartIslandSettings.portraitCameraWidthDp(state: OrientationState): Float = when {
    enableNotchMode -> 0f
    CutoutPlacement.isPlaced(this) -> cameraCutoutWidth
    else -> state.portraitCameraWidthDp
}

fun SmartIslandSettings.contentPillWidthDp(screenWidthDp: Float): Float {
    if (contentPillSameAsIdle) return width
    val room = if (screenWidthDp > 0f) (screenWidthDp - 2f * CONTENT_PILL_SCREEN_MARGIN_DP)
        .coerceAtLeast(width) else Float.MAX_VALUE
    val percent = if (contentPillWidthPercent.isNaN()) SmartIslandSettings.DEFAULT_CONTENT_PILL_PERCENT
    else contentPillWidthPercent.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT,
        SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
    return (width * percent / 100f).coerceAtMost(room).coerceAtLeast(width)
}

fun SmartIslandSettings.landscapeContentWidthDp(screenWidthDp: Float): Float {
    if (contentPillSameAsIdleLandscape) return landscapeWidth
    val percent = if (contentPillWidthPercentLandscape.isNaN()) SmartIslandSettings.DEFAULT_CONTENT_PILL_PERCENT
    else contentPillWidthPercentLandscape.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT,
        SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
    val room = if (screenWidthDp > 0f) (screenWidthDp - 2f * CONTENT_PILL_SCREEN_MARGIN_DP)
        .coerceAtLeast(landscapeWidth) else Float.MAX_VALUE
    return (landscapeWidth * percent / 100f).coerceAtMost(room).coerceAtLeast(landscapeWidth)
}

private const val CONTENT_PILL_SCREEN_MARGIN_DP = 24f

fun SmartIslandSettings.landscapeCameraSide(state: OrientationState): String =
    when (mirrorLandscapeSide(landscapeCirclePosition, state.reversed)) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        else -> state.camera?.let {
            if (it.xOffsetDp < 0f) SmartIslandSettings.LANDSCAPE_POSITION_LEFT
            else SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        } ?: state.rotationSide
    }

internal fun mirrorLandscapeSide(side: String, reversed: Boolean): String = when {
    !reversed -> side
    side == SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
    side == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
    else -> side
}

internal fun mirrorCirclePosition(position: String, reversed: Boolean): String = when {
    !reversed -> position
    position == SmartIslandSettings.CIRCLE_POSITION_LEFT -> SmartIslandSettings.CIRCLE_POSITION_RIGHT
    else -> SmartIslandSettings.CIRCLE_POSITION_LEFT
}

private fun innerCircleSide(side: String, portraitCirclePosition: String): String = when (side) {
    SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> SmartIslandSettings.CIRCLE_POSITION_RIGHT
    SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> SmartIslandSettings.CIRCLE_POSITION_LEFT
    else -> portraitCirclePosition
}

private fun SmartIslandSettings.landscapeContentPillFor(
    portrait: SmartIslandSettings,
    state: OrientationState,
    hasCards: Boolean
): SmartIslandSettings {
    val flip = if (state.reversed) -1f else 1f
    val side = mirrorLandscapeSide(portrait.landscapePillSide(), state.reversed)
    // With a card on it the width may stretch
    val pillWidth = if (hasCards) portrait.landscapeContentWidthDp(state.screenWidthDp) else portrait.landscapeWidth
    val baseX = landscapePillBaseX(
        side = side,
        pillWidthDp = pillWidth,
        screenWidthDp = state.screenWidthDp,
        edgeInsetDp = portrait.landscapeCircleEdgeInset,
        cameraXDp = null,
        useCamera = false
    )
    val maxX = SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET
    val x = (baseX + flip * portrait.landscapeXOffset).coerceIn(-maxX, maxX)
    // the companion side follows where the pill really ended up
    val placedSide = when {
        x < -CENTRE_BAND_DP -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
        x > CENTRE_BAND_DP -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        else -> SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE
    }
    val y = portrait.landscapeYOffset
    return copy(
        width = pillWidth,
        height = portrait.landscapeHeight,
        xOffset = x,
        yOffset = y,
        cornerRadius = portrait.landscapeCornerRadius,
        opacity = portrait.landscapeOpacity,
        circlePosition = innerCircleSide(placedSide, mirrorCirclePosition(portrait.circlePosition, state.reversed))
    )
}

fun SmartIslandSettings.landscapePillSide(): String =
    when (landscapePillPosition) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        else -> SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE
    }

internal fun landscapePillBaseX(
    side: String,
    pillWidthDp: Float,
    screenWidthDp: Float,
    edgeInsetDp: Float,
    cameraXDp: Float?,
    useCamera: Boolean
): Float {
    if (useCamera && cameraXDp != null) return cameraXDp
    val cornerX = (screenWidthDp / 2f - edgeInsetDp - pillWidthDp / 2f).coerceAtLeast(0f)
    return when (side) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> -cornerX
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> cornerX
        else -> 0f
    }
}

private const val CENTRE_BAND_DP = LandscapeClassicPulse.CENTRE_BAND_DP

fun collapsedCompanionSizeDp(settings: SmartIslandSettings): Float = minOf(settings.width, settings.height)
fun OrientationState.edgeCamera(
    tuning: EdgePillTuning = EdgePillTuning(),
    withContent: Boolean = false
): EdgeCamera? {
    if (!isLandscape || screenWidthDp <= 0f || screenHeightDp <= 0f) return null
    return EdgePillLayoutMath.resolveCamera(
        cameraXOffsetDp = camera?.xOffsetDp,
        cameraCenterYDp = camera?.centerYDp,
        cameraWidthDp = camera?.widthDp ?: 0f,
        cameraLengthDp = camera?.lengthDp ?: 0f,
        tuning = tuning,
        withContent = withContent,
        rotationOnLeft = rotationSide == SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp
    )
}

fun SmartIslandSettings.withEdgePill(camera: EdgeCamera): SmartIslandSettings = copy(
    width = camera.pillWidthDp,
    height = camera.pillHeightDp,
    cornerRadius = camera.tuning.cornerRadiusDp,
    opacity = camera.tuning.opacity,
    xOffset = 0f,
    yOffset = 0f,
    enableNotchMode = false
)
