/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotModelTest {
    @Test
    fun everyPaletteIsVisibleAgainstTheBlackIsland() {
        // Non-text graphics need at least 3:1 (HIG accessibility).
        for (palette in MascotPalette.entries) {
            val light = MascotColor.contrast(palette.light, MascotColor.IslandBlack)
            val shade = MascotColor.contrast(palette.shade, MascotColor.IslandBlack)
            assertTrue("${palette.id} light $light", light >= 3.0)
            assertTrue("${palette.id} shade $shade", shade >= 3.0)
        }
    }

    @Test
    fun inkEyesReadOnEveryBodyColor() {
        for (palette in MascotPalette.entries) {
            for (body in listOf(palette.light, palette.shade)) {
                val ratio = MascotColor.contrast(MascotColor.Ink, body)
                assertTrue("${palette.id} ink on body $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun contrastMatchesKnownValues() {
        val white = MascotColor(1f, 1f, 1f)
        val black = MascotColor(0f, 0f, 0f)
        assertEquals(21.0, MascotColor.contrast(white, black), 0.01)
        assertEquals(1.0, MascotColor.contrast(white, white), 0.0001)
    }

    @Test
    fun argbPacksChannels() {
        assertEquals(0xFFFF0000L, MascotColor(1f, 0f, 0f).toArgb())
        assertEquals(0x80000000L, MascotColor(0f, 0f, 0f, 0.5f).toArgb())
    }

    @Test
    fun idsRoundTripAndInvalidFallsBack() {
        for (s in MascotStyle.entries) assertEquals(s, MascotStyle.fromId(s.id))
        for (s in MascotShape.entries) assertEquals(s, MascotShape.fromId(s.id))
        for (p in MascotPalette.entries) assertEquals(p, MascotPalette.fromId(p.id))
        for (s in MascotSide.entries) assertEquals(s, MascotSide.fromId(s.id))
        for (f in MascotVisitFrequency.entries) assertEquals(f, MascotVisitFrequency.fromId(f.id))
        assertEquals(MascotStyle.Minimal, MascotStyle.fromId("nope"))
        assertEquals(MascotShape.Ball, MascotShape.fromId(null))
        assertEquals(MascotPalette.Pearl, MascotPalette.fromId("???"))
        assertEquals(MascotSide.Left, MascotSide.fromId(""))
        assertEquals(MascotVisitFrequency.Normal, MascotVisitFrequency.fromId("weekly"))
    }

    @Test
    fun visitDelaysAreOrderedAndOffHasNone() {
        assertEquals(null, MascotVisitFrequency.Off.delaySec)
        val rare = MascotVisitFrequency.Rare.delaySec!!
        val normal = MascotVisitFrequency.Normal.delaySec!!
        val frequent = MascotVisitFrequency.Frequent.delaySec!!
        assertTrue(rare.start > normal.endInclusive)
        assertTrue(normal.start > frequent.endInclusive)
    }

    @Test
    fun onlyOpenEyesBlink() {
        assertTrue(MascotEye.Open.blinks)
        assertTrue(MascotEye.Wide.blinks)
        assertTrue(MascotEye.Squint.blinks)
        assertFalse(MascotEye.Closed.blinks)
        assertFalse(MascotEye.Happy.blinks)
        assertFalse(MascotEye.Heart.blinks)
        assertFalse(MascotEye.Sleepy.blinks)
        assertFalse(MascotEye.Determined.blinks)
    }

    @Test
    fun moodExpressionsMatchTheReferenceTable() {
        assertEquals(MascotExpression(MascotEye.Open, MascotEye.Open), MascotMood.Idle.expression)
        assertEquals(MascotEye.Heart, MascotMood.Love.expression.left)
        assertEquals(0.14f, MascotMood.Confused.expression.tilt, 0f)
        assertEquals(MascotEye.Squint, MascotMood.Thinking.expression.left)
        // A wink blinks no more than its closed eye allows.
        assertFalse(MascotMood.Wink.expression.blinks)
        assertTrue(MascotMood.Idle.expression.blinks)
        assertEquals(MascotMood.entries.size, MascotMood.entries.map { it.expression }.size)
    }

    @Test
    fun faceLayoutKeepsEyesInsideTheBodyAndSymmetricAbout05() {
        for (style in MascotStyle.entries) for (shape in MascotShape.entries) {
            val face = MascotLook(style, shape, MascotPalette.Pearl).face
            assertEquals(1f, face.leftEye.x + face.rightEye.x, 0.001f)
            assertEquals(face.leftEye.y, face.rightEye.y, 0f)
            assertTrue(face.leftEye.x in 0.2f..0.5f)
            assertTrue(face.leftEye.y in 0.3f..0.8f)
        }
    }

    @Test
    fun reactionLengthsAreShortAndPositive() {
        for (r in MascotReaction.entries) assertTrue("${r.name} ${r.lengthSec}", r.lengthSec in 0.4f..2f)
        assertEquals(1.6f, MascotReaction.WakeUp.lengthSec, 0f)
    }
}
