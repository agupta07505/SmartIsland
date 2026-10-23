/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** A picture made of LEDs: [cols] by [rows], row by row, true = lit. */
class LedGrid(val cols: Int, val rows: Int, val bits: BooleanArray) {
    init {
        require(bits.size == cols * rows) { "bits must hold cols * rows LEDs" }
    }

    operator fun get(x: Int, y: Int): Boolean = x in 0 until cols && y in 0 until rows && bits[y * cols + x]

    companion object {
        val EMPTY = LedGrid(1, 1, BooleanArray(1))
    }
}

sealed interface LedFace {
    data class Text(val text: String) : LedFace
    data class Icon(val icon: LedIcon) : LedFace
}

/** Dot icons the LED slots use instead of the Material glyphs: 7x7, weather icons 9x9 to fill the board's height. */
enum class LedIcon { Clock, Sun, Moon, Cloud, Rain, Snow, Bolt, Battery, BatteryLow, BatteryFull, Alarm, ArrowUp, ArrowDown, Calendar, Wind }

/**
 * Built-in LED font: 5x7 dots per letter, plus 7x7 icons and 9x9 weather icons. Drawn by the app (no font file);
 * other fonts become dots in ui/LedRaster.kt.
 */
object LedFont {
    const val GLYPH_COLS = 5
    const val GLYPH_ROWS = 7
    const val GAP = 1

    private val GLYPHS: Map<Char, String> = mapOf(
        ' ' to "00000000000000000000000000000000000",
        'A' to "01110100011000110001111111000110001",
        'B' to "11110100011000111110100011000111110",
        'C' to "01110100011000010000100001000101110",
        'D' to "11110100011000110001100011000111110",
        'E' to "11111100001000011110100001000011111",
        'F' to "11111100001000011110100001000010000",
        'G' to "01110100011000010111100011000101111",
        'H' to "10001100011000111111100011000110001",
        'I' to "01110001000010000100001000010001110",
        'J' to "00111000100001000010000101001001100",
        'K' to "10001100101010011000101001001010001",
        'L' to "10000100001000010000100001000011111",
        'M' to "10001110111010110101100011000110001",
        'N' to "10001110011010110011100011000110001",
        'O' to "01110100011000110001100011000101110",
        'P' to "11110100011000111110100001000010000",
        'Q' to "01110100011000110001101011001001101",
        'R' to "11110100011000111110101001001010001",
        'S' to "01111100001000001110000010000111110",
        'T' to "11111001000010000100001000010000100",
        'U' to "10001100011000110001100011000101110",
        'V' to "10001100011000110001100010101000100",
        'W' to "10001100011000110101101011101110001",
        'X' to "10001100010101000100010101000110001",
        'Y' to "10001100010101000100001000010000100",
        'Z' to "11111000010001000100010001000011111",
        '0' to "01110100011001110101110011000101110",
        '1' to "00100011000010000100001000010001110",
        '2' to "01110100010000100010001000100011111",
        '3' to "11110000010000101110000010000111110",
        '4' to "00010001100101010010111110001000010",
        '5' to "11111100001111000001000011000101110",
        '6' to "00110010001000011110100011000101110",
        '7' to "11111000010001000100010000100001000",
        '8' to "01110100011000101110100011000101110",
        '9' to "01110100011000101111000010001001100",
        ':' to "00000001000010000000001000010000000",
        '-' to "00000000000000000000111110000000000",
        '.' to "00000000000000000000000000110001100",
        '%' to "11001110010001000100010001001110011",
        '/' to "00001000100001000100010000100010000",
        '+' to "00000001000010011111001000010000000",
        '°' to "01100100101001001100000000000000000",
        // Feels like is drawn as "~19°".
        '~' to "00000000000100010101000100000000000"
    )

    private val ICONS: Map<LedIcon, List<String>> = mapOf(
        LedIcon.Clock to listOf("0011100", "0100010", "1001001", "1001101", "1000001", "0100010", "0011100"),
        LedIcon.Sun to listOf("000010000", "010000010", "000111000", "001111100", "101111101", "001111100", "000111000", "010000010", "000010000"),
        LedIcon.Moon to listOf("000111100", "001111110", "011111000", "011110000", "011110000", "011110000", "011111000", "001111110", "000111100"),
        LedIcon.Cloud to listOf("000000000", "000011100", "000111110", "001111111", "011111111", "111111111", "111111111", "011111110", "000000000"),
        LedIcon.Rain to listOf("000011100", "000111110", "011111111", "111111111", "011111110", "000000000", "001001001", "010010010", "100100100"),
        LedIcon.Snow to listOf("000010000", "010010010", "001010100", "000111000", "111111111", "000111000", "001010100", "010010010", "000010000"),
        LedIcon.Bolt to listOf("000011111", "000111110", "001111100", "011111000", "011111110", "000011100", "000111000", "000110000", "001100000"),
        LedIcon.Battery to listOf("0000000", "1111110", "1111011", "1111011", "1111011", "1111110", "0000000"),
        LedIcon.BatteryLow to listOf("0000000", "1111110", "1100011", "1100011", "1100011", "1111110", "0000000"),
        LedIcon.BatteryFull to listOf("0000000", "1111110", "1111111", "1111111", "1111111", "1111110", "0000000"),
        LedIcon.Alarm to listOf("0001000", "0011100", "0111110", "0111110", "0111110", "1111111", "0001000"),
        LedIcon.ArrowDown to listOf("0001000", "0001000", "0001000", "1001001", "0101010", "0011100", "0001000"),
        LedIcon.ArrowUp to listOf("0001000", "0011100", "0101010", "1001001", "0001000", "0001000", "0001000"),
        LedIcon.Calendar to listOf("0100010", "1111111", "1000001", "1010101", "1000001", "1010101", "1111111"),
        LedIcon.Wind to listOf("0001110", "0010001", "0000001", "1111110", "0000000", "0111110", "0000001")
    )

    /** [text] in the built-in 7-row dot font. Lower case is drawn as capitals; a missing character becomes a space. */
    fun text(text: String): LedGrid {
        if (text.isEmpty()) return LedGrid(1, GLYPH_ROWS, BooleanArray(GLYPH_ROWS))
        val cols = text.length * (GLYPH_COLS + GAP) - GAP
        val bits = BooleanArray(cols * GLYPH_ROWS)
        text.forEachIndexed { i, raw ->
            val glyph = GLYPHS[raw.uppercaseChar()] ?: GLYPHS.getValue(' ')
            for (y in 0 until GLYPH_ROWS) for (x in 0 until GLYPH_COLS) {
                if (glyph[y * GLYPH_COLS + x] == '1') bits[y * cols + i * (GLYPH_COLS + GAP) + x] = true
            }
        }
        return LedGrid(cols, GLYPH_ROWS, bits)
    }

    /** Weather icons are [WEATHER_SIZE] by [WEATHER_SIZE] dots; every other icon is 7x7. */
    const val WEATHER_SIZE = 9

    fun icon(icon: LedIcon): LedGrid {
        val rows = ICONS.getValue(icon)
        val size = rows.size
        val bits = BooleanArray(size * size)
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c == '1') bits[y * size + x] = true } }
        return LedGrid(size, size, bits)
    }

    fun has(c: Char): Boolean = GLYPHS.containsKey(c.uppercaseChar())
}

/** Puts grids on a board and moves them whole LED rows. */
object LedFrames {
    /**
     * [grid] centred on a [boardCols] x [boardRows] board and moved down by [shiftY] rows (negative = up); rows that
     * leave are gone.
     */
    fun place(grid: LedGrid, boardCols: Int, boardRows: Int, shiftY: Int): BooleanArray {
        val out = BooleanArray(boardCols * boardRows)
        val ox = (boardCols - grid.cols) / 2
        val oy = (boardRows - grid.rows) / 2 + shiftY
        for (y in 0 until grid.rows) {
            val by = oy + y
            if (by < 0 || by >= boardRows) continue
            for (x in 0 until grid.cols) {
                val bx = ox + x
                if (bx < 0 || bx >= boardCols) continue
                if (grid.bits[y * grid.cols + x]) out[by * boardCols + bx] = true
            }
        }
        return out
    }

    /**
     * One burst frame: [from] moved [rowsOffset] rows up (down when [towardsDown]) with [to] following; after
     * [LedTick.ROWS] rows only [to] is left. May be the same grid.
     */
    fun frame(from: LedGrid, to: LedGrid, towardsDown: Boolean, rowsOffset: Int, boardCols: Int, boardRows: Int = LedTick.ROWS): BooleanArray {
        val dir = if (towardsDown) 1 else -1
        val rows = rowsOffset.coerceIn(0, boardRows)
        val out = place(from, boardCols, boardRows, dir * rows)
        val incoming = place(to, boardCols, boardRows, -dir * (boardRows - rows))
        for (i in out.indices) if (incoming[i]) out[i] = true
        return out
    }
}

/**
 * Fading afterglow of an LED board: a lit LED is full brightness at once and dies away over about [FADE_MS] once
 * dark, so moving text leaves a short trail.
 */
class LedGlow(val cols: Int, val rows: Int = LedTick.ROWS) {
    val level = FloatArray(cols * rows)

    fun step(frame: BooleanArray, dtMs: Float) {
        val keep = Math.exp((-dtMs.coerceAtLeast(0f) / FADE_MS).toDouble()).toFloat()
        for (i in level.indices) {
            val lit = if (i < frame.size && frame[i]) 1f else 0f
            level[i] = if (lit > level[i] * keep) lit else level[i] * keep
        }
    }

    /**
     * Sets every LED to a mix of two frames with no travel ([from] at 1 - [mix], [to] at [mix]): the cross-fade used
     * when footsteps may not move (Battery Saver, "remove animations").
     */
    fun blend(from: BooleanArray, to: BooleanArray, mix: Float) {
        val m = mix.coerceIn(0f, 1f)
        for (i in level.indices) {
            val a = if (i < from.size && from[i]) 1f else 0f
            val b = if (i < to.size && to[i]) 1f else 0f
            level[i] = a * (1f - m) + b * m
        }
    }

    fun settle(frame: BooleanArray) {
        for (i in level.indices) level[i] = if (i < frame.size && frame[i]) 1f else 0f
    }

    fun fading(frame: BooleanArray): Boolean {
        for (i in level.indices) {
            val lit = i < frame.size && frame[i]
            if (!lit && level[i] > VISIBLE) return true
        }
        return false
    }

    companion object {
        const val FADE_MS = 70f
        const val VISIBLE = 0.04f
    }
}
