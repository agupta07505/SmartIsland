/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WavyMusicSeekBar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White,
    trackColor: Color = Color(0xFF667085),
    thumbColor: Color = Color.White,
    waveHeight: Dp = 8.dp,
    trackThickness: Dp = 4.dp,
    thumbRadius: Dp = 8.dp
) {
    var phase by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val startTime = android.os.SystemClock.elapsedRealtime()
        var lastTime = startTime
        while (true) {
            val now = android.os.SystemClock.elapsedRealtime()
            val delta = now - lastTime
            lastTime = now
            // Progress the phase: 1.5 cycles per second
            phase += (delta / 1000f) * 1.5f * 2f * Math.PI.toFloat()
            if (phase > 2f * Math.PI.toFloat()) {
                phase -= 2f * Math.PI.toFloat()
            }
            kotlinx.coroutines.delay(16)
        }
    }

    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    },
                    onDrag = { change, _ ->
                        val newProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val progressX = progress * width
        
        val baselineY = height / 2f
        val baseThicknessPx = trackThickness.toPx()
        val maxWaveHeightPx = waveHeight.toPx()
        val thumbRadiusPx = thumbRadius.toPx()
        
        // 1. Draw remaining track on the right (thin straight rounded bar)
        val remainingWidth = width - progressX
        if (remainingWidth > 0) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(progressX, baselineY - baseThicknessPx / 2f),
                size = Size(remainingWidth, baseThicknessPx),
                cornerRadius = CornerRadius(baseThicknessPx / 2f, baseThicknessPx / 2f)
            )
        }
        
        // 2. Draw played wave fill on the left
        if (progressX > 0) {
            val path = Path()
            val startX = 0f
            val endX = progressX
            
            // Left cap height
            path.arcTo(
                rect = Rect(
                    left = startX,
                    top = baselineY - baseThicknessPx / 2f,
                    right = startX + baseThicknessPx,
                    bottom = baselineY + baseThicknessPx / 2f
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
            
            // The wave should transition smoothly to flat at the left edge and near the thumb
            // transitionWidth: width over which the wave fades to flat (e.g. 24dp)
            val transitionWidthPx = with(density) { WAVE_FADE_WIDTH.toPx() }
            
            // Generate path points along the wavy top edge
            // We start from the end of the left arc (startX + baseThicknessPx / 2f)
            val pathStartX = startX + baseThicknessPx / 2f
            if (endX > pathStartX) {
                for (xInt in pathStartX.toInt()..endX.toInt()) {
                    val x = xInt.toFloat()
                    
                    // Damp the wave near startX and endX so it smoothly meets the flat baseline
                    val dampLeft = ((x - pathStartX) / transitionWidthPx).coerceIn(0f, 1f)
                    val dampRight = ((endX - x) / transitionWidthPx).coerceIn(0f, 1f)
                    val damp = dampLeft * dampRight
                    
                    // Premium-looking organic wave: combine two sine frequencies
                    // Using phase to shift the wave left/right
                    val waveVal = sin(x * WAVE_FREQ_1 - phase) * 0.6f + cos(x * WAVE_FREQ_2 + phase * 1.3f) * 0.4f
                    val normalizedWave = (waveVal + 1f) / 2f // 0f..1f
                    
                    val y = baselineY - (baseThicknessPx / 2f) - (maxWaveHeightPx * damp * normalizedWave)
                    path.lineTo(x, y)
                }
            }
            
            // Draw down to the bottom baseline at progressX
            path.lineTo(progressX, baselineY + baseThicknessPx / 2f)
            
            // Draw flat bottom baseline back to the start of the left arc
            path.lineTo(pathStartX, baselineY + baseThicknessPx / 2f)
            
            path.close()
            
            drawPath(
                path = path,
                color = waveColor,
                style = Fill
            )
        }
        
        // 3. Draw thumb at progressX
        drawCircle(
            color = thumbColor,
            radius = thumbRadiusPx,
            center = Offset(progressX, baselineY)
        )
    }
}

private val WAVE_FADE_WIDTH = 24.dp
private const val WAVE_FREQ_1 = 0.04f
private const val WAVE_FREQ_2 = 0.02f

