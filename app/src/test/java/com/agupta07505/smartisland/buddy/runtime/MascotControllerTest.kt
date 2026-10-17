/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotControllerTest {
    private var now = 1000.0
    private fun controller() = MascotController { now }

    @Test
    fun playsAtOnceWhenAtRest() {
        val c = controller()
        c.setAtRest(true)
        c.post(MascotMoment.Unlock)
        assertEquals(MascotReaction.WakeUp, c.reaction.value?.reaction)
        assertEquals(now, c.reaction.value!!.startSec, 0.0)
    }

    @Test
    fun waitsWhileBusyAndPlaysTheMomentTheIslandIsAtRest() {
        val c = controller()
        c.setAtRest(false)
        c.post(MascotMoment.Charging)
        assertNull(c.reaction.value)
        now += 5.0
        c.setAtRest(true)
        assertEquals(MascotReaction.Love, c.reaction.value?.reaction)
    }

    @Test
    fun givesUpAfterPatience() {
        val c = controller()
        c.setAtRest(false)
        c.post(MascotMoment.Charging)
        now += MascotReactionGate.PATIENCE_SEC + 0.1
        c.setAtRest(true)
        assertNull(c.reaction.value)
    }

    @Test
    fun theLatestWaitingMomentWins() {
        val c = controller()
        c.post(MascotMoment.Charging)
        now += 1.0
        c.post(MascotMoment.DownloadFinished)
        c.setAtRest(true)
        assertEquals(MascotReaction.Celebrate, c.reaction.value?.reaction)
    }

    @Test
    fun gateSpacesReactionsAtRest() {
        val c = controller()
        c.setAtRest(true)
        c.post(MascotMoment.Charging)
        val first = c.reaction.value
        now += 1.0
        c.post(MascotMoment.DownloadFinished)
        assertEquals(first, c.reaction.value)
        now += 2.0
        c.post(MascotMoment.DownloadFinished)
        assertEquals(MascotReaction.Celebrate, c.reaction.value?.reaction)
        assertNotEquals(first?.id, c.reaction.value?.id)
    }

    @Test
    fun noReactionsWhenTheSettingIsOff() {
        val c = controller()
        c.reacts = false
        c.setAtRest(true)
        c.post(MascotMoment.Unlock)
        assertNull(c.reaction.value)
    }

    @Test
    fun turningTheSettingOffDropsAWaitingMoment() {
        val c = controller()
        c.post(MascotMoment.Unlock)
        c.reacts = false
        c.reacts = true
        c.setAtRest(true)
        assertNull(c.reaction.value)
    }

    @Test
    fun demoIgnoresTheGateAndTheSetting() {
        val c = controller()
        c.reacts = false
        c.demo(MascotReaction.Love)
        val first = c.reaction.value
        c.demo(MascotReaction.Love)
        assertEquals(MascotReaction.Love, first?.reaction)
        assertNotEquals(first?.id, c.reaction.value?.id)
    }

    @Test
    fun everyMomentMapsToAReactionAndTheTableMatchesThePlan() {
        for (m in MascotMoment.entries.filter { it != MascotMoment.CountdownStart && it != MascotMoment.CountdownCancel }) assertTrue(m.name, m.reaction != null)
        assertEquals(MascotReaction.Love, MascotMoment.Charging.reaction)
        assertEquals(MascotReaction.Yawn, MascotMoment.BatteryLow.reaction)
        assertEquals(MascotReaction.Ready, MascotMoment.TimerStarted.reaction)
        assertEquals(MascotReaction.Surprised, MascotMoment.TimeIsUp.reaction)
        assertEquals(MascotReaction.Celebrate, MascotMoment.DownloadFinished.reaction)
        assertEquals(MascotReaction.Confused, MascotMoment.DownloadFailed.reaction)
        assertEquals(MascotReaction.Groove, MascotMoment.Music.reaction)
        assertEquals(MascotReaction.Perk, MascotMoment.TorchOn.reaction)
        assertEquals(MascotReaction.Yawn, MascotMoment.TorchOff.reaction)
        assertEquals(MascotReaction.WakeUp, MascotMoment.Unlock.reaction)
    }

    @Test
    fun leavingRestWhileNothingIsWaitingDoesNothing() {
        val c = controller()
        c.setAtRest(true)
        c.setAtRest(false)
        assertNull(c.reaction.value)
    }
}
