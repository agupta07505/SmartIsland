/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.overlay

enum class CompactNotificationShape {
    Circle,
    MiniPill
}

/**
 * Determines the visual shapes used for companion indicators when multiple
 * notifications are active.
 */
fun compactNotificationShapes(count: Int, expanded: Boolean): List<CompactNotificationShape> {
    if (count < 2 || expanded) return emptyList()
    return when (count) {
        2 -> listOf(CompactNotificationShape.Circle)
        3 -> listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle)
        else -> listOf(
            CompactNotificationShape.MiniPill,
            CompactNotificationShape.Circle
        )
    }
}
