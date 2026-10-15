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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeActionsTest {
    private fun real(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())
    private fun view(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())

    @Test
    fun runningTimerShowsPauseAndAddMinuteOnly() {
        val actions = ShadeActions.mirror(listOf(real("Pause"), real("+1:00")), emptyList())
        assertEquals(listOf("Pause", "+1:00"), actions.map { it.title })
    }

    @Test
    fun pausedTimerShowsResumeAndStop() {
        val actions = ShadeActions.mirror(listOf(real("Resume"), real("Stop")), emptyList())
        assertEquals(listOf("Resume", "Stop"), actions.map { it.title })
        assertTrue(ShadeActions.offersResume(actions))
    }

    @Test
    fun runningStopwatchShowsPauseAndLapOnly() {
        val actions = ShadeActions.mirror(listOf(real("Pause"), real("Lap")), emptyList())
        assertEquals(listOf("Pause", "Lap"), actions.map { it.title })
        assertFalse(ShadeActions.offersResume(actions))
    }

    @Test
    fun buttonsWithoutAnIntentAreNotShown() {
        val actions = ShadeActions.mirror(
            listOf(real("Pause"), IslandNotificationAction("Stop", null)), emptyList()
        )
        assertEquals(listOf("Pause"), actions.map { it.title })
    }

    @Test
    fun noMoreThanTheShadeLimit() {
        val actions = ShadeActions.mirror(
            listOf(real("Pause"), real("Lap"), real("Stop"), real("Reset")), emptyList()
        )
        assertEquals(ShadeActions.SHADE_ACTION_LIMIT, actions.size)
    }

    @Test
    fun customViewButtonsWinAndKeepTheirOrder() {
        val actions = ShadeActions.mirror(
            standard = emptyList(),
            view = listOf(view("pause button"), view("Add 1 minute"))
        )
        assertEquals(listOf("pause button", "Add 1 minute"), actions.map { it.title })
    }

    @Test
    fun theAppsOwnIntentReplacesTheViewProxy() {
        val own = real("Pause")
        val actions = ShadeActions.mirror(listOf(own), listOf(view("Pause"), view("+1:00")))
        assertEquals(2, actions.size)
        assertSame(own.pendingIntent, actions[0].pendingIntent)
    }

    @Test
    fun clickableViewsThatAreNotButtonsAreDropped() {
        val actions = ShadeActions.mirror(emptyList(), listOf(view("notification content"), view("Pause")))
        assertEquals(listOf("Pause"), actions.map { it.title })
    }

    private val timer = TimerStopwatchParser.ClockKind.Timer
    private val stopwatch = TimerStopwatchParser.ClockKind.Stopwatch

    private fun titles(a: List<IslandNotificationAction>) = a.map { it.title }

    @Test
    fun runningGoogleTimerNeverShowsStop() {
        val a = ShadeActions.mirror(
            emptyList(), listOf(view("Pause"), view("+1:00"), view("Stop")), timer
        )
        assertEquals(listOf("Pause", "+1:00"), titles(a))
    }

    @Test
    fun runningGoogleTimerDropsEnd() {
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("End"), real("+1:00")), emptyList(), timer
        )
        assertEquals(listOf("Pause", "+1:00"), titles(a))
    }

    @Test
    fun runningGoogleTimerTakesAddMinuteFromTheAppWhenTheLayoutMissedIt() {
        val own = real("+1:00")
        val a = ShadeActions.mirror(listOf(real("Pause"), own), listOf(view("Pause")), timer)
        assertEquals(listOf("Pause", "+1:00"), titles(a))
        assertSame(own.pendingIntent, a[1].pendingIntent)
    }

    @Test
    fun pausedGoogleTimerShowsResumeAndStopOnly() {
        val a = ShadeActions.mirror(
            emptyList(), listOf(view("Resume"), view("Lap"), view("Stop")), timer
        )
        assertEquals(listOf("Resume", "Stop"), titles(a))
    }

    @Test
    fun runningGoogleStopwatchHasLapButNoStop() {
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("Lap"), real("Stop")), emptyList(), stopwatch
        )
        assertEquals(listOf("Pause", "Lap"), titles(a))
    }

    @Test
    fun pausedGoogleStopwatchHasNoLap() {
        val a = ShadeActions.mirror(
            listOf(real("Resume"), real("Lap"), real("Stop")), emptyList(), stopwatch
        )
        assertEquals(listOf("Resume", "Stop"), titles(a))
    }

    @Test
    fun stateIsEnforcedBeforeTheShadeLimit() {
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("Stop"), real("Reset"), real("Lap")), emptyList(), stopwatch
        )
        assertEquals(listOf("Pause", "Lap"), titles(a))
    }

    @Test
    fun runningTimerShowsPauseAndAddMinuteOnlyForAnyClock() {
        val a = ShadeActions.mirror(listOf(real("Pause"), real("Stop"), real("+1:00")), emptyList(), timer)
        assertEquals(listOf("Pause", "+1:00"), titles(a))
    }

    @Test
    fun pausedTimerShowsResumeAndResetOnly() {
        val a = ShadeActions.mirror(listOf(real("Resume"), real("Stop"), real("Reset")), emptyList(), timer)
        assertEquals(listOf("Resume", "Reset"), titles(a))
    }

    @Test
    fun pausedStopwatchShowsStartAndResetOnly() {
        val a = ShadeActions.mirror(listOf(real("Start"), real("Lap"), real("Reset")), emptyList(), stopwatch)
        assertEquals(listOf("Start", "Reset"), titles(a))
    }

    @Test
    fun runningStopwatchHasNoReset() {
        val a = ShadeActions.mirror(listOf(real("Pause"), real("Reset"), real("Lap")), emptyList(), stopwatch)
        assertEquals(listOf("Pause", "Lap"), titles(a))
    }

    @Test
    fun aListWithNoStateIsLeftAlone() {
        val a = ShadeActions.mirror(listOf(real("Stop"), real("Lap")), emptyList(), stopwatch)
        assertEquals(listOf("Stop", "Lap"), titles(a))
    }

    @Test
    fun everyClockStateOffersExactlyItsButtons() {
        fun shown(kind: TimerStopwatchParser.ClockKind, vararg visible: String) =
            titles(ShadeActions.mirror(visible.map { real(it) }, emptyList(), kind))
        assertEquals(listOf("Pause", "+1:00"), shown(timer, "Pause", "Stop", "+1:00"))
        assertEquals(listOf("Resume", "Stop"), shown(timer, "Resume", "Stop"))
        assertEquals(listOf("Pause", "Lap"), shown(stopwatch, "Pause", "Lap", "Stop"))
        assertEquals(listOf("Resume", "Reset"), shown(stopwatch, "Resume", "Reset", "Lap"))
    }

    @Test
    fun phoneMakerClocksAreNotShadeFixClocks() {
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.miui.deskclock"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.sec.android.app.clockpackage"))
        assertTrue(TimerStopwatchParser.usesShadeFixes("com.google.android.deskclock"))
    }

    @Test
    fun proxyButtonsAreFlaggedAndTheAppsOwnIntentIsNot() {
        val proxy = IslandNotificationAction("Pause", mockk<PendingIntent>(), viaView = true)
        val own = real("Pause")
        assertTrue(proxy.viaView)
        assertFalse(own.viaView)
        val mirrored = ShadeActions.mirror(listOf(own), listOf(proxy), timer)
        assertFalse(mirrored.single().viaView)
    }

    @Test
    fun pausedStopwatchWithResetDropsStop() {
        val a = ShadeActions.mirror(listOf(real("Resume"), real("Stop"), real("Reset")), emptyList(), stopwatch)
        assertEquals(listOf("Resume", "Reset"), titles(a))
    }

    @Test
    fun pausedStopwatchWithoutResetKeepsStop() {
        val a = ShadeActions.mirror(listOf(real("Resume"), real("Lap"), real("Stop")), emptyList(), stopwatch)
        assertEquals(listOf("Resume", "Stop"), titles(a))
    }
}
