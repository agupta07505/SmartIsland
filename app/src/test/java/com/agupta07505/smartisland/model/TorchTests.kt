/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalTorchDetectorTest {
    /** Feeds [on]/[off] pairs in milliseconds and returns what the last flip made of them */
    private fun feed(detector: ExternalTorchDetector, steps: List<Pair<Long, Long>>): ExternalTorchPattern? {
        var t = 0L
        var result: ExternalTorchPattern? = null
        for ((on, off) in steps) {
            result = detector.onFlip(true, t)
            t += on
            result = detector.onFlip(false, t)
            t += off
        }
        return result
    }

    @Test
    fun externalFlashPatterns() {
        val steadySteps = List(8) { 100L to 100L }
        assertEquals(ExternalTorchPattern.Strobe(5), feed(ExternalTorchDetector(), steadySteps))

        assertNull(feed(ExternalTorchDetector(), List(4) { 100L to 100L }))

        val irregularSteps = listOf(100L to 100L, 100L to 400L, 100L to 50L, 100L to 300L, 100L to 100L, 100L to 500L, 100L to 80L)
        assertNull(feed(ExternalTorchDetector(), irregularSteps))

        val sosDot = 200L to 200L
        val dash = 600L to 200L
        val sosSteps = listOf(sosDot, sosDot, sosDot, dash, dash, dash, sosDot, sosDot, sosDot)
        assertEquals(ExternalTorchPattern.Sos, feed(ExternalTorchDetector(), sosSteps))

        val openingDot = 200L to 200L
        assertNull(feed(ExternalTorchDetector(), listOf(openingDot, openingDot, openingDot)))
    }

    @Test
    fun resetForgetsEverything() {
        val detector = ExternalTorchDetector()
        feed(detector, List(8) { 100L to 100L })
        detector.reset()
        assertNull(detector.onFlip(true, 10_000L))
    }
}

class TorchBeamTest {
    @Test
    fun beamReachStaysInSteps() {
        assertEquals(0.37f, TorchBeam.reach(1, 10), 0.001f)
        assertEquals(0.65f, TorchBeam.reach(5, 10), 0.001f)
        assertEquals(1f, TorchBeam.reach(10, 10), 0.001f)
        assertEquals(TorchBeam.reach(10, 10), TorchBeam.reach(40, 10), 0f)
        assertEquals(TorchBeam.reach(1, 10), TorchBeam.reach(-2, 10), 0f)
    }

    @Test
    fun beamOpacity() {
        assertEquals(1f, TorchBeam.reach(1, 1), 0f)
        assertEquals(0.9f, TorchBeam.alpha(1, 1), 0f)

        assertEquals(0.415f, TorchBeam.alpha(1, 10), 0.001f)
        assertEquals(1f, TorchBeam.alpha(10, 10), 0.001f)
    }


    @Test
    fun fingerHeightPicksBoundedLevel() {
        assertEquals(10, TorchBeam.levelAt(214f, 214f, 10))
        assertEquals(5, TorchBeam.levelAt(139.1f, 214f, 10))
        assertEquals(1, TorchBeam.levelAt(0f, 214f, 10))
        assertEquals(10, TorchBeam.levelAt(400f, 214f, 10))
        assertEquals(1, TorchBeam.levelAt(-20f, 214f, 10))
        assertEquals(1, TorchBeam.levelAt(100f, 214f, 1))
        assertEquals(1, TorchBeam.levelAt(100f, 0f, 10))
    }

    @Test
    fun ticksUnderTheTopIconAreLeftOut() {
        assertFalse(TorchBeam.tickClearsIcon(-1f, 23f))
        assertFalse(TorchBeam.tickClearsIcon(15f, 23f))
        assertTrue(TorchBeam.tickClearsIcon(23f, 23f))
        assertTrue(TorchBeam.tickClearsIcon(60f, 23f))
    }
}

class TorchCallSwapTest {
    private fun card(key: String, mode: IslandMode, actions: List<String> = emptyList()) = IslandNotification(
        key = key,
        packageName = "app.$key",
        appName = key,
        title = key,
        text = "",
        timeMillis = 0L,
        mode = mode,
        actionIntents = actions.map { IslandNotificationAction(it, null) }
    )

    private val torch = card("torch", IslandMode.Flashlight)
    private val ringing = card("call", IslandMode.IncomingCall, listOf("Answer", "Decline"))
    private val connected = card("live", IslandMode.IncomingCall, listOf("Hang up"))
    private val note = card("note", IslandMode.Notification)

    @Test
    fun secondaryCallSelection() {
        assertEquals(2, cutoutCardCount(listOf(ringing, note, torch), expanded = true, currentIndex = 2))
        assertEquals(0, secondaryCardIndex(listOf(ringing, note, torch), 2, expanded = true))

        assertEquals(0, cutoutCardCount(listOf(note, torch), expanded = true, currentIndex = 1))
        assertEquals(0, cutoutCardCount(listOf(connected, torch), expanded = true, currentIndex = 1))
        assertEquals(-1, secondaryCardIndex(listOf(note, torch), 1, expanded = true))
        assertEquals(-1, secondaryCardIndex(listOf(connected, torch), 1, expanded = true))

        val collapsedCards = listOf(ringing, torch)
        assertEquals(2, cutoutCardCount(collapsedCards, expanded = false, currentIndex = 1))
        assertEquals(0, secondaryCardIndex(collapsedCards, 1, expanded = false))

        val expandedCards = listOf(ringing, note)
        assertEquals(2, cutoutCardCount(expandedCards, expanded = true, currentIndex = 1))
        assertEquals(0, secondaryCardIndex(expandedCards, 1, expanded = true))
    }

    @Test
    fun onlyRingingCallPulsesOverTorch() {
        assertTrue(TorchCard.mayPulse(true, ringing))
        assertFalse(TorchCard.mayPulse(true, connected))
        assertFalse(TorchCard.mayPulse(true, note))
        listOf(ringing, connected, note, torch).forEach { assertTrue(TorchCard.mayPulse(false, it)) }
    }

    @Test
    fun stopsPatternOnCardSwitch() {
        assertTrue(TorchCard.settlesPatternOnSwap(true, true, IslandMode.IncomingCall, true))
        assertFalse(TorchCard.settlesPatternOnSwap(true, true, IslandMode.Flashlight, true))
        assertFalse(TorchCard.settlesPatternOnSwap(true, false, IslandMode.IncomingCall, true))
        assertFalse(TorchCard.settlesPatternOnSwap(false, true, IslandMode.IncomingCall, true))
        assertFalse(TorchCard.settlesPatternOnSwap(true, true, IslandMode.Notification, false))
    }

    @Test
    fun closingTheTorchCardWhileACallRingsGivesUpThePattern() {
        assertTrue(TorchCard.settlesPatternOnCollapse(true, false, true, true))
        assertFalse(TorchCard.settlesPatternOnCollapse(true, false, false, true))
        assertFalse(TorchCard.settlesPatternOnCollapse(true, true, true, true))
        assertFalse(TorchCard.settlesPatternOnCollapse(false, false, true, true))
        assertFalse(TorchCard.settlesPatternOnCollapse(true, false, true, false))
    }

    private fun stepped() = TorchCardState.initial(maxLevel = 10, level = 6, on = false, acknowledged = true)

    @Test
    fun torchSwapOutput() {
        listOf(stepped().sosTap(), stepped().strobeTap()).forEach { running ->
            val left = running.leftForAnotherCard()
            assertNull(left.pattern)
            assertTrue(left.on)
            assertEquals(TorchView.Main, left.view)
            assertEquals(TorchOutput.Steady(6), left.output)
        }

        val steadyOn = stepped().powerTap()
        assertEquals(steadyOn, steadyOn.leftForAnotherCard())
        assertEquals(stepped(), stepped().leftForAnotherCard())
    }

    @Test
    fun leavingTheWarningDropsItAndLeavesTheTorchAlone() {
        val gate = TorchCardState.initial(10, 6, on = false, acknowledged = false).strobeTap()
        assertTrue(gate.gateShown)
        val left = gate.leftForAnotherCard()
        assertFalse(left.gateShown)
        assertEquals(TorchView.Main, left.view)
        assertEquals(TorchOutput.Off, left.output)
    }
}

class TorchCardEventTest {
    @Test
    fun darkTorchKeepsEventWhileOpen() {
        assertFalse(TorchCard.endsEventWhenDark(cardOpen = true))
        assertTrue(TorchCard.endsEventWhenDark(cardOpen = false))
    }

    @Test
    fun torchEventEndsOnlyAfterAllTorchActivityStops() {
        assertTrue(TorchCard.eventDone(cardOpen = false, torchActive = false, warningShown = false))
        assertFalse(TorchCard.eventDone(cardOpen = true, torchActive = false, warningShown = false))
        assertFalse(TorchCard.eventDone(cardOpen = false, torchActive = true, warningShown = false))
        assertFalse(TorchCard.eventDone(cardOpen = false, torchActive = false, warningShown = true))
    }

}

class TorchCardSizeTest {
    @Test
    fun cardHeightScalesBetweenLimits() {
        assertEquals(340f, TorchCardSize.heightDp(1000f), 0.001f)
        assertEquals(TorchCardSize.SHORT_SCREEN_MIN_HEIGHT_DP, TorchCardSize.heightDp(400f), 0.001f)
        assertEquals(TorchCardSize.MIN_HEIGHT_DP, TorchCardSize.heightDp(500f), 0.001f)
        assertEquals(TorchCardSize.MAX_HEIGHT_DP, TorchCardSize.heightDp(2000f), 0.001f)
    }


    @Test
    fun widthIsInsideTheScreen() {
        assertTrue(TorchCardSize.WIDTH_FRACTION > 0f && TorchCardSize.WIDTH_FRACTION < 0.95f)
    }

    @Test
    fun estimateIsInsideTheRange() {
        assertTrue(TorchCardSize.ESTIMATE_HEIGHT_DP in TorchCardSize.MIN_HEIGHT_DP..TorchCardSize.MAX_HEIGHT_DP)
    }
}

class TorchCardStateTest {
    private fun stepped(acknowledged: Boolean = true) =
        TorchCardState.initial(maxLevel = 10, level = 6, on = false, acknowledged = acknowledged)

    private fun onOff() = TorchCardState.initial(maxLevel = 1, level = 1, on = false, acknowledged = true)

    @Test
    fun torchButtonActions() {
        val on = stepped().powerTap()
        assertTrue(on.on)
        assertEquals(TorchOutput.Steady(6), on.output)
        val off = on.powerTap()
        assertFalse(off.on)
        assertEquals(TorchOutput.Off, off.output)

        val strobeGate = stepped(acknowledged = false).strobeTap()
        assertEquals(TorchView.Strobe, strobeGate.view)
        assertTrue(strobeGate.gateShown)
        assertNull(strobeGate.pattern)
        assertEquals(TorchOutput.Off, strobeGate.output)
        assertEquals(TorchChip.StrobePaused, strobeGate.chip)

        val warningGate = stepped(acknowledged = false).copy(view = TorchView.Strobe).powerTap()
        assertTrue(warningGate.gateShown)
    }

    @Test
    fun theLightIconStopsARunningSosAndTurnsTheTorchOff() {
        val stopped = stepped().sosTap().powerTap()
        assertNull(stopped.pattern)
        assertFalse(stopped.on)
        assertEquals(TorchOutput.Off, stopped.output)
    }

    @Test
    fun sosReplacesASteadyLightAndASecondTapStopsIt() {
        val sos = stepped().powerTap().sosTap()
        assertEquals(TorchPattern.Sos, sos.pattern)
        assertFalse(sos.on)
        assertEquals(TorchOutput.Sos, sos.output)
        assertEquals(TorchOutput.Off, sos.sosTap().output)
    }

    @Test
    fun sosFromStrobeGoesBack() {
        val sos = stepped().strobeTap().sosTap()
        assertEquals(TorchView.Main, sos.view)
        assertEquals(TorchPattern.Sos, sos.pattern)
    }

    @Test
    fun strobeWarningFlow() {
        val acceptedStrobe = stepped(acknowledged = false).strobeTap().gateAccept()
        assertTrue(acceptedStrobe.acknowledged)
        assertFalse(acceptedStrobe.gateShown)
        assertEquals(TorchOutput.Strobe(TorchCardState.DEFAULT_HZ), acceptedStrobe.output)

        val cancelledBack = stepped(acknowledged = false).strobeTap().gateCancel()
        assertEquals(TorchView.Main, cancelledBack.view)
        assertFalse(cancelledBack.gateShown)
        assertNull(cancelledBack.pattern)

        val acknowledgedStrobe = stepped().strobeTap()
        assertEquals(TorchPattern.Strobe, acknowledgedStrobe.pattern)
        assertFalse(acknowledgedStrobe.gateShown)

        val paused = stepped().strobeTap().powerTap()
        assertNull(paused.pattern)
        assertEquals(TorchView.Strobe, paused.view)
        val running = paused.powerTap()
        assertEquals(TorchPattern.Strobe, running.pattern)

        val stoppedBack = stepped().strobeTap().back()
        assertEquals(TorchView.Main, stoppedBack.view)
        assertNull(stoppedBack.pattern)
        assertFalse(stoppedBack.on)
        assertEquals(TorchOutput.Off, stoppedBack.output)
    }

    @Test
    fun strobeRateStaysOneToThirty() {
        assertEquals(1, stepped().withHz(0).hz)
        assertEquals(30, stepped().withHz(99).hz)
        assertEquals(12, stepped().withHz(12).hz)
    }

    @Test
    fun rateChangeChangesOutput() {
        assertEquals(TorchOutput.Strobe(12), stepped().strobeTap().withHz(12).output)
    }

    @Test
    fun theReliableRangeEndsAtTwentyHertz() {
        assertFalse(stepped().withHz(20).extendedRange)
        assertTrue(stepped().withHz(21).extendedRange)
    }

    @Test
    fun levelStaysWithinPhoneSteps() {
        assertEquals(1, stepped().withLevel(-3).level)
        assertEquals(10, stepped().withLevel(40).level)
        assertEquals(1, onOff().withLevel(7).level)
    }

    @Test
    fun theBeamIsAdjustableOnlyForASteadyLightWithSteps() {
        assertFalse(stepped().canAdjust)
        assertTrue(stepped().powerTap().canAdjust)
        assertFalse(stepped().sosTap().canAdjust)
        assertFalse(onOff().powerTap().canAdjust)
    }

    @Test
    fun patternsAndTheStrobeViewShowTheBeamAtTheHighestStep() {
        assertEquals(6, stepped().powerTap().beamLevel)
        assertEquals(10, stepped().sosTap().beamLevel)
        assertEquals(10, stepped(acknowledged = false).strobeTap().beamLevel)
    }

    @Test
    fun theChipFollowsTheState() {
        assertEquals(TorchChip.Off, stepped().chip)
        assertEquals(TorchChip.OnAtLevel(6, 10), stepped().powerTap().chip)
        assertEquals(TorchChip.On, onOff().powerTap().chip)
        assertEquals(TorchChip.Sos, stepped().sosTap().chip)
        assertEquals(TorchChip.Strobe(3), stepped().strobeTap().chip)
    }

    @Test
    fun theHintFollowsTheState() {
        assertEquals(TorchHint.TapToTurnOn, stepped().hint)
        assertEquals(TorchHint.DragBeam, stepped().powerTap().hint)
        assertEquals(TorchHint.NoBrightnessSteps, onOff().powerTap().hint)
        assertEquals(TorchHint.TapToStop, stepped().sosTap().hint)
    }

    @Test
    fun outsideTorchChangeIsFollowed() {
        assertTrue(stepped().systemTorch(true).on)
        assertFalse(stepped().powerTap().systemTorch(false).on)
    }

    @Test
    fun ignoresTorchFeedbackDuringPattern() {
        val sos = stepped().sosTap()
        assertEquals(sos, sos.systemTorch(true))
        assertEquals(sos, sos.systemTorch(false))
    }

    @Test
    fun diedPatternLeavesTorchOff() {
        val ended = stepped().sosTap().patternEnded()
        assertNull(ended.pattern)
        assertEquals(TorchOutput.Off, ended.output)
    }

    @Test
    fun patternEndWithNoneRunningNoChange() {
        val steady = stepped().powerTap()
        assertEquals(steady, steady.patternEnded())
    }

    @Test
    fun theFirstStateIsReadFromThePhoneAndKeptInRange() {
        val state = TorchCardState.initial(maxLevel = 5, level = 9, on = true, acknowledged = false)
        assertEquals(5, state.level)
        assertTrue(state.on)
        assertEquals(1, TorchCardState.initial(maxLevel = 0, level = 3, on = false, acknowledged = false).maxLevel)
    }

    @Test
    fun cycleIsThousandOverRate() {
        assertEquals(1000, TorchCardState.cycleMs(1))
        assertEquals(333, TorchCardState.cycleMs(3))
        assertEquals(50, TorchCardState.cycleMs(20))
        assertEquals(33, TorchCardState.cycleMs(30))
        assertEquals(33, TorchCardState.cycleMs(99))
    }
}

class TorchCardTest {
    @Test
    fun onlyTheFlashlightModeIsTheTorchCard() {
        assertTrue(TorchCard.owns(IslandMode.Flashlight))
        IslandMode.entries.filter { it != IslandMode.Flashlight }.forEach { assertFalse("$it", TorchCard.owns(it)) }
        assertFalse(TorchCard.owns(null))
    }

    @Test
    fun aPatternWithoutItsEventMustStop() {
        assertTrue(TorchCard.mustStopPattern(patternRunning = true, eventShown = false))
        assertFalse(TorchCard.mustStopPattern(patternRunning = true, eventShown = true))
        assertFalse(TorchCard.mustStopPattern(patternRunning = false, eventShown = false))
        assertFalse(TorchCard.mustStopPattern(patternRunning = false, eventShown = true))
    }

    @Test
    fun theCameraAndARingingCallsFlashAreNotFollowed() {
        assertTrue(TorchCard.followsSystemTorch(cameraBusy = false, callRinging = false))
        assertFalse(TorchCard.followsSystemTorch(cameraBusy = true, callRinging = false))
        assertFalse(TorchCard.followsSystemTorch(cameraBusy = false, callRinging = true))
        assertFalse(TorchCard.followsSystemTorch(cameraBusy = true, callRinging = true))
    }
}

class TorchCardTimeoutTest {
    @Test
    fun torchTimeoutPolicy() {
        assertEquals(TorchTimeout.None, TorchCard.timeout(IslandMode.Flashlight, autoOpened = false, touched = false))
        assertEquals(TorchTimeout.None, TorchCard.timeout(IslandMode.Flashlight, autoOpened = false, touched = true))
        assertEquals(TorchTimeout.Hold, TorchCard.timeout(IslandMode.Flashlight, autoOpened = true, touched = true))
        assertEquals(TorchTimeout.Normal, TorchCard.timeout(IslandMode.Flashlight, autoOpened = true, touched = false))

        IslandMode.entries.filter { it != IslandMode.Flashlight }.forEach { mode ->
            listOf(true, false).forEach { auto ->
                listOf(true, false).forEach { touched ->
                    assertEquals("$mode $auto $touched", TorchTimeout.Normal, TorchCard.timeout(mode, auto, touched))
                }
            }
        }
        assertEquals(TorchTimeout.Normal, TorchCard.timeout(null, autoOpened = false, touched = true))
    }
}

class TorchLockdownTest {
    private fun card(key: String, pkg: String, mode: IslandMode) = IslandNotification(
        key = key,
        packageName = pkg,
        appName = pkg,
        title = key,
        text = "",
        timeMillis = 0L,
        mode = mode
    )

    private val torch = card("system_flashlight", "android", IslandMode.Flashlight)
    private val message = card("m", "app.chat", IslandMode.Notification)
    private val music = card("p", "app.music", IslandMode.Music)

    @Test
    fun expandedTorchMode() {
        assertTrue(TorchCard.open(IslandMode.Flashlight, expanded = true))
        assertFalse(TorchCard.open(IslandMode.Flashlight, expanded = false))
        assertFalse(TorchCard.open(null, expanded = true))

        IslandMode.entries.filter { it != IslandMode.Flashlight }.forEach { mode ->
            assertFalse("$mode", TorchCard.open(mode, expanded = true))
        }
    }

    @Test
    fun torchCutoutCount() {
        assertEquals(0, cutoutCardCount(listOf(torch, message, music), expanded = true, currentIndex = 0))
        assertEquals(3, cutoutCardCount(listOf(torch, message, music), expanded = false, currentIndex = 0))

        val cards = listOf(message, torch, music)
        assertEquals(3, cutoutCardCount(cards, expanded = true, currentIndex = 0))
        assertEquals(3, cutoutCardCount(cards, expanded = true, currentIndex = 2))
        assertEquals(3, cutoutCardCount(cards, expanded = false, currentIndex = 1))
    }

}

class TorchPatternClockTest {
    @Test
    fun torchPatternDutyCycle() {
        val cases = listOf(
            0L to true,
            249L to true,
            250L to false,
            500L to true
        )
        for ((input, expected) in cases) {
            assertEquals("$input", expected, TorchPatternClock.lit(TorchPattern.Strobe, 2, input))
        }

        val onBeats = (0 until 1000 step 1).count { TorchPatternClock.lit(TorchPattern.Strobe, 30, it.toLong()) }
        assertEquals(500.0, onBeats.toDouble(), 20.0)

        assertTrue(TorchPatternClock.lit(TorchPattern.Sos, 3, 0))
        assertFalse(TorchPatternClock.lit(TorchPattern.Sos, 3, 200))
        assertTrue(TorchPatternClock.lit(TorchPattern.Sos, 3, 400))

        val total = 7_400L
        assertFalse(TorchPatternClock.lit(TorchPattern.Sos, 3, total - 100))
        assertTrue(TorchPatternClock.lit(TorchPattern.Sos, 3, total))
    }
}

class TorchStrobeSliderTest {
    @Test
    fun strobeRateScale() {
        assertEquals(TorchCardState.MIN_HZ, TorchStrobeSlider.hzAt(0f))
        assertEquals(TorchCardState.MAX_HZ, TorchStrobeSlider.hzAt(1f))
        assertEquals(TorchCardState.MIN_HZ, TorchStrobeSlider.hzAt(-0.4f))
        assertEquals(TorchCardState.MAX_HZ, TorchStrobeSlider.hzAt(1.7f))
        assertEquals(TorchCardState.RELIABLE_HZ, TorchStrobeSlider.hzAt(TorchStrobeSlider.reliableFraction))

        for (hz in TorchCardState.MIN_HZ..TorchCardState.MAX_HZ) {
            assertEquals(hz, TorchStrobeSlider.hzAt(TorchStrobeSlider.fractionOf(hz)))
        }
    }


}
