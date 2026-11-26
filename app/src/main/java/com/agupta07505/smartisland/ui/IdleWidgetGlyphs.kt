/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.AttentionStates
import com.agupta07505.smartisland.model.GlyphAttention
import com.agupta07505.smartisland.model.GlyphMotion
import com.agupta07505.smartisland.model.GlyphMotionKind
import com.agupta07505.smartisland.model.GlyphMotions
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherLook
import com.agupta07505.smartisland.model.WeatherMorph
import com.agupta07505.smartisland.model.weatherLook
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
internal fun IdleWidgetCompact(frame: IdleWidgetFrame?) {
    if (frame == null || !frame.compact) return
    AnimatedContent(
        modifier = Modifier.graphicsLayer { alpha = frame.appear.value },
        targetState = frame.kind,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "idleWidgetCompact"
    ) { kind ->
        Box(contentAlignment = Alignment.Center) {
            if (kind == IdleWidgetKind.Modes) ModeIcon(frame.data.mode, size = 12.dp) else IdleWidgetValue(kind, frame,
                compact = true)
        }
    }
}

@Composable
internal fun IdleWidgetIcon(kind: IdleWidgetKind, frame: IdleWidgetFrame, size: Dp = (15f * frame.glyphScale).dp) {
    val data = frame.data
    val (icon, tint) = when (kind) {
        IdleWidgetKind.Clock -> Icons.Rounded.Schedule to SOFT_WHITE
        IdleWidgetKind.LiveClock -> Icons.Rounded.AccessTime to SOFT_WHITE
        IdleWidgetKind.DayMoments -> Icons.Rounded.WbTwilight to SOFT_WHITE
        IdleWidgetKind.Date -> Icons.Rounded.CalendarToday to SOFT_WHITE
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            if (snapshot == null) Icons.Rounded.Cloud to SOFT_WHITE
            else weatherIcon(weatherLook(snapshot.weatherCode), snapshot.isDay)
        }
        IdleWidgetKind.Wind -> Icons.Rounded.Air to SOFT_WHITE
        IdleWidgetKind.Battery -> batteryIcon(data.batteryPercent, data.charging)
        IdleWidgetKind.NextAlarm -> Icons.Rounded.Alarm to ORANGE
        IdleWidgetKind.NetSpeed ->
            if (data.speedIsUpload) Icons.Rounded.ArrowUpward to ORANGE else Icons.Rounded.ArrowDownward to SKY
        IdleWidgetKind.Notifications -> Icons.Rounded.Notifications to SOFT_WHITE
        IdleWidgetKind.Modes -> modeFallbackIcon(data.mode.kind) to SOFT_WHITE
    }
    val snapshot = data.weather
    if (kind == IdleWidgetKind.Weather && snapshot != null) {
        MorphingWeatherGlyph(weatherLook(snapshot.weatherCode), snapshot.isDay, snapshot.windKmh, size,
            frame.weatherAnimate)
        return
    }
    val attention = AttentionStates.forWidget(
        kind = kind,
        fastSpeed = if (kind == IdleWidgetKind.NetSpeed) rememberFastSpeed(data.speedBytesPerSecond) else false,
        alarmSoon = if (kind == IdleWidgetKind.NextAlarm) rememberAlarmSoon(data.nextAlarmMs) else false,
        animationsOff = SystemAnimScale.animationsOff()
    )
    if (attention != GlyphAttention.None) {
        AttentionGlyph(icon = icon, attention = attention, size = size)
        return
    }
    val motion = GlyphMotions.forWidget(
        kind = kind,
        look = snapshot?.let { weatherLook(it.weatherCode) },
        isDay = snapshot?.isDay ?: true,
        windKmh = snapshot?.windKmh ?: -1f,
        animationsOff = SystemAnimScale.animationsOff()
    )
    MotionGlyph(icon = icon, motion = motion, size = size)
}

private data class WeatherGlyph(val look: WeatherLook, val isDay: Boolean)

@Composable
internal fun MorphingWeatherGlyph(look: WeatherLook, isDay: Boolean, windKmh: Float, size: Dp, animate: Boolean) {
    val animationsOff = SystemAnimScale.animationsOff() || !animate
    val target = WeatherGlyph(look, isDay)
    // [shown] is the glyph the morph arrives at, [outgoing] the one fading away (null when no morph runs)
    var shown by remember { mutableStateOf(target) }
    var outgoing by remember { mutableStateOf<WeatherGlyph?>(null) }
    val progress = remember { Animatable(1f) }
    // 0..1 after the morph: the new glyph lands, swells a little and settles (1 = at rest)
    val landing = remember { Animatable(1f) }
    LaunchedEffect(target, animationsOff) {
        if (target == shown) return@LaunchedEffect
        val from = shown
        landing.snapTo(1f)
        shown = target
        if (animationsOff || !WeatherMorph.changed(from.look, from.isDay, target.look, target.isDay)) {
            outgoing = null
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        outgoing = from
        progress.snapTo(0f)
        progress.animateTo(1f, tween(WeatherMorph.MORPH_MS, easing = LinearEasing))
        landing.snapTo(0f)
        outgoing = null
        landing.animateTo(1f, tween(WeatherMorph.LANDING_MS, easing = LinearEasing))
    }
    val leaving = outgoing
    if (leaving == null) {
        Box(
            modifier = Modifier.graphicsLayer {
                val k = WeatherMorph.landingScale(landing.value)
                scaleX = k
                scaleY = k
            }
        ) {
            MotionGlyph(
                icon = weatherIcon(shown.look, shown.isDay).first,
                motion = GlyphMotions.forWidget(IdleWidgetKind.Weather, shown.look, shown.isDay, windKmh,
                    animationsOff),
                size = size
            )
        }
        return
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        // both glyphs are tinted white, nothing to blend, the cross-fade carries the change
        for ((glyph, incoming) in listOf(leaving to false, shown to true)) {
            Icon(
                imageVector = weatherIcon(glyph.look, glyph.isDay).first,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size).graphicsLayer {
                    val f = WeatherMorph.frame(progress.value)
                    val layer = if (incoming) f.incoming else f.outgoing
                    alpha = layer.alpha
                    scaleX = layer.scale
                    scaleY = layer.scale
                    rotationZ = layer.rotationDegrees
                }
            )
        }
    }
}

/** Net speed counts as fast with hysteresis: on at 1 MB/s, off again below 0.8 MB/s. */
@Composable
internal fun rememberFastSpeed(bytesPerSecond: Long): Boolean {
    val latch = remember { booleanArrayOf(false) }
    val fast = AttentionStates.isFastSpeed(bytesPerSecond, latch[0])
    latch[0] = fast
    return fast
}

/** Whether the next alarm is under 10 minutes away. */
@Composable
internal fun rememberAlarmSoon(nextAlarmMs: Long): Boolean {
    var now by remember(nextAlarmMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nextAlarmMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(AttentionStates.alarmRecheckDelayMs(nextAlarmMs, now) ?: break)
        }
    }
    return AttentionStates.isAlarmSoon(nextAlarmMs, now)
}

@Composable
internal fun AttentionGlyph(icon: ImageVector, attention: GlyphAttention, size: Dp) {
    if (attention == GlyphAttention.None) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size))
        return
    }
    val transition = rememberInfiniteTransition(label = "glyphAttention")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(AttentionStates.periodMs(attention), easing = LinearEasing)),
        label = "attentionPhase"
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (attention == GlyphAttention.AlarmGlow) {
            // The halo is larger than the glyph but takes no layout room of its own
            Box(
                modifier = Modifier
                    .requiredSize(size * 1.6f)
                    .graphicsLayer {
                        val breath = AttentionStates.breath(phase)
                        alpha = AttentionStates.glowAlpha(breath)
                        scaleX = AttentionStates.glowScale(breath)
                        scaleY = AttentionStates.glowScale(breath)
                    }
                    .background(Brush.radialGradient(listOf(ORANGE, Color.Transparent)), CircleShape)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size).graphicsLayer {
                if (attention == GlyphAttention.SpeedPulse) {
                    val s = AttentionStates.pulseScale(AttentionStates.breath(phase))
                    scaleX = s
                    scaleY = s
                }
            }
        )
    }
}

@Composable
private fun MotionGlyph(icon: ImageVector, motion: GlyphMotion, size: Dp) {
    if (!motion.isRunning) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size))
        return
    }
    val transition = rememberInfiniteTransition(label = "glyphMotion")
    // A new loop length (the wind changed) restarts the loop at the new speed
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(motion.periodMs, easing = LinearEasing)),
        label = "glyphPhase"
    )
    when (motion.kind) {
        GlyphMotionKind.SunSpin -> Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size).graphicsLayer { rotationZ = GlyphMotions.sunAngle(phase) }
        )
        GlyphMotionKind.WindSway -> Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size).graphicsLayer { translationX =
                GlyphMotions.windOffsetShare(phase) * this.size.width }
        )
        GlyphMotionKind.RainFall -> Box(modifier = Modifier.size(size).clipToBounds(),
            contentAlignment = Alignment.Center) {
            // Two small drops half a loop apart, each fading in at the top and out at the bottom
            for (shift in floatArrayOf(0f, 0.5f)) {
                val drop = GlyphMotions.drop(phase + shift)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f).graphicsLayer {
                        translationY = drop.offsetShare * this.size.height
                        alpha = drop.alpha
                    }
                )
            }
        }
        GlyphMotionKind.None -> Unit
    }
}

@Composable
internal fun IdleWidgetValue(kind: IdleWidgetKind, frame: IdleWidgetFrame, compact: Boolean,
        monochrome: Boolean = false) {
    val context = LocalContext.current
    val data = frame.data
    val is24 = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale) }
    val dateFormat = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEd"), locale)
    }
    val dayFormat = remember(locale) { SimpleDateFormat("d", locale) }
    val weekdayFormat = remember(locale) { SimpleDateFormat("EEE", locale) }

    var color = Color.White
    val text: String
    when (kind) {
        IdleWidgetKind.Clock, IdleWidgetKind.LiveClock -> text = timeFormat.format(Date(data.nowMs))
        IdleWidgetKind.DayMoments -> text = ""
        IdleWidgetKind.Date -> text = (if (compact) dayFormat else dateFormat).format(Date(data.nowMs))
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            text = if (snapshot == null) "--\u00B0" else WeatherDisplay.compactText(frame.weatherMode, snapshot,
                frame.fahrenheit)
        }
        IdleWidgetKind.Wind -> text = IdleWidgets.displayWind(data.weather?.windKmh ?: -1f, frame.windUnit)
        IdleWidgetKind.Battery -> {
            val pct = data.batteryPercent
            text = if (pct < 0) "--" else if (compact) "$pct" else "$pct%"
            color = when {
                data.charging -> GREEN
                pct in 0..15 -> RED
                else -> Color.White
            }
        }
        IdleWidgetKind.NextAlarm -> {
            val at = data.nextAlarmMs
            text = when {
                at <= 0L -> "--"
                // Within a day the time, further away the weekday so "Mon" is not mistaken for this morning
                at - data.nowMs > DAY_MS -> weekdayFormat.format(Date(at))
                else -> timeFormat.format(Date(at))
            }
            color = ORANGE
        }
        IdleWidgetKind.NetSpeed -> {
            text = IdleWidgets.shortSpeed(data.speedBytesPerSecond)
            color = if (data.speedIsUpload) ORANGE else SKY
        }
        IdleWidgetKind.Notifications -> text = NotificationCounts.badgeText(data.notificationSummary.total)
        // The small circle has no room for a name, so the mode's glyph is drawn instead ([IdleWidgetCompact])
        IdleWidgetKind.Modes -> text = ""
    }
    if (monochrome) color = Color.White
    val size = if (!compact) (11f * frame.glyphScale).sp else when (kind) {
        IdleWidgetKind.Date, IdleWidgetKind.Weather, IdleWidgetKind.Wind, IdleWidgetKind.Battery -> 10.sp
        else -> 8.sp
    }
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontFamily = if (compact) null else frame.glyphFont,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

private const val DAY_MS = 24 * 60 * 60 * 1000L

private val AMBER = Color(0xFFFBBF24)
internal val ORANGE = Color(0xFFFF9F0A)
internal val GREEN = Color(0xFF34C759)
internal val RED = Color(0xFFFF453A)
internal val SKY = Color(0xFF38BDF8)
internal val SOFT_WHITE = Color(0xFFE5E7EB)

internal fun weatherIcon(look: WeatherLook, isDay: Boolean): Pair<ImageVector, Color> = when (look) {
    WeatherLook.Clear -> if (isDay) Icons.Rounded.WbSunny to AMBER else Icons.Rounded.NightsStay to SOFT_WHITE
    WeatherLook.PartlyCloudy -> Icons.Rounded.WbCloudy to (if (isDay) AMBER else SOFT_WHITE)
    WeatherLook.Cloudy, WeatherLook.Fog -> Icons.Rounded.Cloud to SOFT_WHITE
    WeatherLook.Rain -> Icons.Rounded.WaterDrop to SKY
    WeatherLook.Snow -> Icons.Rounded.AcUnit to SKY
    WeatherLook.Storm -> Icons.Rounded.Bolt to AMBER
}

private fun batteryIcon(percent: Int, charging: Boolean): Pair<ImageVector, Color> = when {
    charging -> Icons.Rounded.BatteryChargingFull to GREEN
    percent in 0..15 -> Icons.Rounded.BatteryAlert to RED
    percent >= 90 -> Icons.Rounded.BatteryFull to SOFT_WHITE
    else -> Icons.Rounded.Battery5Bar to SOFT_WHITE
}
