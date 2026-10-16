/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.components

import com.agupta07505.smartisland.ui.IslandPalette
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DottedRing(
    progress: Float,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
    color: Color = IslandPalette.success
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val dotRadius = 1.2.dp.toPx()
        val numDots = 16
        val activeDotsCount = (numDots * progress).toInt()
        for (i in 0 until numDots) {
            val angle = (-90f + rotationAngle + i * 360f / numDots) * (Math.PI / 180f)
            val x = (center.x + radius * Math.cos(angle)).toFloat()
            val y = (center.y + radius * Math.sin(angle)).toFloat()
            val isActive = i < activeDotsCount
            val dotColor = if (isActive) color else IslandPalette.fillStrong
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
        }
    }
}
