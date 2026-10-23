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

class LabColorTest {
    private fun hex(rgb: FloatArray): String =
        "#%02x%02x%02x".format(Math.round(rgb[0] * 255f), Math.round(rgb[1] * 255f), Math.round(rgb[2] * 255f))

    private fun assertClose(expected: String, actual: FloatArray, tol: Int = 2) {
        for (i in 0..2) {
            val e = expected.substring(1 + 2 * i, 3 + 2 * i).toInt(16)
            val a = Math.round(actual[i] * 255f)
            assertTrue("$expected vs ${hex(actual)}", kotlin.math.abs(e - a) <= tol)
        }
    }

    @Test
    fun reproducesTheShadesOwnContainerColours() {
        // Reference values from the notification shade: (chroma, hue) of the container at lightness 90.
        assertClose("#c2e8ff", LabColor.lchToSrgb(90.0, 16.9, 244.0))
        assertClose("#a0f880", LabColor.lchToSrgb(90.0, 68.6, 134.0))
        assertClose("#dae5e1", LabColor.lchToSrgb(90.0, 4.5, 171.0))
    }

    @Test
    fun theLightnessIsTheRequestedToneEvenWhenTheChromaIsTooHighToShow() {
        for (hue in listOf(0.0, 60.0, 134.0, 200.0, 280.0)) {
            val rgb = LabColor.lchToSrgb(90.0, 130.0, hue)
            assertEquals("hue $hue", 90.0, LabColor.lightnessOf(rgb), 1.5)
        }
    }

    @Test
    fun labRoundTripsAColour() {
        val lab = LabColor.fromSrgb(0xA0, 0xF8, 0x80)
        val rgb = LabColor.lchToSrgb(lab[0], LabColor.chromaOf(lab[1], lab[2]), LabColor.hueOf(lab[1], lab[2]))
        assertClose("#a0f880", rgb, 1)
    }

    @Test
    fun greyHasNoChroma() {
        val lab = LabColor.fromSrgb(128, 128, 128)
        assertTrue(LabColor.chromaOf(lab[1], lab[2]) < 0.5)
    }
}
