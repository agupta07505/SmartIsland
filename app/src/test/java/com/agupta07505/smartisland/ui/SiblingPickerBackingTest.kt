/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.expanded.siblingPickerBackingAlpha
import org.junit.Assert.assertEquals
import org.junit.Test

class SiblingPickerBackingTest {
    private fun combined(surface: Float, backing: Float) = 1f - (1f - surface) * (1f - backing)

    @Test
    fun halfOpaqueSurfaceIsLiftedToTheMinimum() {
        val backing = siblingPickerBackingAlpha(0.5f)
        assertEquals(0.7f, backing, 0.001f)
        assertEquals(0.85f, combined(0.5f, backing), 0.001f)
    }

    @Test
    fun veryTransparentSurfaceIsLiftedToo() {
        assertEquals(0.85f, combined(0.2f, siblingPickerBackingAlpha(0.2f)), 0.001f)
    }

    @Test
    fun opaqueEnoughSurfaceNeedsNoBacking() {
        assertEquals(0f, siblingPickerBackingAlpha(0.85f), 0.0001f)
        assertEquals(0f, siblingPickerBackingAlpha(1f), 0.0001f)
    }
}
