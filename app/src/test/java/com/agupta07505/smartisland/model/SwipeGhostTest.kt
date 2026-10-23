/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeGhostTest {
    @Test
    fun parksTheLayerDrawnLastWhenItHoldsTheSwipedCard() {
        assertEquals(0, SwipeGhost.layerToPark(0, arrayOf("rec", "rec"), "rec"))
        assertEquals(1, SwipeGhost.layerToPark(1, arrayOf("rec", "rec"), "rec"))
    }

    @Test
    fun usesTheOtherLayerWhenTheLastFrameAlreadyShowedTheNewCard() {
        // The bug: layer 1 (drawn last) holds the media card; the Recording card is in layer 0.
        assertEquals(0, SwipeGhost.layerToPark(1, arrayOf("rec", "media"), "rec"))
        assertEquals(1, SwipeGhost.layerToPark(0, arrayOf("media", "rec"), "rec"))
    }

    @Test
    fun parksNothingWhenNoLayerHoldsTheSwipedCard() {
        assertEquals(-1, SwipeGhost.layerToPark(0, arrayOf("media", "media"), "rec"))
        assertEquals(-1, SwipeGhost.layerToPark(0, arrayOf(null, null), "rec"))
        assertEquals(-1, SwipeGhost.layerToPark(-1, arrayOf("rec", "rec"), "rec"))
        assertEquals(-1, SwipeGhost.layerToPark(0, arrayOf("rec", "rec"), null))
    }
}
