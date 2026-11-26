/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.PulseWidth
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.withTimeoutOrNull

/** The wide pulse of an event that arrives while a card is open: its fade and its width, both run apart from the shell. */
internal class OverCardPulse(
    val fade: PulsePeakFade,
    private val pulse: State<Float>,
    private val centreShift: State<Float>,
    /** How much taller the pill is while the pulse draws a progress line (dp); the card under it does not move. */
    private val lineExtra: State<Float> = mutableFloatStateOf(0f)
) {
    val pulseNow: Float get() = pulse.value
    val centreShiftNow: Float get() = centreShift.value
    val lineExtraNow: Float get() = lineExtra.value
}

/**
 * The pulse drawn over an open card stands in for the cutout under it: every touch that lands on it stays on it, a
 * finger on it holds the pulse's end like on the collapsed pill ([onHold]), and a tap within the slop does what a
 * tap on that cutout does ([onTap]: the tap swap to the pulsing card). The cutout itself is hidden while the pulse
 * covers it ([IslandInteractionState.pulseCoverFactor]).
 *
 * The camera band keeps what it does on the open island: [bandRange] gives its span across the pill (px, null when
 * there is none), a tap on it runs [onBandTap] and a hold [onBandLongPress], neither swaps
 */
internal fun Modifier.pulseTapTarget(
    onHold: (Boolean) -> Unit,
    bandRange: (pillWidthPx: Float) -> ClosedFloatingPointRange<Float>? = { null },
    onBandTap: () -> Unit = {},
    onBandLongPress: () -> Unit = {},
    onTap: () -> Unit
): Modifier = composed {
    val hold = rememberUpdatedState(onHold)
    val band = rememberUpdatedState(bandRange)
    val bandTap = rememberUpdatedState(onBandTap)
    val bandLong = rememberUpdatedState(onBandLongPress)
    val tap = rememberUpdatedState(onTap)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val inBand = band.value(size.width.toFloat())?.contains(down.position.x) == true
            hold.value(true)
            val touch = PulseTouch()
            var longPressed = false
            try {
                if (inBand) {
                    val done = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        trackPulseTouch(down, touch)
                        true
                    }
                    if (done == null) {
                        if (touch.stayed) {
                            longPressed = true
                            bandLong.value()
                        }
                        trackPulseTouch(down, touch)
                    }
                } else {
                    trackPulseTouch(down, touch)
                }
            } finally {
                hold.value(false)
            }
            if (!touch.lifted || !touch.stayed || longPressed) return@awaitEachGesture
            if (inBand) bandTap.value() else tap.value()
        }
    }
}

private class PulseTouch {
    var stayed = true
    var lifted = false
}

/** Follows the finger of [down] to its lift (or its end), noting whether it left the touch slop on the way. */
private suspend fun AwaitPointerEventScope.trackPulseTouch(down: PointerInputChange, touch: PulseTouch) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: return
        change.consume()
        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) touch.stayed = false
        if (!change.pressed) {
            touch.lifted = true
            return
        }
    }
}

/**
 * Pill on the collapsed cutout's spot that plays the pulse over an open card. It replaces the cutout there: that
 * cutout's content fades out as the pulse fades in (and back), and the pill is the cutout for touches: a tap swaps the
 * open island to the pulsing card the way a tap on any cutout does ([onTap] gets the cutout's id and its bounds), the
 * card stays open until then
 */
@Composable
internal fun PulseOverCardPill(
    layout: IslandLayout,
    settings: SmartIslandSettings,
    animSpeed: AnimationSpeed,
    ui: IslandInteractionState,
    modifier: Modifier = Modifier,
    /** The camera band keeps its tap (put the island away) and hold (long-press action) while the island is open. */
    bandActive: Boolean = false,
    isLandscape: Boolean = false,
    onHold: (Boolean) -> Unit = {},
    onBandTap: () -> Unit = {},
    onBandLongPress: () -> Unit = {},
    onTap: (cutoutId: Int, bounds: () -> Rect?) -> Unit = { _, _ -> }
) {
    // the edge layout has no such pill: its own over-card pulse sets the cover then
    val over = layout.overCard ?: return
    SideEffect {
        ui.pulseCoverOf = { id -> if (id == TapSwap.CUTOUT_PILL_SPOT) pulseCoverCurve(over.fade.alphaNow) else 0f }
    }
    DisposableEffect(ui) { onDispose { ui.pulseCoverOf = NoPulseCover } }
    val card = over.fade.card ?: return
    val bounds = remember { arrayOfNulls<Rect>(1) }
    // Same zone the open island's camera-band target uses; the hole keeps its place on the screen while the pill grows
    val closeZone = rememberExpandedCloseZone(settings, isLandscape, layout.cameraBand, layout.collapsedMainOffset.value)
    val density = LocalDensity.current.density
    val cameraShiftDp = PulseWidth.cameraShiftDp(layout.pulseCamera.centreInPillDp, settings.width) - over.centreShiftNow
    val visible by remember(over) { derivedStateOf { over.fade.alphaNow > 0f } }
    if (!visible) return
    val notch = settings.enableNotchMode
    val radius = settings.cornerRadius.dp.coerceAtMost((settings.height / 2f).dp)
    val shape = if (notch) RoundedCornerShape(bottomStart = radius, bottomEnd = radius) else RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .islandAnimatedSize(
                { (settings.width * (1f + PILL_PULSE_WIDTH_FRACTION * over.pulseNow)).dp },
                { (settings.height + over.lineExtraNow).dp }
            )
            .graphicsLayer {
                val grow = settings.width * PILL_PULSE_WIDTH_FRACTION * over.pulseNow
                translationX = (layout.collapsedMainOffset.value + SideCamera.anchoredShiftDp(layout.pulseAnchorSide, grow) +
                    over.centreShiftNow)
                    .dp.toPx()
                translationY = (layout.collapsedTopShift - layout.topShift).toPx()
                // a tap swap's shells sit on the cutout's spot: it hides there like the cutout does
                // the fill and the content fade on their own (below)
                alpha = ui.swapHideAlpha(TapSwap.CUTOUT_PILL_SPOT)
            }
            .onGloballyPositioned { coords -> if (!ui.tapSwapActive) bounds[0] = coords.boundsInWindow() }
            .clip(shape)
            .drawBehind {
                drawRect(layout.pillBackgroundColor.copy(alpha = settings.opacity * pillFillAlpha(over.fade.alphaNow)))
            }
            // Cutout taps sometimes missed during pulses, with the island open or collapsed.
            // For a pulse over an open card, the pulse handles taps across its full width.
            .pulseTapTarget(
                onHold = onHold,
                bandRange = { widthPx ->
                    if (!bandActive) null else {
                        val centre = widthPx / 2f + cameraShiftDp * density
                        val half = closeZone.widthDp * density / 2f
                        (centre - half)..(centre + half)
                    }
                },
                onBandTap = onBandTap,
                onBandLongPress = onBandLongPress
            ) { onTap(TapSwap.CUTOUT_PILL_SPOT) { bounds[0] } }
    ) {
        PulsePeakLayer(
            card = card,
            settings = settings,
            cameraGapDp = layout.pulseCamera.gapDp,
            redacted = layout.peakRedacted,
            animSpeed = animSpeed,
            cameraShiftDp = PulseWidth.cameraShiftDp(layout.pulseCamera.centreInPillDp, settings.width) -
                over.centreShiftNow,
            cameraSide = layout.peakPlacement?.side ?: CameraSide.Centre,
            glyphFromCameraDp = layout.peakPlacement?.glyphFromCameraDp ?: 0f,
            notch = layout.peakPlacement?.notch == true,
            classic = layout.peakPlacement?.classic == true,
            lineExtra = { over.lineExtraNow },
            modifier = Modifier.graphicsLayer { alpha = pillContentAlpha(over.fade.alphaNow) }
        )
    }
}

internal val NoPulseCover: (Int) -> Float = { 0f }

private fun ramp(v: Float, start: Float, end: Float): Float = ((v - start) / (end - start)).coerceIn(0f, 1f)

/**
 * How far the cutout is covered when the pulse's fade is at [fade]: it stays solid while the pulse's pill fills in, and
 * only goes once that pill is mostly there, so the card behind never shows through the 2 crossfading
 */
internal fun pulseCoverCurve(fade: Float): Float {
    val t = ramp(fade, 0.5f, 1f)
    return t * t * (3f - 2f * t)
}

/** The pill's fill comes first (the fade's first half), the glyphs and text follow it. */
private fun pillFillAlpha(fade: Float): Float = ramp(fade, 0f, 0.5f)
private fun pillContentAlpha(fade: Float): Float = ramp(fade, 0.3f, 1f)
