/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.EdgeCameraSpot
import com.agupta07505.smartisland.model.EdgePillLayout
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePulseLength
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PulseBubble
import com.agupta07505.smartisland.model.PulsePeak
import com.agupta07505.smartisland.model.PulseWidth
import com.agupta07505.smartisland.model.SLOT_START_PADDING_DP
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.ui.IslandText as Text

/**
 * The edge pill's pulse: how long the pill gets and the texts' lengths. [fit] is set for a camera in the middle (both
 * ends move), null for a camera toward 1 end ([spot]), whose end of the pill stays and [maxLengthDp] is the room
 */
internal class EdgePulseShape(
    val fit: EdgePulseLength.Fit?,
    val lengthDp: Float,
    val titleLenDp: Float,
    val valueLenDp: Float,
    /** A song's artist, as long as it wants (cut to the room the drawn pill has), zero without one. */
    val artistLenDp: Float,
    val spot: EdgeCameraSpot,
    val glyphFromCameraDp: Float,
    val maxLengthDp: Float,
    /** The split pulse of a notification with the camera in the middle: glyph and message above the hole, title alone below. */
    val split: Boolean = false,
    /** Split only: the message's length, cut to the room [EdgePulseLength.Fit.messageMaxDp] gives it. */
    val messageLenDp: Float = 0f
)

/**
 * Edge pulse spring: the demo's damping of 0.6 (about 9% overshoot, held back by [EdgePulseLength.span]) on the
 * portrait pulse's stiffness
 */
internal const val EDGE_PULSE_DAMPING = 0.6f

/** Measured text lengths are fractions of a pixel short of what the text needs, this keeps it from ellipsizing. */
private const val ALONG_SLACK_DP = 1f

/** Null when nothing pulses. [layout] is the settled pill's, the one the pulse grows from. */
@Composable
internal fun rememberEdgePulseShape(
    card: IslandNotification?,
    layout: EdgePillLayout,
    redacted: Boolean
): EdgePulseShape? {
    if (card == null) return null
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = LocalTextStyle.current
    val peak = PulsePeak.of(card, System.currentTimeMillis())
    val value = peak.value.replace(Regex("\\d"), "8")
    val stateText = statePulseText(peak)
    val artist = if (redacted) "" else peak.artist
    return remember(peak.title, peak.message, artist, value, stateText, peak.valueKind, layout, redacted, style, density) {
        fun widthOf(text: String, weight: FontWeight): Float = measurer.peakTextWidthDp(density, style, text, weight)
        val valueDp = peak.valueWidthDp(stateText, ::widthOf)
        val titleDp = if (redacted) 0f else widthOf(peak.title, FontWeight.SemiBold)
        val artistDp = widthOf(artist, FontWeight.Normal)
        val camera = layout.camera
        val pill = layout.pill
        if (!camera.sideLayout) {
            val bubble = layout.secondary
            val centre = EdgePillLayoutMath.holeCentreYDp(layout)
            val bubbleSpan = bubble?.let { it.height + camera.tuning.bubbleGapDp } ?: 0f
            val bubbleBelow = bubble == null || bubble.top >= pill.top + pill.height / 2f
            if (!peak.hasValue) {
                // A notification: the glyph and message above the hole, the title alone below it (portrait's split pulse)
                val messageDp = if (redacted) 0f else widthOf(peak.sizedMessage, FontWeight.Normal)
                val fit = EdgePulseLength.fitSplit(pill.height, camera.holeLengthDp, messageDp, titleDp,
                    centre, camera.screenHeightDp, bubbleSpanDp = bubbleSpan, bubbleBelow = bubbleBelow,
                    settledUpperDp = centre - pill.top, settledLowerDp = pill.bottom - centre,
                    mediaIconDp = peak.mediaIconDp(redacted))
                EdgePulseShape(fit, fit.lengthDp, titleDp.coerceIn(0f, fit.titleMaxDp), 0f, 0f,
                    EdgeCameraSpot.Middle, SLOT_START_PADDING_DP, 0f, split = true,
                    messageLenDp = messageDp.coerceIn(0f, fit.messageMaxDp))
            } else {
                val fit = EdgePulseLength.fit(pill.height, camera.holeLengthDp, titleDp,
                    PulseWidth.valueSideDp(valueDp, artistDp), centre, camera.screenHeightDp,
                    bubbleSpanDp = bubbleSpan,
                    bubbleBelow = bubbleBelow,
                    settledUpperDp = centre - pill.top,
                    settledLowerDp = pill.bottom - centre)
                EdgePulseShape(fit, fit.lengthDp, titleDp.coerceIn(0f, fit.titleMaxDp),
                    valueDp.coerceIn(0f, fit.valueMaxDp), artistDp, EdgeCameraSpot.Middle, SLOT_START_PADDING_DP, 0f)
            }
        } else {
            val spot = camera.spot
            val glyphFrom = EdgePillLayoutMath.sideZone(layout)?.let {
                if (spot == EdgeCameraSpot.Upper) it.glyphTopDp
                else pill.height - it.glyphTopDp - EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
            } ?: SLOT_START_PADDING_DP
            val bubbleSpan = layout.secondary?.let { it.height + camera.tuning.bubbleGapDp } ?: 0f
            val max = EdgePulseLength.sideMaxDp(spot, pill.top, pill.height, camera.screenHeightDp, bubbleSpan)
            val fit = PulseWidth.fitSide(pill.height, glyphFrom, titleDp, PulseWidth.valueSideDp(valueDp, artistDp), max)
            EdgePulseShape(null, fit.widthDp, titleDp.coerceIn(0f, fit.titleMaxDp), valueDp, artistDp, spot, glyphFrom, max)
        }
    }
}

/**
 * 0..1 while the pill is lengthened, sprung toward 1 and back, the last shape is kept while it runs back. [owns] is
 * true from the pulse starting until the pill is back at its settled length, the one signal that says the grown pill
 * draws the body in place of the shell ([owns] is the caller's when the shell has to read it too). Readers pass it
 * through [EdgePulseOwnership], which drops it while a card is open
 */
internal class EdgePulseGrowth(val shape: EdgePulseShape?, val progress: State<Float>, val owns: State<Boolean>)

@Composable
internal fun rememberEdgePulseGrowth(
    shape: EdgePulseShape?,
    animSpeed: AnimationSpeed,
    owns: MutableState<Boolean>? = null
): EdgePulseGrowth {
    val progress = remember { Animatable(0f) }
    val ownsState = owns ?: remember { mutableStateOf(false) }
    var last by remember { mutableStateOf<EdgePulseShape?>(null) }
    SideEffect {
        if (shape != null) {
            last = shape
            ownsState.value = true
        }
    }
    val on = shape != null
    LaunchedEffect(on) {
        progress.animateTo(
            if (on) 1f else 0f,
            spring(dampingRatio = EDGE_PULSE_DAMPING, stiffness = animSpeed.stiffness(PULSE_PEAK_STIFFNESS))
        )
        if (!on) ownsState.value = false
    }
    DisposableEffect(ownsState) { onDispose { ownsState.value = false } }
    return EdgePulseGrowth(shape ?: last, remember { derivedStateOf { progress.value } }, ownsState)
}

/**
 * The pill as the pulse has grown it now, from [settled] (its ends and bubble); null while the pill is at its settled
 * length. [centreYDp] is the hole's middle ([EdgePillLayoutMath.holeCentreYDp])
 */
internal fun EdgePulseGrowth.grownLayout(settled: EdgePillLayout, centreYDp: Float, hasSecondary: Boolean): EdgePillLayout? {
    val shape = shape ?: return null
    val progress = progress.value
    if (progress <= GROWN_MIN_PROGRESS) return null
    val camera = settled.camera
    val pill = settled.pill
    val span = if (shape.fit != null) {
        EdgePulseLength.span(pill.top, pill.height, shape.fit, centreYDp, progress, camera.screenHeightDp)
    } else {
        EdgePulseLength.sideSpan(shape.spot, pill.top, pill.height, shape.lengthDp, shape.maxLengthDp, progress)
    }
    return EdgePillLayoutMath.compute(camera, pillWidthDp = pill.width, pillHeightDp = span.lengthDp,
        hasSecondary = hasSecondary, topDp = span.topDp)
}

private const val GROWN_MIN_PROGRESS = 0.001f

/**
 * Glyph at the upper end, title and value rotated to run down the strip, the demo's `along` look. Glyph and value are
 * the settled pill's own, only the title is new. Laid out from the surface it sits on like portrait's peak layer, so
 * the texts can't be placed for a pill longer than the one drawn.
 */
@Composable
internal fun EdgePulsePeakLayer(
    card: IslandNotification,
    settings: SmartIslandSettings,
    shape: EdgePulseShape,
    holeFromTopDp: Float,
    holeLengthDp: Float,
    cornerDp: Float,
    cameraOnLeft: Boolean,
    redacted: Boolean,
    animSpeed: AnimationSpeed,
    /** How much thicker the pill is than its settled thickness (dp): the texts and glyph keep their place, the line sits on the new inner side. */
    lineExtraDp: Float = 0f,
    /** Whether the bubble is below the pill's middle, null when there is none. */
    bubbleBelow: Boolean? = null,
    /** Scale the element nearest to the bubble swells to with it. */
    swell: () -> Float = { 1f },
    modifier: Modifier = Modifier
) {
    val peak = remember(card) { PulsePeak.of(card, System.currentTimeMillis()) }
    val stateText = statePulseText(peak)
    val title = peak.title.takeIf { !redacted && it.isNotBlank() }
    val message = peak.message.takeIf { !redacted && it.isNotBlank() }
    val artist = peak.artist.takeIf { !redacted && it.isNotBlank() }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val fullWidthDp = maxWidth.value
        // The content is laid out on the settled thickness, against the screen-edge side
        val pillWidthDp = (fullWidthDp - lineExtraDp).coerceAtLeast(0f)
        val contentLeftDp = if (cameraOnLeft) 0f else (fullWidthDp - pillWidthDp)
        val pillLengthDp = maxHeight.value
        if (shape.split) {
            SplitStripContent(
                card = card, settings = settings, shape = shape, message = message, title = title,
                holeFromTopDp = holeFromTopDp, holeLengthDp = holeLengthDp, pillLengthDp = pillLengthDp,
                pillWidthDp = pillWidthDp, contentLeftDp = contentLeftDp, ring = peak.showsGlyphRing,
                bubbleBelow = bubbleBelow, swell = swell,
                mediaKind = if (redacted) PulsePeak.MediaKind.None else peak.mediaKind
            )
            return@BoxWithConstraints
        }
        // Long device or connection names can overflow. Size the text to the space left on the strip.
        val texts = if (shape.fit != null) {
            EdgePulseLength.middleTexts(pillLengthDp, holeFromTopDp, holeLengthDp, shape.titleLenDp, shape.valueLenDp,
                shape.artistLenDp)
        } else {
            EdgePulseLength.sideTexts(pillLengthDp, shape.glyphFromCameraDp, shape.titleLenDp, shape.valueLenDp,
                shape.artistLenDp)
        }
        val slots = EdgePulseLength.slots(shape.spot, pillLengthDp, shape.glyphFromCameraDp, texts.titleLenDp,
            texts.valueLenDp, texts.artistLenDp)
        val glyphNearest = bubbleBelow?.let {
            PulseBubble.nearestIsGlyphOnStrip(slots.glyphTopDp, slots.valueTopDp, slots.valueLenDp, it)
        }
        val glyphSwell = if (glyphNearest == true) Modifier.swellScale(swell) else Modifier
        val valueSwell = if (glyphNearest == false) Modifier.swellScale(swell) else Modifier
        val glyphW = EdgePillLayoutMath.GLYPH_SLOT_WIDTH_DP
        val glyphH = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = (contentLeftDp + (pillWidthDp - glyphW) / 2f).dp, y = slots.glyphTopDp.dp)
                .size(glyphW.dp, glyphH.dp).then(glyphSwell),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalGlyphRingAlpha provides if (peak.showsGlyphRing) GLYPH_RING_SHOWN else GLYPH_RING_HIDDEN) {
                CollapsedGlyphContent(card.mode, card, settings)
            }
        }
        if (title != null && slots.titleLenDp > 0f) {
            Box(
                Modifier.align(Alignment.TopStart)
                    .offset(x = contentLeftDp.dp, y = slots.titleTopDp.dp)
                    .size(pillWidthDp.dp, (slots.titleLenDp + ALONG_SLACK_DP).dp),
                contentAlignment = Alignment.TopCenter
            ) {
                MorphSwap(title, enabled = card.canMorph(), direction = rememberMorphDirection(card.category)) { shown ->
                    Text(
                        text = shown,
                        modifier = Modifier.alongStrip(),
                        color = Color.White,
                        fontSize = PEAK_TEXT_SP.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (slots.valueLenDp > 0f) {
            Box(
                Modifier.align(Alignment.TopStart)
                    .offset(x = contentLeftDp.dp, y = slots.valueTopDp.dp)
                    .size(pillWidthDp.dp, (slots.valueLenDp + ALONG_SLACK_DP).dp).then(valueSwell),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(Modifier.valueAlongStrip(card, settings), contentAlignment = Alignment.Center) {
                    PeakValue(card, peak, stateText, settings)
                }
            }
        }
        if (artist != null && slots.artistLenDp > 0f) {
            StripScrollText(artist, FontWeight.Normal, contentLeftDp, slots.artistTopDp, slots.artistLenDp, pillWidthDp)
        }
        peak.progress?.let { target ->
            EdgePulseProgressLine(
                target = target,
                color = pulseAccent(card, settings),
                lengthDp = pillLengthDp,
                cornerDp = cornerDp,
                onInnerRight = cameraOnLeft,
                pillWidthDp = fullWidthDp,
                animSpeed = animSpeed
            )
        }
    }
}

/** Progress line along the pill's inner long side, filling from the upper end. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.EdgePulseProgressLine(
    target: Float,
    color: Color,
    lengthDp: Float,
    cornerDp: Float,
    onInnerRight: Boolean,
    pillWidthDp: Float,
    animSpeed: AnimationSpeed
) {
    val shown = rememberPulseProgress(target, animSpeed)
    val inset = EdgePulseLength.lineInsetDp(lengthDp, pillWidthDp, cornerDp, PROGRESS_BOTTOM_DP)
    val x = if (onInnerRight) pillWidthDp - PROGRESS_BOTTOM_DP - PROGRESS_HEIGHT_DP else PROGRESS_BOTTOM_DP
    val trackDp = (lengthDp - 2f * inset).coerceAtLeast(0f)
    Box(
        Modifier.align(Alignment.TopStart)
            .offset(x = x.dp, y = inset.dp)
            .size(PROGRESS_HEIGHT_DP.dp, trackDp.dp)
            .clip(RoundedCornerShape(PROGRESS_HEIGHT_DP.dp / 2))
            .background(Color.White.copy(alpha = PROGRESS_TRACK_ALPHA))
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height((trackDp * shown.value.coerceIn(0f, 1f)).dp)
                .background(color)
        )
    }
}

/**
 * The split pulse on the strip (a notification, camera in the middle, notch look or not): the glyph and the message
 * above the hole, the title alone below it, all along the strip from the top down. Laid out from the pill as it is drawn now,
 * so no text sits past the end the spring has reached. A text that does not fit its room scrolls, as the portrait
 * pulse's does. Nothing of the settled pill's dot is drawn here, it stays on the settled pill
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SplitStripContent(
    card: IslandNotification,
    settings: SmartIslandSettings,
    shape: EdgePulseShape,
    message: String?,
    title: String?,
    holeFromTopDp: Float,
    holeLengthDp: Float,
    pillLengthDp: Float,
    pillWidthDp: Float,
    contentLeftDp: Float,
    ring: Boolean,
    bubbleBelow: Boolean?,
    swell: () -> Float,
    mediaKind: PulsePeak.MediaKind = PulsePeak.MediaKind.None
) {
    val slots = EdgePulseLength.splitSlots(
        pillLengthDp, holeFromTopDp, holeLengthDp,
        messageDp = if (message != null) shape.messageLenDp else 0f,
        titleDp = if (title != null) shape.titleLenDp else 0f,
        mediaIconDp = if (mediaKind.showsIcon) PulseWidth.MEDIA_ICON_DP else 0f
    )
    // The bubble rides out at the end on its side: above the pill the glyph (the topmost item) is nearest, below it the
    // title, or the message when there is no title
    val glyphSwell = if (bubbleBelow == false) Modifier.swellScale(swell) else Modifier
    val titleSwell = if (bubbleBelow == true && title != null) Modifier.swellScale(swell) else Modifier
    val messageSwell = if (bubbleBelow == true && title == null) Modifier.swellScale(swell) else Modifier
    val glyphW = EdgePillLayoutMath.GLYPH_SLOT_WIDTH_DP
    val glyphH = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
    if (mediaKind.showsIcon) {
        // Turned along the strip like the text, a quarter clockwise; it fades in when the picture arrives late
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = contentLeftDp.dp, y = slots.iconTopDp.dp)
                .size(pillWidthDp.dp, PulseWidth.MEDIA_ICON_SIZE_DP.dp)
                .then(messageSwell),
            contentAlignment = Alignment.Center
        ) {
            PeakMediaIcon(mediaKind, Modifier.rotate(90f))
        }
    }
    if (message != null && slots.messageLenDp > 0f) {
        StripScrollText(message, FontWeight.Normal, contentLeftDp, slots.messageTopDp, slots.messageLenDp,
            pillWidthDp, messageSwell)
    }
    Box(
        Modifier.align(Alignment.TopStart)
            .offset(x = (contentLeftDp + (pillWidthDp - glyphW) / 2f).dp, y = slots.glyphTopDp.dp)
            .size(glyphW.dp, glyphH.dp)
            .alpha(EdgePulseLength.splitGlyphAlpha(holeFromTopDp, holeLengthDp))
            .then(glyphSwell),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalGlyphRingAlpha provides if (ring) GLYPH_RING_SHOWN else GLYPH_RING_HIDDEN) {
            CollapsedGlyphContent(card.mode, card, settings)
        }
    }
    if (title != null && slots.titleLenDp > 0f) {
        StripScrollText(title, FontWeight.SemiBold, contentLeftDp, slots.titleTopDp, slots.titleLenDp, pillWidthDp,
            titleSwell)
    }
}

/** [text] turned along the strip in a slot [lenDp] long from [topDp]; longer than the slot, it scrolls instead of ending in an ellipsis. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.StripScrollText(
    text: String,
    weight: FontWeight,
    leftDp: Float,
    topDp: Float,
    lenDp: Float,
    pillWidthDp: Float,
    modifier: Modifier = Modifier
) {
    Box(
        Modifier.align(Alignment.TopStart)
            .offset(x = leftDp.dp, y = topDp.dp)
            .size(pillWidthDp.dp, (lenDp + ALONG_SLACK_DP).dp)
            .then(modifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            modifier = Modifier.alongStrip().pulseScrollOnce(text),
            color = Color.White,
            fontSize = PEAK_TEXT_SP.sp,
            fontWeight = weight,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Lays [this] out sideways, turned a quarter clockwise so text reads from the top of the strip down. */
internal fun Modifier.alongStrip(): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(
        Constraints(
            minWidth = 0,
            maxWidth = constraints.maxHeight,
            minHeight = 0,
            maxHeight = constraints.maxWidth
        )
    )
    layout(p.height, p.width) {
        p.placeWithLayer((p.height - p.width) / 2, (p.width - p.height) / 2) { rotationZ = 90f }
    }
}
