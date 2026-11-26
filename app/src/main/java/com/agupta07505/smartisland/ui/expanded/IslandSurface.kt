/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings

internal val SPLIT_ISLAND_CORNER = 34.dp

internal val SPLIT_ISLAND_GAP = 14.dp

/** Narrowest the card island gets in the split layout, so tiny cards. */
internal val SPLIT_CARD_MIN_WIDTH = 240.dp

internal fun cardScrimScale(settings: SmartIslandSettings): Float =
    if (settings.expandedGradientEnabled && !settings.oledBlack) 0.3f else 1f

internal fun expandedSurfaceColor(settings: SmartIslandSettings, palette: IslandPalette): Color =
    if (palette.isLight) palette.surface else Color(settings.islandSurfaceColor)

private const val LIGHT_GRADIENT_REACH = 0.65f

private const val GRADIENT_NEAR_SHARE = 0.35f

internal fun expandedGradientBrush(settings: SmartIslandSettings, palette: IslandPalette = IslandPalette.Dark): Brush? {
    if (!settings.expandedGradientEnabled) return null
    if (settings.oledBlack && !palette.isLight) return null
    val start = expandedSurfaceColor(settings, palette)
    val gradientColor = Color(settings.expandedGradientColor)
    val strength = settings.expandedGradientStrength.coerceIn(0f,
        1f) * (if (palette.isLight) LIGHT_GRADIENT_REACH else 1f)
    // The far corner takes the full strength
    val from = lerp(start, gradientColor, strength * GRADIENT_NEAR_SHARE)
    val to = lerp(start, gradientColor, strength)
    return Brush.linearGradient(listOf(from, to))
}

internal fun Modifier.islandSurface(
    settings: SmartIslandSettings,
    alpha: Float,
    shape: Shape = RoundedCornerShape(SPLIT_ISLAND_CORNER),
    /** When set (landscape split) */
    shadowStrength: (() -> Float)? = null,
    palette: IslandPalette = IslandPalette.Dark
): Modifier {
    val shadowed = if (settings.enableShadow && settings.shadowElevation > 0f && shadowStrength != null) {
        val elevation = (settings.shadowElevation * 1.5f * palette.shadowScale).dp
        val islandShape = shape
        this.graphicsLayer {
            shadowElevation = elevation.toPx() * shadowStrength().coerceIn(0f, 1f)
            this.shape = islandShape
            clip = false
            ambientShadowColor = Color.Black
            spotShadowColor = Color.Black
        }
    } else if (settings.enableShadow && settings.shadowElevation > 0f) {
        this.shadow(
            elevation = (settings.shadowElevation * 1.5f * palette.shadowScale).dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black,
            spotColor = Color.Black
        )
    } else {
        this
    }
    val brush = expandedGradientBrush(settings, palette)
    val surfaceColor = expandedSurfaceColor(settings, palette)
    return shadowed
        .clip(shape)
        .then(
            if (brush != null) Modifier.background(brush, shape, alpha)
            else Modifier.background(surfaceColor.copy(alpha = alpha))
        )
}
