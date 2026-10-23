/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.ui.GOO_ALPHA_GAIN
import com.agupta07505.smartisland.ui.GOO_ALPHA_OFFSET
import com.agupta07505.smartisland.ui.gooeyBlurDp
import com.agupta07505.smartisland.ui.gooeyLayerScale
import com.agupta07505.smartisland.ui.gooeyBridgeActive
import com.agupta07505.smartisland.ui.gooeyShapes
import com.agupta07505.smartisland.ui.mergeTouchCrossed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeGooTest {
    @Test
    fun touchFiresOnceWhenMergeCrossesContactGoingUp() {
        assertTrue(mergeTouchCrossed(previous = 0.3f, now = 0.45f, contact = 0.4f))
        assertFalse(mergeTouchCrossed(previous = 0.45f, now = 0.6f, contact = 0.4f))
        assertFalse(mergeTouchCrossed(previous = 0.2f, now = 0.35f, contact = 0.4f))
    }

    @Test
    fun touchNeverFiresOnTheWayBackOrOnAFinishedMerge() {
        assertFalse(mergeTouchCrossed(previous = 0.6f, now = 0.3f, contact = 0.4f))
        assertFalse(mergeTouchCrossed(previous = 0.5f, now = 1f, contact = 0.4f))
    }

    @Test
    fun touchOnAContactAtTheVeryEdgesIsStillOneTick() {
        // Bubble already touching the pill (contact 0): counted as a touch at the first step.
        assertTrue(mergeTouchCrossed(previous = 0f, now = 0.1f, contact = 0f))
        assertFalse(mergeTouchCrossed(previous = 0.1f, now = 0.2f, contact = 0f))
    }

    @Test
    fun gooBridgeIsOnlyDrawnDuringTheJoin() {
        val c = 0.4f
        val joinStart = c + (1f - c) * 0.37f
        assertFalse(gooeyBridgeActive(merge = 0f, contact = c))
        assertFalse(gooeyBridgeActive(merge = 0.3f, contact = c))
        assertFalse(gooeyBridgeActive(merge = c + 0.05f, contact = c))
        assertFalse(gooeyBridgeActive(merge = joinStart, contact = c))
        assertTrue(gooeyBridgeActive(merge = joinStart + 0.05f, contact = c))
        assertTrue(gooeyBridgeActive(merge = 0.9f, contact = c))
        assertFalse(gooeyBridgeActive(merge = 1f, contact = c))
    }

    @Test
    fun thresholdMapsHalfAlphaToHalfAndSteepensEverythingAround() {
        fun out(a: Float) = (a * GOO_ALPHA_GAIN + GOO_ALPHA_OFFSET).coerceIn(0f, 255f)
        assertEquals(127.5f, out(127.5f), 0.01f)
        assertEquals(0f, out(100f), 0f)
        assertEquals(255f, out(150f), 0f)
    }

    @Test
    fun blurFollowsTheCircleHeightWithinLimits() {
        assertEquals(7.92f, gooeyBlurDp(36f), 0.01f)
        assertEquals(3f, gooeyBlurDp(4f), 0f)
        assertEquals(14f, gooeyBlurDp(200f), 0f)
    }

    @Test
    fun shapesPutThePillOnTheSideTheBubbleMergesToward() {
        // Box 100 x 40, pill 300 x 40, near edge 20 px from the bubble's centre, pill to the right.
        val right = gooeyShapes(100f, 40f, toward = 1f, bubbleScaleX = 1f, bubbleScaleY = 1f, edgePx = 20f, pillWidthPx = 300f, pillHeightPx = 40f)
        assertEquals(0f, right.bubble.left, 0f)
        assertEquals(100f, right.bubble.right, 0f)
        assertEquals(70f, right.pill.left, 0f)
        assertEquals(370f, right.pill.right, 0f)
        // Mirrored: pill to the left.
        val left = gooeyShapes(100f, 40f, toward = -1f, bubbleScaleX = 1f, bubbleScaleY = 1f, edgePx = 20f, pillWidthPx = 300f, pillHeightPx = 40f)
        assertEquals(30f, left.pill.right, 0f)
        assertEquals(-270f, left.pill.left, 0f)
    }

    @Test
    fun theBubbleScalesAboutItsCentre() {
        val s = gooeyShapes(100f, 40f, 1f, bubbleScaleX = 0.5f, bubbleScaleY = 0.5f, edgePx = 0f, pillWidthPx = 10f, pillHeightPx = 40f)
        assertEquals(25f, s.bubble.left, 0f)
        assertEquals(75f, s.bubble.right, 0f)
        assertEquals(10f, s.bubble.top, 0f)
        assertEquals(30f, s.bubble.bottom, 0f)
    }

    @Test
    fun thePillSwellsAboutItsOwnCentre() {
        val s = gooeyShapes(100f, 40f, 1f, 1f, 1f, edgePx = 20f, pillWidthPx = 200f, pillHeightPx = 40f, pillScaleX = 1.1f, pillScaleY = 1.5f)
        // Unscaled pill spans 70..270 (centre 170, middle line 20): scaled to 220 wide and 60 tall about that centre.
        assertEquals(60f, s.pill.left, 0.001f)
        assertEquals(280f, s.pill.right, 0.001f)
        assertEquals(170f, s.pill.centerX, 0.001f)
        assertEquals(60f, s.pill.height, 0.001f)
        assertEquals(20f, s.pill.centerY, 0.001f)
    }

    @Test
    fun theBridgeRunsFromTheBubbleCentreToInsideThePill() {
        val s = gooeyShapes(100f, 40f, 1f, 1f, 1f, edgePx = 20f, pillWidthPx = 200f, pillHeightPx = 40f, neckHeightPx = 22f, neckInsidePx = 10f)
        val neck = s.neck!!
        assertEquals(50f, neck.left, 0f)
        assertEquals(80f, neck.right, 0f)
        assertEquals(22f, neck.height, 0f)
        assertEquals(20f, neck.centerY, 0f)
        val left = gooeyShapes(100f, 40f, -1f, 1f, 1f, edgePx = 20f, pillWidthPx = 200f, pillHeightPx = 40f, neckHeightPx = 22f, neckInsidePx = 10f).neck!!
        assertEquals(20f, left.left, 0f)
        assertEquals(50f, left.right, 0f)
    }

    @Test
    fun noBridgeWhenItIsClosedOrHasNoLength() {
        assertEquals(null, gooeyShapes(100f, 40f, 1f, 1f, 1f, 20f, 200f, 40f, neckHeightPx = 0f, neckInsidePx = 10f).neck)
        // The pill's edge is behind the bubble's centre and the bridge reaches less far than that: nothing to draw.
        assertEquals(null, gooeyShapes(100f, 40f, 1f, 1f, 1f, -30f, 200f, 40f, neckHeightPx = 22f, neckInsidePx = 10f).neck)
    }

    @Test
    fun boundsCoverBothShapesPlusTheBlurMargin() {
        val s = gooeyShapes(100f, 40f, 1f, 1f, 1f, 20f, 300f, 60f)
        val b = s.bounds(marginPx = 10f)
        assertEquals(-10f, b.left, 0f)
        assertEquals(380f, b.right, 0f)
        assertEquals(-20f, b.top, 0f)
        assertEquals(60f, b.bottom, 0f)
    }

    @Test
    fun gooLayerIsReducedOnlyWhenTheBlurStaysSoftEnough() {
        assertEquals(1f, gooeyLayerScale(3f), 0f)
        assertEquals(1f, gooeyLayerScale(11.9f), 0f)
        assertEquals(0.5f, gooeyLayerScale(12f), 0f)
        assertEquals(0.5f, gooeyLayerScale(23.9f), 0f)
        assertEquals(0.25f, gooeyLayerScale(24f), 0f)
        // The blur actually applied to the reduced layer never drops below 6 px unless the layer is at full size.
        for (px in listOf(3f, 8f, 12f, 19f, 24f, 38f)) {
            val s = gooeyLayerScale(px)
            assertTrue(s == 1f || px * s >= 6f)
        }
    }
}
