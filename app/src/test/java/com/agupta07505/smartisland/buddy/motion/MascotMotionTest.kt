/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.motion

import com.agupta07505.smartisland.buddy.model.MascotPath
import com.agupta07505.smartisland.buddy.model.MascotReaction
import com.agupta07505.smartisland.buddy.model.MascotSide
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.model.MascotVisitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotMotionTest {
    private val kinds: List<MascotVisitKind> = listOf(
        MascotVisitKind.Lap, MascotVisitKind.Pass, MascotVisitKind.Home, MascotVisitKind.Arrive,
        MascotVisitKind.Farewell, MascotVisitKind.Cross, MascotVisitKind.Retreat,
        MascotVisitKind.Cameo(MascotReaction.Love), MascotVisitKind.Linger(MascotReaction.Yawn),
        MascotVisitKind.Countdown(6f)
    )
    private val camera = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Left)
    private val cameraRight = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Right)
    private val capsule = MascotTrack.forPill(112f, 34f, 24f, null, null, MascotSide.Left)

    private fun tracks() = listOf("camera" to camera, "right" to cameraRight, "capsule" to capsule)

    @Test
    fun keyTimesStartAtZeroEndAtOneAndNeverGoBack() {
        for ((name, track) in tracks()) for (kind in kinds) {
            val p = MascotMotion.path(kind, track)
            assertTrue("$name $kind empty", p.size > 1)
            assertEquals("$name $kind first", 0f, p.keyTimes.first(), 0f)
            assertEquals("$name $kind last", 1f, p.keyTimes.last(), 0f)
            for (i in 1 until p.size) assertTrue("$name $kind $i", p.keyTimes[i] >= p.keyTimes[i - 1])
        }
    }

    @Test
    fun allSamplesAreFiniteAndLiftIsNeverNegative() {
        for ((name, track) in tracks()) for (kind in kinds) {
            val p = MascotMotion.path(kind, track)
            for (i in 0 until p.size) {
                assertTrue("$name $kind x", p.x[i].isFinite())
                assertTrue("$name $kind lift", p.lift[i].isFinite() && p.lift[i] >= 0f)
                assertTrue("$name $kind squash", p.squash[i].isFinite())
                assertTrue("$name $kind gaze", p.gaze[i].isFinite())
            }
        }
    }

    @Test
    fun hopsNeverLeaveThePill() {
        // The figure's top (centre - size/2 - lift) must stay inside the pill, whose top is the ceiling.
        for ((name, track) in tracks()) for (kind in kinds) {
            val p = MascotMotion.path(kind, track)
            val maxLift = p.lift.max()
            val room = track.baseline - track.size / 2f - track.ceiling
            assertTrue("$name $kind lift $maxLift room $room", maxLift <= maxOf(room, track.scale) + 0.001f)
        }
    }

    @Test
    fun durationsMatchTheReference() {
        assertEquals(3.6f, MascotMotion.duration(MascotVisitKind.Lap), 0f)
        assertEquals(3.4f, MascotMotion.duration(MascotVisitKind.Pass), 0f)
        assertEquals(0.62f, MascotMotion.duration(MascotVisitKind.Home), 0f)
        assertEquals(0.62f, MascotMotion.duration(MascotVisitKind.Arrive), 0f)
        assertEquals(0.78f, MascotMotion.duration(MascotVisitKind.Farewell), 0f)
        assertEquals(1.24f, MascotMotion.duration(MascotVisitKind.Cross), 0f)
        assertEquals(0.5f + 1.4f + 0.3f + 0.32f, MascotMotion.duration(MascotVisitKind.Cameo(MascotReaction.Love)), 0.0001f)
        assertEquals(1.2f + 0.3f + 0.32f, MascotMotion.duration(MascotVisitKind.Linger(MascotReaction.Yawn)), 0.0001f)
        for ((name, track) in tracks()) for (kind in kinds) {
            // A capsule has no sides, so crossing it is only the little hop home (as in the reference).
            if (name == "capsule" && kind == MascotVisitKind.Cross) continue
            assertEquals("$name $kind", MascotMotion.duration(kind), MascotMotion.path(kind, track).durationSec, 0.0001f)
        }
        assertEquals(0.62f, MascotMotion.path(MascotVisitKind.Cross, capsule).durationSec, 0f)
    }

    @Test
    fun lapStartsAndEndsAtItsRestingPlace() {
        for ((name, track) in listOf("camera" to camera, "right" to cameraRight, "capsule" to capsule)) {
            val p = MascotMotion.path(MascotVisitKind.Lap, track)
            assertEquals("$name start", track.rest, p.x.first(), 0.5f)
            assertEquals("$name end", track.rest, p.x.last(), 0.5f)
        }
    }

    @Test
    fun lapGoesPastTheCameraAndOutTheFarEnd() {
        val p = MascotMotion.path(MascotVisitKind.Lap, camera)
        val hole = camera.hidden!!
        assertTrue("passes the camera", p.x.any { it > hole.endInclusive })
        assertTrue("leaves at the far end", p.x.any { it >= camera.width + camera.size * 0.9f })
        assertTrue("re-enters at the near end", p.x.any { it <= -camera.size * 0.9f })
    }

    @Test
    fun aMirroredPathIsTheLeftOneSeenInAMirror() {
        for (kind in listOf(MascotVisitKind.Lap, MascotVisitKind.Pass, MascotVisitKind.Farewell, MascotVisitKind.Arrive)) {
            val left = MascotMotion.path(kind, camera)
            val right = MascotMotion.path(kind, cameraRight)
            assertEquals(left.size, right.size)
            for (i in 0 until left.size) {
                assertEquals("$kind x $i", camera.width - left.x[i], right.x[i], 0.001f)
                assertEquals("$kind gaze $i", -left.gaze[i], right.gaze[i], 0.0001f)
                assertEquals("$kind lift $i", left.lift[i], right.lift[i], 0.0001f)
            }
        }
    }

    @Test
    fun arrivingBesideACameraStartsJustBehindIt() {
        val p = MascotMotion.path(MascotVisitKind.Arrive, camera)
        val hole = camera.hidden!!
        assertEquals(camera.behindCamera(hole, 2f), p.x.first(), 0.001f)
        assertEquals(camera.rest, p.x.last(), 0.5f)
    }

    @Test
    fun arrivingInACapsuleHopsInFromTheNearEnd() {
        val p = MascotMotion.path(MascotVisitKind.Arrive, capsule)
        assertEquals(-capsule.size, p.x.first(), 0.001f)
        assertEquals(capsule.rest, p.x.last(), 0.5f)
    }

    @Test
    fun theIdleTuckEndsBehindTheCamera() {
        val hole = camera.hidden!!
        val p = MascotMotion.path(MascotVisitKind.Linger(MascotReaction.Yawn), camera)
        assertEquals(camera.rest, p.x.first(), 0.001f)
        assertEquals(camera.behindCamera(hole, 1f), p.x.last(), 0.5f)
        assertTrue(MascotVisitKind.Linger(MascotReaction.Yawn).endsOutOfSight)
    }

    @Test
    fun farewellEndsOutOfSightAndGreetsFirst() {
        val p = MascotMotion.path(MascotVisitKind.Farewell, camera)
        assertEquals(camera.rest, p.x.first(), 0.5f)
        assertTrue(p.greeting.endInclusive > p.greeting.start)
        assertTrue(MascotVisitKind.Farewell.endsOutOfSight)
    }

    @Test
    fun visitKindFlagsMatchTheReference() {
        assertTrue(MascotVisitKind.Countdown(5f).watchesTimer)
        assertTrue(MascotVisitKind.Retreat.watchesTimer)
        assertTrue(!MascotVisitKind.Lap.watchesTimer)
        assertTrue(MascotVisitKind.Cameo(MascotReaction.Love).takesOnlyItsWing)
        assertTrue(!MascotVisitKind.Lap.takesOnlyItsWing)
        assertEquals(MascotReaction.Love, MascotVisitKind.Cameo(MascotReaction.Love).visitReaction)
        assertEquals(null, MascotVisitKind.Lap.visitReaction)
        assertTrue(!MascotVisitKind.Lap.endsOutOfSight && !MascotVisitKind.Home.endsOutOfSight)
    }

    @Test
    fun countdownIsWatchedFromTheLeftWhicheverSideItRestsOn() {
        val left = MascotMotion.path(MascotVisitKind.Countdown(6f), camera)
        val right = MascotMotion.path(MascotVisitKind.Countdown(6f), cameraRight)
        assertEquals(left.size, right.size)
        for (i in 0 until left.size) assertEquals(left.x[i], right.x[i], 0.001f)
    }

    @Test
    fun pathSamplingInterpolatesAndClamps() {
        val p = MascotPath(
            floatArrayOf(0f, 0.5f, 1f), floatArrayOf(0f, 10f, 20f), floatArrayOf(0f, 0f, 0f),
            floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), 2f
        )
        assertEquals(0f, p.at(0f).x, 0f)
        assertEquals(5f, p.at(0.5f).x, 0.001f)
        assertEquals(10f, p.at(1f).x, 0.001f)
        assertEquals(15f, p.at(1.5f).x, 0.001f)
        assertEquals(20f, p.at(2f).x, 0f)
        assertEquals(20f, p.at(99f).x, 0f)
        assertEquals(0f, p.at(-3f).x, 0f)
        assertEquals(0f, MascotPath.Empty.at(1f).x, 0f)
    }

    @Test
    fun trackHelpers() {
        val hole = camera.hidden!!
        assertEquals(hole.start + camera.size / 2f + 2f, camera.behindCamera(hole, 2f), 0.001f)
        assertEquals(hole.endInclusive + (hole.start - camera.rest), camera.farSpot, 0.001f)
        assertEquals(camera.rest, camera.leftSide.rest, 0f)
        assertEquals(cameraRight.width - cameraRight.rest, cameraRight.leftSide.rest, 0.001f)
        assertEquals(false, cameraRight.leftSide.mirrored)
        assertTrue(camera.hop(0.24f) <= camera.size * 0.24f + 0.001f)
    }
}
