/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.AttentionStates
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.NetSpeedRules
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherSlotFace
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.util.IdleWidgetData
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import com.agupta07505.smartisland.ui.IslandText as Text

/** One side of the wide pill: what the glyph slot (left) or value slot (right) shows. */
@Composable
internal fun IdleWidgetSlot(frame: IdleWidgetFrame?, right: Boolean) {
    if (frame == null || frame.compact) return
    AnimatedContent(
        modifier = Modifier.graphicsLayer { alpha = frame.appear.value }.clipToBounds(),
        targetState = frame.kind,
        transitionSpec = {
            if (frame.autoSwitch && frame.tickAnimates) {
                // Automatic switch: the active glyph steps out to the right in 3 ticks while the next steps in from
                // the left. The right slot follows a little later so the 2 read as 1 flip
                val ease = Easing { IdleSlots.tickProgress(it) }
                val lag = if (right) IdleSlots.STAGGER_MS else 0
                val travel = tween<IntOffset>(SWITCH_MS, delayMillis = lag, easing = ease)
                (slideInHorizontally(travel) { -it * 2 } + fadeIn(tween(SWITCH_MS, delayMillis = lag,
                    easing = ease))) togetherWith
                    (slideOutHorizontally(travel) { it * 2 } + fadeOut(tween(SWITCH_MS, delayMillis = lag,
                        easing = ease)))
            } else {
                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
            }
        },
        label = "idleWidgetSlot"
    ) { kind ->
        IdleWidgetSlotFace(kind, frame, right)
    }
}

@Composable
private fun IdleWidgetSlotFace(kind: IdleWidgetKind, frame: IdleWidgetFrame, right: Boolean) {
    // LED footsteps tick draws the slot on an LED grid (ui/LedTickSlot.kt), widgets it cannot draw keep the smooth tick
    val led = frame.led
    if (led != null && ledSlotSupported(kind, right)) {
        key(led.fontId, led.colorId) { LedSlot(kind, frame, right, led) }
        return
    }
    // True while the slot's own change tick (weather) plays, the 2.5 s sideways tick leaves it alone
    val ownTick = remember { mutableStateOf(false) }
    SideTickBox(kind, right, frame, ownTickRunning = { ownTick.value }) {
        IdleWidgetSlotBody(kind, frame, right, ownTick)
    }
}

private fun beatKey(kind: IdleWidgetKind, frame: IdleWidgetFrame): Any? {
    val d = frame.data
    return when (kind) {
        IdleWidgetKind.Date -> Calendar.getInstance().apply { timeInMillis = d.nowMs }
            .let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
        IdleWidgetKind.Wind -> IdleWidgets.displayWind(d.weather?.windKmh ?: -1f, frame.windUnit)
        IdleWidgetKind.Battery -> {
            val watts = if (d.charging) WidgetIslandContent.batteryWatts(d.batteryCurrentRaw,
                d.batteryVoltageMv) else null
            if (IdleSlots.batteryShowsPower(d.charging, watts) && watts != null) {
                listOf(
                    IdleSlots.shortWatts(watts),
                    d.batteryTempTenthsC.takeIf { it != IdleWidgetData.NO_TEMPERATURE }
                        ?.let { IdleSlots.shortBatteryTemp(it, frame.fahrenheit) }
                )
            } else {
                listOf(d.batteryPercent, d.charging)
            }
        }
        IdleWidgetKind.NextAlarm -> d.nextAlarmMs
        else -> null
    }
}

@Composable
private fun SideTickBox(
    kind: IdleWidgetKind,
    right: Boolean,
    frame: IdleWidgetFrame,
    ownTickRunning: () -> Boolean,
    content: @Composable () -> Unit
) {
    val x = remember { Animatable(0f) }
    val pull = remember { Animatable(0f) }
    val frameNow by rememberUpdatedState(frame)
    val ownNow by rememberUpdatedState(ownTickRunning)
    // what the widget showed at the last beat that counted (for "beat only when a value changes")
    val seen = remember(kind) { arrayOf<Any?>(beatKey(kind, frame)) }
    val beatRuns = IdleSlots.beatRuns(frame.beatPaused, frame.tickAnimates)
    // Pausing (island open over the pill) or turning the beat off restarts this effect: the glyph is put back first,
    // and when the beat is allowed again both slots start together so the right one stays the stagger behind
    LaunchedEffect(kind, right, beatRuns) {
        x.snapTo(0f)
        pull.snapTo(0f)
        if (!beatRuns) return@LaunchedEffect
        if (right) delay(IdleSlots.STAGGER_MS.toLong())
        while (true) {
            delay(frameNow.tickPeriodMs)
            val f = frameNow
            val lead = IdleSlots.speedLead(f.data.downBytesPerSecond, f.data.upBytesPerSecond)
            // net speed shows 1 steady arrow and 1 number, nothing ticks sideways
            if (kind == IdleWidgetKind.NetSpeed) continue
            if (f.appear.value < 1f) continue
            // Keep clear of the widget switch: not while one plays (leaving slot included), not when the next is due
            // before the beat ends
            val nowMs = SystemClock.elapsedRealtime()
            if (!IdleSlots.beatClearOfSwitch(nowMs - f.lastSwitchAtMs, nowMs - f.cycleStartAtMs, f.cycleMs)) continue
            val key = beatKey(kind, f)
            if (!IdleSlots.takesSideTick(kind, right, ownNow(), lead)) {
                // A slot that ticks by itself already showed its change
                seen[0] = key
                continue
            }
            if (!IdleSlots.beatDue(f.beatOnlyOnChange, key != seen[0])) continue
            seen[0] = key
            // the heavy tick: hang back a little, slowly, then snap out
            pull.animateTo(1f, tween(SIDE_WINDUP_MS, easing = FastOutSlowInEasing))
            x.animateTo(1f, tween(SIDE_TICK_MS, easing = FastOutLinearInEasing))
            pull.snapTo(0f)
            x.snapTo(-1f)
            // The glyph lands a little past its place and settles back, like a clock hand
            x.animateTo(IdleSlots.SETTLE_OVERSHOOT, tween(SIDE_TICK_MS, easing = LinearOutSlowInEasing))
            x.animateTo(0f, tween(IdleSlots.SETTLE_MS, easing = FastOutSlowInEasing))
        }
    }
    // Outer box stays put and clips so the glyph never leaves its own glyph space
    Box(modifier = Modifier.clipToBounds()) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = (x.value - SIDE_PULL_DEPTH * pull.value) * size.width
                // Fastest at the edges of the travel: a little stretch along it and a little less alpha
                val fade = ((abs(x.value) - IdleSlots.SETTLE_OVERSHOOT) / (1f - IdleSlots.SETTLE_OVERSHOOT))
                    .coerceIn(0f, 1f)
                scaleX = 1f + IdleSlots.STRETCH_SCALE * min(1f, abs(x.value))
                alpha = (1f - fade) * (1f - IdleSlots.STRETCH_FADE * min(1f, abs(x.value)))
            }
        ) { content() }
    }
}

@Composable
private fun IdleWidgetSlotBody(kind: IdleWidgetKind, frame: IdleWidgetFrame, right: Boolean,
        ownTick: MutableState<Boolean>) {
    val data = frame.data
    when (kind) {
        IdleWidgetKind.Date -> {
            val locale = Locale.getDefault()
            val text = remember(data.nowMs, locale, right) {
                SimpleDateFormat(if (right) IdleSlots.MONTH_PATTERN else IdleSlots.DAY_PATTERN, locale)
                    .format(Date(data.nowMs))
            }
            SlotText(text, frame)
        }
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            if (snapshot == null) {
                if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind,
                    frame)
            } else {
                // Target carries what is drawn, so the old face keeps its values while it ticks away.
                val slots = WeatherDisplay.slots(frame.weatherMode, snapshot, frame.fahrenheit)
                LaunchedEffect(slots) {
                    ownTick.value = true
                    delay(TICK_MS + IdleSlots.SETTLE_MS + IdleSlots.STAGGER_MS + 80L)
                    ownTick.value = false
                }
                if (right) {
                    VerticalTick(target = slots, towardsDown = false, lagMs = IdleSlots.STAGGER_MS,
                        animate = frame.tickAnimates) { shown -> WeatherSlotView(shown.right, frame, snapshot.windKmh) }
                } else if (slots.left is WeatherSlotFace.Icon) {
                    // Glyph morphs only when the look really changes, a new temperature alone leaves it be
                    WeatherSlotView(slots.left, frame, snapshot.windKmh)
                } else {
                    VerticalTick(target = slots, towardsDown = true,
                        animate = frame.tickAnimates) { shown -> WeatherSlotView(shown.left, frame, snapshot.windKmh) }
                }
            }
        }
        IdleWidgetKind.Battery -> {
            val watts = if (data.charging) WidgetIslandContent.batteryWatts(data.batteryCurrentRaw,
                data.batteryVoltageMv) else null
            if (IdleSlots.batteryShowsPower(data.charging, watts) && watts != null) {
                if (right) {
                    val temp = data.batteryTempTenthsC.takeIf { it != IdleWidgetData.NO_TEMPERATURE }
                    SlotText(temp?.let { IdleSlots.shortBatteryTemp(it, frame.fahrenheit) } ?: "--", frame)
                } else {
                    SlotText(IdleSlots.shortWatts(watts), frame)
                }
            } else {
                if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind,
                    frame)
            }
        }
        IdleWidgetKind.NextAlarm -> {
            if (data.nextAlarmMs <= 0L) {
                if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind,
                    frame)
            } else if (right) {
                val minute = remember(data.nextAlarmMs) { alarmField(data.nextAlarmMs, Calendar.MINUTE) }
                SlotText(IdleSlots.alarmMinuteText(minute), frame)
            } else {
                AlarmLeftSlot(frame)
            }
        }
        IdleWidgetKind.NetSpeed -> NetSpeedSlot(frame, right)
        IdleWidgetKind.Notifications -> if (right) NotificationCountText(frame) else NotificationTopAppGlyph(frame)
        IdleWidgetKind.LiveClock -> ClockTimeText(frame, right)
        IdleWidgetKind.Modes -> if (!right) ModeGlyph(frame)
        else -> if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind,
            frame)
    }
}

@Composable
private fun NetSpeedSlot(frame: IdleWidgetFrame, right: Boolean) {
    val data = frame.data
    val upload = NetSpeedRules.isUpload(data.downBytesPerSecond, data.upBytesPerSecond)
    if (right) {
        SlotText(IdleWidgets.shortSpeed(NetSpeedRules.busier(data.downBytesPerSecond, data.upBytesPerSecond)), frame)
    } else {
        val fast = rememberFastSpeed(data.speedBytesPerSecond)
        // Arrow slides in from its own direction: ↓ drops from the top, ↑ rises from the bottom
        val y = remember { Animatable(if (upload) 1f else -1f) }
        LaunchedEffect(upload) {
            y.snapTo(if (upload) 1f else -1f)
            y.animateTo(0f, tween(SIDE_TICK_MS * 2, easing = LinearOutSlowInEasing))
        }
        Box(modifier = Modifier.clipToBounds()) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationY = y.value * size.height
                    alpha = 1f - abs(y.value)
                }
            ) {
                AttentionGlyph(
                    icon = if (upload) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    attention = AttentionStates.forWidget(IdleWidgetKind.NetSpeed, fast, alarmSoon = false,
                        animationsOff = SystemAnimScale.animationsOff()),
                    size = (15f * frame.glyphScale).dp
                )
            }
        }
    }
}

@Composable
private fun AlarmLeftSlot(frame: IdleWidgetFrame) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick++
        }
    }
    val hour = remember(frame.data.nextAlarmMs, is24) {
        IdleSlots.alarmHourText(alarmField(frame.data.nextAlarmMs, Calendar.HOUR_OF_DAY), is24)
    }
    VerticalTick(target = IdleSlots.alarmShowsHour(tick), towardsDown = true,
        animate = frame.tickAnimates) { showHour ->
        if (showHour) SlotText(hour, frame) else IdleWidgetIcon(IdleWidgetKind.NextAlarm, frame)
    }
}

@Composable
private fun WeatherSlotView(face: WeatherSlotFace, frame: IdleWidgetFrame, windKmh: Float) {
    when (face) {
        WeatherSlotFace.Empty -> Unit
        is WeatherSlotFace.Text -> SlotText(face.text, frame)
        is WeatherSlotFace.Icon -> MorphingWeatherGlyph(face.look, face.isDay, windKmh, (15f * frame.glyphScale).dp,
            frame.weatherAnimate)
    }
}

private fun alarmField(atMs: Long, field: Int): Int =
    Calendar.getInstance().apply { timeInMillis = atMs }.get(field)

@Composable
private fun SlotText(text: String, frame: IdleWidgetFrame) {
    Text(
        text = text,
        color = Color.White,
        fontSize = (11f * frame.glyphScale).sp,
        fontFamily = frame.glyphFont,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
private fun <T> VerticalTick(target: T, towardsDown: Boolean, lagMs: Int = 0, animate: Boolean = true,
        content: @Composable (T) -> Unit) {
    val dir = if (towardsDown) 1 else -1
    AnimatedContent(
        targetState = target,
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            if (!animate) {
                // Battery Saver or "remove animations": no move, a plain cross-fade
                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
            } else {
                // A clock's heavy tick: the glyph hangs back slowly, then snaps through and settles, fades wait for the
                // snap
                val settled = Easing { IdleSlots.settledTickProgress(it, TICK_MS) }
                val total = TICK_MS + IdleSlots.SETTLE_MS
                val windUp = (TICK_MS * IdleSlots.HEAVY_WINDUP_SHARE).toInt()
                val snap = TICK_MS - windUp
                (slideInVertically(tween(total, delayMillis = lagMs, easing = settled)) { -dir * it * 3 / 4 } +
                    fadeIn(tween(snap, delayMillis = windUp + lagMs))) togetherWith
                    (slideOutVertically(tween(total, delayMillis = lagMs, easing = settled)) { dir * it * 3 / 4 } +
                        fadeOut(tween(snap * 2 / 3, delayMillis = windUp + lagMs)))
            }
        },
        label = "idleSlotTick"
    ) { shown ->
        if (!animate) {
            content(shown)
        } else {
            // 0..1 with the speed of the snap, for entering and leaving content alike (the lag is part of the curve, so
            // the glyph rests until it starts)
            val sweep by transition.animateFloat(
                transitionSpec = {
                    val total = TICK_MS + IdleSlots.SETTLE_MS
                    tween(total + lagMs, easing = Easing { f ->
                        val ms = f * (total + lagMs) - lagMs
                        if (ms <= 0f) 1f else 1f - IdleSlots.stretchIntensity(ms / total, TICK_MS)
                    })
                },
                label = "idleSlotStretch"
            ) { state -> if (state == EnterExitState.Visible) 0f else 1f }
            val stretch = if (transition.targetState == EnterExitState.Visible) sweep else 1f - sweep
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleY = 1f + IdleSlots.STRETCH_SCALE * stretch
                    alpha = 1f - IdleSlots.STRETCH_FADE * stretch
                }
            ) { content(shown) }
        }
    }
}

private const val TICK_MS = 560

private const val SIDE_TICK_MS = IdleSlots.BEAT_HALF_MS

private const val SIDE_WINDUP_MS = IdleSlots.BEAT_WINDUP_MS
private const val SIDE_PULL_DEPTH = 0.12f

private val SWITCH_MS = Math.round(900 * IdleSlots.switchTimeScale())

@Composable
private fun ClockTimeText(frame: IdleWidgetFrame, right: Boolean) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val format = remember(is24, right, locale) {
        SimpleDateFormat(if (right) "mm" else if (is24) "HH" else "h", locale)
    }
    val text = format.format(Date(frame.data.nowMs))
    VerticalTick(target = text, towardsDown = false, lagMs = if (right) IdleSlots.STAGGER_MS else 0,
        animate = frame.tickAnimates) { shown -> SlotText(shown, frame) }
}

@Composable
private fun NotificationTopAppGlyph(frame: IdleWidgetFrame) {
    val top = frame.data.notificationSummary.apps.firstOrNull()?.packageName
    VerticalTick(target = top, towardsDown = true, animate = frame.tickAnimates) { shown ->
        if (shown == null) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((15f * frame.glyphScale).dp)
            )
        } else {
            AppIcon(shown, size = (16f * frame.glyphScale).dp)
        }
    }
}

@Composable
private fun NotificationCountText(frame: IdleWidgetFrame) {
    val text = NotificationCounts.badgeText(frame.data.notificationSummary.total)
    VerticalTick(target = text, towardsDown = false, lagMs = IdleSlots.STAGGER_MS,
        animate = frame.tickAnimates) { shown -> SlotText(shown, frame) }
}

@Composable
private fun ModeGlyph(frame: IdleWidgetFrame) {
    val mode = frame.data.mode
    VerticalTick(target = mode, towardsDown = true, animate = frame.tickAnimates) { shown ->
        ModeIcon(shown, size = (15f * frame.glyphScale).dp)
    }
}
