/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeFadeTest {
    @Test
    fun fullyOpaqueInTheMiddleAndAtTheLowestAtTheLimit() {
        assertEquals(1f, SwipeFade.alpha(0f, 100f, SwipeFade.CARD_MIN_ALPHA), 1e-6f)
        assertEquals(SwipeFade.CARD_MIN_ALPHA, SwipeFade.alpha(100f, 100f, SwipeFade.CARD_MIN_ALPHA), 1e-6f)
        assertEquals(SwipeFade.CARD_MIN_ALPHA, SwipeFade.alpha(900f, 100f, SwipeFade.CARD_MIN_ALPHA), 1e-6f)
    }

    @Test
    fun sameFadeOnBothSidesAndFallingWithDistance() {
        assertEquals(
            SwipeFade.alpha(40f, 100f, SwipeFade.CARD_MIN_ALPHA),
            SwipeFade.alpha(-40f, 100f, SwipeFade.CARD_MIN_ALPHA), 1e-6f
        )
        assertTrue(SwipeFade.alpha(30f, 100f, 0.3f) > SwipeFade.alpha(60f, 100f, 0.3f))
    }

    @Test
    fun noLimitMeansNoFade() {
        assertEquals(1f, SwipeFade.alpha(50f, 0f, 0.3f), 0f)
    }
}
