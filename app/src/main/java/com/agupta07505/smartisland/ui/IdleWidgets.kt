/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.LedTick
import com.agupta07505.smartisland.model.LedTickConfig
import com.agupta07505.smartisland.model.NetSpeedRules
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherPill
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.nextStepWithContent
import com.agupta07505.smartisland.util.IdleWidgetData
import com.agupta07505.smartisland.util.SystemAnimScale
import com.agupta07505.smartisland.util.rememberIdleWidgetData
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

/**
 * Idle widgets on the collapsed cutout while no event is on the island (Layout > Idle widgets). Portrait pill: icon
 * left, value right, camera hole clear. Landscape circle: value only, centred
 */
class IdleWidgetFrame(
    val kind: IdleWidgetKind,
    val data: IdleWidgetData,
    val fahrenheit: Boolean,
    val compact: Boolean,
    val appear: State<Float>,
    /** Font of the pill's glyph text; null = the island font. */
    val glyphFont: FontFamily? = null,
    val glyphScale: Float = 1f,
    /** Widget just changed by the automatic timer (not a swipe): glyphs travel left to right in 3 ticks. */
    val autoSwitch: Boolean = false,
    val tickPeriodMs: Long = IdleSlots.SIDE_TICK_PERIOD_MS,
    /** LED footsteps tick (Settings > Idle widgets > Tick style), null = smooth tick. See ui/LedTickSlot.kt. */
    val led: LedTickConfig? = null,
    /** Wind speed unit to draw, already resolved from Auto, see [IdleWidgets.resolveWindUnit]. */
    val windUnit: String = IdleWidgets.WIND_KMH,
    val weatherMode: String = WeatherPill.TEMP_ICON,
    val weatherReadouts: List<String> = emptyList(),
    /** False keeps the weather glyph still (off, Battery Saver, or the system's "remove animations"). */
    val weatherAnimate: Boolean = true,
    val beatOnlyOnChange: Boolean = false,
    /**
     * True while the pill cannot be seen (widget island open over it), the beat waits and restarts in step when it is
     * back
     */
    val beatPaused: Boolean = false,
    /**
     * False under Battery Saver or "remove animations": every tick is a plain cross-fade, see [IdleSlots.ticksAnimate]
     */
    val tickAnimates: Boolean = true,
    /**
     * [android.os.SystemClock.elapsedRealtime] of the last widget switch (automatic or swipe), so the beat stays out of
     * it ([IdleSlots.beatClearOfSwitch])
     */
    val lastSwitchAtMs: Long = 0L,
    /** Same clock at the start of the automatic timer's current wait, and its length (0 = no automatic switching). */
    val cycleStartAtMs: Long = 0L,
    val cycleMs: Long = 0L
)

/**
 * Same data and unit as this frame for another [kind], always as the wide layout. Used by the semi-expanded widget
 * island
 */
internal fun IdleWidgetFrame.asWide(kind: IdleWidgetKind): IdleWidgetFrame =
    IdleWidgetFrame(
        kind = kind, data = data, fahrenheit = fahrenheit, compact = false, appear = appear,
        glyphFont = glyphFont, glyphScale = glyphScale, tickPeriodMs = tickPeriodMs, led = led, windUnit = windUnit,
        weatherMode = weatherMode, weatherReadouts = weatherReadouts, weatherAnimate = weatherAnimate,
        beatOnlyOnChange = beatOnlyOnChange, beatPaused = beatPaused, tickAnimates = tickAnimates
    )

/** Content of the semi-expanded widget island per widget (the clock has none, its job is the hourly tick). */
@Composable
internal fun IdleWidgetIslandContent(
    frame: IdleWidgetFrame,
    batteryMetric: String,
    paused: Boolean,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A swipe on the pill changes the widget: body cross-fades, pause button stays
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = frame.kind,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            contentAlignment = Alignment.Center,
            label = "widgetIslandBody"
        ) { kind ->
            val shown = frame.asWide(kind)
            when (kind) {
                IdleWidgetKind.Date -> DateIslandBody(shown)
                IdleWidgetKind.Weather -> WeatherIslandBody(shown)
                IdleWidgetKind.Wind -> WindIslandBody(shown)
                IdleWidgetKind.Battery -> BatteryIslandBody(shown, batteryMetric)
                IdleWidgetKind.NextAlarm -> AlarmIslandBody(shown)
                IdleWidgetKind.NetSpeed -> SpeedIslandBody(shown)
                IdleWidgetKind.Notifications -> NotificationsIslandBody(shown)
                IdleWidgetKind.Modes -> ModesIslandBody(shown)
                IdleWidgetKind.LiveClock -> ClockIslandBody(shown)
                IdleWidgetKind.Clock, IdleWidgetKind.DayMoments -> Unit
            }
        }
        PauseToggle(paused = paused, onClick = onTogglePause)
    }
}

@Composable
private fun PauseToggle(paused: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (paused) 0.22f else 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            contentDescription = if (paused) "Resume widget switching" else "Pause widget switching",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

private const val IDLE_WIDGET_SETTLE_MS = 350L

private const val IDLE_WIDGET_MIN_DP = 20f

/** Cutouts up to this wide (dp) count as the small circle, the narrowest portrait pill is wider (76 dp). */
internal const val IDLE_WIDGET_COMPACT_MAX_DP = 60f

/** What the collapsed cutout draws for the idle widgets, or null when none is due. */
@Composable
internal fun rememberIdleWidgetFrame(
    settings: SmartIslandSettings,
    noCard: Boolean,
    /**
     * Gets the swipe hook ([IslandInteractionState.idleWidgetCycle]) while a widget is on the pill, null (tests,
     * previews) leaves swiping out
     */
    ui: IslandInteractionState? = null,
    /**
     * Held off the pill on purpose (off after tap to close / long press to hide): no widget, like a switched-off
     * setting
     */
    suppressed: Boolean = false
): IdleWidgetFrame? {
    val screenOn = rememberScreenInteractive()
    val kinds = remember(settings.idleWidgetIds, settings.idleWidgetPinnedFirst) {
        IdleSchedule.ordered(IdleWidgets.parse(settings.idleWidgetIds), settings.idleWidgetPinnedFirst)
    }
    val active = settings.idleWidgetsEnabled && !suppressed && noCard && screenOn
        && settings.width >= IDLE_WIDGET_MIN_DP
    // A low-priority card on the pill takes the idle widget off it, an open widget island under it still shows live
    // values
    val keepData = settings.idleWidgetsEnabled && screenOn && ui?.widgetIslandOpen == true
    val weatherPrefs = remember(
        settings.weatherLocationSource, settings.weatherManualCity, settings.weatherManualLat,
            settings.weatherManualLon,
        settings.weatherRefreshMinutes, settings.weatherProvider, settings.weatherCustomUrl, settings.weatherCustomKey,
        settings.weatherPillMode, settings.weatherIslandReadouts
    ) { settings.weatherPrefs() }
    // battery Saver also sends ticks back to a plain cross-fade, so it is followed whenever a widget is on the pill
    val saverOn by rememberBatterySaverOn(track = active)
    val data = rememberIdleWidgetData(kinds, active || keepData, settings.allowNetworkChecks, weatherPrefs)
    val visible = rememberVisibleWidgets(settings, kinds, data, active)
    val cycleMs = settings.idleWidgetCycleSeconds * 1000L
    // widget island's pause button holds the pill on its widget: no automatic switching (a swipe still steps it)
    val paused = ui?.idleCyclePaused == true || ui?.widgetIslandOpen == true
    val rotates = active && visible.size > 1 && !paused
    val rotation = rememberIdleRotation(active, settings.idleWidgetPinnedFirst.isNotEmpty())
    val appear = animateFloatAsState(
        targetValue = if (active && rotation.settled) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "idleWidgetAppear"
    )
    if (ui != null) BindIdleSwipe(ui, active, visible, data, rotation)
    IdleRotationTimer(rotates, cycleMs, rotation)
    if (!active) return null
    val kind = IdleWidgets.pick(visible, rotation.step) ?: return null
    val country = remember { Locale.getDefault().country }
    val context = LocalContext.current
    val glyphFont = remember(settings.idleGlyphFont) {
        if (settings.idleGlyphFont.isEmpty()) null else islandFontFamily(context, settings.idleGlyphFont)
    }
    val led = rememberLedConfig(settings)
    return IdleWidgetFrame(
        kind = kind,
        data = data,
        fahrenheit = IdleWidgets.useFahrenheit(settings.idleWidgetTempUnit, country),
        compact = settings.width <= IDLE_WIDGET_COMPACT_MAX_DP,
        appear = appear,
        glyphFont = glyphFont,
        glyphScale = settings.idleGlyphScale / 100f,
        autoSwitch = rotation.autoSwitched,
        tickPeriodMs = IdleSlots.tickPeriodMs(settings.idleGlyphTickSeconds),
        led = led,
        windUnit = IdleWidgets.resolveWindUnit(settings.idleWidgetWindUnit, country),
        weatherMode = WeatherPill.normalize(settings.weatherPillMode),
        weatherReadouts = WeatherReadouts.parse(settings.weatherIslandReadouts),
        weatherAnimate = WeatherDisplay.iconAnimates(
            enabled = settings.weatherIconAnimated,
            pauseOnSaver = settings.weatherIconPauseOnSaver,
            saverOn = saverOn,
            systemAnimationsOff = SystemAnimScale.animationsOff()
        ),
        beatOnlyOnChange = settings.idleBeatOnlyOnChange,
        beatPaused = IdleSlots.beatPaused(screenOn, ui?.widgetIslandOpen == true),
        tickAnimates = IdleSlots.ticksAnimate(saverOn, SystemAnimScale.animationsOff()),
        lastSwitchAtMs = rotation.lastSwitchAt,
        cycleStartAtMs = rotation.cycleStartAt,
        cycleMs = if (rotates) cycleMs else 0L
    )
}

/** Widgets taking a turn now: scheduled, with something to show, passing the weather and net speed rules. */
@Composable
private fun rememberVisibleWidgets(
    settings: SmartIslandSettings,
    kinds: List<IdleWidgetKind>,
    data: IdleWidgetData,
    active: Boolean
): List<IdleWidgetKind> {
    // weather's smart display rules (only when notable, hide at night), they only take it out of the rotation
    val weatherRules = remember(
        settings.weatherShowRule, settings.weatherNotableTriggers, settings.weatherHotC, settings.weatherColdC,
        settings.weatherHideAtNight, settings.weatherNightStartHour, settings.weatherNightEndHour
    ) { settings.weatherRules() }
    val localHour by rememberLocalHour(track = active && weatherRules.hideAtNight)
    val weatherShown = WeatherRules.shouldShow(weatherRules, data.weather, localHour)
    // Schedules (work hours, night, charging) decide which widgets take turns, they only take widgets out of the
    // rotation
    val schedulePrefs = settings.idleSchedulePrefs()
    val scheduleContext = rememberIdleScheduleContext(schedulePrefs, track = active)
    val scheduled = IdleSchedule.filter(kinds, schedulePrefs, scheduleContext)
    // net speed only takes a turn while the busier direction is above the chosen threshold (with a little hysteresis)
    val netLatch = remember { booleanArrayOf(false) }
    val netShown = NetSpeedRules.connectionAllows(settings.netSpeedConnection, data.connection) &&
        NetSpeedRules.shouldShow(settings.netSpeedThresholdKbps, data.downBytesPerSecond, data.upBytesPerSecond,
            netLatch[0])
    netLatch[0] = netShown
    // A widget with nothing to show yet (no alarm, weather not loaded, battery unread) sits out of the rotation
    val visible = scheduled.filter { isAvailable(it, data, settings.idleAlarmHideBeyond12h, weatherShown, netShown,
        settings.batteryOnlyWhenCharging) }
        .ifEmpty {
            scheduled.filter {
                it !in NEEDS_CONTENT_KINDS &&
                    !(it == IdleWidgetKind.Weather && !weatherShown) &&
                    !(it == IdleWidgetKind.NetSpeed && !netShown)
            }
        }
    return visible
}

private class IdleRotation {
    var step by mutableIntStateOf(0)
    var settled by mutableStateOf(false)

    /**
     * A swipe moves the rotation by hand, the automatic timer then restarts its count so a widget never flips right
     * after one
     */
    var manualTicks by mutableIntStateOf(0)

    /**
     * Set by the automatic timer just before it steps, cleared by a swipe: the glyph slots then play the three-tick
     * switch
     */
    var autoSwitched by mutableStateOf(false)

    /**
     * When the pill last switched and when the automatic timer's wait began, the beat keeps clear of both
     * (IdleSlots.beatClearOfSwitch)
     */
    var lastSwitchAt by mutableLongStateOf(SystemClock.elapsedRealtime() - 100_000L)
    var cycleStartAt by mutableLongStateOf(SystemClock.elapsedRealtime())
}

@Composable
private fun rememberIdleRotation(active: Boolean, pinnedFirst: Boolean): IdleRotation {
    val rotation = remember { IdleRotation() }
    LaunchedEffect(active) {
        rotation.settled = false
        if (active) {
            // A pinned widget leads: each time the widgets return to the pill the rotation restarts from it
            if (pinnedFirst) rotation.step = 0
            delay(IDLE_WIDGET_SETTLE_MS)
            rotation.settled = true
        }
    }
    return rotation
}

@Composable
@NonRestartableComposable
private fun BindIdleSwipe(
    ui: IslandInteractionState,
    active: Boolean,
    visible: List<IdleWidgetKind>,
    data: IdleWidgetData,
    rotation: IdleRotation
) {
    SideEffect {
        ui.idleWidgetCycle = if (active) idleSwipeHook(ui, visible, data, rotation) else null
    }
    DisposableEffect(ui) { onDispose { ui.idleWidgetCycle = null } }
}

/** A swipe on the pill steps the rotation; true when it moved. */
private fun idleSwipeHook(
    ui: IslandInteractionState,
    visible: List<IdleWidgetKind>,
    data: IdleWidgetData,
    rotation: IdleRotation
): (Boolean) -> Boolean = { forward: Boolean ->
    if (visible.size > 1) {
        val direction = if (forward) 1 else -1
        // With the island open it follows the swipe, so the next widget needs something to show, otherwise the swipe
        // springs back
        val target = if (ui.widgetIslandOpen) {
            nextStepWithContent(rotation.step, direction, visible.size) { index ->
                WidgetIslandContent.hasContent(
                    kind = visible[index],
                    charging = data.charging,
                    nextAlarmMs = data.nextAlarmMs,
                    nowMs = System.currentTimeMillis(),
                    hasWeather = data.weather != null,
                    notificationCount = data.notificationSummary.total,
                    modeActive = data.mode.active
                )
            }
        } else {
            rotation.step + direction
        }
        if (target != null) {
            rotation.step = target
            rotation.autoSwitched = false
            val now = SystemClock.elapsedRealtime()
            rotation.lastSwitchAt = now
            rotation.cycleStartAt = now
            rotation.manualTicks++
            true
        } else {
            false
        }
    } else {
        false
    }
}

@Composable
@NonRestartableComposable
private fun IdleRotationTimer(rotates: Boolean, cycleMs: Long, rotation: IdleRotation) {
    LaunchedEffect(rotates, cycleMs, rotation.manualTicks) {
        if (!rotates) return@LaunchedEffect
        rotation.cycleStartAt = SystemClock.elapsedRealtime()
        while (true) {
            delay(cycleMs)
            rotation.autoSwitched = true
            rotation.step++
            val now = SystemClock.elapsedRealtime()
            rotation.lastSwitchAt = now
            rotation.cycleStartAt = now
        }
    }
}

@Composable
private fun rememberLedConfig(settings: SmartIslandSettings): LedTickConfig? =
    remember(settings.idleTickStyle, settings.ledTickPattern, settings.ledTickPaceMs, settings.ledTickFont,
        settings.ledTickColor, settings.ledTickStagger) {
        if (settings.idleTickStyle == LedTick.STYLE_LED) {
            LedTickConfig(
                pattern = LedTick.parsePattern(settings.ledTickPattern),
                paceMs = LedTick.normalizePace(settings.ledTickPaceMs),
                fontId = settings.ledTickFont,
                colorId = settings.ledTickColor,
                stagger = settings.ledTickStagger
            )
        } else {
            null
        }
    }

private val NEEDS_CONTENT_KINDS = setOf(
    IdleWidgetKind.Clock, IdleWidgetKind.DayMoments, IdleWidgetKind.Battery, IdleWidgetKind.Notifications,
        IdleWidgetKind.Modes
)

private fun isAvailable(
    kind: IdleWidgetKind,
    data: IdleWidgetData,
    hideFarAlarm: Boolean = false,
    weatherShown: Boolean = true,
    netShown: Boolean = true,
    batteryOnlyWhenCharging: Boolean = false
): Boolean = when (kind) {
    IdleWidgetKind.Clock, IdleWidgetKind.DayMoments -> false
    IdleWidgetKind.Notifications -> data.notificationSummary.total > 0
    IdleWidgetKind.Modes -> data.mode.active
    IdleWidgetKind.NextAlarm -> System.currentTimeMillis().let { now ->
        if (hideFarAlarm) WidgetIslandContent.alarmInWindow(data.nextAlarmMs, now) else data.nextAlarmMs > now
    }
    IdleWidgetKind.Weather -> data.weather != null && weatherShown
    IdleWidgetKind.Wind -> (data.weather?.windKmh ?: -1f) >= 0f
    IdleWidgetKind.Battery -> data.batteryPercent >= 0 && (data.charging || !batteryOnlyWhenCharging)
    IdleWidgetKind.NetSpeed -> netShown
    else -> true
}
