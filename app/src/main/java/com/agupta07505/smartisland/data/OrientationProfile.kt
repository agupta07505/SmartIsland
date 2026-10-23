/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

/** Camera cutout position as seen in landscape, relative to the screen centre, in dp. */
data class LandscapeCamera(
    val xOffsetDp: Float,
    val centerYDp: Float
)

data class OrientationState(
    val isLandscape: Boolean = false,
    val screenWidthDp: Float = 0f,
    /** Landscape height (the short side) in dp; 0 when unknown. */
    val screenHeightDp: Float = 0f,
    val camera: LandscapeCamera? = null,
    /** Side the camera sits on when no cutout was reported, derived from display rotation. */
    val rotationSide: String = SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
    /**
     * Reverse landscape (display rotation 270, the camera on the right): the landscape profile is authored for the
     * normal landscape, so every left / right choice and horizontal fine tuning is mirrored here.
     */
    val reversed: Boolean = false
)

fun SmartIslandSettings.forOrientation(
    state: OrientationState,
    hasContent: Boolean = false,
    /** A card is on the island (the pill may also be on it only to carry the idle widgets: [hasContent] without cards). */
    hasCards: Boolean = hasContent
): SmartIslandSettings {
    if (!state.isLandscape) return if (hasContent) withContentPillWidth(state.screenWidthDp) else this

    val base = copy(
        disabledEvents = disabledEventsLandscape,
        enableNotchMode = false,
        // The overlay reads one `mergeStyle`; in landscape it is the landscape choice.
        mergeStyle = mergeStyleLandscape,
        // The idle widgets' switches are per orientation too; the overlay keeps reading the plain fields.
        idleWidgetsEnabled = idleWidgetsEnabledLandscape,
        idleOffAfterTapClose = idleOffAfterTapCloseLandscape,
        idleOffAfterLongPress = idleOffAfterLongPressLandscape,
        idleOffWhenStored = idleOffWhenStoredLandscape
    )

    if (hasContent) return base.landscapeContentPillFor(this, state, hasCards)

    // Idle cutout. `landscapeShape` is a legacy setting kept only so old backups still load; it
    // does not change the geometry.
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
            // No cutout reported: the screen edge on the side the display rotation puts the camera.
            // A centre-top camera sits mid-height on the side edge in landscape.
            side = state.rotationSide
            val sign = if (side == SmartIslandSettings.LANDSCAPE_POSITION_LEFT) -1f else 1f
            baseX = sign * cornerX
            baseY = if (state.screenHeightDp > 0f) state.screenHeightDp / 2f - size / 2f else landscapeCircleYOffset
        }
    }

    // A second notification opens its companion circle on the inner side, away from the edge.
    val innerSide = innerCircleSide(side, mirrorCirclePosition(circlePosition, state.reversed))

    return base.copy(
        width = size,
        height = size,
        xOffset = (baseX + flip * landscapeCircleXAdjust)
            .coerceIn(-SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET),
        yOffset = (baseY + landscapeCircleYAdjust)
            .coerceIn(SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET),
        cornerRadius = size / 2f,
        opacity = landscapeCircleOpacity,
        circlePosition = innerSide
    )
}

/**
 * Portrait collapsed cutout with a card on it: the idle width stretched to
 * [SmartIslandSettings.contentPillWidthPercent], unless "same as idle" is on.
 */
fun SmartIslandSettings.withContentPillWidth(screenWidthDp: Float): SmartIslandSettings {
    val stretched = contentPillWidthDp(screenWidthDp)
    return if (stretched == width) this else copy(width = stretched)
}

fun SmartIslandSettings.contentPillWidthDp(screenWidthDp: Float): Float {
    if (contentPillSameAsIdle) return width
    val percent = if (contentPillWidthPercent.isNaN()) SmartIslandSettings.DEFAULT_CONTENT_PILL_PERCENT
    else contentPillWidthPercent.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
    val wanted = width * percent / 100f
    // Unknown screen width (tests, first frame): no clamp.
    val room = if (screenWidthDp > 0f) (screenWidthDp - 2f * CONTENT_PILL_SCREEN_MARGIN_DP).coerceAtLeast(width) else Float.MAX_VALUE
    return wanted.coerceAtMost(room).coerceAtLeast(width)
}

/** Landscape pill width with a card on it: the landscape width, stretched by the landscape percent unless "same as idle" is on. */
fun SmartIslandSettings.landscapeContentWidthDp(screenWidthDp: Float): Float {
    if (contentPillSameAsIdleLandscape) return landscapeWidth
    val percent = if (contentPillWidthPercentLandscape.isNaN()) SmartIslandSettings.DEFAULT_CONTENT_PILL_PERCENT
    else contentPillWidthPercentLandscape.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
    val room = if (screenWidthDp > 0f) (screenWidthDp - 2f * CONTENT_PILL_SCREEN_MARGIN_DP).coerceAtLeast(landscapeWidth) else Float.MAX_VALUE
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

/** Left becomes right and right becomes left in reverse landscape; middle / camera stay as they are. */
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
    val side = mirrorLandscapeSide(portrait.landscapePillSide(state), state.reversed)
    // With a card on it the width may stretch.
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
    // The companion side follows where the pill really ended up, not just the chosen preset.
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

/** Resolved side (left / right / middle) of the collapsed pill with content; an old "camera" choice counts as middle. */
fun SmartIslandSettings.landscapePillSide(@Suppress("UNUSED_PARAMETER") state: OrientationState): String =
    when (landscapePillPosition) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        else -> SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE
    }

/**
 * Horizontal centre of the pill relative to the screen centre before fine tuning: the camera cutout when asked for
 * and reported, a corner inset from the screen edge, or the middle.
 */
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

private const val CENTRE_BAND_DP = 30f

fun collapsedCompanionSizeDp(settings: SmartIslandSettings): Float = minOf(settings.width, settings.height)