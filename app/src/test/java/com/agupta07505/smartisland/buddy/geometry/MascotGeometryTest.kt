/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.geometry

import com.agupta07505.smartisland.buddy.model.MascotEye
import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotPalette
import com.agupta07505.smartisland.buddy.model.MascotPoint
import com.agupta07505.smartisland.buddy.model.MascotShape
import com.agupta07505.smartisland.buddy.model.MascotSize
import com.agupta07505.smartisland.buddy.model.MascotStyle
import com.agupta07505.smartisland.buddy.model.face
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MascotGeometryTest {
    private val open = MascotSize(0.10f * 24f, 0.20f * 24f)

    @Test
    fun everyEyeHasExactlyTheSameNumberOfPointsOnBothSides() {
        for (eye in MascotEye.entries) for (right in listOf(false, true)) {
            val pts = MascotGeometry.eyePoints(eye, open, right)
            assertEquals("$eye right=$right", MascotGeometry.EYE_CURVE_COUNT, pts.size)
            assertTrue(pts.all { it.x.isFinite() && it.y.isFinite() })
        }
    }

    @Test
    fun rightEyeMirrorsTheLeftOne() {
        for (eye in MascotEye.entries) {
            val left = MascotGeometry.eyePoints(eye, open, false)
            val right = MascotGeometry.eyePoints(eye, open, true)
            // Mirroring flips x. Resampling then restarts at the top of the middle line, so the curves
            // match as point sets even if the walk direction differs.
            val leftXs = left.map { it.x }.sorted()
            val rightXs = right.map { -it.x }.sorted()
            for (i in leftXs.indices) assertEquals("$eye", leftXs[i], rightXs[i], 0.3f)
        }
    }

    @Test
    fun eyePointsStartOnTheMiddleLineAtTheTop() {
        for (eye in listOf(MascotEye.Open, MascotEye.Closed, MascotEye.Wide, MascotEye.Squint)) {
            val pts = MascotGeometry.eyePoints(eye, open, false)
            assertEquals("$eye start x", 0f, pts[0].x, 0.001f)
            assertTrue("$eye start is highest", pts.all { it.y >= pts[0].y - 0.01f })
        }
    }

    @Test
    fun resampleIsEvenlySpacedOnAClosedCircle() {
        val circle = (0 until 200).map {
            val a = it / 200f * 2f * Math.PI.toFloat()
            MascotPoint(10f * kotlin.math.cos(a), 10f * kotlin.math.sin(a))
        }
        val pts = MascotGeometry.resample(circle, 24)
        assertEquals(24, pts.size)
        val gaps = pts.indices.map {
            val a = pts[it]
            val b = pts[(it + 1) % pts.size]
            kotlin.math.hypot(a.x - b.x, a.y - b.y)
        }
        assertTrue("gaps ${gaps.min()}..${gaps.max()}", gaps.max() - gaps.min() < 0.3f)
    }

    @Test
    fun resampleOfTooFewPointsReturnsInput() {
        val two = listOf(MascotPoint(0f, 0f), MascotPoint(1f, 1f))
        assertEquals(two, MascotGeometry.resample(two, 24))
    }

    @Test
    fun determinedEyeIsSmallerThanAnOpenOne() {
        fun height(e: MascotEye): Float {
            val p = MascotGeometry.eyePoints(e, open, false)
            return p.maxOf { it.y } - p.minOf { it.y }
        }
        assertTrue(height(MascotEye.Determined) < height(MascotEye.Open))
        assertTrue(height(MascotEye.Squint) < height(MascotEye.Open))
    }

    @Test
    fun bodiesStayInsideTheirBox() {
        for (shape in MascotShape.entries) {
            val b = MascotGeometry.body(shape, 100f).bounds()
            assertTrue("$shape $b", b.left >= -0.5f && b.top >= -0.5f && b.right <= 100.5f && b.bottom <= 100.5f)
            assertTrue("$shape too small $b", b.width > 60f && b.height > 50f)
        }
    }

    @Test
    fun robotPartsAreInsideTheBoxAndHeadContainsVisor() {
        val r = MascotGeometry.robot(100f)
        val head = r.head.bounds()
        val visor = r.visor.bounds()
        assertTrue(visor.left > head.left && visor.right < head.right && visor.top > head.top && visor.bottom < head.bottom)
        for (part in listOf(r.head, r.ears, r.antenna, r.bulb, r.visor)) {
            val b = part.bounds()
            assertTrue(b.left >= -0.5f && b.top >= -0.5f && b.right <= 100.5f && b.bottom <= 100.5f)
        }
    }

    @Test
    fun faceEyesSitInsideTheirBodyBounds() {
        for (style in MascotStyle.entries) for (shape in MascotShape.entries) {
            val look = MascotLook(style, shape, MascotPalette.Pearl)
            val face = look.face
            val bounds = if (style == MascotStyle.Robot) MascotGeometry.robot(1f).head.bounds()
            else MascotGeometry.body(shape, 1f).bounds()
            assertTrue("$style $shape left eye", face.leftEye.x > bounds.left && face.leftEye.x < bounds.right)
            assertTrue("$style $shape eye y", face.leftEye.y > bounds.top && face.leftEye.y < bounds.bottom)
        }
    }

    @Test
    fun figureOffsetIsFiniteAndSmall() {
        for (style in MascotStyle.entries) for (shape in MascotShape.entries) {
            val off = MascotGeometry.figureOffset(MascotLook(style, shape, MascotPalette.Pearl))
            assertTrue("$style $shape $off", off.isFinite() && abs(off) < 0.25f)
        }
    }

    @Test
    fun smoothClosedPathHasOneCubicPerPoint() {
        val pts = MascotGeometry.eyePoints(MascotEye.Open, open, false)
        val path = MascotGeometry.smoothClosedPath(pts)
        assertEquals(pts.size, path.commands.count { it is PathCmd.Cubic })
        assertTrue(path.commands.first() is PathCmd.Move)
        assertTrue(path.commands.last() is PathCmd.Close)
        assertTrue(MascotGeometry.smoothClosedPath(listOf(MascotPoint.Zero)).commands.isEmpty())
    }

    @Test
    fun pathBoundsOfAnEllipseAreItsRect() {
        val b = MascotPathSpec.ellipseIn(MascotRect(10f, 20f, 50f, 40f)).bounds()
        assertEquals(10f, b.left, 0.2f); assertEquals(50f, b.right, 0.2f)
        assertEquals(20f, b.top, 0.2f); assertEquals(40f, b.bottom, 0.2f)
    }

    @Test
    fun shineIsCentredWhereTheLayoutSaysAndLeansWithItsAngle() {
        val face = MascotLook.Standard.face
        val shine = MascotGeometry.shine(face, 100f).bounds()
        assertEquals(face.shineCenter.x * 100f, (shine.left + shine.right) / 2f, 0.5f)
        assertEquals(face.shineCenter.y * 100f, shine.midY, 0.5f)
    }
}
