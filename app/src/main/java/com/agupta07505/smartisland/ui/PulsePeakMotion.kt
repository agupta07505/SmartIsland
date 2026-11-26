/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.LandscapeClassicPulse
import com.agupta07505.smartisland.model.PulsePeak
import com.agupta07505.smartisland.model.PulseWidth
import com.agupta07505.smartisland.model.SLOT_PAIR_GAP_DP
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** About 1.5% overshoot, as the demo's pulse spring. */
internal const val PULSE_PEAK_DAMPING = 0.8f
internal const val PULSE_PEAK_STIFFNESS = 130f
internal const val PEAK_TEXT_SP = 11f
internal const val PEAK_BARS_DP = 14f
internal const val PEAK_ICON_DP = 20f
internal const val PEAK_READING_LEAD_DP = 15f
internal const val PEAK_TEXT_SLACK_DP = 1f

internal fun TextMeasurer.peakTextWidthDp(density: Density, style: TextStyle, text: String, weight: FontWeight): Float =
    if (text.isEmpty()) 0f else with(density) {
        measure(text, style.copy(fontSize = PEAK_TEXT_SP.sp, fontWeight = weight), softWrap = false, maxLines = 1)
            .size.width.toDp().value
    }

/** Width of the pulse's value; digits count as the widest one so a changing value does not move the edge. */
/** Room the icon in front of the message takes, zero without a picture or when the lock screen hides the content. */
internal fun PulsePeak.Peak.mediaIconDp(redacted: Boolean): Float =
    if (redacted || !mediaKind.showsIcon) 0f else PulseWidth.MEDIA_ICON_DP

internal fun PulsePeak.Peak.valueWidthDp(stateText: String = "", textWidthDp: (String, FontWeight) -> Float): Float = when (valueKind) {
    PulsePeak.ValueKind.Text -> textWidthDp(value.replace(Regex("\\d"), "8"), FontWeight.Bold)
    PulsePeak.ValueKind.State -> textWidthDp(stateText, FontWeight.Bold)
    PulsePeak.ValueKind.Clock -> textWidthDp("00:00", FontWeight.Bold)
    PulsePeak.ValueKind.Bars -> PEAK_BARS_DP
    PulsePeak.ValueKind.Icon -> PEAK_ICON_DP
    PulsePeak.ValueKind.Reading -> PEAK_READING_LEAD_DP + textWidthDp(value.replace(Regex("\\d"), "8"), FontWeight.Bold)
    PulsePeak.ValueKind.None -> 0f
}

/**
 * Size the wide pulse of [card] needs (width, and where a split pulse moves the pill's centre), null when nothing pulses. The title and value are measured the way the peak
 * layout draws them. A centre camera widens both sides of the hole; a side camera grows away from it ([side],
 * [glyphFromCameraDp], and [max] as [PulseWidth.maxWidth] gives it; a centre camera's 2 sides have rooms of their own)
 */
@Composable
internal fun rememberPulsePeakFit(
    card: IslandNotification?,
    baseWidthDp: Float,
    cameraGapDp: Float,
    max: PulseWidth.MaxWidth,
    /** Centre camera: how far the hole lies right of the pill's centre ([PulseWidth.cameraShiftDp]). */
    cameraShiftDp: Float = 0f,
    side: CameraSide = CameraSide.Centre,
    glyphFromCameraDp: Float = 0f,
    /** Lock screen hides the content: the layer draws no title, so none is measured. */
    redacted: Boolean = false,
    /** Landscape classic: no hole, the settled pill's own gap between glyph and value is kept ([PulseWidth.fitClassic]). */
    classic: Boolean = false
): PulseWidth.Fit? {
    if (card == null) return null
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = LocalTextStyle.current
    val peak = PulsePeak.of(card, System.currentTimeMillis())
    // digits are drawn as the widest one so a changing value does not move the edge
    val value = peak.value.replace(Regex("\\d"), "8")
    val stateText = statePulseText(peak)
    val artist = if (redacted) "" else peak.artist
    return remember(peak.title, peak.message, artist, value, stateText, peak.valueKind, baseWidthDp, cameraGapDp, max, cameraShiftDp, side,
        glyphFromCameraDp, redacted, classic, style, density) {
        fun widthOf(text: String, weight: FontWeight): Float = measurer.peakTextWidthDp(density, style, text, weight)
        val valueDp = peak.valueWidthDp(stateText, ::widthOf)
        // A text measured a fraction of a pixel short would scroll although the pill is wide enough for it
        fun textOf(text: String, weight: FontWeight): Float =
            widthOf(text, weight).let { if (it > 0f) it + PEAK_TEXT_SLACK_DP else 0f }
        val titleDp = if (redacted) 0f else textOf(peak.title, FontWeight.SemiBold)
        val sideMessageDp = if (redacted || peak.hasValue) 0f else textOf(peak.sizedMessage, FontWeight.Normal)
        if (classic) {
            val gap = LandscapeClassicPulse.centreGapDp(baseWidthDp, valueDp)
            // The gap stays the settled pill's own; the artist joins the visualiser at the far end
            PulseWidth.fitClassic(baseWidthDp, gap, titleDp,
                PulseWidth.valueSideDp(valueDp, textOf(artist, FontWeight.Normal)), max.widthDp,
                sideMessageDp, peak.mediaIconDp(redacted))
        } else if (side == CameraSide.Centre && !peak.hasValue) {
            val messageDp = if (redacted) 0f else textOf(peak.sizedMessage, FontWeight.Normal)
            // Read once with the rest: a picture that arrives after the pulse started fades in inside the fit
            // already made, so the pill keeps its size
            PulseWidth.fitSplit(baseWidthDp, cameraGapDp, messageDp, titleDp, max, cameraShiftDp,
                mediaIconDp = peak.mediaIconDp(redacted))
        } else if (side == CameraSide.Centre) {
            val artistDp = textOf(artist, FontWeight.Normal)
            PulseWidth.fitPerSide(baseWidthDp, cameraGapDp, titleDp,
                PulseWidth.valueSideDp(valueDp, artistDp, SLOT_PAIR_GAP_DP), max, cameraShiftDp)
        } else {
            PulseWidth.fitSide(baseWidthDp, glyphFromCameraDp, titleDp,
                PulseWidth.valueSideDp(valueDp, textOf(artist, FontWeight.Normal)), max.widthDp,
                sideMessageDp, peak.mediaIconDp(redacted))
        }
    }
}

/** The wide pulse as the shell follows it: the arrival-pulse scale that widens the pill, and where its centre has moved to (dp). */
internal class PeakPulse(val pulse: State<Float>, val centreShiftDp: State<Float>)

/**
 * Pulse value the layout widens the pill with while the wide pulse plays: the same 0..1 scale as the arrival pulse,
 * so the content slots and the shell follow without knowing the difference. A split pulse's centre shift runs on the
 * same spring, so the 2 sides of the pill reach their ends together. A fit that changes after the pill is out (a
 * song's artist arriving late) moves the pill to its new size on that spring instead of jumping there
 */
@Composable
internal fun rememberPeakPulse(peak: PulseWidth.Fit?, baseWidthDp: Float, animSpeed: AnimationSpeed): PeakPulse {
    val progress = remember { Animatable(0f) }
    val width = remember { Animatable(baseWidthDp) }
    val shift = remember { Animatable(0f) }
    val on = peak != null
    LaunchedEffect(on) {
        progress.animateTo(
            if (on) 1f else 0f,
            spring(dampingRatio = PULSE_PEAK_DAMPING, stiffness = animSpeed.stiffness(PULSE_PEAK_STIFFNESS))
        )
    }
    LaunchedEffect(peak?.widthDp, peak?.centreShiftDp) {
        val fit = peak ?: return@LaunchedEffect
        if (PulseWidth.followsFitChange(progress.value)) {
            val spec = spring<Float>(
                dampingRatio = PULSE_PEAK_DAMPING, stiffness = animSpeed.stiffness(PULSE_PEAK_STIFFNESS)
            )
            coroutineScope {
                launch { width.animateTo(fit.widthDp, spec) }
                launch { shift.animateTo(fit.centreShiftDp, spec) }
            }
        } else {
            width.snapTo(fit.widthDp)
            shift.snapTo(fit.centreShiftDp)
        }
    }
    val pulse = remember(baseWidthDp) {
        derivedStateOf {
            PulseWidth.equivalentPulse(width.value - baseWidthDp, baseWidthDp, PILL_PULSE_WIDTH_FRACTION, progress.value)
        }
    }
    val centreShift = remember { derivedStateOf { shift.value * progress.value } }
    return remember(pulse, centreShift) { PeakPulse(pulse, centreShift) }
}

internal const val PEAK_FADE_IN_MS = 120
internal const val PEAK_FADE_OUT_MS = 220

/** The pulse over an open card swaps with a cutout that is solid beneath it: slower than the collapsed pill's fade. */
internal const val OVER_CARD_FADE_IN_MS = 200
internal const val OVER_CARD_FADE_OUT_MS = 280
internal const val GLYPH_RING_FADE_IN_MS = 200

/**
 * How much taller (dp) the pill is while its wide pulse draws a progress line: the line's height, grown in with the
 * pulse and eased back as it settles. [card] is the pulse that runs now, null when none does
 */
@Composable
internal fun rememberPulseLineExtra(card: IslandNotification?, animSpeed: AnimationSpeed): State<Float> {
    val extra = remember(card) { PulsePeak.extraHeightDp(card, System.currentTimeMillis()) }
    return animateFloatAsState(
        targetValue = extra,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = animSpeed.stiffness(PULSE_PEAK_STIFFNESS)
        ),
        label = "pulseLineExtra"
    )
}

/**
 * Alpha of the ring on the settled glyph: gone from the frame a pulse with a progress line starts, back only once the
 * pulse has faded out, then it fades in. [hidden] is whether such a pulse runs
 */
@Composable
internal fun rememberGlyphRingAlpha(hidden: Boolean, animSpeed: AnimationSpeed): () -> Float {
    val shown = remember { Animatable(1f) }
    LaunchedEffect(hidden) {
        if (hidden) {
            shown.snapTo(0f)
        } else {
            delay(animSpeed.durationMs(PEAK_FADE_OUT_MS).toLong())
            shown.animateTo(1f, tween(animSpeed.durationMs(GLYPH_RING_FADE_IN_MS), easing = FastOutSlowInEasing))
        }
    }
    return remember(hidden) { { if (hidden) 0f else shown.value } }
}

/** The card the wide pulse draws and how visible that layer is; the card is kept while the layer fades out. */
internal class PulsePeakFade(
    val card: IslandNotification?,
    private val alpha: State<Float>,
    private val keepsSettled: Boolean = false
) {
    val alphaNow: Float get() = alpha.value

    /** The settled content crossfades with the pulse, unless the pulse has nothing of its own to show. */
    val settledAlphaNow: Float get() = if (keepsSettled) 1f else 1f - alphaNow

    val drawsPeak: Boolean get() = !keepsSettled && alphaNow > 0f
}

@Composable
internal fun rememberPulsePeakFade(
    card: IslandNotification?,
    animSpeed: AnimationSpeed,
    redacted: Boolean = false,
    fadeInMs: Int = PEAK_FADE_IN_MS,
    fadeOutMs: Int = PEAK_FADE_OUT_MS
): PulsePeakFade {
    var last by remember { mutableStateOf<IslandNotification?>(null) }
    SideEffect { if (card != null) last = card }
    val on = card != null
    val alpha = animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(
            durationMillis = animSpeed.durationMs(if (on) fadeInMs else fadeOutMs),
            easing = FastOutSlowInEasing
        ),
        label = "pulsePeakFade"
    )
    val shown = card ?: last
    return PulsePeakFade(shown, alpha, PulsePeak.keepsSettledLook(shown, redacted))
}
