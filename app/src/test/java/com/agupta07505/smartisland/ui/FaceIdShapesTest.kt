/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceIdShapesTest {
    @Test
    fun resampleGivesTheRequestedNumberOfPointsOnTheOutline() {
        val square = floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        val out = faceIdResample(square, 8)
        assertEquals(16, out.size)
        // Perimeter 40, so one point every 5 units, starting at the first corner.
        assertEquals(0f, out[0], 1e-4f)
        assertEquals(0f, out[1], 1e-4f)
        assertEquals(5f, out[2], 1e-4f)
        assertEquals(0f, out[3], 1e-4f)
        assertEquals(10f, out[4], 1e-4f)
        assertEquals(0f, out[5], 1e-4f)
    }

    @Test
    fun alignKeepsTheSamePointsInANewOrder() {
        val (face, tick) = faceIdMorphOutlines()
        assertEquals(FACE_ID_MORPH_POINTS * 2, face.size)
        assertEquals(face.size, tick.size)
        val raw = faceIdResample(FACE_ID_TICK, FACE_ID_MORPH_POINTS)
        val rawSorted = raw.toList().chunked(2).map { it[0] to it[1] }.sortedWith(compareBy({ it.first }, { it.second }))
        val alignedSorted = tick.toList().chunked(2).map { it[0] to it[1] }.sortedWith(compareBy({ it.first }, { it.second }))
        assertEquals(rawSorted, alignedSorted)
    }

    @Test
    fun alignedTickStartsNearTheFaceOutlineStart() {
        val (face, tick) = faceIdMorphOutlines()
        var total = 0f
        for (i in 0 until FACE_ID_MORPH_POINTS) {
            val dx = face[2 * i] - tick[2 * i]
            val dy = face[2 * i + 1] - tick[2 * i + 1]
            total += kotlin.math.hypot(dx, dy)
        }
        assertTrue("average travel was ${total / FACE_ID_MORPH_POINTS}", total / FACE_ID_MORPH_POINTS < 45f)
    }

    @Test
    fun blendEndsAreTheTwoOutlines() {
        val (face, tick) = faceIdMorphOutlines()
        val out = FloatArray(face.size)
        faceIdBlend(face, tick, 0f, out)
        assertArrayEquals(face, out, 1e-4f)
        faceIdBlend(face, tick, 1f, out)
        assertArrayEquals(tick, out, 1e-4f)
        faceIdBlend(face, tick, 0.5f, out)
        assertEquals((face[10] + tick[10]) / 2f, out[10], 1e-4f)
    }

    @Test
    fun tickSitsInsideTheFaceAreaAndIsCentredOnIt() {
        val xs = FACE_ID_TICK.filterIndexed { i, _ -> i % 2 == 0 }
        val ys = FACE_ID_TICK.filterIndexed { i, _ -> i % 2 == 1 }
        assertTrue(xs.min() > 65.8f && xs.max() < 154.2f)
        assertTrue(ys.min() > 60.8f && ys.max() < 136.1f)
        assertEquals(FACE_ID_HEAD_CX, (xs.min() + xs.max()) / 2f, 3f)
    }

    @Test
    fun clipKeepsOnlyOneSideOfTheCut() {
        val left = faceIdClipX(FACE_ID_TICK, FACE_ID_TICK_SPLIT_X, keepLeft = true)
        val right = faceIdClipX(FACE_ID_TICK, FACE_ID_TICK_SPLIT_X, keepLeft = false)
        assertTrue(left.size >= 6 && right.size >= 6)
        assertTrue(left.filterIndexed { i, _ -> i % 2 == 0 }.all { it <= FACE_ID_TICK_SPLIT_X + 1e-3f })
        assertTrue(right.filterIndexed { i, _ -> i % 2 == 0 }.all { it >= FACE_ID_TICK_SPLIT_X - 1e-3f })
    }

    private val u = 1.25f

    private fun pillTrack() = FaceIdEdgeTrack(
        cx = FACE_ID_HEAD_CX, cy = 17f * u, w = 112f * u, h = 34f * u, r = 17f * u,
        thickness = FaceIdArcThickness.Medium.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u
    )

    private fun outlineOf(track: FaceIdEdgeTrack, left: Boolean, turn: Float): FloatArray {
        val out = FloatArray(FACE_ID_ARC_OUTLINE_POINTS * 2)
        if (left) track.leftOutlineInto(turn, out) else track.rightOutlineInto(turn, out)
        return out
    }

    private fun xs(p: FloatArray) = p.filterIndexed { i, _ -> i % 2 == 0 }
    private fun ys(p: FloatArray) = p.filterIndexed { i, _ -> i % 2 == 1 }

    @Test
    fun leftArcHugsTheLeftSideInsideTheCutoutAndLeavesTheMiddleEmpty() {
        val arc = outlineOf(pillTrack(), left = true, turn = 0f)
        val x = xs(arc)
        val y = ys(arc)
        assertTrue(x.min() >= FACE_ID_HEAD_CX - 56f * u)
        assertTrue(y.min() >= 0f && y.max() <= 34f * u)
        assertTrue(x.max() < FACE_ID_HEAD_CX - 10f)
        assertTrue(x.max() > FACE_ID_HEAD_CX - 56f * u + 17f * u)
        assertTrue(y.min() < 2f && y.max() > 34f * u - 2f)
    }

    @Test
    fun rightArcIsTheLeftArcMirrored() {
        val track = pillTrack()
        val left = outlineOf(track, left = true, turn = 0f)
        val right = outlineOf(track, left = false, turn = 0f)
        assertEquals(2f * FACE_ID_HEAD_CX - xs(left).min(), xs(right).max(), 0.05f)
        assertEquals(2f * FACE_ID_HEAD_CX - xs(left).max(), xs(right).min(), 0.05f)
        assertEquals(ys(left).min(), ys(right).min(), 0.05f)
        assertEquals(ys(left).max(), ys(right).max(), 0.05f)
    }

    @Test
    fun turningHalfWayRoundSwapsTheTwoArcs() {
        val track = pillTrack()
        val half = track.perimeter / 2f
        val leftTurned = outlineOf(track, left = true, turn = -half)
        val rightAtRest = outlineOf(track, left = false, turn = 0f)
        assertEquals(xs(rightAtRest).min(), xs(leftTurned).min(), 0.1f)
        assertEquals(xs(rightAtRest).max(), xs(leftTurned).max(), 0.1f)
        assertEquals(ys(rightAtRest).min(), ys(leftTurned).min(), 0.1f)
        assertEquals(ys(rightAtRest).max(), ys(leftTurned).max(), 0.1f)
        // A full perimeter brings an arc back to where it started.
        val round = outlineOf(track, left = true, turn = -track.perimeter)
        assertArrayEquals(outlineOf(track, left = true, turn = 0f), round, 0.01f)
    }

    @Test
    fun trackPointsStayOnTheCutoutAndWrap() {
        val track = pillTrack()
        val p = FloatArray(2)
        track.pointAt(0f, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-3f)
        val topY = p[1]
        track.pointAt(track.perimeter, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-3f)
        assertEquals(topY, p[1], 1e-3f)
        // Half way round from the top middle is the bottom middle.
        track.pointAt(track.perimeter / 2f, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-2f)
        assertEquals(34f * u - topY, p[1], 1e-2f)
        for (k in 0 until 40) {
            track.pointAt(track.perimeter * k / 40f, p, 0)
            assertTrue(p[0] >= FACE_ID_HEAD_CX - 56f * u && p[0] <= FACE_ID_HEAD_CX + 56f * u)
            assertTrue(p[1] >= 0f && p[1] <= 34f * u)
        }
    }

    @Test
    fun aRoundCutoutStillGetsTwoSeparateArcs() {
        val size = 26f * u
        val track = FaceIdEdgeTrack(
            cx = FACE_ID_HEAD_CX, cy = size / 2f, w = size, h = size, r = size / 2f,
            thickness = FaceIdArcThickness.Medium.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u
        )
        assertTrue(track.arcLength <= track.perimeter / 2f - 3f * track.thickness + 1e-3f)
        val left = outlineOf(track, left = true, turn = 0f)
        val right = outlineOf(track, left = false, turn = 0f)
        assertTrue(xs(left).max() < FACE_ID_HEAD_CX)
        assertTrue(xs(right).min() > FACE_ID_HEAD_CX)
    }

    @Test
    fun splitOutlinesPairEachHalfWithItsOwnSideArc() {
        val split = faceIdSplitOutlines(pillTrack())
        assertEquals(FACE_ID_SPLIT_POINTS * 2, split.leftFrom.size)
        assertEquals(split.leftFrom.size, split.leftTo.size)
        assertEquals(split.rightFrom.size, split.rightTo.size)
        assertTrue(xs(split.leftTo).max() < FACE_ID_HEAD_CX)
        assertTrue(xs(split.rightTo).min() > FACE_ID_HEAD_CX)
        var total = 0f
        for (i in 0 until FACE_ID_SPLIT_POINTS) {
            total += kotlin.math.hypot(split.leftFrom[2 * i] - split.leftTo[2 * i], split.leftFrom[2 * i + 1] - split.leftTo[2 * i + 1])
        }
        assertTrue("average travel was ${total / FACE_ID_SPLIT_POINTS}", total / FACE_ID_SPLIT_POINTS < 110f)
    }

    @Test
    fun mediumIsTheDefaultArcThickness() {
        assertEquals(3f, FaceIdArcThickness.Medium.dp, 0f)
        assertTrue(FaceIdArcThickness.Thin.dp < FaceIdArcThickness.Medium.dp)
        assertTrue(FaceIdArcThickness.Medium.dp < FaceIdArcThickness.Thick.dp)
    }

    @Test
    fun arcKeepsFullThicknessAlongItsSideAndThinsContinuouslyToBothTips() {
        val track = pillTrack()
        val full = track.thickness / 2f
        val n = FACE_ID_ARC_SEGMENTS_FOR_TEST
        val mid = track.halfWidthAt(n / 2)
        assertEquals(full, mid, 1e-3f)
        var previous = mid
        for (k in n / 2..n) {
            val w = track.halfWidthAt(k)
            assertTrue("width grew at $k", w <= previous + 1e-4f)
            assertTrue(w <= full + 1e-4f)
            previous = w
        }
        assertEquals(full * FACE_ID_ARC_TIP_WIDTH, track.halfWidthAt(n), 1e-3f)
        assertEquals(full * FACE_ID_ARC_TIP_WIDTH, track.halfWidthAt(0), 1e-3f)
        // Mirror image around the middle.
        for (k in 0..n) assertEquals(track.halfWidthAt(k), track.halfWidthAt(n - k), 1e-3f)
        // No sudden step between neighbours.
        for (k in 1..n) assertTrue(kotlin.math.abs(track.halfWidthAt(k) - track.halfWidthAt(k - 1)) < full * 0.2f)
    }

    @Test
    fun thickPartCoversTheStraightSideOfThePill() {
        val track = pillTrack()
        assertTrue(track.halfWidthAt(FACE_ID_ARC_SEGMENTS_FOR_TEST / 4) < track.thickness / 2f)
        val tall = FaceIdEdgeTrack(
            cx = FACE_ID_HEAD_CX, cy = 40f * u, w = 112f * u, h = 80f * u, r = 12f * u,
            thickness = FaceIdArcThickness.Medium.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u
        )
        assertEquals(tall.thickness / 2f, tall.halfWidthAt(FACE_ID_ARC_SEGMENTS_FOR_TEST / 2 + 2), 1e-3f)
    }

    @Test
    fun taperedArcStaysInsideTheCutout() {
        val arc = outlineOf(pillTrack(), left = true, turn = 0f)
        assertTrue(xs(arc).min() >= FACE_ID_HEAD_CX - 56f * u)
        assertTrue(ys(arc).min() >= 0f && ys(arc).max() <= 34f * u)
    }

    private val FACE_ID_ARC_SEGMENTS_FOR_TEST = 48

    @Test
    fun theCrossHasTwelveCornersCentredOnTheFaceAndInsideTheHead() {
        assertEquals(24, FACE_ID_CROSS.size)
        var sx = 0f
        var sy = 0f
        for (i in 0 until 12) {
            val dx = FACE_ID_CROSS[2 * i] - FACE_ID_HEAD_CX
            val dy = FACE_ID_CROSS[2 * i + 1] - FACE_ID_HEAD_CY
            sx += dx
            sy += dy
            // Every corner is well inside the 110 wide blue head.
            assertTrue(kotlin.math.hypot(dx, dy) < 40f)
        }
        assertEquals(0f, sx, 1e-3f)
        assertEquals(0f, sy, 1e-3f)
    }

    @Test
    fun theCrossMorphOutlinesHaveTheSamePointCountAsTheFace() {
        val (face, cross) = faceIdCrossMorphOutlines()
        assertEquals(FACE_ID_MORPH_POINTS * 2, face.size)
        assertEquals(face.size, cross.size)
    }

    @Test
    fun fingerprintTickOutlinesHaveTheMorphPointCount() {
        val (from, to) = faceIdFingerprintTickOutlines()
        assertEquals(FACE_ID_MORPH_POINTS * 2, from.size)
        assertEquals(from.size, to.size)
    }
}
