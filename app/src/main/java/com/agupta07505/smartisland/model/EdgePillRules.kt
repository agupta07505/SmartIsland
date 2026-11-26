/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.max

class EdgePillMotion(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val originX: Float,
    val cue: Float = 1f
)

enum class EdgePillZone { Middle, Glyph }

enum class EdgePillAction {
    ToggleCard,
    OpenCard,
    CloseCard,
    PutCardsAway,
    BringCardsBack,
    SwitchCard,
    PillSwipeAction,
    SelectOther,
    NormalLongPress,
    AppsShortcut,
    None
}

object EdgePillRules {
    const val MIDDLE_TARGET_HEIGHT_DP = 28f

    const val GAP_DP = 8f

    const val TAP_SLOP_DP = 10f

    fun flatCameraSide(flatEdge: Boolean, xOffsetDp: Float): Boolean = flatEdge && xOffsetDp <= 0.5f

    fun shellFlat(expandProgress: Float): Float = 1f - expandProgress.coerceIn(0f, 1f)

    fun tapSlopDp(sensitivity: GestureSensitivity): Float = sensitivity.scaled(TAP_SLOP_DP)

    fun zoneAt(touchYDp: Float, cameraCenterYDp: Float, targetHeightDp: Float = MIDDLE_TARGET_HEIGHT_DP): EdgePillZone =
        if (abs(touchYDp - cameraCenterYDp) <= targetHeightDp / 2f) EdgePillZone.Middle else EdgePillZone.Glyph

    fun zoneInPill(touchYDp: Float, pill: EdgeRect, side: EdgeSideZone?, holeCentreYDp: Float? = null): EdgePillZone {
        if (side == null) {
            val centre = holeCentreYDp?.takeIf { it in pill.top..pill.bottom } ?: (pill.top + pill.height / 2f)
            return zoneAt(touchYDp, centre)
        }
        val top = pill.top + side.bandTopDp
        return if (touchYDp >= top && touchYDp <= top + side.bandHeightDp) EdgePillZone.Middle else EdgePillZone.Glyph
    }

    fun tap(zone: EdgePillZone, cardsAway: Boolean, cardOpen: Boolean = false,
            otherPillShown: Boolean = false): EdgePillAction = when {
        cardsAway -> EdgePillAction.BringCardsBack
        zone == EdgePillZone.Middle -> EdgePillAction.PutCardsAway
        cardOpen && otherPillShown -> EdgePillAction.SelectOther
        else -> EdgePillAction.ToggleCard
    }

    fun exitTravelDp(pillWidthDp: Float, xOffsetDp: Float): Float =
            xOffsetDp.coerceAtLeast(0f) + pillWidthDp + PILL_EXIT_MARGIN_DP

    fun exitSlidePx(cameraOnLeft: Boolean, travelPx: Float, presence: Float): Float =
        (if (cameraOnLeft) -1f else 1f) * travelPx * pillSlideProgress(presence)

    fun idleHidden(edgeStyle: Boolean, pillHidden: Boolean, hasCards: Boolean): Boolean =
        edgeStyle && pillHidden && !hasCards

    fun cardsAwayShown(cardsAway: Boolean, pillHidden: Boolean): Boolean = cardsAway && !pillHidden

    /**
     * Auto-hide hid the pill and no card is on the island: the edge layout treats it like the long-press hide, so the
     * cards-away pill (stored idle cards) does not come back at full opacity once the card has expired
     */
    fun autoHiddenIdle(autoHidePill: Boolean, isAutoHidden: Boolean, hasCards: Boolean): Boolean =
        autoHidePill && isAutoHidden && !hasCards

    fun hideRunning(edgeStyle: Boolean, pillHidden: Boolean, hasCards: Boolean, layerAlive: Boolean): Boolean =
        edgeStyle && pillHidden && !hasCards && layerAlive

    fun idleDotStandsIn(exiting: Boolean, hideRunning: Boolean): Boolean = exiting && !hideRunning

    fun mergeTimerRuns(canMerge: Boolean, hiding: Boolean): Boolean = canMerge && !hiding

    fun exitAlpha(presence: Float): Float {
        val t = ((pillSlideProgress(presence) - 0.3f) / EXIT_FADE_SPAN).coerceIn(0f, 1f)
        return 1f - t * t * (3f - 2f * t)
    }

    private const val EXIT_FADE_SPAN = 0.5f

    const val CLEAR_MARGIN_DP = 4f

    fun pillClearance(cameraOnLeft: Boolean, shellLeftDp: Float, shellRightDp: Float, pillLeftDp: Float,
            pillRightDp: Float, gapDp: Float): Float {
        val distance = if (cameraOnLeft) shellLeftDp - pillRightDp else pillLeftDp - shellRightDp
        return ((distance + CLEAR_MARGIN_DP) / (gapDp.coerceAtLeast(0f) + CLEAR_MARGIN_DP)).coerceIn(0f, 1f)
    }

    fun shellIsSurface(layoutOn: Boolean, cardsAway: Boolean, hasCards: Boolean, expanded: Boolean,
            shellWide: Boolean): Boolean =
        layoutOn && !cardsAway && (hasCards || expanded || shellWide)

    fun pillSurfaceAlpha(shellIsSurface: Boolean, otherAtPill: Boolean, clearance: Float, opacity: Float,
            fullAt: Float = 1f): Float = when {
        !shellIsSurface -> opacity
        otherAtPill -> opacity * (clearance / fullAt.coerceAtLeast(0.01f)).coerceIn(0f, 1f)
        else -> 0f
    }

    fun shellShadowShare(translucent: Boolean, expandProgress: Float): Float =
        if (translucent) expandProgress.coerceIn(0f, 1f) else 1f

    const val HIDDEN_TARGET_HEIGHT_DP = 40f

    const val HIDDEN_TARGET_MIN_WIDTH_DP = 40f

    fun hiddenTargetActive(edgeStyle: Boolean, pillHidden: Boolean, visibleCount: Int): Boolean =
        edgeStyle && pillHidden && visibleCount == 0

    fun hiddenTarget(pill: EdgeRect, centreYDp: Float = pill.top + pill.height / 2f): EdgeRect {
        val width = max(pill.width, HIDDEN_TARGET_MIN_WIDTH_DP)
        val left = (pill.left + pill.width / 2f - width / 2f).coerceAtLeast(0f)
        val top = (centreYDp - HIDDEN_TARGET_HEIGHT_DP / 2f).coerceAtLeast(0f)
        return EdgeRect(left, top, width, HIDDEN_TARGET_HEIGHT_DP)
    }

    fun hold(zone: EdgePillZone): EdgePillAction =
        if (zone == EdgePillZone.Middle) EdgePillAction.NormalLongPress else EdgePillAction.AppsShortcut

    fun glyphHold(zone: EdgePillZone, cardOnPill: Boolean, cardOpen: Boolean, cardsAway: Boolean,
            widgetIsland: Boolean): Boolean =
        zone == EdgePillZone.Glyph && cardOnPill && !cardOpen && !cardsAway && !widgetIsland

    fun glyphSwiped(alongPx: Float, acrossPx: Float, slopPx: Float): Boolean =
        HoldGesture.glyphSwiped(alongPx, acrossPx, slopPx, horizontal = true)

    fun pillStretches(edgeLayoutOn: Boolean, widgetPill: Boolean): Boolean = edgeLayoutOn || widgetPill

    fun intoScreenDp(dxDp: Float, cameraOnLeft: Boolean): Float = if (cameraOnLeft) dxDp else -dxDp

    fun opensUnderFinger(dxDp: Float, dyDp: Float, cameraOnLeft: Boolean, slopDp: Float = TAP_SLOP_DP): Boolean =
        abs(dxDp) > abs(dyDp) && intoScreenDp(dxDp, cameraOnLeft) > slopDp

    fun glyphMaxSlideDp(pillHeightDp: Float): Float = (pillHeightDp * 0.4f).coerceIn(8f, 56f)

    fun armAlong(dy: Float): Float = dy

    fun armInto(dx: Float, cameraOnLeft: Boolean): Float = if (cameraOnLeft) dx else -dx

    fun armedAction(direction: PillSwipeDirection?, cardOpen: Boolean, cardsAway: Boolean,
            swipesOn: Boolean = true): EdgePillAction = when {
        direction == null || cardsAway || !swipesOn -> EdgePillAction.None
        direction == PillSwipeDirection.Left || direction == PillSwipeDirection.Right -> EdgePillAction.SwitchCard
        direction == PillSwipeDirection.Down -> if (cardOpen) EdgePillAction.None else EdgePillAction.OpenCard
        else -> if (cardOpen) EdgePillAction.CloseCard else EdgePillAction.PillSwipeAction
    }

    const val ARM_SWELL = 0.04f

    const val DEAD_SHRINK = 0.04f

    fun liftZone(direction: PillSwipeDirection?, cardOpen: Boolean, cardsAway: Boolean,
            swipesOn: Boolean = true): TouchZone =
        touchZone(slopExceeded = true, liftFires = armedAction(direction, cardOpen, cardsAway,
            swipesOn) != EdgePillAction.None)

    fun swipesOn(cardOpen: Boolean, pillSwipes: Boolean, swipeActions: Boolean): Boolean =
            if (cardOpen) swipeActions else pillSwipes

    fun pullAllowed(swipesOn: Boolean, cardOpen: Boolean, cardsAway: Boolean, openingPossible: Boolean,
            intoDominant: Boolean): Boolean =
        swipesOn && !cardOpen && !cardsAway && !openingPossible && intoDominant

    fun cueScale(arm: Float, dead: Float): Float = 1f + ARM_SWELL * arm - DEAD_SHRINK * dead

    fun anchoredSideShiftPx(scaleAcross: Float, widthPx: Float, cameraOnLeft: Boolean): Float =
        (if (cameraOnLeft) 1f else -1f) * SwipeDownPull.topAnchorShiftPx(scaleAcross, widthPx)

    fun originX(cameraOnLeft: Boolean, flat: Boolean): Float = when {
        !flat -> 0.5f
        cameraOnLeft -> 0f
        else -> 1f
    }

    private const val MIN_COUNTER_SCALE = 0.01f

    fun counterScale(scale: Float): Float = 1f / scale.coerceAtLeast(MIN_COUNTER_SCALE)

    fun glyphScale(scale: Float, cue: Float): Float = cue * counterScale(scale)

    /**
     * [flat] is the look ([flatCameraSide]), [widthPx] is the resting width, used only for the compact look's
     * anchored-side shift
     */
    fun pillMotion(
        cameraOnLeft: Boolean, acrossPx: Float, alongPx: Float, arm: Float, dead: Float, pull: Float, pulseScale: Float,
        flat: Boolean = true, widthPx: Float = 0f
    ): EdgePillMotion {
        val cue = cueScale(arm, dead * SwipeDownPull.deadShrinkWeight(pull))
        val pullAcross = SwipeDownPull.pillScaleY(pull)
        val anchorShift = if (flat) 0f else anchoredSideShiftPx(pullAcross, widthPx, cameraOnLeft)
        return EdgePillMotion(
            translationX = (if (cameraOnLeft) acrossPx else -acrossPx) + anchorShift,
            translationY = alongPx,
            scaleX = cue * pullAcross,
            scaleY = pulseScale * cue * SwipeDownPull.pillScaleX(pull),
            originX = originX(cameraOnLeft, flat),
            cue = cue
        )
    }

    fun holeLocalY(yPx: Float, heightPx: Float, translationPx: Float, scale: Float): Float =
        heightPx / 2f + (yPx - translationPx - heightPx / 2f) / scale

    fun holeLocalX(xPx: Float, widthPx: Float, originX: Float, translationPx: Float, scale: Float): Float {
        val origin = widthPx * originX
        return origin + (xPx - translationPx - origin) / scale
    }

    fun holeClearWidthDp(holeWidthDp: Float, holeLengthDp: Float): Float =
        if (holeWidthDp > 0f) holeWidthDp else holeLengthDp

    const val LEAN_SLOPE = 0.35f

    const val LEAN_ALONG_MAX_DP = 24f

    fun leanAlongPx(dyPx: Float, density: Float): Float = softFollow(dyPx, LEAN_SLOPE, LEAN_ALONG_MAX_DP * density)

    fun leanAcrossPx(intoPx: Float, density: Float): Float =
            softFollow(intoPx, LEAN_SLOPE, SwipeFeel.PILL_LEAN_Y_MAX_DP * density)

    fun enterSign(dyDp: Float): Int = if (dyDp < 0f) 1 else -1

    fun enterStartPx(sign: Int, maxSlidePx: Float, fraction: Float): Float = sign * maxSlidePx * fraction

    fun enterShare(offsetPx: Float, maxSlidePx: Float, fraction: Float): Float {
        val full = maxSlidePx * fraction
        return if (full > 0f) (abs(offsetPx) / full).coerceIn(0f, 1f) else 0f
    }

    /** Finger drag as the glyphs follow it: held to [limitPx] either way with a side camera, free otherwise. */
    fun followPx(dragPx: Float, limitPx: Float?): Float = if (limitPx == null) dragPx else dragPx.coerceIn(-limitPx,
            limitPx)

    fun glyphSlidePx(
        entersNow: Boolean, pendingSign: Int, maxSlidePx: Float, fraction: Float, dragPx: Float, enterPx: Float
    ): Float = if (entersNow) enterStartPx(pendingSign, maxSlidePx, fraction) else dragPx + enterPx

    fun glyphEnterShare(entersNow: Boolean, enterPx: Float, maxSlidePx: Float, fraction: Float): Float =
        if (entersNow) 1f else enterShare(enterPx, maxSlidePx, fraction)

    fun pillTopDp(cameraCenterYDp: Float, pillHeightDp: Float): Float = cameraCenterYDp - pillHeightDp / 2f

    fun secondaryBelow(cameraCenterYDp: Float, screenHeightDp: Float,
            preference: Int = EdgePillTuning.BUBBLE_AUTO): Boolean = when (preference) {
        EdgePillTuning.BUBBLE_BELOW -> true
        EdgePillTuning.BUBBLE_ABOVE -> false
        else -> cameraCenterYDp < screenHeightDp / 2f
    }

    fun bubbleTopBesidePillDp(pillTopDp: Float, pillHeightDp: Float, bubbleSizeDp: Float, gapDp: Float,
            screenHeightDp: Float, below: Boolean): Float {
        val under = pillTopDp + pillHeightDp + gapDp
        val over = pillTopDp - gapDp - bubbleSizeDp
        fun fits(top: Float) = top >= 0f && top + bubbleSizeDp <= screenHeightDp
        val preferred = if (below) under else over
        val other = if (below) over else under
        return when {
            fits(preferred) -> preferred
            fits(other) -> other
            else -> preferred.coerceIn(0f, (screenHeightDp - bubbleSizeDp).coerceAtLeast(0f))
        }
    }

    fun cardLeftDp(screenWidthDp: Float, pillWidthDp: Float, cardWidthDp: Float, cameraOnLeft: Boolean,
            gapDp: Float = GAP_DP): Float =
        if (cameraOnLeft) pillWidthDp + gapDp else screenWidthDp - pillWidthDp - gapDp - cardWidthDp
}

object EdgePillFit {
    const val PAD_LENGTH_DP = 24f

    fun lengthFor(holeLengthDp: Float): Float =
        (holeLengthDp + PAD_LENGTH_DP)
            .coerceIn(EdgePillTuning.HEIGHT.start, EdgePillTuning.HEIGHT.endInclusive)

    /**
     * Fitted [tuning]: the hole's across width is the thickness, its length along the edge ([lengthDp], 0 when
     * unknown) sizes the pill's length
     */
    fun fit(
        tuning: EdgePillTuning,
        centreFromEdgeDp: Float,
        acrossDp: Float,
        lengthDp: Float = 0f,
        assumed: Boolean = false
    ): EdgePillTuning {
        if (centreFromEdgeDp <= 0f || acrossDp <= 0f) return tuning
        // An assumed hole (placed by hand, or guessed) is no measure of the camera: the pill keeps its default size and
        // only grows for a bigger one, where a detected hole fits it tight
        val sized = if (lengthDp > 0f) {
            val length = lengthFor(lengthDp)
            tuning.copy(heightDp = if (assumed) maxOf(length, EdgePillTuning.DEFAULT_HEIGHT) else length)
        } else tuning
        return fitAcross(sized, centreFromEdgeDp, acrossDp,
            if (assumed) EdgePillTuning.defaultWidthFor(tuning.flatEdge) else 0f)
    }

    private fun fitAcross(
        tuning: EdgePillTuning,
        centreFromEdgeDp: Float,
        acrossDp: Float,
        minWidthDp: Float
    ): EdgePillTuning {
        val w = EdgePillTuning.WIDTH
        return if (tuning.flatEdge) {
            val width = maxOf(centreFromEdgeDp + acrossDp / 2f, minWidthDp).coerceIn(w.start, w.endInclusive)
            tuning.copy(widthDp = width, xOffsetDp = 0f, yOffsetDp = 0f, cornerRadiusDp = radiusFor(width),
                cameraManual = false)
        } else {
            val width = maxOf(acrossDp, minWidthDp).coerceIn(w.start, w.endInclusive)
            val x = (centreFromEdgeDp - width / 2f).coerceAtLeast(0f)
                .coerceIn(EdgePillTuning.X_OFFSET.start, EdgePillTuning.X_OFFSET.endInclusive)
            tuning.copy(widthDp = width, xOffsetDp = x, yOffsetDp = 0f, cornerRadiusDp = radiusFor(width),
                cameraManual = false)
        }
    }

    fun fitPortrait(tuning: EdgePillTuning, hole: CutoutArea?, assumed: Boolean = false): EdgePillTuning =
        if (hole == null) tuning else fit(tuning, hole.topDp + hole.heightDp / 2f, hole.heightDp, hole.widthDp, assumed)

    fun fitLandscape(tuning: EdgePillTuning, cameraXOffsetDp: Float, cameraWidthDp: Float,
            screenWidthDp: Float, cameraLengthDp: Float = 0f, assumed: Boolean = false): EdgePillTuning =
        fit(tuning, (screenWidthDp / 2f - abs(cameraXOffsetDp)).coerceAtLeast(0f), cameraWidthDp, cameraLengthDp,
            assumed)

    private fun radiusFor(widthDp: Float): Float = (widthDp / 2f)
            .coerceIn(EdgePillTuning.RADIUS.start, EdgePillTuning.RADIUS.endInclusive)
}
