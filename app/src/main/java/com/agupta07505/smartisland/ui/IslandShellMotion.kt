/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CollapseMotion
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgeCardPlacement
import com.agupta07505.smartisland.model.ExpandMotion
import com.agupta07505.smartisland.model.ExpandOrigin
import com.agupta07505.smartisland.model.ExpandTravel
import com.agupta07505.smartisland.model.PillOpenDrag
import com.agupta07505.smartisland.model.SwipeFeel
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleCloseSequence

internal class CloseMotion(
    val idlePressClose: Boolean,
    val idleSequence: IdleCloseSequence.Plan,
    val edgeMirror: Boolean,
    val closeStiffness: Float,
    val closeWidthDamping: Float,
    val collapseWidthSpec: FiniteAnimationSpec<Dp>,
    val collapseSettleSpec: FiniteAnimationSpec<Dp>,
    val collapseWidthSpecFloat: FiniteAnimationSpec<Float>,
    val collapseSettleSpecFloat: FiniteAnimationSpec<Float>
)

@Composable
internal fun rememberCloseMotion(
    animSpeed: AnimationSpeed,
    expanded: Boolean,
    idleClosing: Boolean,
    edgeCamera: EdgeCamera?,
    travelSlowdown: Float
): CloseMotion {
    // a close started by the idle circle, in both orientations
    val idlePressLatch = remember { BooleanArray(1) }
    if (expanded) idlePressLatch[0] = false else if (idleClosing) idlePressLatch[0] = true
    val idlePressClose = idlePressLatch[0]
    // the close is step 1 of the two-step sequence
    val idleSequence = remember(animSpeed) { IdleCloseSequence.plan(animSpeed) }
    val edgePlan = remember(animSpeed) { idleClosePlan(animSpeed, true) }
    val closeBaseStiffness = if (edgeCamera != null) {
        animSpeed.systemStiffness(edgePlan.closeStiffness)
    } else if (idlePressClose) {
        animSpeed.systemStiffness(idleSequence.closeStiffness)
    } else {
        animSpeed.stiffness(CollapseMotion.STIFFNESS)
    }
    val closeStiffness = if (idlePressClose) closeBaseStiffness
    else ExpandTravel.stiffness(closeBaseStiffness, travelSlowdown)
    // Edge pill: every collapse, the idle-circle tap included
    val edgeMirror = edgeCamera != null
    val closeWidthDamping = CollapseMotion.closeWidthDamping(idlePressClose, noBounce = edgeCamera != null)
    val collapseWidthSpec = spring<Dp>(dampingRatio = closeWidthDamping, stiffness = closeStiffness)
    val collapseSettleSpec = spring<Dp>(
        dampingRatio = CollapseMotion.SETTLE_DAMPING,
        stiffness = closeStiffness
    )
    val collapseWidthSpecFloat = spring<Float>(dampingRatio = closeWidthDamping, stiffness = closeStiffness)
    val collapseSettleSpecFloat = spring<Float>(
        dampingRatio = CollapseMotion.SETTLE_DAMPING,
        stiffness = closeStiffness
    )
    return CloseMotion(
        idlePressClose, idleSequence, edgeMirror, closeStiffness, closeWidthDamping,
        collapseWidthSpec, collapseSettleSpec, collapseWidthSpecFloat, collapseSettleSpecFloat
    )
}

internal class ShellSizes(
    val width: State<Dp>,
    val height: State<Dp>,
    val expandedTopShown: State<Dp>,
    val topShift: State<Dp>,
    val radius: State<Dp>
)

@Composable
private fun animateShellSize(
    transition: Transition<Boolean>,
    settings: SmartIslandSettings,
    expandedWidth: Dp,
    expandedHeight: Dp,
    expandedTopOffset: Dp,
    edgePlacement: EdgeCardPlacement?,
    collapsedTopShift: Dp,
    springs: ExpandSprings,
    closeMotion: CloseMotion,
    stretchSpec: FiniteAnimationSpec<Dp>,
    collapseOnPill: Boolean,
    cardSwitching: () -> Boolean,
    animSpeed: AnimationSpeed
): ShellSizes {
    val sizeSpec = springs.sizeSpec
    val heightSpec = springs.heightSpec
    val collapseWidthSpec = closeMotion.collapseWidthSpec
    val collapseSettleSpec = closeMotion.collapseSettleSpec
    val settleOrSnap: FiniteAnimationSpec<Dp> =
        snap()
    val widthState = transition.animateDp(transitionSpec =
        { if (initialState == targetState) stretchSpec else if (!targetState
        && collapseOnPill) collapseWidthSpec else sizeSpec }, label = "islandWidth") {
        if (it) expandedWidth else settings.width.dp
    }
    // the height of an island that STAYS expanded
    val heightFollowSpec = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = animSpeed.stiffness(HEIGHT_FOLLOW_STIFFNESS)
    )
    val cardSwitchHeightSpec = spring<Dp>(
        dampingRatio = SwipeFeel.RETURN_DAMPING_RATIO,
        stiffness = SwipeFeel.EASE_BACK_STIFFNESS
    )
    val heightState = transition.animateDp(transitionSpec = {
        val switching = Snapshot.withoutReadObservation { cardSwitching() }
        val expandedAtRest = Snapshot.withoutReadObservation {
            // currentState only turns true when the whole transition has finished
            transition.currentState && transition.targetState
        }
        if (expandedAtRest) (if (switching) cardSwitchHeightSpec else heightFollowSpec)
        else if (initialState == targetState) (if (edgePlacement != null) stretchSpec else settleOrSnap)
        else if (!targetState && collapseOnPill) collapseSettleSpec
        else heightSpec
    }, label = "islandHeight") {
        if (it) expandedHeight else settings.height.dp
    }
    val topFollowAtRest = Snapshot.withoutReadObservation {
        transition.currentState && transition.targetState
    }
    val topFollowSwitching = Snapshot.withoutReadObservation { cardSwitching() }
    val expandedTopShownState = animateDpAsState(
        targetValue = expandedTopOffset,
        animationSpec = if (edgePlacement != null && topFollowAtRest) {
            if (topFollowSwitching) cardSwitchHeightSpec else heightFollowSpec
        } else snap(),
        label = "islandEdgeTop"
    )
    val topShiftState = transition.animateDp(transitionSpec =
        { if (initialState == targetState) settleOrSnap else sizeSpec }, label = "islandTopShift") {
        if (it) 0.dp else collapsedTopShift
    }
    val radiusState =
        transition.animateDp(transitionSpec = { if (initialState == targetState) settleOrSnap else if (!targetState
        && collapseOnPill) collapseSettleSpec else sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else settings.cornerRadius.dp
    }
    return ShellSizes(widthState, heightState, expandedTopShownState, topShiftState, radiusState)
}

internal class ShellFades(val collapsed: State<Float>, val expanded: State<Float>)

@Composable
private fun animateShellFades(
    transition: Transition<Boolean>,
    settings: SmartIslandSettings,
    expandedWidth: Dp,
    widthState: State<Dp>,
    springs: ExpandSprings,
    closeMotion: CloseMotion,
    collapseOnPill: Boolean,
    isHiding: Boolean,
    animSpeed: AnimationSpeed
): ShellFades {
    val alphaMs = springs.alphaMs
    val expandStiffness = springs.expandStiffness
    val expandDamping = springs.expandDamping
    val idlePressClose = closeMotion.idlePressClose
    val idleSequence = closeMotion.idleSequence
    val edgeMirror = closeMotion.edgeMirror
    val closeStiffness = closeMotion.closeStiffness
    val closeWidthDamping = closeMotion.closeWidthDamping
    val collapsedAlphaState = transition.animateFloat(
        transitionSpec = {
            when {
                initialState == targetState -> tween(alphaMs, easing = FastOutSlowInEasing)
                // The pill's content is gone when the shell is a quarter grown
                targetState -> tween(
                    ExpandMotion.pillOutMs(expandStiffness, expandDamping),
                    easing = FastOutSlowInEasing
                )
                edgeMirror && collapseOnPill -> tween(
                    durationMillis = ExpandMotion.pillInMs(expandStiffness, expandDamping),
                    delayMillis = ExpandMotion.pillInDelayFromMs(
                        fromProgress = Snapshot.withoutReadObservation {
                            val pillW = settings.width.dp.value
                            val travel = expandedWidth.value - pillW
                            if (travel <= 0f) 1f else ((widthState.value.value - pillW) / travel)
                        },
                        stiffness = expandStiffness,
                        damping = expandDamping
                    ),
                    easing = FastOutSlowInEasing
                )
                // the pill's own content waits until the width is almost home
                collapseOnPill && !idlePressClose -> tween(
                    durationMillis = CollapseMotion.pillContentInMs(alphaMs),
                    // From how far the island is open right now, not always from full size
                    delayMillis = CollapseMotion.pillContentDelayFromMs(
                        expandProgress = Snapshot.withoutReadObservation {
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
            if (targetState) tween(
                durationMillis = ExpandMotion.cardInMs(expandStiffness, expandDamping),
                delayMillis = ExpandMotion.cardInDelayMs(expandStiffness, expandDamping),
                easing = LinearOutSlowInEasing
            )
            else if (idlePressClose && !edgeMirror) tween(animSpeed.systemDurationMs(idleSequence.contentOutMs),
                easing = FastOutSlowInEasing)
            else if (edgeMirror) {
                // Edge pill: the expand's card fade-in played in reverse
                val from = Snapshot.withoutReadObservation {
                    val pillW = settings.width.dp.value
                    val travel = expandedWidth.value - pillW
                    if (travel <= 0f) 1f else ((widthState.value.value - pillW) / travel)
                }
                tween(
                    durationMillis = ExpandMotion.cardOutMsFrom(from, expandStiffness, expandDamping),
                    delayMillis = ExpandMotion.cardOutDelayFromMs(from, expandStiffness, expandDamping),
                    easing = FastOutLinearInEasing
                )
            }
            else tween(CollapseMotion.cardContentOutMs(alphaMs), easing = FastOutSlowInEasing)
        },
        label = "expandedAlpha"
    ) {
        if (it) 1f else 0f
    }
    return ShellFades(collapsedAlphaState, expandedAlphaState)
}

internal class ShellAnimations(
    val sizes: ShellSizes,
    val fades: ShellFades,
    val expandProgress: State<Float>,
    val expandProgressY: State<Float>,
    val contentScale: State<Float>,
    val contentSlideY: State<Dp>
)

@Composable
internal fun animateShell(
    transition: Transition<Boolean>,
    settings: SmartIslandSettings,
    expandedWidth: Dp,
    expandedHeight: Dp,
    expandedTopOffset: Dp,
    edgePlacement: EdgeCardPlacement?,
    collapsedTopShift: Dp,
    springs: ExpandSprings,
    closeMotion: CloseMotion,
    stretchSpec: FiniteAnimationSpec<Dp>,
    collapseOnPill: Boolean,
    isHiding: Boolean,
    animSpeed: AnimationSpeed,
    cardSwitching: () -> Boolean
): ShellAnimations {
    val sizes = animateShellSize(
        transition, settings, expandedWidth, expandedHeight, expandedTopOffset, edgePlacement,
        collapsedTopShift, springs, closeMotion, stretchSpec, collapseOnPill, cardSwitching, animSpeed
    )
    val fades = animateShellFades(
        transition, settings, expandedWidth, sizes.width, springs, closeMotion,
        collapseOnPill, isHiding, animSpeed
    )
    val sizeSpec = springs.sizeSpec
    val sizeSpecFloat = springs.sizeSpecFloat
    val collapseWidthSpecFloat = closeMotion.collapseWidthSpecFloat
    val collapseSettleSpecFloat = closeMotion.collapseSettleSpecFloat
    // The expand spring as 1 number (0 = pill, 1 = card)
    val expandProgressState = transition.animateFloat(
        transitionSpec = { if (initialState == targetState) snap() else if (!targetState
            && collapseOnPill) collapseWidthSpecFloat else sizeSpecFloat },
        label = "islandExpandProgress"
    ) {
        if (it) 1f else 0f
    }
    // The same number for the vertical axis
    val expandProgressYState = transition.animateFloat(
        transitionSpec = { if (initialState == targetState) snap() else if (!targetState
            && collapseOnPill) collapseSettleSpecFloat else sizeSpecFloat },
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
    return ShellAnimations(sizes, fades, expandProgressState, expandProgressYState, contentScaleState, contentSlideYState)
}

internal fun shellOriginX(
    settings: SmartIslandSettings,
    geometry: PillGeometry,
    anim: ShellAnimations,
    expandedWidth: Dp,
    pulseWidthState: State<Dp>,
    pulseCentreShift: State<Float>,
    edgePlacement: EdgeCardPlacement?,
    dragProgressNow: () -> Float,
    blendsNow: () -> Boolean,
    bubbleCentreX: Dp,
    bubbleSize: Float,
    anchorSide: CameraSide = geometry.cameraSide
): () -> Dp {
    val cameraSide = anchorSide
    val collapsedMainOffset = geometry.collapsedMainOffset
    val expandedCentreOffsetDp = geometry.expandedCentreOffsetDp
    val widthState = anim.sizes.width
    val expandProgressState = anim.expandProgress
    return {
        val dragP = dragProgressNow()
        val sideAnchored = cameraSide != CameraSide.Centre
        val size = if (dragP >= 0f) {
            PillOpenDrag.lerp(settings.width, expandedWidth.value, dragP)
        } else if (sideAnchored) {
            pulseWidthState.value.coerceAtLeast(0.dp).value
        } else {
            widthState.value.coerceAtLeast(0.dp).value
        }
        val progressX = if (dragP >= 0f) dragP else expandProgressState.value
        val growStartX = if (sideAnchored && edgePlacement == null) {
            val edgeFraction = if (cameraSide == CameraSide.Left) 0f else 1f
            ExpandOrigin.startAnchored(
                progress = progressX,
                size = size,
                pillAnchor = collapsedMainOffset.value + (edgeFraction - 0.5f) * settings.width,
                pillAnchorFraction = edgeFraction,
                pillCentre = collapsedMainOffset.value,
                cardStart = expandedCentreOffsetDp - expandedWidth.value / 2f,
                cardSize = expandedWidth.value
            )
        } else if (edgePlacement != null) {
            ExpandOrigin.edgeStart(progressX, size, edgePlacement.pillEdgeOffsetDp, edgePlacement.cardEdgeOffsetDp,
                edgePlacement.onLeft)
        } else {
            ExpandOrigin.start(
                progress = progressX,
                size = size,
                pillCentre = collapsedMainOffset.value,
                cardStart = expandedCentreOffsetDp - expandedWidth.value / 2f,
                cardSize = expandedWidth.value
            )
        }
        if (blendsNow()) {
            val blendedWidth = BubbleOrigin.edge(
                bubbleSize, expandedWidth.value, size, progressX
            )
            val blendedLeft = BubbleOrigin.edge(
                bubbleCentreX.value - bubbleSize / 2f,
                expandedCentreOffsetDp - expandedWidth.value / 2f,
                growStartX,
                progressX
            )
            (blendedLeft + blendedWidth / 2f).dp
        } else {
            // a split pulse moves the pill's centre off the settled one; the shift is gone as the card opens
            val centreShift = if (dragP >= 0f || sideAnchored) 0f else
                pulseCentreShift.value * (1f - progressX.coerceIn(0f, 1f))
            (growStartX + size / 2f + centreShift).dp
        }
    }
}

internal fun shellOriginY(
    anim: ShellAnimations,
    expandedHeight: Dp,
    expandedTopOffset: Dp,
    collapsedTopShift: Dp,
    pillHeightNow: Float,
    edgePlacement: EdgeCardPlacement?,
    lineExtraState: State<Float>,
    dragProgressNow: () -> Float,
    blendsNow: () -> Boolean
): () -> Dp {
    val heightState = anim.sizes.height
    val topShiftState = anim.sizes.topShift
    val expandProgressYState = anim.expandProgressY
    val expandedTopShownState = anim.sizes.expandedTopShown
    return {
        val dragP = dragProgressNow()
        // The pill grows downward: its top stays where it was
        val extra = if (dragP >= 0f) 0f else lineExtraState.value
        val size = if (dragP >= 0f) {
            PillOpenDrag.lerp(pillHeightNow, expandedHeight.value, dragP)
        } else {
            heightState.value.coerceAtLeast(0.dp).value + extra
        }
        val progressY = if (dragP >= 0f) dragP else expandProgressYState.value.coerceAtMost(1f)
        // Edge layout, card open: the top stays on the card's own top
        val growTopRaw = if (edgePlacement != null && ExpandOrigin.settled(progressY)) {
            expandedTopShownState.value.value
        } else {
            ExpandOrigin.start(
                progress = progressY,
                size = size,
                pillCentre = edgePlacement?.pillCentreYDp ?: (collapsedTopShift.value + pillHeightNow / 2f + extra / 2f),
                cardStart = expandedTopOffset.value,
                cardSize = expandedHeight.value
            )
        }
        val growTop = growTopRaw.coerceAtLeast(minOf(0f, collapsedTopShift.value, expandedTopOffset.value))
        val topShiftNow = if (dragP >= 0f) {
            PillOpenDrag.lerp(collapsedTopShift.value, 0f, dragP).coerceAtLeast(0f)
        } else {
            topShiftState.value.coerceAtLeast(0.dp).value
        }
        val shownTop = if (blendsNow()) {
            BubbleOrigin.edge(
                collapsedTopShift.value, expandedTopOffset.value, growTop, progressY
            )
        } else growTop
        // The outer Box already pads by topShift
        (shownTop - topShiftNow).dp
    }
}
