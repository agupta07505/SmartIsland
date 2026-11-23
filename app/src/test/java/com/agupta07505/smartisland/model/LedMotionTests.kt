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

class LedMotionTest {
    private fun motion(
        widget: IdleWidgetKind,
        look: WeatherLook? = null,
        isDay: Boolean = true,
        wind: Float = -1f,
        fast: Boolean = false,
        soon: Boolean = false,
        off: Boolean = false
    ) = LedMotions.forGlyph(widget, look, isDay, wind, fast, soon, off)

    @Test
    fun weatherFrameChoice() {
        assertEquals(LedMotionKind.SunSpin, motion(IdleWidgetKind.Weather, WeatherLook.Clear, isDay = true).kind)
        assertEquals(LedMotionKind.None, motion(IdleWidgetKind.Weather, WeatherLook.Clear, isDay = false).kind)
        assertEquals(LedMotionKind.RainFall, motion(IdleWidgetKind.Weather, WeatherLook.Rain).kind)
        assertEquals(LedMotionKind.None, motion(IdleWidgetKind.Weather, WeatherLook.Snow).kind)
        assertEquals(LedMotionKind.WindSway, motion(IdleWidgetKind.Wind, wind = 30f).kind)
        assertEquals(LedMotionKind.None, motion(IdleWidgetKind.Wind, wind = 1f).kind)
        assertEquals(LedMotionKind.ArrowBob, motion(IdleWidgetKind.NetSpeed, fast = true).kind)
        assertEquals(LedMotionKind.None, motion(IdleWidgetKind.NetSpeed, fast = false).kind)
        assertEquals(LedMotionKind.AlarmGlow, motion(IdleWidgetKind.NextAlarm, soon = true).kind)
        assertEquals(LedMotionKind.None, motion(IdleWidgetKind.NextAlarm, soon = false).kind)

        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Rain, off = true).isRunning)
        assertFalse(motion(IdleWidgetKind.NetSpeed, fast = true, off = true).isRunning)
        assertFalse(motion(IdleWidgetKind.NextAlarm, soon = true, off = true).isRunning)

        val strong = motion(IdleWidgetKind.Wind, wind = 120f)
        assertTrue(strong.stepMs >= LedMotions.MIN_STEP_MS)
        assertTrue(motion(IdleWidgetKind.Wind, wind = 5f).stepMs > strong.stepMs)
    }

    @Test
    fun theLoopFollowsTheClock() {
        assertEquals(0, LedMotions.frameAt(0L, 300, 4))
        assertEquals(1, LedMotions.frameAt(300L, 300, 4))
        assertEquals(3, LedMotions.frameAt(1_199L, 300, 4))
        assertEquals(0, LedMotions.frameAt(1_200L, 300, 4))
        assertEquals(0, LedMotions.frameAt(9_999L, 0, 4))
        assertEquals(0, LedMotions.frameAt(9_999L, 300, 1))
    }

    private val all = listOf(
        LedIcon.Sun to LedMotionKind.SunSpin,
        LedIcon.Rain to LedMotionKind.RainFall,
        LedIcon.Wind to LedMotionKind.WindSway,
        LedIcon.ArrowUp to LedMotionKind.ArrowBob,
        LedIcon.ArrowDown to LedMotionKind.ArrowBob,
        LedIcon.Alarm to LedMotionKind.AlarmGlow
    )

    @Test
    fun frameZeroIsThePlainIconOnTheBoard() {
        for ((icon, kind) in all) {
            val frames = LedMotionFrames.frames(icon, kind)
            assertTrue("$icon has frames to play", frames.size >= 2)
            val plain = LedFont.icon(icon)
            assertEquals("$icon width", plain.cols, frames[0].cols)
            assertTrue(
                "$icon frame 0",
                LedFrames.place(frames[0], plain.cols, LedTick.ROWS, 0).contentEquals(LedFrames.place(plain, plain.cols, LedTick.ROWS, 0))
            )
        }
    }

    @Test
    fun noFrameChangesBoardWidth() {
        for ((icon, kind) in all) {
            val frames = LedMotionFrames.frames(icon, kind)
            val cols = LedFont.icon(icon).cols
            for (f in frames) assertEquals("$icon width", cols, f.cols)
            val first = LedFrames.place(frames[0], cols, LedTick.ROWS, 0)
            assertTrue("$icon moves", frames.drop(1).any { !LedFrames.place(it, cols, LedTick.ROWS, 0).contentEquals(first) })
        }
    }

    @Test
    fun oversizeMotionIsJustIcon() {
        assertEquals(1, LedMotionFrames.frames(LedIcon.Moon, LedMotionKind.SunSpin).size)
        assertEquals(1, LedMotionFrames.frames(LedIcon.Sun, LedMotionKind.None).size)
    }

    @Test
    fun rainFallsRowByRow() {
        val frames = LedMotionFrames.frames(LedIcon.Rain, LedMotionKind.RainFall)
        assertEquals(LedMotions.RAIN_FRAMES, frames.size)
        val plain = LedFont.icon(LedIcon.Rain)
        // the cloud above the streaks never moves
        for (f in frames) for (i in 0 until 5 * plain.cols) assertEquals(plain.bits[i], f.bits[i])
        // every streak row shows up in every frame, once
        val lit = frames.map { f -> f.bits.count { it } }
        assertTrue(lit.all { it == lit[0] })
    }

    @Test
    fun theSunKeepsItsBodyAndTurnsItsRays() {
        val frames = LedMotionFrames.frames(LedIcon.Sun, LedMotionKind.SunSpin)
        val a = frames[0]
        val b = frames[1]
        // the disc (the middle rows' inner dots) stays lit in both
        for (y in 2..6) for (x in 3..5) {
            assertTrue(a[x, y])
            assertTrue(b[x, y])
        }
        assertTrue(a[4, 0])
        assertFalse(b[4, 0])
        assertEquals(a.bits.count { it }, b.bits.count { it })
    }
}

class LedTickTest {
    @Test
    fun patternParsing() {
        assertEquals(listOf(1, 2, 3, 2, 5), LedTick.parsePattern("1, 2, 3, 2, 5"))
        assertEquals(listOf(4, 7), LedTick.parsePattern("0 12 4;x7"))
        assertEquals("3, 4", LedTick.normalizePattern("3 4"))

        val default = listOf(4, 5, 4)
        assertEquals(default, LedTick.parsePattern("abc"))
        assertEquals(default, LedTick.parsePattern(""))
        assertEquals(default, LedTick.parsePattern(null))
        assertEquals(default, LedTick.parsePattern("10, 0, 99"))

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
    fun burstLengthFollowsPattern() {
        val pattern = listOf(1, 2, 3, 2, 5)
        assertEquals(listOf(1, 2, 3, 2, 5, 1, 2, 3, 2, 5), (0L..9L).map { LedTick.hopsAt(pattern, it) })
        assertEquals(5, LedTick.hopsAt(pattern, -1L))
    }

    @Test
    fun burstRunsZeroToOne() {
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
    fun burstRestsBetweenItsHops() {
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
    fun valueSlotWaitsHalfPace() {
        assertEquals(210L, LedTick.staggerMs(420, right = true, stagger = true))
        assertEquals(0L, LedTick.staggerMs(420, right = false, stagger = true))
        assertEquals(0L, LedTick.staggerMs(420, right = true, stagger = false))
        assertEquals(80L, LedTick.staggerMs(160, right = true, stagger = true))
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
    fun builtinFontMetrics() {
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
    fun iconFrames() {
        val iconsWeather = setOf(LedIcon.Sun, LedIcon.Moon, LedIcon.Cloud, LedIcon.Rain, LedIcon.Snow, LedIcon.Bolt)
        for (icon in LedIcon.values()) {
            val grid = LedFont.icon(icon)
            val size = if (icon in iconsWeather) LedFont.WEATHER_SIZE else 7
            assertEquals("$icon", size, grid.cols)
            assertEquals("$icon", size, grid.rows)
            assertTrue("$icon", grid.bits.any { it })
        }

        assertEquals(LedTick.ROWS, LedFont.WEATHER_SIZE)
        // each weather icon lights its top and bottom row somewhere, so it uses the whole height of the board
        for (icon in listOf(LedIcon.Sun, LedIcon.Moon, LedIcon.Rain, LedIcon.Snow, LedIcon.Bolt)) {
            val g = LedFont.icon(icon)
            assertTrue("$icon top", (0 until g.cols).any { g.bits[it] })
            assertTrue("$icon bottom", (0 until g.cols).any { g.bits[(g.rows - 1) * g.cols + it] })
        }
    }

    @Test
    fun ledFramePlacement() {
        val from = LedFont.text("12:45")
        val to = LedFont.text("12:46")
        val endpointsCols = 29
        assertTrue(LedFrames.frame(from, to, false, 0, endpointsCols).contentEquals(LedFrames.place(from, endpointsCols, LedTick.ROWS, 0)))
        assertTrue(LedFrames.frame(from, to, false, LedTick.ROWS, endpointsCols).contentEquals(LedFrames.place(to, endpointsCols, LedTick.ROWS, 0)))
        assertTrue(LedFrames.frame(from, to, true, LedTick.ROWS, endpointsCols).contentEquals(LedFrames.place(to, endpointsCols, LedTick.ROWS, 0)))

        val face = LedFont.text("12:45")
        val repeatedCols = 29
        val rest = LedFrames.place(face, repeatedCols, LedTick.ROWS, 0)
        val mid = LedFrames.frame(face, face, false, 4, repeatedCols)
        assertTrue(mid.any { it })
        assertFalse(mid.contentEquals(rest))

        val board = LedFrames.place(LedFont.icon(LedIcon.Sun), 29, LedTick.ROWS, 0)
        // the Sun's top ray is column 4 of the 9 by 9 icon; the icon starts at column 10 and row 0
        assertTrue(board[0 * 29 + 10 + 4])

        val afterglowCols = 29
        val lit = LedFrames.place(LedFont.text("12:45"), afterglowCols, LedTick.ROWS, 0)
        val dark = BooleanArray(afterglowCols * LedTick.ROWS)
        val glow = LedGlow(afterglowCols)
        glow.settle(lit)
        assertFalse(glow.fading(lit))
        glow.step(dark, 16f)
        assertTrue(glow.fading(dark))
        repeat(40) { glow.step(dark, 16f) }
        assertFalse(glow.fading(dark))
    }

    @Test
    fun ledSettingsBackupCleaned() {
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
    fun defaultsKeepTheOriginalSmoothTick() {
        val defaults = SmartIslandSettings.Default
        assertEquals(LedTick.STYLE_SMOOTH, defaults.idleTickStyle)
        assertEquals("4, 5, 4", defaults.ledTickPattern)
        assertEquals(LedTick.DEFAULT_PACE_MS, defaults.ledTickPaceMs)
        assertEquals(LedTick.FONT_BUILTIN, defaults.ledTickFont)
        assertEquals(LedTick.COLOR_RED, defaults.ledTickColor)
        assertTrue(defaults.ledTickStagger)
    }

    @Test
    fun blendCrossFadesFrames() {
        val glow = LedGlow(cols = 2, rows = 2)
        val from = booleanArrayOf(true, true, false, false)
        val to = booleanArrayOf(true, false, false, true)
        glow.blend(from, to, 0f)
        assertEquals(listOf(1f, 1f, 0f, 0f), glow.level.toList())
        glow.blend(from, to, 1f)
        assertEquals(listOf(1f, 0f, 0f, 1f), glow.level.toList())
        glow.blend(from, to, 0.5f)
        // A dot lit in both stays lit, a dot lit in neither stays dark, the others are half way
        assertEquals(listOf(1f, 0.5f, 0f, 0.5f), glow.level.toList())
        // out-of-range mix is clamped, and a short frame counts as dark
        glow.blend(booleanArrayOf(true), booleanArrayOf(), 3f)
        assertEquals(listOf(0f, 0f, 0f, 0f), glow.level.toList())
    }
}
