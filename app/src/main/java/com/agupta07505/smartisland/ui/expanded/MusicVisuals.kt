/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.expanded

import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.util.runCatchingLogged
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
    val phase by transition.animateFloat(
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

/**
 * Current playback position of [notification]'s media session, ticking once a second while playing.
 * Falls back to the position captured when the notification was posted.
 */
@Composable
internal fun rememberMediaPositionMs(notification: IslandNotification?): Long {
    val context = LocalContext.current
    val controller = remember(notification?.mediaToken) {
        notification?.mediaToken?.let { token ->
            runCatchingLogged("MusicVisuals", "Failed to create MediaController") { MediaController(context, token) }
        }
    }
    fun read(): Long {
        val state = controller?.playbackState
        return if (state != null && state.state == PlaybackState.STATE_PLAYING) {
            val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            (state.position + (elapsed * state.playbackSpeed).toLong()).coerceAtLeast(0L)
        } else {
            state?.position ?: notification?.mediaPositionMs ?: 0L
        }
    }
    var position by remember(controller, notification?.mediaPositionMs) { mutableStateOf(read()) }
    val playing = notification?.mediaIsPlaying == true
    LaunchedEffect(controller, playing) {
        position = read()
        while (playing) {
            kotlinx.coroutines.delay(1000)
            position = read()
        }
    }
    return position
}
