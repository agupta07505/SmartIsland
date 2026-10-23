/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SameAppCardsTest {

    private fun card(
        key: String,
        pkg: String,
        mode: IslandMode = IslandMode.Notification
    ) = IslandNotification(
        key = key,
        packageName = pkg,
        appName = pkg,
        title = key,
        text = "",
        timeMillis = 0L,
        mode = mode
    )

    @Test
    fun noSiblingMeansNoSecondaryTarget() {
        val list = listOf(card("a", "app.one"), card("b", "app.two"))
        assertEquals(-1, nextSameAppIndex(list, 0))
    }

    @Test
    fun picksNextSameAppCardSkippingOtherApps() {
        val list = listOf(
            card("a1", "app.one"),
            card("b1", "app.two"),
            card("a2", "app.one")
        )
        assertEquals(2, nextSameAppIndex(list, 0))
    }

    @Test
    fun wrapsAroundAndCyclesThroughSiblings() {
        val list = listOf(
            card("a1", "app.one"),
            card("a2", "app.one"),
            card("a3", "app.one")
        )
        assertEquals(1, nextSameAppIndex(list, 0))
        assertEquals(2, nextSameAppIndex(list, 1))
        assertEquals(0, nextSameAppIndex(list, 2))
    }

    @Test
    fun systemCardsSharingAPackageDoNotGroup() {
        val list = listOf(
            card("battery", "com.android.systemui", IslandMode.Battery),
            card("bt", "com.android.systemui", IslandMode.Bluetooth)
        )
        assertEquals(-1, nextSameAppIndex(list, 0))
    }

    @Test
    fun realNotificationsFromSystemUiStillGroup() {
        val list = listOf(
            card("rec1", "com.android.systemui"),
            card("rec2", "com.android.systemui")
        )
        assertEquals(1, nextSameAppIndex(list, 0))
    }

    @Test
    fun outOfRangeIndexIsSafe() {
        assertEquals(-1, nextSameAppIndex(emptyList(), 0))
        assertEquals(-1, nextSameAppIndex(listOf(card("a", "app.one")), 5))
    }

    @Test
    fun siblingNeverIncludesItself() {
        val a = card("a", "app.one")
        assertFalse(isSameAppSibling(a, a))
        assertTrue(isSameAppSibling(a, card("b", "app.one")))
    }

    @Test
    fun expandedSecondaryFallsBackToOtherCardWithoutSameAppSibling() {
        val list = listOf(card("a", "app.one"), card("b", "app.two"))
        assertEquals(1, secondaryCardIndex(list, 0, expanded = true))
        assertEquals(-1, secondaryCardIndex(listOf(card("a", "app.one")), 0, expanded = true))
    }

    @Test
    fun expandedSecondaryShowsSameAppSibling() {
        val list = listOf(card("a1", "app.one"), card("b", "app.two"), card("a2", "app.one"))
        assertEquals(2, secondaryCardIndex(list, 0, expanded = true))
    }

    @Test
    fun collapsedSecondaryIsFirstOtherCard() {
        val list = listOf(card("a", "app.one"), card("b", "app.two"))
        assertEquals(1, secondaryCardIndex(list, 0, expanded = false))
        assertEquals(-1, secondaryCardIndex(listOf(card("a", "app.one")), 0, expanded = false))
    }

    @Test
    fun collapsedSecondarySkipsSiblingsOfTheCurrentApp() {
        val list = listOf(card("a1", "app.one"), card("a2", "app.one"), card("b", "app.two"))
        assertEquals(2, secondaryCardIndex(list, 0, expanded = false))
        // Only siblings of the current app: no second cutout at all.
        val onlyOneApp = listOf(card("a1", "app.one"), card("a2", "app.one"), card("a3", "app.one"))
        assertEquals(-1, secondaryCardIndex(onlyOneApp, 0, expanded = false))
        // Expanded keeps the sibling picker button.
        assertEquals(1, secondaryCardIndex(onlyOneApp, 0, expanded = true))
    }

    @Test
    fun collapsedCutoutCountTreatsAnAppAsOneCard() {
        val oneApp = listOf(card("a1", "app.one"), card("a2", "app.one"), card("a3", "app.one"))
        assertEquals(1, cutoutCardCount(oneApp, expanded = false))
        // Expanded: the app's own card plus the sibling picker button.
        assertEquals(2, cutoutCardCount(oneApp, expanded = true))
        val twoApps = oneApp + card("b", "app.two")
        assertEquals(2, cutoutCardCount(twoApps, expanded = false))
        // System cards are their own entry even when they share a package.
        val system = listOf(
            card("bat", "com.android.systemui", IslandMode.Battery),
            card("bt", "com.android.systemui", IslandMode.Bluetooth)
        )
        assertEquals(2, cutoutCardCount(system, expanded = false))
    }

    // app groups (sibling track S1)

    @Test
    fun siblingIndicesListsTheWholeAppInListOrder() {
        val list = listOf(
            card("a1", "app.one"),
            card("b1", "app.two"),
            card("a2", "app.one"),
            card("a3", "app.one")
        )
        assertEquals(listOf(0, 2, 3), siblingIndices(list, 2))
        assertEquals(emptyList<Int>(), siblingIndices(list, 1))
        assertEquals(emptyList<Int>(), siblingIndices(list, 9))
    }

    @Test
    fun nextSiblingStaysInsideTheApp() {
        val list = listOf(
            card("a1", "app.one"),
            card("b1", "app.two"),
            card("a2", "app.one"),
            card("a3", "app.one")
        )
        assertEquals(2, nextSiblingIndex(list, 0, forward = true))
        assertEquals(3, nextSiblingIndex(list, 2, forward = true))
        assertEquals(0, nextSiblingIndex(list, 3, forward = true))
        assertEquals(3, nextSiblingIndex(list, 0, forward = false))
        assertEquals(-1, nextSiblingIndex(list, 1, forward = true))
    }

    @Test
    fun nextAppGroupSkipsAllSiblings() {
        val list = listOf(
            card("a1", "app.one"),
            card("a2", "app.one"),
            card("a3", "app.one"),
            card("b1", "app.two")
        )
        assertEquals(3, nextAppGroupIndex(list, 0, forward = true))
        assertEquals(3, nextAppGroupIndex(list, 2, forward = true))
        assertEquals(0, nextAppGroupIndex(list, 3, forward = true))
        assertEquals(0, nextAppGroupIndex(list, 3, forward = false))
        assertEquals(3, nextAppGroupIndex(list, 1, forward = false))
    }

    @Test
    fun nextAppGroupReachesEveryAppEvenWhenAGroupIsSplitInTheList() {
        val list = listOf(
            card("a1", "app.one"),
            card("b1", "app.two"),
            card("a2", "app.one"),
            card("c1", "app.three")
        )
        assertEquals(1, nextAppGroupIndex(list, 0, forward = true))
        assertEquals(3, nextAppGroupIndex(list, 1, forward = true))
        assertEquals(0, nextAppGroupIndex(list, 3, forward = true))
        assertEquals(3, nextAppGroupIndex(list, 0, forward = false))
        // From the second card of app.one the next entry is still app.two.
        assertEquals(1, nextAppGroupIndex(list, 2, forward = true))
    }

    @Test
    fun nextAppGroupIsMinusOneWithASingleEntry() {
        val list = listOf(card("a1", "app.one"), card("a2", "app.one"))
        assertEquals(-1, nextAppGroupIndex(list, 0, forward = true))
        assertEquals(-1, nextAppGroupIndex(emptyList(), 0, forward = true))
        assertEquals(-1, nextAppGroupIndex(list, 7, forward = true))
    }

    @Test
    fun systemCardsAreTheirOwnEntries() {
        val list = listOf(
            card("battery", "com.android.systemui", IslandMode.Battery),
            card("bt", "com.android.systemui", IslandMode.Bluetooth)
        )
        assertEquals(1, nextAppGroupIndex(list, 0, forward = true))
        assertEquals(emptyList<Int>(), siblingIndices(list, 0))
    }

    @Test
    fun pagerHasOnePagePerAppAndShowsTheCurrentSibling() {
        val list = listOf(
            card("a1", "app.one"),
            card("b1", "app.two"),
            card("a2", "app.one"),
            card("a3", "app.one")
        )
        // Current card a1: app.one shows a1, app.two shows its first card.
        assertEquals(listOf(0, 1), appGroupPageIndices(list, 0))
        // Picking a3 changes what the app.one page shows, not the page count.
        assertEquals(listOf(3, 1), appGroupPageIndices(list, 3))
        assertEquals(0, appGroupPageOf(list, 3))
        assertEquals(1, appGroupPageOf(list, 1))
    }

    @Test
    fun cardsThatCannotGroupGetTheirOwnPage() {
        val list = listOf(
            card("a1", "app.one"),
            card("bat", "android", IslandMode.Battery),
            card("a2", "app.one"),
            card("bt", "android", IslandMode.Bluetooth)
        )
        assertEquals(listOf(0, 1, 3), appGroupPageIndices(list, 0))
    }

    @Test
    fun pickerCutoutOnlyWhenExpandedWithSiblings() {
        val list = listOf(card("a1", "app.one"), card("a2", "app.one"), card("b1", "app.two"))
        assertTrue(siblingPickerCutoutActive(list, 0, expanded = true))
        assertFalse(siblingPickerCutoutActive(list, 0, expanded = false))
        assertFalse(siblingPickerCutoutActive(list, 2, expanded = true))
    }

    @Test
    fun tertiaryNeverRepeatsTheCurrentOrSecondaryApp() {
        val list = listOf(
            card("a1", "app.one"), card("b1", "app.two"), card("b2", "app.two"),
            card("a2", "app.one"), card("c1", "app.three")
        )
        // Current app.one, secondary app.two: the third cutout is app.three, not a sibling of either.
        assertEquals(4, tertiaryCardIndex(list, 0, 1))
        // Only two apps: no tertiary at all.
        val two = listOf(card("a1", "app.one"), card("b1", "app.two"), card("b2", "app.two"))
        assertEquals(-1, tertiaryCardIndex(two, 0, 1))
        // Sibling picker as secondary (same app as current): the tertiary is another app.
        val picker = listOf(card("a1", "app.one"), card("b1", "app.two"), card("a2", "app.one"))
        assertEquals(1, tertiaryCardIndex(picker, 0, 2))
    }

    @Test
    fun expandedCutoutCountDoesNotCountSiblingsOfAnotherApp() {
        // Expanded, no sibling for the current card: the two other-app cards are ONE entry.
        val list = listOf(card("a1", "app.one"), card("b1", "app.two"), card("b2", "app.two"))
        assertEquals(2, cutoutCardCount(list, expanded = true, currentIndex = 0))
        // Expanded on app.two: its sibling picker is an extra entry.
        assertEquals(3, cutoutCardCount(list, expanded = true, currentIndex = 1))
    }

    @Test
    fun batteryEventTakesTheSecondaryCutoutBesideANotificationWithNoLiveEvent() {
        val list = listOf(card("note", "app.one"), card("bat", "com.android.systemui", IslandMode.Battery))
        assertEquals(1, secondaryCardIndex(list, 0, expanded = false))
        assertEquals(2, cutoutCardCount(list, expanded = false, currentIndex = 0))
        // While it pops up it is the current card, and the notification takes the secondary cutout.
        val popped = listOf(list[1], list[0])
        assertEquals(1, secondaryCardIndex(popped, 0, expanded = false))
        assertEquals(2, cutoutCardCount(popped, expanded = false, currentIndex = 0))
    }

    @Test
    fun batteryEventTakesTheSecondaryCutoutBehindALiveEvent() {
        val list = listOf(
            card("timer", "com.google.clock", IslandMode.Timer),
            card("bat", "com.android.systemui", IslandMode.Battery),
            card("note", "app.one")
        )
        assertEquals(1, secondaryCardIndex(list, 0, expanded = false))
        assertEquals(2, tertiaryCardIndex(list, 0, 1))
        assertEquals(3, cutoutCardCount(list, expanded = false, currentIndex = 0))
    }
}
