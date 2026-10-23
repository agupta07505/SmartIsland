/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * The VPN card's picture: a tilted wireframe globe in the accent colour, in the same 46 dp tinted box the Bluetooth
 * card uses for its earbuds. Drawn as lines in a 20-unit-radius space.
 */
@Composable
internal fun VpnGlobeBox(accent: Color, modifier: Modifier = Modifier, slashed: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.3f), shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val unit = size.minDimension / 2f / GLOBE_RADIUS
            val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(accent, radius = GLOBE_RADIUS * unit, center = c, style = stroke)
            rotate(degrees = GLOBE_TILT_DEG, pivot = c) {
                for (rx in floatArrayOf(8f, 15f)) {
                    drawOval(
                        color = accent,
                        topLeft = Offset(c.x - rx * unit, c.y - GLOBE_RADIUS * unit),
                        size = Size(2f * rx * unit, 2f * GLOBE_RADIUS * unit),
                        style = stroke
                    )
                }
                val axis = Path().apply {
                    moveTo(c.x, c.y - GLOBE_RADIUS * unit)
                    lineTo(c.x, c.y + GLOBE_RADIUS * unit)
                }
                drawPath(axis, accent, style = stroke)
                // Latitudes: half-width, height above (-) or below (+) the middle; each bows downwards by 8 units at its control point.
                for ((halfWidth, y) in LATITUDES) {
                    val lat = Path().apply {
                        moveTo(c.x - halfWidth * unit, c.y + y * unit)
                        quadraticTo(c.x, c.y + (y + 8f) * unit, c.x + halfWidth * unit, c.y + y * unit)
                    }
                    drawPath(lat, accent, style = stroke)
                }
            }
            // Disconnected: a slash from the top left to the bottom right, with a thin dark gap either side so it reads over the lines.
            if (slashed) {
                val from = Offset(c.x - 0.78f * GLOBE_RADIUS * unit, c.y - 0.78f * GLOBE_RADIUS * unit)
                val to = Offset(c.x + 0.78f * GLOBE_RADIUS * unit, c.y + 0.78f * GLOBE_RADIUS * unit)
                drawLine(Color.Black.copy(alpha = 0.55f), from, to, strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(accent, from, to, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

private const val GLOBE_RADIUS = 20f
private const val GLOBE_TILT_DEG = -14f
private val LATITUDES = listOf(14.3f to -14f, 19.1f to -6f, 19.1f to 6f, 14.3f to 14f)
