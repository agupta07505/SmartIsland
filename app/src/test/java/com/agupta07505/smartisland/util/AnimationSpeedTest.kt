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

class AnimationSpeedTest {
    @Test
    fun normalKeepsTheOldNumbers() {
        assertEquals(400f, AnimationSpeed.NORMAL.stiffness(400f), 0.0001f)
        assertEquals(220, AnimationSpeed.NORMAL.durationMs(220))
    }

    @Test
    fun snappyIsStifferAndShorterRelaxedIsTheOpposite() {
        assertTrue(AnimationSpeed.SNAPPY.stiffness(400f) > 400f)
        assertTrue(AnimationSpeed.SNAPPY.durationMs(220) < 220)
        assertTrue(AnimationSpeed.RELAXED.stiffness(400f) < 400f)
        assertTrue(AnimationSpeed.RELAXED.durationMs(220) > 220)
    }

    @Test
    fun durationNeverDropsToZero() {
        assertEquals(1, AnimationSpeed.SNAPPY.durationMs(0))
    }

    @Test
    fun unknownIdsFallBackToNormal() {
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromId(null))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromId("turbo"))
        assertEquals(AnimationSpeed.SNAPPY, AnimationSpeed.fromId("snappy"))
        assertEquals(AnimationSpeed.RELAXED, AnimationSpeed.fromId("relaxed"))
    }
}
