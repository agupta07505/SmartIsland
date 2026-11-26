/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.LandscapeClassicPulse
import com.agupta07505.smartisland.model.PulseBubble
import com.agupta07505.smartisland.model.PulsePeak
import com.agupta07505.smartisland.model.PulseWidth
import com.agupta07505.smartisland.model.SLOT_END_PADDING_DP
import com.agupta07505.smartisland.model.SLOT_GLYPH_SIZE_DP
import com.agupta07505.smartisland.model.SLOT_PAIR_GAP_DP
import com.agupta07505.smartisland.model.SLOT_START_PADDING_DP
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
import com.agupta07505.smartisland.ui.expanded.cardAccentColor
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.delay
import com.agupta07505.smartisland.ui.IslandText as Text

/**
 * Wide pulse. A centre camera has glyph and title left of the hole and the value right of it (a song's artist leads
 * the visualiser there; a notification: the glyph and the message left of the hole, the title alone on its right); a side camera keeps all
 * three on its roomy side, the glyph next to the hole and the value at the far end. The glyph and the value are the
 * settled pill's own, so colours and fitting stay the same; only the title is new
 */
@Composable
internal fun PulsePeakLayer(
    card: IslandNotification,
    settings: SmartIslandSettings,
    cameraGapDp: Float,
    redacted: Boolean,
    animSpeed: AnimationSpeed,
    /** Centre camera: how far the hole lies right of the pill's centre ([PulseWidth.cameraShiftDp]). */
    cameraShiftDp: Float = 0f,
    cameraSide: CameraSide = CameraSide.Centre,
    /** Side camera: where the glyph starts from the camera-side edge ([SideCamera.glyphStartFromCameraDp]). */
    glyphFromCameraDp: Float = 0f,
    /** Notch mode: flat top, bottom corners with the pill's own radius. */
    notch: Boolean = false,
    /** Landscape classic: the gap before the value is the settled pill's own ([LandscapeClassicPulse.centreGapDp]). */
    classic: Boolean = false,
    /** Side of the pill the secondary bubble is on, null when there is none. */
    bubbleOnLeft: Boolean? = null,
    /** Scale the element nearest to the bubble swells to with it. */
    swell: () -> Float = { 1f },
    /** How much taller the pill is than its settled height (dp), the content keeps its place and the line sits at the new bottom. */
    lineExtra: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val peak = remember(card) { PulsePeak.of(card, System.currentTimeMillis()) }
    val title = peak.title.takeIf { !redacted && it.isNotBlank() }
    val message = peak.message.takeIf { !redacted && it.isNotBlank() }
    val artist = peak.artist.takeIf { !redacted && it.isNotBlank() }
    val hasValue = peak.hasValue
    // an opposite event taking over the pulse changes the title in place instead of cutting it
    val titleMorphs = card.canMorph()
    val titleDirection = rememberMorphDirection(card.category)
    val keepsSettled = PulsePeak.keepsSettledLook(card, redacted)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = LocalTextStyle.current
    val stateText = statePulseText(peak)
    val valueDp = remember(peak, stateText, style, density) {
        peak.valueWidthDp(stateText) { text, weight -> measurer.peakTextWidthDp(density, style, text, weight) }
    }
    val classicGapDp = if (classic) LandscapeClassicPulse.centreGapDp(settings.width, valueDp) else 0f
    val sideIconDp = peak.mediaIconDp(redacted)
    val sideMessage = !hasValue && (message != null || sideIconDp > 0f)
    val sideTitleDp = remember(title, style, density) {
        title?.let { measurer.peakTextWidthDp(density, style, it, FontWeight.SemiBold) + PEAK_TEXT_SLACK_DP } ?: 0f
    }
    val sideMessageDp = remember(message, style, density) {
        message?.let { measurer.peakTextWidthDp(density, style, it.take(PulsePeak.MESSAGE_MAX_CHARS), FontWeight.Normal) + PEAK_TEXT_SLACK_DP } ?: 0f
    }
    val glyphNearest = bubbleOnLeft?.let { !hasValue || PulseBubble.nearestIsGlyph(it, cameraSide) }
    val glyphSwell = if (glyphNearest == true) Modifier.swellScale(swell) else Modifier
    val valueSwell = if (glyphNearest == false) Modifier.swellScale(swell) else Modifier
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthDp = maxWidth.value
        Box(
            Modifier.fillMaxSize().graphicsLayer { translationY = -lineExtra() * this.density / 2f }
        ) {
            when (cameraSide) {
                CameraSide.Centre -> if (!hasValue && !keepsSettled) {
                    val rooms = PulseWidth.rooms(widthDp, cameraGapDp, cameraShiftDp)
                    val iconDp = peak.mediaIconDp(redacted)
                    val messageMaxDp = PulseWidth.splitMessageMaxDp(rooms.leftDp, iconDp)
                    val titleMaxDp = (rooms.rightDp - PulseWidth.splitRightFixedDp(title != null)).coerceAtLeast(0f)
                    // The glyph leads the message on the left; the pill is still narrow when the pulse starts, so what
                    // the room has not cleared of the hole stays hidden instead of sliding under the camera
                    val glyphAlpha = PulseWidth.revealAlpha(rooms.leftDp, SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP)
                    // The bubble's side decides what swells: on the left the glyph is nearest, on the right the title
                    // (the message when there is none)
                    val splitGlyphSwell = if (bubbleOnLeft == true) Modifier.swellScale(swell) else Modifier
                    val titleSwell = if (bubbleOnLeft == false && title != null) Modifier.swellScale(swell) else Modifier
                    val messageSwell =
                        if (bubbleOnLeft == false && title == null) Modifier.swellScale(swell) else Modifier
                    Row(
                        Modifier.align(Alignment.CenterStart).padding(start = SLOT_START_PADDING_DP.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PeakGlyph(card, settings, peak.showsGlyphRing, splitGlyphSwell.alpha(glyphAlpha))
                        if (message != null || iconDp > 0f) {
                            Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
                            Row(messageSwell, verticalAlignment = Alignment.CenterVertically) {
                                if (iconDp > 0f) {
                                    PeakMediaIcon(peak.mediaKind)
                                    Spacer(Modifier.width(PulseWidth.MEDIA_ICON_GAP_DP.dp))
                                }
                                message?.let { PeakText(it, FontWeight.Normal, Modifier.widthIn(max = messageMaxDp.dp)) }
                            }
                        }
                    }
                    if (title != null) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart)
                                .padding(start = (rooms.leftDp + cameraGapDp + SLOT_PAIR_GAP_DP).dp).then(titleSwell),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PeakText(title, FontWeight.SemiBold, Modifier.widthIn(max = titleMaxDp.dp), morphs = titleMorphs, direction = titleDirection)
                        }
                    }
                } else {
                    val rooms = PulseWidth.rooms(widthDp, cameraGapDp, cameraShiftDp)
                    val titleMaxDp =
                        (rooms.leftDp - SLOT_START_PADDING_DP - SLOT_GLYPH_SIZE_DP - SLOT_PAIR_GAP_DP).coerceAtLeast(0f)
                    val valueMaxDp = (rooms.rightDp - SLOT_END_PADDING_DP).coerceAtLeast(0f)
                    // The pill is still narrow when the pulse starts and the hole can lie off its middle: what the room
                    // has not cleared of the hole stays hidden instead of sliding under the camera
                    val glyphAlpha = PulseWidth.revealAlpha(rooms.leftDp, SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP)
                    val valueAlpha = PulseWidth.revealAlpha(valueMaxDp, valueDp)
                    // A song's artist sits between the hole and the visualiser and takes what the room leaves after
                    // the gaps and the bars; the group fades in as 1 and the artist is clipped to the room while
                    // the pill grows, so none of it slides under the hole
                    val artistMaxDp = PulseWidth.artistMaxDp(valueMaxDp, valueDp, SLOT_PAIR_GAP_DP)
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = SLOT_START_PADDING_DP.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PeakGlyph(card, settings, peak.showsGlyphRing, glyphSwell.alpha(glyphAlpha))
                        if (title != null) {
                            Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
                            PeakText(title, FontWeight.SemiBold, Modifier.widthIn(max = titleMaxDp.dp), morphs = titleMorphs, direction = titleDirection)
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = SLOT_END_PADDING_DP.dp)
                            .widthIn(max = valueMaxDp.dp).alpha(valueAlpha),
                        contentAlignment = Alignment.Center
                    ) {
                        PeakValueGroup(card, peak, stateText, settings, artist, artistMaxDp, artistFirst = true, valueSwell)
                    }
                }
                CameraSide.Left -> {
                    val gapDp = if (classic) classicGapDp else SideCamera.PAIR_GAP_DP
                    val artistMaxDp = PulseWidth.sideArtistMaxDp(widthDp, glyphFromCameraDp, title != null, valueDp, gapDp)
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth()
                            .padding(start = glyphFromCameraDp.dp, end = SLOT_END_PADDING_DP.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PeakGlyph(card, settings, peak.showsGlyphRing, glyphSwell)
                        if (sideMessage) {
                            Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
                            val rooms = PulseWidth.sideTextRooms(
                                widthDp - glyphFromCameraDp - SLOT_GLYPH_SIZE_DP - SLOT_PAIR_GAP_DP - SLOT_END_PADDING_DP,
                                sideTitleDp, PulseWidth.messageBlockDp(sideMessageDp, sideIconDp)
                            )
                            PeakSideTexts(title, message, peak.mediaKind, sideIconDp, rooms, mirrored = false, Modifier.weight(1f))
                        } else {
                            if (title != null) Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
                            Box(Modifier.weight(1f)) { title?.let { PeakText(it, FontWeight.SemiBold, morphs = titleMorphs, direction = titleDirection) } }
                        }
                        if (hasValue) {
                            Spacer(Modifier.width(gapDp.dp))
                            PeakValueGroup(card, peak, stateText, settings, artist, artistMaxDp, artistFirst = true, valueSwell)
                        }
                    }
                }
                CameraSide.Right -> {
                    val gapDp = if (classic) classicGapDp else SideCamera.PAIR_GAP_DP
                    val artistMaxDp = PulseWidth.sideArtistMaxDp(widthDp, glyphFromCameraDp, title != null, valueDp, gapDp)
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth()
                            .padding(start = SLOT_END_PADDING_DP.dp, end = glyphFromCameraDp.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasValue) {
                            PeakValueGroup(card, peak, stateText, settings, artist, artistMaxDp, artistFirst = false, valueSwell)
                            Spacer(Modifier.width(gapDp.dp))
                        }
                        if (sideMessage) {
                            val rooms = PulseWidth.sideTextRooms(
                                widthDp - glyphFromCameraDp - SLOT_GLYPH_SIZE_DP - SLOT_PAIR_GAP_DP - SLOT_END_PADDING_DP,
                                sideTitleDp, PulseWidth.messageBlockDp(sideMessageDp, sideIconDp)
                            )
                            PeakSideTexts(title, message, peak.mediaKind, sideIconDp, rooms, mirrored = true, Modifier.weight(1f))
                            Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
                        } else {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                title?.let { PeakText(it, FontWeight.SemiBold, morphs = titleMorphs, direction = titleDirection) }
                            }
                            if (title != null) Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
                        }
                        PeakGlyph(card, settings, peak.showsGlyphRing, glyphSwell)
                    }
                }
            }
        }
        // The notch used to cover the middle of the line. Draw it at the bottom of the taller pulse.
        peak.progress?.let { target ->
            PulseProgressLine(
                target = target,
                color = pulseAccent(card, settings),
                insetDp = PulseWidth.lineInsetDp(
                    settings.height, settings.cornerRadius, notch, PROGRESS_BOTTOM_DP, PROGRESS_INSET_MIN_DP
                ),
                animSpeed = animSpeed,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

/**
 * Thin line along the bottom edge: it fills in shortly after the pulse starts, then follows the value. Only events
 * with real progress get one ([PulsePeak.hasProgressLine]); it belongs to the pulse and is gone from the settled pill
 */
@Composable
private fun PulseProgressLine(
    target: Float,
    color: Color,
    insetDp: Float,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier
) {
    val shown = rememberPulseProgress(target, animSpeed)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = insetDp.dp, end = insetDp.dp, bottom = PROGRESS_BOTTOM_DP.dp)
            .height(PROGRESS_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(PROGRESS_HEIGHT_DP.dp / 2))
            .background(Color.White.copy(alpha = PROGRESS_TRACK_ALPHA))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(shown.value.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

/** The line's fill: it starts shortly after the pulse does, then follows [target]. */
@Composable
internal fun rememberPulseProgress(target: Float, animSpeed: AnimationSpeed): State<Float> {
    val shown = remember { Animatable(0f) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (!started) {
            delay(animSpeed.durationMs(PROGRESS_FILL_DELAY_MS).toLong())
            started = true
            shown.animateTo(target, tween(animSpeed.durationMs(PROGRESS_FILL_MS), easing = PROGRESS_FILL_EASING))
        } else {
            shown.animateTo(target, tween(animSpeed.durationMs(PROGRESS_FOLLOW_MS), easing = LinearEasing))
        }
    }
    return shown.asState()
}

/** The colour the card's glyph and expanded card use, so the line matches the event. */
internal fun pulseAccent(card: IslandNotification, settings: SmartIslandSettings): Color =
    settings.eventColorFor(card)?.let { Color(it) } ?: when (card.mode) {
    IslandMode.Battery -> batteryVisualColor(
        resolveBatteryVisual(card.category, card.title, card.text.replace("%", "").trim().toFloatOrNull() ?: 50f),
        Color(settings.batteryColor)
    )
    IslandMode.Music -> Color(settings.musicVisualizerColor)
    else -> cardAccentColor(card, settings)
}

internal const val PROGRESS_HEIGHT_DP = PulsePeak.LINE_HEIGHT_DP
internal const val PROGRESS_BOTTOM_DP = PulsePeak.LINE_EDGE_GAP_DP
private const val PROGRESS_INSET_MIN_DP = 14f
internal const val PROGRESS_TRACK_ALPHA = 0.14f
private const val PROGRESS_FILL_DELAY_MS = 300
private const val PROGRESS_FILL_MS = 800
private const val PROGRESS_FOLLOW_MS = 500
private val PROGRESS_FILL_EASING = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
private fun PeakGlyph(card: IslandNotification, settings: SmartIslandSettings, ring: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalGlyphRingAlpha provides if (ring) GLYPH_RING_SHOWN else GLYPH_RING_HIDDEN) {
            GlyphSlot(card.mode) { CollapsedGlyphContent(card.mode, card, settings) }
        }
    }
}

internal fun Modifier.swellScale(scale: () -> Float): Modifier = graphicsLayer {
    val s = scale()
    scaleX = s
    scaleY = s
}

/**
 * The value and, for a song, its artist beside the visualiser: the artist first and the bars at the far end of the
 * pulse, or the other way round on the right of the screen ([artistFirst] false). The artist keeps its own room
 * ([artistMaxDp]) and scrolls inside it. The bubble's swell ([swell]) takes the visualiser, the element at the far end
 * and so the nearest to the bubble; scaled with it the artist would grow into the hole and past the pill's end, as
 * the room is sized for it at its own size
 */
@Composable
private fun PeakValueGroup(
    card: IslandNotification,
    peak: PulsePeak.Peak,
    stateText: String,
    settings: SmartIslandSettings,
    artist: String?,
    artistMaxDp: Float,
    artistFirst: Boolean,
    swell: Modifier
) {
    val shown = artist != null && artistMaxDp > 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (shown && artistFirst) {
            PeakText(artist!!, FontWeight.Normal, Modifier.widthIn(max = artistMaxDp.dp))
            Spacer(Modifier.width(PulseWidth.ARTIST_GAP_DP.dp))
        }
        Box(swell) { PeakValue(card, peak, stateText, settings) }
        if (shown && !artistFirst) {
            Spacer(Modifier.width(PulseWidth.ARTIST_GAP_DP.dp))
            PeakText(artist!!, FontWeight.Normal, Modifier.widthIn(max = artistMaxDp.dp))
        }
    }
}

/**
 * A notification's title and message on the one side of a side camera's pill: the title first and the message after
 * it, or the other way round when [mirrored] (the right layout). Each keeps its room from [PulseWidth.sideTextRooms]
 * and scrolls when it does not fit; a picture or a sticker leads the message with its icon
 */
@Composable
private fun PeakSideTexts(
    title: String?,
    message: String?,
    mediaKind: PulsePeak.MediaKind,
    iconDp: Float,
    rooms: PulseWidth.TextRooms,
    mirrored: Boolean,
    modifier: Modifier = Modifier
) {
    val titleContent: @Composable () -> Unit = {
        if (title != null) PeakText(title, FontWeight.SemiBold, Modifier.widthIn(max = rooms.titleDp.dp))
    }
    val messageContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconDp > 0f) {
                PeakMediaIcon(mediaKind)
                Spacer(Modifier.width(PulseWidth.MEDIA_ICON_GAP_DP.dp))
            }
            message?.let {
                PeakText(it, FontWeight.Normal, Modifier.widthIn(max = (rooms.messageDp - iconDp).coerceAtLeast(0f).dp))
            }
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = if (mirrored) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mirrored) {
            messageContent()
            if (title != null) Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
            titleContent()
        } else {
            titleContent()
            if (title != null) Spacer(Modifier.width(SLOT_PAIR_GAP_DP.dp))
            messageContent()
        }
    }
}

@Composable
private fun PeakText(
    text: String,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    morphs: Boolean = false,
    direction: Int = 0
) {
    MorphSwap(text, enabled = morphs, direction = direction) { shown ->
        Text(
            text = shown,
            modifier = modifier.pulseScrollOnce(shown),
            color = Color.White,
            fontSize = PEAK_TEXT_SP.sp,
            fontWeight = weight,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Icon in front of a notification's message for a photo. A picture that arrives after the pulse started
 * fades in; the pill keeps the size it had. [PulseWidth.MEDIA_ICON_SIZE_DP] square, white like the texts
 */
@Composable
internal fun PeakMediaIcon(kind: PulsePeak.MediaKind, modifier: Modifier = Modifier) {
    if (!kind.showsIcon) return
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(MEDIA_ICON_FADE_MS)) }
    Icon(
        imageVector = Icons.Rounded.Image,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier.size(PulseWidth.MEDIA_ICON_SIZE_DP.dp).graphicsLayer { this.alpha = alpha.value }
    )
}

private const val MEDIA_ICON_FADE_MS = 200
