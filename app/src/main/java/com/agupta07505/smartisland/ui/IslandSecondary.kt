/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.shouldShowIdleDot
import com.agupta07505.smartisland.model.BubbleIntoCutout
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.ExpandMotion
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.PulseBubble
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.nextSameAppIndex
import com.agupta07505.smartisland.model.pillOnIsland
import com.agupta07505.smartisland.model.pillSpringSettleMs
import com.agupta07505.smartisland.model.secondaryCardIndex
import com.agupta07505.smartisland.model.tertiaryCardIndex
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.MediaArt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PICKER_SWAP_MS = 50

/**
 * The secondary bubble, the tertiary pill and the cutout beside them, grouped by what changes together. Animated
 * values stay as [State] inside their group: read `.value` where you draw, not earlier, or the whole caller
 * recomposes on every frame
 */
internal class IslandSecondary(
    val cards: SecondaryCards,
    val anims: SecondaryAnimations,
    val offsets: SecondaryOffsets,
    val merge: SecondaryMerge,
    val picker: PickerFlight,
    val glide: GlideSpec,
    val idleDot: IdleDotState,
    val shown: ShownFlags,
    val cornerPillCenterRel: Dp,
    private val circleSize: Dp,
    private val miniPillWidth: Dp,
    private val swell: State<Float>?
) {
    /** 1 at rest, bigger while the bubble swells with a pulse. */
    val swellScale: Float get() = swell?.value ?: 1f

    /** Secondary bubble is the pill on the cutout's spot (the picker button and a round bubble are not). */
    val holdsPillSpot: Boolean get() = cards.secondaryIsPill

    /** Cutout id of the secondary bubble (see [com.agupta07505.smartisland.model.TapSwap]). */
    val cutoutId: Int get() = TapSwap.secondaryCutoutId(cards.secondaryIsPill)

    val secondaryPillProgress: Float
        get() {
            val delta = (miniPillWidth - circleSize).value
            return if (delta > -0.0001f && delta < 0.0001f) {
                if (cards.secondaryIsPill) 1f else 0f
            } else {
                ((anims.bubbleWidth.value - circleSize).value / delta).coerceIn(0f, 1f)
            }
        }

    /** Light stretch along the glide (1 = none), biggest at the start, gone once the bubble is on the cutout. */
    fun glideStretch(): Float {
        if (!glide.runs) return 1f
        val span = abs((glide.from - glide.to).value)
        if (span < 0.5f) return 1f
        val remaining = abs((offsets.state.value - glide.to).value) / span
        return BubbleIntoCutout.stretch(remaining)
    }

    /** Amount the picker has appeared in the bubble's old spot (1 when no glide runs). */
    fun pickerArrival(): Float =
        if (picker.run) BubbleIntoCutout.pickerArrival(picker.flight.value) else 1f

    fun pickerScale(): Float =
        if (picker.run) BubbleIntoCutout.pickerScale(picker.flight.value) else 1f
}

/** Where the bubble glides from and to when the card closes onto the cutout. */
internal class GlideSpec(val from: Dp, val to: Dp, val runs: Boolean)

internal class IdleDotState(val shown: Boolean, val alpha: Float)

/** Derived on/off flags: each flips rarely, so reading 1 does not recompose on every animation frame. */
internal class ShownFlags(
    val cornerPill: State<Boolean>,
    val secondary: State<Boolean>,
    val tertiary: State<Boolean>,
    val mergeBackdrop: State<Boolean>
)

internal class SecondaryCards(
    val sameAppCompanion: Boolean,
    val secondaryNotification: IslandNotification?,
    val secondaryIndex: Int,
    val tertiaryNotification: IslandNotification?,
    val tertiaryIndex: Int,
    val secondaryIsPill: Boolean,
    val showTertiaryPill: Boolean
)

private fun secondaryCardsOf(
    notifications: List<IslandNotification>,
    safeIndex: Int,
    expanded: Boolean,
    layout: IslandLayout,
    pulsingId: String?
): SecondaryCards {
    // a pulsing card takes the cutout, the bubble holds the best one besides it
    val basisIndex = if (expanded) safeIndex else PulseBubble.basisIndex(notifications, pulsingId, safeIndex)
    val sameAppRule = expanded
    val sameAppIndex = if (sameAppRule) {
        nextSameAppIndex(notifications, safeIndex)
    } else -1
    val sameAppCompanion = sameAppIndex >= 0
    val secondaryNotification = notifications.getOrNull(secondaryCardIndex(notifications, basisIndex, sameAppRule))
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    val tertiaryNotification = if (notifications.size >= 3) {
        notifications.getOrNull(
            tertiaryCardIndex(notifications, basisIndex, secondaryIndex)
        )
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val compactShapes = layout.compactShapes
    val secondaryIsPill = !sameAppCompanion &&
        compactShapes.singleOrNull() == CompactNotificationShape.MiniPill
    val showTertiaryPill = (compactShapes.size == 2 || sameAppCompanion) && tertiaryNotification != null
    return SecondaryCards(
        sameAppCompanion = sameAppCompanion,
        secondaryNotification = secondaryNotification,
        secondaryIndex = secondaryIndex,
        tertiaryNotification = tertiaryNotification,
        tertiaryIndex = tertiaryIndex,
        secondaryIsPill = secondaryIsPill,
        showTertiaryPill = showTertiaryPill
    )
}

private class BubbleFlight(
    val cornerPillVisible: Boolean,
    val fromBubble: Boolean,
    val bubbleFlies: Boolean,
    val cornerHandsBack: Boolean,
    val portraitGlides: Boolean,
    val pickerGlides: Boolean,
    val glides: Boolean,
    val pickerSettled: MutableState<Boolean>,
    val pickerRun: Boolean,
    val flightMs: Int,
    val bubbleWasOut: MutableState<Boolean>
)

@Composable
private fun rememberBubbleFlight(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    cards: SecondaryCards,
    expanded: Boolean,
    isLandscape: Boolean,
    bubbleOpen: Boolean,
    onFlightChange: (Boolean) -> Unit
): BubbleFlight {
    val hasCompanion = layout.hasCompanion
    val isHiding = layout.isHiding
    // like the portrait mini pill beside the open card
    val cornerPillVisible = isLandscape && !settings.enableNotchMode && expanded && !isHiding &&
        cards.secondaryIsPill && cards.secondaryNotification != null && cards.secondaryIndex >= 0
    val bubbleWasOut = remember { mutableStateOf(false) }
    val fromBubble = BubbleOrigin.opens(bubbleOpen, expanded)
    val bubbleFlies = cornerPillVisible && bubbleWasOut.value && !fromBubble
    // Landscape: the collapsed cutout beside the open card was showing and the island now closes. The bubble takes its
    // place in the same frame, same size and spot, and leaves as portrait's bubble does
    var cornerWasShown by remember { mutableStateOf(false) }
    val cornerHandsBack = cornerWasShown && !expanded && !isHiding && hasCompanion
    SideEffect { cornerWasShown = cornerPillVisible }
    // Portrait and notch mode: the bubble flies into the collapsed cutout and turns into it
    val portraitGlides = !isLandscape && expanded && cards.secondaryIsPill && bubbleWasOut.value && !fromBubble
    // picker takes the bubble's spot, the bubble still glides to the cutout where the other event waits
    val pickerGlides = !isHiding && BubbleIntoCutout.flies(
        expanded = expanded,
        bubbleWasOut = bubbleWasOut.value && !fromBubble,
        cutoutTakesEvent = cards.sameAppCompanion && cards.showTertiaryPill
    )
    val pickerSettled = remember { mutableStateOf(false) }
    val pickerRun = pickerGlides && !pickerSettled.value
    // Glide lasts the island's whole expand (the spring already carries the animation speed)
    val flightMs = ExpandMotion.glideMs(layout.expandStiffness, layout.expandDamping)
    val flightNow = expanded && (bubbleFlies || portraitGlides || pickerGlides)
    val onFlightChangeNow = rememberUpdatedState(onFlightChange)
    LaunchedEffect(expanded) {
        if (flightNow) {
            onFlightChangeNow.value(true)
            delay(flightMs.toLong())
        }
        onFlightChangeNow.value(false)
    }
    DisposableEffect(Unit) { onDispose { onFlightChangeNow.value(false) } }
    return BubbleFlight(
        cornerPillVisible = cornerPillVisible,
        fromBubble = fromBubble,
        bubbleFlies = bubbleFlies,
        cornerHandsBack = cornerHandsBack,
        portraitGlides = portraitGlides,
        pickerGlides = pickerGlides,
        glides = expanded && (bubbleFlies || portraitGlides),
        pickerSettled = pickerSettled,
        pickerRun = pickerRun,
        flightMs = flightMs,
        bubbleWasOut = bubbleWasOut
    )
}

internal class SecondaryAnimations(
    val cornerPillAlpha: State<Float>,
    val secondaryAlpha: State<Float>,
    val secondaryScale: State<Float>,
    val bubbleWidth: State<Dp>,
    val bubbleCorner: State<Dp>,
    val tertiaryAlpha: State<Float>,
    val tertiaryScale: State<Float>
)

@Composable
private fun rememberSecondaryAnimations(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    animSpeed: AnimationSpeed,
    cards: SecondaryCards,
    flight: BubbleFlight,
    tapSwapRunning: Boolean
): SecondaryAnimations {
    val miniPillWidth = layout.miniPillWidth
    val circleSize = layout.circleSize
    val isHiding = layout.isHiding
    val isSplitMode = layout.hasCompanion
    val secondaryIsPill = cards.secondaryIsPill
    val showTertiaryPill = cards.showTertiaryPill
    val cornerPillVisible = flight.cornerPillVisible
    val cornerHandsBack = flight.cornerHandsBack
    val fromBubble = flight.fromBubble
    val bubbleFlies = flight.bubbleFlies
    val pickerRun = flight.pickerRun
    val flightMs = flight.flightMs
    val cornerPillAlphaState = animateFloatAsState(
        targetValue = if (cornerPillVisible) 1f else 0f,
        animationSpec = if (cornerHandsBack || fromBubble) {
            snap()
        } else if (bubbleFlies) {
            // bubble flies all the way to the cutout's spot first, the cutout takes over there not on the way
            tween<Float>(durationMillis = 1, delayMillis = flightMs)
        } else {
            tween<Float>(durationMillis = animSpeed.durationMs(300), easing = FastOutSlowInEasing)
        },
        label = "landscapeCornerPillAlpha"
    )
    val secondaryAlphaState = animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding && !cornerPillVisible) 1f else 0f,
        // A tap swap changes the selected event under the bubbles (the bubble turns into the picker button or back,
        // the pill's spot fills or empties): they change at once, a fade or a pop-in there reads as gliding in and out
        animationSpec = if (cornerHandsBack || fromBubble || tapSwapRunning) {
            snap()
        } else if (bubbleFlies) {
            // Stays fully drawn until it has landed, then hands over in the same frame as the cutout appears
            tween<Float>(durationMillis = 1, delayMillis = flightMs)
        } else {
            tween<Float>(durationMillis = animSpeed.durationMs(260), easing = FastOutSlowInEasing)
        },
        label = "secondaryAlpha"
    )
    val secondaryScaleState = animateFloatAsState(
        // Landscape: the bubble hands off to the corner pill at its own size, as portrait's bubble does as it lands
        targetValue = if (isSplitMode && !isHiding && (!cornerPillVisible || bubbleFlies)) 1f else 0.3f,
        animationSpec = if (bubbleFlies || cornerHandsBack || fromBubble || tapSwapRunning) {
            // No pop: it is the cutout's size on landing and leaves at full size
            snap()
        } else {
            spring<Float>(dampingRatio = 0.8f, stiffness = animSpeed.stiffness(320f))
        },
        label = "secondaryScale"
    )
    // Flying into the cutout: glides as a round bubble and only turns into the pill as it lands
    val glidesAsCircle = flight.glides
    val shapeSpec: FiniteAnimationSpec<Dp> = if (tapSwapRunning || fromBubble) {
        // The bubble changes role in place (pill <-> picker button) with its offset: a width still springing would
        // stretch the picker button over the cutout's spot while the shell lands there
        snap()
    } else if (glidesAsCircle) {
        // Widens from the first frame on the travel's own curve, the bubble's outer edge goes straight to the pill's
        // edge
        tween(durationMillis = flightMs, easing = GLIDE_EASING)
    } else {
        spring(dampingRatio = 0.85f, stiffness = animSpeed.stiffness(360f))
    }
    val secondaryBubbleWidthState = animateDpAsState(
        targetValue = if (secondaryIsPill) miniPillWidth else circleSize,
        animationSpec = shapeSpec,
        label = "secondaryBubbleWidth"
    )
    val secondaryBubbleCornerState = animateDpAsState(
        targetValue = if (secondaryIsPill) settings.cornerRadius.dp else circleSize / 2f,
        animationSpec = shapeSpec,
        label = "secondaryBubbleCorner"
    )
    val tertiaryAlphaState = animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0f,
        animationSpec = if (tapSwapRunning) {
            snap<Float>()
        } else if (pickerRun) {
            // the flying copy is the pill until it lands; the real pill takes over then
            tween<Float>(durationMillis = PICKER_SWAP_MS, delayMillis = (flightMs - PICKER_SWAP_MS).coerceAtLeast(0),
                easing = LinearEasing)
        } else {
            tween<Float>(durationMillis = animSpeed.durationMs(260), easing = FastOutSlowInEasing)
        },
        label = "tertiaryAlpha"
    )
    val tertiaryScaleState = animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0.3f,
        animationSpec = if (pickerRun || tapSwapRunning) {
            snap<Float>()
        } else {
            spring<Float>(dampingRatio = 0.8f, stiffness = animSpeed.stiffness(320f))
        },
        label = "tertiaryScale"
    )
    return SecondaryAnimations(
        cornerPillAlpha = cornerPillAlphaState,
        secondaryAlpha = secondaryAlphaState,
        secondaryScale = secondaryScaleState,
        bubbleWidth = secondaryBubbleWidthState,
        bubbleCorner = secondaryBubbleCornerState,
        tertiaryAlpha = tertiaryAlphaState,
        tertiaryScale = tertiaryScaleState
    )
}

internal class SecondaryOffsets(
    val expandedCompactX: Dp,
    val collapsed: Dp,
    val expanded: Dp,
    val state: State<Dp>
)

@Composable
private fun rememberSecondaryOffsets(
    layout: IslandLayout,
    screenCenter: Dp,
    expanded: Boolean,
    isFullWidth: Boolean,
    isLandscape: Boolean,
    animSpeed: AnimationSpeed,
    secondaryIsPill: Boolean,
    flight: BubbleFlight,
    tapSwapRunning: Boolean,
    secondaryAlphaState: State<Float>
): SecondaryOffsets {
    val miniPillWidth = layout.miniPillWidth
    val circleSize = layout.circleSize
    val compactGap = layout.compactGap
    val isCircleLeft = layout.isCircleLeft
    val circleCenter = layout.circleCenter
    val groupCenter = layout.groupCenter
    val isHiding = layout.isHiding
    val fromBubble = flight.fromBubble
    val cornerHandsBack = flight.cornerHandsBack
    val pickerRun = flight.pickerRun
    val flightMs = flight.flightMs
    val expandedCompactX = layout.collapsedMainLeft
    val collapsedSecondaryOffset = if (isFullWidth) {
        circleCenter - screenCenter
    } else {
        circleCenter - groupCenter
    }
    val secondaryExpandedOffset = calculateSecondaryExpandedOffset(
        secondaryIsPill = secondaryIsPill,
        isCircleLeft = isCircleLeft,
        isFullWidth = isFullWidth,
        expandedCompactX = expandedCompactX.value,
        screenCenter = screenCenter.value,
        miniPillWidth = miniPillWidth.value,
        circleSize = circleSize.value,
        compactGap = compactGap.value
    ).dp
    // Same 2 speeds as the main island
    var prevExpandedForOffset by remember { mutableStateOf(expanded) }
    val offsetFollowsExpand = expanded != prevExpandedForOffset
    SideEffect { prevExpandedForOffset = expanded }
    // the secondary pill changing sides because a system chip came or went jumps
    var prevCircleLeftForOffset by remember { mutableStateOf(isCircleLeft) }
    val sideJumped = isCircleLeft != prevCircleLeftForOffset
    SideEffect { prevCircleLeftForOffset = isCircleLeft }
    val bubbleUnseen = !cornerHandsBack && (isHiding || Snapshot.withoutReadObservation {
        secondaryAlphaState.value <= 0.01f
    })
    val secondaryOffsetState = animateDpAsState(
        targetValue = if (!expanded) collapsedSecondaryOffset else secondaryExpandedOffset,
        animationSpec = if (sideJumped || bubbleUnseen || tapSwapRunning || fromBubble) {
            snap()
        } else if (pickerRun && offsetFollowsExpand) {
            snap()
        } else if (offsetFollowsExpand && flight.glides) {
            tween<Dp>(durationMillis = flightMs, easing = GLIDE_EASING)
        } else if (offsetFollowsExpand) {
            spring<Dp>(dampingRatio = islandSpringDamping(isLandscape),
                stiffness = animSpeed.stiffness(islandSpringStiffness(isLandscape)))
        } else {
            spring<Dp>(dampingRatio = 1f, stiffness = animSpeed.stiffness(if (isLandscape) 150f else 220f))
        },
        label = "secondaryOffset"
    )
    return SecondaryOffsets(expandedCompactX, collapsedSecondaryOffset, secondaryExpandedOffset, secondaryOffsetState)
}

internal class SecondaryMerge(
    val anim: Animatable<Float, AnimationVector1D>,
    val idleCloseMerge: Boolean
)

@Composable
private fun rememberSecondaryMerge(
    settings: SmartIslandSettings,
    animSpeed: AnimationSpeed,
    secondaryBubblePresent: Boolean,
    expanded: Boolean,
    isHiding: Boolean,
    userInteractionTimestamp: Long,
    forceMerge: Boolean,
    holdMerge: Boolean,
    forcedMergeMs: Int,
    pulseBusy: Boolean
): SecondaryMerge {
    val secondaryCanIdleMerge = PulseBubble.mayMerge(expanded, isHiding, secondaryBubblePresent, pulseBusy)
    var secondaryIdleMerged by remember { mutableStateOf(false) }
    val idleCloseMerge = forceMerge && secondaryCanIdleMerge
    LaunchedEffect(secondaryCanIdleMerge, userInteractionTimestamp, settings.secondaryMergeMs, idleCloseMerge) {
        secondaryIdleMerged = false
        if (idleCloseMerge) {
            secondaryIdleMerged = true
            return@LaunchedEffect
        }
        if (!secondaryCanIdleMerge) return@LaunchedEffect
        delay(settings.secondaryMergeMs.toLong())
        secondaryIdleMerged = true
    }
    val secondaryMergeAnim = remember { Animatable(0f) }
    LaunchedEffect(secondaryIdleMerged, expanded, settings.mergeStyle, secondaryBubblePresent, holdMerge,
        idleCloseMerge) {
        if (holdMerge && !secondaryBubblePresent && !expanded) {
            // The pill is leaving and the last card went
            val remainingMs = (settings.mergeStyleKind().baseDurationMs * (1f - secondaryMergeAnim.value)).toInt()
                .coerceAtLeast(60)
            val pillSpringMs = pillSpringSettleMs(
                animSpeed.stiffness(PILL_SPRING_STIFFNESS)
            ).toInt().coerceAtLeast(60)
            secondaryMergeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(minOf(animSpeed.durationMs(remainingMs), pillSpringMs), easing = MERGE_IN_EASING)
            )
        } else if (secondaryIdleMerged) {
            // The small bubble slides sideways to the big one
            val mergeMs = if (idleCloseMerge && forcedMergeMs > 0) {
                animSpeed.systemDurationMs(forcedMergeMs)
            } else {
                animSpeed.durationMs(settings.mergeStyleKind().baseDurationMs)
            }
            secondaryMergeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(mergeMs, easing = MERGE_IN_EASING)
            )
        } else if (expanded || !secondaryBubblePresent) {
            // No cutout left (expanded, or none to show)
            secondaryMergeAnim.snapTo(0f)
        } else {
            secondaryMergeAnim.animateTo(0f, tween(animSpeed.durationMs(SECONDARY_RETURN_MS),
                easing = MERGE_OUT_EASING))
        }
    }
    return SecondaryMerge(secondaryMergeAnim, idleCloseMerge)
}

/** The sibling picker takes the bubble's spot while the bubble glides to the cutout. */
internal class PickerFlight(
    val run: Boolean,
    val flight: Animatable<Float, AnimationVector1D>,
    val shape: Animatable<Float, AnimationVector1D>,
    val startOffset: Dp,
    val endOffset: Dp
)

@Composable
private fun rememberPickerFlight(
    expanded: Boolean,
    bubble: BubbleFlight,
    run: Boolean,
    startOffset: Dp,
    endOffset: Dp
): PickerFlight {
    val pickerFlight = remember { Animatable(0f) }
    val pickerShape = remember { Animatable(0f) }
    val flightMs = bubble.flightMs
    LaunchedEffect(expanded) {
        pickerFlight.snapTo(0f)
        pickerShape.snapTo(0f)
        if (expanded && bubble.pickerGlides) {
            // Same two runs as the portrait bubble: the glide, and the widening on the same curve.
            coroutineScope {
                launch { pickerFlight.animateTo(1f, tween(flightMs, easing = GLIDE_EASING)) }
                launch { pickerShape.animateTo(1f, tween(flightMs, easing = GLIDE_EASING)) }
            }
            bubble.pickerSettled.value = true
        } else {
            bubble.pickerSettled.value = expanded
        }
    }
    return PickerFlight(run, pickerFlight, pickerShape, startOffset, endOffset)
}

@Composable
internal fun rememberIslandSecondary(
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    safeIndex: Int,
    expanded: Boolean,
    layout: IslandLayout,
    activeNotification: IslandNotification?,
    isFullWidth: Boolean,
    isLandscape: Boolean,
    screenWidth: Dp,
    screenCenter: Dp,
    landscapeDot: IdleDotGeometry?,
    /** Landscape: the widget pill is on the island with no card. */
    widgetPill: Boolean = false,
    userInteractionTimestamp: Long,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    forceMerge: Boolean = false,
    holdMerge: Boolean = false,
    /** Length (ms, user speed) of the merge a forced one plays. */
    forcedMergeMs: Int = 0,
    /** A tap swap runs: cutout and picker change in place instead of sliding to a new spot. */
    tapSwapRunning: Boolean = false,
    onFlightChange: (Boolean) -> Unit = {},
    bubbleOpen: Boolean = false,
    /** Collapsed pulse that takes the cutout: the bubble holds what it displaced and waits to merge until it is over. */
    pulsingId: String? = null,
    pulseBusy: Boolean = false,
    swell: State<Float>? = null
): IslandSecondary {
    val miniPillWidth = layout.miniPillWidth
    val circleSize = layout.circleSize
    val animSpeed = settings.animationSpeed(isLandscape)
    val cards = secondaryCardsOf(notifications, safeIndex, expanded, layout, pulsingId)
    // the expanded window is always full width
    val cornerPillCenterRel = layout.mainCenter - screenCenter
    val flight = rememberBubbleFlight(settings, layout, cards, expanded, isLandscape, bubbleOpen, onFlightChange)
    val anims = rememberSecondaryAnimations(settings, layout, animSpeed, cards, flight, tapSwapRunning)
    val offsets = rememberSecondaryOffsets(
        layout, screenCenter, expanded, isFullWidth, isLandscape, animSpeed,
        cards.secondaryIsPill, flight, tapSwapRunning, anims.secondaryAlpha
    )
    val merge = rememberSecondaryMerge(
        settings = settings,
        animSpeed = animSpeed,
        secondaryBubblePresent = cards.secondaryNotification != null,
        expanded = expanded,
        isHiding = layout.isHiding,
        userInteractionTimestamp = userInteractionTimestamp,
        forceMerge = forceMerge,
        holdMerge = holdMerge,
        forcedMergeMs = forcedMergeMs,
        pulseBusy = pulseBusy
    )
    SideEffect {
        if (!expanded) flight.bubbleWasOut.value = anims.secondaryAlpha.value > 0.5f && merge.anim.value < 0.1f
    }
    // While a tap swap runs nothing glides: the picker bubble and the pill's spot appear in place (a flight that is
    // still running when a swap starts is cut short, the picker is then simply there)
    val picker = rememberPickerFlight(
        expanded = expanded,
        bubble = flight,
        run = flight.pickerRun && !tapSwapRunning,
        startOffset = offsets.collapsed,
        endOffset = offsets.expandedCompactX - screenCenter + miniPillWidth / 2f
    )

    val idleDotOn = !settings.enableNotchMode && shouldShowIdleDot(
        dot = landscapeDot,
        hasCards = pillOnIsland(notifications.isNotEmpty(), widgetPill),
        isLandscape = isLandscape
    )
    val shown = ShownFlags(
        cornerPill = remember(anims.cornerPillAlpha) { derivedStateOf { anims.cornerPillAlpha.value > 0.01f } },
        secondary = remember(anims.secondaryAlpha, merge.anim) {
            derivedStateOf { anims.secondaryAlpha.value > 0f && merge.anim.value < 0.999f }
        },
        tertiary = remember(anims.tertiaryAlpha) { derivedStateOf { anims.tertiaryAlpha.value > 0f } },
        mergeBackdrop = remember(anims.secondaryAlpha, merge.anim) {
            derivedStateOf {
                anims.secondaryAlpha.value > 0f && merge.anim.value > 0f && merge.anim.value < 0.999f
            }
        }
    )
    return IslandSecondary(
        cards = cards,
        anims = anims,
        offsets = offsets,
        merge = merge,
        picker = picker,
        glide = GlideSpec(from = offsets.collapsed, to = offsets.expanded, runs = flight.glides && !tapSwapRunning),
        idleDot = IdleDotState(shown = idleDotOn, alpha = if (idleDotOn) 1f else 0f),
        shown = shown,
        cornerPillCenterRel = cornerPillCenterRel,
        circleSize = circleSize,
        miniPillWidth = miniPillWidth,
        swell = swell
    )
}

@Composable
@Suppress("NAME_SHADOWING")
internal fun SecondaryBubbleContent(
    notification: IslandNotification,
    settings: SmartIslandSettings
) {
    val settings = settings.forCard(notification)
    when (notification.mode) {
        IslandMode.Bluetooth -> {
            ConnectionCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Flashlight -> {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(settings.flashlightColor).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color(settings.flashlightColor),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Battery -> {
            BatteryCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.LiveActivity -> {
            LiveActivityCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Navigation -> {
            NavigationCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.IncomingCall -> {
            val icon = notification.largeIcon ?: notification.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        IslandMode.Music -> {
            val artwork = MediaArt.glyphBitmap(notification)
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.musicVisualizerColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        IslandMode.ScreenRecording -> {
            ScreenRecordingCollapsedGlyph(settings = settings, paused = notification?.recordingPaused == true)
        }
        IslandMode.AudioRecording -> {
            AudioRecordingCollapsedGlyph(settings = settings, paused = notification?.recordingPaused == true)
        }
        IslandMode.Timer -> {
            TimerCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Stopwatch -> {
            StopwatchCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.CallEnded -> {
            CallEndedGlyph(notification = notification, size = 20.dp)
        }
        IslandMode.Notification, IslandMode.DownloadUpload, IslandMode.Empty -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}
