/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

enum class BatteryVisual { PluggedIn, PluggedOut, Full, Low, Saver }

/** Category decides, title / percentage heuristics are only a fallback. */
fun resolveBatteryVisual(category: String?, title: String?, pct: Float): BatteryVisual = when (category) {
    "battery_plugged_in" -> BatteryVisual.PluggedIn
    "battery_plugged_out" -> BatteryVisual.PluggedOut
    "battery_full" -> BatteryVisual.Full
    "battery_saver" -> BatteryVisual.Saver
    "battery_low" -> BatteryVisual.Low
    else -> {
        val t = title?.lowercase().orEmpty()
        when {
            t.contains("saver") -> BatteryVisual.Saver
            t.contains("low") || pct <= 20f -> BatteryVisual.Low
            else -> BatteryVisual.PluggedIn
        }
    }
}

private val LowRed = Color(0xFFFF4D5A)
private val SaverAmber = Color(0xFFFFB81F)

/** User's colour pushed to near full brightness, hue kept. */
fun batteryVisualColor(visual: BatteryVisual, userColor: Color): Color = when (visual) {
    BatteryVisual.Saver -> SaverAmber
    BatteryVisual.Low, BatteryVisual.PluggedOut -> LowRed
    else -> vivid(userColor)
}

/** [batteryVisualColor], or the colour the user gave the event ([overrideColor]) when it has one of its own. */
fun batteryVisualColor(visual: BatteryVisual, userColor: Color, overrideColor: Long?): Color =
    overrideColor?.let { Color(it) } ?: batteryVisualColor(visual, userColor)

/** Hue kept, a touch more saturated, brightness raised to at least [minValue] (HSV), alpha kept. */
fun vivid(color: Color, minValue: Float = 0.96f): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (max <= 0f) return color
    val saturation = if (max == 0f) 0f else delta / max
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta).mod(6f)
        max == g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    }
    val s = if (saturation < 0.05f) saturation else (saturation * 1.05f).coerceAtMost(1f)
    val v = maxOf(max, minValue)
    val c = v * s
    val x = c * (1f - abs(hue.mod(2f) - 1f))
    val m = v - c
    val (r1, g1, b1) = when (hue.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m, color.alpha)
}

private val SolidPulse: State<Float> = mutableFloatStateOf(1f)

/** Low fades softly, Full and PluggedIn glow slowly, the rest stay solid. */
@Composable
fun rememberBatteryPulse(visual: BatteryVisual): State<Float> {
    val transition = rememberInfiniteTransition(label = "batteryPulse")
    return when (visual) {
        BatteryVisual.Low, BatteryVisual.PluggedOut -> transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fade"
        )
        BatteryVisual.Full, BatteryVisual.PluggedIn -> transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
        else -> SolidPulse
    }
}
