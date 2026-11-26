/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.EdgeRect
import com.agupta07505.smartisland.model.ExpandMotion
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotchCompanionPlan
import com.agupta07505.smartisland.model.PILL_SPRING_DAMPING
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.notchCompanionPlan
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@NonRestartableComposable
@Composable
internal fun BoxScope.EdgePillOverlay(
    edge: EdgePillState,
    edgeSurface: EdgeSurface,
    edgeCamera: EdgeCamera,
    settings: SmartIslandSettings,
    expanded: Boolean,
    layout: IslandLayout,
    screenWidth: Dp,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    ui: IslandInteractionState,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    selectFromPill: (Int) -> Unit,
    animSpeedNow: AnimationSpeed,
    idleClosing: Boolean,
    onOverCardTap: (cutoutId: Int, bounds: () -> Rect?) -> Unit = { _, _ -> }
) {
    val edgeCardsAway = edge.cardsAway
    val edgeLayerNotifications = edge.layerNotifications
    val edgeLayerIndex = edge.layerIndex
    val edgeLayoutOn = edge.layoutOn
    val edgePresence = edge.presence
    val edgeFade = edge.fade
    val edgeHideRunning = edge.hideRunning
    val edgeActive = edgeSurface.active
    val edgePillOwnsSurface = edgeSurface.ownsSurface
    val edgeShellSurface = edgeSurface.shellSurface
    // The companions follow the compact mode's rules (the notch mode's plan)
    val edgePlan = if (edgeCardsAway) NotchCompanionPlan.None
    else notchCompanionPlan(edgeLayerNotifications, edgeLayerIndex, expanded)
    val edgeNeedsBubble = edgePlan.earShown || edgePlan.pillStays
    val edgeLayout = remember(edgeCamera, edgeNeedsBubble) {
        EdgePillLayoutMath.compute(
            camera = edgeCamera,
            hasSecondary = edgeNeedsBubble
        )
    }
    val edgeLayoutNow by rememberUpdatedState(layout)
    val edgeClearance: () -> Float = {
        val l = edgeLayoutNow
        val half = screenWidth.value / 2f
        val centre = l.animatedXOffset.value + half
        val w = l.safeWidth.value
        EdgePillRules.pillClearance(
            cameraOnLeft = edgeCamera.onLeft,
            shellLeftDp = centre - w / 2f,
            shellRightDp = centre + w / 2f,
            pillLeftDp = edgeLayout.pill.left,
            pillRightDp = edgeLayout.pill.right,
            gapDp = edgeCamera.tuning.cardGapDp
        )
    }
    EdgePillLayer(
        layout = edgeLayout,
        settings = settings,
        notifications = edgeLayerNotifications,
        safeIndex = edgeLayerIndex,
        plan = edgePlan,
        cardsAway = edgeCardsAway,
        cardOpen = expanded,
        wiring = wiring,
        runner = runner,
        autoHidden = settings.autoHidePill && ui.isAutoHidden && !expanded,
        onReveal = { ui.isAutoHidden = false; ui.touch() },
        ui = ui,
        clearance = edgeClearance,
        switchScaleAnim = switchScaleAnim,
        onPageSelected = selectFromPill,
        shellTakesOver = edgeLayoutOn && !edgeActive && !edgePillOwnsSurface,
        presence = { edgePresence.value },
        hiding = edgeHideRunning,
        shellIsSurface = edgeShellSurface && !edgePillOwnsSurface,
        contentAlpha = { layout.collapsedAlpha },
        shellStretch = {
            if (edgeShellSurface) (layout.safeHeight.value / edgeLayout.pill.height.coerceAtLeast(1f))
                .coerceIn(0.6f, 1.4f) else 1f
        },
        pulse = { layout.pillPulse },
        cardWidthDp = layout.expandedWidth.value,
        animSpeed = animSpeedNow,
        forceMerge = idleClosing,
        forcedMergeMs = if (idleClosing) idleClosePlan(animSpeedNow, true).step1Ms else 0,
        glideMs = ExpandMotion.glideMs(layout.expandStiffness, layout.expandDamping),
        snapSize = layout.orientationSnapping,
        pulses = layout.pulses,
        pulseCard = layout.edgePulseCard,
        pulseRedacted = layout.peakRedacted,
        pulseOwnsSurface = edge.pulseOwnsSurface,
        bubbleSwell = { layout.bubbleSwellScale },
        modifier = Modifier.align(Alignment.TopStart)
    )
    EdgeOverCardPill(
        layout = edgeLayout,
        settings = settings,
        card = layout.edgeOverCardPulse,
        redacted = layout.peakRedacted,
        animSpeed = animSpeedNow,
        ui = ui,
        modifier = Modifier.align(Alignment.TopStart),
        onHold = wiring.actions.pulse.touch,
        onTap = onOverCardTap
    )
}

@NonRestartableComposable
@Composable
internal fun BoxScope.EdgeHiddenHoldTarget(
    edgeCamera: EdgeCamera,
    ui: IslandInteractionState,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    context: Context
) {
    val hiddenTarget = remember(edgeCamera) {
        EdgePillLayoutMath.compute(camera = edgeCamera, hasSecondary = false).let {
            EdgePillRules.hiddenTarget(
                it.pill,
                EdgePillLayoutMath.holeCentreYDp(it)
            )
        }
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = hiddenTarget.left.dp, y = hiddenTarget.top.dp)
            .size(hiddenTarget.width.dp, hiddenTarget.height.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Auto-hidden: the first tap only brings the pill back
                        if (wiring.view.settings.autoHidePill && ui.isAutoHidden) {
                            ui.isAutoHidden = false
                            ui.touch()
                        }
                    },
                    onLongPress = {
                        ui.touch()
                        IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength),
                            HapticKind.Hold)
                        val longPress = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
                        if (longPress != SwipeAction.None) {
                            runner.runLongPress(longPress)
                        }
                    }
                )
            }
    )
}

internal class EdgePillState(
    val cardsAway: Boolean,
    val idleHidden: Boolean,
    val layoutOn: Boolean,
    val hold: BooleanArray,
    val presence: Animatable<Float, AnimationVector1D>,
    val fade: Animatable<Float, AnimationVector1D>,
    val showSettle: MutableState<Boolean>,
    val alive: State<Boolean>,
    val fading: State<Boolean>,
    val exiting: Boolean,
    val hideRunning: Boolean,
    val layerNotifications: List<IslandNotification>,
    val layerIndex: Int,
    val density: Float,
    val cutRect: EdgeRect?,
    val cutShape: Shape?,
    /** The grown pulse pill draws the body, the shell does not. */
    val pulseOwnsSurface: MutableState<Boolean>
)

@Composable
internal fun rememberEdgePill(
    edgeStyle: Boolean,
    edgeCamera: EdgeCamera?,
    notifications: List<IslandNotification>,
    safeIndex: Int,
    hasIdleStoredCards: Boolean,
    pillHidden: Boolean,
    expanded: Boolean,
    animationSpeed: AnimationSpeed
): EdgePillState {
    val edgeCardsAway = notifications.isEmpty() && hasIdleStoredCards
    // The hold on the camera middle also hides the cards-put-away pill.
    val edgeHasCards = edgeStyle && (notifications.isNotEmpty() ||
        EdgePillRules.cardsAwayShown(edgeCardsAway, pillHidden))
    val edgeIdleHidden = EdgePillRules.idleHidden(
        edgeStyle = edgeStyle, pillHidden = pillHidden, hasCards = notifications.isNotEmpty()
    )
    val edgeHold = remember { BooleanArray(2) }
    if (edgeHasCards) edgeHold[0] = true else if (!expanded && !edgeHold[1]) edgeHold[0] = false
    val edgeLayoutOn = edgeStyle && (edgeHasCards || edgeHold[0])
    // Hide / show of the edge pill (long press)
    val edgeSpeed = rememberUpdatedState(animationSpeed)
    val edgePresence = remember { Animatable(if (edgeLayoutOn) 1f else 0f) }
    val edgePillHiddenNow = rememberUpdatedState(pillHidden)
    val edgeFade = remember { Animatable(1f) }
    val edgeUserHidden = edgeStyle && pillHidden && notifications.isEmpty()
    // Set when the hold shows the pill again
    val showSettleState = remember { mutableStateOf(false) }
    var edgeShowSettle by showSettleState
    LaunchedEffect(edgeStyle, edgeUserHidden) {
        when {
            !edgeStyle -> edgeFade.snapTo(1f)
            // The pill sinks into the camera and rises out of it again (model/HideSink.kt)
            edgeUserHidden -> edgeFade.animateTo(
                0f,
                spring(
                    dampingRatio = PILL_SPRING_DAMPING,
                    stiffness = edgeSpeed.value.stiffness(PILL_SPRING_STIFFNESS)
                )
            )
            edgeFade.value < 1f -> {
                edgeShowSettle = true
                edgeFade.animateTo(
                    1f,
                    spring(
                        dampingRatio = PILL_SPRING_DAMPING,
                        stiffness = edgeSpeed.value.stiffness(PILL_SPRING_STIFFNESS)
                    )
                )
            }
        }
    }
    LaunchedEffect(edgeLayoutOn, edgeStyle) {
        if (!edgeStyle) return@LaunchedEffect
        if (!edgeLayoutOn && edgePillHiddenNow.value) {
            snapshotFlow { edgeFade.value }.first { it < 0.01f }
            edgePresence.snapTo(0f)
            return@LaunchedEffect
        }
        if (edgeLayoutOn && edgeFade.value < 1f) {
            edgePresence.snapTo(1f)
            return@LaunchedEffect
        }
        edgePresence.animateTo(
            if (edgeLayoutOn) 1f else 0f,
            spring(
                dampingRatio = PILL_SPRING_DAMPING,
                stiffness = edgeSpeed.value.stiffness(PILL_SPRING_STIFFNESS)
            )
        )
    }
    val aliveState = remember { derivedStateOf { edgePresence.value > 0.001f } }
    val edgeAlive by aliveState
    val fadingState = remember { derivedStateOf { edgeStyle && edgeFade.value < 0.999f } }
    val edgeFading by fadingState
    val edgeExiting = edgeStyle && !edgeLayoutOn && edgeAlive
    // the hold hid the pill with cards on it
    val edgeHideRunning = EdgePillRules.hideRunning(
        edgeStyle = edgeStyle, pillHidden = pillHidden, hasCards = notifications.isNotEmpty(), layerAlive = edgeAlive
    )
    val edgeHeld = remember { arrayOfNulls<List<IslandNotification>>(1) }
    if (notifications.isNotEmpty()) edgeHeld[0] = notifications else if (edgeCardsAway || !edgeAlive) edgeHeld[0] = null
    val edgeLayerNotifications = if (notifications.isEmpty() && !edgeCardsAway) edgeHeld[0]
        ?: notifications else notifications
    val edgeLayerIndex = if (edgeLayerNotifications === notifications) safeIndex else 0
    val edgeDensity = LocalDensity.current.density
    val edgeCutRect = remember(edgeCamera) {
        edgeCamera?.let { EdgePillLayoutMath.compute(camera = it, hasSecondary = false).pill }
    }
    val edgeCutShape = remember(edgeCamera) { edgeCamera?.let { edgePillShapeFor(it) } }
    val pulseOwnsSurface = remember { mutableStateOf(false) }
    return EdgePillState(
        cardsAway = edgeCardsAway,
        idleHidden = edgeIdleHidden,
        layoutOn = edgeLayoutOn,
        hold = edgeHold,
        presence = edgePresence,
        fade = edgeFade,
        showSettle = showSettleState,
        alive = aliveState,
        fading = fadingState,
        exiting = edgeExiting,
        hideRunning = edgeHideRunning,
        layerNotifications = edgeLayerNotifications,
        layerIndex = edgeLayerIndex,
        density = edgeDensity,
        cutRect = edgeCutRect,
        cutShape = edgeCutShape,
        pulseOwnsSurface = pulseOwnsSurface
    )
}

internal class EdgeSurface(
    val active: Boolean,
    val ownsSurface: Boolean,
    val shellSurface: Boolean,
    val cutOn: Boolean
)

@Composable
internal fun rememberEdgeSurface(
    layoutState: State<IslandLayout>,
    edge: EdgePillState,
    edgeCamera: EdgeCamera?,
    expanded: Boolean,
    pillHidden: Boolean,
    notifications: List<IslandNotification>
): EdgeSurface {
    val layoutForSurface by layoutState
    val edgeLayoutOn = edge.layoutOn
    val edgeCardsAway = edge.cardsAway
    val edgeHold = edge.hold
    val edgeFade = edge.fade
    val edgeFading by edge.fading
    var edgeShowSettle by edge.showSettle
    val edgeShellWide by remember {
        derivedStateOf {
            layoutForSurface.safeWidth.value > (edgeCamera?.pillWidthDp
                ?: EdgePillLayoutMath.PILL_WIDTH_DP) + EDGE_SHELL_GONE_DP ||
                layoutForSurface.safeHeight.value > (edgeCamera?.pillHeightDp
                    ?: EdgePillLayoutMath.PILL_HEIGHT_DP) + EDGE_SHELL_GONE_DP
        }
    }
    SideEffect { edgeHold[1] = edgeShellWide }
    val edgeActive = edgeLayoutOn && !expanded && !edgeShellWide
    val edgePillOwnsSurface = !expanded && (edgeFading || edgeShowSettle)
        && (!edgeShellWide || edgeShowSettle || !pillHidden)
    LaunchedEffect(edgeShowSettle) {
        if (!edgeShowSettle) return@LaunchedEffect
        withTimeoutOrNull(1500L) {
            snapshotFlow { edgeFade.value >= 0.999f && !edgeShellWide }.first { it }
        }
        edgeShowSettle = false
    }
    val edgeShellSurface = EdgePillRules.shellIsSurface(
        layoutOn = edgeLayoutOn,
        cardsAway = edgeCardsAway,
        hasCards = notifications.isNotEmpty(),
        expanded = expanded,
        shellWide = edgeShellWide
    )
    // The cut-out is only for a pill that draws its own surface on top of the shell
    val edgeCutOn = edgeActive && !edgeShellSurface && edgeCamera != null && edgeCamera.tuning.opacity < 0.999f
    return EdgeSurface(
        active = edgeActive,
        ownsSurface = edgePillOwnsSurface,
        shellSurface = edgeShellSurface,
        cutOn = edgeCutOn
    )
}
