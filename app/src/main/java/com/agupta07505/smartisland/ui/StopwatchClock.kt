/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.util.TimerStopwatchParser

/**
 * Time elapsed since [startTimeMs], refreshed once per display frame while [running] (Rule_Book §4.11). Compose's
 * frame clock is Choreographer-backed and halts with the overlay's lifecycle when the screen goes off (§5), so
 * this costs nothing while hidden. While not running the value stays where it was.
 */
@Composable
fun rememberFrameElapsedMillis(startTimeMs: Long, running: Boolean): LongState {
    val elapsed = remember(startTimeMs) {
        mutableLongStateOf((System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L))
    }
    LaunchedEffect(startTimeMs, running) {
        if (!running) return@LaunchedEffect
        while (true) {
            withFrameMillis { }
            elapsed.longValue = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
        }
    }
    return elapsed
}

/**
 * MM:SS.cc readout that ticks every frame without layout thrashing: tabular digits and a fixed width measured
 * once from the widest reading, so only this text redraws and nothing around it is re-measured. The state is
 * read here, not by the caller, so the caller does not recompose per frame either.
 */
@Composable
fun StopwatchReadout(
    elapsedMs: LongState,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    val style = remember(color, fontSize, fontWeight) {
        TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight, fontFeatureSettings = "tnum")
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val width = remember(style, density) {
        with(density) { measurer.measure(WIDEST_READING, style).size.width.toDp() } + 1.dp
    }
    Box(modifier = modifier.width(width)) {
        Text(
            text = TimerStopwatchParser.formatStopwatchMillis(elapsedMs.longValue),
            style = style,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Every digit is the same width with "tnum", so this is as wide as any reading gets (H:MM:SS is shorter). */
private const val WIDEST_READING = "00:00.00"
