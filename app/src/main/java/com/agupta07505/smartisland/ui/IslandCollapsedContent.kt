package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxTranslationPx = with(density) { 48.dp.toPx() }
    val translationProgress = 1f - collapsedAlpha
    val translationXLeft = translationProgress * maxTranslationPx
    val translationXRight = -translationProgress * maxTranslationPx

    Box(modifier = modifier.fillMaxSize()) {
        // Left Slot (Icon / Glyphs)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .graphicsLayer {
                    translationX = translationXLeft
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> NotificationGlyph(notification = notification)
                IslandMode.IncomingCall -> {
                    val icon = notification?.icon
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
                IslandMode.Empty -> Unit
            }
        }

        // Right Slot (Visualizer / Indicators)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
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
                            .background(Color(0xFF2563EB))
                    )
                }
                IslandMode.IncomingCall -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7FD35E))
                    )
                }
                IslandMode.Music -> {
                    AudioVisualizer(
                        isPlaying = notification?.mediaIsPlaying == true,
                        color = Color(0xFFFF6B9A)
                    )
                }
                IslandMode.Empty -> Unit
            }
        }

        // Central Notch / Camera cutout mimic
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun NotificationGlyph(notification: IslandNotification?) {
    val icon = notification?.icon
    if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
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

internal fun formatNotificationTime(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
