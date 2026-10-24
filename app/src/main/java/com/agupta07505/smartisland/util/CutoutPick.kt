/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

data class CutoutRectPx(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Long get() = width.toLong() * height.toLong()
}

object CutoutPick {
    const val MERGE_GAP_DP = 48f
    const val TOP_SLACK_DP = 4f

    fun pick(rects: List<CutoutRectPx>, screenWidthPx: Int, density: Float): CutoutRectPx? {
        val real = rects.filter { it.width > 0 && it.height > 0 }
        if (real.isEmpty()) return null
        val d = if (density > 0f) density else 1f
        val slack = (TOP_SLACK_DP * d).toInt()
        val gap = (MERGE_GAP_DP * d).toInt()
        val topRects = real.filter { it.top <= slack }.sortedBy { it.left }
        if (topRects.isEmpty()) return real.minByOrNull { it.top }
        val groups = ArrayList<CutoutRectPx>()
        for (r in topRects) {
            val last = groups.lastOrNull()
            if (last != null && r.left - last.right <= gap) {
                groups[groups.lastIndex] = CutoutRectPx(
                    minOf(last.left, r.left), minOf(last.top, r.top), maxOf(last.right, r.right),
                        maxOf(last.bottom, r.bottom)
                )
            } else groups.add(r)
        }
        val centre = screenWidthPx / 2f
        return groups.minWithOrNull(
            compareBy<CutoutRectPx> { abs((it.left + it.right) / 2f - centre) }.thenByDescending { it.area }
        )
    }

    fun refine(picked: CutoutRectPx, pathBounds: CutoutRectPx?): CutoutRectPx {
        if (pathBounds != null) {
            val l = maxOf(picked.left, pathBounds.left)
            val t = maxOf(picked.top, pathBounds.top)
            val r = minOf(picked.right, pathBounds.right)
            val bt = minOf(picked.bottom, pathBounds.bottom)
            if (r > l && bt > t) return CutoutRectPx(l, t, r, bt)
        }
        return if (picked.height > picked.width) CutoutRectPx(picked.left, picked.top, picked.right,
            picked.top + picked.width) else picked
    }

    /** [refine] for a camera on a side edge (landscape). */
    fun refineSide(picked: CutoutRectPx, pathBounds: CutoutRectPx?, screenWidthPx: Int): CutoutRectPx {
        if (pathBounds != null) {
            val l = maxOf(picked.left, pathBounds.left)
            val t = maxOf(picked.top, pathBounds.top)
            val r = minOf(picked.right, pathBounds.right)
            val bt = minOf(picked.bottom, pathBounds.bottom)
            if (r > l && bt > t) return CutoutRectPx(l, t, r, bt)
        }
        if (picked.width <= picked.height) return picked
        val onLeft = (picked.left + picked.right) / 2f < screenWidthPx / 2f
        return if (onLeft) CutoutRectPx(picked.left, picked.top, picked.left + picked.height, picked.bottom)
        else CutoutRectPx(picked.right - picked.height, picked.top, picked.right, picked.bottom)
    }
}
