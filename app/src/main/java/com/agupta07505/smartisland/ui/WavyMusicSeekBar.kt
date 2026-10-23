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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun WavyMusicSeekBar(
    /** Read while drawing: a changing position redraws the bar without recomposing the card around it. */
    progress: () -> Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White,
    trackColor: Color = Color(0xFF667085),
    thumbColor: Color = Color.White,
    waveHeight: Dp = 8.dp,
    trackThickness: Dp = 4.dp,
    thumbRadius: Dp = 8.dp,
    /** The shade's thumb: a short vertical rounded bar instead of a dot. */
    pillThumb: Boolean = false,
    pillThumbWidth: Dp = 5.dp,
    pillThumbHeight: Dp = 18.dp
) {
    // The wave settles into a straight line when playback is paused, like the shade's bar.
    val waveAmount by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "waveAmount"
    )

    // The phase only ticks while the wave is visible (playing, or still settling flat). A paused, flat bar runs no animation at all,
    // so it asks for no frames.
    val waveVisible = (isPlaying || waveAmount > 0f) && LocalCardShown.current
    val phaseState: State<Float> = if (waveVisible) {
        rememberInfiniteTransition(label = "wavySeekBarPhase").animateFloat(
            initialValue = 0f,
            targetValue = 6.2831855f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 667, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else remember { mutableFloatStateOf(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragProgress)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        val phase = phaseState.value
        val width = size.width
        val height = size.height
        val progressX = (if (isDragging) dragProgress else progress()) * width
        
        val baselineY = height / 2f
        val baseThicknessPx = trackThickness.toPx()
        val maxWaveHeightPx = waveHeight.toPx() * waveAmount
        val thumbRadiusPx = thumbRadius.toPx()
        
        val remainingWidth = width - progressX
        if (remainingWidth > 0) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(progressX, baselineY - baseThicknessPx / 2f),
                size = Size(remainingWidth, baseThicknessPx),
                cornerRadius = CornerRadius(baseThicknessPx / 2f, baseThicknessPx / 2f)
            )
        }
        
        if (progressX > 0) {
            val path = Path()
            val startX = 0f
            val endX = progressX
            
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
            val transitionWidthPx = with(density) { WAVE_FADE_WIDTH.toPx() }
            
            val pathStartX = startX + baseThicknessPx / 2f
            if (endX > pathStartX) {
                for (xInt in pathStartX.toInt()..endX.toInt()) {
                    val x = xInt.toFloat()
                    
                    val dampLeft = ((x - pathStartX) / transitionWidthPx).coerceIn(0f, 1f)
                    val dampRight = ((endX - x) / transitionWidthPx).coerceIn(0f, 1f)
                    val damp = dampLeft * dampRight
                    
                    // Two sine frequencies for an organic wave.
                    val waveVal = sin(x * WAVE_FREQ_1 - phase) * 0.6f + cos(x * WAVE_FREQ_2 + phase * 1.3f) * 0.4f
                    val normalizedWave = (waveVal + 1f) / 2f
                    
                    val y = baselineY - (baseThicknessPx / 2f) - (maxWaveHeightPx * damp * normalizedWave)
                    path.lineTo(x, y)
                }
            }
            
            path.lineTo(progressX, baselineY + baseThicknessPx / 2f)
            
            path.lineTo(pathStartX, baselineY + baseThicknessPx / 2f)
            
            path.close()
            
            drawPath(
                path = path,
                color = waveColor,
                style = Fill
            )
        }
        
        if (pillThumb) {
            val pillW = pillThumbWidth.toPx()
            val pillH = pillThumbHeight.toPx()
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(progressX - pillW / 2f, baselineY - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillW / 2f, pillW / 2f)
            )
        } else {
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(progressX, baselineY)
            )
        }
    }
}

private val WAVE_FADE_WIDTH = 24.dp
private const val WAVE_FREQ_1 = 0.04f
private const val WAVE_FREQ_2 = 0.02f

