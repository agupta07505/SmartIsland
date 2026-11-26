/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.IslandText as Text

internal val RING_GLANCE_HEIGHT = 94.dp

/** Pixel ring glance used by the timer and stopwatch cards. */
@Composable
internal fun RingGlanceRow(
    glyph: ImageVector,
    glyphDescription: String,
    fraction: Float,
    accent: Color,
    big: String,
    bigColor: Color,
    subtitle: String,
    subtitleColor: Color,
    bottomPadding: Dp,
    onClick: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    extra: (@Composable RowScope.() -> Unit)? = null
) {
    val palette = LocalIslandPalette.current
    val track = palette.tint(0.2f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(64.dp)) {
                    val stroke = 6.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
                    )
                    val sweep = 360f * fraction.coerceIn(0f, 1f)
                    if (sweep > 0f) {
                        drawArc(
                            color = accent, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Icon(
                    imageVector = glyph,
                    contentDescription = glyphDescription,
                    tint = palette.text,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = big,
                    color = bigColor,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                content = buttons)
        }
        if (extra != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = extra
            )
        }
    }
}

@Composable
internal fun RingGlanceButton(
    glyph: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color? = null
) {
    val palette = LocalIslandPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(1.5.dp, palette.tint(0.4f), CircleShape)
            .clip(CircleShape)
            .bounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = glyph, contentDescription = description, tint = tint ?: palette.text,
            modifier = Modifier.size(20.dp))
    }
}
