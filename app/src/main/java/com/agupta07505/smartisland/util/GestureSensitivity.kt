/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

enum class PillSwipeDirection { Left, Right, Up, Down }

enum class GestureSensitivity(val id: String, val distanceScale: Float, val dominance: Float) {
    LOW("low", 1.45f, 1.6f),
    NORMAL("normal", 1f, 1f),
    HIGH("high", 0.7f, 1f);

    fun scaled(dp: Float): Float = dp * distanceScale

    fun isHorizontal(dx: Float, dy: Float): Boolean = abs(dx) > abs(dy) * dominance

    fun isVertical(dx: Float, dy: Float): Boolean = abs(dy) >= abs(dx) * dominance

    /** Null while the drag is too short or too diagonal. [threshold] is in the same unit as the drag. */
    fun pillSwipe(dx: Float, dy: Float, threshold: Float, horizontalThreshold: Float = threshold): PillSwipeDirection? = when {
        abs(dx) >= horizontalThreshold && isHorizontal(dx, dy) ->
            if (dx < 0f) PillSwipeDirection.Left else PillSwipeDirection.Right
        abs(dy) >= threshold && isVertical(dx, dy) ->
            if (dy < 0f) PillSwipeDirection.Up else PillSwipeDirection.Down
        else -> null
    }

    companion object {
        const val ID_LOW = "low"
        const val ID_NORMAL = "normal"
        const val ID_HIGH = "high"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): GestureSensitivity = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
