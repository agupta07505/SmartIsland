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
}
