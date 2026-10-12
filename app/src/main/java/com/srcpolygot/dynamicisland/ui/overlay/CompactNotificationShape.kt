/*
 * Realistic Dynamic Island Fork
 * Package: com.srcpolygot.dynamicisland
 */

package com.srcpolygot.dynamicisland.ui.overlay

enum class CompactNotificationShape {
    Circle,
    MiniPill
}

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
