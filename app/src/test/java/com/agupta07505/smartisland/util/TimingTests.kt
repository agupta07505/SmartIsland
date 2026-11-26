/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.app.PendingIntent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.model.TransferState
import com.agupta07505.smartisland.util.SpeedTracker.Phase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordingClockTest {

    @Test
    fun parsesMinutesSecondsAndHours() {
        assertEquals(26_000L, RecordingClock.parseElapsedMs("Currently recording 00:26"))
        assertEquals(3_723_000L, RecordingClock.parseElapsedMs("Voice Recorder", null, "Recording paused • 1:02:03"))
        assertNull(RecordingClock.parseElapsedMs("Recorder", null, "no time here"))
    }

    @Test
    fun pausedOnResumeOrPausedText() {
        assertTrue(RecordingClock.isPaused(listOf("Resume recording", "Save recording"), "Recording paused 00:30"))
        assertTrue(RecordingClock.isPaused(emptyList(), "Recording paused"))
        assertFalse(RecordingClock.isPaused(listOf("Pause recording", "Save recording"), "Currently recording 00:26"))
    }

    @Test
    fun runningClockFollowsRecorder() {
        val first = RecordingClock.next(null, 100_000L, paused = false, finished = false, parsedMs = 26_000L, chronometerBaseMs = null)
        val second = RecordingClock.next(first, 101_050L, paused = false, finished = false, parsedMs = 27_000L, chronometerBaseMs = null)
        assertEquals(first.startMillis, second.startMillis)
        assertNull(second.frozenMs)
    }

    @Test
    fun pauseFreezesResumeContinues() {
        val running = RecordingClock.next(null, 0L, paused = false, finished = false, parsedMs = 30_000L, chronometerBaseMs = null)
        val paused = RecordingClock.next(running, 10_000L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(40_000L, paused.frozenMs)
        val stillPaused = RecordingClock.next(paused, 90_000L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(40_000L, stillPaused.elapsedMs(500_000L))
        val resumed = RecordingClock.next(stillPaused, 100_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertNull(resumed.frozenMs)
        assertEquals(45_000L, resumed.elapsedMs(105_000L))
    }

    @Test
    fun finishedRecordingNeverTicks() {
        val running = RecordingClock.next(null, 0L, paused = false, finished = false, parsedMs = 12_000L, chronometerBaseMs = null)
        val done = RecordingClock.next(running, 3_000L, paused = false, finished = true, parsedMs = null, chronometerBaseMs = null)
        assertEquals(15_000L, done.elapsedMs(1_000_000L))
    }

    @Test
    fun pausedNoReadableKeepsTime() {
        val running = RecordingClock.next(null, 104_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 100_000L)
        val paused = RecordingClock.next(running, 104_050L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(4_050L, paused.frozenMs)
        assertEquals(4_050L, paused.elapsedMs(104_050L))
        assertEquals(4_050L, paused.elapsedMs(160_000L))
        val resumed = RecordingClock.next(paused, 200_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertNull(resumed.frozenMs)
        assertEquals(4_050L, resumed.elapsedMs(200_000L))
        assertEquals(9_050L, resumed.elapsedMs(205_000L))
    }

    @Test
    fun pausedClockFallback() {
        val zeroRunning = RecordingClock.next(null, 104_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 100_000L)
        val zeroPaused = RecordingClock.next(zeroRunning, 104_050L, paused = true, finished = false, parsedMs = 0L, chronometerBaseMs = null)
        assertEquals(4_050L, zeroPaused.frozenMs)

        val noHistoryPaused = RecordingClock.next(null, 50_000L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null, fallbackFrozenMs = 7_000L)
        assertEquals(7_000L, noHistoryPaused.frozenMs)

        val repostRunning = RecordingClock.next(null, 104_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 100_000L)
        val repostPaused = RecordingClock.next(repostRunning, 104_050L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        val previous = RecordingClock.continuation(own = null, lastForApp = repostPaused, paused = true)
        val repostReposted = RecordingClock.next(previous, 164_050L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(4_050L, repostReposted.frozenMs)
        assertEquals(4_050L, repostReposted.elapsedMs(170_000L))

        val frozen = RecordingClock.State(startMillis = 0L, frozenMs = 4_000L)
        val frozenReposted = RecordingClock.next(frozen, 164_000L, paused = true, finished = false, parsedMs = 64_000L, chronometerBaseMs = null)
        assertEquals(4_000L, frozenReposted.frozenMs)
        assertEquals(4_000L, frozenReposted.elapsedMs(999_000L))

        val fallback = RecordingClock.withoutPostAge(125_000L, 5_000L)
        val state = RecordingClock.next(
            null, 900_000L, paused = true, finished = false, parsedMs = null,
            chronometerBaseMs = null, fallbackFrozenMs = fallback
        )
        assertEquals(120_000L, state.frozenMs)
    }

    @Test
    fun runningPostNoBorrowedClock() {
        val frozen = RecordingClock.State(startMillis = 0L, frozenMs = 4_000L)
        assertNull(RecordingClock.continuation(own = null, lastForApp = frozen, paused = false))
        assertEquals(frozen, RecordingClock.continuation(own = null, lastForApp = frozen, paused = true))
    }

    @Test
    fun pausedStopwatchStaysPausedOnRepost() {
        val frozen = RecordingClock.State(startMillis = 0L, frozenMs = 4_000L)
        assertTrue(RecordingClock.resolvePaused(detected = false, lastForApp = frozen, actionTitles = emptyList()))
        assertTrue(RecordingClock.resolvePaused(detected = false, lastForApp = frozen, actionTitles = listOf("Lap", "Reset")))
    }

    @Test
    fun pausedRunsAgainOnPauseOffer() {
        val frozen = RecordingClock.State(startMillis = 0L, frozenMs = 4_000L)
        assertFalse(RecordingClock.resolvePaused(detected = false, lastForApp = frozen, actionTitles = listOf("Pause", "Lap")))
        val running = RecordingClock.State(startMillis = 0L, frozenMs = null)
        assertFalse(RecordingClock.resolvePaused(detected = false, lastForApp = running, actionTitles = emptyList()))
        assertFalse(RecordingClock.resolvePaused(detected = false, lastForApp = null, actionTitles = emptyList()))
        assertTrue(RecordingClock.resolvePaused(detected = true, lastForApp = null, actionTitles = emptyList()))
    }

    @Test
    fun exactPauseBeatsFrozenValue() {
        val frozen = RecordingClock.State(startMillis = 0L, frozenMs = 120_000L)
        val again = RecordingClock.next(
            frozen, 500_000L, paused = true, finished = false, parsedMs = 187_000L,
            chronometerBaseMs = null, exactFrozenMs = 120_300L
        )
        assertEquals(120_300L, again.frozenMs)
    }

    @Test
    fun noExactReadingStaysFrozen() {
        val frozen = RecordingClock.State(startMillis = 0L, frozenMs = 120_000L)
        val again = RecordingClock.next(
            frozen, 500_000L, paused = true, finished = false, parsedMs = 187_000L, chronometerBaseMs = null
        )
        assertEquals(120_000L, again.frozenMs)
    }

    @Test
    fun exactBaseNotSmoothed() {
        val first = RecordingClock.next(null, 100_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 70_000L, exactStart = true)
        val second = RecordingClock.next(first, 101_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 99_800L, exactStart = true)
        assertEquals(99_800L, second.startMillis)
    }

    @Test
    fun stoppedReadingLosesPostAge() {
        assertEquals(120_000L, RecordingClock.withoutPostAge(123_500L, 3_500L))
        assertEquals(120_000L, RecordingClock.withoutPostAge(120_000L, -50L))
        assertNull(RecordingClock.withoutPostAge(2_000L, 5_000L))
        assertNull(RecordingClock.withoutPostAge(null, 1_000L))
    }
}

class RunSuspendCatchingTest {
    @Test
    fun holdsAnOrdinaryFailure() = runTest {
        val result = runSuspendCatching { error("boom") }
        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun runsSuspendCallsInTheBlock() = runTest {
        assertEquals(1, runSuspendCatching { delay(10); 1 }.getOrNull())
    }

    @Test
    fun rethrowsACancellationException() = runTest {
        var thrown: Throwable? = null
        try {
            runSuspendCatching { throw CancellationException("stop") }
        } catch (e: CancellationException) {
            thrown = e
        }
        assertEquals("stop", thrown?.message)
    }

    @Test
    fun cancelledCallerStopsAtTheBlock() = runTest {
        var reached = false
        val job = launch {
            runSuspendCatching { delay(1_000) }
            reached = true
        }
        yield()
        job.cancel()
        job.join()
        assertFalse(reached)
        assertTrue(job.isCancelled)
    }
}

class SpeedTrackerTest {
    private val mb = 1024L * 1024L

    @Test
    fun startsAfterSustainedTraffic() {
        val t = SpeedTracker()
        assertEquals(Phase.IDLE, t.onSample(1_000, 2 * mb, 0))
        assertEquals(Phase.IDLE, t.onSample(2_000, 2 * mb, 0))
        assertEquals(Phase.IDLE, t.onSample(4_000, 2 * mb, 0))
        assertEquals(Phase.DOWNLOAD, t.onSample(5_000, 2 * mb, 0))
    }

    @Test
    fun lightTrafficNeverStarts() {
        val t = SpeedTracker()
        for (s in 1..20) assertEquals(Phase.IDLE, t.onSample(s * 1_000L, 300 * 1024L, 10 * 1024L))
    }

    @Test
    fun uploadIsRecognisedByOutgoingTraffic() {
        val t = SpeedTracker()
        var phase = Phase.IDLE
        for (s in 1..6) phase = t.onSample(s * 1_000L, 100 * 1024L, 2 * mb)
        assertEquals(Phase.UPLOAD, phase)
    }

    @Test
    fun shortStallDoesNotDropTheCard() {
        val t = SpeedTracker()
        for (s in 1..5) t.onSample(s * 1_000L, 2 * mb, 0)
        assertEquals(Phase.DOWNLOAD, t.onSample(6_000, 0, 0))
        assertEquals(Phase.DOWNLOAD, t.onSample(8_000, 0, 0))
        assertEquals(Phase.DOWNLOAD, t.onSample(9_000, 2 * mb, 0))
    }

    @Test
    fun endsAfterSixQuietSeconds() {
        val t = SpeedTracker()
        for (s in 1..5) t.onSample(s * 1_000L, 2 * mb, 0)
        assertEquals(Phase.DOWNLOAD, t.onSample(10_000, 0, 0))
        assertEquals(Phase.IDLE, t.onSample(11_000, 0, 0))
    }

    @Test
    fun formatsSpeeds() {
        assertEquals("2.3 MB/s", SpeedTracker.formatSpeed(2_411_724L))
        assertEquals("512 KB/s", SpeedTracker.formatSpeed(512L * 1024L))
    }
}

class StockClockPausedStopwatchTest {

    @Before
    fun reset() = TimerStopwatchParser.forgetClockKinds()

    private fun extras(): Bundle {
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) } returns false
        every { extras.getBoolean("android.showChronometer", false) } returns false
        every { extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) } returns false
        every { extras.getBoolean("android.chronometerCountDown", false) } returns false
        every { extras.getLong("android.chronometerBase", 0L) } returns 0L
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "00:12"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_INFO_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) } returns null
        every { extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) } returns null
        return extras
    }

    private fun notification(vararg labels: String): Notification {
        val acts = labels.map { label ->
            mockk<Notification.Action>().also { it.title = label }
        }
        val n = mockk<Notification>()
        n.extras = extras()
        n.flags = Notification.FLAG_ONGOING_EVENT
        n.actions = acts.toTypedArray()
        n.`when` = 0L
        n.tickerText = null
        n.category = null
        return n
    }

    @Test
    fun pausedStockStopwatchStaysAStopwatch() {
        val pkg = "com.google.android.deskclock"
        val key = "$pkg#7"
        assertTrue(TimerStopwatchParser.isStopwatch(notification("Pause", "Lap"), pkg, key))
        val paused = notification("Resume", "Stop")
        assertTrue(TimerStopwatchParser.isStopwatch(paused, pkg, key))
        assertFalse(TimerStopwatchParser.isTimer(paused, pkg, key))
    }

    @Test
    fun otherClockAppsKeepTheirOldDetection() {
        val pkg = "com.miui.deskclock"
        val key = "$pkg#7"
        TimerStopwatchParser.isStopwatch(notification("Pause", "Lap"), pkg, key)
        assertFalse(TimerStopwatchParser.isStopwatch(notification("Resume", "Stop"), pkg, key))
    }

    @Test
    fun shadeClockPackages() {
        assertTrue(ClockPackageRules.usesShadeFixes("com.google.android.deskclock"))
        assertTrue(ClockPackageRules.usesShadeFixes("com.android.deskclock"))
        assertTrue(ClockPackageRules.usesShadeFixes("com.jee.timer"))
        assertTrue(ClockPackageRules.usesShadeFixes("com.example.coolstopwatch"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.miui.deskclock"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.xiaomi.somethingclock"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.sec.android.app.clockpackage"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.oneplus.deskclock"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.android.bbkclock"))
        assertFalse(ClockPackageRules.usesShadeFixes("com.huawei.deskclock"))
    }

    @Test
    fun pausedThirdPartyStaysStopwatch() {
        val pkg = "com.jee.timer"
        val key = "$pkg#3"
        assertTrue(TimerStopwatchParser.isStopwatch(notification("Pause", "Lap"), pkg, key))
        assertTrue(TimerStopwatchParser.isStopwatch(notification("Resume", "Stop"), pkg, key))
    }

    @Test
    fun pausedFreezesAtShadeTime() {
        val reading = RecordingClock.frozenReading(paused = true, shownMs = 12_000L, parsedMs = 0L)
        assertEquals(12_000L, reading)
        val state = RecordingClock.next(
            previous = null, nowMillis = 100_000L, paused = true, finished = false,
            parsedMs = reading, chronometerBaseMs = null, fallbackFrozenMs = 99_000L
        )
        assertEquals(12_000L, state.frozenMs)
        assertEquals(12_000L, state.elapsedMs(500_000L))
    }

    @Test
    fun runningIgnoresPrintedTime() {
        assertEquals(5_000L, RecordingClock.frozenReading(paused = false, shownMs = 12_000L, parsedMs = 5_000L))
        assertEquals(null, RecordingClock.frozenReading(paused = false, shownMs = 12_000L, parsedMs = null))
    }

    @Test
    fun zeroPrintedFallsToText() {
        assertEquals(7_000L, RecordingClock.frozenReading(paused = true, shownMs = 0L, parsedMs = 7_000L))
        assertEquals(7_000L, RecordingClock.frozenReading(paused = true, shownMs = null, parsedMs = 7_000L))
    }
}

class TimerClockTest {
    @Before
    fun resetParserState() {
        // The parser keeps notification-kind history across calls. Keep each test independent.
        TimerStopwatchParser.forgetClockKinds()
    }

    @Test
    fun secondsTruncatedLikeShade() {
        assertEquals(4L, TimerClock.secondsLeft(4_999L))
        assertEquals(5L, TimerClock.secondsLeft(5_000L))
        assertEquals(0L, TimerClock.secondsLeft(999L))
        assertEquals(0L, TimerClock.secondsLeft(-50L))
    }

    @Test
    fun legacyCardsKeepTheirOwnRounding() {
        assertEquals(5L, TimerClock.displaySeconds(4_600L, exact = false, legacyBiasMs = 500L))
        assertEquals(5L, TimerClock.displaySeconds(4_100L, exact = false, legacyBiasMs = 999L))
        assertEquals(4L, TimerClock.displaySeconds(4_999L, exact = true, legacyBiasMs = 999L))
    }

    @Test
    fun tickLandsJustAfterTheSecondChanges() {
        assertEquals(405L, TimerClock.msToNextTick(4_400L))
        assertEquals(5L, TimerClock.msToNextTick(4_000L))
    }

    @Test
    fun wholeSecondShowsNow() {
        val target = TimerClock.targetFromShownSeconds(10_000L, 299L)
        assertEquals(299L, TimerClock.secondsLeft(target - 10_000L))
        // and moves on within a second
        assertEquals(298L, TimerClock.secondsLeft(target - 11_000L))
    }

    @Test
    fun pauseValueIsBaseMinusPost() {
        // paused with 12:00.250 left; the post is 3.4 s old when inflated, so the view prints 11:56 (truncated)
        val postElapsed = 1_000_000L
        val base = postElapsed + 720_250L
        val shown = (720_250L - 3_400L) / 1000L
        assertEquals(720_250L, TimerClock.frozenFromChronometer(base, postElapsed, shown, 3_400L))
    }

    @Test
    fun unrelatedBaseRejected() {
        // the app does not use the base as a deadline: base - postElapsed disagrees with the printed text
        assertNull(TimerClock.frozenFromChronometer(5_000_000L, 1_000_000L, 30L, 100L))
        assertNull(TimerClock.frozenFromChronometer(900_000L, 1_000_000L, null, 0L))
    }

    @Test
    fun pausedStopwatchRecovery() {
        // paused at 05:07.300. Inflated `age` ms after the post the stopped chronometer prints elapsed + age
        val postElapsed = 2_000_000L
        val base = postElapsed - 307_300L
        for (age in listOf(0L, 40L, 999L, 1_000L, 25_000L, 600_000L)) {
            val shown = (307_300L + age) / 1000L
            assertEquals("age $age", 307_300L, TimerClock.elapsedFromChronometer(base, postElapsed, shown, age))
        }
    }

    @Test
    fun mismatchedStopwatchBase() {
        assertNull(TimerClock.elapsedFromChronometer(1_000_000L, 2_000_000L, 5L, 0L))
        assertNull(TimerClock.elapsedFromChronometer(2_500_000L, 2_000_000L, null, 0L))
    }

    @Test
    fun countUpTickAfterBoundary() {
        assertEquals(605L, TimerClock.msToNextTickUp(400L))
        assertEquals(1_005L, TimerClock.msToNextTickUp(3_000L))
    }
}

class TimerStopwatchParserTest {

    @Before
    fun resetParserState() {
        // Parser history is process-wide; isolate each test from earlier notifications.
        TimerStopwatchParser.forgetClockKinds()
    }

    private fun createBaseExtras(): Bundle {
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) } returns false
        every { extras.getBoolean("android.showChronometer", false) } returns false
        every { extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) } returns false
        every { extras.getBoolean("android.chronometerCountDown", false) } returns false
        every { extras.getLong("android.chronometerBase", 0L) } returns 0L
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_INFO_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) } returns null
        every { extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) } returns null
        return extras
    }

    private fun extrasOf(
        title: String? = null,
        text: String? = null,
        subText: String? = null,
        chronometer: Boolean = false
    ): Bundle {
        val extras = createBaseExtras()
        if (title != null) every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns title
        if (text != null) every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns text
        if (subText != null) every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns subText
        if (chronometer) every { extras.getBoolean("android.showChronometer", false) } returns true
        return extras
    }

    private fun notificationOf(
        extras: Bundle,
        actions: List<String>? = null,
        flags: Int = 0,
        category: String? = null
    ): Notification {
        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = flags
        notification.actions = actions?.map { label ->
            mockk<Notification.Action>().also { it.title = label }
        }?.toTypedArray()
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = category
        return notification
    }

    private fun postedBy(pkg: String, notification: Notification): StatusBarNotification {
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns pkg
        every { sbn.notification } returns notification
        return sbn
    }

    private fun remainingOf(text: String? = null, subText: String? = null) =
        TimerStopwatchParser.parseTimerRemainingSeconds(
            notificationOf(extrasOf(title = "Timer", text = text, subText = subText))
        )

    @Test
    fun googleClockTimerDetection() {
        val extras = extrasOf(title = "Timer", text = "04:59")
        val notification = notificationOf(extras, listOf("Pause", "Reset"), Notification.FLAG_ONGOING_EVENT)
        val sbn = postedBy("com.google.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun xiaomiClockTimerDetection() {
        val extras = extrasOf(title = "00:04:55", subText = "Timer")
        val notification = notificationOf(extras, listOf("Pause", "Cancel"), Notification.FLAG_ONGOING_EVENT)
        val sbn = postedBy("com.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun samsungClockTimerDetection() {
        val extras = extrasOf(title = "Tea Timer", text = "02:15")
        val notification = notificationOf(extras, listOf("Cancel", "Pause"), Notification.FLAG_ONGOING_EVENT)
        val sbn = postedBy("com.sec.android.app.clockpackage", notification)

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun stopwatchDetectionWithLapAction() {
        val extras = extrasOf(title = "Stopwatch", text = "00:14.28")
        val notification = notificationOf(extras, listOf("Lap", "Pause"), Notification.FLAG_ONGOING_EVENT)
        val sbn = postedBy("com.google.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isStopwatch(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
    }

    @Test
    fun stopwatchDetectionWithChronometer() {
        val extras = extrasOf(title = "00:14.28", chronometer = true)
        val notification = notificationOf(extras, listOf("Pause"), Notification.FLAG_ONGOING_EVENT)
        val sbn = postedBy("com.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isStopwatch(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
    }

    @Test
    fun timerRemainingFromTextOrSub() {
        assertEquals(330L, remainingOf(text = "05:30"))
        assertEquals(5L, remainingOf(text = "0:05"))
        assertEquals(165L, remainingOf(subText = "02:45"))
    }

    @Test
    fun stopwatchElapsedSecondsParsing() {
        val extras = extrasOf(title = "Stopwatch", text = "00:14.28")
        val elapsed = TimerStopwatchParser.parseStopwatchElapsedSeconds(notificationOf(extras))
        assertEquals(14L, elapsed)
    }

    @Test
    fun timerFinishedDetection() {
        val extras = extrasOf(title = "Timer", text = "Time's up")
        assertTrue(TimerStopwatchParser.isTimerFinished(notificationOf(extras)))
    }

    @Test
    fun formatTimeMinSecOrHours() {
        assertEquals("05:30", TimerStopwatchParser.formatTime(330L))
        assertEquals("00:45", TimerStopwatchParser.formatTime(45L))
        assertEquals("1:02:15", TimerStopwatchParser.formatTime(3735L))
    }

    @Test
    fun alarmNotTimerOrStopwatch() {
        val extras = extrasOf(title = "Alarm", text = "07:00 AM")
        val notification = notificationOf(
            extras, listOf("Snooze", "Dismiss"), Notification.FLAG_ONGOING_EVENT, Notification.CATEGORY_ALARM
        )
        val sbn = postedBy("com.google.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isAlarm(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun upcomingAlarmNotClock() {
        val extras = extrasOf(title = "Upcoming alarm", text = "08:30 AM")
        val notification = notificationOf(extras, listOf("Dismiss now"), category = Notification.CATEGORY_ALARM)
        val sbn = postedBy("com.google.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isAlarm(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun pausedTimerIsTimer() {
        val extras = extrasOf(title = "04:52", text = "Paused")
        val notification = notificationOf(extras, listOf("Resume", "Reset"))
        val sbn = postedBy("com.google.android.deskclock", notification)

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
        assertTrue(TimerStopwatchParser.isTimerPaused(notification))
        assertFalse(TimerStopwatchParser.isTimerFinished(notification))
    }

    @Test
    fun finishedTimerNotPlusOneMinute() {
        val extras = extrasOf(title = "Timer", text = "Paused")
        val notification = notificationOf(extras, listOf("+1:00", "Resume"))
        assertFalse(TimerStopwatchParser.isTimerFinished(notification))
    }

    @Test
    fun pausedTimerReadsRemaining() {
        val card = IslandNotification(
            key = "k",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "30M TIMER",
            text = "29:52",
            timeMillis = 0L
        )
        assertEquals(29L * 60 + 52, TimerStopwatchParser.parseTimerRemainingSeconds(card))
    }

    @Test
    fun unitTitleWorksNoClockText() {
        val card = IslandNotification(
            key = "k",
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "5 min timer",
            text = "Running",
            timeMillis = 0L
        )
        assertEquals(300L, TimerStopwatchParser.parseTimerRemainingSeconds(card))
    }

    private fun chatLikeNotification(text: String, pkg: String): StatusBarNotification {
        val extras = extrasOf(title = "Working on it", text = text)
        return postedBy(pkg, notificationOf(extras, flags = Notification.FLAG_ONGOING_EVENT))
    }

    @Test
    fun otherAppStopwatchTextIgnored() {
        val sbn = chatLikeNotification("Next I'm matching the stopwatch card and the elapsed time", "com.example.assistant")
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun lapInsideAnotherWordDoesNotCount() {
        val sbn = chatLikeNotification("Checking the overlap and the collapse animation", "com.example.assistant")
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }
}

class TimerFreezeTest {
    private val min = 60_000L

    @Test
    fun pausedTimerAdjustment() {
        assertEquals(12 * min, TimerFreeze.remainingMs(1_000L, null, 1_000L + 12 * min, null, 30 * min))
        assertEquals(12 * min, TimerFreeze.remainingMs(400_000L, 12 * min, 1_000L + 12 * min, null, 30 * min))
        assertEquals(12 * min, TimerFreeze.remainingMs(100_000L, 12 * min, 100_000L + 3 * min, null, null))
        assertEquals(7 * min, TimerFreeze.remainingMs(0L, null, null, 7 * min, 30 * min))
        assertEquals(30 * min, TimerFreeze.remainingMs(0L, null, null, null, 30 * min))
        assertEquals(0L, TimerFreeze.remainingMs(0L, null, null, null, null))

        assertEquals(
            13 * min,
            TimerFreeze.remainingMs(0L, 12 * min, null, null, null, shadeExactMs = 13 * min)
        )

        assertEquals(
            13 * min,
            TimerFreeze.remainingMs(0L, 12 * min, null, null, null, shownMs = 31 * min, previousShownMs = 30 * min)
        )

        assertEquals(
            12 * min,
            TimerFreeze.remainingMs(0L, 12 * min, null, null, null, shownMs = 30 * min + 1_000L, previousShownMs = 30 * min)
        )
        assertEquals(
            12 * min,
            TimerFreeze.remainingMs(0L, 12 * min, null, null, null, shownMs = 29 * min, previousShownMs = 30 * min)
        )

        assertEquals(
            12 * min,
            TimerFreeze.remainingMs(0L, 12 * min, null, null, null, shownMs = 31 * min, previousShownMs = null)
        )
    }

    @Test
    fun exactChronometerWins() {
        assertEquals(
            12 * min + 250L,
            TimerFreeze.remainingMs(
                nowMs = 1_000L, rememberedFrozenMs = 11 * min, runningTargetMs = 1_000L + 10 * min,
                keptFrozenMs = 9 * min, textRemainingMs = 30 * min, shadeShownMs = 12 * min,
                shadeExactMs = 12 * min + 250L
            )
        )
    }
}

class TimerPausedReentryTest {
    private fun action(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())

    @Test
    fun pauseReentry() {
        // paused inside the clock app: the island never saw it running at the pause, so the old running deadline is stale
        val firstRemaining = TimerFreeze.remainingMs(
            nowMs = 1_000_000L, rememberedFrozenMs = null, runningTargetMs = 1_240_000L,
            keptFrozenMs = null, textRemainingMs = 300_000L, shadeShownMs = 95_000L
        )
        assertEquals(95_000L, firstRemaining)

        val jitterRemaining = TimerFreeze.remainingMs(
            nowMs = 1_000_000L, rememberedFrozenMs = 95_000L, runningTargetMs = null,
            keptFrozenMs = null, textRemainingMs = null, shadeShownMs = 94_000L
        )
        assertEquals(95_000L, jitterRemaining)

        val newPauseRemaining = TimerFreeze.remainingMs(
            nowMs = 1_000_000L, rememberedFrozenMs = 95_000L, runningTargetMs = null,
            keptFrozenMs = null, textRemainingMs = null, shadeShownMs = 40_000L
        )
        assertEquals(40_000L, newPauseRemaining)

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
}

class TransferDetectorTest {

    private fun verdict(input: TransferInput) = TransferDetector.evaluate(input)

    @Test
    fun transferEvidence() {
        val browser = verdict(
            TransferInput(
                packageName = "com.android.chrome", title = "video.bin", text = "45% · 3 min left",
                progress = 45, progressMax = 100, isOngoing = true
            )
        )
        assertTrue(browser.isTransfer)
        assertEquals(45, browser.percent)
        assertEquals("3 min left", browser.eta)
        assertEquals(TransferState.ACTIVE, browser.state)

        val android16 = verdict(
            TransferInput(
                packageName = "com.x", title = "Uploading photos", text = "12 of 40 photos",
                isProgressStyle = true, isOngoing = true, requestedPromoted = true
            )
        )
        assertTrue(android16.isTransfer)
        assertEquals(30, android16.percent)

        val fraction = verdict(TransferInput(packageName = "com.x", title = "Downloading", text = "12.5 of 50 MB", isOngoing = true))
        assertTrue(fraction.isTransfer)
        assertEquals(25, fraction.percent)

        assertTrue(verdict(TransferInput(packageName = "com.dropbox.android", title = "Working", progress = 3, progressMax = 10)).isTransfer)

        val base = TransferInput(packageName = "com.android.chrome", title = "video.bin", progress = 40, progressMax = 100)
        assertEquals(TransferState.PAUSED, verdict(base.copy(text = "Paused", actionLabels = listOf("Resume", "Cancel"))).state)
        assertEquals(TransferState.PAUSED, verdict(base.copy(text = "40%", actionLabels = listOf("Resume", "Cancel"))).state)
        assertEquals(TransferState.ACTIVE, verdict(base.copy(text = "40%", actionLabels = listOf("Pause", "Cancel"))).state)
        assertEquals(TransferState.FAILED, verdict(base.copy(text = "Download failed")).state)
        assertEquals(TransferState.WAITING, verdict(base.copy(text = "Waiting for network")).state)
        assertEquals(TransferState.ACTIVE, verdict(base.copy(text = "3 done, 0 failed")).state)
    }

    @Test
    fun detectsTransfersFromProgressAndFileHints() {
        val cases = listOf(
            TransferInput(category = "progress") to true,
            TransferInput(packageName = "com.x", title = "report.pdf", progress = 10, progressMax = 100) to true,
            TransferInput(packageName = "com.x", title = "Workout", progress = 10, progressMax = 100) to false,
            TransferInput(packageName = "com.spotify.music", title = "Downloading 3 songs", isOngoing = true) to true
        )
        for ((input, expected) in cases) {
            assertEquals("$input", expected, verdict(input).isTransfer)
        }

        val meter = TransferInput(packageName = "com.example.netspeed", title = "↓ 2.1 MB/s ↑ 0.3 MB/s", isOngoing = true)
        assertFalse(verdict(meter).isTransfer)

        val chat = TransferInput(category = "msg", title = "Sam", text = "I'm at 50% of the download", isConversational = true)
        assertFalse(verdict(chat).isTransfer)
    }

    @Test
    fun transferMetadata() {
        assertFalse(verdict(TransferInput(isMedia = true, title = "Song download file", isOngoing = true)).isTransfer)
        assertFalse(verdict(TransferInput(category = "progress", isGroupSummary = true)).isTransfer)
        assertFalse(verdict(TransferInput(category = "progress", isMessageSync = true)).isTransfer)

        val v = verdict(TransferInput(packageName = "com.x", title = "Downloading file.zip", text = "62%"))
        assertTrue(v.isTransfer)
        assertEquals(62, v.percent)

        val noBar = TransferInput(packageName = "com.x", channelId = "file_downloads", title = "Working", isOngoing = true)
        assertFalse(verdict(noBar).isTransfer)
        assertTrue(verdict(noBar.copy(indeterminate = true)).isTransfer)

        val inputs = listOf(
            TransferInput(packageName = "com.x", title = "Descargando archivo.zip", progress = 5, progressMax = 10),
            TransferInput(packageName = "com.x", title = "正在下载 文件", indeterminate = true),
            TransferInput(packageName = "com.x", title = "Datei wird hochgeladen", indeterminate = true),
            TransferInput(packageName = "com.whatsapp", title = "Backup in progress", text = "Backing up chats", indeterminate = true),
            TransferInput(packageName = "com.whatsapp", title = "Backup in progress", progress = 20, progressMax = 100)
        )
        for (input in inputs) {
            assertTrue("$input", verdict(input).isTransfer)
        }
    }

    @Test
    fun textAnalyzerReadsSpeedPercentAndEta() {
        val f = TransferTextAnalyzer.analyze("Downloading 45% • 1.2 MB/s • 5 min left")
        assertEquals(45, f.percent)
        assertEquals("1.2 MB/s", f.speed)
        assertEquals("5 min left", f.eta)
        assertEquals("800 kbps", TransferTextAnalyzer.analyze("up 800 kbps").speed)
        assertNull(TransferTextAnalyzer.analyze("1000%").percent)
    }

    @Test
    fun classifierLanguagesCloudSync() {
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.x", "Descargando archivo.zip"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.x", "Subiendo foto"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.x", "Datei wird hochgeladen"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.dropbox.android", "Working"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify("com.google.android.apps.photos", "Backing up 3 of 120"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify("com.x", "Syncing files"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("org.proninyaroslav.libretorrent", "Seeding ubuntu.iso"))
        assertTrue(TransferClassifier.isKnownTransferApp("com.delphicoder.flud"))
        assertTrue(TransferClassifier.isCloudApp("com.nextcloud.client"))
    }
}

class TransferClassifierTest {
    private val recorder = "com.google.android.apps.recorder"

    @Test
    fun recorderBackupQualifiesAsATransfer() {
        assertTrue(TransferClassifier.isRecorderTransfer(recorder, "Recorder Backing up recordings", false))
        assertTrue(TransferClassifier.isRecorderTransfer(recorder, "Recorder Uploading to your account", false))
        assertTrue(TransferClassifier.isRecorderTransfer(recorder, "Recorder Syncing", true))
        assertFalse(TransferClassifier.isRecorderTransfer(recorder, "Recorder Recording 00:12", false))
        assertFalse(TransferClassifier.isRecorderTransfer(recorder, "Recorder Transcribing", true))
        assertFalse(TransferClassifier.isRecorderTransfer("com.whatsapp", "Backing up chats", true))
        assertFalse(TransferClassifier.isRecorderTransfer("com.sec.android.app.screenrecorder", "Uploading", true))
    }

    @Test
    fun transferKinds() {
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify(recorder, "Recorder Backing up recordings"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify(recorder, "Recorder 40%"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify("com.whatsapp", "Backing up chats and media"))
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.whatsapp", "Restoring backup"))

        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.android.chrome", "video.bin 12% 3 min left"))
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.deniscerri.ytdl", "Some video Downloading"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.google.android.apps.docs", "Uploading report.pdf"))
        val cases = listOf(
            "Copying 3 files" to TransferKind.COPY_MOVE,
            "Working" to TransferKind.COPY_MOVE,
            "Deleting 12 files" to TransferKind.DELETE
        )
        for ((line, expected) in cases) {
            assertEquals(line, expected, TransferClassifier.classify("com.google.android.documentsui", line))
        }
        assertEquals(TransferKind.INSTALL, TransferClassifier.classify("com.android.vending", "Downloading Maps"))
        assertEquals(TransferKind.INSTALL, TransferClassifier.classify("com.some.app", "Installing update"))
    }


    @Test
    fun knownTransferAppsAreRecognised() {
        assertTrue(TransferClassifier.isKnownTransferApp("com.deniscerri.ytdl"))
        assertTrue(TransferClassifier.isKnownTransferApp("com.brave.browser"))
        assertTrue(TransferClassifier.isKnownTransferApp("org.mozilla.firefox"))
        assertTrue(TransferClassifier.isKnownTransferApp("com.google.android.documentsui"))
        assertFalse(TransferClassifier.isKnownTransferApp("com.whatsapp"))
    }
}

class TransferExpiryTest {
    private fun card(
        key: String,
        mode: IslandMode = IslandMode.DownloadUpload,
        postedMs: Long,
        estimated: Boolean = false
    ) = IslandNotification(
        key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = mode, lastPostedMs = postedMs, isEstimatedTransfer = estimated
    )

    private val now = 10_000_000L
    private val min = 60_000L

    @Test
    fun transferExpiration() {
        assertEquals(listOf("a"), TransferExpiry.keysToExpire(listOf(card("a", postedMs = now - 20 * min)), now, 15))

        assertEquals(emptyList<String>(), TransferExpiry.keysToExpire(listOf(card("a", postedMs = now - 5 * min)), now, 15))

        assertEquals(emptyList<String>(), TransferExpiry.keysToExpire(listOf(card("a", postedMs = 1L)), now, 0))

        val cards = listOf(
            card("n", mode = IslandMode.Notification, postedMs = 1L),
            card("e", postedMs = 1L, estimated = true),
            card("u", postedMs = 0L)
        )
        assertEquals(emptyList<String>(), TransferExpiry.keysToExpire(cards, now, 15))
    }
}

class UsageForegroundTrackerTest {
    private val ignored: (String) -> Boolean = { it == "me" || it == "ime" }

    private fun UsageForegroundTracker.resume(pkg: String, t: Long) = onEvent(UsageForegroundTracker.RESUMED, pkg, t, ignored)
    private fun UsageForegroundTracker.pause(pkg: String, t: Long) = onEvent(UsageForegroundTracker.PAUSED, pkg, t, ignored)

    @Test fun resumedAppIsInFrontUntilItPauses() {
        val t = UsageForegroundTracker()
        t.resume("chatgpt", 1)
        assertEquals("chatgpt", t.current)
        t.pause("chatgpt", 2)
        assertNull(t.current)
    }

    @Test fun switchingAppsFollowsTheNewestResume() {
        val t = UsageForegroundTracker()
        t.resume("a", 1)
        t.pause("a", 2)
        t.resume("b", 2)
        assertEquals("b", t.current)
    }

    @Test fun latePauseOldAppKeepsNew() {
        val t = UsageForegroundTracker()
        t.resume("a", 1)
        t.resume("b", 2)
        t.pause("a", 3)
        assertEquals("b", t.current)
    }

    @Test fun stoppedEndsTheTurnToo() {
        val t = UsageForegroundTracker()
        t.resume("a", 1)
        t.onEvent(UsageForegroundTracker.STOPPED, "a", 2, ignored)
        assertNull(t.current)
    }

    @Test fun ignoredPackagesAdvanceClock() {
        val t = UsageForegroundTracker()
        t.resume("chatgpt", 1)
        t.resume("me", 5)
        t.resume("ime", 6)
        assertEquals("chatgpt", t.current)
        assertEquals(6L, t.lastEventTime)
    }

    @Test fun missingPackageIsSkipped() {
        val t = UsageForegroundTracker()
        t.onEvent(UsageForegroundTracker.RESUMED, null, 1, ignored)
        t.onEvent(UsageForegroundTracker.RESUMED, "", 2, ignored)
        assertNull(t.current)
    }
}
