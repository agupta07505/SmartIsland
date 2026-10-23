/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** How the expanded island's card pager and the selection follow each other. */
object PagerSelection {
    /**
     * Identity of the group behind each page, in page order (one page per app group, see [appGroupPageIndices]).
     * Unchanged by a selection move; a card inserted, removed or re-ordered changes it.
     */
    fun pageGroups(cards: List<IslandNotification>, selectedIndex: Int): List<String> =
        appGroupPageIndices(cards, selectedIndex).mapNotNull { index -> cards.getOrNull(index)?.let { appGroupKey(it) } }

    /**
     * The pages are not those of the last composition (page numbers may hold other groups now). False for the first
     * composition.
     */
    fun shapeChanged(previous: List<String>?, current: List<String>): Boolean = previous != null && previous != current

    /**
     * Page the pager must jump to (no animation) after the pages changed shape, or null when it already shows the
     * right one. [selectedPage] is the shown card's page in the new list.
     */
    fun jumpTarget(currentPage: Int, selectedPage: Int, pageCount: Int): Int? {
        if (pageCount <= 0) return null
        val target = selectedPage.coerceIn(0, pageCount - 1)
        return target.takeIf { it != currentPage }
    }

    /**
     * A settled page becomes the open card only when a finger dragged the pager there; programmatic scrolls never
     * write back.
     */
    fun mayWriteBack(draggedByFinger: Boolean, settledPage: Int, pageCount: Int): Boolean =
        draggedByFinger && settledPage in 0 until pageCount

    /**
     * A drag running while the pages changed shape is not written back (page numbers moved under it); the pager goes
     * to the shown card's page instead.
     */
    fun dragStillValid(shapeChangedDuringDrag: Boolean): Boolean = !shapeChangedDuringDrag

    /** Log line for a selection change that came from the pager (card shown before and after, and where the pager was). */
    fun selectionLogLine(
        oldKey: String?,
        newKey: String?,
        settledPage: Int,
        pageCount: Int,
        selectedPage: Int,
        groups: List<String>
    ): String =
        "Pager selected card: $oldKey -> $newKey (settledPage=$settledPage of $pageCount, selectedPage=$selectedPage, pages=$groups)"
}
