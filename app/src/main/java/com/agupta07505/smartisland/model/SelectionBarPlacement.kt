/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object SelectionBarPlacement {
    data class Spot(val x: Int, val y: Int)

    fun place(
        selectionCentreX: Int,
        selectionTop: Int,
        selectionBottom: Int,
        barWidth: Int,
        barHeight: Int,
        areaLeft: Int,
        areaTop: Int,
        areaRight: Int,
        areaBottom: Int,
        gap: Int,
        edge: Int
    ): Spot {
        val minX = areaLeft + edge
        val minY = areaTop + edge
        val x = (selectionCentreX - barWidth / 2).coerceIn(minX, maxOf(minX, areaRight - barWidth - edge))
        val above = selectionTop - barHeight - gap
        val below = selectionBottom + gap
        val y = when {
            above >= minY -> above
            below + barHeight <= areaBottom - edge -> below
            else -> above.coerceIn(minY, maxOf(minY, areaBottom - barHeight - edge))
        }
        return Spot(x, y)
    }
}
