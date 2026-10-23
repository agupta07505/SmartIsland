/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.util.IslandTimings
import com.agupta07505.smartisland.util.SystemAnimScale
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Turns the plugged-in ring makes: once for a short card, twice for a long one (one turn per ~3 s of card time, 1..2). */
fun pluggedInTurns(cardMs: Long): Int = (cardMs / 3000f).roundToInt().coerceIn(1, 2)

/** Length of one turn: at most 1.5 s, and short enough that all the turns finish inside the card's time. */
fun pluggedInTurnMs(cardMs: Long): Int =
    minOf(MAX_TURN_MS, cardMs / pluggedInTurns(cardMs)).toInt().coerceAtLeast(MIN_TURN_MS)

fun batterySwellScale(q: Float): Float = 1f + (SWELL_PEAK - 1f) * sin(PI.toFloat() * q.coerceIn(0f, 1f))

private const val MAX_TURN_MS = 1500L
private const val MIN_TURN_MS = 400
private const val SWELL_MS = 1100
private const val SWELL_PEAK = 1.2f
private const val FILL_SHARE = 0.85f

private const val GRID = 48f
private const val RING_RADIUS = 20.4f
private const val RING_STROKE = 3.2f
private const val DISC_RADIUS = 16.8f

private val DiscColor = Color.White.copy(alpha = 0.16f)
private val TrackColor = Color(0x33FFFFFF)
private val NoPulse: State<Float> = mutableFloatStateOf(1f)

/**
 * The battery event as it sits in a cutout: a grey disc with the state icon, inside one connected ring that shows the
 * level (a faint track with the level drawn over it, from 12 o'clock, clockwise).
 */
@Composable
fun BatteryCutoutBadge(
    visual: BatteryVisual,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 24.dp,
    cardMs: Long = IslandTimings.PLUGGED_IN_CARD_MS
) {
    val turns = pluggedInTurns(cardMs)
    val spin = remember(visual) { Animatable(0f) }
    val swell = remember(visual) { Animatable(0f) }
    val fill = remember(visual) { Animatable(0f) }

    LaunchedEffect(visual, cardMs) {
        val off = SystemAnimScale.animationsOff()
        when (visual) {
            BatteryVisual.PluggedIn -> {
                val end = 360f * turns
                if (off) spin.snapTo(end)
                else spin.animateTo(end, tween(turns * pluggedInTurnMs(cardMs), easing = FastOutSlowInEasing))
            }
            BatteryVisual.Full -> {
                if (off) {
                    fill.snapTo(1f)
                } else {
                    coroutineScope {
                        launch { swell.animateTo(1f, tween(SWELL_MS, easing = LinearEasing)) }
                        fill.animateTo(1f, tween((SWELL_MS * FILL_SHARE).toInt(), easing = FastOutSlowInEasing))
                    }
                }
            }
            BatteryVisual.Low, BatteryVisual.Saver -> {
                if (!off) swell.animateTo(1f, tween(SWELL_MS, easing = LinearEasing))
            }
            else -> Unit
        }
    }

    val level = if (visual == BatteryVisual.Full) 1f else progress.coerceIn(0f, 1f)
    val drawsBattery = visual == BatteryVisual.Full || visual == BatteryVisual.Low || visual == BatteryVisual.Saver

    Box(modifier = modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(boxSize)) {
            // Animated values are read here, in the draw step, so the frames never recompose the card.
            val k = size.minDimension / GRID
            drawCircle(color = DiscColor, radius = DISC_RADIUS * k)
            rotate(degrees = spin.value, pivot = center) {
                drawRing(level = level, color = color, k = k)
            }
            if (drawsBattery) {
                val s = batterySwellScale(swell.value)
                scale(scale = s, pivot = center) {
                    drawBatteryGlyph(visual = visual, color = color, fill = fill.value, k = k)
                }
            }
        }
        if (!drawsBattery) {
            // The bolt: it breathes when plugged in and drains when unplugged.
            BatteryStateIcon(
                visual = visual,
                color = color,
                pulse = NoPulse,
                iconSize = boxSize * 0.58f,
                size = boxSize
            )
        }
    }
}

private fun DrawScope.drawRing(level: Float, color: Color, k: Float) {
    val r = RING_RADIUS * k
    val style = Stroke(width = RING_STROKE * k, cap = StrokeCap.Round)
    drawCircle(color = TrackColor, radius = r, style = Stroke(width = RING_STROKE * k))
    val sweep = 360f * level
    if (sweep < 1f) return
    if (level >= 0.999f) {
        drawCircle(color = color, radius = r, style = Stroke(width = RING_STROKE * k))
    } else {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2f, r * 2f),
            style = style
        )
    }
}

private fun DrawScope.drawBatteryGlyph(visual: BatteryVisual, color: Color, fill: Float, k: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(13.8f * k, 17f * k),
        size = Size(19f * k, 14f * k),
        cornerRadius = CornerRadius(1.4f * k),
        style = Stroke(width = 2.2f * k)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(33.6f * k, 21.2f * k),
        size = Size(2.4f * k, 5.6f * k),
        cornerRadius = CornerRadius(0.6f * k)
    )
    when (visual) {
        BatteryVisual.Full -> if (fill > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(16.2f * k, 19.4f * k),
                size = Size(14.2f * k * fill, 9.2f * k),
                cornerRadius = CornerRadius(0.5f * k)
            )
        }
        BatteryVisual.Low -> {
            drawRoundRect(color, Offset(22.4f * k, 20.4f * k), Size(2.2f * k, 5f * k), CornerRadius(0.5f * k))
            drawRoundRect(color, Offset(22.4f * k, 26.6f * k), Size(2.2f * k, 2f * k), CornerRadius(0.5f * k))
        }
        BatteryVisual.Saver -> {
            drawLine(color, Offset(23.3f * k, 20.6f * k), Offset(23.3f * k, 27.8f * k), strokeWidth = 2.2f * k)
            drawLine(color, Offset(19.7f * k, 24.2f * k), Offset(26.9f * k, 24.2f * k), strokeWidth = 2.2f * k)
        }
        else -> Unit
    }
}
