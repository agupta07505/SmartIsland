/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * How the expanded island's card pager and the selection follow each other.
 *
 * The pager goes by PAGE POSITION and the selection by card, so syncing both ways blindly goes wrong when the list changes shape (a
 * screen recording ranks above ordinary notifications, so it is inserted at page 0 and every page shifts by one): the pager, still on
 * page 0, would show the recorder, then scroll to the owner's card, and a page that settled while the list was changing would write
 * whichever card sat there back as the open card. The rules below keep that from happening; the pager code only applies them.
 *
 *  - The list-to-page sync is told apart by what the pages ARE ([pageGroups]): a deliberate change (a tap on a cutout, a swipe action,
 *    the picker) moves the selection between pages that stay the same, so the pager scrolls as before; a list change alters the pages
 *    themselves ([shapeChanged]), so the pager only jumps, with no animation, to the page the shown card now has ([jumpTarget]).
 *  - A page that settled is written back as the selection only when a finger dragged the pager ([mayWriteBack]); a scroll the app
 *    started itself never writes back.
 */
object PagerSelection {

    /**
     * The identity of the group behind each page, in page order (one page per app group, see [appGroupPageIndices]). A selection
     * that moves between pages keeps this list; a card that is inserted, removed or re-ordered changes it.
     */
    fun pageGroups(cards: List<IslandNotification>, selectedIndex: Int): List<String> =
        appGroupPageIndices(cards, selectedIndex).mapNotNull { index -> cards.getOrNull(index)?.let { appGroupKey(it) } }

    /** The pages are not the pages of the last composition: page numbers may now hold other groups. False for the first composition. */
    fun shapeChanged(previous: List<String>?, current: List<String>): Boolean = previous != null && previous != current

    /**
     * The page the pager has to jump to (no animation) after the pages changed shape, or null when it already shows the right one.
     * [selectedPage] is the page of the shown card in the new list.
     */
    fun jumpTarget(currentPage: Int, selectedPage: Int, pageCount: Int): Int? {
        if (pageCount <= 0) return null
        val target = selectedPage.coerceIn(0, pageCount - 1)
        return target.takeIf { it != currentPage }
    }

    /**
     * A settled page becomes the open card only when a finger dragged the pager to it (the pager's own drag, which exists only while
     * its user scroll is enabled). `animateScrollToPage` / `scrollToPage` that the app starts itself never writes back, whatever page
     * they end on.
     */
    fun mayWriteBack(draggedByFinger: Boolean, settledPage: Int, pageCount: Int): Boolean =
        draggedByFinger && settledPage in 0 until pageCount

    /**
     * A finger drag that was running while the pages changed shape no longer means what the finger meant (page numbers moved under
     * it), so its result is not written back; the pager is put on the shown card's page instead.
     */
    fun dragStillValid(shapeChangedDuringDrag: Boolean): Boolean = !shapeChangedDuringDrag

    /** The log line for a selection change that came from the pager (the card shown before and after, and where the pager was). */
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
