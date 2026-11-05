/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.util.NotificationViewInspector
import com.agupta07505.smartisland.util.RecordingClock
import com.agupta07505.smartisland.util.ShadeActions
import com.agupta07505.smartisland.util.TimerClock
import com.agupta07505.smartisland.util.TimerFreeze
import com.agupta07505.smartisland.util.TimerStopwatchParser
import com.agupta07505.smartisland.util.VoipCallSignal
import com.agupta07505.smartisland.util.isScreenRecordingComplete
import java.util.concurrent.ConcurrentHashMap

internal class NotificationClockReader {
    private val recordingStates = ConcurrentHashMap<String, RecordingClock.State>()
    private val lastClocks = ConcurrentHashMap<String, RecordingClock.State>()
    private val lastTimerTargets = ConcurrentHashMap<String, Long>()
    private val lastTimerFrozen = ConcurrentHashMap<String, Long>()
    private val lastTimerShown = ConcurrentHashMap<String, Long>()

    fun forgetRecording(key: String) {
        recordingStates.remove(key)
    }

    fun clearRecordings() {
        recordingStates.clear()
    }

    fun forgetTimer(packageName: String) {
        lastTimerFrozen.remove(packageName)
        lastTimerShown.remove(packageName)
        lastTimerTargets.remove(packageName)
    }

    fun clear() {
        recordingStates.clear()
        lastClocks.clear()
        lastTimerTargets.clear()
        lastTimerFrozen.clear()
        lastTimerShown.clear()
    }

    fun recordingStateFor(
        sbn: StatusBarNotification,
        notification: Notification,
        extras: Bundle,
        mode: IslandMode,
        viewInfo: NotificationViewInspector.Result?,
        actions: List<IslandNotificationAction>,
        isShadeFixCard: Boolean
    ): RecordingClock.State? {
        val isRecorder = mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording
        val isStopwatch = mode == IslandMode.Stopwatch
        return if (isRecorder || isStopwatch) {
            val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val rawBig = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val rawSub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val nowMs = System.currentTimeMillis()
            val clockKey = "${sbn.packageName}|${mode.name}"
            val clockPaused = if (isStopwatch) {
                // stays paused across the app's cancel-and-repost until it offers Pause again
                RecordingClock.resolvePaused(
                    detected = TimerStopwatchParser.isStopwatchPaused(notification) ||
                        (isShadeFixCard && ShadeActions.offersResume(actions)),
                    lastForApp = recordingStates[sbn.key] ?: lastClocks[clockKey],
                    actionTitles = actions.map { it.title }
                )
            } else {
                RecordingClock.isPaused(actions.map { it.title }, "$rawTitle $rawText $rawBig")
            }
            val previousState = RecordingClock.continuation(recordingStates[sbn.key], lastClocks[clockKey], clockPaused)
            // `when` is only the post time; the view's chronometer has the real elapsed time
            val viewStartMs = viewInfo?.chronometerElapsedMs
                ?.takeIf { !clockPaused }
                ?.let { nowMs - it }
            val chronometerBase = viewStartMs ?: if (!extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN,
                false) &&
                notification.`when` in 1..nowMs &&
                // `when` is trusted as the start only before the first clock
                (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) ||
                    (isStopwatch && previousState == null))
            ) notification.`when` else null
            val stockStopwatch = isShadeFixCard && isStopwatch
            val showsChronometer = !extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) &&
                extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) && notification.`when` in 1..nowMs
            val exactRunningBase: Long? = if (stockStopwatch && !clockPaused) {
                viewStartMs ?: notification.`when`.takeIf { showsChronometer }
            } else {
                null
            }
            // stopped chronometer corrected to the post time
            val exactFrozenMs: Long? = if (stockStopwatch && clockPaused) {
                viewInfo?.takeIf { !it.chronometerCountDown }?.let { cv ->
                    cv.chronometerBaseMs?.let { base ->
                        val applyDelayMs = (nowMs - sbn.postTime).coerceAtLeast(0L)
                        TimerClock.elapsedFromChronometer(
                            base,
                            SystemClock.elapsedRealtime() - applyDelayMs,
                            cv.shownTimeSeconds?.takeIf { cv.shownFromChronometer },
                            applyDelayMs
                        )
                    }
                }
            } else {
                null
            }
            RecordingClock.next(
                previous = previousState,
                nowMillis = nowMs,
                paused = clockPaused,
                finished = !isStopwatch && notification.isScreenRecordingComplete(),
                parsedMs = if (stockStopwatch) {
                    if (clockPaused) {
                        RecordingClock.frozenReading(
                            paused = true,
                            shownMs = viewInfo?.shownTimeSeconds?.let { it * 1000L }?.let { shown ->
                                if (viewInfo?.shownFromChronometer == true && !viewInfo.chronometerCountDown) {
                                    RecordingClock.withoutPostAge(shown, nowMs - sbn.postTime)
                                } else {
                                    shown
                                }
                            },
                            parsedMs = RecordingClock.parseElapsedMs(rawTitle, rawText, rawBig, rawSub)
                        )
                    } else if (exactRunningBase != null) {
                        null
                    } else {
                        RecordingClock.parseElapsedMs(rawTitle, rawText, rawBig, rawSub)
                    }
                } else {
                    RecordingClock.parseElapsedMs(rawTitle, rawText, rawBig, rawSub)
                },
                chronometerBaseMs = exactRunningBase ?: chronometerBase,
                fallbackFrozenMs = if (clockPaused) {
                    RecordingClock.withoutPostAge(viewInfo?.chronometerElapsedMs, nowMs - sbn.postTime)
                } else {
                    viewInfo?.chronometerElapsedMs
                },
                exactFrozenMs = exactFrozenMs,
                exactStart = exactRunningBase != null
            ).also {
                recordingStates[sbn.key] = it
                lastClocks[clockKey] = it
            }
        } else {
            null
        }
    }

    fun isTimerPausedPost(
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode,
        actions: List<IslandNotificationAction>,
        isShadeFixCard: Boolean
    ): Boolean =
        mode == IslandMode.Timer &&
            (TimerStopwatchParser.isTimerPaused(notification) ||
                (isShadeFixCard && ShadeActions.offersResume(actions)) ||
                (isShadeFixCard && lastTimerFrozen[sbn.packageName] != null &&
                    !ShadeActions.offersPause(actions) &&
                    !TimerStopwatchParser.isTimerFinished(notification)))

    fun frozenTimerMs(
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode,
        existingNotif: IslandNotification?,
        viewInfo: NotificationViewInspector.Result?,
        isShadeFixCard: Boolean,
        timerPaused: Boolean,
        timerViewShownMs: Long?
    ): Long? {
        return if (timerPaused) {
            val nowMs = System.currentTimeMillis()
            val pauseAtMs = sbn.postTime.coerceIn(nowMs - 10_000L, nowMs)
            val runningTarget = existingNotif
                ?.takeIf { it.mode == IslandMode.Timer && !it.recordingPaused && it.timeMillis > pauseAtMs }
                ?.timeMillis
                ?: lastTimerTargets[sbn.packageName]?.takeIf { it > pauseAtMs }
            val keptFrozen = existingNotif
                ?.takeIf { it.mode == IslandMode.Timer && it.recordingPaused }
                ?.recordingFrozenMs
            val clockTextSec = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
            val chronoView = viewInfo?.takeIf { isShadeFixCard && it.chronometerCountDown }
            val shadeExactMs = chronoView?.let { cv ->
                cv.chronometerBaseMs?.let { base ->
                    val applyDelayMs = (nowMs - sbn.postTime).coerceAtLeast(0L)
                    TimerClock.frozenFromChronometer(
                        base,
                        SystemClock.elapsedRealtime() - applyDelayMs,
                        cv.shownTimeSeconds?.takeIf { cv.shownFromChronometer },
                        applyDelayMs
                    )
                }
            }
            TimerFreeze.remainingMs(
                nowMs = pauseAtMs,
                rememberedFrozenMs = lastTimerFrozen[sbn.packageName],
                runningTargetMs = runningTarget,
                keptFrozenMs = keptFrozen,
                textRemainingMs = timerViewShownMs?.takeIf { it > 0L } ?: clockTextSec?.let { it * 1000L },
                shadeShownMs = if (isShadeFixCard && viewInfo?.shownFromChronometer == true) timerViewShownMs else null,
                shadeExactMs = shadeExactMs,
                shownMs = if (isShadeFixCard) timerViewShownMs?.takeIf { it > 0L } else null,
                previousShownMs = lastTimerShown[sbn.packageName]
            ).also {
                lastTimerFrozen[sbn.packageName] = it
                if (isShadeFixCard) timerViewShownMs?.takeIf { shown -> shown > 0L }
                    ?.let { shown -> lastTimerShown[sbn.packageName] = shown }
            }
        } else {
            if (mode == IslandMode.Timer) {
                lastTimerFrozen.remove(sbn.packageName)
                lastTimerShown.remove(sbn.packageName)
            }
            null
        }
    }

    fun cardTimeMillis(
        sbn: StatusBarNotification,
        notification: Notification,
        extras: Bundle,
        mode: IslandMode,
        existingNotif: IslandNotification?,
        recordingState: RecordingClock.State?,
        isNowRinging: Boolean,
        isNowConnecting: Boolean,
        timerFrozenMs: Long?,
        timerViewRunningMs: Long?,
        timerViewShownMs: Long?,
        timerPaused: Boolean,
        isShadeFixCard: Boolean,
        isMiuiRom: Boolean
    ): Long {
        val voipLive = VoipCallSignal.active
        return when {
            recordingState != null -> recordingState.startMillis
            // a running call's chronometer start is the real call start
            mode == IslandMode.IncomingCall && !isNowRinging &&
                extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) &&
                notification.`when` in 1..System.currentTimeMillis() -> notification.`when`
            // the call was answered (incoming) or picked up (outgoing): the clock starts now
            mode == IslandMode.IncomingCall && existingNotif != null && !isNowRinging && !isNowConnecting &&
                (existingNotif.isCallRinging || existingNotif.isCallConnecting) -> {
                System.currentTimeMillis()
            }
            existingNotif != null && mode == IslandMode.IncomingCall && !isNowRinging
                && existingNotif.timeMillis > 0 -> {
                existingNotif.timeMillis
            }
            mode == IslandMode.IncomingCall && !isNowRinging && voipLive && existingNotif == null &&
                VoipCallSignal.sinceMillis > 0 ->
                VoipCallSignal.sinceMillis
            mode == IslandMode.Timer && timerFrozenMs != null -> System.currentTimeMillis() + timerFrozenMs
            // the shade's own count-down chronometer is the exact time left
            mode == IslandMode.Timer && timerViewRunningMs != null -> {
                val target = System.currentTimeMillis() + timerViewRunningMs
                lastTimerTargets[sbn.packageName] = target
                target
            }
            // No count-down chronometer, but the shade prints the time ("04:59") in the view
            mode == IslandMode.Timer && !timerPaused && (timerViewShownMs ?: 0L) > 0L -> {
                // whole-second text: the real time left is up to a second more than it prints
                val target = TimerClock
                    .targetFromShownSeconds(System.currentTimeMillis(), timerViewShownMs!! / 1000L)
                lastTimerTargets[sbn.packageName] = target
                target
            }
            mode == IslandMode.Timer ->
                timerTargetMillis(sbn, notification, existingNotif, isShadeFixCard, isMiuiRom)
            mode == IslandMode.Stopwatch -> stopwatchStartMillis(notification, existingNotif)
            notification.`when` != 0L -> notification.`when`
            else -> sbn.postTime
        }
    }

    private fun timerTargetMillis(
        sbn: StatusBarNotification,
        notification: Notification,
        existingNotif: IslandNotification?,
        isShadeFixCard: Boolean,
        isMiuiRom: Boolean
    ): Long {
        return if (isMiuiRom) {
            val preciseDeadline = TimerStopwatchParser
                .parseTimerDeadlineMillis(notification)
            if (preciseDeadline != null) {
                lastTimerTargets[sbn.packageName] = preciseDeadline
                preciseDeadline
            } else {
                val remSec = TimerStopwatchParser
                    .parseTimerRemainingSeconds(notification)
                val target = if (remSec != null && remSec > 0) {
                    System.currentTimeMillis() + remSec * 1000L
                } else if (notification.`when` > System.currentTimeMillis()) {
                    notification.`when`
                } else if (existingNotif != null && existingNotif.mode == IslandMode.Timer
                    && existingNotif.timeMillis > System.currentTimeMillis()) {
                    existingNotif.timeMillis
                } else {
                    System.currentTimeMillis()
                }
                lastTimerTargets[sbn.packageName] = target
                target
            }
        } else {
            val remSec = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
            // a stock / third-party clock's own deadline is exact to the millisecond
            val exactDeadline = if (isShadeFixCard) {
                TimerStopwatchParser.parseTimerDeadlineExact(notification)
            } else {
                null
            }
            val target = if (exactDeadline != null) {
                exactDeadline
            } else if (remSec != null && remSec > 0) {
                if (isShadeFixCard) {
                    TimerClock
                        .targetFromShownSeconds(System.currentTimeMillis(), remSec)
                } else {
                    System.currentTimeMillis() + remSec * 1000L
                }
            } else if (notification.`when` > System.currentTimeMillis()) {
                notification.`when`
            } else if (existingNotif != null && existingNotif.mode == IslandMode.Timer
                && existingNotif.timeMillis > System.currentTimeMillis()) {
                existingNotif.timeMillis
            } else {
                System.currentTimeMillis()
            }
            lastTimerTargets[sbn.packageName] = target
            target
        }
    }

    private fun stopwatchStartMillis(notification: Notification, existingNotif: IslandNotification?): Long {
        val elSec = TimerStopwatchParser.parseStopwatchElapsedSeconds(notification)
        return if (elSec != null && elSec > 0) {
            System.currentTimeMillis() - elSec * 1000L
        } else if (existingNotif != null && existingNotif.mode == IslandMode.Stopwatch
            && existingNotif.timeMillis > 0) {
            existingNotif.timeMillis
        } else if (notification.`when` in 1..System.currentTimeMillis()) {
            notification.`when`
        } else {
            System.currentTimeMillis()
        }
    }
}
