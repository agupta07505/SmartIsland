/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotReaction
import com.agupta07505.smartisland.buddy.model.MascotVisitFrequency
import com.agupta07505.smartisland.buddy.model.MascotVisitKind
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotVisitsTest {
    private var now = 500.0
    private fun controller() = MascotController { now }

    @Test
    fun aVisitStartsOnlyAtRestAndOnlyOneAtATime() {
        val c = controller()
        assertFalse(c.startVisit(MascotVisitKind.Lap))
        c.setAtRest(true)
        assertTrue(c.startVisit(MascotVisitKind.Lap))
        assertFalse(c.startVisit(MascotVisitKind.Lap))
        val id = c.visit.value!!.id
        c.endVisit(id)
        assertNull(c.visit.value)
        assertTrue(c.startVisit(MascotVisitKind.Lap))
    }

    @Test
    fun endingAStaleVisitIsIgnored() {
        val c = controller()
        c.setAtRest(true)
        c.startVisit(MascotVisitKind.Lap)
        c.endVisit(-5)
        assertNotNull(c.visit.value)
    }

    @Test
    fun reactionsWaitForAVisitToEnd() {
        val c = controller()
        c.setAtRest(true)
        c.startVisit(MascotVisitKind.Lap)
        c.post(MascotMoment.Charging)
        assertNull(c.reaction.value)
        now += 3.6
        c.endVisit(c.visit.value!!.id)
        assertEquals(MascotReaction.Love, c.reaction.value?.reaction)
    }

    @Test
    fun theIdleTuckTucksWhenItEndsAndOnlyThen() {
        val c = controller()
        c.setAtRest(true)
        assertTrue(c.requestTuck())
        assertFalse(c.tucked.value)
        c.endVisit(c.visit.value!!.id)
        assertTrue(c.tucked.value)
    }

    @Test
    fun anOrdinaryVisitDoesNotTuck() {
        val c = controller()
        c.setAtRest(true)
        c.startVisit(MascotVisitKind.Lap)
        c.endVisit(c.visit.value!!.id)
        assertFalse(c.tucked.value)
    }

    @Test
    fun untuckingBringsItBackOutWithAnArrival() {
        val c = controller()
        c.setAtRest(true)
        c.requestTuck()
        c.endVisit(c.visit.value!!.id)
        c.untuck(MascotMood.Wink)
        assertFalse(c.tucked.value)
        assertEquals(MascotVisitKind.Arrive, c.visit.value?.kind)
        assertEquals(MascotMood.Wink, c.visit.value?.greeting)
    }

    @Test
    fun untuckWhenNotTuckedDoesNothing() {
        val c = controller()
        c.setAtRest(true)
        c.untuck()
        assertNull(c.visit.value)
    }

    @Test
    fun aReactionWhileTuckedBringsItBackOutFirstAndPlaysAfterItLands() {
        val c = controller()
        c.setAtRest(true)
        c.requestTuck()
        c.endVisit(c.visit.value!!.id)
        now += 40.0
        c.post(MascotMoment.DownloadFinished)
        assertEquals(MascotVisitKind.Arrive, c.visit.value?.kind)
        assertNull(c.reaction.value)
        now += 0.62
        c.endVisit(c.visit.value!!.id)
        assertEquals(MascotReaction.Celebrate, c.reaction.value?.reaction)
    }

    @Test
    fun hideForArrivalTucksAtOnceAndResetClearsEverything() {
        val c = controller()
        c.hideForArrival()
        assertTrue(c.tucked.value)
        c.setAtRest(true)
        c.startVisit(MascotVisitKind.Lap)
        c.reset()
        assertNull(c.visit.value)
        assertFalse(c.tucked.value)
    }

    @Test
    fun visitDelaysFollowTheFrequency() {
        val rng = Random(3)
        assertNull(MascotVisitScheduler.nextDelaySec(MascotVisitFrequency.Off, rng))
        repeat(300) {
            assertTrue(MascotVisitScheduler.nextDelaySec(MascotVisitFrequency.Rare, rng)!! in 600f..1200f)
            assertTrue(MascotVisitScheduler.nextDelaySec(MascotVisitFrequency.Normal, rng)!! in 240f..540f)
            assertTrue(MascotVisitScheduler.nextDelaySec(MascotVisitFrequency.Frequent, rng)!! in 90f..180f)
        }
    }

    @Test
    fun greetingsAreFriendlyFaces() {
        val seen = (0 until 200).map { MascotVisitScheduler.greeting(Random(it)) }.toSet()
        assertEquals(setOf(MascotMood.Happy, MascotMood.Wink, MascotMood.Love), seen)
        assertEquals(30f, MascotVisitScheduler.HIDE_DELAY_SEC, 0f)
    }
}
