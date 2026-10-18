package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRecoveryTest {

    @Test
    fun anchorsRoundTripThroughTheirStoredForm() {
        val anchor = TimerAnchor(TimerKind.Timer, 1_716_382_920_000L)
        assertEquals("T:1716382920000", anchor.encode())
        assertEquals(anchor, TimerAnchor.decode(anchor.encode()))
        assertEquals(TimerAnchor(TimerKind.Stopwatch, 5L), TimerAnchor.decode("S:5"))
        assertNull(TimerAnchor.decode(null))
        assertNull(TimerAnchor.decode("X:5"))
        assertNull(TimerAnchor.decode("T:soon"))
        assertNull(TimerAnchor.decode("T"))
    }

    @Test
    fun aTimerThatRanOutWhileTheProcessWasDeadIsPromotedToCritical() {
        val stored = TimerAnchor(TimerKind.Timer, 10_000L)
        val result = TimerRecovery.recover(stored, TimerKind.Timer, freshEpochMs = 99_000L, nowMs = 20_000L, isRestore = true)
        // The stored absolute end wins over the stale re-parse, and it has passed.
        assertEquals(10_000L, result.epochMs)
        assertTrue(result.promoteToCritical)
    }

    @Test
    fun aTimerStillRunningAfterARestartKeepsItsStoredEnd() {
        val stored = TimerAnchor(TimerKind.Timer, 50_000L)
        val result = TimerRecovery.recover(stored, TimerKind.Timer, freshEpochMs = 60_000L, nowMs = 20_000L, isRestore = true)
        assertEquals(50_000L, result.epochMs)
        assertFalse(result.promoteToCritical)
    }

    @Test
    fun outsideARestoreTheFreshAnchorIsUsedAndNothingIsPromoted() {
        val stored = TimerAnchor(TimerKind.Timer, 10_000L)
        val result = TimerRecovery.recover(stored, TimerKind.Timer, freshEpochMs = 15_000L, nowMs = 20_000L, isRestore = false)
        assertEquals(15_000L, result.epochMs)
        assertFalse(result.promoteToCritical)
    }

    @Test
    fun aPausedTimerOrAStopwatchIsNeverPromoted() {
        val stored = TimerAnchor(TimerKind.Timer, 10_000L)
        val paused = TimerRecovery.recover(stored, TimerKind.Timer, 25_000L, nowMs = 20_000L, isRestore = true, paused = true)
        assertEquals(25_000L, paused.epochMs)
        assertFalse(paused.promoteToCritical)
        val watch = TimerRecovery.recover(TimerAnchor(TimerKind.Stopwatch, 1_000L), TimerKind.Stopwatch, 9_000L, 20_000L, isRestore = true)
        assertEquals(1_000L, watch.epochMs)
        assertFalse(watch.promoteToCritical)
    }

    @Test
    fun anAnchorOfTheWrongKindIsIgnored() {
        val result = TimerRecovery.recover(TimerAnchor(TimerKind.Stopwatch, 1_000L), TimerKind.Timer, 30_000L, 20_000L, isRestore = true)
        assertEquals(30_000L, result.epochMs)
        assertFalse(result.promoteToCritical)
    }

    @Test
    fun theSameAnchorIsNotRewrittenOnEveryTick() {
        val anchor = TimerAnchor(TimerKind.Timer, 10_000L)
        assertTrue(TimerRecovery.shouldPersist(null, anchor))
        assertFalse(TimerRecovery.shouldPersist(anchor, anchor.copy(epochMs = 10_400L)))
        assertTrue(TimerRecovery.shouldPersist(anchor, anchor.copy(epochMs = 11_000L)))
        assertTrue(TimerRecovery.shouldPersist(anchor, TimerAnchor(TimerKind.Stopwatch, 10_000L)))
    }

    @Test
    fun aPromotedTimerIsClassifiedCritical() {
        // The listener promotes by posting the timer with the alarm category; the classifier does the rest.
        val timer = IslandNotification(
            key = "t", packageName = "com.clock", appName = "Clock", title = "Timer", text = "",
            timeMillis = 10_000L, mode = IslandMode.Timer, category = "alarm"
        )
        assertEquals(AlertTier.Critical, AlertClassifier.classify(timer))
    }

    @Test
    fun onlyListenerAlertsAreFlushedWhenNotificationAccessGoes() {
        fun n(key: String) = IslandNotification(key = key, packageName = "p", appName = "p", title = "", text = "", timeMillis = 0L)
        val list = listOf(n("0|com.whatsapp|1|null|10"), n("system_battery"), n("system_flashlight"), n("demo_music"), n("dbg_x"))
        assertEquals(listOf("0|com.whatsapp|1|null|10", "dbg_x"), AlertSource.listenerSourcedKeys(list))
    }
}
