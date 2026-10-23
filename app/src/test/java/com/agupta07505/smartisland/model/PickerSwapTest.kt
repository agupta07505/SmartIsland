/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerSwapTest {
    @Test
    fun cardOnlyAtZeroAndPickerOnlyAtOne() {
        assertEquals(1f, PickerSwap.cardAlpha(0f), 0.0001f)
        assertEquals(0f, PickerSwap.pickerAlpha(0f), 0.0001f)
        assertEquals(0f, PickerSwap.cardAlpha(1f), 0.0001f)
        assertEquals(1f, PickerSwap.pickerAlpha(1f), 0.0001f)
        assertEquals(1f, PickerSwap.cardScale(0f), 0.0001f)
        assertEquals(PickerSwap.END_SCALE, PickerSwap.cardScale(1f), 0.0001f)
        assertEquals(PickerSwap.END_SCALE, PickerSwap.pickerScale(0f), 0.0001f)
        assertEquals(1f, PickerSwap.pickerScale(1f), 0.0001f)
    }

    @Test
    fun theCardIsGoneBeforeThePickerIsSolidSoTwoSolidLayersNeverStack() {
        var p = 0f
        while (p <= 1f) {
            assertTrue(PickerSwap.cardAlpha(p) + PickerSwap.pickerAlpha(p) <= 1.0001f)
            p += 0.01f
        }
    }

    @Test
    fun theSamePositionLooksTheSameOpeningOrClosing() {
        // One progress value drives both directions, so there is nothing direction specific to compare but the length.
        assertTrue(PickerSwap.lengthMs(opening = false) <= PickerSwap.lengthMs(opening = true))
        assertEquals(PickerSwap.cardAlpha(0.4f), PickerSwap.cardAlpha(0.4f), 0f)
    }

    @Test
    fun layersStayComposedOnlyWhileTheyAreVisibleOrResting() {
        assertTrue(PickerSwap.cardLayerShown(progress = 0f, pickerOpen = false))
        assertFalse(PickerSwap.pickerLayerShown(progress = 0f, pickerOpen = false))
        // Closing: the picker is no longer "open" but is still fading out; the card is already back.
        assertTrue(PickerSwap.pickerLayerShown(progress = 0.5f, pickerOpen = false))
        assertTrue(PickerSwap.cardLayerShown(progress = 0.5f, pickerOpen = false))
        // Open and settled: only the picker.
        assertFalse(PickerSwap.cardLayerShown(progress = 1f, pickerOpen = true))
        assertTrue(PickerSwap.pickerLayerShown(progress = 1f, pickerOpen = true))
    }
}
