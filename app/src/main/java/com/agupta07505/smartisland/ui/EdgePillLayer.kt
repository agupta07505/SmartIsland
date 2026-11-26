/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgeGlyphSlots
import com.agupta07505.smartisland.model.EdgePillLayout
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillMotion
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.EdgePulseOwnership
import com.agupta07505.smartisland.model.EdgeSideCamera
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotchCompanionPlan
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.PillEnterStep
import com.agupta07505.smartisland.model.PulsePeak
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.pillEnterStep
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.Job
import kotlin.math.abs

@Composable
internal fun EdgePillLayer(
    layout: EdgePillLayout,
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    safeIndex: Int,
    plan: NotchCompanionPlan,
    cardsAway: Boolean,
    cardOpen: Boolean,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    /** Auto-hide pill is on and the pill has hidden itself. */
    autoHidden: Boolean = false,
    onReveal: () -> Unit = {},
    ui: IslandInteractionState,
    /** Distance the open card has left the pill's spot. */
    clearance: () -> Float = { 1f },
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    onPageSelected: (Int) -> Unit = {},
    /** Card shell is the island's 1 surface (cards on the island). */
    shellIsSurface: Boolean = false,
    shellTakesOver: Boolean = false,
    /** Hide / show: 1 with the pill in place. */
    presence: () -> Float = { 1f },
    /** Hold hid the pill and it is sliding out. */
    hiding: Boolean = false,
    /** Fade of the pill's glyphs, read where it is drawn. */
    contentAlpha: () -> Float = { 1f },
    /** Card shell's length against the pill's (1 = the pill's own). */
    shellStretch: () -> Float = { 1f },
    /** Arrival pulse, 0..1, read where it is drawn. */
    pulse: () -> Float = { 0f },
    /** Open card's width, for how far a drag into the screen has to go. */
    cardWidthDp: Float = 0f,
    animSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    /** Merge the pill with its bubble (idle-circle close). */
    forceMerge: Boolean = false,
    forcedMergeMs: Int = 0,
    glideMs: Int = 0,
    /** Phone just turned: the pill's length snaps instead of growing from the old orientation's. */
    snapSize: Boolean = false,
    /** The pill pulses its events, so quick cards have no settled look ([CollapsedPulse.drawsSettled]). */
    pulses: Boolean = false,
    /** Card whose wide pulse the pill plays, null when nothing pulses. */
    pulseCard: IslandNotification? = null,
    pulseRedacted: Boolean = false,
    /** Set while the grown pulse pill draws the body in place of the shell. */
    pulseOwnsSurface: MutableState<Boolean>? = null,
    /** 1 at rest, bigger while the bubble swells with a pulse. */
    bubbleSwell: () -> Float = { 1f },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // A tap swap uses the idle bounds as its target. Layout callbacks skip updates during the swap.
    val pillBounds = remember { arrayOfNulls<Rect>(1) }
    // The edge gesture stays installed as the pill moves. Give it fresh geometry without cancelling the touch.
    val cardWidthState = rememberUpdatedState(cardWidthDp)
    val planState = rememberUpdatedState(plan)
    val clearanceState = rememberUpdatedState(clearance)
    val layoutState = rememberUpdatedState(layout)
    val awayState = rememberUpdatedState(cardsAway)
    val openState = rememberUpdatedState(cardOpen)
    val hiddenState = rememberUpdatedState(autoHidden)
    val revealState = rememberUpdatedState(onReveal)
    val notification = notifications.getOrNull(safeIndex)
    val notificationState = rememberUpdatedState(notification)
    // Cards put away (a tap on the camera middle)
    val glyphFade = rememberCollapsedContentFade(if (cardsAway) null else notification, animSpeed, fadeOut = true)
    val shownNotification = glyphFade.notification
    val screenDensity = LocalDensity.current
    val glyphMaxSlidePxNow = with(screenDensity) { EdgePillRules.glyphMaxSlideDp(layout.pill.height).dp.toPx() }
    val glyphMaxSlideState = rememberUpdatedState(glyphMaxSlidePxNow)
    val animSpeedState = rememberUpdatedState(animSpeed)
    // The wait for a card after a swipe along the edge; a newer swipe replaces it
    val enterWait = remember { arrayOfNulls<Job>(1) }
    val shownKey = shownNotification?.key
    val glyphEnter = rememberEdgeGlyphEnter(ui, notification?.key, shownKey, animSpeedState, glyphMaxSlideState,
        enterWait)
    val entersNow = glyphEnter.entersNow
    val glyphEnterShare = glyphEnter.glyphEnterShare
    val tuning = layout.camera.tuning
    val opacity = tuning.opacity.coerceIn(0f, 1f)
    val pillShape = remember(layout.camera) { edgePillShapeFor(layout.camera) }
    val lenTarget = layout.pill.height
    val lenAnim = remember { Animatable(lenTarget) }
    LaunchedEffect(lenTarget, shellIsSurface) {
        if (shellIsSurface || snapSize) lenAnim.snapTo(lenTarget)
        else lenAnim.animateTo(
            lenTarget,
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = animSpeed.stiffness(PILL_SPRING_STIFFNESS)
            )
        )
    }
    val settledLayout = if (abs(lenAnim.value - lenTarget) < 0.01f) layout else
        EdgePillLayoutMath.compute(layout.camera, pillHeightDp = lenAnim.value, hasSecondary = layout.secondary != null)
    // The wide pulse lengthens the pill: a camera in the middle has each end go its own way, a camera toward 1 end
    // keeps its end of the pill and the pill grows away from it. The bubble is placed again from the new ends
    val peakShape = rememberEdgePulseShape(pulseCard, layout, pulseRedacted)
    val growth = rememberEdgePulseGrowth(peakShape, animSpeed, pulseOwnsSurface)
    val peakFade = rememberPulsePeakFade(pulseCard, animSpeed, pulseRedacted)
    val growthShape = growth.shape
    val grownLayout = growth.grownLayout(settledLayout, EdgePillLayoutMath.holeCentreYDp(layout),
        hasSecondary = layout.secondary != null)
    // Thicker by the progress line's height while the pulse draws one; the screen-edge side stays, the content holds
    val lineExtra = rememberPulseLineExtra(pulseCard, animSpeed)
    val drawLayout = EdgePillLayoutMath.thickened(grownLayout ?: settledLayout, lineExtra.value)
    val pulseGrown = EdgePulseOwnership.ownsSurface(growth.owns.value, cardOpen)
    val shadowOn = settings.enableShadow && settings.shadowElevation > 0f && opacity >= OPAQUE && !shellTakesOver
        && (!shellIsSurface || pulseGrown)
    val followLimitPx = EdgePillLayoutMath.followLimitDp(drawLayout)?.let { it * screenDensity.density }
    val glyphSlidePx: () -> Float = {
        EdgePillRules.glyphSlidePx(
            entersNow(), ui.pendingEnterSign, glyphMaxSlideState.value, PILL_ENTER_DISTANCE_FRACTION,
            EdgePillRules.followPx(ui.pillContentDragX, followLimitPx),
            ui.pillEnter.value
        )
    }
    val slots = EdgePillLayoutMath.glyphSlots(settledLayout)

    // Arrival pulse lengthens the pill along the edge, lean follows the finger. The look decides the lean / pull
    // origin: flat sits on the screen edge, compact floats (portrait's top anchor)
    val flatLook = EdgePillRules.flatCameraSide(tuning.flatEdge, tuning.xOffsetDp)
    val pillWidthPx = drawLayout.pill.width * screenDensity.density
    val pillMotionNow: () -> EdgePillMotion = {
        EdgePillRules.pillMotion(
            cameraOnLeft = layout.camera.onLeft,
            acrossPx = ui.dragOffset,
            alongPx = ui.pillDragOffsetX,
            arm = ui.pillArm.value,
            dead = ui.deadZone.value,
            pull = ui.swipeDownPull,
            pulseScale = 1f + PILL_PULSE_WIDTH_FRACTION * pulse(),
            flat = flatLook,
            widthPx = pillWidthPx
        )
    }
    // Glyphs are cut at the camera hole's edges, only when the system reported the hole
    val holeCamera = layout.camera
    val holeClip = holeCamera.cutoutReported && holeCamera.holeLengthDp > 0f
    val holeTopDp = holeCamera.holeCentreYDp - holeCamera.holeLengthDp / 2f - drawLayout.pill.top
    val settledHoleTopDp = holeCamera.holeCentreYDp - holeCamera.holeLengthDp / 2f - settledLayout.pill.top
    val holeCentreXDp =
        if (holeCamera.onLeft) holeCamera.centerFromEdgeDp else holeCamera.screenWidthDp - holeCamera.centerFromEdgeDp
    val exitTravelPx = EdgePillRules.exitTravelDp(layout.camera.pillWidthDp,
        layout.camera.tuning.xOffsetDp) * screenDensity.density
    val touch = EdgePillTouch(
        ui, wiring, runner, scope, context, enterWait, pillBounds, hiddenState, revealState, layoutState,
        openState, awayState, notificationState, planState, clearanceState, cardWidthState
    )
    Box(
        modifier = modifier.fillMaxSize().graphicsLayer {
            // The one slide for everything on this layer (pill, glyphs, bubble)
            val here = presence()
            translationX = EdgePillRules.exitSlidePx(layout.camera.onLeft, exitTravelPx, here)
            alpha = EdgePillRules.exitAlpha(here) * ui.pulseCoverFactor(TapSwap.CUTOUT_EDGE_PILL)
        }
    ) {
        Box(
            modifier = Modifier
                .offset(x = drawLayout.pill.left.dp, y = drawLayout.pill.top.dp)
                .size(drawLayout.pill.width.dp, drawLayout.pill.height.dp)
                // Hit-test the resting pill. The graphics layer below only moves its appearance.
                .pointerInput(Unit) {
                    awaitEachGesture { with(touch) { handle() } }
                }
                .onGloballyPositioned { coords -> if (!ui.tapSwapActive) pillBounds[0] = coords.boundsInWindow() }
                .graphicsLayer {
                    val motion = pillMotionNow()
                    scaleX = motion.scaleX * ui.edgeMergeScaleX
                    scaleY = motion.scaleY * ui.edgeMergeScaleY
                    transformOrigin = TransformOrigin(motion.originX, 0.5f)
                    // Pill leans with the finger (compact look: camera-side edge held in place)
                    translationX = motion.translationX
                    translationY = motion.translationY
                }
                .then(if (shadowOn) Modifier.shadow(settings.shadowElevation.dp, pillShape, clip = false) else Modifier)
                .clip(pillShape)
                // Black fill runs unbroken under the camera (see EDGE_CLEAR_LENS), no see-through circle in the pill.
                .edgePillFill(
                    color = Color.Black.copy(
                        alpha = if (pulseGrown) opacity else if (shellIsSurface) {
                            EdgePillRules.pillSurfaceAlpha(true, false, clearanceState.value(), opacity,
                                GLIDE_CLEARANCE)
                        } else if (shellTakesOver) 0f else opacity
                    ),
                    enabled = holeClip && EDGE_CLEAR_LENS,
                    holeCentreXInPillDp = holeCentreXDp - drawLayout.pill.left,
                    holeCentreYInPillDp = holeTopDp + holeCamera.holeLengthDp / 2f,
                    holeWidthDp = EdgePillRules.holeClearWidthDp(holeCamera.holeWidthDp, holeCamera.holeLengthDp),
                    holeLengthDp = holeCamera.holeLengthDp,
                    motion = pillMotionNow,
                    mergeScaleX = { ui.edgeMergeScaleX },
                    mergeScaleY = { ui.edgeMergeScaleY }
                )
                .holdGlowOutline(shape = { pillShape }, glow = { ui.holdGlow.value }),
            contentAlignment = Alignment.Center
        ) {
            val ringAlpha = rememberGlyphRingAlpha(
                remember(pulseCard) { PulsePeak.hidesGlyphRing(pulseCard, System.currentTimeMillis()) }, animSpeed
            )
            if (shownNotification != null && CollapsedPulse.drawsSettled(shownNotification, pulses)) {
                CompositionLocalProvider(LocalGlyphRingAlpha provides ringAlpha) {
                    EdgePillGlyphs(
                        shown = shownNotification,
                        settings = settings,
                        drawLayout = settledLayout,
                        holeCamera = holeCamera,
                        holeClip = holeClip,
                        holeTopDp = settledHoleTopDp,
                        slots = slots,
                        pillMotionNow = pillMotionNow,
                        ui = ui,
                        glyphSlidePx = glyphSlidePx,
                        shellStretch = shellStretch,
                        contentAlpha = contentAlpha,
                        glyphFade = glyphFade,
                        glyphEnterShare = glyphEnterShare,
                        topShiftDp = settledLayout.pill.top - drawLayout.pill.top,
                        leftShiftDp = settledLayout.pill.left - drawLayout.pill.left,
                        settledAlpha = { peakFade.settledAlphaNow }
                    )
                }
            }
            val peakDrawCard = peakFade.card
            if (peakDrawCard != null && growthShape != null && peakFade.drawsPeak) {
                EdgePulsePeakLayer(
                    card = peakDrawCard,
                    settings = settings,
                    shape = growthShape,
                    holeFromTopDp = EdgePillLayoutMath.holeCentreYDp(settledLayout) - drawLayout.pill.top,
                    holeLengthDp = layout.camera.holeLengthDp,
                    cornerDp = layout.camera.tuning.cornerRadiusDp,
                    cameraOnLeft = layout.camera.onLeft,
                    redacted = pulseRedacted,
                    animSpeed = animSpeed,
                    lineExtraDp = lineExtra.value,
                    bubbleBelow = drawLayout.secondary?.let { it.top >= drawLayout.pill.top + drawLayout.pill.height / 2f },
                    swell = bubbleSwell,
                    modifier = Modifier.graphicsLayer { alpha = peakFade.alphaNow }
                )
            }
        }

        // the other events: bubble, the other event's pill on the pill's spot.
        if (!cardsAway) {
            EdgeCompanions(
                layout = drawLayout,
                settings = settings,
                notifications = notifications,
                plan = plan,
                cardOpen = cardOpen,
                clearance = clearance,
                switchScaleAnim = switchScaleAnim,
                onPageSelected = onPageSelected,
                wiring = wiring,
                ui = ui,
                forceMerge = forceMerge,
                forcedMergeMs = forcedMergeMs,
                hiding = hiding,
                glideMs = glideMs,
                bubbleSwell = bubbleSwell
            )
        }
    }
}

private class EdgeGlyphEnter(val entersNow: () -> Boolean, val glyphEnterShare: () -> Float)

@Composable
private fun rememberEdgeGlyphEnter(
    ui: IslandInteractionState,
    notificationKey: String?,
    shownKey: String?,
    animSpeedState: State<AnimationSpeed>,
    glyphMaxSlideState: State<Float>,
    enterWait: Array<Job?>
): EdgeGlyphEnter {
    // Start the edge slide on the first frame that shows the new card.
    var seenKey by remember { mutableStateOf(shownKey) }
    val entersNow: () -> Boolean = { ui.pendingEnterSign != 0 && seenKey != shownKey }
    // The edge layout does not compose the classic CollapsedContentLayer
    LaunchedEffect(notificationKey) {
        val sign = ui.pendingEnterSign
        when (pillEnterStep(sign, ui.pillEnter.value)) {
            PillEnterStep.Nothing -> {
                seenKey = shownKey
                return@LaunchedEffect
            }
            PillEnterStep.SettleStale -> {
                seenKey = shownKey
                ui.pillEnter.animateTo(0f, edgeEnterSpring(animSpeedState.value))
                return@LaunchedEffect
            }
            PillEnterStep.Start -> Unit
        }
        enterWait[0]?.cancel()
        // Nothing suspends after the snap, no frame in between
        ui.pillEnter.snapTo(EdgePillRules.enterStartPx(sign, glyphMaxSlideState.value, PILL_ENTER_DISTANCE_FRACTION))
        seenKey = shownKey
        ui.pendingEnterSign = 0
        ui.pillContentDragX = 0f
        ui.pillEnter.animateTo(0f, edgeEnterSpring(animSpeedState.value))
    }
    val glyphEnterShare: () -> Float = {
        EdgePillRules.glyphEnterShare(entersNow(), ui.pillEnter.value, glyphMaxSlideState.value,
            PILL_ENTER_DISTANCE_FRACTION)
    }
    return EdgeGlyphEnter(entersNow, glyphEnterShare)
}

@NonRestartableComposable
@Composable
private fun EdgePillGlyphs(
    shown: IslandNotification,
    settings: SmartIslandSettings,
    drawLayout: EdgePillLayout,
    holeCamera: EdgeCamera,
    holeClip: Boolean,
    holeTopDp: Float,
    slots: EdgeGlyphSlots,
    pillMotionNow: () -> EdgePillMotion,
    ui: IslandInteractionState,
    glyphSlidePx: () -> Float,
    shellStretch: () -> Float,
    contentAlpha: () -> Float,
    glyphFade: CollapsedContentFade,
    glyphEnterShare: () -> Float,
    /** The pill's top moved by this much while it pulses, the settled glyphs stay where they were as they fade. */
    topShiftDp: Float,
    /** The pill's left moved by this much (a right-edge pill thickened toward the inside), the glyphs stay where they were. */
    leftShiftDp: Float,
    settledAlpha: () -> Float
) {
    val slotW = slots.width
    val slotH = slots.height
    val glyphTop = slots.glyphTop
    val valueAlong = EdgePillLayoutMath.valueAlong(drawLayout)
    val slotLeft = slots.left
    val fadeSpan = if (holeClip) EdgePillLayoutMath.sideZone(drawLayout)?.let {
        EdgeSideCamera.fadeSpan(
            holeCamera.spot, drawLayout.pill.height, holeTopDp + holeCamera.holeLengthDp / 2f, holeCamera.holeLengthDp,
                it
        )
    } else null
    Box(Modifier.fillMaxSize().edgeGlyphFade(fadeSpan, pillMotionNow) { ui.edgeMergeScaleY }) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = slotLeft.dp, y = glyphTop.dp)
                .size(slotW.dp, slotH.dp)
                .graphicsLayer {
                    // Glyphs keep their size while the pill pulses and stretches, and swell with its arm cue like
                    // portrait's
                    val m = pillMotionNow()
                    scaleX = EdgePillRules.glyphScale(m.scaleX * ui.edgeMergeScaleX, m.cue)
                    scaleY = EdgePillRules.glyphScale(m.scaleY * ui.edgeMergeScaleY, m.cue)
                    translationX = leftShiftDp * density
                    translationY = glyphSlidePx() + topShiftDp * density +
                        (shellStretch() - 1f) * (glyphTop + slotH / 2f - drawLayout.pill.height / 2f) * density
                    alpha = contentAlpha()
                        .coerceIn(0f, 1f) * settledAlpha() * glyphFade.factor * (1f - PILL_ENTER_FADE * glyphEnterShare())
                },
            contentAlignment = Alignment.Center
        ) {
            CollapsedGlyphContent(shown.mode, shown, settings)
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(y = valueAlong.topDp.dp)
                .size(drawLayout.pill.width.dp, valueAlong.lengthDp.dp)
                .graphicsLayer {
                    val m = pillMotionNow()
                    scaleX = EdgePillRules.glyphScale(m.scaleX * ui.edgeMergeScaleX, m.cue)
                    scaleY = EdgePillRules.glyphScale(m.scaleY * ui.edgeMergeScaleY, m.cue)
                    translationX = leftShiftDp * density
                    translationY = glyphSlidePx() + topShiftDp * density +
                        (shellStretch() - 1f) * (valueAlong.centreDp - drawLayout.pill.height / 2f) * density
                    alpha = contentAlpha()
                        .coerceIn(0f, 1f) * settledAlpha() * glyphFade.factor * (1f - PILL_ENTER_FADE * glyphEnterShare())
                },
            contentAlignment = if (valueAlong.fromBottom) Alignment.BottomCenter else Alignment.TopCenter
        ) {
            Box(Modifier.valueAlongStrip(shown, settings), contentAlignment = Alignment.Center) {
                CollapsedValueContent(shown.mode, shown, settings)
            }
        }
    }
}
