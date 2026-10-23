/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    fun shadeFixesCoverStockAndThirdPartyClocksButNotPhoneMakers() {
        assertTrue(TimerStopwatchParser.usesShadeFixes("com.google.android.deskclock"))
        assertTrue(TimerStopwatchParser.usesShadeFixes("com.android.deskclock"))
        assertTrue(TimerStopwatchParser.usesShadeFixes("com.jee.timer"))
        assertTrue(TimerStopwatchParser.usesShadeFixes("com.example.coolstopwatch"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.miui.deskclock"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.xiaomi.somethingclock"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.sec.android.app.clockpackage"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.oneplus.deskclock"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.android.bbkclock"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.huawei.deskclock"))
    }

    @Test
    fun pausedThirdPartyStopwatchStaysAStopwatchToo() {
        val pkg = "com.jee.timer"
        val key = "$pkg#3"
        assertTrue(TimerStopwatchParser.isStopwatch(notification("Pause", "Lap"), pkg, key))
        assertTrue(TimerStopwatchParser.isStopwatch(notification("Resume", "Stop"), pkg, key))
    }

    @Test
    fun pausedStopwatchFreezesAtTheTimeTheShadePrints() {
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
    fun runningStopwatchIgnoresThePrintedTime() {
        assertEquals(5_000L, RecordingClock.frozenReading(paused = false, shownMs = 12_000L, parsedMs = 5_000L))
        assertEquals(null, RecordingClock.frozenReading(paused = false, shownMs = 12_000L, parsedMs = null))
    }

    @Test
    fun aZeroPrintedTimeFallsBackToTheTextReading() {
        assertEquals(7_000L, RecordingClock.frozenReading(paused = true, shownMs = 0L, parsedMs = 7_000L))
        assertEquals(7_000L, RecordingClock.frozenReading(paused = true, shownMs = null, parsedMs = 7_000L))
    }
}
