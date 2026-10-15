/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.expanded.siblingPickerFirstVisible
import com.agupta07505.smartisland.ui.expanded.siblingPickerHeight
import com.agupta07505.smartisland.ui.expanded.readingTextOwnsDrag
import com.agupta07505.smartisland.ui.expanded.siblingPickerOwnsDrag
import com.agupta07505.smartisland.ui.expanded.siblingPickerOwnsTouch
import com.agupta07505.smartisland.ui.expanded.siblingRowTapAccepted
import com.agupta07505.smartisland.ui.expanded.siblingScrollPositionPx
import com.agupta07505.smartisland.ui.expanded.stableSiblingOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiblingPickerTest {

    @Test
    fun heightGrowsWithRowsUpToThreeThenStays() {
        assertTrue(siblingPickerHeight(2) < siblingPickerHeight(3))
        assertEquals(siblingPickerHeight(3), siblingPickerHeight(9))
    }

    @Test
    fun heightStaysInsideTheIslandRange() {
        for (count in 1..12) {
            val h = siblingPickerHeight(count)
            assertTrue("$count rows -> $h", h >= 72.dp && h <= 240.dp)
        }
    }

    @Test
    fun firstVisibleRowKeepsTheCurrentCardInView() {
        assertEquals(0, siblingPickerFirstVisible(count = 3, currentPosition = 2))
        assertEquals(1, siblingPickerFirstVisible(count = 9, currentPosition = 3))
        assertEquals(3, siblingPickerFirstVisible(count = 9, currentPosition = 5))
        // Never scrolls past the last full page.
        assertEquals(6, siblingPickerFirstVisible(count = 9, currentPosition = 8))
    }

    @Test
    fun scrollPositionCountsWholeRowsAndTheOffsetIntoTheFirstVisibleOne() {
        assertEquals(0, siblingScrollPositionPx(0, 0, 100))
        assertEquals(230, siblingScrollPositionPx(2, 30, 100))
        // A scroll of one px changes the position by one px, so the tap check still sees it.
        assertEquals(1, siblingScrollPositionPx(0, 1, 100) - siblingScrollPositionPx(0, 0, 100))
        assertEquals(1, siblingScrollPositionPx(1, 0, 100) - siblingScrollPositionPx(0, 99, 100))
    }

    @Test
    fun anyDragOnTheRowsBelongsToTheList() {
        assertTrue(siblingPickerOwnsDrag(pickerOpen = true, downYDp = 80f, dragX = 1f, dragY = 12f))
        assertTrue(siblingPickerOwnsDrag(pickerOpen = true, downYDp = 80f, dragX = 0f, dragY = -9f))
        // Sideways over the rows must not fire the expanded left/right swipe actions.
        assertTrue(siblingPickerOwnsDrag(pickerOpen = true, downYDp = 80f, dragX = 14f, dragY = 3f))
        assertTrue(siblingPickerOwnsDrag(pickerOpen = true, downYDp = 80f, dragX = -14f, dragY = 0f))
    }

    @Test
    fun headerStillnessAndClosedPickerAreNotOwned() {
        // Touch began on the header.
        assertFalse(siblingPickerOwnsDrag(true, downYDp = 20f, dragX = 0f, dragY = 12f))
        // The finger has not really moved (a tap on a row).
        assertFalse(siblingPickerOwnsDrag(true, downYDp = 80f, dragX = 0.2f, dragY = 0f))
        // Picker not open.
        assertFalse(siblingPickerOwnsDrag(false, downYDp = 80f, dragX = 0f, dragY = 12f))
    }

    @Test
    fun sideBySideOnlyTheRightHalfOwnsTheDrag() {
        assertTrue(siblingPickerOwnsDrag(true, 80f, 0f, 12f, sideBySide = true, downXFraction = 0.75f))
        assertTrue(siblingPickerOwnsDrag(true, 80f, 9f, 1f, sideBySide = true, downXFraction = 0.75f))
        // Touch began on the card at the left: island swipes keep working there.
        assertFalse(siblingPickerOwnsDrag(true, 80f, 0f, 12f, sideBySide = true, downXFraction = 0.25f))
        // Portrait (not side by side) ignores x.
        assertTrue(siblingPickerOwnsDrag(true, 80f, 0f, 12f, sideBySide = false, downXFraction = 0.1f))
    }

    @Test
    fun anyMovementWhileReadingTheTextBelongsToTheText() {
        assertTrue(readingTextOwnsDrag(true, 0f, -12f))
        assertTrue(readingTextOwnsDrag(true, 0f, 12f))
        assertTrue(readingTextOwnsDrag(true, 15f, 2f))
        assertFalse(readingTextOwnsDrag(true, 0.1f, 0.1f))
        assertFalse(readingTextOwnsDrag(false, 0f, -12f))
    }

    @Test
    fun pickerLeftEdgeDecidesWhichSideOfTheSplitOwnsTheDrag() {
        // Picker island starts at 48% of the window: a touch at 40% is on the card, 60% on the picker.
        assertFalse(siblingPickerOwnsDrag(true, 80f, 0f, 12f, sideBySide = true, downXFraction = 0.40f, pickerLeftFraction = 0.48f))
        assertTrue(siblingPickerOwnsDrag(true, 80f, 0f, 12f, sideBySide = true, downXFraction = 0.60f, pickerLeftFraction = 0.48f))
        // Portrait / single island ignores the fraction.
        assertTrue(siblingPickerOwnsDrag(true, 80f, 0f, 12f, sideBySide = false, downXFraction = 0.1f, pickerLeftFraction = 0.9f))
    }

    @Test
    fun compactPickerIsShorterAndStillInsideTheIslandRange() {
        for (count in 1..12) {
            val compact = siblingPickerHeight(count, compact = true)
            assertTrue(compact <= siblingPickerHeight(count))
            assertTrue("$count rows -> $compact", compact >= 72.dp && compact <= 240.dp)
        }
        assertEquals(208.dp, siblingPickerHeight(3, compact = true))
    }

    @Test
    fun aTouchOnThePickerIsThePickersEvenWithoutMovement() {
        assertTrue(siblingPickerOwnsTouch(pickerOpen = true, sideBySide = false, downXFraction = 0.1f, pickerLeftFraction = 0.5f, pickerOnLeft = false))
        assertFalse(siblingPickerOwnsTouch(pickerOpen = false, sideBySide = false, downXFraction = 0.9f, pickerLeftFraction = 0.5f, pickerOnLeft = false))
    }

    @Test
    fun inTheSplitLayoutOnlyTheTouchesOnThePickersIslandCount() {
        // Picker on the right: the card side (left) keeps its own tap.
        assertFalse(siblingPickerOwnsTouch(true, true, 0.2f, 0.5f, false))
        assertTrue(siblingPickerOwnsTouch(true, true, 0.8f, 0.5f, false))
        // Reverse landscape: picker on the left.
        assertTrue(siblingPickerOwnsTouch(true, true, 0.2f, 0.5f, true))
        assertFalse(siblingPickerOwnsTouch(true, true, 0.8f, 0.5f, true))
    }

    private fun tap(
        peak: Float = 2f, slop: Float = 20f, scrolled: Int = 0, flinging: Boolean = false,
        age: Long = 1_000L, shifted: Boolean = false
    ) = siblingRowTapAccepted(peak, slop, scrolled, flinging, age, shifted)

    @Test
    fun aStillFingerOnASettledListIsATap() {
        assertTrue(tap())
        assertTrue(tap(peak = 20f)) // exactly the slop
    }

    @Test
    fun aScrollIsNeverATapNoMatterHowLittleItMoved() {
        // The list moved 3 px under the finger: Compose's own slop would still have called it a click.
        assertFalse(tap(scrolled = 3))
        // The finger travelled further than the slop (the row went with it).
        assertFalse(tap(peak = 21f))
    }

    @Test
    fun touchingAFlingingListOnlyStopsIt() {
        assertFalse(tap(flinging = true))
    }

    @Test
    fun tapsRightAfterTheRowsMovedAreIgnored() {
        assertFalse(tap(age = 100L))
        assertTrue(tap(age = 250L))
        // The rows moved while the finger was down: it was aimed at a row that is no longer there.
        assertFalse(tap(shifted = true))
    }

    @Test
    fun rowsKeepTheirPlaceAndNewCardsGoLast() {
        assertEquals(listOf("a", "b", "c"), stableSiblingOrder(emptyList(), listOf("a", "b", "c")))
        // While the picker is open the newest card is appended instead of pushing every row down.
        assertEquals(listOf("a", "b", "c", "n"), stableSiblingOrder(listOf("a", "b", "c"), listOf("n", "a", "b", "c")))
        // Re-posted cards come back in another order: the rows do not move.
        assertEquals(listOf("a", "b", "c"), stableSiblingOrder(listOf("a", "b", "c"), listOf("c", "a", "b")))
        // A card that left is dropped, the rest stay in place.
        assertEquals(listOf("a", "c"), stableSiblingOrder(listOf("a", "b", "c"), listOf("c", "a")))
    }

    @Test
    fun stableOrderIsAFixedPoint() {
        val once = stableSiblingOrder(listOf("a", "b"), listOf("n", "b", "a", "m"))
        assertEquals(once, stableSiblingOrder(once, listOf("n", "b", "a", "m")))
    }
}
