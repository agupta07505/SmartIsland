/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.runtime.Composable
import kotlin.math.cos
import kotlin.math.sin

private const val GRID = 24f
private const val STROKE = 2f
private const val HORIZON_Y = 20.5f
private const val SUN_CENTER_X = 12f
private const val SUN_CENTER_Y = 18f
private const val SUN_RADIUS = 5.5f
private const val RAY_FROM = 8.2f
private const val RAY_TO = 10.8f
private const val CLIP_BOTTOM = 19f
private val RAY_ANGLES = floatArrayOf(0f, 45f, 90f, 135f, 180f)

/**
 * The half sun on its horizon line, drawn like the Material "twilight" glyph but in two parts: the horizon line stays
 * fixed, and the sun (disc and rays) can sit anywhere between hidden behind it ([lift] 0) and fully up ([lift] 1).
 */
@Composable
internal fun HorizonSunGlyph(lift: Float, modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val k = size.width / GRID
        val stroke = STROKE * k
        // How far down the sun has to go to be completely hidden: its highest ray tip to the clip line.
        val hide = (CLIP_BOTTOM - (SUN_CENTER_Y - RAY_TO)) * k + stroke
        clipRect(left = 0f, top = 0f, right = size.width, bottom = CLIP_BOTTOM * k) {
            translate(top = hide * (1f - lift.coerceIn(0f, 1f))) {
                drawCircle(color, radius = SUN_RADIUS * k, center = Offset(SUN_CENTER_X * k, SUN_CENTER_Y * k))
                for (deg in RAY_ANGLES) {
                    val rad = Math.toRadians(deg.toDouble())
                    val dx = cos(rad).toFloat()
                    val dy = -sin(rad).toFloat()
                    drawLine(
                        color,
                        Offset((SUN_CENTER_X + dx * RAY_FROM) * k, (SUN_CENTER_Y + dy * RAY_FROM) * k),
                        Offset((SUN_CENTER_X + dx * RAY_TO) * k, (SUN_CENTER_Y + dy * RAY_TO) * k),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        drawLine(
            color,
            Offset(3f * k, HORIZON_Y * k),
            Offset(21f * k, HORIZON_Y * k),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
