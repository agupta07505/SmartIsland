/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import com.agupta07505.smartisland.model.HideSink
import com.agupta07505.smartisland.model.SinkPivot

/**
 * The whole island sinks into the camera at [pivot] as [presence] (1 shown, 0 hidden) falls and rises out of it as it
 * climbs. Only the drawing moves, touch targets stay on the pill so a hidden pill can still be revealed from its own
 * place. No [pivot] and it just fades
 */
internal fun Modifier.hideSink(pivot: SinkPivot?, presence: () -> Float): Modifier = this
    .graphicsLayer { alpha = HideSink.fade(presence()) }
    .drawWithContent {
        val sink = HideSink.sink(presence())
        if (pivot == null || sink <= 0f) {
            drawContent()
        } else {
            val at = Offset(size.width / 2f + pivot.xDp * density, pivot.yDp * density)
            withTransform({
                scale(HideSink.scale(sink, pivot.endScaleX), HideSink.scale(sink, pivot.endScaleY), at)
            }) { this@drawWithContent.drawContent() }
        }
    }
