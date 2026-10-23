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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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
import com.agupta07505.smartisland.model.HourChimeLabels
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.PILL_CENTER_ZONE_SHARE
import com.agupta07505.smartisland.model.hourChimeLabels
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val CHIME_TRAVEL = 0.75f

/** The clock idle widget's hourly tick. */
@Composable
internal fun HourChimeLayer(
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
    /** The idle widgets are held off the pill: no hourly tick either. */
    suppressed: Boolean = false,
    /** Landscape: the pill may be brought in for the tick although a long press hid it ([withTickPill]). */
    canHold: Boolean = false
) {
    val context = LocalContext.current
    val schedulePrefs = settings.idleSchedulePrefs()
    val scheduleContext = rememberIdleScheduleContext(schedulePrefs, track = settings.idleWidgetsEnabled)
    val clockOn = remember(settings.idleWidgetIds) { IdleWidgetKind.Clock in IdleWidgets.parse(settings.idleWidgetIds) } &&
        IdleSchedule.allows(schedulePrefs, scheduleContext, IdleWidgetKind.Clock)
    val enabled = settings.idleWidgetsEnabled && !suppressed && clockOn && (canHold || settings.width > IDLE_WIDGET_COMPACT_MAX_DP)
    val settingsNow = androidx.compose.runtime.rememberUpdatedState(settings)
    val screenOn = rememberScreenInteractive()
    val tick = remember { Animatable(0f) }
    var labels by remember { mutableStateOf<HourChimeLabels?>(null) }
    // Battery Saver or "remove animations": the tick is a plain cross-fade (IdleSlots.ticksAnimate), decided when it starts.
    val saverOn by rememberBatterySaverOn(track = enabled)
    val animatesNow = androidx.compose.runtime.rememberUpdatedState(IdleSlots.ticksAnimate(saverOn, SystemAnimScale.animationsOff()))
    var moves by remember { mutableStateOf(true) }

    LaunchedEffect(enabled, screenOn) {
        if (!enabled || !screenOn) return@LaunchedEffect
        try {
            while (true) {
                val target = HourChime.nextFullHourMs(System.currentTimeMillis())
                delay((target - System.currentTimeMillis()).coerceAtLeast(0L))
                // Landscape with the pill hidden: the pill comes in just for the tick (see withTickPill).
                ui.withTickPill(canHold, { settingsNow.value.width > IDLE_WIDGET_COMPACT_MAX_DP }) {
                    val is24 = android.text.format.DateFormat.is24HourFormat(context)
                    labels = hourChimeLabels(HourChime.hourOf(target), is24)
                    moves = animatesNow.value
                    tick.snapTo(0f)
                    ui.chimeShow.animateTo(1f, tween(animSpeed.durationMs(HourChime.FADE_IN_MS)))
                    delay(animSpeed.durationMs(HourChime.HOLD_BEFORE_MS).toLong())
                    // The tick's clock runs linearly; each slot works out its own heavy tick, settle and stretch from
                    // it (the minutes a beat late).
                    val span = if (moves) IdleSlots.takeoverSpanMs(HourChime.TICK_MS) else IdleSlots.PLAIN_FADE_MS
                    tick.animateTo(1f, tween(animSpeed.durationMs(span), easing = LinearEasing))
                    delay(animSpeed.durationMs(HourChime.HOLD_AFTER_MS).toLong())
                    ui.chimeShow.animateTo(0f, tween(animSpeed.durationMs(HourChime.FADE_OUT_MS)))
                    labels = null
                }
            }
        } finally {
            // Cancelled half way (screen off, the island expands, the setting is switched off): the pill's own content must come back.
            withContext(NonCancellable) {
                ui.chimeShow.snapTo(0f)
                labels = null
            }
        }
    }

    val shown = labels ?: return
    // The hour tick uses the glyph font and size set for the idle widgets, so it matches the glyphs it replaces.
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
        // Hour: the old one leaves downwards, the new one comes in from above.
        ChimeSlot(Modifier.weight(slot), shown.oldHour, shown.newHour, towardsDown = true, lagged = false, moves = moves, tick = tick, family = glyphFont, scale = glyphScale)
        Spacer(Modifier.weight(PILL_CENTER_ZONE_SHARE))
        // Minutes: "59" leaves upwards, "00" comes in from below.
        ChimeSlot(Modifier.weight(slot), HourChime.MINUTES_BEFORE, HourChime.MINUTES_AFTER, towardsDown = false, lagged = true, moves = moves, tick = tick, family = glyphFont, scale = glyphScale)
    }
}

@Composable
private fun ChimeSlot(
    modifier: Modifier,
    old: String,
    new: String,
    towardsDown: Boolean,
    /** The minutes slot: it starts [IdleSlots.STAGGER_MS] after the hour. */
    lagged: Boolean,
    /** False: no move, a plain cross-fade ([IdleSlots.ticksAnimate]). */
    moves: Boolean,
    tick: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    family: androidx.compose.ui.text.font.FontFamily? = null,
    scale: Float = 1f
) {
    val dir = if (towardsDown) 1f else -1f
    // Progress runs a little past 1 in the settle (the glyph lands past its place and eases back); the stretch follows the snap's speed.
    fun progress() = if (moves) IdleSlots.takeoverProgress(tick.value, HourChime.TICK_MS, lagged) else tick.value
    fun stretch() = if (moves) IdleSlots.takeoverStretch(tick.value, HourChime.TICK_MS, lagged) else 0f
    Box(modifier = modifier.fillMaxHeight().clipToBounds(), contentAlignment = Alignment.Center) {
        ChimeGlyph(old, family, scale) {
            val p = progress()
            val s = stretch()
            translationY = if (moves) dir * p * size.height * CHIME_TRAVEL else 0f
            scaleY = 1f + IdleSlots.STRETCH_SCALE * s
            alpha = (1f - p.coerceIn(0f, 1f)) * (1f - IdleSlots.STRETCH_FADE * s)
        }
        ChimeGlyph(new, family, scale) {
            val p = progress()
            val s = stretch()
            translationY = if (moves) -dir * (1f - p) * size.height * CHIME_TRAVEL else 0f
            scaleY = 1f + IdleSlots.STRETCH_SCALE * s
            alpha = p.coerceIn(0f, 1f) * (1f - IdleSlots.STRETCH_FADE * s)
        }
    }
}

@Composable
private fun ChimeGlyph(text: String, family: androidx.compose.ui.text.font.FontFamily?, scale: Float, layer: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit) {
    Text(
        text = text,
        color = Color.White,
        fontSize = (12f * scale).sp,
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.graphicsLayer(block = layer)
    )
}
