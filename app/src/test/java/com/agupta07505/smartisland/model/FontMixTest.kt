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

class FontMixTest {

    @Test
    fun theThreeSlotsCoverEveryWeightExactlyOnce() {
        val all = FontSlot.entries.flatMap { it.weights }
        assertEquals((1..9).map { it * 100 }, all.sorted())
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun slotForAgreesWithTheWeightsEachSlotRegisters() {
        FontSlot.entries.forEach { slot ->
            slot.weights.forEach { w -> assertEquals("weight $w", slot, FontMix.slotFor(w)) }
        }
    }

    @Test
    fun theIslandsOwnWeightsLandOnTheRightSlot() {
        // Weights the island text actually asks for (FontWeight.Normal / Medium / SemiBold / Bold / ExtraBold).
        assertEquals(FontSlot.Light, FontMix.slotFor(400))
        assertEquals(FontSlot.Regular, FontMix.slotFor(500))
        assertEquals(FontSlot.Regular, FontMix.slotFor(600))
        assertEquals(FontSlot.Medium, FontMix.slotFor(700))
        assertEquals(FontSlot.Medium, FontMix.slotFor(800))
    }

    @Test
    fun oddWeightsFallIntoTheNearestBand() {
        assertEquals(FontSlot.Light, FontMix.slotFor(1))
        assertEquals(FontSlot.Light, FontMix.slotFor(350))
        assertEquals(FontSlot.Regular, FontMix.slotFor(450))
        assertEquals(FontSlot.Medium, FontMix.slotFor(650))
        assertEquals(FontSlot.Medium, FontMix.slotFor(1000))
    }

    @Test
    fun representativeWeightBelongsToItsOwnSlot() {
        FontSlot.entries.forEach { slot ->
            assertTrue(slot.representativeWeight in slot.weights)
        }
    }

    @Test
    fun anEmptySlotUsesTheIslandFont() {
        assertEquals("lora", FontMix.effectiveId("", "lora"))
        assertEquals("poppins", FontMix.effectiveId("poppins", "lora"))
        assertEquals("custom:SF Pro Display Light.otf", FontMix.effectiveId("custom:SF Pro Display Light.otf", "system"))
    }

    @Test
    fun idOfReadsTheSlotsOwnField() {
        assertEquals("a", FontMix.idOf(FontSlot.Light, "a", "b", "c"))
        assertEquals("b", FontMix.idOf(FontSlot.Regular, "a", "b", "c"))
        assertEquals("c", FontMix.idOf(FontSlot.Medium, "a", "b", "c"))
    }

    @Test
    fun theMixNeedsTheSwitchAndAFontOfItsOwn() {
        assertFalse(FontMix.isActive(enabled = false, light = "poppins", regular = "", medium = ""))
        assertFalse(FontMix.isActive(enabled = true, light = "", regular = "", medium = ""))
        assertTrue(FontMix.isActive(enabled = true, light = "", regular = "", medium = "lora"))
        assertTrue(FontMix.isActive(enabled = true, light = "poppins", regular = "lora", medium = "inter_display"))
    }
}
