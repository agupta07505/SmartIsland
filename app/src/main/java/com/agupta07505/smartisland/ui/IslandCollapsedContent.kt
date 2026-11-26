/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CallEnds
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CollapsedSlotPlan
import com.agupta07505.smartisland.model.CollapsedSlots
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SLOT_END_PADDING_DP
import com.agupta07505.smartisland.model.SLOT_GLYPH_SIZE_DP
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
import com.agupta07505.smartisland.util.MediaArt
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
fun IslandCollapsedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    collapsedAlpha: Float,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier,
    /** Idle widget to draw while no card is on the island (mode Empty). */
    idleWidget: IdleWidgetFrame? = null,
    cameraGapDp: Float = 0f,
    /** Settled pill width (dp) the slots are sized against. */
    pillWidthDp: Float = 0f,
    /** Camera hole's centre from the pill's left edge (dp). */
    cameraCentreInPillDp: Float? = null,
    /** Side camera pins its own edge. */
    cameraSide: CameraSide = CameraSide.Centre,
    /** Side camera's slot plan (model/SideCamera.kt) */
    sidePlan: CollapsedSlotPlan? = null,
    pulse: () -> Float = { 0f }
) {
    val density = LocalDensity.current
    val maxTranslationPx = with(density) { COLLAPSED_TRANSLATION_MAX_DP.toPx() }
    val translationProgress = 1f - collapsedAlpha
    val translationXLeft = translationProgress * maxTranslationPx
    val translationXRight = -translationProgress * maxTranslationPx
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val slotPillWidth = if (pillWidthDp > 0f) pillWidthDp else maxWidth.value
        val classicPlan = remember(slotPillWidth, cameraCentreInPillDp, cameraGapDp) {
            CollapsedSlots.zone(slotPillWidth, cameraCentreInPillDp, cameraGapDp).plan
        }
        val plan = sidePlan ?: classicPlan
        val stacked = plan.side != SlotSide.Both
        val startShift = if (pillWidthDp > 0f) {
            SideCamera.startShiftDp(cameraSide, maxWidth.value, pillWidthDp)
        } else 0f
        val endShift = if (pillWidthDp > 0f) {
            SideCamera.endShiftDp(cameraSide, maxWidth.value, pillWidthDp)
        } else 0f
        val valueSlotFixed = sidePlan != null
        val valueToFarEnd = sidePlan != null && cameraSide != CameraSide.Centre
        val followSide = sidePlan != null && pillWidthDp > 0f && cameraSide != CameraSide.Centre
        val cameraOnRight = cameraSide == CameraSide.Right
        val glyphFollow = 0f
        val valueFollow = if (followSide) {
            SideCamera.farEndShiftDp(maxWidth.value - pillWidthDp)
        } else 0f
        val followSign = if (cameraOnRight) -1f else 1f
        val pulsing = cameraSide == CameraSide.Centre && pillWidthDp > 0f
        val pulseHalfPx = with(density) { (pillWidthDp * PILL_PULSE_WIDTH_FRACTION / 2f).dp.toPx() }
        val glyphPulseSign = if (plan.side == SlotSide.RightOfCamera) 1f else -1f
        val valuePulseSign = if (stacked) glyphPulseSign else 1f
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (plan.glyphStartDp + startShift + followSign * glyphFollow).coerceAtLeast(0f).dp)
                .graphicsLayer {
                    val p = if (pulsing) pulse().coerceIn(0f, 1f) else 0f
                    translationX = translationXLeft + glyphPulseSign * pulseHalfPx * p
                },
            contentAlignment = Alignment.Center
        ) {
            GlyphSlot(mode) { CollapsedGlyphContent(mode, notification, settings, idleWidget) }
        }

        Row(
            modifier = Modifier
                .align(if (stacked) Alignment.CenterStart else Alignment.CenterEnd)
                .then(
                    if (stacked) Modifier.padding(start = (plan.valueStartDp + startShift + followSign * valueFollow)
                        .coerceAtLeast(0f).dp)
                    else Modifier.padding(end = (SLOT_END_PADDING_DP + endShift).dp)
                )
                .then(
                    plan.valueMaxWidthDp?.let { if (valueSlotFixed) Modifier.width(it.dp) else Modifier.widthIn(max =
                        it.dp) } ?: Modifier
                )
                .graphicsLayer {
                    val p = if (pulsing) pulse().coerceIn(0f, 1f) else 0f
                    // a swipe slides the whole layer, so the value moves with the glyph
                    translationX = translationXRight + valuePulseSign * pulseHalfPx * p
                },
            horizontalArrangement = if (valueToFarEnd && !cameraOnRight) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(contentAlignment = Alignment.Center) {
            CollapsedValueContent(mode, notification, settings, idleWidget)
        }
        }

        // the small idle circle has no 2 sides: its one value sits in the middle
        if (mode == IslandMode.Empty) {
            Box(modifier = Modifier.align(Alignment.Center)) { IdleWidgetCompact(idleWidget) }
        }
    }
}

/** Fixed glyph square so every glyph shares 1 centre, as the slot width maths assume. */
@Composable
internal fun GlyphSlot(mode: IslandMode, content: @Composable () -> Unit) {
    if (mode == IslandMode.Empty) {
        content()
        return
    }
    Box(
        modifier = Modifier.size(SLOT_GLYPH_SIZE_DP.dp).wrapContentSize(Alignment.Center, unbounded = true),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Suppress("NAME_SHADOWING")
@Composable
internal fun CollapsedGlyphContent(
    mode: IslandMode,
    notification: IslandNotification?,
    settings: SmartIslandSettings,
    idleWidget: IdleWidgetFrame? = null
) {
    val settings = settings.forCard(notification)
    when (mode) {
        IslandMode.Notification -> NotificationGlyph(notification = notification, settings = settings)
        IslandMode.IncomingCall -> {
            val icon = notification?.largeIcon ?: notification?.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IslandMode.Music -> {
            val artwork = notification?.let { MediaArt.glyphBitmap(it) }
            if (artwork != null) {
                Image(
                    bitmap = remember(artwork) { artwork.asImageBitmap() },
                    contentDescription = null,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(settings.musicVisualizerColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
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
        IslandMode.DownloadUpload -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Bluetooth -> {
            ConnectionCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Flashlight -> {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(settings.flashlightColor).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color(settings.flashlightColor),
                    modifier = Modifier.size(14.dp)
                )
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
        IslandMode.CallEnded -> CallEndedGlyph(notification = notification)
        IslandMode.Empty -> IdleWidgetSlot(idleWidget, right = false)
    }
}

@Suppress("NAME_SHADOWING")
@Composable
internal fun CollapsedValueContent(
    mode: IslandMode,
    notification: IslandNotification?,
    settings: SmartIslandSettings,
    idleWidget: IdleWidgetFrame? = null
) {
    val settings = settings.forCard(notification)
    when (mode) {
        IslandMode.Notification -> {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(settings.notificationDotColor))
            )
        }
        IslandMode.IncomingCall -> {
            if (notification?.isCallRinging == true || notification?.isCallConnecting == true) {
                val status = notification?.callStatusText?.takeIf { it.isNotBlank() } ?: "Ringing..."
                FitSlotText(
                    text = status,
                    color = Color(settings.callColor),
                    fontWeight = FontWeight.Bold,
                    compact = compactWord(status)
                )
            } else {
                val time = notification?.timeMillis ?: System.currentTimeMillis()
                CallTimer(postTimeMillis = time, color = Color(settings.callColor))
            }
        }
        IslandMode.Music -> {
            val artworkColor = rememberArtworkVisualizerColor(
                notification = notification,
                fallback = Color(settings.musicVisualizerColor)
            )
            AudioVisualizer(
                isPlaying = notification?.mediaIsPlaying == true,
                color = artworkColor
            )
        }
        IslandMode.Battery -> {
            val pctText = notification?.text ?: "49%"
            val textColor = batteryVisualColor(
                resolveBatteryVisual(
                    notification?.category,
                    notification?.title,
                    pctText.replace("%", "").toFloatOrNull() ?: 50f
                ),
                Color(settings.batteryColor),
                settings.cardEventColor
            )
            FitSlotText(
                text = pctText,
                color = textColor,
                fontWeight = FontWeight.Bold,
                compact = pctText.removeSuffix("%")
            )
        }
        IslandMode.LiveActivity -> {
            LiveActivityCollapsedRight(notification = notification, settings = settings)
        }
        IslandMode.Navigation -> {
            NavigationCollapsedRight(notification = notification, settings = settings)
        }
        IslandMode.DownloadUpload -> {
            DownloadUploadCollapsedRight(notification = notification, settings = settings)
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedRight(notification = notification, settings = settings)
        }
        IslandMode.Bluetooth -> {
            BluetoothCollapsedRight(notification = notification, settings = settings)
        }
        IslandMode.Flashlight -> {
            Text(
                text = "ON",
                color = Color(settings.flashlightColor),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        IslandMode.ScreenRecording, IslandMode.AudioRecording -> {
            RecordingTimerFit(notification = notification, color = Color(settings.screenRecordingColor))
        }
        IslandMode.Timer -> {
            TimerCountdown(notification = notification, color = Color(settings.timerColor))
        }
        IslandMode.Stopwatch -> {
            StopwatchTimer(notification = notification, color = Color(settings.stopwatchColor))
        }
        IslandMode.CallEnded -> {
            val duration = notification?.callEnd?.durationMs ?: 0L
            FitSlotText(
                text = CallEnds.format(duration),
                color = Color(settings.callColor),
                fontWeight = FontWeight.SemiBold,
                compact = CallEnds.compact(duration)
            )
        }
        IslandMode.Empty -> IdleWidgetSlot(idleWidget, right = true)
    }
}

private val COLLAPSED_TRANSLATION_MAX_DP = 32.dp

internal fun compactClock(text: String): String {
    val parts = text.split(':')
    return when (parts.size) {
        3 -> "${parts[0]}h ${parts[1].toIntOrNull() ?: 0}m"
        2 -> "${parts[0].toIntOrNull() ?: 0}m"
        else -> text
    }
}

internal fun compactDistance(text: String): String = text
    .removePrefix("In ")
    .replace(Regex("\\s*(miles?|mi)\\b", RegexOption.IGNORE_CASE), "mi")
    .replace(Regex("\\s*(meters?|m)\\b", RegexOption.IGNORE_CASE), "m")
    .replace(Regex("\\s*(km)\\b", RegexOption.IGNORE_CASE), "km")
    .replace(Regex("\\s*(feet|ft)\\b", RegexOption.IGNORE_CASE), "ft")

internal fun compactEta(text: String): String =
    if (text.endsWith(" min")) text.removeSuffix(" min") + "m" else compactWord(text)

private fun compactWord(text: String): String {
    val word = text.trimEnd('.', '\u2026').trim().split(' ').first()
    return if (word.length > 5) word.take(4) + "\u2026" else word
}
