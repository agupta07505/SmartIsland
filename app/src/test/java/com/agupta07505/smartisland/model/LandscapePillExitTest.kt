/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapePillExitTest {
    private val screen = 800f

    @Test
    fun middleOfTheScreenSlidesUp() {
        assertEquals(PillExitDirection.Up, pillExitDirection(0f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(-120f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(150f, screen))
    }

    @Test
    fun nearLeftEdgeSlidesLeft_nearRightEdgeSlidesRight() {
        assertEquals(PillExitDirection.Left, pillExitDirection(-360f, screen))
        assertEquals(PillExitDirection.Right, pillExitDirection(360f, screen))
    }

    @Test
    fun edgeZoneIsAQuarterOfTheScreen() {
        // Centre 200 dp from the left edge: exactly on the border, still counts as near.
        assertEquals(PillExitDirection.Left, pillExitDirection(-200f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(-199f, screen))
        assertEquals(PillExitDirection.Right, pillExitDirection(200f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(199f, screen))
    }

    @Test
    fun unknownScreenWidthFallsBackToUp() {
        assertEquals(PillExitDirection.Up, pillExitDirection(-300f, 0f))
    }

    @Test
    fun travelStopsPastTheEdgeButNeverExceedsTheCap() {
        // Pill 24 wide, centre 20 dp from the left edge: 20 + 12 + 8 margin = 40.
        val left = pillExitTravelDp(PillExitDirection.Left, -380f, 24f, 60f, 10f, screen)
        assertEquals(40f, left, 0.001f)
        val right = pillExitTravelDp(PillExitDirection.Right, 380f, 24f, 60f, 10f, screen)
        assertEquals(40f, right, 0.001f)
        val up = pillExitTravelDp(PillExitDirection.Up, 0f, 120f, 36f, 0f, screen)
        assertEquals(44f, up, 0.001f)
        val tall = pillExitTravelDp(PillExitDirection.Up, 0f, 120f, 36f, 300f, screen)
        assertEquals(PILL_EXIT_MAX_TRAVEL_DP, tall, 0.001f)
    }

    @Test
    fun offsetFollowsTheDirection() {
        assertEquals(PillExitOffset(0f, -50f), pillExitOffsetDp(PillExitDirection.Up, 100f, 0.5f))
        assertEquals(PillExitOffset(-50f, 0f), pillExitOffsetDp(PillExitDirection.Left, 100f, 0.5f))
        assertEquals(PillExitOffset(50f, 0f), pillExitOffsetDp(PillExitDirection.Right, 100f, 0.5f))
        assertEquals(PillExitOffset(0f, 0f).dy, pillExitOffsetDp(PillExitDirection.Up, 100f, 0f).dy + 0f, 0.0001f)
    }

    @Test
    fun progressIsClamped() {
        assertEquals(-100f, pillExitOffsetDp(PillExitDirection.Left, 100f, 3f).dx, 0.001f)
        assertEquals(0f, pillExitOffsetDp(PillExitDirection.Right, 100f, -1f).dx, 0.001f)
    }

    @Test
    fun leavingPillIsSolidThenFades() {
        assertEquals(1f, pillExitAlpha(0f), 0.0001f)
        assertEquals(1f, pillExitAlpha(0.3f), 0.0001f)
        assertTrue(pillExitAlpha(0.65f) in 0.01f..0.99f)
        assertEquals(0f, pillExitAlpha(1f), 0.0001f)
    }
}
