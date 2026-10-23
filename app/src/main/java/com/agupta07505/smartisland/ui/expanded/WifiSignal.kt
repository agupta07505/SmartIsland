/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The Wi-Fi card's picture, in the same 46 dp tinted box as the VPN globe and the Bluetooth earbuds. See [WifiArcs]. */
@Composable
internal fun WifiSignalBox(accent: Color, modifier: Modifier = Modifier, lost: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.3f), shape),
        contentAlignment = Alignment.Center
    ) {
        WifiArcs(accent = accent, lost = lost, modifier = Modifier.size(32.dp))
    }
}

/**
 * A dot and three arcs drawn as lines, scaled to whatever size it is given (the expanded card's box uses 32 dp, the
 * collapsed pill 20 dp).
 */
@Composable
internal fun WifiArcs(accent: Color, modifier: Modifier = Modifier, lost: Boolean = false) {
    var level by remember { mutableIntStateOf(if (lost) 3 else 1) }
    val animate = !lost && !com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()
    if (animate) {
        // One tiny state change every step; only this glyph is redrawn.
        LaunchedEffect(Unit) {
            var i = 0
            while (true) {
                level = WIFI_LEVEL_CYCLE[i % WIFI_LEVEL_CYCLE.size]
                i++
                delay(WIFI_STEP_MS)
            }
        }
    } else {
        level = 3
    }
    val arc1 by animateFloatAsState(if (level >= 1) 1f else WIFI_ARC_OFF_ALPHA, tween(WIFI_FADE_MS), label = "wifiArc1")
    val arc2 by animateFloatAsState(if (level >= 2) 1f else WIFI_ARC_OFF_ALPHA, tween(WIFI_FADE_MS), label = "wifiArc2")
    val arc3 by animateFloatAsState(if (level >= 3) 1f else WIFI_ARC_OFF_ALPHA, tween(WIFI_FADE_MS), label = "wifiArc3")
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 2f / 20f
        // 1.9 dp on the 32 dp card glyph, in proportion at every other size.
        val line = size.minDimension * 0.0594f
        val stroke = Stroke(width = line, cap = StrokeCap.Round)
        val origin = Offset(size.width / 2f, size.height / 2f + 11f * unit)
        drawCircle(accent, radius = 2.1f * unit, center = origin)
        val alphas = floatArrayOf(arc1, arc2, arc3)
        for ((i, r) in WIFI_ARC_RADII.withIndex()) {
            drawArc(
                color = accent.copy(alpha = alphas[i]),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(origin.x - r * unit, origin.y - r * unit),
                size = Size(2f * r * unit, 2f * r * unit),
                style = stroke
            )
        }
        if (lost) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val from = Offset(c.x - 0.78f * 20f * unit, c.y - 0.78f * 20f * unit)
            val to = Offset(c.x + 0.78f * 20f * unit, c.y + 0.78f * 20f * unit)
            drawLine(Color.Black.copy(alpha = 0.55f), from, to, strokeWidth = line * 2.37f, cap = StrokeCap.Round)
            drawLine(accent, from, to, strokeWidth = line * 1.05f, cap = StrokeCap.Round)
        }
    }
}

private val WIFI_LEVEL_CYCLE = intArrayOf(1, 2, 3)
private const val WIFI_STEP_MS = 600L
private const val WIFI_FADE_MS = 240
private const val WIFI_ARC_OFF_ALPHA = 0.18f
private val WIFI_ARC_RADII = floatArrayOf(9f, 16f, 23f)
