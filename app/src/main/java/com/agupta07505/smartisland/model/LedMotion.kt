/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class LedMotionKind { None, SunSpin, RainFall, WindSway, ArrowBob, AlarmGlow }

data class LedMotion(val kind: LedMotionKind, val stepMs: Int = 0) {
    val isRunning: Boolean get() = kind != LedMotionKind.None && stepMs > 0

    companion object {
        val NONE = LedMotion(LedMotionKind.None)
    }
}

object LedMotions {
    const val SUN_STEPS_PER_TURN = 16

    const val RAIN_FRAMES = 4

    const val WIND_FRAMES = 4

    /** Frame is never up shorter than this, so strong wind does not turn the board into a flicker. */
    const val MIN_STEP_MS = 120

    fun forGlyph(
        widget: IdleWidgetKind,
        look: WeatherLook?,
        isDay: Boolean,
        windKmh: Float,
        fastSpeed: Boolean,
        alarmSoon: Boolean,
        animationsOff: Boolean
    ): LedMotion {
        if (animationsOff) return LedMotion.NONE
        val glyph = GlyphMotions.forWidget(widget, look, isDay, windKmh, animationsOff = false)
        if (glyph.isRunning) {
            return when (glyph.kind) {
                GlyphMotionKind.SunSpin -> step(LedMotionKind.SunSpin, glyph.periodMs / SUN_STEPS_PER_TURN)
                GlyphMotionKind.RainFall -> step(LedMotionKind.RainFall, glyph.periodMs / RAIN_FRAMES)
                GlyphMotionKind.WindSway -> step(LedMotionKind.WindSway, glyph.periodMs / WIND_FRAMES)
                GlyphMotionKind.None -> LedMotion.NONE
            }
        }
        val attention = AttentionStates.forWidget(widget, fastSpeed, alarmSoon, animationsOff = false)
        val half = AttentionStates.periodMs(attention) / 2
        return when (attention) {
            GlyphAttention.SpeedPulse -> step(LedMotionKind.ArrowBob, half)
            GlyphAttention.AlarmGlow -> step(LedMotionKind.AlarmGlow, half)
            GlyphAttention.None -> LedMotion.NONE
        }
    }

    private fun step(kind: LedMotionKind, stepMs: Int) = LedMotion(kind, stepMs.coerceAtLeast(MIN_STEP_MS))

    /**
     * Frame of [frameCount] that is up at [nowMs]. Follows the clock, so the loop keeps its place when the board is
     * redrawn
     */
    fun frameAt(nowMs: Long, stepMs: Int, frameCount: Int): Int {
        if (stepMs <= 0 || frameCount <= 1) return 0
        return ((nowMs / stepMs) % frameCount).toInt()
    }
}

/**
 * Dot frames of a motion. Frame 0 is always the plain icon, so a board that stops moving ends on what it showed before
 */
object LedMotionFrames {
    private const val ROWS = LedTick.ROWS

    fun frames(icon: LedIcon, kind: LedMotionKind): List<LedGrid> = when {
        kind == LedMotionKind.SunSpin && icon == LedIcon.Sun -> sun()
        kind == LedMotionKind.RainFall && icon == LedIcon.Rain -> rain()
        kind == LedMotionKind.WindSway && icon == LedIcon.Wind -> wind()
        kind == LedMotionKind.ArrowBob && icon == LedIcon.ArrowUp -> bob(icon, -1)
        kind == LedMotionKind.ArrowBob && icon == LedIcon.ArrowDown -> bob(icon, 1)
        kind == LedMotionKind.AlarmGlow && icon == LedIcon.Alarm -> glow()
        else -> listOf(LedFont.icon(icon))
    }

    private val SUN_RAYS_A = listOf(4 to 0, 1 to 1, 7 to 1, 0 to 4, 8 to 4, 1 to 7, 7 to 7, 4 to 8)
    private val SUN_RAYS_B = listOf(2 to 0, 6 to 0, 8 to 2, 0 to 2, 0 to 6, 8 to 6, 2 to 8, 6 to 8)

    private fun sun(): List<LedGrid> {
        val base = LedFont.icon(LedIcon.Sun)
        val turned = base.bits.copyOf()
        for ((x, y) in SUN_RAYS_A) turned[y * base.cols + x] = false
        for ((x, y) in SUN_RAYS_B) turned[y * base.cols + x] = true
        return listOf(base, LedGrid(base.cols, base.rows, turned))
    }

    private fun rain(): List<LedGrid> {
        val base = LedFont.icon(LedIcon.Rain)
        val first = base.rows - 4
        return (0 until 4).map { k ->
            val bits = base.bits.copyOf()
            for (i in 0 until 4) {
                val from = first + i
                val to = first + (i + k) % 4
                for (x in 0 until base.cols) bits[to * base.cols + x] = base.bits[from * base.cols + x]
            }
            LedGrid(base.cols, base.rows, bits)
        }
    }

    // The two long streaks slide opposite ways by one dot, so it seems to gust. Stays 7 wide, the board does not resize.
    private fun wind(): List<LedGrid> {
        val base = LedFont.icon(LedIcon.Wind)
        fun slide(top: Int, bottom: Int): LedGrid {
            val bits = base.bits.copyOf()
            fun shiftRow(y: Int, dx: Int) {
                val row = BooleanArray(base.cols) { x -> base.bits[y * base.cols + x] }
                for (x in 0 until base.cols) {
                    val src = x - dx
                    bits[y * base.cols + x] = src in 0 until base.cols && row[src]
                }
            }
            shiftRow(3, top)
            shiftRow(5, bottom)
            return LedGrid(base.cols, base.rows, bits)
        }
        return listOf(base, slide(1, -1), base, slide(-1, 1))
    }

    // The arrow breathes: 1 dot further along its direction, then back. Drawn on the full board so a row can move.
    private fun bob(icon: LedIcon, dy: Int): List<LedGrid> {
        val base = LedFont.icon(icon)
        fun board(shift: Int) = LedGrid(base.cols, ROWS, LedFrames.place(base, base.cols, ROWS, shift))
        return listOf(board(0), board(dy))
    }

    private fun glow(): List<LedGrid> {
        val base = LedFont.icon(LedIcon.Alarm)
        val plain = LedFrames.place(base, base.cols, ROWS, 0)
        val marked = plain.copyOf()
        val last = base.cols - 1
        for (y in listOf(3, 5)) {
            marked[y * base.cols] = true
            marked[y * base.cols + last] = true
        }
        return listOf(LedGrid(base.cols, ROWS, plain), LedGrid(base.cols, ROWS, marked))
    }
}
