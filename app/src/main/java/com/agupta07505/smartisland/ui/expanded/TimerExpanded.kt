/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.ActionRole
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.classifyAction
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ClockPackageRules
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.TimerClock
import com.agupta07505.smartisland.util.TimerStopwatchParser
import kotlinx.coroutines.delay

@Composable
fun TimerExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    if (OemDeviceRules.isMiuiOrHyperOs()) {
        TimerExpandedMiui(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = onOpenNotification,
            onCollapse = onCollapse,
            settings = settings
        )
    } else {
        TimerExpandedCurrent(
            notification = notification,
            bottomPadding = bottomPadding,
            onOpenNotification = onOpenNotification,
            onCollapse = onCollapse,
            settings = settings
        )
    }
}

@Composable
private fun TimerExpandedMiui(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val timerColor = Color(settings.timerColor)

    val resumeKeywords = remember {
        listOf("resume", "start", "play", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
    }
    val pauseKeywords = remember {
        listOf("pause", "pausa", "pausar", "sospendi", "interrompi", "onderbreek", "stoppa", "रोकें", "暂停", "一時停止", "пауза")
    }
    val pausedKeywords = remember {
        listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
    }
    val destructiveKeywords = remember {
        listOf("stop", "reset", "cancel", "delete", "dismiss", "clear", "annuler", "abbrechen", "eliminar", "borrar")
    }

    val isNotificationPaused = remember(notification?.key, notification?.actionIntents, notification?.text,
        notification?.title) {
        val actions = notification?.actionIntents.orEmpty()
        actions.any { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }

    var isPaused by remember(notification?.key, isNotificationPaused) {
        mutableStateOf(isNotificationPaused)
    }

    var targetTime by remember(notification?.key, notification?.timeMillis) {
        mutableLongStateOf(notification?.timeMillis ?: (System.currentTimeMillis() + 300000L))
    }

    var remainingSec by remember(notification?.key) {
        val parsed = notification?.let { TimerStopwatchParser.parseTimerRemainingSeconds(it) }
        val rem = if (parsed != null && parsed > 0) {
            parsed
        } else {
            val t = notification?.timeMillis ?: (System.currentTimeMillis() + 300000L)
            if (t > System.currentTimeMillis()) {
                ((t - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
        }
        mutableLongStateOf(rem)
    }

    LaunchedEffect(
        notification?.text,
        notification?.title,
        notification?.timerRemainingSeconds
    ) {
        if (isPaused && notification != null) {
            val pausedRemaining = notification.timerRemainingSeconds
                ?: TimerStopwatchParser.parseTimerRemainingSeconds(notification)

            if (pausedRemaining != null && pausedRemaining >= 0L) {
                remainingSec = pausedRemaining
            }
        }
    }

    LaunchedEffect(
        notification?.key,
        targetTime,
        isPaused,
        notification?.timerRemainingSeconds
    ) {
        if (isPaused) {
            notification?.timerRemainingSeconds?.let {
                remainingSec = it
            }
        } else {
            while (true) {
                val now = System.currentTimeMillis()
                val rem = if (targetTime > now) {
                    ((targetTime - now + 999L) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                remainingSec = rem
                if (rem <= 0L) break
                delay(100L)
            }
        }
    }

    val displayTime = remember(remainingSec) {
        TimerStopwatchParser.formatTime(remainingSec)
    }

    val palette = LocalIslandPalette.current
    var totalSec by remember(notification?.key) { mutableLongStateOf(remainingSec.coerceAtLeast(1L)) }
    LaunchedEffect(remainingSec) { if (remainingSec > totalSec) totalSec = remainingSec }
    val timeUp = remainingSec == 0L && !isPaused
    val fraction = if (timeUp) 0f else (remainingSec.toFloat() / maxOf(totalSec, remainingSec, 1L).toFloat())
        .coerceIn(0f, 1f)

    val timerSubtitle = timerSubtitleFor(timeUp, isPaused, notification)

    val pauseAction = miuiToggleAction(notification, isPaused, resumeKeywords, pauseKeywords, destructiveKeywords)
    val stopAction = notification?.actionIntents?.firstOrNull { act ->
        val t = act.title.lowercase()
        destructiveKeywords.any { t.contains(it) }
    }

    RingGlanceRow(
        glyph = Icons.Rounded.HourglassBottom,
        glyphDescription = "Timer",
        fraction = fraction,
        accent = timerColor,
        big = displayTime,
        bigColor = if (timeUp) Color(0xFFEF4444) else timerColor,
        subtitle = timerSubtitle,
        subtitleColor = palette.textSecondary,
        bottomPadding = bottomPadding,
        onClick = onOpenNotification,
        buttons = {
            MiuiTimerButtons(
                notification = notification,
                isPaused = isPaused,
                pauseAction = pauseAction,
                stopAction = stopAction,
                onCollapse = onCollapse
            )
        }
    )
}

@Composable
private fun MiuiTimerButtons(
    notification: IslandNotification?,
    isPaused: Boolean,
    pauseAction: IslandNotificationAction?,
    stopAction: IslandNotificationAction?,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    RingGlanceButton(
        glyph = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
        description = if (isPaused) "Resume" else "Pause",
        onClick = {
            val currentNotification = notification
            val action = pauseAction
            val pendingIntent = action?.pendingIntent

            if (currentNotification == null || action == null || pendingIntent == null) {
                AppLogRecorder.w("TimerExpanded", "no Clock pause/resume action available")
                Toast.makeText(context, "Clock Pause/Resume action unavailable", Toast.LENGTH_SHORT).show()
            } else {
                triggerAction(context, currentNotification.packageName, pendingIntent, action.title,
                    currentNotification.contentIntent)
            }
        }
    )
    RingGlanceButton(
        glyph = Icons.Rounded.Stop,
        description = "Stop",
        tint = Color(0xFFEF4444),
        onClick = {
            if (stopAction?.pendingIntent != null && notification != null) {
                triggerAction(context, notification.packageName, stopAction.pendingIntent, stopAction.title,
                    notification.contentIntent)
            }
            val repository = SmartIslandRepositories.notificationRepository(context)
            notification?.key?.let { repository.removeNotification(it) }
            onCollapse()
        }
    )
}

private fun miuiToggleAction(
    notification: IslandNotification?,
    isPaused: Boolean,
    resumeKeywords: List<String>,
    pauseKeywords: List<String>,
    destructiveKeywords: List<String>
): IslandNotificationAction? {
    return if (isPaused) {
        notification?.actionIntents?.firstOrNull { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } ?: notification?.actionIntents?.firstOrNull { act ->
            val t = act.title.lowercase()
            !destructiveKeywords.any { t.contains(it) } && !t.contains("+1") && !t.contains("add")
        }
    } else {
        notification?.actionIntents?.firstOrNull { act ->
            val t = act.title.lowercase()
            pauseKeywords.any { t.contains(it) }
        } ?: notification?.actionIntents?.firstOrNull { act ->
            val t = act.title.lowercase()
            !destructiveKeywords.any { t.contains(it) } && !t.contains("+1") && !t.contains("add")
        }
    }
}

@Composable
private fun TimerExpandedCurrent(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val timerColor = Color(settings.timerColor)

    val pausedKeywords = remember {
        listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
    }

    // the notification is the source of truth
    val isNotificationPaused = remember(notification?.key, notification?.actionIntents, notification?.text,
        notification?.title) {
        notification.offersResume() || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }

    // Optimistic flip after a tap
    var isPaused by remember(notification?.key, isNotificationPaused) {
        mutableStateOf(isNotificationPaused)
    }

    var targetTime by remember(notification?.key, notification?.timeMillis) {
        mutableLongStateOf(notification?.timeMillis ?: (System.currentTimeMillis() + 300000L))
    }

    // Exact cards truncate to whole seconds like the shade
    val exact = notification?.exactClock == true

    var remainingSec by remember(notification?.key) {
        mutableLongStateOf(startRemainingSec(notification, exact, isPaused))
    }

    // the paused remaining time the listener froze
    val frozenSec = if (isPaused && notification?.recordingPaused == true) {
        notification.recordingFrozenMs?.let { TimerClock.displaySeconds(it, exact, 500L) }
    } else {
        null
    }

    LaunchedEffect(notification?.text, notification?.title, frozenSec) {
        if (isPaused && notification != null) {
            if (frozenSec != null) {
                remainingSec = frozenSec
            } else {
                val parsed = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
                if (parsed != null && parsed > 0) {
                    remainingSec = parsed
                }
            }
        }
    }

    // "Time's up!" only after the real deadline; the last second reads 00:00
    var timeUp by remember(notification?.key, targetTime, isPaused) { mutableStateOf(false) }

    LaunchedEffect(notification?.key, targetTime, isPaused) {
        if (!isPaused) {
            while (true) {
                val now = System.currentTimeMillis()
                val remMs = (targetTime - now).coerceAtLeast(0L)
                val rem = TimerClock.displaySeconds(remMs, exact, 500L)
                remainingSec = rem
                if (exact) {
                    if (remMs <= 0L) {
                        timeUp = true
                        break
                    }
                    // Wake just after the shown second changes
                    delay(TimerClock.msToNextTick(remMs))
                } else {
                    if (rem <= 0L) break
                    delay(500L)
                }
            }
        }
    }
    val finished = if (exact) timeUp else remainingSec == 0L

    val shownSec = frozenSec ?: remainingSec
    val displayTime = remember(shownSec) {
        TimerStopwatchParser.formatTime(shownSec)
    }

    val palette = LocalIslandPalette.current

    var totalSec by remember(notification?.key) { mutableLongStateOf(shownSec.coerceAtLeast(1L)) }
    LaunchedEffect(shownSec) { if (shownSec > totalSec) totalSec = shownSec }
    val showTimeUp = finished && !isPaused
    val fraction = if (finished) 0f else (shownSec.toFloat() / maxOf(totalSec, shownSec, 1L).toFloat()).coerceIn(0f, 1f)

    val timerSubtitle = timerSubtitleFor(showTimeUp, isPaused, notification)

    // Each button is only shown when the timer app offers it
    val toggleAction = if (isPaused) notification.actionWithRole(ActionRole.Resume)
    else notification.actionWithRole(ActionRole.Pause)
    val addMinuteAction = notification.actionWithRole(ActionRole.AddMinute)
    val stopAction = notification.actionWithRole(ActionRole.Stop, ActionRole.Reset)
    val leftovers = if (notification != null) notification.leftoverActions(toggleAction, addMinuteAction,
        stopAction) else emptyList()
    val extraChips: (@Composable RowScope.() -> Unit)? =
        if (notification != null && leftovers.isNotEmpty()) ({
            // Any other button the timer app put in its notification
            leftovers.forEach { ShadeActionChip(notification, it, timerColor) }
        }) else null

    RingGlanceRow(
        glyph = Icons.Rounded.HourglassBottom,
        glyphDescription = "Timer",
        fraction = fraction,
        accent = timerColor,
        big = displayTime,
        bigColor = if (showTimeUp) palette.danger else timerColor,
        subtitle = timerSubtitle,
        subtitleColor = palette.textSecondary,
        bottomPadding = bottomPadding,
        onClick = onOpenNotification,
        buttons = {
            TimerButtons(
                notification = notification,
                isPaused = isPaused,
                toggleAction = toggleAction,
                addMinuteAction = addMinuteAction,
                stopAction = stopAction,
                onPauseToggled = {
                    val newPaused = !isPaused
                    isPaused = newPaused
                    targetTime = System.currentTimeMillis() + remainingSec * 1000L
                },
                onMinuteAdded = {
                    // Show the extra minute right away; the app's re-posted notification confirms it
                    if (isPaused) {
                        remainingSec += 60L
                    } else {
                        targetTime = maxOf(targetTime, System.currentTimeMillis()) + 60_000L
                        remainingSec += 60L
                    }
                },
                onCollapse = onCollapse
            )
        },
        extra = extraChips
    )
}

@Composable
private fun TimerButtons(
    notification: IslandNotification?,
    isPaused: Boolean,
    toggleAction: IslandNotificationAction?,
    addMinuteAction: IslandNotificationAction?,
    stopAction: IslandNotificationAction?,
    onPauseToggled: () -> Unit,
    onMinuteAdded: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    if (toggleAction != null && notification != null) {
        RingGlanceButton(
            glyph = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            description = toggleAction.title,
            onClick = {
                if (notification.fireAction(context, toggleAction)) {
                    onPauseToggled()
                }
            }
        )
    }

    if (addMinuteAction != null && notification != null) {
        RingGlanceButton(
            glyph = Icons.Rounded.Add,
            description = addMinuteAction.title,
            onClick = {
                if (notification.fireAction(context, addMinuteAction)) {
                    onMinuteAdded()
                }
            }
        )
    }

    // stop / Reset / Cancel: fires the app's own button, then clears the island card
    if (stopAction != null && notification != null) {
        RingGlanceButton(
            glyph = if (
                classifyAction(stopAction.title) == ActionRole.Reset &&
                ClockPackageRules.usesShadeFixes(notification.packageName)
            ) Icons.Rounded.Refresh else Icons.Rounded.Stop,
            description = stopAction.title,
            tint = palette.danger,
            onClick = {
                // The card only goes once the app's button was delivered
                if (notification.fireAction(context, stopAction)) {
                    val repository = SmartIslandRepositories.notificationRepository(context)
                    repository.sendCommand(SmartIslandCommand.TimerStopped(notification.packageName))
                    repository.removeNotification(notification.key)
                    onCollapse()
                }
            }
        )
    }
}

private fun startRemainingSec(notification: IslandNotification?, exact: Boolean, isPaused: Boolean): Long {
    val parsed = notification?.let { TimerStopwatchParser.parseTimerRemainingSeconds(it) }
    val startTarget = notification?.timeMillis ?: 0L
    val rem = if (exact && !isPaused && startTarget > System.currentTimeMillis()) {
        // The deadline beats the rounded text, which can be a second off
        TimerClock.secondsLeft(startTarget - System.currentTimeMillis())
    } else if (parsed != null && parsed > 0) {
        parsed
    } else {
        val t = notification?.timeMillis ?: (System.currentTimeMillis() + 300000L)
        if (t > System.currentTimeMillis()) {
            ((t - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
    }
    return rem
}

private fun timerSubtitleFor(showTimeUp: Boolean, isPaused: Boolean, notification: IslandNotification?): String {
    return if (showTimeUp) {
        "Time's up!"
    } else if (isPaused) {
        "Paused"
    } else {
        val notifTitle = notification?.title.orEmpty()
        val notifText = notification?.text.orEmpty()
        if (notifTitle.isNotBlank() && !notifTitle.equals("Timer", ignoreCase = true)
            && !notifTitle.matches(ClockOnlyRegex)) {
            notifTitle
        } else if (notifText.isNotBlank() && !notifText.equals("Timer", ignoreCase = true)
            && !notifText.matches(ClockOnlyRegex)) {
            notifText
        } else {
            "Timer Active"
        }
    }
}

private val ClockOnlyRegex = Regex("""^[\d:.]+$""")
