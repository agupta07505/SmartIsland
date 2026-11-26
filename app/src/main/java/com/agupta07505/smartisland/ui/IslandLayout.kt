/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PillEnterStep
import com.agupta07505.smartisland.model.PulseCamera
import com.agupta07505.smartisland.model.PulsePeak
import com.agupta07505.smartisland.model.PulsePeakPlacement
import com.agupta07505.smartisland.model.PulseWidth
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.model.SideCameraZone
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.model.pillEnterStep
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlin.math.abs

/** The wide pulse's animated numbers, read where they are drawn. */
internal class PulseStates(
    val pulse: State<Float>,
    val centreShift: State<Float>,
    val lineExtra: State<Float>
)

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
    val expandedWidth: Dp,
    val singleExpandedWidth: Dp,
    val pickerLeftFraction: Float,
    val pickerEdgeFraction: Float,
    // The animated values stay behind State objects / functions
    private val shown: ShownStates,
    private val singleSurface: SingleSurfaceAlpha,
    private val collapsedShownState: State<Boolean>,
    private val expandedShownState: State<Boolean>,
    private val originX: () -> Dp,
    private val originY: () -> Dp,
    private val hidingAlphaState: State<Float>,
    private val pulseStates: PulseStates,
    /** The pill pulses its events here (the wide pulse or the edge pill's), so quick cards have no settled look. */
    val pulses: Boolean = false,
    /** Wide pulse layer of a centre camera (portrait). */
    val peakFade: PulsePeakFade? = null,
    val peakRedacted: Boolean = false,
    val edgePulseCard: IslandNotification? = null,
    /** The edge pill's pulse for an event that arrives over an open card. */
    val edgeOverCardPulse: IslandNotification? = null,
    val peakPlacement: PulsePeakPlacement? = null,
    /** The hole as the wide pulse reads it, notch look included. */
    val pulseCamera: PulseCamera = PulseCamera(CameraSide.Centre, 0f, null, null),
    /** Wide pulse over an open card, drawn on the cutout's spot (portrait). */
    val overCard: OverCardPulse? = null,
    private val bubbleSwellState: State<Float>? = null,
    /** Camera hole's centre from the collapsed pill's left edge (dp). */
    val cameraCentreInPillDp: Float? = null,
    /** Camera hole's width (dp); 0 = none. */
    val cameraGapDp: Float = 0f,
    val cameraBand: CameraBand = CameraBand(0f, 0f),
    /** Open island's centre in dp from the screen centre. */
    val expandedCentreOffsetDp: Float = 0f,
    val slotSide: SlotSide = SlotSide.Both,
    val cameraSide: CameraSide = CameraSide.Centre,
    /** Edge the wide pulse keeps fixed: the camera side, or the landscape classic pill's outer side. */
    val pulseAnchorSide: CameraSide = CameraSide.Centre,
    /** Side-camera tap band and glyph slots (dp from the pill's left edge). */
    val sideZone: SideCameraZone? = null,
    /** Expand spring as it really runs (speed-scaled), so the bubble's glide lands before it ends. */
    val expandStiffness: Float = 0f,
    val expandDamping: Float = 0f,
    /** Phone just turned: size changes snap instead of springing from the old orientation's size. */
    val orientationSnapping: Boolean = false,
    /** The island sinks into the camera when it hides (model/HideSink.kt), its opacity is the sink's own. */
    val sinks: Boolean = false
) {
    /** 1 at rest, bigger while the bubble swells with a pulse. */
    val bubbleSwellScale: Float get() = bubbleSwellState?.value ?: 1f

    /** Arrival pulse, 0..1 (the edge pill scales its own layer by it). */
    val pillPulse: Float get() = pulseStates.pulse.value

    /** How much taller the pill is than its settled height while the wide pulse draws a progress line (dp). */
    val lineExtraDp: Float get() = pulseStates.lineExtra.value

    /** How far the wide pulse has moved the pill's centre right of the settled one (dp), a split pulse only. */
    val pulseCentreShiftDp: Float get() = pulseStates.centreShift.value

    /** How far the wide pulse has grown the settled pill (dp); 0 when no wide pulse draws. */
    val widePulseGrowDp: Float
        get() = if (peakFade == null) 0f else miniPillWidth.value * PILL_PULSE_WIDTH_FRACTION * pulseStates.pulse.value
    val expandProgress: Float get() = shown.expandProgress.value
    val expandProgressY: Float get() = shown.expandProgressY.value
    val singleSurfaceAlpha: Float get() = singleSurface.surface.value
    val singleShadowAlpha: Float get() = singleSurface.shadow.value
    val hidingAlpha: Float get() = hidingAlphaState.value

    /** Opacity a hide puts on 1 layer: a sinking island fades as a whole, so its layers do not fade themselves. */
    val hidingFade: Float get() = if (sinks) 1f else hidingAlphaState.value
    val width: Dp get() = shown.width.value
    val height: Dp get() = shown.height.value
    val yOffset: Dp get() = originY()
    val topShift: Dp get() = shown.topShift.value
    val animatedXOffset: Dp get() = originX()
    val collapsedAlpha: Float get() = shown.collapsedAlpha.value
    val expandedAlpha: Float get() = shown.expandedAlpha.value
    val contentScale: Float get() = shown.contentScale.value
    val contentSlideY: Dp get() = shown.contentSlideY.value
    val safeWidth: Dp get() = (if (isHiding && hidingAlphaState.value <= 0.01f) 0.dp else shown.width.value)
        .coerceAtLeast(0.dp)
    val safeHeight: Dp get() = (if (isHiding && hidingAlphaState.value <= 0.01f) 0.dp else shown.height.value)
        .coerceAtLeast(0.dp)
    val safeRadius: Dp get() = shown.radius.value.coerceAtLeast(0.dp)
    val collapsedShown: Boolean get() = collapsedShownState.value
    val expandedShown: Boolean get() = expandedShownState.value
}

@Composable
internal fun CollapsedContentLayer(
    layout: IslandLayout,
    fade: CollapsedContentFade,
    activeMode: IslandMode,
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    idleSuppressed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxSlidePx = with(density) { pillContentMaxSlideDp(settings.width).dp.toPx() }
    val shownMode = fade.notification?.mode ?: activeMode
    val idleFrame = rememberIdleWidgetFrame(settings, noCard = shownMode == IslandMode.Empty, ui = ui,
        suppressed = idleSuppressed)
    // a different card is now in the pill
    val shownKey = fade.notification?.key ?: idleFrame?.kind?.id
    SideEffect { ui.idleFrameNow = idleFrame }
    DisposableEffect(ui) { onDispose { ui.idleFrameNow = null } }
    // Track which card already has its entry slide set up.
    var seenKey by remember { mutableStateOf(shownKey) }
    val cameraGapDp = layout.cameraGapDp
    val cameraCentreInPillDp = layout.cameraCentreInPillDp
    val deadZone = collapsedDeadZone(layout, settings)

    PillEnterSlide(shownKey, maxSlidePx, ui, animSpeed) { seenKey = shownKey }
    val entersNow: () -> Boolean = { ui.pendingEnterSign != 0 && seenKey != shownKey }
    // Slide (px) the layer carries: finger drag plus the incoming card's offset. A side camera keeps the finger's
    // follow to the same short travel both ways, and the incoming card slides in from the side opposite the swipe
    // (toward the camera it emerges from the hole's edge, where the clip below cuts it), so both swipes look alike
    val layerSlidePx: (Density) -> Float = { d ->
        if (entersNow()) {
            ui.pendingEnterSign * maxSlidePx * PILL_ENTER_DISTANCE_FRACTION
        } else {
            val follow = if (deadZone == null) ui.pillContentDragX else {
                val limitPx = with(d) { deadZone.slideLimitDp.dp.toPx() }
                ui.pillContentDragX.coerceIn(-limitPx, limitPx)
            }
            follow + ui.pillEnter.value
        }
    }
    Box(
        modifier = modifier
            .pillContentSize(layout, settings)
            .cameraHoleMask(deadZone, cameraCentreInPillDp, cameraGapDp, settings.width)
            .graphicsLayer {
                val slide = layerSlidePx(this)
                translationX = slide
                val enterShare = if (maxSlidePx > 0f) {
                    // A swipe's new card, in the frame before its slide starts, counts as the start of the slide
                    val enterPx = if (entersNow()) maxSlidePx * PILL_ENTER_DISTANCE_FRACTION else abs(ui.pillEnter.value)
                    (enterPx / (maxSlidePx * PILL_ENTER_DISTANCE_FRACTION)).coerceIn(0f, 1f)
                } else 0f
                alpha = layout.collapsedAlpha * fade.factor * (1f - PILL_ENTER_FADE * enterShare) * (1f - ui.chimeShow.value)
            }
    ) {
        CollapsedCardSwap(layout, fade, shownMode, idleFrame, settings, ui, animSpeed, cameraGapDp,
            cameraCentreInPillDp)
    }
}

/** Side camera's dead zone: how far pill content may slide toward the hole, where it is clipped, where its fade ends. */
private class CollapsedDeadZone(
    val slideLimitDp: Float,
    val clipFromCameraDp: Float,
    val side: CameraSide,
    val fadeEndFromCameraDp: Float
)

private fun collapsedDeadZone(layout: IslandLayout, settings: SmartIslandSettings): CollapsedDeadZone? {
    val sideCam = layout.cameraSide
    val sidePlan = layout.sideZone?.plan
    val cameraCentreInPillDp = layout.cameraCentreInPillDp
    if (sideCam == CameraSide.Centre || sidePlan == null || cameraCentreInPillDp == null) return null
    val cam = if (sideCam == CameraSide.Left) cameraCentreInPillDp else settings.width - cameraCentreInPillDp
    val dead = SideCamera.deadZoneEndDp(cam, layout.cameraGapDp)
    val glyph = SideCamera.glyphStartFromCameraDp(sideCam, sidePlan, settings.width)
    val slideLimit = SideCamera.slideTowardCameraLimitDp(dead, glyph)
    val clip = SideCamera.contentClipFromCameraDp(dead, glyph)
    // Fade ends where the glyph rests, so a glyph at rest is untouched and one slid toward the hole fades
    val fadeEnd = maxOf(clip, SideCamera.fadeEndFromCameraDp(glyph, slideLimit))
    return CollapsedDeadZone(slideLimit, clip, sideCam, fadeEnd)
}

/** A new card in the pill slides to the middle from the side opposite the swipe. */
@Composable
private fun PillEnterSlide(
    shownKey: Any?,
    maxSlidePx: Float,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    onKeySeen: () -> Unit
) {
    LaunchedEffect(shownKey) {
        val sign = ui.pendingEnterSign
        when (pillEnterStep(sign, ui.pillEnter.value)) {
            PillEnterStep.Nothing -> {
                onKeySeen()
                return@LaunchedEffect
            }
            PillEnterStep.SettleStale -> {
                onKeySeen()
                ui.pillEnter.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = animSpeed.stiffness(CARD_ENTER_STIFFNESS)
                    )
                )
                return@LaunchedEffect
            }
            PillEnterStep.Start -> Unit
        }
        val from = sign * maxSlidePx * PILL_ENTER_DISTANCE_FRACTION
        // nothing suspends after the snap, so no frame shows the old position
        ui.pillEnter.snapTo(from)
        onKeySeen()
        ui.pendingEnterSign = 0
        ui.pillContentDragX = 0f
        ui.pillEnter.animateTo(
            0f,
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = animSpeed.stiffness(CARD_ENTER_STIFFNESS)
            )
        )
    }
}

private fun Modifier.pillContentSize(shell: IslandLayout, settings: SmartIslandSettings): Modifier =
    layout { measurable, constraints ->
        val settled = settings.width.dp.roundToPx()
        val follow = shell.expandProgress <= 0.001f && !shell.isHiding
        val w = if (follow) shell.width.roundToPx().coerceAtLeast(0) else settled
        val h = settings.height.dp.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(w, h))
        this.layout(w, h) { placeable.place(0, 0) }
    }

private fun Modifier.cameraHoleMask(
    zone: CollapsedDeadZone?,
    cameraCentreInPillDp: Float?,
    cameraGapDp: Float,
    pillWidthDp: Float
): Modifier = drawWithContent {
    if (zone == null) {
        drawContent()
    } else {
        // Content is hidden only where the camera hole really is: a soft round mask centred on it, radius clear plus a
        // short feather
        val w = size.width
        val camFromEdgeDp = if (zone.side == CameraSide.Left) {
            cameraCentreInPillDp ?: 0f
        } else {
            pillWidthDp - (cameraCentreInPillDp ?: 0f)
        }
        val centreX = if (zone.side == CameraSide.Left) {
            camFromEdgeDp.dp.toPx()
        } else {
            w - camFromEdgeDp.dp.toPx()
        }
        val holeRadiusPx = (cameraGapDp / 2f).dp.toPx().coerceAtLeast(0f)
        // Feather ends where a glyph at rest begins, so a resting glyph is never touched
        val featherEndPx = ((zone.fadeEndFromCameraDp - camFromEdgeDp).dp.toPx())
            .coerceAtLeast(holeRadiusPx + 1.dp.toPx())
        val clear = Color.Transparent
        val solid = Color.Black
        val inner = (holeRadiusPx / featherEndPx).coerceIn(0f, 0.99f)
        val mask = Brush.radialGradient(
            0f to clear, inner to clear, 1f to solid,
            center = Offset(centreX, size.height / 2f),
            radius = featherEndPx
        )
        drawIntoCanvas { canvas ->
            val paint = Paint()
            canvas.saveLayer(Rect(0f, 0f, w, size.height), paint)
            this@drawWithContent.drawContent()
            drawRect(brush = mask, blendMode = BlendMode.DstIn)
            canvas.restore()
        }
    }
}

@Composable
private fun CollapsedCardSwap(
    layout: IslandLayout,
    fade: CollapsedContentFade,
    shownMode: IslandMode,
    idleFrame: IdleWidgetFrame?,
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    cameraGapDp: Float,
    cameraCentreInPillDp: Float?
) {
    val swap = rememberCardSwap(fade.notification, ui.pendingEnterSign != 0, animSpeed.durationMs(CARD_SWAP_FADE_MS))
    val outgoing = swap.outgoing
    val peak = layout.peakFade
    val peakCard = peak?.card
    val ringAlpha = rememberGlyphRingAlpha(
        remember(peakCard) { PulsePeak.hidesGlyphRing(peakCard, System.currentTimeMillis()) }, animSpeed
    )
    // the settled content crossfades with the wide pulse
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            alpha = peak?.settledAlphaNow ?: 1f
            // The extra height is for the underline. Keep the glyphs at cutout height.
            translationY = -layout.lineExtraDp * this.density / 2f
        }
    ) {
        CompositionLocalProvider(LocalGlyphRingAlpha provides ringAlpha) {
            CollapsedSettledSwap(layout, fade, shownMode, idleFrame, settings, swap, outgoing, cameraGapDp,
                cameraCentreInPillDp)
        }
    }
    if (peak != null && peakCard != null && peak.drawsPeak) {
        PulsePeakLayer(
            bubbleOnLeft = if (layout.hasCompanion) layout.isCircleLeft else null,
            swell = { layout.bubbleSwellScale },
            card = peakCard,
            settings = settings,
            cameraGapDp = layout.pulseCamera.gapDp,
            redacted = layout.peakRedacted,
            animSpeed = animSpeed,
            cameraShiftDp = PulseWidth.cameraShiftDp(layout.pulseCamera.centreInPillDp, settings.width) -
                layout.pulseCentreShiftDp,
            cameraSide = layout.peakPlacement?.side ?: CameraSide.Centre,
            glyphFromCameraDp = layout.peakPlacement?.glyphFromCameraDp ?: 0f,
            notch = layout.peakPlacement?.notch == true,
            classic = layout.peakPlacement?.classic == true,
            lineExtra = { layout.lineExtraDp },
            modifier = Modifier.graphicsLayer { alpha = peak.alphaNow }
        )
    }
}

@Composable
private fun CollapsedSettledSwap(
    layout: IslandLayout,
    fade: CollapsedContentFade,
    shownMode: IslandMode,
    idleFrame: IdleWidgetFrame?,
    settings: SmartIslandSettings,
    swap: CardSwap,
    outgoing: IslandNotification?,
    cameraGapDp: Float,
    cameraCentreInPillDp: Float?
) {
    if (outgoing != null && CollapsedPulse.drawsSettled(outgoing, layout.pulses)) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = swapOutgoingAlpha(swap.progress()) }) {
            IslandCollapsedContent(
                mode = outgoing.mode,
                notification = outgoing,
                collapsedAlpha = layout.collapsedAlpha,
                settings = settings,
                idleWidget = null,
                cameraGapDp = cameraGapDp,
                pillWidthDp = settings.width,
                cameraCentreInPillDp = cameraCentreInPillDp,
                cameraSide = layout.cameraSide,
                sidePlan = layout.sideZone?.plan,
                pulse = { layout.pillPulse }
            )
        }
    }
    if (!CollapsedPulse.drawsSettled(fade.notification, layout.pulses)) return
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = if (swap.crossing) swapIncomingAlpha(swap.progress()) else 1f }
    ) {
        IslandCollapsedContent(
            mode = shownMode,
            notification = fade.notification,
            collapsedAlpha = layout.collapsedAlpha,
            settings = settings,
            idleWidget = idleFrame,
            cameraGapDp = cameraGapDp,
            pillWidthDp = settings.width,
            cameraCentreInPillDp = cameraCentreInPillDp,
            cameraSide = layout.cameraSide,
            sidePlan = layout.sideZone?.plan,
            pulse = { layout.pillPulse }
        )
    }
}

internal fun pillContentMaxSlideDp(pillWidthDp: Float): Float = (pillWidthDp * 0.4f).coerceIn(8f, 56f)

internal const val CARD_ENTER_STIFFNESS = 380f

internal const val PILL_ENTER_DISTANCE_FRACTION = 0.6f

/** Share of its opacity the incoming card starts without (0 = no fade). */
internal const val PILL_ENTER_FADE = 0.35f

internal const val PILL_STRETCH_DAMPING = 0.72f
internal const val PILL_STRETCH_STIFFNESS = 420f

internal const val PILL_PULSE_WIDTH_FRACTION = 0.125f

internal const val HEIGHT_FOLLOW_STIFFNESS = 900f

internal const val SPRING_REST_DP = 1f

internal class CollapsedContentFade(val notification: IslandNotification?, private val presence: State<Float>?) {
    val factor: Float get() = presence?.value ?: 1f
}

@Composable
internal fun rememberCollapsedContentFade(
    active: IslandNotification?,
    animSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    fadeOut: Boolean = false
): CollapsedContentFade {
    val lastShown = remember { arrayOfNulls<IslandNotification>(1) }
    if (active != null) lastShown[0] = active
    val presence = animateFloatAsState(
        targetValue = if (active != null) 1f else 0f,
        animationSpec = if (active == null && !fadeOut) snap()
        else tween(durationMillis = animSpeed.durationMs(300), easing = FastOutSlowInEasing),
        label = "collapsedContentPresence"
    )
    // flips once, when the card has faded out completely
    val faded by remember(active == null, presence) { derivedStateOf { active == null && presence.value <= 0.001f } }
    if (faded) lastShown[0] = null
    val held = active ?: lastShown[0]
    // With nothing held the idle content shows at full strength
    return CollapsedContentFade(held, if (held == null) null else presence)
}
