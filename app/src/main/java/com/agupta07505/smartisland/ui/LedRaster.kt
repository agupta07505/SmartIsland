/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.agupta07505.smartisland.model.LedFont
import com.agupta07505.smartisland.model.LedGrid
import com.agupta07505.smartisland.model.LedTick

/**
 * Turns text in any font (the phone's own, a built-in one or a font the user added) into LED dots for the LED tick style.
 *
 * The text is drawn once into a tiny bitmap whose letters are [LedFont.GLYPH_ROWS] pixels tall (the font's capital H is scaled to
 * that height), and every pixel that comes out lit becomes one LED. Nothing of the font is stored or copied anywhere: the font file
 * stays where it already is and only the dots are kept, for a moment, in memory.
 */
internal object LedRaster {
    private const val MAX_COLS = 120
    private const val CACHE_LIMIT = 64
    private const val ALPHA_LIT = 100

    private val cache = LinkedHashMap<String, LedGrid?>()

    /**
     * [text] in the font [fontId] as dots, [LedTick.ROWS] rows tall; null when the font cannot be drawn (the caller
     * uses the built-in font).
     */
    fun text(context: Context, text: String, fontId: String): LedGrid? {
        if (text.isEmpty()) return null
        val key = fontId + "\u0000" + text
        synchronized(cache) {
            if (cache.containsKey(key)) return cache[key]
        }
        val grid = runCatching { draw(context, text, fontId) }.getOrNull()
        synchronized(cache) {
            if (cache.size >= CACHE_LIMIT) cache.remove(cache.keys.first())
            cache[key] = grid
        }
        return grid
    }

    private fun draw(context: Context, text: String, fontId: String): LedGrid? {
        val paint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
            color = Color.WHITE
            typeface = islandTypeface(context, fontId)
            textSize = 100f
        }
        val bounds = Rect()
        paint.getTextBounds("H", 0, 1, bounds)
        val capAt100 = bounds.height().toFloat()
        if (capAt100 <= 0f) return null
        paint.textSize = 100f * LedFont.GLYPH_ROWS / capAt100
        val width = (Math.ceil(paint.measureText(text).toDouble()).toInt() + 2).coerceIn(3, MAX_COLS)
        val rows = LedTick.ROWS
        val bitmap = Bitmap.createBitmap(width, rows, Bitmap.Config.ARGB_8888)
        // Letters sit on row 7 (one spare row above, one below), like the built-in font.
        Canvas(bitmap).drawText(text, 1f, 1f + LedFont.GLYPH_ROWS, paint)
        val pixels = IntArray(width * rows)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, rows)
        bitmap.recycle()
        val lit = BooleanArray(width * rows) { (pixels[it] ushr 24) >= ALPHA_LIT }
        var first = -1
        var last = -1
        for (x in 0 until width) {
            var any = false
            for (y in 0 until rows) if (lit[y * width + x]) { any = true; break }
            if (any) {
                if (first < 0) first = x
                last = x
            }
        }
        if (first < 0) return null
        val cols = last - first + 1
        val bits = BooleanArray(cols * rows)
        for (y in 0 until rows) for (x in 0 until cols) bits[y * cols + x] = lit[y * width + first + x]
        return LedGrid(cols, rows, bits)
    }
}
