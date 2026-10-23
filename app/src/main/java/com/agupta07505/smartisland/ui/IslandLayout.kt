/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.ExpandOrigin
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.hasSecondaryCutout
import com.agupta07505.smartisland.model.siblingPickerCutoutActive
import androidx.compose.runtime.getValue

/** Geometry and transition values for [IslandOverlayView]; computed outside it to keep that composable
 * small enough for the dex register limit (a 299-register method made D8 emit invalid bytecode). */
internal class IslandLayout(
    val miniPillWidth: Dp,
    val circleSize: Dp,
    val compactGap: Dp,
    val compactShapes: List<CompactNotificationShape>,
    val hasCompanion: Boolean,
    val isCircleLeft: Boolean,
    val collapsedMainLeft: Dp,
    val mainCenter: Dp,
    val circleCenter: Dp,
    val groupStart: Dp,
    val groupEnd: Dp,
    val groupCenter: Dp,
    val collapsedMainOffset: Dp,
    val topBarHeight: Float,
    val isHiding: Boolean,
    val pillBackgroundColor: Color,
    val collapsedTopShift: Dp,
    /** The card's width at rest: the width the expanded content is laid out at (see IslandOverlayView). */
    val expandedWidth: Dp,
    /** The expanded island's width without the landscape split: what the card island has at split progress 0. */
    val singleExpandedWidth: Dp,
    val pickerLeftFraction: Float,
    val pickerEdgeFraction: Float,
    // The animated values stay behind State objects / functions.
    private val widthState: State<Dp>,
    private val heightState: State<Dp>,
    private val radiusState: State<Dp>,
    private val topShiftState: State<Dp>,
    private val collapsedAlphaState: State<Float>,
    private val expandedAlphaState: State<Float>,
    private val contentScaleState: State<Float>,
    private val contentSlideYState: State<Dp>,
    private val singleSurfaceAlphaState: State<Float>,
    private val singleShadowAlphaState: State<Float>,
    private val collapsedShownState: State<Boolean>,
    private val expandedShownState: State<Boolean>,
    private val originX: () -> Dp,
    private val originY: () -> Dp,
    private val expandProgressState: State<Float>,
    private val hidingAlphaState: State<Float>
) {
    val expandProgress: Float get() = expandProgressState.value
    val singleSurfaceAlpha: Float get() = singleSurfaceAlphaState.value
    /** The single island's shadow: it still fades in (the card island's shadow is off by then), the surface does not. */
    val singleShadowAlpha: Float get() = singleShadowAlphaState.value
    val hidingAlpha: Float get() = hidingAlphaState.value
    val width: Dp get() = widthState.value
    val height: Dp get() = heightState.value
    val yOffset: Dp get() = originY()
    val topShift: Dp get() = topShiftState.value
    val animatedXOffset: Dp get() = originX()
    val collapsedAlpha: Float get() = collapsedAlphaState.value
    val expandedAlpha: Float get() = expandedAlphaState.value
    val contentScale: Float get() = contentScaleState.value
    val contentSlideY: Dp get() = contentSlideYState.value
    val safeWidth: Dp get() = (if (isHiding && hidingAlphaState.value <= 0.01f) 0.dp else widthState.value).coerceAtLeast(0.dp)
    val safeHeight: Dp get() = (if (isHiding && hidingAlphaState.value <= 0.01f) 0.dp else heightState.value).coerceAtLeast(0.dp)
    val safeRadius: Dp get() = radiusState.value.coerceAtLeast(0.dp)
    /**
     * The pill's own content layer is composed (its fade is above 0). Only changes when that flips, so reading it in
     * composition is cheap.
     */
    val collapsedShown: Boolean get() = collapsedShownState.value
    /** The card content layer is composed while it is still visible after a collapse (fade above 1 %). Same idea as [collapsedShown]. */
    val expandedShown: Boolean get() = expandedShownState.value
}

/**
 * The pill's own content. Own composable so the fade's value is read here, not in IslandOverlayView.
 *
 * While a sideways swipe is in progress the content follows the finger. When the swipe
 * puts another card in the pill, that card slides in from the opposite side ([IslandInteractionState.pillEnter]).
 */
@Composable
internal fun CollapsedContentLayer(
    layout: IslandLayout,
    fade: CollapsedContentFade,
    activeMode: IslandMode,
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: com.agupta07505.smartisland.util.AnimationSpeed,
    /** The idle widgets are held off the pill (see [com.agupta07505.smartisland.model.IdleWidgetOff]). */
    idleSuppressed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxSlidePx = with(density) { pillContentMaxSlideDp(settings.width).dp.toPx() }
    // With no card on the island the cutout can show the idle widgets (null when they are off or not due).
    val shownMode = fade.notification?.mode ?: activeMode
    val idleFrame = rememberIdleWidgetFrame(settings, noCard = shownMode == IslandMode.Empty, ui = ui, suppressed = idleSuppressed)
    // A different card is now in the pill. If a swipe asked for it, bring it in from the far side.
    val shownKey = fade.notification?.key ?: idleFrame?.kind?.id
    // The pill's tap handling and the widget island read the frame from here (no second copy of the widget data and rotation).
    androidx.compose.runtime.SideEffect { ui.idleFrameNow = idleFrame }
    androidx.compose.runtime.DisposableEffect(ui) { onDispose { ui.idleFrameNow = null } }
    androidx.compose.runtime.LaunchedEffect(shownKey) {
        val sign = ui.pendingEnterSign
        when (com.agupta07505.smartisland.model.pillEnterStep(sign, ui.pillEnter.value)) {
            com.agupta07505.smartisland.model.PillEnterStep.Nothing -> return@LaunchedEffect
            // An earlier slide was cut off (a card replaced the card mid-slide, or this layer left the screen while
            // it ran): the offset lives in the shared state, so without this the whole pill content stays shifted to
            // one side and is clipped by the pill.
            com.agupta07505.smartisland.model.PillEnterStep.SettleStale -> {
                ui.pillEnter.animateTo(
                    0f,
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = animSpeed.stiffness(CARD_ENTER_STIFFNESS)
                    )
                )
                return@LaunchedEffect
            }
            com.agupta07505.smartisland.model.PillEnterStep.Start -> Unit
        }
        // The new card starts on the side opposite to the swipe and slides to the middle.
        val from = sign * maxSlidePx * PILL_ENTER_DISTANCE_FRACTION
        ui.pendingEnterSign = 0
        ui.pillContentDragX = 0f
        ui.pillEnter.snapTo(from)
        ui.pillEnter.animateTo(
            0f,
            androidx.compose.animation.core.spring(
                // No bounce.
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = animSpeed.stiffness(CARD_ENTER_STIFFNESS)
            )
        )
    }
    Box(
        modifier = modifier
            // While the pill stretches or shrinks the content is laid out at the shell's width, so it grows and shrinks with it.
            // Otherwise (expanding, collapsing, hidden) it keeps the settled width.
            .layout { measurable, constraints ->
                val settled = settings.width.dp.roundToPx()
                val follow = layout.expandProgress <= 0.001f && !layout.isHiding
                val w = if (follow) layout.width.roundToPx().coerceAtLeast(0) else settled
                val h = settings.height.dp.roundToPx()
                val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                this.layout(w, h) { placeable.place(0, 0) }
            }
            .graphicsLayer {
                val slide = ui.pillContentDragX + ui.pillEnter.value
                translationX = slide
                // The card that just arrived eases in (a short slide and a light fade-up) instead of popping in at full strength
                // off to one side. The old content leaving is the swap itself; it is never faded.
                val enterShare = if (maxSlidePx > 0f) {
                    (kotlin.math.abs(ui.pillEnter.value) / (maxSlidePx * PILL_ENTER_DISTANCE_FRACTION)).coerceIn(0f, 1f)
                } else 0f
                alpha = layout.collapsedAlpha * fade.factor * (1f - PILL_ENTER_FADE * enterShare) * (1f - ui.chimeShow.value)
            }
    ) {
        IslandCollapsedContent(
            mode = shownMode,
            notification = fade.notification,
            collapsedAlpha = layout.collapsedAlpha,
            settings = settings,
            idleWidget = idleFrame
        )
    }
}

internal fun pillContentMaxSlideDp(pillWidthDp: Float): Float = (pillWidthDp * 0.4f).coerceIn(8f, 56f)

internal const val CARD_ENTER_STIFFNESS = 380f

/** How far the incoming card starts from the middle, as a share of the content's full slide range. */
internal const val PILL_ENTER_DISTANCE_FRACTION = 0.6f

/** How much of its opacity the incoming card starts without (0 = no fade). */
internal const val PILL_ENTER_FADE = 0.35f

/**
 * The collapsed pill's stretch / shrink between its idle and with-content widths: a little bounce on the way out,
 * settles on the way back.
 */
internal const val PILL_STRETCH_DAMPING = 0.72f
internal const val PILL_STRETCH_STIFFNESS = 420f

/** Arrival pulse of the collapsed pill: +12.5 % width (of the with-content width), out fast, back soft. The height does not change. */
internal const val PILL_PULSE_WIDTH_FRACTION = 0.125f
internal const val PILL_PULSE_OUT_STIFFNESS = 600f
internal const val PILL_PULSE_BACK_STIFFNESS = 380f

/**
 * The shell's height follow when the island stays expanded and the card's height changes (no overshoot; a little
 * stiffer than the card's own slide-in so the two end together).
 */
internal const val HEIGHT_FOLLOW_STIFFNESS = 900f

@Composable
internal fun rememberIslandLayout(
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    expanded: Boolean,
    safeIndex: Int,
    /** A system status bar chip (screen recording, camera / microphone / location) is showing. */
    isFullWidth: Boolean,
    pickerOnLeft: Boolean,
    statusBarHeight: Float,
    pickerSideBySide: Boolean,
    /**
     * The content really draws the two islands (the flag above also sizes the window early); the single surface and
     * its shadow follow this.
     */
    surfaceTwoIslands: Boolean = pickerSideBySide,
    splitCardWidthDp: Float,
    expandedHeight: Dp,
    isAutoHidden: Boolean,
    /**
     * Step 1 of the idle-circle close is running (an expanded island closing with the secondary merge): the pill
     * stays until it is over.
     */
    idleClosing: Boolean = false,
    /** The expanded island is the app-shortcuts launcher: its window sits a little higher. */
    launcherOpen: Boolean = false,
    context: android.content.Context,
    configuration: Configuration,
    isLandscape: Boolean,
    screenWidth: Dp,
    screenCenter: Dp,
    /** Unused: collapsed geometry never glides. Kept for the callers. */
    @Suppress("UNUSED_PARAMETER") snapGeometry: Boolean = false,
    /** True while a swipe's card swap is in flight: the shell's height then runs on the slide's own spring, so the two finish together. */
    cardSwitching: () -> Boolean = { false },
    /** Landscape: the pill stays on the island to carry the idle widgets, so a card arriving or leaving stretches / shrinks it in place. */
    landscapePillStretch: Boolean = false,
    /** The widget island is open or still shrinking into the cutout: a card arriving does not pulse the cutout. */
    holdArrivalPulse: Boolean = false
): IslandLayout {
    val expandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat(),
        pickerSideBySide = pickerSideBySide,
        splitCardWidthDp = if (splitCardWidthDp > 0f) splitCardWidthDp else null
    ).dp
    val singleExpandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat(),
        pickerSideBySide = false,
        splitCardWidthDp = null
    ).dp
    // Where the picker island starts, as a fraction of the island's width (landscape split layout).
    val pickerLeftFraction = splitPickerLeftFraction(expandedWidth.value, configuration.screenWidthDp.toFloat())
    // Reverse landscape: the picker is the left island, so its inner edge is the mirrored fraction.
    val pickerEdgeFraction = if (pickerOnLeft) 1f - pickerLeftFraction else pickerLeftFraction
    val transition = updateTransition(targetState = expanded, label = "islandTransition")

    // Landscape moves the pills and cards much further, so it gets softer, slower, less bouncy springs; portrait is
    // gentler.
    val animSpeed = settings.animationSpeed(isLandscape)
    val sizeSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = islandSpringDamping(isLandscape),
        stiffness = animSpeed.stiffness(islandSpringStiffness(isLandscape))
    )
    val sizeSpecFloat = spring<Float>(
        dampingRatio = islandSpringDamping(isLandscape),
        stiffness = animSpeed.stiffness(islandSpringStiffness(isLandscape))
    )
    val heightSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = islandSpringDamping(isLandscape) + 0.04f,
        stiffness = animSpeed.stiffness(islandSpringStiffness(isLandscape))
    )
    // Collapsed geometry changing while the island stays collapsed (a card arriving or leaving): no glide.
    val settleOrSnap: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.Dp> =
        androidx.compose.animation.core.snap()
    val alphaMs = animSpeed.durationMs(if (isLandscape) 280 else 220)
    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = settings.width.dp
    val circleSize = com.agupta07505.smartisland.data.collapsedCompanionSizeDp(settings).dp
    val cutoutCards = com.agupta07505.smartisland.model.cutoutCardCount(notifications, expanded, safeIndex)
    val compactShapes = compactNotificationShapes(cutoutCards, expanded)
    val hasCompanion = hasSecondaryCutout(cutoutCards, settings.enableNotchMode)
    val isCircleLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
    val collapsedGroupWidth = settings.width.dp + if (hasCompanion) compactGap + circleSize else 0.dp

    val desiredMainLeft = screenCenter + settings.xOffset.dp - settings.width.dp / 2f
    // Landscape: the cutout may touch the screen edge (the camera sits right at it); elsewhere keep a margin.
    val edgeMargin = if (isLandscape && !settings.enableNotchMode) 0.dp else compactGap
    val (minMainLeft, maxMainLeft) = when {
        !hasCompanion -> edgeMargin to (screenWidth - edgeMargin - settings.width.dp).coerceAtLeast(edgeMargin)
        isCircleLeft -> (edgeMargin + circleSize + compactGap) to (screenWidth - edgeMargin - settings.width.dp).coerceAtLeast(edgeMargin + circleSize + compactGap)
        else -> edgeMargin to (screenWidth - edgeMargin - collapsedGroupWidth).coerceAtLeast(edgeMargin)
    }
    val collapsedMainLeft = desiredMainLeft.coerceIn(minMainLeft, maxMainLeft)
    val mainCenter = collapsedMainLeft + settings.width.dp / 2f
    val circleLeft = if (isCircleLeft) {
        collapsedMainLeft - compactGap - circleSize
    } else {
        collapsedMainLeft + settings.width.dp + compactGap
    }
    val circleCenter = circleLeft + circleSize / 2f
    val groupStart = if (isCircleLeft && hasCompanion) circleLeft else collapsedMainLeft
    val groupEnd = if (!isCircleLeft && hasCompanion) circleLeft + circleSize else collapsedMainLeft + settings.width.dp
    val groupCenter = (groupStart + groupEnd) / 2f

    val collapsedMainOffset = if (settings.enableNotchMode) {
        settings.xOffset.dp
    } else if (isFullWidth) {
        mainCenter - screenCenter
    } else {
        mainCenter - groupCenter
    }
    val topBarHeight = remember(isLandscape, statusBarHeight) {
        // Primitive Float + sentinel (0f = unavailable) instead of `Float? ?: Float`:
        // the boxed-Float elvis inside this inlined lambda generated bytecode ART's verifier rejects.
        val landscapeHeight = if (isLandscape) landscapeStatusBarHeightDp(context) else 0f
        if (landscapeHeight > 0f) landscapeHeight else statusBarHeight
    }
    val baseExpandedTopOffset = calculateExpandedTopOffset(
        enableNotchMode = settings.enableNotchMode,
        hasCompanion = hasCompanion,
        statusBarHeightDp = topBarHeight,
        notchHeightDp = com.agupta07505.smartisland.data.collapsedCompanionSizeDp(settings),
        circleSizeDp = com.agupta07505.smartisland.data.collapsedCompanionSizeDp(settings),
        compactGapDp = COMPACT_INDICATOR_GAP_DP,
        isLandscape = isLandscape
    )
    // Landscape: where the expanded card starts depends on where the collapsed pill sits.
    // - Pill in a corner (clear of the card): the card moves up to a small gap from the screen edge.
    // - Pill in the middle (it would sit on the card): the card moves down to just below the pill.
    val expandedTopOffsetBase = if (isLandscape && !settings.enableNotchMode) {
        val cardLeft = screenCenter - expandedWidth / 2f
        val cardRight = screenCenter + expandedWidth / 2f
        val pillOverlapsCard = landscapePillOverlapsCard(
            groupStartDp = groupStart.value,
            groupEndDp = groupEnd.value,
            cardLeftDp = cardLeft.value,
            cardRightDp = cardRight.value,
            gapDp = COMPACT_INDICATOR_GAP_DP.toFloat()
        )
        val pillBottomDp = collapsedTopShiftDp(isLandscape, settings.enableNotchMode, settings.yOffset) +
            maxOf(settings.height.toFloat(), if (hasCompanion) circleSize.value else 0f)
        landscapeExpandedTopDp(
            pillOverlapsCard = pillOverlapsCard,
            baseDp = baseExpandedTopOffset,
            pillBottomDp = pillBottomDp,
            gapDp = COMPACT_INDICATOR_GAP_DP.toFloat(),
            edgeGapDp = LANDSCAPE_EDGE_GAP_DP,
            screenHeightDp = configuration.screenHeightDp.toFloat(),
            expandedHeightDp = expandedHeight.value
        ).dp
    } else {
        baseExpandedTopOffset.dp
    }
    // The shortcuts window in landscape is the collapsed cutout grown (it has no cards, so no corner pill stays
    // beside it), and there is no camera strip to clear as in portrait: it sits just under the top edge instead of
    // leaving the card gap or the pill's height above it.
    val expandedTopOffset = if (launcherOpen) {
        if (isLandscape && !settings.enableNotchMode) LANDSCAPE_LAUNCHER_TOP_GAP_DP.dp
        else (expandedTopOffsetBase - LAUNCHER_EXPANDED_LIFT_DP.dp).coerceAtLeast(0.dp)
    } else expandedTopOffsetBase
    val isIdleHiding = settings.hideWhenIdle && notifications.isEmpty()
    val isHiding = (isIdleHiding || (settings.autoHidePill && isAutoHidden))
    val hidingAlphaState = animateFloatAsState(
        targetValue = if (isHiding) 0f else 1f,
        animationSpec = tween(
            durationMillis = animSpeed.durationMs(alphaMs),
            easing = FastOutSlowInEasing
        ),
        label = "hidingAlpha"
    )
    val pillBackgroundColor = Color(settings.pillColor)
    // 0 while the two separate islands draw their own surfaces, 1 for the single island.
    val singleSurfaceAlphaState = animateFloatAsState(
        targetValue = if (surfaceTwoIslands) 0f else 1f,
        // Out (two islands open): 160 ms. In (two islands gone): at once.
        animationSpec = if (surfaceTwoIslands) {
            tween(durationMillis = animSpeed.durationMs(160))
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "islandSingleSurface"
    )
    // The shadow fades: the card island's shadow is already off (SplitMotion.shadowStrength), and a shadow that
    // appears at full strength in one frame would pop.
    val singleShadowAlphaState = animateFloatAsState(
        targetValue = if (surfaceTwoIslands) 0f else 1f,
        animationSpec = tween(durationMillis = animSpeed.durationMs(if (surfaceTwoIslands) 160 else 60)),
        label = "islandSingleShadow"
    )

    // A collapse that lands on a visible pill uses one shared motion in both orientations (width bounces a little,
    // height / radius / vertical progress settle without overshoot).
    val collapseOnPill = !isHiding || hidingAlphaState.value > 0.01f
    // The expand spring as it really runs (speed-scaled), which the expand fades are read off.
    val expandStiffness = animSpeed.stiffness(islandSpringStiffness(isLandscape))
    val expandDamping = islandSpringDamping(isLandscape)
    // A close started by the idle circle, in both orientations.
    val idlePressLatch = remember { BooleanArray(1) }
    if (expanded) idlePressLatch[0] = false else if (idleClosing) idlePressLatch[0] = true
    val idlePressClose = idlePressLatch[0]
    // The close is step 1 of the two-step sequence: its springs are sized so the shell has settled when the pill may leave.
    val idleSequence = remember(animSpeed) { com.agupta07505.smartisland.model.IdleCloseSequence.plan(animSpeed) }
    val closeStiffness = if (idlePressClose) {
        animSpeed.systemStiffness(idleSequence.closeStiffness)
    } else {
        animSpeed.stiffness(com.agupta07505.smartisland.model.CollapseMotion.STIFFNESS)
    }
    val closeWidthDamping = com.agupta07505.smartisland.model.CollapseMotion.closeWidthDamping(idlePressClose)
    val collapseWidthSpec = spring<androidx.compose.ui.unit.Dp>(dampingRatio = closeWidthDamping, stiffness = closeStiffness)
    val collapseSettleSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = com.agupta07505.smartisland.model.CollapseMotion.SETTLE_DAMPING,
        stiffness = closeStiffness
    )
    val collapseWidthSpecFloat = spring<Float>(dampingRatio = closeWidthDamping, stiffness = closeStiffness)
    val collapseSettleSpecFloat = spring<Float>(
        dampingRatio = com.agupta07505.smartisland.model.CollapseMotion.SETTLE_DAMPING,
        stiffness = closeStiffness
    )

    // The collapsed pill stretching to its with-content width and shrinking back (portrait): a card arriving, a tap
    // that brings the stored cards back, and the idle timeout putting them away all change the pill's width while the
    // island stays collapsed.
    val lastLandscape = remember { BooleanArray(1) { isLandscape } }
    val orientationFlipped = lastLandscape[0] != isLandscape
    lastLandscape[0] = isLandscape
    val stretchSpec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.Dp> =
        if ((isLandscape && !landscapePillStretch) || orientationFlipped) androidx.compose.animation.core.snap()
        else spring(
            dampingRatio = PILL_STRETCH_DAMPING,
            stiffness = animSpeed.stiffness(PILL_STRETCH_STIFFNESS)
        )
    val widthState = transition.animateDp(transitionSpec = { if (initialState == targetState) stretchSpec else if (!targetState && collapseOnPill) collapseWidthSpec else sizeSpec }, label = "islandWidth") {
        if (it) expandedWidth else settings.width.dp
    }
    // The height of an island that STAYS expanded (another card is swiped in, a text opens, a step finishes).
    val heightFollowSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = animSpeed.stiffness(HEIGHT_FOLLOW_STIFFNESS)
    )
    // A swipe's card swap: the same (no bounce) spring as the swap itself (swipeEaseBackSpring), so height, lean and the two
    val cardSwitchHeightSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = com.agupta07505.smartisland.util.SwipeFeel.RETURN_DAMPING_RATIO,
        stiffness = com.agupta07505.smartisland.util.SwipeFeel.EASE_BACK_STIFFNESS
    )
    val heightState = transition.animateDp(transitionSpec = {
        val switching = androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation { cardSwitching() }
        val expandedAtRest = androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
            // currentState only turns true when the whole transition has finished, so this is "fully open".
            transition.currentState && transition.targetState
        }
        if (expandedAtRest) (if (switching) cardSwitchHeightSpec else heightFollowSpec)
        else if (initialState == targetState) settleOrSnap
        else if (!targetState && collapseOnPill) collapseSettleSpec
        else heightSpec
    }, label = "islandHeight") {
        if (it) expandedHeight else settings.height.dp
    }
    // Landscape only: lowers the collapsed cutouts inside a window that starts at the screen top.
    val collapsedTopShift = collapsedTopShiftDp(isLandscape, settings.enableNotchMode, settings.yOffset).dp
    val topShiftState = transition.animateDp(transitionSpec = { if (initialState == targetState) settleOrSnap else sizeSpec }, label = "islandTopShift") {
        if (it) 0.dp else collapsedTopShift
    }
    val radiusState = transition.animateDp(transitionSpec = { if (initialState == targetState) settleOrSnap else if (!targetState && collapseOnPill) collapseSettleSpec else sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else settings.cornerRadius.dp
    }

    // Expanding: the collapsed content leaves quickly while the card grows, then the card content
    // fades in. Collapsing is the mirror image, so the two never cross-fade into a muddy middle.
    val collapsedAlphaState = transition.animateFloat(
        transitionSpec = {
            when {
                initialState == targetState -> tween(alphaMs, easing = FastOutSlowInEasing)
                // The pill's content is gone when the shell is a quarter grown.
                targetState -> tween(
                    com.agupta07505.smartisland.model.ExpandMotion.pillOutMs(expandStiffness, expandDamping),
                    easing = FastOutSlowInEasing
                )
                // The pill's own content waits until the width is almost home, so the width dip does not
                // squeeze it. Landscape's idle-circle close (the pill leaves at once) and a hidden landing keep their own timing.
                collapseOnPill && !idlePressClose -> tween(
                    durationMillis = com.agupta07505.smartisland.model.CollapseMotion.pillContentInMs(alphaMs),
                    // From how far the island is open right now, not always from full size.
                    delayMillis = com.agupta07505.smartisland.model.CollapseMotion.pillContentDelayFromMs(
                        expandProgress = androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
                            val pillW = settings.width.dp.value
                            val travel = expandedWidth.value - pillW
                            if (travel <= 0f) 1f else ((widthState.value.value - pillW) / travel)
                        },
                        stiffness = closeStiffness,
                        damping = closeWidthDamping
                    ),
                    easing = FastOutSlowInEasing
                )
                else -> tween(alphaMs, delayMillis = alphaMs / 3, easing = FastOutSlowInEasing)
            }
        },
        label = "collapsedAlpha"
    ) {
        if (it || isHiding) 0f else 1f
    }

    val expandedAlphaState = transition.animateFloat(
        transitionSpec = {
            // The card content starts with the growth and is complete when the shell is 90 % grown
            // The shell clips it, so it is revealed as the shell grows.
            if (targetState) tween(
                durationMillis = com.agupta07505.smartisland.model.ExpandMotion.cardInMs(expandStiffness, expandDamping),
                delayMillis = com.agupta07505.smartisland.model.ExpandMotion.cardInDelayMs(expandStiffness, expandDamping),
                easing = androidx.compose.animation.core.LinearOutSlowInEasing
            )
            else if (idlePressClose) tween(animSpeed.systemDurationMs(idleSequence.contentOutMs), easing = FastOutSlowInEasing)
            else tween(com.agupta07505.smartisland.model.CollapseMotion.cardContentOutMs(alphaMs), easing = FastOutSlowInEasing)
        },
        label = "expandedAlpha"
    ) {
        if (it) 1f else 0f
    }

    // The expand spring as one number (0 = pill, 1 = card).
    val expandProgressState = transition.animateFloat(
        transitionSpec = { if (initialState == targetState) androidx.compose.animation.core.snap() else if (!targetState && collapseOnPill) collapseWidthSpecFloat else sizeSpecFloat },
        label = "islandExpandProgress"
    ) {
        if (it) 1f else 0f
    }
    // The same number for the vertical axis: it follows the height spring (no overshoot on a visible-pill collapse),
    // so the pill's centre row stays pinned while the card shrinks into it.
    val expandProgressYState = transition.animateFloat(
        transitionSpec = { if (initialState == targetState) androidx.compose.animation.core.snap() else if (!targetState && collapseOnPill) collapseSettleSpecFloat else sizeSpecFloat },
        label = "islandExpandProgressY"
    ) {
        if (it) 1f else 0f
    }

    val contentScaleState = transition.animateFloat(
        transitionSpec = { sizeSpecFloat },
        label = "contentScale"
    ) {
        if (it) 1f else 0.95f
    }

    val contentSlideYState = transition.animateDp(
        transitionSpec = { sizeSpec },
        label = "contentSlideY"
    ) {
        if (it) 0.dp else (-6).dp
    }

    // The island grows around the pill's centre instead of sliding to the card's centre. The frame's centre line is the
    // expanded card's centre (offset 0), so the pill's centre is `collapsedMainOffset`.
    val pillHeightNow = settings.height.toFloat()
    val originX: () -> Dp = {
        val size = widthState.value.coerceAtLeast(0.dp).value
        val growStartX = ExpandOrigin.start(
            progress = expandProgressState.value,
            size = size,
            pillCentre = collapsedMainOffset.value,
            cardStart = -expandedWidth.value / 2f,
            cardSize = expandedWidth.value
        )
        (growStartX + size / 2f).dp
    }
    val originY: () -> Dp = {
        val size = heightState.value.coerceAtLeast(0.dp).value
        val growTop = ExpandOrigin.start(
            progress = expandProgressYState.value,
            size = size,
            pillCentre = collapsedTopShift.value + pillHeightNow / 2f,
            cardStart = expandedTopOffset.value,
            cardSize = expandedHeight.value
        ).coerceAtLeast(minOf(0f, collapsedTopShift.value, expandedTopOffset.value))
        // The outer Box already pads by topShift, so the translation is what is left to reach the top edge.
        (growTop - topShiftState.value.coerceAtLeast(0.dp).value).dp
    }
    // Only these two flip between composed and not; everything else about the fades is read at draw time.
    val collapsedShownState = remember(collapsedAlphaState) { derivedStateOf { collapsedAlphaState.value > 0f } }
    val expandedShownState = remember(expandedAlphaState) { derivedStateOf { expandedAlphaState.value > 0.01f } }

    // Arrival pulse: the pill grows by PILL_PULSE_WIDTH_FRACTION and settles back, added on top of the base width springs.
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }
    val lastPulseAt = remember { longArrayOf(-com.agupta07505.smartisland.model.PillPulse.MIN_GAP_MS) }
    val seenKeys = remember { HashSet<String>().apply { notifications.forEach { add(it.key) } } }
    seenKeys.retainAll(notifications.map { it.key }.toSet())
    val arrivalKey = notifications.firstOrNull { seenKeys.add(it.key) }?.key
    LaunchedEffect(arrivalKey) {
        if (arrivalKey == null || expanded || holdArrivalPulse || (isLandscape && !landscapePillStretch)) return@LaunchedEffect
        val now = android.os.SystemClock.elapsedRealtime()
        if (!com.agupta07505.smartisland.model.PillPulse.allowed(now, lastPulseAt[0])) return@LaunchedEffect
        lastPulseAt[0] = now
        pulse.snapTo(0f)
        pulse.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = animSpeed.stiffness(PILL_PULSE_OUT_STIFFNESS)))
        pulse.animateTo(0f, spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = animSpeed.stiffness(PILL_PULSE_BACK_STIFFNESS)))
    }
    LaunchedEffect(expanded, holdArrivalPulse) { if (expanded || holdArrivalPulse) pulse.snapTo(0f) }
    val pulseWidthState = remember(widthState, pulse, settings.width) {
        derivedStateOf { widthState.value + (settings.width * PILL_PULSE_WIDTH_FRACTION * pulse.value).dp }
    }

    return IslandLayout(
        miniPillWidth = miniPillWidth,
        circleSize = circleSize,
        compactGap = compactGap,
        compactShapes = compactShapes,
        hasCompanion = hasCompanion,
        isCircleLeft = isCircleLeft,
        collapsedMainLeft = collapsedMainLeft,
        mainCenter = mainCenter,
        circleCenter = circleCenter,
        groupStart = groupStart,
        groupEnd = groupEnd,
        groupCenter = groupCenter,
        collapsedMainOffset = collapsedMainOffset,
        topBarHeight = topBarHeight,
        isHiding = isHiding,
        pillBackgroundColor = pillBackgroundColor,
        collapsedTopShift = collapsedTopShift,
        expandedWidth = expandedWidth,
        singleExpandedWidth = singleExpandedWidth,
        pickerLeftFraction = pickerLeftFraction,
        pickerEdgeFraction = pickerEdgeFraction,
        widthState = pulseWidthState,
        heightState = heightState,
        radiusState = radiusState,
        topShiftState = topShiftState,
        collapsedAlphaState = collapsedAlphaState,
        expandedAlphaState = expandedAlphaState,
        contentScaleState = contentScaleState,
        contentSlideYState = contentSlideYState,
        singleSurfaceAlphaState = singleSurfaceAlphaState,
        singleShadowAlphaState = singleShadowAlphaState,
        collapsedShownState = collapsedShownState,
        expandedShownState = expandedShownState,
        originX = originX,
        originY = originY,
        expandProgressState = expandProgressState,
        hidingAlphaState = hidingAlphaState
    )
}

/** [factor] reads the fade where it is drawn (a graphicsLayer block), so the fade does not recompose the overlay every frame. */
internal class CollapsedContentFade(val notification: IslandNotification?, private val presence: State<Float>?) {
    val factor: Float get() = presence?.value ?: 1f
}

@Composable
internal fun rememberCollapsedContentFade(
    active: IslandNotification?,
    animSpeed: com.agupta07505.smartisland.util.AnimationSpeed = com.agupta07505.smartisland.util.AnimationSpeed.NORMAL,
    /** The landscape pill is sliding away: its content stays on it and fades with the slide. Otherwise it goes at once. */
    fadeOut: Boolean = false
): CollapsedContentFade {
    // The last shown card lives in plain memory (state written from a SideEffect would make every card update recompose
    // twice), and the fade is a State read at draw time (not here, per frame).
    val lastShown = remember { arrayOfNulls<IslandNotification>(1) }
    if (active != null) lastShown[0] = active
    val presence = animateFloatAsState(
        targetValue = if (active != null) 1f else 0f,
        animationSpec = if (active == null && !fadeOut) androidx.compose.animation.core.snap()
        else tween(durationMillis = animSpeed.durationMs(300), easing = FastOutSlowInEasing),
        label = "collapsedContentPresence"
    )
    // Flips once, when the card has faded out completely: only then is the held card dropped.
    val faded by remember(active == null, presence) { derivedStateOf { active == null && presence.value <= 0.001f } }
    if (faded) lastShown[0] = null
    val held = active ?: lastShown[0]
    // With nothing held the idle content shows at full strength; otherwise it follows the fade.
    return CollapsedContentFade(held, if (held == null) null else presence)
}
