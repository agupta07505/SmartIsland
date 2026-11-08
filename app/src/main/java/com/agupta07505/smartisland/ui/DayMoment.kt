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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.DayMoment
import com.agupta07505.smartisland.model.DayMoments
import com.agupta07505.smartisland.model.GlyphPose
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.MomentGlyph
import com.agupta07505.smartisland.model.MomentMotion
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.model.cameraBandFor
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun dayMomentProgress(clock: Float, moves: Boolean): Float =
    if (moves) IdleSlots.takeoverProgress(clock, DayMoments.TICK_MS, lagged = false) else clock

private fun landed(pose: GlyphPose, moment: DayMoment, clock: Float, moves: Boolean, incoming: Boolean): GlyphPose {
    if (!moves) return pose
    val travels = DayMoments.motion(moment) != MomentMotion.GrowFromPrevious
    val stretch = if (travels) IdleSlots.takeoverStretch(clock, DayMoments.TICK_MS, lagged = false) else 0f
    val past = if (incoming) (IdleSlots.takeoverProgress(clock, DayMoments.TICK_MS, lagged = false) - 1f)
        .coerceAtLeast(0f) else 0f
    return pose.copy(
        scale = pose.scale * (1f + 2f * past),
        alpha = pose.alpha * (1f - IdleSlots.STRETCH_FADE * stretch),
        stretch = 1f + IdleSlots.STRETCH_SCALE * stretch
    )
}

@Composable
private fun MomentGlyphView(moment: DayMoment, glyphScale: Float, poseOf: () -> GlyphPose) {
    val size = Modifier.size((16f * glyphScale).dp)
    val layer: GraphicsLayerScope.() -> Unit = {
        val pose = poseOf()
        translationY = pose.offsetShare * this.size.height
        scaleX = pose.scale
        scaleY = pose.scale * pose.stretch
        alpha = pose.alpha
    }
    when (DayMoments.glyph(moment)) {
        MomentGlyph.Horizon -> HorizonSunGlyph(lift = poseOf().lift, modifier = size.graphicsLayer(layer))
        MomentGlyph.Sun -> Icon(Icons.Rounded.WbSunny, null, tint = Color.White, modifier = size.graphicsLayer(layer))
        MomentGlyph.Moon -> Icon(Icons.Rounded.NightsStay, null, tint = Color.White,
            modifier = size.graphicsLayer(layer))
    }
}

/** Day moments widget: at the phone's local 6:00, 12:00. */
@Composable
internal fun DayMomentLayer(
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
    cameraBand: CameraBand = cameraBandFor(0f, null, 0f),
    slotSide: SlotSide = SlotSide.Both,
    suppressed: Boolean = false,
    canHold: Boolean = false
) {
    val kinds = remember(settings.idleWidgetIds) { IdleWidgets.parse(settings.idleWidgetIds) }
    val schedulePrefs = settings.idleSchedulePrefs()
    val scheduleContext = rememberIdleScheduleContext(schedulePrefs, track = settings.idleWidgetsEnabled)
    val momentsOn = IdleWidgetKind.DayMoments in kinds && IdleSchedule.allows(schedulePrefs, scheduleContext,
        IdleWidgetKind.DayMoments)
    val clockOn = IdleWidgetKind.Clock in kinds && IdleSchedule.allows(schedulePrefs, scheduleContext,
        IdleWidgetKind.Clock)
    val enabled = settings.idleWidgetsEnabled && !suppressed && momentsOn
        && (canHold || settings.width > IDLE_WIDGET_COMPACT_MAX_DP)
    val settingsNow = rememberUpdatedState(settings)
    val screenOn = rememberScreenInteractive()
    val tick = remember { Animatable(0f) }
    var shown by remember { mutableStateOf<DayMoment?>(null) }
    // Battery Saver or "remove animations"
    val saverOn by rememberBatterySaverOn(track = enabled)
    val animatesNow = rememberUpdatedState(IdleSlots.ticksAnimate(saverOn, SystemAnimScale.animationsOff()))
    var moves by remember { mutableStateOf(true) }

    LaunchedEffect(enabled, screenOn) {
        if (!enabled || !screenOn) return@LaunchedEffect
        try {
            while (true) {
                val (next, at) = DayMoments.next(System.currentTimeMillis())
                delay((at - System.currentTimeMillis()).coerceAtLeast(0L))
                ui.withTickPill(canHold, { settingsNow.value.width > IDLE_WIDGET_COMPACT_MAX_DP }) {
                    // 6:00, 12:00 and 23:00 are full hours: the hour tick has the pill first
                    if (clockOn && next.minute == 0) delay(DayMoments.AFTER_HOUR_TICK_MS)
                    while (ui.chimeShow.value > 0.001f) delay(100)
                    moves = animatesNow.value
                    shown = next
                    tick.snapTo(0f)
                    ui.chimeShow.animateTo(1f, tween(animSpeed.durationMs(DayMoments.FADE_IN_MS)))
                    delay(animSpeed.durationMs(DayMoments.HOLD_BEFORE_MS).toLong())
                    // the tick's clock runs linearly
                    val span = if (moves) IdleSlots.takeoverSpanMs(DayMoments.TICK_MS) else IdleSlots.PLAIN_FADE_MS
                    tick.animateTo(1f, tween(animSpeed.durationMs(span), easing = LinearEasing))
                    delay(animSpeed.durationMs(DayMoments.HOLD_AFTER_MS).toLong())
                    ui.chimeShow.animateTo(0f, tween(animSpeed.durationMs(DayMoments.FADE_OUT_MS)))
                    shown = null
                }
            }
        } finally {
            // Cancelled half way (screen off, the setting switched off)
            withContext(NonCancellable) {
                ui.chimeShow.snapTo(0f)
                shown = null
            }
        }
    }

    val moment = shown ?: return
    val glyphScale = settings.idleGlyphScale / 100f
    // The empty middle follows the camera band; the glyph keeps the strip beside it
    IdleCameraRow(
        pillWidthDp = settings.width,
        band = cameraBand,
        side = slotSide,
        modifier = modifier
            .width(settings.width.dp)
            .height(settings.height.dp)
            .graphicsLayer { alpha = ui.chimeShow.value },
        first = { slotModifier ->
        Box(modifier = slotModifier.clipToBounds(), contentAlignment = Alignment.Center) {
            MomentGlyphView(DayMoments.previous(moment), glyphScale) {
                val p = dayMomentProgress(tick.value, moves)
                landed(DayMoments.outgoingPose(moment, p), moment, tick.value, moves, incoming = false)
            }
            // ... and the new one comes up from behind the horizon, grows out of the old one
            MomentGlyphView(moment, glyphScale) {
                val p = dayMomentProgress(tick.value, moves)
                landed(DayMoments.incomingPose(moment, p), moment, tick.value, moves, incoming = true)
            }
        }
        }
    )
}
