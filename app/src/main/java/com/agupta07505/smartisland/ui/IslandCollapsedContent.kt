/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.util.formatNotificationTime
import com.agupta07505.smartisland.ui.components.DottedRing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IslandCollapsedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    collapsedAlpha: Float,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxTranslationPx = with(density) { COLLAPSED_TRANSLATION_MAX_DP.toPx() }
    val translationProgress = 1f - collapsedAlpha
    val translationXLeft = translationProgress * maxTranslationPx
    val translationXRight = -translationProgress * maxTranslationPx

    Box(modifier = modifier.fillMaxSize()) {
        // Left Slot (Icon / Glyphs)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = LEFT_SLOT_PADDING_START_DP.dp)
                .graphicsLayer {
                    translationX = translationXLeft
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> NotificationGlyph(notification = notification)
                IslandMode.IncomingCall -> {
                    val icon = notification?.largeIcon ?: notification?.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = null,
                            tint = Color(0xFF7FD35E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IslandMode.Music -> {
                    val artwork = notification?.largeIcon ?: notification?.icon
                    if (artwork != null) {
                        Image(
                            bitmap = artwork.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6B9A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                IslandMode.Battery -> {
                    BatteryCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Empty -> Unit
            }
        }

        // Right Slot (Visualizer / Indicators)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = RIGHT_SLOT_PADDING_END_DP.dp)
                .graphicsLayer {
                    translationX = translationXRight
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(settings.notificationDotColor))
                    )
                }
                IslandMode.IncomingCall -> {
                    val time = notification?.timeMillis ?: System.currentTimeMillis()
                    CallTimer(postTimeMillis = time, color = Color(0xFF7FD35E))
                }
                IslandMode.Music -> {
                    AudioVisualizer(
                        isPlaying = notification?.mediaIsPlaying == true,
                        color = Color(settings.musicVisualizerColor)
                    )
                }
                IslandMode.Battery -> {
                    Text(
                        text = notification?.text ?: "49%",
                        color = Color(settings.batteryColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.Empty -> Unit
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(CENTER_DOT_SIZE_DP.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun BatteryCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings) {
    val pctText = notification?.text?.replace("%", "")?.trim() ?: "49"
    val pct = pctText.toFloatOrNull() ?: 49f
    val progress = (pct / 100f).coerceIn(0f, 1f)
    val batteryColor = Color(settings.batteryColor)

    val infiniteTransition = rememberInfiniteTransition(label = "batteryPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "batteryScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dottedRingRotation"
    )

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        DottedRing(
            progress = progress,
            rotationAngle = rotationAngle,
            modifier = Modifier.size(22.dp),
            color = batteryColor
        )
        Icon(
            Icons.Rounded.Bolt,
            contentDescription = "Charging",
            tint = batteryColor,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
        )
    }
}

@Composable
private fun NotificationGlyph(notification: IslandNotification?) {
    val largeIcon = notification?.largeIcon
    val icon = notification?.icon
    val mainIcon = largeIcon ?: icon
    if (mainIcon != null) {
        Box(modifier = Modifier.size(22.dp)) {
            Image(
                bitmap = mainIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (largeIcon != null && icon != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.Black, CircleShape)
                        .padding(1.dp)
                ) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification?.appName?.firstOrNull()?.uppercase() ?: "S",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_visualizer")
        
        val heights = listOf(0.3f to 0.9f, 0.5f to 1.0f, 0.2f to 0.7f)
        
        heights.forEachIndexed { index, (min, max) ->
            val heightFraction by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = min,
                    targetValue = max,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + index * 80, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
            } else {
                remember { mutableStateOf(min) }
            }
            
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = (14.dp.value * heightFraction).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}


@Composable
private fun CallTimer(postTimeMillis: Long, color: Color) {
    var elapsedSeconds by remember(postTimeMillis) {
        mutableStateOf(((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L))
    }
    LaunchedEffect(postTimeMillis) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L)
            kotlinx.coroutines.delay(1000)
        }
    }
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    Text(
        text = "%02d:%02d".format(minutes, seconds),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// Collapsed content animation
private val COLLAPSED_TRANSLATION_MAX_DP = 32.dp
private const val LEFT_SLOT_PADDING_START_DP = 8
private const val RIGHT_SLOT_PADDING_END_DP = 12
private const val CENTER_DOT_SIZE_DP = 20
