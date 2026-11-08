/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.OrientationSnap
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.collapsedCompanionSizeDp
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CollapsedSlots
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgeCardPlacement
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.ExpandTravel
import com.agupta07505.smartisland.model.ExpandedAnchor
import com.agupta07505.smartisland.model.HideSink
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.LandscapeClassicPulse
import com.agupta07505.smartisland.model.PILL_SPRING_DAMPING
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.PulseBubble
import com.agupta07505.smartisland.model.PulseCamera
import com.agupta07505.smartisland.model.PulsePeakPlacement
import com.agupta07505.smartisland.model.PulsePlacement
import com.agupta07505.smartisland.model.PulseWidth
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.model.SideCameraZone
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.model.cutoutCardCount
import com.agupta07505.smartisland.model.hasSecondaryCutout
import com.agupta07505.smartisland.util.AnimationSpeed

private fun desiredCollapsedLeft(
    side: CameraSide,
    area: CutoutArea?,
    settings: SmartIslandSettings,
    screenWidth: Dp,
    screenCenter: Dp
): Dp {
    if (side == CameraSide.Centre || area == null) {
        return screenCenter + settings.xOffset.dp - settings.width.dp / 2f
    }
    return SideCamera.desiredLeftDp(
        side, screenWidth.value, area.centreXDp, area.widthDp, settings.width, settings.xOffset
    ).dp
}

@Composable
internal fun rememberIslandLayout(
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    expanded: Boolean,
    safeIndex: Int,
    /** System status bar chip */
    isFullWidth: Boolean,
    pickerOnLeft: Boolean,
    statusBarHeight: Float,
    pickerSideBySide: Boolean,
    surfaceTwoIslands: Boolean = pickerSideBySide,
    splitCardWidthDp: Float,
    expandedHeight: Dp,
    expandedWidthRatio: Float = EXPANDED_WIDTH_RATIO,
    isAutoHidden: Boolean,
    /** Hold hid the idle cutout (edge layout, nothing on the island). */
    userHidden: Boolean = false,
    idleClosing: Boolean = false,
    launcherOpen: Boolean = false,
    context: Context,
    configuration: Configuration,
    isLandscape: Boolean,
    screenWidth: Dp,
    screenCenter: Dp,
    cardSwitching: () -> Boolean = { false },
    /** Landscape: the pill stays on the island to carry the idle widgets. */
    landscapePillStretch: Boolean = false,
    pillOpen: PillOpenDragState? = null,
    edgeCamera: EdgeCamera? = null,
    bubbleOrigin: () -> Boolean = { false },
    /** The collapsed pulse is on for this orientation (portrait), and the card it plays now, if any. */
    pulseDriven: Boolean = false,
    /** Landscape edge style: its pulse is the edge pill's, never portrait's wide pulse, whether or not the edge layout is up. */
    edgeStyle: Boolean = false,
    pulseCard: IslandNotification? = null,
    pulseRedacted: Boolean = false,
    /** Scale of the bubble's swell, shared with the wide pulse's element nearest to it. */
    bubbleSwell: State<Float>? = null
): IslandLayout {
    val expandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat(),
        ratio = expandedWidthRatio,
        pickerSideBySide = pickerSideBySide,
        splitCardWidthDp = if (splitCardWidthDp > 0f) splitCardWidthDp else null
    ).dp
    val singleExpandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat(),
        ratio = expandedWidthRatio,
        pickerSideBySide = false,
        splitCardWidthDp = null
    ).dp
    val pickerLeftFraction = splitPickerLeftFraction(expandedWidth.value, configuration.screenWidthDp.toFloat())
    // Reverse landscape: the picker is the left island
    val pickerEdgeFraction = if (pickerOnLeft) 1f - pickerLeftFraction else pickerLeftFraction
    val transition = updateTransition(targetState = expanded, label = "islandTransition")
    val motionLandscape = isLandscape
    val landscapeNoStretch = motionLandscape &&
        !EdgePillRules.pillStretches(edgeCamera != null, landscapePillStretch)

    // Landscape moves the pills and cards much further, so it gets softer, slower
    val animSpeed = settings.animationSpeed(motionLandscape)
    val springs = expandSprings(animSpeed, motionLandscape, edgeCamera, expandedWidth, expandedHeight, configuration)
    val geometry = rememberPillGeometry(
        settings, notifications, expanded, safeIndex, isLandscape, isFullWidth,
        screenWidth, screenCenter, expandedWidth, statusBarHeight, context
    )
    val edgePlacement = edgeCamera?.let {
        EdgePillLayoutMath.cardPlacement(it, expandedWidth.value)
    }
    val expandedTopOffset = expandedTopOffsetFor(
        settings, geometry, edgePlacement, isLandscape, launcherOpen,
        screenCenter, expandedWidth, expandedHeight, configuration
    )
    val alphaMs = springs.alphaMs
    val isIdleHiding = settings.hideWhenIdle && notifications.isEmpty()
    val isHiding = (isIdleHiding || userHidden || (settings.autoHidePill && isAutoHidden))
    val sinks = HideSink.applies(isLandscape, settings.enableNotchMode, edgeStyle)
    val hidingAlphaState = rememberHidingAlpha(isHiding, animSpeed, alphaMs, sinks)
    val pillBackgroundColor = Color(settings.islandSurfaceColor)
    val singleSurface = rememberSingleSurfaceAlpha(surfaceTwoIslands, animSpeed)

    val collapseOnPill = !isHiding || hidingAlphaState.value > 0.01f
    val closeMotion = rememberCloseMotion(animSpeed, expanded, idleClosing, edgeCamera, springs.travelSlowdown)

    // A turn reaches the window's configuration and the orientation read in different frames, so size changes snap for
    // a short window after either, not only the frame the flag flips (model/OrientationSnap.kt)
    val orientationSnap = remember { OrientationSnap() }
    val orientationFlipped = orientationSnap.observe(isLandscape, edgeCamera != null, SystemClock.elapsedRealtime())
    val stretchSpec = pillStretchSpec(landscapeNoStretch || orientationFlipped, animSpeed)
    val collapsedTopShift = collapsedTopShiftDp(isLandscape, settings.enableNotchMode, settings.yOffset).dp
    val anim = animateShell(
        transition, settings, expandedWidth, expandedHeight, expandedTopOffset, edgePlacement,
        collapsedTopShift, springs, closeMotion, stretchSpec, collapseOnPill, isHiding, animSpeed, cardSwitching
    )

    val pillHeightNow = settings.height.toFloat()
    val drag = pillOpen
    // -1 while the springs run the shell
    val dragProgressNow: () -> Float = { if (drag != null && drag.active) drag.progress else -1f }
    val widePulse = pulseDriven && edgeCamera == null && !edgeStyle
    // The status bar follows the system's layout direction, which the pulse's 2 reserves follow (PulseWidth.edgeReserves)
    val statusBarRtl = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    // landscape classic picks the left, centre or right pulse layout from the pill's own place (the gate
    // `landscapeClassicPulses` lets every classic pill pulse)
    val liveClassicSide = classicPulseSide(settings.enableNotchMode, isLandscape, edgeCamera != null, geometry, screenCenter.value)
    // The side is chosen when a pulse starts and held until it ends
    val heldClassicSide = remember { arrayOfNulls<CameraSide>(1) }
    val classicPulsing = widePulse && pulseCard != null
    val classicSide = LandscapeClassicPulse.heldSide(liveClassicSide, heldClassicSide[0], classicPulsing)
    SideEffect { heldClassicSide[0] = if (classicPulsing) classicSide else null }
    val anchorSide = LandscapeClassicPulse.anchorSide(widePulse, classicSide, geometry.pulseCamera.side)
    val peakPlacement = PulsePlacement.peak(
        settings.enableNotchMode, isLandscape, geometry.pulseCamera, settings.width, classicSide
    )
    val peakCard = if (widePulse && !expanded) pulseCard else null
    val peakFade = rememberPulsePeakFade(peakCard, animSpeed, pulseRedacted)
    val overCard = if (widePulse) {
        rememberOverCardPulse(
            card = if (expanded) pulseCard else null,
            settings = settings,
            geometry = geometry,
            placement = peakPlacement,
            classicSide = classicSide,
            screenWidthDp = screenWidth.value,
            statusBarRtl = statusBarRtl,
            redacted = pulseRedacted,
            animSpeed = animSpeed
        )
    } else null
    val peakPulse = if (widePulse) {
        val peakFit = rememberPulsePeakFit(
            card = peakCard,
            baseWidthDp = settings.width,
            cameraGapDp = geometry.pulseCamera.gapDp,
            max = pulseMaxWidth(settings, geometry, screenWidth.value, classicSide, statusBarRtl, peakPlacement.classic),
            cameraShiftDp = PulseWidth.cameraShiftDp(geometry.pulseCamera.centreInPillDp, settings.width),
            side = peakPlacement.side,
            glyphFromCameraDp = peakPlacement.glyphFromCameraDp,
            redacted = pulseRedacted,
            classic = peakPlacement.classic
        )
        rememberPeakPulse(peakFit, settings.width, animSpeed)
    } else null
    // The edge pill draws its own wide pulse, so it has none of the shell's
    val pulse: State<Float> = peakPulse?.pulse ?: remember { mutableFloatStateOf(0f) }
    val pulseCentreShift: State<Float> = peakPulse?.centreShiftDp ?: remember { mutableFloatStateOf(0f) }
    val pulseWidthState = remember(anim.sizes.width, pulse, settings.width) {
        // The edge pill draws the pulse on its own pill layer (see EdgePillLayer)
        derivedStateOf { if (edgeCamera != null) anim.sizes.width.value else anim.sizes.width.value + (settings.width * PILL_PULSE_WIDTH_FRACTION * pulse.value).dp }
    }

    // Taller by the line's height while the pulse draws one, gone as the card opens
    val pulseLineExtra = rememberPulseLineExtra(peakCard, animSpeed)
    val lineExtraState = remember(pulseLineExtra, anim.expandProgressY) {
        derivedStateOf { pulseLineExtra.value * (1f - anim.expandProgressY.value.coerceIn(0f, 1f)) }
    }
    val bubbleCentreX =
        if (isFullWidth) geometry.circleCenter - screenCenter else geometry.circleCenter - geometry.groupCenter
    val bubbleSize = geometry.circleSize.value
    val blendsNow: () -> Boolean = {
        edgePlacement == null && dragProgressNow() < 0f &&
            BubbleOrigin.blends(bubbleOrigin(), expanded, edgeLayout = false, fingerLed = false)
    }
    val originX = shellOriginX(
        settings, geometry, anim, expandedWidth, pulseWidthState, pulseCentreShift, edgePlacement,
        dragProgressNow, blendsNow, bubbleCentreX, bubbleSize, anchorSide
    )
    val originY = shellOriginY(
        anim, expandedHeight, expandedTopOffset, collapsedTopShift,
        pillHeightNow, edgePlacement, lineExtraState, dragProgressNow, blendsNow
    )
    val shown = rememberShownStates(
        settings, expandedWidth, expandedHeight, expanded, isHiding, drag, bubbleSize,
        pillHeightNow, collapsedTopShift, anim, pulseWidthState, lineExtraState, blendsNow
    )
    SideEffect {
        bindPillOpenDrag(drag, settings, expandedWidth, expandedHeight, pillHeightNow, anim)
    }
    // Only these two flip between composed and not
    val collapsedShownState = remember(shown.collapsedAlpha) { derivedStateOf { shown.collapsedAlpha.value > 0f } }
    val expandedShownState = remember(shown.expandedAlpha) { derivedStateOf { shown.expandedAlpha.value > 0.01f } }

    return IslandLayout(
        miniPillWidth = geometry.miniPillWidth,
        circleSize = geometry.circleSize,
        compactGap = geometry.compactGap,
        compactShapes = geometry.compactShapes,
        hasCompanion = geometry.hasCompanion,
        isCircleLeft = geometry.isCircleLeft,
        collapsedMainLeft = geometry.collapsedMainLeft,
        mainCenter = geometry.mainCenter,
        circleCenter = geometry.circleCenter,
        groupStart = geometry.groupStart,
        groupEnd = geometry.groupEnd,
        groupCenter = geometry.groupCenter,
        collapsedMainOffset = geometry.collapsedMainOffset,
        topBarHeight = geometry.topBarHeight,
        isHiding = isHiding,
        pillBackgroundColor = pillBackgroundColor,
        collapsedTopShift = collapsedTopShift,
        expandedWidth = expandedWidth,
        singleExpandedWidth = singleExpandedWidth,
        pickerLeftFraction = pickerLeftFraction,
        pickerEdgeFraction = pickerEdgeFraction,
        shown = shown,
        singleSurface = singleSurface,
        collapsedShownState = collapsedShownState,
        expandedShownState = expandedShownState,
        originX = originX,
        originY = originY,
        hidingAlphaState = hidingAlphaState,
        pulseStates = PulseStates(pulse, pulseCentreShift, lineExtraState),
        pulses = widePulse || (pulseDriven && edgeCamera != null),
        peakFade = if (widePulse) peakFade else null,
        edgePulseCard = if (pulseDriven && edgeCamera != null && !expanded) pulseCard else null,
        edgeOverCardPulse = if (pulseDriven && edgeCamera != null && expanded) pulseCard else null,
        peakPlacement = peakPlacement,
        overCard = overCard,
        peakRedacted = pulseRedacted,
        bubbleSwellState = bubbleSwell,
        cameraCentreInPillDp = geometry.cameraCentreInPillDp,
        cameraGapDp = geometry.cameraGapDp,
        cameraBand = geometry.cameraBand,
        expandedCentreOffsetDp = geometry.expandedCentreOffsetDp,
        slotSide = geometry.slotSide,
        cameraSide = geometry.cameraSide,
        pulseAnchorSide = anchorSide,
        pulseCamera = geometry.pulseCamera,
        sideZone = geometry.sideZone,
        expandStiffness = springs.expandStiffness,
        expandDamping = springs.expandDamping,
        orientationSnapping = orientationFlipped,
        sinks = sinks
    )
}

/**
 * How present the island is while it hides (idle, held, auto-hidden) and back. Where it sinks into the camera this is
 * the presence spring (the same the pill's slide uses), so a hide turned around runs back from where it is. Landscape
 * classic fades
 */
@Composable
private fun rememberHidingAlpha(isHiding: Boolean, animSpeed: AnimationSpeed, alphaMs: Int, sinks: Boolean): State<Float> =
    animateFloatAsState(
        targetValue = if (isHiding) 0f else 1f,
        animationSpec = if (sinks) {
            spring(dampingRatio = PILL_SPRING_DAMPING, stiffness = animSpeed.stiffness(PILL_SPRING_STIFFNESS))
        } else {
            tween(
                durationMillis = animSpeed.durationMs(alphaMs),
                easing = FastOutSlowInEasing
            )
        },
        label = "hidingAlpha"
    )

/** Single island's surface and shadow alpha, both 0 while 2 separate islands draw their own. */
internal class SingleSurfaceAlpha(val surface: State<Float>, val shadow: State<Float>)

@Composable
private fun rememberSingleSurfaceAlpha(surfaceTwoIslands: Boolean, animSpeed: AnimationSpeed): SingleSurfaceAlpha {
    val surface = animateFloatAsState(
        targetValue = if (surfaceTwoIslands) 0f else 1f,
        // Out (two islands open): 160 ms
        animationSpec = if (surfaceTwoIslands) {
            tween(durationMillis = animSpeed.durationMs(160))
        } else {
            snap()
        },
        label = "islandSingleSurface"
    )
    val shadow = animateFloatAsState(
        targetValue = if (surfaceTwoIslands) 0f else 1f,
        animationSpec = tween(durationMillis = animSpeed.durationMs(if (surfaceTwoIslands) 160 else 60)),
        label = "islandSingleShadow"
    )
    return SingleSurfaceAlpha(surface, shadow)
}

private fun pillStretchSpec(snapSize: Boolean, animSpeed: AnimationSpeed): FiniteAnimationSpec<Dp> =
    if (snapSize) snap()
    else spring(
        dampingRatio = PILL_STRETCH_DAMPING,
        stiffness = animSpeed.stiffness(PILL_STRETCH_STIFFNESS)
    )

internal class ExpandSprings(
    val travelSlowdown: Float,
    val sizeSpec: FiniteAnimationSpec<Dp>,
    val sizeSpecFloat: FiniteAnimationSpec<Float>,
    val heightSpec: FiniteAnimationSpec<Dp>,
    val alphaMs: Int,
    val expandStiffness: Float,
    val expandDamping: Float
)

private fun expandSprings(
    animSpeed: AnimationSpeed,
    motionLandscape: Boolean,
    edgeCamera: EdgeCamera?,
    expandedWidth: Dp,
    expandedHeight: Dp,
    configuration: Configuration
): ExpandSprings {
    val travelSlowdown = if (motionLandscape && edgeCamera != null) {
        val placement = EdgePillLayoutMath.cardPlacement(edgeCamera, expandedWidth.value)
        val cardTop = landscapeExpandedTopDp(
            pillOverlapsCard = false,
            baseDp = 0f,
            pillBottomDp = 0f,
            gapDp = COMPACT_INDICATOR_GAP_DP.toFloat(),
            edgeGapDp = LANDSCAPE_EDGE_GAP_DP,
            screenHeightDp = configuration.screenHeightDp.toFloat(),
            expandedHeightDp = expandedHeight.value
        )
        ExpandTravel.slowdown(
            edgeDxDp = placement.cardEdgeOffsetDp - placement.pillEdgeOffsetDp,
            pillCentreYDp = placement.pillCentreYDp,
            cardCentreYDp = cardTop + expandedHeight.value / 2f,
            screenHeightDp = configuration.screenHeightDp.toFloat()
        )
    } else 1f
    val expandBaseStiffness = ExpandTravel.stiffness(
        islandSpringStiffness(motionLandscape), travelSlowdown
    )
    val sizeSpec = spring<Dp>(
        dampingRatio = islandSpringDamping(motionLandscape),
        stiffness = animSpeed.stiffness(expandBaseStiffness)
    )
    val sizeSpecFloat = spring<Float>(
        dampingRatio = islandSpringDamping(motionLandscape),
        stiffness = animSpeed.stiffness(expandBaseStiffness)
    )
    val heightSpec = spring<Dp>(
        dampingRatio = islandSpringDamping(motionLandscape) + 0.04f,
        stiffness = animSpeed.stiffness(expandBaseStiffness)
    )
    val alphaMs = animSpeed.durationMs(if (motionLandscape) 280 else 220)
    // The expand spring as it really runs (speed-scaled)
    val expandStiffness = animSpeed.stiffness(expandBaseStiffness)
    val expandDamping = islandSpringDamping(motionLandscape)
    return ExpandSprings(travelSlowdown, sizeSpec, sizeSpecFloat, heightSpec, alphaMs, expandStiffness, expandDamping)
}

internal class PillGeometry(
    val compactGap: Dp,
    val miniPillWidth: Dp,
    val circleSize: Dp,
    val compactShapes: List<CompactNotificationShape>,
    val hasCompanion: Boolean,
    val cameraArea: CutoutArea?,
    val cameraSide: CameraSide,
    val isCircleLeft: Boolean,
    val collapsedMainLeft: Dp,
    val mainCenter: Dp,
    val cameraGapDp: Float,
    val cameraCentreInPillDp: Float?,
    val sideZone: SideCameraZone?,
    val pulseCamera: PulseCamera,
    val cameraBand: CameraBand,
    val slotSide: SlotSide,
    val expandedCentreOffsetDp: Float,
    val circleCenter: Dp,
    val groupStart: Dp,
    val groupEnd: Dp,
    val groupCenter: Dp,
    val collapsedMainOffset: Dp,
    val topBarHeight: Float
)

@Composable
private fun rememberPillGeometry(
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    expanded: Boolean,
    safeIndex: Int,
    isLandscape: Boolean,
    isFullWidth: Boolean,
    screenWidth: Dp,
    screenCenter: Dp,
    expandedWidth: Dp,
    statusBarHeight: Float,
    context: Context
): PillGeometry {
    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = settings.width.dp
    val circleSize = collapsedCompanionSizeDp(settings).dp
    val cutoutCards = cutoutCardCount(notifications, expanded, safeIndex)
    val compactShapes = compactNotificationShapes(cutoutCards, expanded)
    val hasCompanion = hasSecondaryCutout(cutoutCards)
    // The hole the placement reads (null in landscape and notch mode); the settled pill and the pulse read the notch's too
    val cameraArea = rememberCameraCutoutArea(settings, isLandscape)
    val pulseHole = rememberCameraCutoutArea(settings, isLandscape, withNotchHole = true, assumeCentre = true)
    val cameraSide = SideCamera.sideOf(cameraArea?.centreXDp)
    val notchGeometry = if (settings.enableNotchMode) {
        notchGroupGeometry(
            screenWidth = screenWidth.value,
            xOffset = settings.xOffset.toFloat(),
            notchWidth = settings.width.toFloat(),
            earWidth = circleSize.value,
            gap = compactGap.value,
            edgeMargin = compactGap.value,
            hasEar = hasCompanion,
            preferLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
        )
    } else null
    val isCircleLeft = notchGeometry?.earOnLeft
        ?: SideCamera.circleLeft(
            cameraSide, settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
        )
    val collapsedGroupWidth = settings.width.dp + if (hasCompanion) compactGap + circleSize else 0.dp

    val desiredMainLeft = desiredCollapsedLeft(cameraSide, cameraArea, settings, screenWidth, screenCenter)
    // landscape: the cutout may touch the screen edge (the camera sits right at it)
    val edgeMargin = if (isLandscape && !settings.enableNotchMode) 0.dp else compactGap
    val (minMainLeft, maxMainLeft) = when {
        !hasCompanion -> edgeMargin to (screenWidth - edgeMargin - settings.width.dp).coerceAtLeast(edgeMargin)
        isCircleLeft -> (edgeMargin + circleSize + compactGap) to (screenWidth - edgeMargin - settings.width.dp)
            .coerceAtLeast(edgeMargin + circleSize + compactGap)
        else -> edgeMargin to (screenWidth - edgeMargin - collapsedGroupWidth).coerceAtLeast(edgeMargin)
    }
    val collapsedMainLeft = notchGeometry?.mainLeft?.dp ?: desiredMainLeft.coerceIn(minMainLeft, maxMainLeft)
    val mainCenter = collapsedMainLeft + settings.width.dp / 2f
    // The settled pill and the pulse read 1 hole: the notch's own too, where the placement keeps its rules
    val pulseCamera = PulsePlacement.camera(pulseHole, screenCenter.value, mainCenter.value, settings.width)
    val cameraGapDp = pulseCamera.gapDp
    val cameraCentreInPillDp = pulseCamera.centreInPillDp
    val collapsedZone = CollapsedSlots.zone(settings.width, cameraCentreInPillDp, cameraGapDp)
    val sideZone = pulseCamera.sideZone
    val cameraBand = sideZone?.band ?: collapsedZone.band
    val slotSide = sideZone?.plan?.side ?: collapsedZone.side
    val expandedCentreOffsetDp = if (isLandscape || settings.enableNotchMode) 0f else {
        ExpandedAnchor.centreOffsetDp(cameraArea?.centreXDp, expandedWidth.value, screenWidth.value)
    }
    val circleLeft = if (notchGeometry != null) {
        notchGeometry.earLeft.dp
    } else if (isCircleLeft) {
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
        // Primitive Float + sentinel (0f = unavailable) instead of `Float? ?: Float`
        val landscapeHeight = if (isLandscape) landscapeStatusBarHeightDp(context) else 0f
        if (landscapeHeight > 0f) landscapeHeight else statusBarHeight
    }
    return PillGeometry(
        compactGap, miniPillWidth, circleSize, compactShapes, hasCompanion, cameraArea, pulseCamera.side,
        isCircleLeft, collapsedMainLeft, mainCenter, cameraGapDp, cameraCentreInPillDp, sideZone, pulseCamera,
        cameraBand, slotSide, expandedCentreOffsetDp, circleCenter, groupStart, groupEnd, groupCenter,
        collapsedMainOffset, topBarHeight
    )
}

@Composable
private fun rememberOverCardPulse(
    card: IslandNotification?,
    settings: SmartIslandSettings,
    geometry: PillGeometry,
    placement: PulsePeakPlacement,
    classicSide: CameraSide,
    screenWidthDp: Float,
    statusBarRtl: Boolean,
    redacted: Boolean,
    animSpeed: AnimationSpeed
): OverCardPulse {
    val fade = rememberPulsePeakFade(card, animSpeed, fadeInMs = OVER_CARD_FADE_IN_MS, fadeOutMs = OVER_CARD_FADE_OUT_MS)
    val peakFit = rememberPulsePeakFit(
        card = card,
        baseWidthDp = settings.width,
        cameraGapDp = geometry.pulseCamera.gapDp,
        max = pulseMaxWidth(settings, geometry, screenWidthDp, classicSide, statusBarRtl, placement.classic),
        cameraShiftDp = PulseWidth.cameraShiftDp(geometry.pulseCamera.centreInPillDp, settings.width),
        side = placement.side,
        glyphFromCameraDp = placement.glyphFromCameraDp,
        redacted = redacted,
        classic = placement.classic
    )
    // The pill over the card gets the line's height too; it is drawn on top, so the card stays where it is
    val lineExtra = rememberPulseLineExtra(card, animSpeed)
    val peakPulse = rememberPeakPulse(peakFit, settings.width, animSpeed)
    return OverCardPulse(fade, peakPulse.pulse, peakPulse.centreShiftDp, lineExtra)
}

/**
 * Room the wide pulse may reach, per side about the settled centre: the screen room, minus the bubble that rides out on
 * the growing edge. A centre camera's 2 sides differ ([PulseWidth.centredMaxWidth]); the rest share 1 width
 */
private fun pulseMaxWidth(
    settings: SmartIslandSettings,
    geometry: PillGeometry,
    screenWidthDp: Float,
    classicSide: CameraSide,
    statusBarRtl: Boolean,
    classic: Boolean
): PulseWidth.MaxWidth {
    val bubbleSpan = if (geometry.hasCompanion) {
        PulseBubble.spanDp(geometry.circleSize.value, geometry.compactGap.value)
    } else 0f
    val room = if (classicSide != CameraSide.Centre) {
        // off-centre landscape classic: the outer edge stays, the pill grows toward the screen centre
        PulseWidth.MaxWidth.symmetric(
            LandscapeClassicPulse.sideMaxWidthDp(classicSide, screenWidthDp, geometry.collapsedMainLeft.value,
                settings.width, bubbleSpan)
        )
    } else {
        PulseWidth.maxWidth(
            geometry.pulseCamera.side, screenWidthDp, geometry.collapsedMainLeft.value, settings.width,
            bubbleSpan, geometry.isCircleLeft, statusBarRtl
        )
    }
    return if (classic) LandscapeClassicPulse.capped(room, screenWidthDp) else room
}

private fun expandedTopOffsetFor(
    settings: SmartIslandSettings,
    geometry: PillGeometry,
    edgePlacement: EdgeCardPlacement?,
    isLandscape: Boolean,
    launcherOpen: Boolean,
    screenCenter: Dp,
    expandedWidth: Dp,
    expandedHeight: Dp,
    configuration: Configuration
): Dp {
    val hasCompanion = geometry.hasCompanion
    val circleSize = geometry.circleSize
    val groupStart = geometry.groupStart
    val groupEnd = geometry.groupEnd
    val cameraArea = geometry.cameraArea
    val topBarHeight = geometry.topBarHeight
    val baseExpandedTopOffset = calculateExpandedTopOffset(
        enableNotchMode = settings.enableNotchMode,
        hasCompanion = hasCompanion,
        statusBarHeightDp = topBarHeight,
        notchHeightDp = collapsedCompanionSizeDp(settings),
        circleSizeDp = collapsedCompanionSizeDp(settings),
        compactGapDp = COMPACT_INDICATOR_GAP_DP,
        isLandscape = isLandscape
    )
    val expandedTopOffsetBase = if (edgePlacement != null) {
        landscapeExpandedTopDp(
            pillOverlapsCard = false,
            baseDp = 0f,
            pillBottomDp = 0f,
            gapDp = COMPACT_INDICATOR_GAP_DP.toFloat(),
            edgeGapDp = LANDSCAPE_EDGE_GAP_DP,
            screenHeightDp = configuration.screenHeightDp.toFloat(),
            expandedHeightDp = expandedHeight.value
        ).dp
    } else if (isLandscape && !settings.enableNotchMode) {
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
        val cutout = cameraArea?.takeIf { it.widthDp > 0f && it.heightDp > 0f }
        if (!hasCompanion && !settings.enableNotchMode && cutout != null) {
            singleCardTopBelowCutoutDp(cutout.topDp + cutout.heightDp, settings.yOffset).dp
        } else {
            baseExpandedTopOffset.dp
        }
    }
    return if (launcherOpen) {
        if (isLandscape && !settings.enableNotchMode) LANDSCAPE_LAUNCHER_TOP_GAP_DP.dp
        else (expandedTopOffsetBase - LAUNCHER_EXPANDED_LIFT_DP.dp).coerceAtLeast(0.dp)
    } else expandedTopOffsetBase
}

/**
 * Landscape classic has no hole, so the pill's own place decides the pulse layout ([LandscapeClassicPulse.sideOf]).
 * Centre everywhere else: portrait, notch mode and the edge pill keep their own rules
 */
private fun classicPulseSide(
    notchMode: Boolean,
    isLandscape: Boolean,
    isEdge: Boolean,
    geometry: PillGeometry,
    screenCentreDp: Float
): CameraSide =
    if (!isLandscape || notchMode || isEdge) CameraSide.Centre
    else LandscapeClassicPulse.sideOf(geometry.mainCenter.value - screenCentreDp)
