/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.expanded.toneColor
import com.agupta07505.smartisland.util.ArtworkPalette
import com.agupta07505.smartisland.util.MediaArt
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
internal fun NotificationGlyph(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
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
        val fallbackColor = if (notification?.mode == IslandMode.DownloadUpload) {
            settings.transferColor
        } else {
            settings.notificationDotColor
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(fallbackColor)),
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

private const val VISUALIZER_TONE = 78.0

private const val VISUALIZER_MIN_CHROMA = 28.0
private const val VISUALIZER_MAX_CHROMA = 62.0

@Composable
internal fun rememberArtworkVisualizerColor(notification: IslandNotification?, fallback: Color): Color {
    val artwork = notification?.let { MediaArt.glyphBitmap(it) }
    val tint = remember(artwork) { artwork?.let { ArtworkPalette.fromBitmap(it) } }
    val target = remember(tint, fallback) {
        if (tint == null) {
            fallback
        } else {
            toneColor(
                VISUALIZER_TONE,
                tint.chroma.toDouble().coerceIn(VISUALIZER_MIN_CHROMA, VISUALIZER_MAX_CHROMA),
                tint.labHue
            )
        }
    }
    val color by animateColorAsState(target, tween(350), label = "visualizer_tint")
    return color
}

@Composable
internal fun AudioVisualizer(
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
            val heightFraction by infiniteTransition.animateFloat(
                initialValue = min,
                targetValue = if (isPlaying) max else min,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + index * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .graphicsLayer {
                        scaleY = if (isPlaying) heightFraction else min
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
