/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.os.SystemClock
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.ActionRole
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.rememberRecordingSeconds
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.TimerStopwatchParser
import kotlinx.coroutines.delay

@Composable
fun StopwatchExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    if (OemDeviceRules.isMiuiOrHyperOs()) {
        StopwatchExpandedMiui(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = onOpenNotification,
            onCollapse = onCollapse,
            settings = settings
        )
    } else {
        StopwatchExpandedCurrent(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = onOpenNotification,
            onCollapse = onCollapse,
            settings = settings
        )
    }
}

@Composable
private fun StopwatchExpandedMiui(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val stopwatchColor = Color(settings.stopwatchColor)

    val resumeKeywords = remember { listOf("resume", "start", "play", "unpause", "weiter", "reanudar") }
    val pauseKeywords = remember { listOf("pause", "pausa", "pausar", "sospendi", "रोकें", "暂停") }

    val startTime = notification?.timeMillis ?: remember { System.currentTimeMillis() }

    val frozenElapsedSeconds = notification?.stopwatchElapsedSeconds
    val chronometerBase = notification?.stopwatchChronometerBase

    val isNotificationPaused = remember(
        notification?.key,
        notification?.actionIntents,
        notification?.title,
        notification?.text,
        frozenElapsedSeconds
    ) {
        frozenElapsedSeconds != null ||
            notification?.actionIntents?.any {
                it.title.contains("resume", ignoreCase = true) ||
                    it.title.contains("start", ignoreCase = true)
            } == true ||
            notification?.title?.contains("paused", ignoreCase = true) == true ||
            notification?.text?.contains("paused", ignoreCase = true) == true
    }

    val isPaused = isNotificationPaused

    var elapsedSeconds by remember(
        notification?.key,
        startTime,
        frozenElapsedSeconds,
        chronometerBase
    ) {
        mutableLongStateOf(
            frozenElapsedSeconds
                ?: chronometerBase?.let { base ->
                    (
                        (SystemClock.elapsedRealtime() - base) / 1000L
                    ).coerceAtLeast(0L)
                }
                ?: ((System.currentTimeMillis() - startTime) / 1000L)
                    .coerceAtLeast(0L)
        )
    }

    val notificationLapCount = remember(
        notification?.title,
        notification?.text
    ) {
        val lapRegex = Regex("""(?i)\blap\s*[:#]?\s*(\d+)\b""")

        listOf(
            notification?.title.orEmpty(),
            notification?.text.orEmpty()
        ).firstNotNullOfOrNull { value ->
            lapRegex.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()
        } ?: 1
    }

    var lapCount by remember(notification?.key) {
        mutableIntStateOf(notificationLapCount)
    }

    LaunchedEffect(
        notification?.key,
        startTime,
        isPaused,
        frozenElapsedSeconds,
        chronometerBase
    ) {
        if (frozenElapsedSeconds != null) {
            elapsedSeconds = frozenElapsedSeconds
        } else if (!isPaused) {
            while (true) {
                val clockElapsed = chronometerBase?.let { base ->
                    (
                        (SystemClock.elapsedRealtime() - base) / 1000L
                    ).coerceAtLeast(0L)
                }

                elapsedSeconds = clockElapsed
                    ?: ((System.currentTimeMillis() - startTime) / 1000L)
                        .coerceAtLeast(0L)

                delay(100L)
            }
        }
    }

    val displayTime = remember(elapsedSeconds) {
        TimerStopwatchParser.formatTime(elapsedSeconds)
    }

    val palette = LocalIslandPalette.current
    val subtitle = miuiStopwatchSubtitle(isPaused, lapCount, notification)
    val actions = miuiStopwatchActions(notification, isPaused, resumeKeywords, pauseKeywords)
    val lapAction = actions.lap
    val pauseAction = actions.pause
    val resetAction = actions.reset

    RingGlanceRow(
        glyph = Icons.Rounded.AvTimer,
        glyphDescription = "Stopwatch",
        fraction = (elapsedSeconds % 60L).toFloat() / 60f,
        accent = stopwatchColor,
        big = displayTime,
        bigColor = stopwatchColor,
        subtitle = subtitle,
        subtitleColor = palette.textSecondary,
        bottomPadding = bottomPadding,
        onClick = onOpenNotification,
        buttons = {
            MiuiStopwatchButtons(
                notification = notification,
                isPaused = isPaused,
                lapAction = lapAction,
                pauseAction = pauseAction,
                resetAction = resetAction,
                onLapCounted = { lapCount++ },
                onOpenNotification = onOpenNotification,
                onCollapse = onCollapse
            )
        }
    )
}

private fun miuiStopwatchSubtitle(isPaused: Boolean, lapCount: Int, notification: IslandNotification?): String {
    val notifText = notification?.text.orEmpty()
    val notifTitle = notification?.title.orEmpty()
    return if (isPaused) {
        "Paused"
    } else if (lapCount > 1) {
        "Lap $lapCount"
    } else if (notifText.isNotBlank() && !notifText.equals("Stopwatch", ignoreCase = true)
        && !notifText.contains("00:")) {
        notifText
    } else if (notifTitle.isNotBlank() && !notifTitle.equals("Stopwatch", ignoreCase = true)
        && !notifTitle.contains("00:")) {
        notifTitle
    } else {
        "Stopwatch Active"
    }
}

private class MiuiStopwatchActions(
    val lap: IslandNotificationAction?,
    val pause: IslandNotificationAction?,
    val reset: IslandNotificationAction?
)

private fun miuiStopwatchActions(
    notification: IslandNotification?,
    isPaused: Boolean,
    resumeKeywords: List<String>,
    pauseKeywords: List<String>
): MiuiStopwatchActions {
    val lapAction = notification?.actionIntents?.firstOrNull {
        it.title.contains("lap", ignoreCase = true) || it.title.contains("split", ignoreCase = true)
    }
    val pauseAction = if (isPaused) {
        notification?.actionIntents?.firstOrNull { action ->
            resumeKeywords.any { action.title.contains(it, ignoreCase = true) }
        }
    } else {
        notification?.actionIntents?.firstOrNull { action ->
            pauseKeywords.any { action.title.contains(it, ignoreCase = true) }
        }
    }
    val resetAction = notification?.actionIntents?.firstOrNull {
        val t = it.title.lowercase()
        t.contains("reset") || t.contains("stop") || t.contains("clear") || t.contains("cancel")
    }
    return MiuiStopwatchActions(lapAction, pauseAction, resetAction)
}

@Composable
private fun MiuiStopwatchButtons(
    notification: IslandNotification?,
    isPaused: Boolean,
    lapAction: IslandNotificationAction?,
    pauseAction: IslandNotificationAction?,
    resetAction: IslandNotificationAction?,
    onLapCounted: () -> Unit,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    RingGlanceButton(
        glyph = Icons.Rounded.Flag,
        description = "Lap",
        onClick = {
            // Clock does not record laps while the stopwatch is paused
            if (!isPaused) {
                if (lapAction?.pendingIntent != null && notification != null) {
                    triggerAction(context, notification.packageName, lapAction.pendingIntent, lapAction.title,
                        notification.contentIntent)
                }
                onLapCounted()
            }
        }
    )
    RingGlanceButton(
        glyph = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
        description = if (isPaused) "Resume" else "Pause",
        onClick = {
            val currentNotification = notification
            val action = pauseAction
            if (currentNotification != null && action?.pendingIntent != null) {
                triggerAction(context, currentNotification.packageName, action.pendingIntent, action.title,
                    currentNotification.contentIntent)
            } else {
                // don't fake a state change if Clock provides no matching action
                onOpenNotification()
            }
        }
    )
    RingGlanceButton(
        glyph = Icons.Rounded.Refresh,
        description = "Reset",
        onClick = {
            if (resetAction?.pendingIntent != null && notification != null) {
                triggerAction(context, notification.packageName, resetAction.pendingIntent, resetAction.title,
                    notification.contentIntent)
            }
            val repository = SmartIslandRepositories.notificationRepository(context)
            notification?.key?.let { repository.removeNotification(it) }
            onCollapse()
        }
    )
}

@Composable
private fun StopwatchExpandedCurrent(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val stopwatchColor = Color(settings.stopwatchColor)

    // the listener keeps the stopwatch clock
    val frozenMs = notification?.recordingFrozenMs

    // Optimistic freeze after tapping Pause
    var tappedPauseAtSec by remember(notification?.key, frozenMs) { mutableLongStateOf(-1L) }
    val isPaused = frozenMs != null || tappedPauseAtSec >= 0L

    val liveSeconds = rememberRecordingSeconds(notification)
    val elapsedSeconds = if (tappedPauseAtSec >= 0L) tappedPauseAtSec else liveSeconds

    val displayTime = remember(elapsedSeconds) {
        TimerStopwatchParser.formatTime(elapsedSeconds)
    }

    val palette = LocalIslandPalette.current
    val notifText = notification?.text.orEmpty()
    val notifTitle = notification?.title.orEmpty()
    val subtitle = if (isPaused) {
        "Paused"
    } else if (notifText.isNotBlank() && !notifText.equals("Stopwatch", ignoreCase = true)
        && !notifText.contains("00:")) {
        notifText
    } else if (notifTitle.isNotBlank() && !notifTitle.equals("Stopwatch", ignoreCase = true)
        && !notifTitle.contains("00:")) {
        notifTitle
    } else {
        "Stopwatch Active"
    }

    // Each button is only shown when the stopwatch app offers it
    val lapAction = notification.actionWithRole(ActionRole.Lap)
    val toggleAction = if (isPaused) notification.actionWithRole(ActionRole.Resume)
    else notification.actionWithRole(ActionRole.Pause)
    val resetAction = notification.actionWithRole(ActionRole.Reset, ActionRole.Stop)
    val leftovers = if (notification != null) notification.leftoverActions(lapAction, toggleAction,
        resetAction) else emptyList()
    val extraChips: (@Composable RowScope.() -> Unit)? =
        if (notification != null && leftovers.isNotEmpty()) ({
            // Any other button the stopwatch app put in its notification
            leftovers.forEach { ShadeActionChip(notification, it, stopwatchColor) }
        }) else null

    RingGlanceRow(
        glyph = Icons.Rounded.AvTimer,
        glyphDescription = "Stopwatch",
        // 1 turn of the ring per minute
        fraction = (elapsedSeconds % 60L).toFloat() / 60f,
        accent = stopwatchColor,
        big = displayTime,
        bigColor = stopwatchColor,
        subtitle = subtitle,
        subtitleColor = palette.textSecondary,
        bottomPadding = bottomPadding,
        onClick = onOpenNotification,
        buttons = {
            if (lapAction != null && notification != null && !isPaused) {
                RingGlanceButton(
                    glyph = Icons.Rounded.Flag,
                    description = lapAction.title,
                    onClick = { notification.fireAction(context, lapAction) }
                )
            }
            if (toggleAction != null && notification != null) {
                RingGlanceButton(
                    glyph = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    description = toggleAction.title,
                    onClick = {
                        if (notification.fireAction(context, toggleAction) && !isPaused) {
                            tappedPauseAtSec = elapsedSeconds
                        }
                    }
                )
            }
            if (resetAction != null && notification != null) {
                RingGlanceButton(
                    glyph = Icons.Rounded.Refresh,
                    description = resetAction.title,
                    onClick = {
                        // Clear the card only if the press was delivered
                        if (notification.fireAction(context, resetAction)) {
                            SmartIslandRepositories.notificationRepository(context).removeNotification(notification.key)
                            onCollapse()
                        }
                    }
                )
            }
        },
        extra = extraChips
    )
}
