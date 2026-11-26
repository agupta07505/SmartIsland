/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.util.MediaArt
import com.agupta07505.smartisland.util.quietly

internal fun Modifier.widenedBackdrop(widthPx: () -> Int): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val plain = measurable.measure(constraints)
        layout(plain.width, plain.height) { plain.place(0, 0) }
    } else {
        val target = maxOf(constraints.maxWidth, widthPx()).coerceAtLeast(1)
        val placeable = measurable.measure(
            Constraints.fixed(target, constraints.maxHeight)
        )
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
    }
}

internal fun notificationHasPictureBackdrop(notification: IslandNotification, settings: SmartIslandSettings): Boolean {
    if (!settings.enableNotificationBackdrop || notification.mode == IslandMode.Music) return false
    val artwork = notification.largeIcon ?: notification.icon
    return artwork != null && !artwork.isRecycled
}

@Composable
internal fun NotificationBackdrop(
    notification: IslandNotification,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    val artwork = notification.largeIcon ?: notification.icon
    val blurRadiusDp = if (settings.expandedBlurEnabled) settings.expandedBlurRadius else null
    val imageBitmap = remember(artwork, blurRadiusDp) {
        if (artwork != null && !artwork.isRecycled) {
            quietly {
                (MediaArt.blurredBackdrop(artwork, blurRadiusDp) ?: artwork).asImageBitmap()
            }
        } else null
    }

    val accentColor = cardAccentColor(notification, settings)
    val palette = LocalIslandPalette.current
    // a picture keeps its own look (black dim, white text) on either theme.
    val scrimColor = palette.scrim

    Box(modifier = modifier) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.38f }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f * cardScrimScale(settings)),
                                Color.Black.copy(alpha = 0.85f * cardScrimScale(settings))
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 700f
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 750f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                scrimColor.copy(alpha = if (palette.isLight) 0.45f else 0.65f)
                            )
                        )
                    )
            )
        }
    }
}
