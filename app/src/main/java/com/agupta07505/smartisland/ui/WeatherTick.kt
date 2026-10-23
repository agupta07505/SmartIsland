/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.HourChime
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.PILL_CENTER_ZONE_SHARE
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherRefresh
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WeatherSlotFace
import com.agupta07505.smartisland.model.WeatherSlots
import com.agupta07505.smartisland.model.WeatherSnapshot
import com.agupta07505.smartisland.ui.IslandText as Text
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleWeather
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private typealias TickFace = WeatherSlots

private const val WEATHER_TICK_TRAVEL = 0.75f

private const val WEATHER_POLL_MS = 60_000L

private const val WEATHER_GRACE_MS = 60_000L

/**
 * The weather change tick as a takeover, like the hour tick: whenever the weather's condition or temperature changes,
 * the pill's content gives way for a moment and the left slot ticks from the old icon to the new one (downwards), the
 * right slot from the old temperature to the new one (upwards), at the slow pace of the hour tick.
 */
@Composable
internal fun WeatherTickLayer(
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
    /** Landscape: the pill may be brought in for the tick although a long press hid it ([withTickPill]). */
    canHold: Boolean = false
) {
    val context = LocalContext.current
    val settingsNow = androidx.compose.runtime.rememberUpdatedState(settings)
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
        settings.weatherLocationSource, settings.weatherManualCity, settings.weatherManualLat, settings.weatherManualLon,
        settings.weatherRefreshMinutes, settings.weatherProvider, settings.weatherCustomUrl, settings.weatherCustomKey,
        settings.weatherPillMode, settings.weatherIslandReadouts
    ) { settings.weatherPrefs() }
    val pillMode = weatherPrefs.pillMode
    // The smart display rules: while they keep the weather off the pill, its change tick stays quiet too.
    val weatherRules = remember(
        settings.weatherShowRule, settings.weatherNotableTriggers, settings.weatherHotC, settings.weatherColdC,
        settings.weatherHideAtNight, settings.weatherNightStartHour, settings.weatherNightEndHour
    ) { settings.weatherRules() }
    val localHour by rememberLocalHour(track = enabled && weatherRules.hideAtNight)

    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var change by remember { mutableStateOf<Pair<TickFace, TickFace>?>(null) }
    val tick = remember { Animatable(0f) }
    // Battery Saver or "remove animations": the tick is a plain cross-fade (IdleSlots.ticksAnimate), decided when it starts.
    val saverOn by rememberBatterySaverOn(track = enabled)
    val animatesNow = androidx.compose.runtime.rememberUpdatedState(IdleSlots.ticksAnimate(saverOn, SystemAnimScale.animationsOff()))
    var moves by remember { mutableStateOf(true) }
    val fahrenheitNow = androidx.compose.runtime.rememberUpdatedState(fahrenheit)

    // Keeps the latest stored reading; refreshes it only when it is stale and the pill's own widget has not (it asks first).
    LaunchedEffect(enabled, screenOn, networkOk, weatherPrefs) {
        if (!enabled || !screenOn) return@LaunchedEffect
        while (true) {
            var latest = IdleWeather.cachedFor(context, weatherPrefs)
            val age = System.currentTimeMillis() - (latest?.fetchedAtMs ?: 0L)
            // Manual refresh (0) never fetches here once there is a reading; otherwise this waits a grace period
            // behind the pill's own lookup.
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
        // Choosing another display in Settings is not a change in the weather: no tick for it.
        if (lastMode != pillMode) {
            lastMode = pillMode
            return@LaunchedEffect
        }
        if (old == null || old == face) return@LaunchedEffect
        // The pill's own weather widget ticks by itself while it is on the pill; nothing to add then.
        if (ui.idleFrameNow?.kind == IdleWidgetKind.Weather) return@LaunchedEffect
        try {
            ui.withTickPill(canHold, { settingsNow.value.width > IDLE_WIDGET_COMPACT_MAX_DP }) {
                while (ui.chimeShow.value > 0.001f) delay(100)
                moves = animatesNow.value
                change = old to face
                tick.snapTo(0f)
                ui.chimeShow.animateTo(1f, tween(animSpeed.durationMs(HourChime.FADE_IN_MS)))
                delay(animSpeed.durationMs(HourChime.HOLD_BEFORE_MS).toLong())
                // The tick's clock runs linearly; each slot works out its own heavy tick, settle and stretch from it
                // (the temperature a beat late).
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
    val slot = (1f - PILL_CENTER_ZONE_SHARE) / 2f
    Row(
        modifier = modifier
            .width(settings.width.dp)
            .height(settings.height.dp)
            .graphicsLayer { alpha = ui.chimeShow.value },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: the old face leaves downwards, the new one comes in from above.
        TickSlot(Modifier.weight(slot), towardsDown = true, lagged = false, moves = moves, tick = tick,
            old = { TickFaceView(old.left, glyphFont, glyphScale) },
            new = { TickFaceView(new.left, glyphFont, glyphScale) })
        Spacer(Modifier.weight(PILL_CENTER_ZONE_SHARE))
        // Right: the old face leaves upwards, the new one comes in from below.
        TickSlot(Modifier.weight(slot), towardsDown = false, lagged = true, moves = moves, tick = tick,
            old = { TickFaceView(old.right, glyphFont, glyphScale) },
            new = { TickFaceView(new.right, glyphFont, glyphScale) })
    }
}

@Composable
private fun TickFaceView(face: WeatherSlotFace, family: androidx.compose.ui.text.font.FontFamily?, scale: Float) {
    when (face) {
        WeatherSlotFace.Empty -> Unit
        is WeatherSlotFace.Icon ->
            Icon(weatherIcon(face.look, face.isDay).first, null, tint = Color.White, modifier = Modifier.size((15f * scale).dp))
        is WeatherSlotFace.Text -> TickText(face.text, family, scale)
    }
}

@Composable
private fun TickText(text: String, family: androidx.compose.ui.text.font.FontFamily?, scale: Float) {
    Text(text = text, color = Color.White, fontSize = (11f * scale).sp, fontFamily = family, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}

@Composable
private fun TickSlot(
    modifier: Modifier,
    towardsDown: Boolean,
    /** The right slot: it starts [IdleSlots.STAGGER_MS] after the left one. */
    lagged: Boolean,
    /** False: no move, a plain cross-fade ([IdleSlots.ticksAnimate]). */
    moves: Boolean,
    tick: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    old: @Composable () -> Unit,
    new: @Composable () -> Unit
) {
    val dir = if (towardsDown) 1f else -1f
    // Progress runs a little past 1 in the settle (the face lands past its place and eases back); the stretch follows the snap's speed.
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
