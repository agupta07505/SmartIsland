/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Liquid glass for the app's own screens (not the island).

/** True while the liquid glass theme is on. Read by the glass-aware Card / Switch and by the surfaces that draw a rim. */
val LocalLiquidGlass = staticCompositionLocalOf { false }

/** The colours the glass backdrop is drawn with, so a layer that slides over the screen can bring its own copy of it. */
class GlassBackdropColors(val base: Color, val glowA: Color, val glowB: Color, val glowC: Color)

val LocalGlassBackdropColors = staticCompositionLocalOf<GlassBackdropColors?> { null }

/** True when the glass theme is drawn in its dark variant (the rim and glare strengths differ). */
val LocalGlassDark = staticCompositionLocalOf { false }

private val FringeLit = Color(0xFFD6F1FF)
private val FringeFar = Color(0xFFFFD9F4)

/**
 * The edge of a glass panel, drawn over its content: a hairline rim (bright at the top left, nearly gone in the
 * middle, a soft second highlight at the bottom right) and a glare that fades out over the top of the panel.
 */
fun Modifier.glassEdge(shape: Shape, glass: Boolean, dark: Boolean): Modifier {
    if (!glass) return this
    return drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val litAlpha = if (dark) 0.50f else 0.95f
        val farAlpha = if (dark) 0.22f else 0.55f
        val rim = Brush.linearGradient(
            0f to FringeLit.copy(alpha = litAlpha),
            0.30f to Color.White.copy(alpha = if (dark) 0.10f else 0.28f),
            0.70f to Color.White.copy(alpha = if (dark) 0.05f else 0.12f),
            1f to FringeFar.copy(alpha = farAlpha),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        drawOutline(outline, brush = rim, style = Stroke(width = 1.2.dp.toPx()))
        if (!dark) {
            // On a light backdrop a white rim alone vanishes: a faint dark line just inside keeps the panel's edge readable.
            drawOutline(outline, color = Color.Black.copy(alpha = 0.05f), style = Stroke(width = 0.6.dp.toPx()))
        }
        // Glare: the light streak along the top that the kit's shader puts on the surface normal facing the light.
        val glareHeight = 56.dp.toPx().coerceAtMost(size.height)
        if (glareHeight > 0f) {
            val fraction = (glareHeight / size.height).coerceIn(0.02f, 1f)
            drawOutline(
                outline,
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (dark) 0.09f else 0.40f),
                    fraction to Color.Transparent,
                    1f to Color.Transparent
                )
            )
        }
    }
}

/**
 * The colour field behind every screen of the glass theme: the plain background colour with three large soft glows in
 * the theme's own accent colours, so the translucent panels above it have something to tint.
 */
@Composable
fun LiquidGlassBackdrop(
    base: Color,
    glowA: Color,
    glowB: Color,
    glowC: Color,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strength = if (dark) 0.34f else 0.30f
        drawRect(base)
        fun glow(color: Color, center: Offset, radius: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = strength), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        glow(glowA, Offset(w * 0.10f, h * 0.06f), w * 0.95f)
        glow(glowB, Offset(w * 0.98f, h * 0.38f), w * 0.85f)
        glow(glowC, Offset(w * 0.18f, h * 0.94f), w * 1.0f)
        // A very light sheen over the top third, the way glass catches the room's light.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = if (dark) 0.035f else 0.20f), Color.Transparent),
                endY = h * 0.34f
            )
        )
    }
}

/**
 * An opaque fill in the theme's own look: the glass backdrop on the glass theme, the plain background colour
 * otherwise.
 */
@Composable
fun ThemedBackdrop(modifier: Modifier = Modifier) {
    val colors = LocalGlassBackdropColors.current
    if (LocalLiquidGlass.current && colors != null) {
        LiquidGlassBackdrop(
            base = colors.base,
            glowA = colors.glowA,
            glowB = colors.glowB,
            glowC = colors.glowC,
            dark = LocalGlassDark.current,
            modifier = modifier
        )
    } else {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
}
