/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PillContentSlideTest {
    @Test
    fun slideRoomIsAShareOfThePillWithinLimits() {
        assertEquals(44.8f, pillContentMaxSlideDp(112f), 0.01f)
        assertEquals(56f, pillContentMaxSlideDp(400f), 0f)
        assertEquals(8f, pillContentMaxSlideDp(10f), 0f)
    }
}
