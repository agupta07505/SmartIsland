/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.PendingIntent
import com.agupta07505.smartisland.model.IslandNotificationAction
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerPausedReentryTest {
    private fun action(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())

    @Test
    fun firstSightOfAPausedTimerUsesTheTimeTheShadePrints() {
        // Paused inside the clock app: the island never saw it running at the pause, so the old running deadline is stale.
        val remaining = TimerFreeze.remainingMs(
            nowMs = 1_000_000L, rememberedFrozenMs = null, runningTargetMs = 1_240_000L,
            keptFrozenMs = null, textRemainingMs = 300_000L, shadeShownMs = 95_000L
        )
        assertEquals(95_000L, remaining)
    }

    @Test
    fun theSamePauseKeepsItsRememberedValueDespiteWholeSecondJitter() {
        val remaining = TimerFreeze.remainingMs(
            nowMs = 1_000_000L, rememberedFrozenMs = 95_000L, runningTargetMs = null,
            keptFrozenMs = null, textRemainingMs = null, shadeShownMs = 94_000L
        )
        assertEquals(95_000L, remaining)
    }

    @Test
    fun aNewPauseInsideTheAppReplacesTheStaleRememberedValue() {
        val remaining = TimerFreeze.remainingMs(
            nowMs = 1_000_000L, rememberedFrozenMs = 95_000L, runningTargetMs = null,
            keptFrozenMs = null, textRemainingMs = null, shadeShownMs = 40_000L
        )
        assertEquals(40_000L, remaining)
    }

    @Test
    fun withoutAShadeReadingNothingChanges() {
        assertEquals(
            95_000L,
            TimerFreeze.remainingMs(1_000_000L, 95_000L, null, null, 300_000L)
        )
        assertEquals(
            240_000L,
            TimerFreeze.remainingMs(1_000_000L, null, 1_240_000L, null, 300_000L)
        )
    }

    @Test
    fun offersPauseTellsRunningFromPaused() {
        assertTrue(ShadeActions.offersPause(listOf(action("Pause"), action("+1:00"))))
        assertFalse(ShadeActions.offersPause(listOf(action("Resume"), action("Stop"))))
        assertFalse(ShadeActions.offersPause(emptyList()))
    }

    @Test
    fun googleClockSortKeyExtractsRemainingAndElapsed() {
        val sortKey = "1 0|↺348|PAUSED|Σ0:01:00|Δ0:00:06|⏳0:00:54"
        assertEquals(54L, TimerStopwatchParser.parseGoogleClockSortKeyRemaining(sortKey))
        assertEquals(6L, TimerStopwatchParser.parseGoogleClockSortKeyElapsed(sortKey))
    }

    @Test
    fun googleClockSortKeyDetectsPausedState() {
        val notification = mockk<android.app.Notification>()
        io.mockk.every { notification.sortKey } returns "1 0|↺348|PAUSED|Σ0:01:00|Δ0:00:06|⏳0:00:54"
        notification.actions = null
        notification.extras = null
        notification.tickerText = null
        notification.flags = 0
        notification.`when` = 0L
        assertTrue(TimerStopwatchParser.isTimerPaused(notification))
        assertTrue(TimerStopwatchParser.isStopwatchPaused(notification))
        assertEquals(54L, TimerStopwatchParser.parseTimerRemainingSeconds(notification))
        assertEquals(6L, TimerStopwatchParser.parseStopwatchElapsedSeconds(notification))
    }

    @Test
    fun pausedTimerPrefersTextOverTitleTotalDuration() {
        val extras = mockk<android.os.Bundle>()
        io.mockk.every { extras.getCharSequence(android.app.Notification.EXTRA_TITLE) } returns "05:00"
        io.mockk.every { extras.getCharSequence(android.app.Notification.EXTRA_TEXT) } returns "03:42"
        io.mockk.every { extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT) } returns null
        io.mockk.every { extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT) } returns null
        io.mockk.every { extras.getCharSequence(android.app.Notification.EXTRA_INFO_TEXT) } returns null
        io.mockk.every { extras.getCharSequence(android.app.Notification.EXTRA_SUMMARY_TEXT) } returns null
        io.mockk.every { extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES) } returns null
        io.mockk.every { extras.containsKey(any()) } returns false
        io.mockk.every { extras.getBoolean(any(), any()) } returns false
        io.mockk.every { extras.getLong(any(), any()) } returns 0L

        val notification = mockk<android.app.Notification>()
        io.mockk.every { notification.sortKey } returns "PAUSED"
        notification.extras = extras
        notification.actions = null
        notification.tickerText = null
        notification.flags = 0
        notification.`when` = 0L

        // 3m 42s = 222s, NOT 5m = 300s!
        assertEquals(222L, TimerStopwatchParser.parseTimerRemainingSeconds(notification))
    }

    @Test
    fun pausedStopwatchKeepsPreviousFrozenValueWithoutWallClockDrift() {
        val previous = RecordingClock.State(startMillis = 100_000L, frozenMs = 15_000L)
        // 30 seconds of wall clock elapsed since pause, but stopwatch is still paused
        val nowMs = 130_000L
        val next = RecordingClock.next(
            previous = previous,
            nowMillis = nowMs,
            paused = true,
            finished = false,
            parsedMs = null,
            chronometerBaseMs = 100_000L, // unadjusted base would have given 30_000L drift!
            exactFrozenMs = null // TimerClock returned null because shownSec was null
        )
        // It should preserve the 15_000L frozen state, NOT 30_000L!
        assertEquals(15_000L, next.frozenMs)
        assertEquals(15_000L, next.elapsedMs(nowMs))
    }

    @Test
    fun islandNotificationFrozenTimePreservedInParser() {
        val pausedNotif = com.agupta07505.smartisland.model.IslandNotification(
            key = "key1",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "Timer",
            text = "",
            timeMillis = 100000L,
            recordingPaused = true,
            timerRemainingSeconds = 43L,
            recordingFrozenMs = 43_000L
        )
        assertEquals(43L, TimerStopwatchParser.parseTimerRemainingSeconds(pausedNotif))

        val pausedStopwatch = com.agupta07505.smartisland.model.IslandNotification(
            key = "key2",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "Stopwatch",
            text = "",
            timeMillis = 100000L,
            recordingPaused = true,
            stopwatchElapsedSeconds = 17L,
            recordingFrozenMs = 17_000L
        )
        assertEquals(17L, TimerStopwatchParser.parseTimerRemainingSeconds(pausedStopwatch))
    }

    @Test
    fun runningTimerDeadlineFloorsWithoutRoundingUp() {
        val notification = mockk<android.app.Notification>()
        io.mockk.every { notification.sortKey } returns null
        notification.extras = null
        notification.actions = null
        notification.tickerText = null
        notification.flags = 0
        val now = System.currentTimeMillis()
        notification.`when` = now + 55_700L // 55.7 seconds remaining

        assertEquals(55L, TimerStopwatchParser.parseTimerRemainingSeconds(notification))
    }
}
