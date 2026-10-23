/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The card pager and the selection (see [PagerSelection]). */
class PagerSelectionTest {

    private fun card(
        key: String,
        pkg: String,
        mode: IslandMode = IslandMode.Notification,
        time: Long = 0L
    ) = IslandNotification(
        key = key,
        packageName = pkg,
        appName = pkg,
        title = key,
        text = "",
        timeMillis = time,
        mode = mode
    )

    private fun recorder() = card("rec", "rec.app", IslandMode.ScreenRecording)

    private fun shown(vararg cards: IslandNotification) = IslandPriority.order(cards.toList())

    private fun indexOfKey(list: List<IslandNotification>, key: String) = list.indexOfFirst { it.key == key }

    // the page / index mapping across a list shift

    @Test
    fun aRecorderThatStartsIsInsertedAtPageZeroAndShiftsEveryPage() {
        val before = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2), card("c", "app.c", time = 1))
        val after = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2), card("c", "app.c", time = 1), recorder())
        assertEquals("rec", after.first().key)
        // The owner is on "a". Before: page 0. After: the recorder took page 0 and "a" is on page 1 (the pager, still
        // on page 0, would show the recorder).
        assertEquals(0, appGroupPageOf(before, indexOfKey(before, "a")))
        assertEquals(1, appGroupPageOf(after, indexOfKey(after, "a")))
        assertTrue(PagerSelection.shapeChanged(PagerSelection.pageGroups(before, 0), PagerSelection.pageGroups(after, indexOfKey(after, "a"))))
    }

    @Test
    fun afterTheShiftTheJumpTargetIsThePageOfTheShownCardNotThePageTheCardHadBefore() {
        val after = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2), recorder())
        val selected = indexOfKey(after, "a")
        val selectedPage = appGroupPageOf(after, selected)
        assertEquals(1, selectedPage)
        // The pager is on page 0 (the recorder's page now): it jumps to page 1, where "a" is.
        assertEquals(1, PagerSelection.jumpTarget(currentPage = 0, selectedPage = selectedPage, pageCount = 3))
        assertEquals("a", after[appGroupPageIndices(after, selected)[1]].key)
    }

    @Test
    fun theRecorderEndingShiftsThePagesBackAndTheJumpGoesBackToo() {
        val withRecorder = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2), recorder())
        val without = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2))
        val groupsBefore = PagerSelection.pageGroups(withRecorder, indexOfKey(withRecorder, "a"))
        val groupsAfter = PagerSelection.pageGroups(without, indexOfKey(without, "a"))
        assertTrue(PagerSelection.shapeChanged(groupsBefore, groupsAfter))
        // The pager is on page 1 ("a" with the recorder in front); without it "a" is page 0.
        val page = appGroupPageOf(without, indexOfKey(without, "a"))
        assertEquals(0, page)
        assertEquals(0, PagerSelection.jumpTarget(currentPage = 1, selectedPage = page, pageCount = 2))
        assertEquals("a", without[appGroupPageIndices(without, indexOfKey(without, "a"))[page]].key)
    }

    @Test
    fun theShownCardKeepsItsPageThroughASingleJumpWithFiveOrMoreCardsWaiting() {
        val base = (1..6).map { card("n$it", "app.$it", time = (10 - it).toLong()) }
        val owner = "n4"
        val before = shown(*base.toTypedArray())
        val after = shown(*(base + recorder()).toTypedArray())
        val pageBefore = appGroupPageOf(before, indexOfKey(before, owner))
        val pageAfter = appGroupPageOf(after, indexOfKey(after, owner))
        assertEquals(pageBefore + 1, pageAfter)
        val target = PagerSelection.jumpTarget(currentPage = pageBefore, selectedPage = pageAfter, pageCount = 7)
        assertEquals(pageAfter, target)
        // Whichever card the pager would otherwise have shown on the old page number is not the owner's.
        assertFalse(after[appGroupPageIndices(after, indexOfKey(after, owner))[pageBefore]].key == owner)
    }

    @Test
    fun aDeliberateSelectionChangeKeepsThePagesSoItIsNotAShapeChange() {
        val list = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2), card("c", "app.c", time = 1))
        val onA = PagerSelection.pageGroups(list, 0)
        val onC = PagerSelection.pageGroups(list, 2)
        assertEquals(onA, onC)
        assertFalse(PagerSelection.shapeChanged(onA, onC))
    }

    @Test
    fun aPickedSiblingChangesTheCardBehindAPageButNotThePages() {
        val list = shown(card("a1", "app.a", time = 5), card("a2", "app.a", time = 4), card("b", "app.b", time = 3))
        assertEquals(PagerSelection.pageGroups(list, 0), PagerSelection.pageGroups(list, 1))
    }

    @Test
    fun aNewCardOfANewAppChangesThePages() {
        val before = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2))
        val after = shown(card("a", "app.a", time = 3), card("b", "app.b", time = 2), card("c", "app.c", time = 9))
        assertTrue(PagerSelection.shapeChanged(PagerSelection.pageGroups(before, 0), PagerSelection.pageGroups(after, 1)))
    }

    @Test
    fun theFirstCompositionIsNeverAShapeChange() {
        assertFalse(PagerSelection.shapeChanged(null, listOf("app:a")))
    }

    // the jump

    @Test
    fun noJumpWhenThePagerAlreadyShowsTheShownCardsPage() {
        assertNull(PagerSelection.jumpTarget(currentPage = 2, selectedPage = 2, pageCount = 4))
    }

    @Test
    fun theJumpTargetStaysInsideThePages() {
        assertEquals(2, PagerSelection.jumpTarget(currentPage = 0, selectedPage = 9, pageCount = 3))
        assertEquals(0, PagerSelection.jumpTarget(currentPage = 2, selectedPage = -1, pageCount = 3))
        assertNull(PagerSelection.jumpTarget(currentPage = 0, selectedPage = 0, pageCount = 0))
    }

    // the write-back rule

    @Test
    fun aPageThatAFingerDraggedToIsWrittenBack() {
        assertTrue(PagerSelection.mayWriteBack(draggedByFinger = true, settledPage = 1, pageCount = 3))
    }

    @Test
    fun aScrollTheAppStartedItselfNeverWritesBack() {
        assertFalse(PagerSelection.mayWriteBack(draggedByFinger = false, settledPage = 1, pageCount = 3))
        assertFalse(PagerSelection.mayWriteBack(draggedByFinger = false, settledPage = 0, pageCount = 1))
    }

    @Test
    fun aSettledPageOutsideThePagesIsNeverWrittenBack() {
        assertFalse(PagerSelection.mayWriteBack(draggedByFinger = true, settledPage = 3, pageCount = 3))
        assertFalse(PagerSelection.mayWriteBack(draggedByFinger = true, settledPage = -1, pageCount = 3))
        assertFalse(PagerSelection.mayWriteBack(draggedByFinger = true, settledPage = 0, pageCount = 0))
    }

    @Test
    fun aDragThatWasRunningWhileThePagesChangedIsNotWrittenBack() {
        assertTrue(PagerSelection.dragStillValid(shapeChangedDuringDrag = false))
        assertFalse(PagerSelection.dragStillValid(shapeChangedDuringDrag = true))
    }

    @Test
    fun theLogLineNamesTheCardBeforeAndAfterAndWhereThePagerWas() {
        val line = PagerSelection.selectionLogLine("keyA", "keyB", settledPage = 2, pageCount = 4, selectedPage = 1, groups = listOf("app:x", "app:y"))
        assertTrue(line.startsWith("Pager selected card: keyA -> keyB"))
        assertTrue(line.contains("settledPage=2 of 4"))
        assertTrue(line.contains("selectedPage=1"))
        assertTrue(line.contains("app:x"))
    }
}
