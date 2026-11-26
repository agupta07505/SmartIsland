/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object LedTick {
    const val STYLE_SMOOTH = "smooth"
    const val STYLE_LED = "led"
    val VALID_STYLES = setOf(STYLE_SMOOTH, STYLE_LED)

    fun normalizeStyle(id: String?): String = id?.takeIf { it in VALID_STYLES } ?: STYLE_SMOOTH

    const val DEFAULT_PATTERN = "4, 5, 4"
    const val MIN_HOPS = 1
    const val MAX_HOPS = 9
    const val MAX_PATTERN_LENGTH = 12
    private val DEFAULT_PATTERN_LIST = listOf(4, 5, 4)

    fun parsePattern(text: String?): List<Int> {
        val found = (text ?: "").split(Regex("[^0-9]+"))
            .mapNotNull { it.toIntOrNull() }
            .filter { it in MIN_HOPS..MAX_HOPS }
            .take(MAX_PATTERN_LENGTH)
        return found.ifEmpty { DEFAULT_PATTERN_LIST }
    }

    fun normalizePattern(text: String?): String = parsePattern(text).joinToString(", ")

    const val MIN_PACE_MS = 160
    const val MAX_PACE_MS = 800
    const val DEFAULT_PACE_MS = 420
    const val PACE_STEP_MS = 20

    fun normalizePace(ms: Int): Int {
        val clamped = ms.coerceIn(MIN_PACE_MS, MAX_PACE_MS)
        return MIN_PACE_MS + Math.round((clamped - MIN_PACE_MS).toFloat() / PACE_STEP_MS) * PACE_STEP_MS
    }

    const val ROWS = 9

    const val HOP_MS = 80

    fun hopsAt(pattern: List<Int>, beat: Long): Int {
        if (pattern.isEmpty()) return DEFAULT_PATTERN_LIST[Math.floorMod(beat, DEFAULT_PATTERN_LIST.size.toLong())
            .toInt()]
        return pattern[Math.floorMod(beat, pattern.size.toLong()).toInt()]
    }

    fun burstMs(hops: Int, paceMs: Int): Int = (hops.coerceAtLeast(1) - 1) * paceMs + HOP_MS

    fun progress(tMs: Float, hops: Int, paceMs: Int): Float {
        val n = hops.coerceAtLeast(1)
        for (i in 0 until n) {
            val from = i.toFloat() / n
            val to = (i + 1).toFloat() / n
            val start = i * paceMs.toFloat()
            if (tMs >= start + HOP_MS) continue
            if (tMs < start) return from
            val u = (tMs - start) / HOP_MS
            return from + (to - from) * (1f - (1f - u) * (1f - u))
        }
        return 1f
    }

    fun rowOffset(progress: Float, rows: Int = ROWS): Int = Math.round(progress.coerceIn(0f, 1f) * rows)

    fun beatIndex(nowMs: Long, periodMs: Long): Long {
        val p = periodMs.coerceAtLeast(1L)
        return Math.floorDiv(nowMs + p / 2, p)
    }

    const val COLOR_RED = "red"
    const val COLOR_AMBER = "amber"
    const val COLOR_GREEN = "green"
    const val COLOR_WHITE = "white"
    val VALID_COLORS = listOf(COLOR_RED, COLOR_AMBER, COLOR_GREEN, COLOR_WHITE)

    fun normalizeColor(id: String?): String = id?.takeIf { it in VALID_COLORS } ?: COLOR_RED

    /**
     * With the stagger on, the right slot (usually the value) starts half a pace after the left (usually the glyph), so
     * a change reads left, right, left, right. Off = together
     */
    const val STAGGER_SHARE = 0.5f

    fun staggerMs(paceMs: Int, right: Boolean, stagger: Boolean): Long =
        if (stagger && right) Math.round(paceMs * STAGGER_SHARE).toLong() else 0L

    const val FONT_BUILTIN = ""
}

data class LedTickConfig(
    val pattern: List<Int> = LedTick.parsePattern(LedTick.DEFAULT_PATTERN),
    val paceMs: Int = LedTick.DEFAULT_PACE_MS,
    val fontId: String = LedTick.FONT_BUILTIN,
    val colorId: String = LedTick.COLOR_RED,
    val stagger: Boolean = true
)
