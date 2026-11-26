/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.ui.collapsedGroupSpan
import com.agupta07505.smartisland.ui.notchGroupGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsedGroupSpanTest {
    private val screen = 1000f
    private val edge = 20f
    private val main = 300f
    private val circle = 80f
    private val gap = 20f

    private fun span(
        desired: Float = 350f,
        split: Boolean = false,
        circleLeft: Boolean = false,
        notchXOffset: Float? = null
    ) = collapsedGroupSpan(desired, screen, edge, main, circle, gap, split, circleLeft, notchXOffset)

    @Test
    fun collapsedGroupBounds() {
        val single = span()
        assertEquals(350f, single.mainLeftPx, 0f)
        assertEquals(350f, single.startPx, 0f)
        assertEquals(650f, single.endPx, 0f)

        val right = span(split = true)
        assertEquals(350f, right.startPx, 0f)
        assertEquals(650f + gap + circle, right.endPx, 0f)

        val left = span(split = true, circleLeft = true)
        assertEquals(350f, left.mainLeftPx, 0f)
        assertEquals(350f - gap - circle, left.startPx, 0f)
        assertEquals(650f, left.endPx, 0f)

        val clamped = span(desired = -400f, split = true, circleLeft = true)
        assertEquals(edge + circle + gap, clamped.mainLeftPx, 0f)
        assertEquals(edge, clamped.startPx, 0f)
        assertEquals(edge + circle + gap + main, clamped.endPx, 0f)

        val notch = span(desired = 50f, notchXOffset = 30f)
        assertEquals(380f, notch.mainLeftPx, 0f)
        assertEquals(380f, notch.startPx, 0f)
        assertEquals(680f, notch.endPx, 0f)
    }

    @Test
    fun notchGroupWidth() {
        val right = span(split = true, notchXOffset = 0f)
        assertEquals(350f, right.mainLeftPx, 0f)
        assertEquals(350f, right.startPx, 0f)
        assertEquals(650f + gap + circle, right.endPx, 0f)
        val left = span(split = true, circleLeft = true, notchXOffset = 0f)
        assertEquals(350f, left.mainLeftPx, 0f)
        assertEquals(350f - gap - circle, left.startPx, 0f)
        assertEquals(650f, left.endPx, 0f)

        assertEquals(0, span().windowXPx(screen))
        assertEquals(50, span(split = true).windowXPx(screen))
    }
}

class NotchGroupGeometryTest {

    private fun geo(xOffset: Float = 0f, hasEar: Boolean = true, preferLeft: Boolean = false) =
        notchGroupGeometry(
            screenWidth = 400f, xOffset = xOffset, notchWidth = 100f, earWidth = 30f,
            gap = 8f, edgeMargin = 8f, hasEar = hasEar, preferLeft = preferLeft
        )

    @Test
    fun notchStaysPutWithOrWithoutEar() {
        assertEquals(150f, geo(hasEar = false).mainLeft, 0.001f)
        assertEquals(150f, geo(hasEar = true).mainLeft, 0.001f)
        assertEquals(170f, geo(xOffset = 20f).mainLeft, 0.001f)
    }

    @Test
    fun noEarMeansTheGroupIsJustTheNotch() {
        val g = geo(hasEar = false)
        assertEquals(150f, g.groupStart, 0.001f)
        assertEquals(250f, g.groupEnd, 0.001f)
    }

    @Test
    fun earSitsOneGapFromNotch() {
        val right = geo(preferLeft = false)
        assertFalse(right.earOnLeft)
        assertEquals(258f, right.earLeft, 0.001f)
        assertEquals(288f, right.groupEnd, 0.001f)
        val left = geo(preferLeft = true)
        assertTrue(left.earOnLeft)
        assertEquals(112f, left.earLeft, 0.001f)
        assertEquals(112f, left.groupStart, 0.001f)
        assertEquals(250f, left.groupEnd, 0.001f)
    }

    @Test
    fun earFlipsWhenNoRoom() {
        // notch pushed far right: 400 - 8 - 340 = 52 left on the right side, fits 38
        val crowdedRight = geo(xOffset = 130f, preferLeft = false)
        assertTrue(crowdedRight.earOnLeft)
        val crowdedLeft = geo(xOffset = -130f, preferLeft = true)
        assertFalse(crowdedLeft.earOnLeft)
    }

    @Test
    fun earKeepsSideWhenNoneFits() {
        val g = notchGroupGeometry(
            screenWidth = 150f, xOffset = 0f, notchWidth = 100f, earWidth = 30f,
            gap = 8f, edgeMargin = 8f, hasEar = true, preferLeft = false
        )
        assertFalse(g.earOnLeft)
    }

    @Test
    fun earSpotKeepsSideNoEar() {
        assertTrue(geo(xOffset = 130f, hasEar = false, preferLeft = false).earOnLeft)
        assertEquals(geo(xOffset = 130f, hasEar = true).earLeft, geo(xOffset = 130f, hasEar = false).earLeft, 0.001f)
        // and the group is still only the notch
        assertEquals(geo(xOffset = 130f, hasEar = false).mainLeft, geo(xOffset = 130f, hasEar = false).groupStart, 0.001f)
    }
}
