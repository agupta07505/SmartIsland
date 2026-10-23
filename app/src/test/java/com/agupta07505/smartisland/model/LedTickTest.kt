/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LedTickTest {
    @Test
    fun patternKeepsTheNumbersFromOneToNineInOrder() {
        assertEquals(listOf(1, 2, 3, 2, 5), LedTick.parsePattern("1, 2, 3, 2, 5"))
        assertEquals(listOf(4, 7), LedTick.parsePattern("0 12 4;x7"))
        assertEquals("3, 4", LedTick.normalizePattern("3 4"))
    }

    @Test
    fun anEmptyOrUnusablePatternFallsBackToTheDefault() {
        val default = listOf(4, 5, 4)
        assertEquals(default, LedTick.parsePattern("abc"))
        assertEquals(default, LedTick.parsePattern(""))
        assertEquals(default, LedTick.parsePattern(null))
        assertEquals(default, LedTick.parsePattern("10, 0, 99"))
    }

    @Test
    fun patternIsCappedAtTwelveEntries() {
        assertEquals(LedTick.MAX_PATTERN_LENGTH, LedTick.parsePattern("1,1,1,1,1,1,1,1,1,1,1,1,1,1").size)
    }

    @Test
    fun paceIsKeptInRangeAndOnTheSliderStep() {
        assertEquals(LedTick.MIN_PACE_MS, LedTick.normalizePace(0))
        assertEquals(LedTick.MAX_PACE_MS, LedTick.normalizePace(9_999))
        assertEquals(420, LedTick.normalizePace(420))
        assertEquals(0, (LedTick.normalizePace(437) - LedTick.MIN_PACE_MS) % LedTick.PACE_STEP_MS)
    }

    @Test
    fun theBurstLengthFollowsThePatternAndRepeats() {
        val pattern = listOf(1, 2, 3, 2, 5)
        assertEquals(listOf(1, 2, 3, 2, 5, 1, 2, 3, 2, 5), (0L..9L).map { LedTick.hopsAt(pattern, it) })
        assertEquals(5, LedTick.hopsAt(pattern, -1L))
    }

    @Test
    fun aBurstStartsAtZeroEndsAtOneAndNeverGoesBack() {
        for (hops in 1..9) {
            val pace = 420
            val total = LedTick.burstMs(hops, pace).toFloat()
            assertEquals(0f, LedTick.progress(0f, hops, pace), 0f)
            assertEquals(1f, LedTick.progress(total, hops, pace), 0f)
            var previous = 0f
            var t = 0f
            while (t <= total + 50f) {
                val now = LedTick.progress(t, hops, pace)
                assertTrue("hops=$hops t=$t", now >= previous - 1e-6f)
                previous = now
                t += 4f
            }
        }
    }

    @Test
    fun aBurstRestsBetweenItsHops() {
        val hops = 4
        val pace = 420
        for (i in 0 until hops - 1) {
            val resting = i * pace + LedTick.HOP_MS + 10f
            assertEquals((i + 1).toFloat() / hops, LedTick.progress(resting, hops, pace), 1e-5f)
        }
    }

    @Test
    fun rowOffsetIsWholeLedRows() {
        assertEquals(0, LedTick.rowOffset(0f))
        assertEquals(LedTick.ROWS, LedTick.rowOffset(1f))
        assertEquals(5, LedTick.rowOffset(0.5f))
    }

    @Test
    fun slotsThatStartTogetherShareABeat() {
        assertEquals(LedTick.beatIndex(1_000L, 2_500L), LedTick.beatIndex(1_200L, 2_500L))
        assertFalse(LedTick.beatIndex(0L, 2_500L) == LedTick.beatIndex(2_500L, 2_500L))
    }

    @Test
    fun theValueSlotWaitsHalfAPaceWhenTheStaggerIsOn() {
        assertEquals(210L, LedTick.staggerMs(420, right = true, stagger = true))
        assertEquals(0L, LedTick.staggerMs(420, right = false, stagger = true))
        assertEquals(0L, LedTick.staggerMs(420, right = true, stagger = false))
        assertEquals(80L, LedTick.staggerMs(160, right = true, stagger = true))
    }

    @Test
    fun theStaggerIsOnByDefault() {
        assertTrue(LedTickConfig().stagger)
    }

    @Test
    fun unknownStylesAndColoursFallBack() {
        assertEquals(LedTick.STYLE_SMOOTH, LedTick.normalizeStyle(null))
        assertEquals(LedTick.STYLE_SMOOTH, LedTick.normalizeStyle("blink"))
        assertEquals(LedTick.STYLE_LED, LedTick.normalizeStyle("led"))
        assertEquals(LedTick.COLOR_RED, LedTick.normalizeColor("purple"))
        assertEquals(LedTick.COLOR_AMBER, LedTick.normalizeColor("amber"))
    }

    @Test
    fun builtInFontDrawsFiveBySevenLettersWithOneColumnBetween() {
        assertEquals(29, LedFont.text("12:45").cols)
        assertEquals(LedFont.GLYPH_ROWS, LedFont.text("12:45").rows)
        assertTrue(LedFont.text("a").bits.contentEquals(LedFont.text("A").bits))
        assertTrue(LedFont.text("^").bits.none { it })
        for (c in " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:-.%/+°~") {
            val glyph = LedFont.text(c.toString())
            assertEquals("glyph $c", 5, glyph.cols)
            assertEquals("glyph $c", 7, glyph.rows)
        }
    }

    @Test
    fun everyDotIconIsSquareAndNotBlank() {
        val weather = setOf(LedIcon.Sun, LedIcon.Moon, LedIcon.Cloud, LedIcon.Rain, LedIcon.Snow, LedIcon.Bolt)
        for (icon in LedIcon.values()) {
            val grid = LedFont.icon(icon)
            val size = if (icon in weather) LedFont.WEATHER_SIZE else 7
            assertEquals("$icon", size, grid.cols)
            assertEquals("$icon", size, grid.rows)
            assertTrue("$icon", grid.bits.any { it })
        }
    }

    @Test
    fun weatherIconsFillTheBoardHeight() {
        assertEquals(LedTick.ROWS, LedFont.WEATHER_SIZE)
        // Each weather icon lights its top and bottom row somewhere, so it uses the whole height of the board.
        for (icon in listOf(LedIcon.Sun, LedIcon.Moon, LedIcon.Rain, LedIcon.Snow, LedIcon.Bolt)) {
            val g = LedFont.icon(icon)
            assertTrue("$icon top", (0 until g.cols).any { g.bits[it] })
            assertTrue("$icon bottom", (0 until g.cols).any { g.bits[(g.rows - 1) * g.cols + it] })
        }
    }

    @Test
    fun aBurstFrameStartsOnTheOldFaceAndEndsOnTheNewOne() {
        val from = LedFont.text("12:45")
        val to = LedFont.text("12:46")
        val cols = 29
        assertTrue(LedFrames.frame(from, to, false, 0, cols).contentEquals(LedFrames.place(from, cols, LedTick.ROWS, 0)))
        assertTrue(LedFrames.frame(from, to, false, LedTick.ROWS, cols).contentEquals(LedFrames.place(to, cols, LedTick.ROWS, 0)))
        assertTrue(LedFrames.frame(from, to, true, LedTick.ROWS, cols).contentEquals(LedFrames.place(to, cols, LedTick.ROWS, 0)))
    }

    @Test
    fun theSameFaceStillMovesMidBurst() {
        val face = LedFont.text("12:45")
        val cols = 29
        val rest = LedFrames.place(face, cols, LedTick.ROWS, 0)
        val mid = LedFrames.frame(face, face, false, 4, cols)
        assertTrue(mid.any { it })
        assertFalse(mid.contentEquals(rest))
    }

    @Test
    fun aNarrowFaceIsCentredOnAWideBoard() {
        val board = LedFrames.place(LedFont.icon(LedIcon.Sun), 29, LedTick.ROWS, 0)
        // The Sun's top ray is column 4 of the 9 by 9 icon; the icon starts at column 10 and row 0.
        assertTrue(board[0 * 29 + 10 + 4])
    }

    @Test
    fun theGlowFadesOutAfterTheLedGoesDark() {
        val cols = 29
        val lit = LedFrames.place(LedFont.text("12:45"), cols, LedTick.ROWS, 0)
        val dark = BooleanArray(cols * LedTick.ROWS)
        val glow = LedGlow(cols)
        glow.settle(lit)
        assertFalse(glow.fading(lit))
        glow.step(dark, 16f)
        assertTrue(glow.fading(dark))
        repeat(40) { glow.step(dark, 16f) }
        assertFalse(glow.fading(dark))
    }

    @Test
    fun theLedSettingsSurviveABackupAndComeBackCleaned() {
        val original = SmartIslandSettings.Default.copy(
            idleTickStyle = LedTick.STYLE_LED,
            ledTickPattern = "3, 1, 4",
            ledTickPaceMs = 300,
            ledTickFont = "monospace",
            ledTickColor = LedTick.COLOR_AMBER,
            ledTickStagger = false
        )
        assertEquals(original, SmartIslandSettings.fromJson(original.toJson(appVersion = "7.0.0")))

        val messy = original.copy(ledTickPattern = "9 and 12, 0", ledTickPaceMs = 99_999, ledTickColor = "purple", idleTickStyle = "blink")
        val restored = SmartIslandSettings.fromJson(messy.toJson(appVersion = "7.0.0"))
        assertEquals("9", restored.ledTickPattern)
        assertEquals(LedTick.MAX_PACE_MS, restored.ledTickPaceMs)
        assertEquals(LedTick.COLOR_RED, restored.ledTickColor)
        assertEquals(LedTick.STYLE_SMOOTH, restored.idleTickStyle)
    }

    @Test
    fun theDefaultsKeepTheOriginalSmoothTick() {
        val defaults = SmartIslandSettings.Default
        assertEquals(LedTick.STYLE_SMOOTH, defaults.idleTickStyle)
        assertEquals("4, 5, 4", defaults.ledTickPattern)
        assertEquals(LedTick.DEFAULT_PACE_MS, defaults.ledTickPaceMs)
        assertEquals(LedTick.FONT_BUILTIN, defaults.ledTickFont)
        assertEquals(LedTick.COLOR_RED, defaults.ledTickColor)
        assertTrue(defaults.ledTickStagger)
    }

    @Test
    fun blendCrossFadesTwoFramesWithoutMovingAnything() {
        val glow = LedGlow(cols = 2, rows = 2)
        val from = booleanArrayOf(true, true, false, false)
        val to = booleanArrayOf(true, false, false, true)
        glow.blend(from, to, 0f)
        assertEquals(listOf(1f, 1f, 0f, 0f), glow.level.toList())
        glow.blend(from, to, 1f)
        assertEquals(listOf(1f, 0f, 0f, 1f), glow.level.toList())
        glow.blend(from, to, 0.5f)
        // A dot lit in both stays lit, a dot lit in neither stays dark, the others are half way.
        assertEquals(listOf(1f, 0.5f, 0f, 0.5f), glow.level.toList())
        // Out-of-range mix is clamped, and a short frame counts as dark.
        glow.blend(booleanArrayOf(true), booleanArrayOf(), 3f)
        assertEquals(listOf(0f, 0f, 0f, 0f), glow.level.toList())
    }
}
