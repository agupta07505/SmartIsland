/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.ActionRole
import com.agupta07505.smartisland.model.FallbackTitles
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.classifyAction
import com.agupta07505.smartisland.util.CallStyleExtras
import com.agupta07505.smartisland.util.NotificationViewInspector
import com.agupta07505.smartisland.util.ShadeActions
import com.agupta07505.smartisland.util.TimerStopwatchParser
import com.agupta07505.smartisland.util.runCatchingLogged

internal object NotificationCardReader {
    private const val TAG = "SmartIslandNotificationListener"

    fun standardActionsOf(
        icons: NotificationIconLoader,
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode
    ): List<IslandNotificationAction> {
        return notification.actions?.mapNotNull { action ->
            action.title?.toString()?.let { title ->
                val remoteInput = action.remoteInputs?.firstOrNull()
                IslandNotificationAction(
                    title = title,
                    pendingIntent = action.actionIntent,
                    remoteInputKey = remoteInput?.resultKey ?: "key_text_reply",
                    hasRemoteInput = remoteInput != null,
                    icon = if (mode == IslandMode.Music) icons.action(action, sbn.packageName) else null
                )
            }
        }.orEmpty().let { base ->
            if (mode == IslandMode.IncomingCall) CallStyleExtras.withCallIntents(notification, base) else base
        }
    }

    @Suppress("DEPRECATION")
    fun miuiFocusActions(extras: Bundle): List<IslandNotificationAction> {
        return extras.getBundle("miui.focus.actions")
            ?.let { focusBundle ->
                runCatchingLogged(TAG, "MIUI focus actions could not be read") {
                    focusBundle.keySet().mapNotNull { actionKey ->
                        val focusAction =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                focusBundle.getParcelable(
                                    actionKey,
                                    Notification.Action::class.java
                                )
                            } else {
                                focusBundle.getParcelable<Notification.Action>(
                                    actionKey
                                )
                            } ?: return@mapNotNull null

                        focusAction.title?.toString()?.let { title ->
                            val remoteInput = focusAction.remoteInputs?.firstOrNull()

                            IslandNotificationAction(
                                title = title,
                                pendingIntent = focusAction.actionIntent,
                                remoteInputKey =
                                    remoteInput?.resultKey ?: "key_text_reply",
                                hasRemoteInput = remoteInput != null
                            )
                        }
                    }
                }
            }.orEmpty()
    }

    fun readCustomViews(
        context: Context,
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode,
        isShadeFixCard: Boolean
    ): NotificationViewInspector.Result? {
        val usesCustomViews = mode == IslandMode.Stopwatch || isShadeFixCard ||
            mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording
        return if (usesCustomViews) {
            runCatchingLogged(TAG, "Custom notification views could not be read") {
                NotificationViewInspector.inspect(context, notification, sbn.key)
            }
        } else {
            null
        }
    }

    fun resolveActions(
        sbn: StatusBarNotification,
        mode: IslandMode,
        baseActions: List<IslandNotificationAction>,
        viewInfo: NotificationViewInspector.Result?,
        isShadeFixCard: Boolean
    ): List<IslandNotificationAction> {
        return if (
            viewInfo != null && viewInfo.buttons.isNotEmpty() &&
            (mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording) &&
            baseActions.none {
                it.pendingIntent != null &&
                    classifyAction(it.title) in
                    arrayOf(
                        ActionRole.Pause,
                        ActionRole.Resume
                    )
            }
        ) {
            baseActions.filter { it.pendingIntent != null } + viewInfo.buttons
        } else if (isShadeFixCard) {
            ShadeActions.mirror(
                baseActions,
                viewInfo?.buttons.orEmpty(),
                if (mode == IslandMode.Stopwatch) {
                    TimerStopwatchParser.ClockKind.Stopwatch
                } else {
                    TimerStopwatchParser.ClockKind.Timer
                }
            )
        } else {
            baseActions
        }
    }

    fun miuiClockValues(
        notification: Notification,
        mode: IslandMode,
        existingNotif: IslandNotification?
    ): MiuiClockValues {
        val stopwatchIsPaused = mode == IslandMode.Stopwatch &&
            TimerStopwatchParser.isStopwatchPaused(notification)

        val stopwatchElapsedSeconds = if (stopwatchIsPaused) {
            val previousStopwatch = existingNotif?.takeIf { it.mode == IslandMode.Stopwatch }
            val parsedElapsed = TimerStopwatchParser
                .parseStopwatchElapsedSeconds(notification)

            parsedElapsed
                ?: previousStopwatch?.stopwatchElapsedSeconds
                ?: previousStopwatch?.let { previous ->
                    ((System.currentTimeMillis() - previous.timeMillis) / 1000L).coerceAtLeast(0L)
                }
        } else {
            null
        }

        val stopwatchChronometerBase = if (mode == IslandMode.Stopwatch && !stopwatchIsPaused) {
            notification.extras?.getLong("android.chronometerBase", 0L)?.takeIf { it > 0L }
        } else {
            null
        }

        val nowMillis = System.currentTimeMillis()
        val timerIsPaused = mode == IslandMode.Timer &&
            TimerStopwatchParser.isTimerPaused(notification)

        val timerRemainingSeconds = if (timerIsPaused) {
            val previousTimer = existingNotif?.takeIf { it.mode == IslandMode.Timer }

            previousTimer?.timerRemainingSeconds
                ?: previousTimer?.let { old ->
                    ((old.timeMillis - nowMillis + 999L) / 1000L).coerceAtLeast(0L)
                }
                ?: notification.`when`.takeIf { it > nowMillis }?.let { deadline ->
                    ((deadline - nowMillis + 999L) / 1000L).coerceAtLeast(0L)
                }
        } else {
            null
        }
        return MiuiClockValues(stopwatchElapsedSeconds, stopwatchChronometerBase, timerRemainingSeconds)
    }

    fun cardTitle(
        mediaInfo: MediaInfo?,
        extras: Bundle,
        notification: Notification,
        mode: IslandMode
    ): String =
        mediaInfo?.title?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: (if (mode == IslandMode.IncomingCall) CallStyleExtras.callerName(notification) else null)
            ?: (if (mode == IslandMode.Stopwatch) FallbackTitles.STOPWATCH else if (mode == IslandMode.Timer) FallbackTitles.TIMER else "")

    fun cardText(
        mediaInfo: MediaInfo?,
        extras: Bundle,
        notification: Notification,
        mode: IslandMode
    ): String =
        mediaInfo?.artist?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: notification.tickerText?.toString()
            ?: (if (mode == IslandMode.Stopwatch) "Running" else if (mode == IslandMode.Timer) "Running" else "")
}

internal class MiuiClockValues(
    val stopwatchElapsedSeconds: Long? = null,
    val stopwatchChronometerBase: Long? = null,
    val timerRemainingSeconds: Long? = null
)
