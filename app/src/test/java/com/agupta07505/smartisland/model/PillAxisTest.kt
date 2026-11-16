/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 * Modified for Static Boat (2026)
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PillAxisTest {

    @Test
    fun horizontalIsTheIdentity() {
        assertEquals(3f to 4f, PillAxis.Horizontal.toPillFrame(3f, 4f))
    }

    @Test
    fun leftCameraMovingTowardTheEdgeIsNegativeAway() {
        assertTrue(PillAxis.VerticalLeft.toPillFrame(-10f, 0f).second < 0f)
    }

    @Test
    fun rightCameraMovingTowardTheEdgeIsNegativeAway() {
        assertTrue(PillAxis.VerticalRight.toPillFrame(10f, 0f).second < 0f)
    }

    @Test
    fun movingDownTheEdgeIsPositiveAlong() {
        assertTrue(PillAxis.VerticalLeft.toPillFrame(0f, 10f).first > 0f)
        assertTrue(PillAxis.VerticalRight.toPillFrame(0f, 10f).first > 0f)
    }

    @Test
    fun ofPicksAxisFromOrientationAndCameraSide() {
        assertEquals(PillAxis.Horizontal, PillAxis.of(vertical = false, cameraOnRight = true))
        assertEquals(PillAxis.VerticalRight, PillAxis.of(vertical = true, cameraOnRight = true))
        assertEquals(PillAxis.VerticalLeft, PillAxis.of(vertical = true, cameraOnRight = false))
    }
}
