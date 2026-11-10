/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.HourChime
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherRefresh
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WeatherSlotFace
import com.agupta07505.smartisland.model.WeatherSlots
import com.agupta07505.smartisland.model.WeatherSnapshot
import com.agupta07505.smartisland.model.cameraBandFor
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleWeather
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import com.agupta07505.smartisland.ui.IslandText as Text

private typealias TickFace = WeatherSlots

private const val WEATHER_TICK_TRAVEL = 0.75f

private const val WEATHER_POLL_MS = 60_000L

private const val WEATHER_GRACE_MS = 60_000L

/** Weather change tick as a takeover, like the hour tick. */
@Composable
internal fun WeatherTickLayer(
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
    cameraBand: CameraBand = cameraBandFor(0f, null, 0f),
    slotSide: SlotSide = SlotSide.Both,
    canHold: Boolean = false
) {
    val context = LocalContext.current
    val settingsNow = rememberUpdatedState(settings)
    val schedulePrefs = settings.idleSchedulePrefs()
    val scheduleContext = rememberIdleScheduleContext(schedulePrefs, track = settings.idleWidgetsEnabled)
    val weatherOn = remember(settings.idleWidgetIds) { IdleWidgetKind.Weather in IdleWidgets.parse(settings.idleWidgetIds) } &&
        IdleSchedule.allows(schedulePrefs, scheduleContext, IdleWidgetKind.Weather)
    val enabled = settings.idleWidgetsEnabled && weatherOn && (canHold || settings.width > IDLE_WIDGET_COMPACT_MAX_DP)
    val screenOn = rememberScreenInteractive()
    val country = remember { Locale.getDefault().country }
    val fahrenheit = IdleWidgets.useFahrenheit(settings.idleWidgetTempUnit, country)
    val networkOk = settings.allowNetworkChecks
    val weatherPrefs = remember(
        settings.weatherLocationSource, settings.weatherManualCity, settings.weatherManualLat,
            settings.weatherManualLon,
        settings.weatherRefreshMinutes, settings.weatherProvider, settings.weatherCustomUrl, settings.weatherCustomKey,
        settings.weatherPillMode, settings.weatherIslandReadouts
    ) { settings.weatherPrefs() }
    val pillMode = weatherPrefs.pillMode
    val weatherRules = remember(
        settings.weatherShowRule, settings.weatherNotableTriggers, settings.weatherHotC, settings.weatherColdC,
        settings.weatherHideAtNight, settings.weatherNightStartHour, settings.weatherNightEndHour
    ) { settings.weatherRules() }
    val localHour by rememberLocalHour(track = enabled && weatherRules.hideAtNight)

    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var change by remember { mutableStateOf<Pair<TickFace, TickFace>?>(null) }
    val tick = remember { Animatable(0f) }
    // Battery Saver or "remove animations"
    val saverOn by rememberBatterySaverOn(track = enabled)
    val animatesNow = rememberUpdatedState(IdleSlots.ticksAnimate(saverOn, SystemAnimScale.animationsOff()))
    var moves by remember { mutableStateOf(true) }
    val fahrenheitNow = rememberUpdatedState(fahrenheit)

    // Keeps the latest stored reading
    LaunchedEffect(enabled, screenOn, networkOk, weatherPrefs) {
        if (!enabled || !screenOn) return@LaunchedEffect
        while (true) {
            var latest = IdleWeather.cachedFor(context, weatherPrefs)
            val age = System.currentTimeMillis() - (latest?.fetchedAtMs ?: 0L)
            // Manual refresh (0) never fetches here once there is a reading
            val due = if (weatherPrefs.refreshMs > 0L) weatherPrefs.refreshMs + WEATHER_GRACE_MS else 0L
            if (networkOk && WeatherRefresh.shouldFetch(due, latest != null, age, missingData = false)) {
                IdleWeather.fetch(context, weatherPrefs)?.let { latest = it }
            }
            if (latest != null) snapshot = latest
            delay(WEATHER_POLL_MS)
        }
    }

    fun faceOf(s: WeatherSnapshot, f: Boolean, mode: String): TickFace = WeatherDisplay.slots(mode, s, f)

    var lastFace by remember { mutableStateOf<TickFace?>(null) }
    var lastMode by remember { mutableStateOf(pillMode) }
    LaunchedEffect(snapshot, fahrenheit, enabled, pillMode, weatherRules, localHour) {
        val s = snapshot
        if (!enabled || s == null || !WeatherRules.shouldShow(weatherRules, s, localHour)) {
            lastFace = null
            return@LaunchedEffect
        }
        val face = faceOf(s, fahrenheitNow.value, pillMode)
        val old = lastFace
        lastFace = face
        // Choosing another display in Settings is not a change in the weather
        if (lastMode != pillMode) {
            lastMode = pillMode
            return@LaunchedEffect
        }
        if (old == null || old == face) return@LaunchedEffect
        if (ui.idleFrameNow?.kind == IdleWidgetKind.Weather) return@LaunchedEffect
        try {
            ui.withTickPill(canHold, { settingsNow.value.width > IDLE_WIDGET_COMPACT_MAX_DP }) {
                while (ui.chimeShow.value > 0.001f) delay(100)
                moves = animatesNow.value
                change = old to face
                tick.snapTo(0f)
                ui.chimeShow.animateTo(1f, tween(animSpeed.durationMs(HourChime.FADE_IN_MS)))
                delay(animSpeed.durationMs(HourChime.HOLD_BEFORE_MS).toLong())
                // the tick's clock runs linearly
                val span = if (moves) IdleSlots.takeoverSpanMs(HourChime.TICK_MS) else IdleSlots.PLAIN_FADE_MS
                tick.animateTo(1f, tween(animSpeed.durationMs(span), easing = LinearEasing))
                delay(animSpeed.durationMs(HourChime.HOLD_AFTER_MS).toLong())
                ui.chimeShow.animateTo(0f, tween(animSpeed.durationMs(HourChime.FADE_OUT_MS)))
                change = null
            }
        } finally {
            withContext(NonCancellable) {
                if (change != null) ui.chimeShow.snapTo(0f)
                change = null
            }
        }
    }

    val (old, new) = change ?: return
    val glyphFont = remember(settings.idleGlyphFont) {
        if (settings.idleGlyphFont.isEmpty()) null else islandFontFamily(context, settings.idleGlyphFont)
    }
    val glyphScale = settings.idleGlyphScale / 100f
    IdleCameraRow(
        pillWidthDp = settings.width,
        band = cameraBand,
        side = slotSide,
        modifier = modifier
            .width(settings.width.dp)
            .height(settings.height.dp)
            .graphicsLayer { alpha = ui.chimeShow.value },
        // left: the old face leaves downwards, the new one comes in from above
        first = { slotModifier ->
            TickSlot(slotModifier, towardsDown = true, lagged = false, moves = moves, tick = tick,
                old = { TickFaceView(old.left, glyphFont, glyphScale) },
                new = { TickFaceView(new.left, glyphFont, glyphScale) })
        },
        // right: the old face leaves upwards, the new one comes in from below
        second = { slotModifier ->
            TickSlot(slotModifier, towardsDown = false, lagged = true, moves = moves, tick = tick,
                old = { TickFaceView(old.right, glyphFont, glyphScale) },
                new = { TickFaceView(new.right, glyphFont, glyphScale) })
        }
    )
}

@Composable
private fun TickFaceView(face: WeatherSlotFace, family: FontFamily?, scale: Float) {
    when (face) {
        WeatherSlotFace.Empty -> Unit
        is WeatherSlotFace.Icon ->
            Icon(weatherIcon(face.look, face.isDay).first, null, tint = Color.White,
                modifier = Modifier.size((15f * scale).dp))
        is WeatherSlotFace.Text -> TickText(face.text, family, scale)
    }
}

@Composable
private fun TickText(text: String, family: FontFamily?, scale: Float) {
    Text(text = text, color = Color.White, fontSize = (11f * scale).sp, fontFamily = family,
        fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}

@Composable
private fun TickSlot(
    modifier: Modifier,
    towardsDown: Boolean,
    /** Right slot: it starts [IdleSlots.STAGGER_MS] after the left one. */
    lagged: Boolean,
    /** False: no move, a plain cross-fade ([IdleSlots.ticksAnimate]). */
    moves: Boolean,
    tick: Animatable<Float, AnimationVector1D>,
    old: @Composable () -> Unit,
    new: @Composable () -> Unit
) {
    val dir = if (towardsDown) 1f else -1f
    fun progress() = if (moves) IdleSlots.takeoverProgress(tick.value, HourChime.TICK_MS, lagged) else tick.value
    fun stretch() = if (moves) IdleSlots.takeoverStretch(tick.value, HourChime.TICK_MS, lagged) else 0f
    Box(modifier = modifier.fillMaxHeight().clipToBounds(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.graphicsLayer {
            val p = progress()
            val s = stretch()
            translationY = if (moves) dir * p * size.height * WEATHER_TICK_TRAVEL else 0f
            scaleY = 1f + IdleSlots.STRETCH_SCALE * s
            alpha = (1f - p.coerceIn(0f, 1f)) * (1f - IdleSlots.STRETCH_FADE * s)
        }) { old() }
        Box(modifier = Modifier.graphicsLayer {
            val p = progress()
            val s = stretch()
            translationY = if (moves) -dir * (1f - p) * size.height * WEATHER_TICK_TRAVEL else 0f
            scaleY = 1f + IdleSlots.STRETCH_SCALE * s
            alpha = p.coerceIn(0f, 1f) * (1f - IdleSlots.STRETCH_FADE * s)
        }) { new() }
    }
}
