/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.animateFloatCalm
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * A lightweight waveform: [barCount] rounded bars on one Canvas, driven by a single infinite
 * transition phase (no per-bar animations). When paused the bars settle to a low, flat shape.
 * Bars fade toward the right edge, like the reference.
 */
@Composable
internal fun MusicWaveform(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    barWidth: Dp = 2.dp,
    gap: Dp = 2.dp,
    height: Dp = 24.dp
) {
    val transition = rememberInfiniteTransition(label = "musicWaveform")
    // Calm: rests when animations are off or the screen is off (Rule_Book 5).
    val phase by transition.animateFloatCalm(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing), RepeatMode.Restart),
        label = "waveformPhase"
    )
    val totalWidth = barWidth * barCount + gap * (barCount - 1)
    Canvas(modifier = modifier.width(totalWidth).height(height)) {
        val barW = barWidth.toPx()
        val step = barW + gap.toPx()
        val maxH = size.height
        val minH = barW * 1.6f
        for (i in 0 until barCount) {
            // Two out-of-phase sines give an organic, non-repeating-looking envelope.
            val wave = abs(sin(phase + i * 0.62f)) * 0.65f + abs(sin(phase * 1.7f + i * 1.15f)) * 0.35f
            val h = if (isPlaying) minH + (maxH - minH) * wave else minH
            val fade = 1f - 0.55f * (i.toFloat() / barCount.coerceAtLeast(1))
            drawRoundRect(
                color = color.copy(alpha = color.alpha * fade),
                topLeft = Offset(i * step, (maxH - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
