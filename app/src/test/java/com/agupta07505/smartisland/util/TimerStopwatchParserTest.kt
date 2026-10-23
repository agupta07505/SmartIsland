/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStopwatchParserTest {

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

    @Test
    fun testGoogleClockTimerDetection() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "04:59"

        val pause = mockk<Notification.Action>()
        pause.title = "Pause"
        val reset = mockk<Notification.Action>()
        reset.title = "Reset"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = arrayOf(pause, reset)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun testXiaomiClockTimerDetection() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "00:04:55"
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns "Timer"

        val pause = mockk<Notification.Action>()
        pause.title = "Pause"
        val cancel = mockk<Notification.Action>()
        cancel.title = "Cancel"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = arrayOf(pause, cancel)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun testSamsungClockTimerDetection() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Tea Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "02:15"

        val cancel = mockk<Notification.Action>()
        cancel.title = "Cancel"
        val pause = mockk<Notification.Action>()
        pause.title = "Pause"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = arrayOf(cancel, pause)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.sec.android.app.clockpackage"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun testStopwatchDetectionWithLapAction() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Stopwatch"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "00:14.28"

        val lap = mockk<Notification.Action>()
        lap.title = "Lap"
        val pause = mockk<Notification.Action>()
        pause.title = "Pause"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = arrayOf(lap, pause)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isStopwatch(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
    }

    @Test
    fun testStopwatchDetectionWithChronometer() {
        val extras = createBaseExtras()
        every { extras.getBoolean("android.showChronometer", false) } returns true
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "00:14.28"

        val pause = mockk<Notification.Action>()
        pause.title = "Pause"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = arrayOf(pause)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isStopwatch(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
    }

    @Test
    fun testTimerRemainingSecondsParsing() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "05:30"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val remaining = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
        assertNotNull(remaining)
        assertEquals(330L, remaining)
    }

    @Test
    fun testTimerSingleDigitParsing() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "0:05"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val remaining = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
        assertNotNull(remaining)
        assertEquals(5L, remaining)
    }

    @Test
    fun testTimerSubTextParsing() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns "02:45"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val remaining = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
        assertNotNull(remaining)
        assertEquals(165L, remaining)
    }

    @Test
    fun testStopwatchElapsedSecondsParsing() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Stopwatch"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "00:14.28"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val elapsed = TimerStopwatchParser.parseStopwatchElapsedSeconds(notification)
        assertNotNull(elapsed)
        assertEquals(14L, elapsed)
    }

    @Test
    fun testTimerFinishedDetection() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Time's up"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = 0
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        assertTrue(TimerStopwatchParser.isTimerFinished(notification))
    }

    @Test
    fun testFormatTime() {
        assertEquals("05:30", TimerStopwatchParser.formatTime(330L))
        assertEquals("00:45", TimerStopwatchParser.formatTime(45L))
        assertEquals("1:02:15", TimerStopwatchParser.formatTime(3735L))
    }

    @Test
    fun testAlarmNotificationNotDetectedAsTimerOrStopwatch() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alarm"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "07:00 AM"

        val snooze = mockk<Notification.Action>()
        snooze.title = "Snooze"
        val dismiss = mockk<Notification.Action>()
        dismiss.title = "Dismiss"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = arrayOf(snooze, dismiss)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = Notification.CATEGORY_ALARM

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isAlarm(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun testUpcomingAlarmNotDetectedAsTimerOrStopwatch() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Upcoming alarm"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "08:30 AM"

        val dismiss = mockk<Notification.Action>()
        dismiss.title = "Dismiss now"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = 0
        notification.actions = arrayOf(dismiss)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = Notification.CATEGORY_ALARM

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isAlarm(sbn))
        assertFalse(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun testPausedTimerDetectedAsTimerNotStopwatch() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "04:52"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Paused"

        val resume = mockk<Notification.Action>()
        resume.title = "Resume"
        val reset = mockk<Notification.Action>()
        reset.title = "Reset"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = 0
        notification.actions = arrayOf(resume, reset)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        assertTrue(TimerStopwatchParser.isTimer(sbn))
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
        assertTrue(TimerStopwatchParser.isTimerPaused(notification))
        assertFalse(TimerStopwatchParser.isTimerFinished(notification))
    }

    @Test
    fun testTimerFinishedDoesNotMatchPlusOneMinute() {
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Paused"

        val addOne = mockk<Notification.Action>()
        addOne.title = "+1:00"
        val resume = mockk<Notification.Action>()
        resume.title = "Resume"

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = 0
        notification.actions = arrayOf(addOne, resume)
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        assertFalse(TimerStopwatchParser.isTimerFinished(notification))
    }

    @Test
    fun pausedTimerReadsRemainingTimeNotTheTitleLength() {
        val card = com.agupta07505.smartisland.model.IslandNotification(
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
    fun unitStyleTitleStillWorksWhenThereIsNoClockText() {
        val card = com.agupta07505.smartisland.model.IslandNotification(
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
        val extras = createBaseExtras()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Working on it"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns text

        val notification = mockk<Notification>()
        notification.extras = extras
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null
        notification.`when` = 0L
        notification.tickerText = null
        notification.category = null

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns pkg
        every { sbn.notification } returns notification
        return sbn
    }

    @Test
    fun otherAppsTextMentioningStopwatchIsNotAStopwatch() {
        val sbn = chatLikeNotification("Next I'm matching the stopwatch card and the elapsed time", "com.example.assistant")
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }

    @Test
    fun lapInsideAnotherWordDoesNotCount() {
        val sbn = chatLikeNotification("Checking the overlap and the collapse animation", "com.example.assistant")
        assertFalse(TimerStopwatchParser.isStopwatch(sbn))
    }
}
