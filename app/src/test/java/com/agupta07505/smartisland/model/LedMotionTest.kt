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
    fun theSameGlyphsMoveAsOnTheSmoothPill() {
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
    }

    @Test
    fun animationsOffStopsEverything() {
        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Rain, off = true).isRunning)
        assertFalse(motion(IdleWidgetKind.NetSpeed, fast = true, off = true).isRunning)
        assertFalse(motion(IdleWidgetKind.NextAlarm, soon = true, off = true).isRunning)
    }

    @Test
    fun aStrongWindDoesNotFlickerTheBoard() {
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
    fun noFrameChangesTheBoardWidthAndLaterFramesDiffer() {
        for ((icon, kind) in all) {
            val frames = LedMotionFrames.frames(icon, kind)
            val cols = LedFont.icon(icon).cols
            for (f in frames) assertEquals("$icon width", cols, f.cols)
            val first = LedFrames.place(frames[0], cols, LedTick.ROWS, 0)
            assertTrue("$icon moves", frames.drop(1).any { !LedFrames.place(it, cols, LedTick.ROWS, 0).contentEquals(first) })
        }
    }

    @Test
    fun aMotionThatDoesNotFitTheIconIsJustTheIcon() {
        assertEquals(1, LedMotionFrames.frames(LedIcon.Moon, LedMotionKind.SunSpin).size)
        assertEquals(1, LedMotionFrames.frames(LedIcon.Sun, LedMotionKind.None).size)
    }

    @Test
    fun theRainFallsRowByRowAndComesRoundAgain() {
        val frames = LedMotionFrames.frames(LedIcon.Rain, LedMotionKind.RainFall)
        assertEquals(LedMotions.RAIN_FRAMES, frames.size)
        val plain = LedFont.icon(LedIcon.Rain)
        // The cloud above the streaks never moves.
        for (f in frames) for (i in 0 until 5 * plain.cols) assertEquals(plain.bits[i], f.bits[i])
        // Every streak row shows up in every frame, once.
        val lit = frames.map { f -> f.bits.count { it } }
        assertTrue(lit.all { it == lit[0] })
    }

    @Test
    fun theSunKeepsItsBodyAndTurnsItsRays() {
        val frames = LedMotionFrames.frames(LedIcon.Sun, LedMotionKind.SunSpin)
        val a = frames[0]
        val b = frames[1]
        // The disc (the middle rows' inner dots) stays lit in both.
        for (y in 2..6) for (x in 3..5) {
            assertTrue(a[x, y])
            assertTrue(b[x, y])
        }
        assertTrue(a[4, 0])
        assertFalse(b[4, 0])
        assertEquals(a.bits.count { it }, b.bits.count { it })
    }
}
