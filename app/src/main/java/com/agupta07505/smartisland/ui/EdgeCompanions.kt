/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.BubbleIntoCutout
import com.agupta07505.smartisland.model.EdgePillLayout
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillMotion
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.EdgeRect
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.MergeStyle
import com.agupta07505.smartisland.model.NotchCompanionPlan
import com.agupta07505.smartisland.model.NotchEarContent
import com.agupta07505.smartisland.model.PillOpenDrag
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal const val GLIDE_CLEARANCE = 0.5f

@Composable
internal fun EdgeCompanions(
    layout: EdgePillLayout,
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    plan: NotchCompanionPlan,
    cardOpen: Boolean,
    clearance: () -> Float,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    onPageSelected: (Int) -> Unit,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    /** Idle circle pressed: the bubble merges into the pill now. */
    forceMerge: Boolean = false,
    forcedMergeMs: Int = 0,
    hiding: Boolean = false,
    /** Bubble glide length in ms, lasts the island's whole expand (0 = base length). */
    glideMs: Int = 0,
    /** 1 at rest, bigger while the bubble swells with a pulse. */
    bubbleSwell: () -> Float = { 1f },
    modifier: Modifier = Modifier
) {
    val ear = layout.secondary ?: return
    val pill = layout.pill
    val camera = layout.camera
    val tuning = camera.tuning
    val opacity = tuning.opacity.coerceIn(0f, 1f)
    val surface = Color.Black.copy(alpha = opacity)
    val animSpeed = settings.animationSpeed(false)

    val pickerEar = plan.earContent == NotchEarContent.SiblingPicker
    val collapsedEar = !plan.pillStays && plan.earContent == NotchEarContent.OtherEvent
    val cleared = true
    val openDrag = ui.pillOpen
    val pillEventActive = PillOpenDrag.nextEventOnCutout(
        dragActive = openDrag.active,
        peeked = openDrag.peekNext,
        closing = openDrag.closing,
        progress = openDrag.progress,
        expandedPlan = plan.pillStays && cleared
    )
    // 1 persistent element for the other event: the bubble beside the pill, and the pill on the cutout's spot once
    // the card is open. It morphs between the 2, never swapped
    val morphActive = collapsedEar || pillEventActive
    val thirdActive = plan.pillStays && plan.earContent == NotchEarContent.OtherEvent

    // cards, kept while an item fades out so it does not go blank first
    val secondaryIndex = plan.earCardIndex
    val thirdIndex = plan.earCardIndex
    val lastMorph = remember { arrayOfNulls<IslandNotification>(1) }
    val lastThird = remember { arrayOfNulls<IslandNotification>(1) }
    val morphCard = when {
        pillEventActive -> notifications.getOrNull(plan.pillCardIndex)
        collapsedEar -> notifications.getOrNull(secondaryIndex)
        else -> null
    }
    val thirdCard = if (thirdActive) notifications.getOrNull(thirdIndex) else null
    if (morphCard != null) lastMorph[0] = morphCard
    if (thirdCard != null) lastThird[0] = thirdCard
    // Where the element last rested (ear or pill), so it leaves from there
    val lastAtPill = remember { BooleanArray(1) }
    if (morphActive) lastAtPill[0] = pillEventActive

    val fadeSpec = tween<Float>(durationMillis = animSpeed.durationMs(260), easing = FastOutSlowInEasing)
    val popSpec = spring<Float>(dampingRatio = 0.8f, stiffness = animSpeed.stiffness(320f))
    val secondaryPresence = animateFloatAsState(if (morphActive) 1f else 0f, fadeSpec, label = "edgeSecondaryPresence")
    val secondaryPop = animateFloatAsState(
        if (morphActive) 1f else if (lastAtPill[0]) 0.85f else 0.3f, popSpec, label = "edgeSecondaryPop"
    )
    val pickerPresence = animateFloatAsState(if (pickerEar) 1f else 0f, fadeSpec, label = "edgePickerPresence")
    val pickerPop = animateFloatAsState(if (pickerEar) 1f else 0.3f, popSpec, label = "edgePickerPop")
    val thirdPresence = animateFloatAsState(if (thirdActive) 1f else 0f, fadeSpec, label = "edgeThirdPresence")
    val thirdPop = animateFloatAsState(if (thirdActive) 1f else 0.3f, popSpec, label = "edgeThirdPop")

    // Idle merge (collapsed only)
    val merge = rememberEdgeMerge(
        ui, settings, animSpeed, collapsedEar, cardOpen, forceMerge, forcedMergeMs, hiding, secondaryPresence
    )
    val mergeAnim = merge.mergeAnim
    val mergeActive by merge.mergeActive
    val forcedMerge = merge.forcedMerge
    val bubbleOutBeforeOpen = merge.bubbleOutBeforeOpen

    // Position and shape of the element between bubble (0) and pill (1), as portrait's bubble has offset and width
    val flight = rememberEdgeBubbleFlight(
        ui, cardOpen, plan, pillEventActive, collapsedEar, animSpeed, glideMs, fadeSpec,
        lastMorph, bubbleOutBeforeOpen, secondaryPresence
    )
    val density = LocalDensity.current.density
    val glyphMaxSlidePx = EdgePillRules.glyphMaxSlideDp(pill.height) * density
    val followLimitPx = EdgePillLayoutMath.followLimitDp(layout)?.let { it * density }
    // The other event's pill moves like the main one, same look (flat or compact) so same origin and anchored side
    val flatLook = EdgePillRules.flatCameraSide(tuning.flatEdge, tuning.xOffsetDp)
    val pillEventMotion: () -> EdgePillMotion = {
        EdgePillRules.pillMotion(
            cameraOnLeft = camera.onLeft,
            acrossPx = 0f,
            alongPx = ui.pillDragOffsetX,
            arm = ui.pillArm.value,
            dead = ui.deadZone.value,
            pull = 0f,
            pulseScale = 1f,
            flat = flatLook,
            widthPx = pill.width * density
        )
    }
    val earBelow = ear.top > pill.top
    // The bubble ends half over the pill's near edge, then it is gone
    val mergeTop = if (earBelow) pill.bottom - ear.height * 0.55f else pill.top - ear.height * 0.45f

    // The pill swells as the bubble reaches it and sinks in (portrait's merge swell)
    TrackMergeSwell(ui, settings.mergeStyleKind(), mergeAnim, forcedMerge, ear, pill, mergeTop,
        camera.tuning.bubbleGapDp)

    // edge lean the ear bubbles take from the pill, read in the draw phase
    val earLeanPx: () -> Float = {
        edgeEarFollowPx(ui.pillDragOffsetX, ear.top, ear.height, camera.screenHeightDp, density)
    }

    val bubbleShape = remember(ear.width) { RoundedCornerShape((ear.width / 2f).dp) }

    // Where each ear sits (a tap swap dips the shell to it)
    val earBounds = remember { arrayOfNulls<Rect>(2) }
    val shared = EarShared(
        ui, wiring, surface, switchScaleAnim, earBounds, earLeanPx, notifications, settings, onPageSelected
    )
    val secondaryEar = EarPresence(secondaryPresence, secondaryPop)
    val pickerEarPresence = EarPresence(pickerPresence, pickerPop)
    val thirdEar = EarPresence(thirdPresence, thirdPop)

    // Sibling picker beside the open card grows in the bubble's old spot while the bubble glides to the pill
    if (pickerPresence.value > 0.01f || pickerEar) {
        EdgePickerEar(
            shared = shared,
            modifier = modifier,
            ear = ear,
            bubbleShape = bubbleShape,
            pickerEar = pickerEar,
            presence = pickerEarPresence,
            flight = flight
        )
    }

    val shownMorph = morphCard ?: lastMorph[0]
    if (shownMorph != null && (secondaryPresence.value > 0.01f || morphActive)) {
        EdgeMorphBubble(
            shared = shared,
            shownMorph = shownMorph,
            layout = layout,
            presence = secondaryEar,
            flight = flight,
            mergeAnim = mergeAnim,
            mergeActive = mergeActive,
            pillEventActive = pillEventActive,
            morphActive = morphActive,
            lastAtPill = lastAtPill,
            mergeTop = mergeTop,
            flatLook = flatLook,
            secondaryIndex = secondaryIndex,
            followLimitPx = followLimitPx,
            glyphMaxSlidePx = glyphMaxSlidePx,
            pillEventMotion = pillEventMotion,
            bubbleSwell = bubbleSwell
        )
    }

    val shownThird = thirdCard ?: lastThird[0]
    if ((thirdPresence.value > 0.01f || thirdActive) && shownThird != null) {
        EdgeThirdBubble(
            shared = shared,
            shownThird = shownThird,
            ear = ear,
            bubbleShape = bubbleShape,
            presence = thirdEar,
            thirdIndex = thirdIndex
        )
    }
}

/** What the 3 ear bubbles all read: the shared interaction state and the draw-phase lean. */
private class EarShared(
    val ui: IslandInteractionState,
    val wiring: IslandWiring,
    val surface: Color,
    val switchScaleAnim: Animatable<Float, AnimationVector1D>,
    /** [0] the secondary ear, [1] the third one. */
    val earBounds: Array<Rect?>,
    val earLeanPx: () -> Float,
    val notifications: List<IslandNotification>,
    val settings: SmartIslandSettings,
    val onPageSelected: (Int) -> Unit
)

/** How much of an ear bubble is there (fade) and how far it has popped in. */
private class EarPresence(val presence: State<Float>, val pop: State<Float>)

private class EdgeMerge(
    val mergeAnim: State<Float>,
    val mergeActive: State<Boolean>,
    val forcedMerge: Boolean,
    val bubbleOutBeforeOpen: BooleanArray
)

@Composable
private fun rememberEdgeMerge(
    ui: IslandInteractionState,
    settings: SmartIslandSettings,
    animSpeed: AnimationSpeed,
    collapsedEar: Boolean,
    cardOpen: Boolean,
    forceMerge: Boolean,
    forcedMergeMs: Int,
    hiding: Boolean,
    secondaryPresence: State<Float>
): EdgeMerge {
    var merged by remember { mutableStateOf(false) }
    val canMerge = collapsedEar && !cardOpen
    val forcedMerge = forceMerge && canMerge
    LaunchedEffect(canMerge, ui.userInteractionTimestamp, settings.secondaryMergeMs, forcedMerge, hiding) {
        if (hiding) return@LaunchedEffect
        merged = false
        if (!EdgePillRules.mergeTimerRuns(canMerge, hiding)) return@LaunchedEffect
        if (forcedMerge) {
            merged = true
            return@LaunchedEffect
        }
        delay(settings.secondaryMergeMs.toLong())
        merged = true
    }
    val mergeAnim = animateFloatAsState(
        targetValue = if (merged && canMerge) 1f else 0f,
        animationSpec = tween(
            durationMillis = when {
                merged && forcedMerge && forcedMergeMs > 0 -> animSpeed.systemDurationMs(forcedMergeMs)
                merged -> animSpeed.durationMs(settings.mergeStyleKind().baseDurationMs)
                else -> animSpeed.durationMs(260)
            },
            easing = FastOutSlowInEasing
        ),
        label = "edgeSecondaryMerge"
    )
    val mergeActive = remember { derivedStateOf { mergeAnim.value > 0.5f } }
    // Tells the view model whether a bubble is out beside the pill
    val bubbleApart by remember { derivedStateOf { collapsedEar && !hiding && secondaryPresence.value > 0.5f
        && mergeAnim.value < 0.1f } }
    val bubbleOutBeforeOpen = remember { BooleanArray(1) }
    // Remember on every composition while shut, not only when bubbleApart flips. A turn can skip a flag-keyed
    // effect or read a stale cardOpen, which snapped the bubble onto the cutout on the next open
    SideEffect {
        if (!cardOpen && !ui.pillOpen.active) bubbleOutBeforeOpen[0] = bubbleApart
    }
    LaunchedEffect(bubbleApart) {
        ui.edgeBubbleApart = bubbleApart
    }
    DisposableEffect(Unit) { onDispose { ui.edgeBubbleApart = false } }
    return EdgeMerge(mergeAnim, mergeActive, forcedMerge, bubbleOutBeforeOpen)
}

private class EdgeBubbleFlight(
    val moveA: Animatable<Float, AnimationVector1D>,
    val shapeA: Animatable<Float, AnimationVector1D>,
    val entry: Animatable<Float, AnimationVector1D>,
    val flyingState: MutableState<Boolean>
)

@Composable
private fun rememberEdgeBubbleFlight(
    ui: IslandInteractionState,
    cardOpen: Boolean,
    plan: NotchCompanionPlan,
    pillEventActive: Boolean,
    collapsedEar: Boolean,
    animSpeed: AnimationSpeed,
    glideMs: Int,
    fadeSpec: FiniteAnimationSpec<Float>,
    lastMorph: Array<IslandNotification?>,
    bubbleOutBeforeOpen: BooleanArray,
    secondaryPresence: State<Float>
): EdgeBubbleFlight {
    val moveA = remember { Animatable(0f) }
    val shapeA = remember { Animatable(0f) }
    // a fresh fade when the element comes to the pill without a glide
    val entry = remember { Animatable(1f) }
    val flyingState = remember { mutableStateOf(false) }
    var flying by flyingState
    val flightMs = if (glideMs > 0) glideMs else animSpeed.durationMs(BubbleIntoCutout.BASE_MS)
    val dragActive = ui.pillOpen.active
    // While the finger drags the island, the bubble follows the finger (no timed glide). It arrives at the cutout as
    // the other event reaches it and goes back out when the finger goes back up
    LaunchedEffect(dragActive) {
        if (!dragActive) return@LaunchedEffect
        if (!(bubbleOutBeforeOpen[0] || moveA.value > 0f)) return@LaunchedEffect
        flying = true
        try {
            snapshotFlow { ui.pillOpen.progress }.collect { p ->
                val g = (p / PillOpenDrag.NEXT_EVENT_PEEK_AT).coerceIn(0f, 1f)
                moveA.snapTo(g)
                shapeA.snapTo(g)
            }
        } finally {
            flying = false
        }
    }
    LaunchedEffect(cardOpen, plan.pillStays, pillEventActive, collapsedEar, dragActive) {
        if (dragActive) return@LaunchedEffect
        val bubbleWasOut = bubbleOutBeforeOpen[0]
        if (BubbleIntoCutout.flies(cardOpen, bubbleWasOut, plan.pillStays) &&
            pillEventActive && lastMorph[0] != null
        ) {
            // Opening: bubble glides and widens into the pill, both on the glide's curve
            bubbleOutBeforeOpen[0] = false
            val glideSpec = tween<Float>(flightMs, easing = GLIDE_EASING)
            if (moveA.value <= 0f) {
                moveA.snapTo(0f)
                shapeA.snapTo(0f)
            }
            entry.snapTo(1f)
            flying = true
            coroutineScope {
                launch { moveA.animateTo(1f, glideSpec) }
                launch { shapeA.animateTo(1f, glideSpec) }
            }
            flying = false
        } else if (!pillEventActive && collapsedEar && (moveA.value > 0f || shapeA.value > 0f)) {
            // Closing: back out to the bubble on portrait's springs (offset follows the island's expand spring, width
            // its own)
            flying = false
            coroutineScope {
                launch {
                    moveA.animateTo(
                        0f,
                        spring(dampingRatio = islandSpringDamping(false),
                            stiffness = animSpeed.stiffness(islandSpringStiffness(false)))
                    )
                }
                launch { shapeA.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = animSpeed.stiffness(360f))) }
            }
        } else if (pillEventActive) {
            // On the pill's spot with no glide (the bubble was not out)
            flying = false
            bubbleOutBeforeOpen[0] = false
            val cameFromEar = moveA.value < 0.5f && secondaryPresence.value > 0.5f
            moveA.snapTo(1f)
            shapeA.snapTo(1f)
            if (cameFromEar) {
                entry.snapTo(0f)
                entry.animateTo(1f, fadeSpec)
            }
        } else if (collapsedEar) {
            flying = false
            moveA.snapTo(0f)
            shapeA.snapTo(0f)
        } else {
            // leaving: it fades where it is
            flying = false
        }
    }
    LaunchedEffect(flying) { ui.setBubbleFlight(flying) }
    DisposableEffect(Unit) { onDispose { ui.setBubbleFlight(false) } }
    return EdgeBubbleFlight(moveA, shapeA, entry, flyingState)
}

@Composable
private fun TrackMergeSwell(
    ui: IslandInteractionState,
    mergeStyle: MergeStyle,
    mergeAnim: State<Float>,
    forcedMerge: Boolean,
    ear: EdgeRect,
    pill: EdgeRect,
    mergeTop: Float,
    bubbleGapDp: Float
) {
    val mergeContact = remember(ear.top, mergeTop, bubbleGapDp) {
        val travel = abs(mergeTop - ear.top)
        if (travel <= 0f) 0f else (bubbleGapDp / travel).coerceIn(0f, 1f)
    }
    LaunchedEffect(mergeStyle, mergeContact, pill.width, pill.height, forcedMerge) {
        snapshotFlow { mergeAnim.value }.collect { mg ->
            if (forcedMerge) {
                ui.edgeMergeScaleX = 1f
                ui.edgeMergeScaleY = 1f
            } else if (mergeStyle == MergeStyle.BUBBLE) {
                ui.edgeMergeScaleX = mergePillScaleY(mg, mergeContact, pill.width)
                ui.edgeMergeScaleY = mergePillScaleX(mg, mergeContact, pill.height)
            } else {
                val swell = 1f + 0.07f * mergeSwell(mg, mergeContact)
                ui.edgeMergeScaleX = swell
                ui.edgeMergeScaleY = swell
            }
        }
    }
    DisposableEffect(Unit) { onDispose { ui.edgeMergeScaleX = 1f; ui.edgeMergeScaleY = 1f } }
}

@NonRestartableComposable
@Composable
private fun EdgePickerEar(
    shared: EarShared,
    modifier: Modifier,
    ear: EdgeRect,
    bubbleShape: Shape,
    pickerEar: Boolean,
    presence: EarPresence,
    flight: EdgeBubbleFlight
) {
    val ui = shared.ui
    val wiring = shared.wiring
    val earBounds = shared.earBounds
    val earLeanPx = shared.earLeanPx
    val moveA = flight.moveA
    val pickerPresence = presence.presence
    val pickerPop = presence.pop
    val flying by flight.flyingState
    Box(
        modifier = modifier
            .absoluteOffset { IntOffset(ear.left.dp.roundToPx(), ear.top.dp.roundToPx()) }
            .size(ear.width.dp, ear.height.dp)
            .graphicsLayer {
                val pickerFlies = flying && pickerEar
                val arrival = if (pickerFlies) BubbleIntoCutout.pickerArrival(moveA.value) else 0f
                alpha = (if (flying) arrival else 1f) * pickerPresence.value *
                    ui.cutoutSwapAlpha(TapSwap.CUTOUT_EDGE_EAR)
                val pickerScale = if (pickerFlies) BubbleIntoCutout.pickerScale(moveA.value) else 1f
                val grow = pickerPop.value * pickerScale
                scaleX = grow * shared.switchScaleAnim.value
                scaleY = grow * shared.switchScaleAnim.value
                // Rides along the edge with the pill's lean, held to its own room on the screen
                translationY = earLeanPx()
            }
            .onGloballyPositioned { coords -> if (!ui.tapSwapActive) earBounds[0] = coords.boundsInWindow() }
            .clip(bubbleShape)
            .background(shared.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                ui.userTouched()
                if (ui.tapSwapActive) {
                    ui.onPickerTapDuringSwap?.invoke(TapSwap.CUTOUT_EDGE_EAR)
                    return@clickable
                }
                if (wiring.view.siblingPickerOpen) wiring.actions.sibling.closePicker()
                else if (!ui.cardTouchLocked(SystemClock.uptimeMillis())) wiring.actions.sibling.openPicker()
            },
        contentAlignment = Alignment.Center
    ) {
        SiblingPickerLines(open = wiring.view.siblingPickerOpen)
    }
}

@NonRestartableComposable
@Composable
private fun EdgeMorphBubble(
    shared: EarShared,
    shownMorph: IslandNotification,
    layout: EdgePillLayout,
    presence: EarPresence,
    flight: EdgeBubbleFlight,
    mergeAnim: State<Float>,
    mergeActive: Boolean,
    pillEventActive: Boolean,
    morphActive: Boolean,
    lastAtPill: BooleanArray,
    mergeTop: Float,
    flatLook: Boolean,
    secondaryIndex: Int,
    followLimitPx: Float?,
    glyphMaxSlidePx: Float,
    pillEventMotion: () -> EdgePillMotion,
    bubbleSwell: () -> Float
) {
    val ui = shared.ui
    val wiring = shared.wiring
    val settings = shared.settings
    val notifications = shared.notifications
    val surface = shared.surface
    val switchScaleAnim = shared.switchScaleAnim
    val earBounds = shared.earBounds
    val earLeanPx = shared.earLeanPx
    val onPageSelected = shared.onPageSelected
    val moveA = flight.moveA
    val shapeA = flight.shapeA
    val entry = flight.entry
    val secondaryPresence = presence.presence
    val secondaryPop = presence.pop
    val ear = layout.secondary ?: return
    val pill = layout.pill
    val camera = layout.camera
    val tuning = camera.tuning
    val flying by flight.flyingState
    val earCentreX = ear.left + ear.width / 2f
    val earCentreY = ear.top + ear.height / 2f
    val pillCentreX = pill.left + pill.width / 2f
    val pillCentreY = pill.top + pill.height / 2f
    val shapeNow: () -> Float = { shapeA.value.coerceIn(0f, 1f) }
    val bubbleW: () -> Dp = { lerp(ear.width, pill.width, shapeNow()).dp }
    val bubbleH: () -> Dp = { lerp(ear.height, pill.height, shapeNow()).dp }
    val glyphsOn = pillEventActive || shapeA.isRunning || (lastAtPill[0] && !morphActive)
    Box(
        modifier = Modifier
            .absoluteOffset {
                val p = moveA.value
                val s = shapeNow()
                // The idle merge only moves it while it is a bubble
                val mg = mergeAnim.value * (1f - p).coerceIn(0f, 1f)
                val cx = lerp(earCentreX, pillCentreX, p)
                val earY = lerp(earCentreY, mergeTop + ear.height / 2f, mg)
                val cy = lerp(earY, pillCentreY, p)
                IntOffset(
                    (cx - lerp(ear.width, pill.width, s) / 2f).dp.roundToPx(),
                    (cy - lerp(ear.height, pill.height, s) / 2f).dp.roundToPx()
                )
            }
            .islandAnimatedSize(bubbleW, bubbleH)
            .graphicsLayer {
                val p = moveA.value
                val mg = mergeAnim.value * (1f - p).coerceIn(0f, 1f)
                alpha = secondaryPresence.value * entry.value * (1f - ((mg - 0.4f) / 0.6f).coerceIn(0f, 1f)) *
                    ui.cutoutSwapAlpha(
                        if (pillEventActive) TapSwap.CUTOUT_EDGE_PILL
                        else TapSwap.CUTOUT_EDGE_EAR
                    )
                // Collapsed pill's swipe motion once it is the pill, the bubble's own pop and shrink before
                val motion = pillEventMotion()
                val pop = secondaryPop.value
                val bubbleScale = pop * (1f - 0.25f * mg) * switchScaleAnim.value * bubbleSwell()
                var sx = lerp(bubbleScale, pop * motion.scaleX, p)
                var sy = lerp(bubbleScale, pop * motion.scaleY, p)
                if (flying) {
                    // Light stretch along the travel, position is on the glide's curve so speed follows what is left
                    val stretch = BubbleIntoCutout.stretch(1f - p)
                    val across = BubbleIntoCutout.squash(stretch)
                    if (abs(pillCentreY - earCentreY) >= abs(pillCentreX - earCentreX)) {
                        sy *= stretch; sx *= across
                    } else {
                        sx *= stretch; sy *= across
                    }
                }
                scaleX = sx
                scaleY = sy
                transformOrigin = TransformOrigin(lerp(0.5f, motion.originX, p), 0.5f)
                // Rides the edge with the pill's lean as a bubble, the pill's own motion as the pill
                translationY = lerp(earLeanPx(), motion.translationY, p)
                // Round as a bubble, the pill's own corners as the pill (a flat camera side stays square)
                val s = shapeNow()
                val round = lerp(ear.width / 2f, tuning.cornerRadiusDp, s).coerceAtLeast(0f).dp.toPx()
                val flat = lerp(ear.width / 2f, 0f, s).coerceAtLeast(0f).dp.toPx()
                shape = when {
                    !flatLook -> RoundedCornerShape(round)
                    camera.onLeft -> AbsoluteRoundedCornerShape(flat, round, round, flat)
                    else -> AbsoluteRoundedCornerShape(round, flat, flat, round)
                }
                clip = true
            }
            .onGloballyPositioned { coords -> if (!ui.tapSwapActive && !pillEventActive) earBounds[0] =
                coords.boundsInWindow() }
            .background(surface)
            .then(
                if (mergeActive || pillEventActive) Modifier
                else Modifier.clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null) {
                    ui.userTouched()
                    if (turnAroundOnCutoutTap(ui, wiring, secondaryIndex, onPageSelected)) return@clickable
                    if (secondaryIndex >= 0) {
                        ui.markCutout(TapSwap.CUTOUT_EDGE_EAR) { earBounds[0] }
                        onPageSelected(secondaryIndex)
                    }
                    if (!wiring.view.expanded) wiring.actions.expand.toggle()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = shownMorph.key,
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(200),
            label = "edgeSecondaryContent"
        ) { key ->
            val card = notifications.firstOrNull { it.key == key } ?: shownMorph
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // icon turns into the pill's glyphs as the shape widens, as portrait's bubble content does
                Box(Modifier.graphicsLayer { alpha = BubbleIntoCutout.iconAlpha(shapeNow()) }) {
                    SecondaryBubbleContent(notification = card, settings = settings)
                }
                if (glyphsOn) {
                    Box(
                        Modifier
                            .requiredSize(pill.width.dp, pill.height.dp)
                            .graphicsLayer {
                                // The glyphs keep their size while the pill swells around them
                                val motion = pillEventMotion()
                                scaleX = EdgePillRules.glyphScale(motion.scaleX, motion.cue)
                                scaleY = EdgePillRules.glyphScale(motion.scaleY, motion.cue)
                                translationY = EdgePillRules.followPx(ui.pillContentDragX,
                                    followLimitPx) + ui.pillEnter.value
                                val enterShare = EdgePillRules.enterShare(ui.pillEnter.value, glyphMaxSlidePx,
                                    PILL_ENTER_DISTANCE_FRACTION)
                                alpha = BubbleIntoCutout.glyphAlpha(shapeNow()) * ((secondaryPresence.value - 0.3f) / 0.7f).coerceIn(0f, 1f) *
                                    (1f - PILL_ENTER_FADE * enterShare)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        EdgePillSlots(card, layout, settings)
                    }
                }
            }
        }
    }
}

@NonRestartableComposable
@Composable
private fun EdgeThirdBubble(
    shared: EarShared,
    shownThird: IslandNotification,
    ear: EdgeRect,
    bubbleShape: Shape,
    presence: EarPresence,
    thirdIndex: Int
) {
    val ui = shared.ui
    val wiring = shared.wiring
    val settings = shared.settings
    val notifications = shared.notifications
    val surface = shared.surface
    val switchScaleAnim = shared.switchScaleAnim
    val earBounds = shared.earBounds
    val earLeanPx = shared.earLeanPx
    val onPageSelected = shared.onPageSelected
    val thirdPresence = presence.presence
    val thirdPop = presence.pop
    Box(
        modifier = Modifier
            .offset(x = ear.left.dp, y = ear.top.dp)
            .size(ear.width.dp, ear.height.dp)
            .graphicsLayer {
                alpha = thirdPresence.value * ui.cutoutSwapAlpha(TapSwap.CUTOUT_EDGE_THIRD)
                val grow = thirdPop.value * switchScaleAnim.value
                scaleX = grow
                scaleY = grow
                translationY = earLeanPx()
            }
            .onGloballyPositioned { coords -> if (!ui.tapSwapActive) earBounds[1] = coords.boundsInWindow() }
            .clip(bubbleShape)
            .background(surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                ui.userTouched()
                if (turnAroundOnCutoutTap(ui, wiring, thirdIndex, onPageSelected)) return@clickable
                if (thirdIndex >= 0) {
                    ui.markCutout(TapSwap.CUTOUT_EDGE_THIRD) { earBounds[1] }
                    onPageSelected(thirdIndex)
                }
                if (!wiring.view.expanded) wiring.actions.expand.toggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = shownThird.key, animationSpec = tween(200), label = "edgeThirdContent") { key ->
            val card = notifications.firstOrNull { it.key == key } ?: shownThird
            SecondaryBubbleContent(notification = card, settings = settings)
        }
    }
}

@Composable
internal fun EdgePillSlots(card: IslandNotification, layout: EdgePillLayout, settings: SmartIslandSettings) {
    val slots = EdgePillLayoutMath.glyphSlots(layout)
    val slotW = slots.width
    val slotH = slots.height
    Box(modifier = Modifier.requiredSize(layout.pill.width.dp, layout.pill.height.dp)) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = slots.left.dp, y = slots.glyphTop.dp)
                .size(slotW.dp, slotH.dp),
            contentAlignment = Alignment.Center
        ) { CollapsedGlyphContent(card.mode, card, settings) }
        val valueAlong = EdgePillLayoutMath.valueAlong(layout)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(y = valueAlong.topDp.dp)
                .size(layout.pill.width.dp, valueAlong.lengthDp.dp),
            contentAlignment = if (valueAlong.fromBottom) Alignment.BottomCenter else Alignment.TopCenter
        ) {
            Box(Modifier.valueAlongStrip(card, settings), contentAlignment = Alignment.Center) {
                CollapsedValueContent(card.mode, card, settings)
            }
        }
    }
}
