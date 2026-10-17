/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction
import com.agupta07505.smartisland.buddy.model.MascotSide
import com.agupta07505.smartisland.buddy.model.MascotTrack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MascotTouchTest {
    private var now = 10.0
    private fun controller() = MascotController { now }
    private val track = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Left)

    @Test
    fun gazeTurnsTowardTheTouchAndIsCapped() {
        val (rx, ry) = MascotGaze.pointerGaze(40f, 17f, 100f, 17f, 24f)
        assertTrue(rx > 0f && rx <= 0.08f && ry == 0f)
        val (lx, _) = MascotGaze.pointerGaze(40f, 17f, -500f, 17f, 24f)
        assertEquals(-0.08f, lx, 0.0001f)
        val (_, dy) = MascotGaze.pointerGaze(40f, 17f, 40f, 500f, 24f)
        assertEquals(0.05f, dy, 0.0001f)
        assertEquals(0f to 0f, MascotGaze.pointerGaze(40f, 17f, 90f, 90f, 0f))
    }

    @Test
    fun aNearerTouchTurnsTheEyesLess() {
        val near = MascotGaze.pointerGaze(40f, 17f, 55f, 17f, 24f).first
        val far = MascotGaze.pointerGaze(40f, 17f, 95f, 17f, 24f).first
        assertTrue(near in 0f..far)
    }

    @Test
    fun aTouchOnTheBuddyIsAPatTargetAndOneFarAwayIsNot() {
        assertTrue(track.touches(track.rest, track.baseline, 0f))
        assertTrue(track.touches(track.rest + track.size / 2f + 5f, track.baseline, 8f))
        assertFalse(track.touches(track.rest + track.size / 2f + 20f, track.baseline, 8f))
        assertFalse(track.touches(track.rest, track.baseline + 40f, 8f))
    }

    @Test
    fun attentionIsPublishedOnlyWhileAtRest() = runTest(UnconfinedTestDispatcher()) {
        val c = controller()
        val seen = ArrayList<MascotAttention>()
        val job = launch { c.attention.toList(seen) }
        c.attend(10f, 10f)
        assertEquals(0, seen.size)
        c.setAtRest(true)
        c.attend(20f, 12f)
        assertEquals(1, seen.size)
        assertEquals(20f, seen[0].xDp, 0f)
        c.startVisit(com.agupta07505.smartisland.buddy.model.MascotVisitKind.Lap)
        c.attend(30f, 12f)
        assertEquals(1, seen.size)
        job.cancel()
    }

    @Test
    fun aPatPlaysHeartsOnceInAWhile() {
        val c = controller()
        c.setAtRest(true)
        c.pat()
        assertEquals(MascotReaction.Love, c.reaction.value?.reaction)
        val first = c.reaction.value
        now += 10.0
        c.pat()
        assertEquals(first, c.reaction.value)
        now += 25.0
        c.pat()
        assertEquals(MascotReaction.Love, c.reaction.value?.reaction)
        assertTrue(c.reaction.value!!.id != first!!.id)
    }

    @Test
    fun noPatWhenNotAtRest() {
        val c = controller()
        c.pat()
        assertEquals(null, c.reaction.value)
    }
}
