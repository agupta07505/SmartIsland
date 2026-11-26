/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

object PagerSelection {
    fun pageGroups(cards: List<IslandNotification>, selectedIndex: Int): List<String> =
        appGroupPageIndices(cards, selectedIndex).mapNotNull { index -> cards.getOrNull(index)
            ?.let { appGroupKey(it) } }

    fun shapeChanged(previous: List<String>?, current: List<String>): Boolean = previous != null && previous != current

    /** Page the pager must jump to (no animation) after the pages changed shape. */
    fun jumpTarget(currentPage: Int, selectedPage: Int, pageCount: Int): Int? {
        if (pageCount <= 0) return null
        val target = selectedPage.coerceIn(0, pageCount - 1)
        return target.takeIf { it != currentPage }
    }

    fun jumpsToPage(snapRequested: Boolean, currentPage: Int, selectedPage: Int): Boolean =
        snapRequested || abs(selectedPage - currentPage) > 1

    fun mayWriteBack(draggedByFinger: Boolean, settledPage: Int, pageCount: Int): Boolean =
        draggedByFinger && settledPage in 0 until pageCount

    fun dragStillValid(shapeChangedDuringDrag: Boolean): Boolean = !shapeChangedDuringDrag
}
