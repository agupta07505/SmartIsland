/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionMathTest {
    @Test
    fun smoothstepEndpoints() {
        assertEquals(0f, smoothstep(0f), 0f)
        assertEquals(0.5f, smoothstep(0.5f), 1e-6f)
        assertEquals(1f, smoothstep(1f), 0f)
        assertEquals(0f, smoothstep(-3f), 0f)
        assertEquals(1f, smoothstep(7f), 0f)
    }


    @Test fun lerpBlendsAndDoesNotClamp() {
        assertEquals(10f, lerpFloat(10f, 20f, 0f), 0f)
        assertEquals(15f, lerpFloat(10f, 20f, 0.5f), 1e-6f)
        assertEquals(30f, lerpFloat(10f, 20f, 2f), 1e-6f)
    }

    @Test
    fun rampClampsAndEmptyRange() {
        assertEquals(0f, ramp(0.1f, 0.2f, 0.6f), 0f)
        assertEquals(0.5f, ramp(0.4f, 0.2f, 0.6f), 1e-6f)
        assertEquals(1f, ramp(0.9f, 0.2f, 0.6f), 0f)
        assertEquals(0f, ramp(0.4f, 0.5f, 0.5f), 0f)
        assertEquals(1f, ramp(0.5f, 0.5f, 0.5f), 0f)
    }

}
