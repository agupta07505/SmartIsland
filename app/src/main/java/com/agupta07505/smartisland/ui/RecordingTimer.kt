/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Elapsed whole seconds of a recorder card. Frozen (never ticking) while paused or finished. */
@Composable
internal fun rememberRecordingSeconds(notification: IslandNotification?): Long {
    val frozen = notification?.recordingFrozenMs
    val start = notification?.timeMillis ?: System.currentTimeMillis()
    var now by remember(notification?.key) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(notification?.key, frozen) {
        if (frozen == null) {
            while (true) {
                now = System.currentTimeMillis()
                // Wake just after the shown second changes, at most 250 ms out.
                val elapsed = (now - start).coerceAtLeast(0L)
                kotlinx.coroutines.delay(
                    com.agupta07505.smartisland.util.TimerClock.msToNextTickUp(elapsed).coerceIn(10L, 250L)
                )
            }
        }
    }
    val elapsedMs = frozen ?: (now - start)
    return (elapsedMs / 1000).coerceAtLeast(0L)
}

internal fun formatRecordingTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
internal fun RecordingTimerText(
    notification: IslandNotification?,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    val seconds = rememberRecordingSeconds(notification)
    Text(
        text = formatRecordingTime(seconds),
        color = if (notification?.recordingFrozenMs != null) color.copy(alpha = 0.6f) else color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}
