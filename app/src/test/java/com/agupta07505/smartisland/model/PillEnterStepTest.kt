/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PillEnterStepTest {

    @Test
    fun aWaitingSwipeStartsTheSlide() {
        assertEquals(PillEnterStep.Start, pillEnterStep(pendingEnterSign = 1, enterOffsetPx = 0f))
        assertEquals(PillEnterStep.Start, pillEnterStep(pendingEnterSign = -1, enterOffsetPx = 0f))
    }

    @Test
    fun aNewSwipeWinsOverALeftOverOffset() {
        // The slide snaps to its own start, so the old offset does not matter.
        assertEquals(PillEnterStep.Start, pillEnterStep(pendingEnterSign = 1, enterOffsetPx = -44f))
    }

    @Test
    fun aCutOffSlideIsSettledBackToZero() {
        // The reported glitch: the offset stayed at about -16 dp (-44 px) and the icon sat half outside the pill.
        assertEquals(PillEnterStep.SettleStale, pillEnterStep(pendingEnterSign = 0, enterOffsetPx = -44f))
        assertEquals(PillEnterStep.SettleStale, pillEnterStep(pendingEnterSign = 0, enterOffsetPx = 12f))
    }

    @Test
    fun anOffsetAtRestNeedsNothing() {
        assertEquals(PillEnterStep.Nothing, pillEnterStep(0, 0f))
        assertEquals(PillEnterStep.Nothing, pillEnterStep(0, PILL_ENTER_REST_PX))
        assertEquals(PillEnterStep.Nothing, pillEnterStep(0, -0.2f))
    }
}
