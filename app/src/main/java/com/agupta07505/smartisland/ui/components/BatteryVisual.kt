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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

/**
 * The five looks the battery card can take. Every state has its own icon and colour, and every motion is slow and
 * soft so the card stays calm.
 */
enum class BatteryVisual { PluggedIn, PluggedOut, Full, Low, Saver }

/** The notification category decides the state; title/percentage heuristics are only a fallback. */
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

/** Battery colours are shown vivid: the user's colour pushed to (nearly) full brightness, hue kept. */
fun batteryVisualColor(visual: BatteryVisual, userColor: Color): Color = when (visual) {
    BatteryVisual.Saver -> SaverAmber
    BatteryVisual.Low, BatteryVisual.PluggedOut -> LowRed
    else -> vivid(userColor)
}

/** [color] with its hue kept, a touch more saturated and brightness raised to at least [minValue] (HSV). Alpha is kept. */
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
    val x = c * (1f - kotlin.math.abs(hue.mod(2f) - 1f))
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

/** Alpha for ring, icon and bar: Low fades softly, Full and PluggedIn glow slowly, the rest stay solid. */
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

@Composable
private fun OutwardWaves(color: Color, boxSize: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "outwardRipple")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4200
                0f at 0 using FastOutSlowInEasing
                1f at 2800
                1f at 4200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleTime"
    )
    Canvas(modifier = modifier.size(boxSize)) {
        // Read here, in the draw step, so the ripple's frames never recompose the card.
        val t = t
        if (t >= 1f) return@Canvas
        val start = size.minDimension * 0.22f
        val end = size.minDimension * 0.85f
        drawCircle(
            color = color.copy(alpha = (1f - t) * 0.45f),
            radius = start + (end - start) * t,
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

/**
 * Ring + state icon as one unit. [disc] adds the tinted circle behind the icon (expanded card); the collapsed pill
 * goes without. [pulse] is read in graphicsLayer blocks only.
 */
@Composable
fun BatteryBadge(
    visual: BatteryVisual,
    progress: Float,
    color: Color,
    pulse: State<Float>,
    boxSize: Dp,
    iconSize: Dp,
    disc: Boolean,
    modifier: Modifier = Modifier
) {
    val ringSize = boxSize - 2.dp
    val discSize = boxSize * 0.7f

    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        if (visual == BatteryVisual.PluggedIn) {
            OutwardWaves(color = color, boxSize = boxSize)
        }
        ProgressRing(
            progress = progress,
            modifier = Modifier
                .size(ringSize)
                .graphicsLayer { alpha = pulse.value },
            color = color,
            strokeWidth = if (disc) 2.dp else 1.6.dp
        )
        if (disc) {
            Box(
                modifier = Modifier
                    .size(discSize)
                    .background(color.copy(alpha = 0.12f), shape = CircleShape)
                    .border(1.2.dp, color.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                BatteryStateIcon(
                    visual = visual,
                    color = color,
                    pulse = pulse,
                    iconSize = iconSize,
                    size = discSize
                )
            }
        } else {
            BatteryStateIcon(
                visual = visual,
                color = color,
                pulse = pulse,
                iconSize = iconSize,
                size = boxSize
            )
        }
    }
}

@Composable
fun BatteryStateIcon(
    visual: BatteryVisual,
    color: Color,
    pulse: State<Float>,
    iconSize: Dp,
    size: Dp,
    modifier: Modifier = Modifier
) {
    // The bolt breathes only on the plugged-in card; no other state runs this loop.
    val breathe: State<Float> = if (visual == BatteryVisual.PluggedIn) {
        rememberInfiniteTransition(label = "batteryIcon").animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe"
        )
    } else SolidPulse

    // One-shot entrance (full battery pops) plus one ripple for Full.
    val enter = remember(visual) { Animatable(0f) }
    val ripple = remember(visual) { Animatable(0f) }
    // Unplugged: 0 = bolt at full strength, 1 = drained (smaller and dimmer).
    val drain = remember(visual) { Animatable(0f) }
    LaunchedEffect(visual) {
        enter.snapTo(0f)
        ripple.snapTo(0f)
        drain.snapTo(0f)
        if (visual == BatteryVisual.PluggedOut) {
            if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
                drain.snapTo(1f)
            } else {
                launch {
                    delay(UNPLUG_HOLD_MS)
                    drain.animateTo(1f, tween(UNPLUG_DRAIN_MS, easing = FastOutSlowInEasing))
                }
            }
        }
        if (visual == BatteryVisual.Full) {
            launch { ripple.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing)) }
        }
        enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f))
    }

    val vector = when (visual) {
        BatteryVisual.PluggedIn, BatteryVisual.PluggedOut -> Icons.Rounded.Bolt
        BatteryVisual.Full -> Icons.Rounded.BatteryFull
        BatteryVisual.Low -> Icons.Rounded.BatteryAlert
        else -> Icons.Rounded.BatterySaver
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // The Full ripple reads its value in the draw block, not here.
        if (visual == BatteryVisual.Full) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        val r = ripple.value
                        val ring = 1f + 0.45f * r
                        scaleX = ring
                        scaleY = ring
                        alpha = if (r > 0f && r < 1f) 1f - r else 0f
                    }
                    .border(1.5.dp, color, CircleShape)
            )
        }
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    when (visual) {
                        BatteryVisual.PluggedIn -> {
                            val s = (0.4f + 0.6f * enter.value) * breathe.value
                            scaleX = s
                            scaleY = s
                        }
                        BatteryVisual.PluggedOut -> {
                            val d = drain.value
                            val s = 1.08f - 0.26f * d
                            scaleX = s
                            scaleY = s
                            alpha = 1f - 0.5f * d
                        }
                        BatteryVisual.Full -> {
                            val s = 0.4f + 0.6f * enter.value
                            scaleX = s
                            scaleY = s
                            alpha = pulse.value
                        }
                        BatteryVisual.Low -> {
                            alpha = pulse.value
                        }
                        else -> Unit
                    }
                }
        )
    }
}

private const val UNPLUG_HOLD_MS = 150L
private const val UNPLUG_DRAIN_MS = 900
