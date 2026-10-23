/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {
    private fun argb(r: Int, g: Int, b: Int, a: Int = 255) = (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun fill(count: Int, colour: Int) = IntArray(count) { colour }

    @Test
    fun aRedPictureGivesAHueNearZero() {
        val tint = ArtworkPalette.dominantTint(fill(100, argb(220, 30, 40)))
        assertNotNull(tint)
        val hue = tint!!.hue
        assertTrue("hue was $hue", hue < 15f || hue > 345f)
        assertTrue(tint.saturation > 0.6f)
    }

    @Test
    fun theBiggerColourWins() {
        val pixels = fill(70, argb(30, 60, 220)) + fill(30, argb(220, 30, 40))
        val tint = ArtworkPalette.dominantTint(pixels)!!
        assertEquals(230f, tint.hue, 15f)
    }

    @Test
    fun aSmallPatchDoesNotBeatABigOne() {
        val pixels = fill(95, argb(40, 200, 60)) + fill(5, argb(255, 0, 0))
        assertEquals(125f, ArtworkPalette.dominantTint(pixels)!!.hue, 15f)
    }

    @Test
    fun greyBlackAndWhiteGiveNoTint() {
        val pixels = fill(40, argb(0, 0, 0)) + fill(40, argb(255, 255, 255)) + fill(40, argb(128, 128, 128))
        assertNull(ArtworkPalette.dominantTint(pixels))
    }

    @Test
    fun nearBlackPixelsDoNotVote() {
        val pixels = fill(90, argb(10, 0, 0)) + fill(10, argb(30, 90, 220))
        // 10 % of the picture is blue, the rest is too dark to carry a colour: blue wins.
        assertEquals(220f, ArtworkPalette.dominantTint(pixels)!!.hue, 15f)
    }

    @Test
    fun transparentPixelsAreIgnored() {
        val pixels = fill(100, argb(255, 0, 0, a = 0))
        assertNull(ArtworkPalette.dominantTint(pixels))
    }

    @Test
    fun aHueAcrossTheWrapAroundStaysOneColour() {
        val pixels = fill(40, argb(230, 20, 60)) + fill(40, argb(230, 20, 10)) + fill(20, argb(30, 80, 220))
        val hue = ArtworkPalette.dominantTint(pixels)!!.hue
        assertTrue("hue was $hue", hue < 20f || hue > 340f)
    }

    @Test
    fun anEmptyPictureGivesNoTint() {
        assertNull(ArtworkPalette.dominantTint(IntArray(0)))
    }

    @Test
    fun aVividColourHasMoreChromaThanADarkOne() {
        val vivid = ArtworkPalette.dominantTint(fill(100, argb(120, 230, 40)))!!
        val dark = ArtworkPalette.dominantTint(fill(100, argb(70, 35, 25)))!!
        assertTrue("vivid ${vivid.chroma} dark ${dark.chroma}", vivid.chroma > dark.chroma + 20f)
        assertEquals(134f, vivid.labHue, 25f)
    }
}
