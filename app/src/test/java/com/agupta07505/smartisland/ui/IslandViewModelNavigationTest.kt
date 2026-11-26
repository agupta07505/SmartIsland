/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.graphics.Bitmap
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotificationMedia
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModelNavigationTest : IslandViewModelTestBase() {

    private fun pictureCard(key: String, pictures: Int) = IslandNotification(
        key = key, packageName = "com.chat", appName = "Chat", title = key, text = "text",
        mode = IslandMode.Notification, timeMillis = clockMs,
        media = List(pictures) { NotificationMedia(mockk<Bitmap>(relaxed = true)) }
    )

    @Test
    fun swipeWalksPictureStepsBack() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(pictureCard("a", 3))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.First, viewModel.cardDepth.value)

        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Preview, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Reader, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        // several pictures: the image selector, not a picture
        assertEquals(CardDepth.Selector, viewModel.cardDepth.value)
        assertEquals(null, viewModel.expandedMedia.value)
        assertFalse(viewModel.cardGoDeeper())
        viewModel.setCardSteps("a", CardSteps.pickFromSelector(
            CardSteps(reader = true, selector = true), 1
        ))
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Picture, viewModel.cardDepth.value)
        assertEquals("a" to 1, viewModel.expandedMedia.value)

        // swipe up: full picture -> selector -> preview -> first
        assertTrue(viewModel.cardGoBack())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Selector, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoBack())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Preview, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoBack())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.First, viewModel.cardDepth.value)
        assertFalse(viewModel.cardGoBack())
    }

    @Test
    fun cardWithOnePictureSkipsThePreview() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(pictureCard("a", 1))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Reader, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.PictureMid, viewModel.cardDepth.value)
        assertEquals("a" to 0, viewModel.expandedMedia.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Picture, viewModel.cardDepth.value)
    }

    @Test
    fun plainCardNoDeeperStep() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "short"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.cardGoDeeper())

        // a long text (the card reports it is cut off) opens the reader
        viewModel.reportCardTextExpandable("a", true)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.Reader, viewModel.cardDepth.value)
        assertFalse(viewModel.cardGoDeeper())
        viewModel.collapse()
        testDispatcher.scheduler.runCurrent()
        assertEquals(CardDepth.First, viewModel.cardDepth.value)
    }

    @Test
    fun replyInputCollapsePause() = runTest {
        val (viewModel, notifRepo) = newViewModel()

        // a card to show (an expanded island with nothing in it closes by itself)
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()

        // expand island
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        // activate text input (e.g. user clicked reply)
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.isInputActive.value)

        // advance time past the 5000ms auto-collapse delay
        testDispatcher.scheduler.advanceTimeBy(6000L)
        testDispatcher.scheduler.runCurrent()

        // Island should STILL be expanded because input is active!
        assertTrue(viewModel.expanded.value)

        // deactivate input
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()
        Assert.assertFalse(viewModel.isInputActive.value)

        // advance 5000ms now that input is inactive
        testDispatcher.scheduler.advanceTimeBy(5500L)
        testDispatcher.scheduler.runCurrent()

        // island should now have auto-collapsed
        Assert.assertFalse(viewModel.expanded.value)
    }

    @Test
    fun explicitCollapseResetsInputActive() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.isInputActive.value)

        viewModel.collapse()
        testDispatcher.scheduler.runCurrent()
        Assert.assertFalse(viewModel.expanded.value)
        Assert.assertFalse(viewModel.isInputActive.value)
    }

    @Test
    fun touchClearsAutoExpand() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(
                autoExpandOnNotification = true,
                autoExpandPackages = setOf("com.chat")
            )
        )
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(chat("a", "hello"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertTrue(viewModel.autoExpanded.value)

        assertFalse(viewModel.handleBack())
        assertTrue(viewModel.expanded.value)

        // the first touch on the island ends the pass-through state
        viewModel.markUserEngaged()
        assertTrue(viewModel.expanded.value)
        assertFalse(viewModel.autoExpanded.value)
    }

    @Test
    fun manualExpandFlag() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "hello"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertFalse(viewModel.autoExpanded.value)

        viewModel.autoExpanded.value = true
        viewModel.collapse()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertFalse(viewModel.autoExpanded.value)
    }

    @Test
    fun emptyIslandCloseDelay() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        notifRepo.removeAllNotifications()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(500L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun newNotifKeepsLivePrimary() = runTest {
        val (viewModel, notifRepo) = newViewModel()

        notifRepo.postNotification(
            IslandNotification(
                key = "nav", packageName = "maps", appName = "Maps", title = "Turn left", text = "",
                mode = IslandMode.Navigation, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        notifRepo.postNotification(
            IslandNotification(
                key = "msg", packageName = "chat", appName = "Chat", title = "Hi", text = "there",
                mode = IslandMode.Notification, timeMillis = 2L
            )
        )
        testDispatcher.scheduler.runCurrent()

        // a live event keeps the primary cutout
        assertEquals(listOf("nav", "msg"), viewModel.visibleNotifications.value.map { it.key })
        assertEquals(0, viewModel.selectedIndex.value)
        assertEquals(IslandMode.Navigation, viewModel.mode.value)
    }

    @Test
    fun portraitArrivalKeepsOrder() = runTest {
        val (viewModel, notifRepo) = newViewModel()

        notifRepo.postNotification(
            IslandNotification(
                key = "msg", packageName = "chat", appName = "Chat", title = "Hi", text = "there",
                mode = IslandMode.Notification, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        notifRepo.postNotification(
            IslandNotification(
                key = "bt", packageName = "android", appName = "Bluetooth", title = "Buds", text = "",
                mode = IslandMode.Bluetooth, timeMillis = 2L
            )
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("msg", "bt"), viewModel.visibleNotifications.value.map { it.key })
    }

    @Test
    fun adjacentEventTarget() {
        val wrappedOrdered = listOf(card("a"), card("silent", silent = true), card("b"), card("c"))
        assertEquals("silent", IslandViewModel.nextEventTarget(wrappedOrdered, "a", forward = true)?.key)
        assertEquals("b", IslandViewModel.nextEventTarget(wrappedOrdered, "silent", forward = true)?.key)
        assertEquals("c", IslandViewModel.nextEventTarget(wrappedOrdered, "b", forward = true)?.key)
        assertEquals("a", IslandViewModel.nextEventTarget(wrappedOrdered, "c", forward = true)?.key)
        assertEquals("c", IslandViewModel.nextEventTarget(wrappedOrdered, "a", forward = false)?.key)
        assertEquals("silent", IslandViewModel.nextEventTarget(wrappedOrdered, "b", forward = false)?.key)
        assertEquals("a", IslandViewModel.nextEventTarget(wrappedOrdered, "silent", forward = false)?.key)

        // a paused player is low importance on purpose
        val silentOrdered = listOf(card("a"), card("music", IslandMode.Music, silent = true))
        assertEquals("music", IslandViewModel.nextEventTarget(silentOrdered, "a", forward = true)?.key)
    }

    @Test
    fun nextTargetPrefersLoudCard() {
        val ordered = listOf(
            card("a"),
            appCard("quiet", "pkg.chat", silent = true),
            appCard("loud", "pkg.chat", silent = false)
        )
        assertEquals("loud", IslandViewModel.nextEventTarget(ordered, "a", forward = true)?.key)
    }

    @Test
    fun nextTargetNoOtherCards() {
        assertEquals(null, IslandViewModel.nextEventTarget(emptyList(), null, forward = true))
        assertEquals(null, IslandViewModel.nextEventTarget(listOf(card("a")), "a", forward = true))
        val onlySilentElsewhere = listOf(card("a"), card("s", silent = true))
        assertEquals("s", IslandViewModel.nextEventTarget(onlySilentElsewhere, "a", forward = false)?.key)
    }

    @Test
    fun emptySelectionNavigation() {
        val ordered = listOf(card("a"), card("b"), card("c"))
        assertEquals("a", IslandViewModel.nextEventTarget(ordered, null, forward = true)?.key)
        assertEquals("c", IslandViewModel.nextEventTarget(ordered, null, forward = false)?.key)
        // selected card vanished from the list: same as nothing selected
        assertEquals("a", IslandViewModel.nextEventTarget(ordered, "gone", forward = true)?.key)
    }

    @Test
    fun nextTargetSiblingsOneEntry() {
        val ordered = listOf(appCard("a1", "app.a"), appCard("a2", "app.a"), appCard("b1", "app.b"))
        // from either card of app.a the next entry is app.b, never the other app.a card
        assertEquals("b1", IslandViewModel.nextEventTarget(ordered, "a1", forward = true)?.key)
        assertEquals("b1", IslandViewModel.nextEventTarget(ordered, "a2", forward = true)?.key)
        assertEquals("b1", IslandViewModel.nextEventTarget(ordered, "a2", forward = false)?.key)
        // coming back lands on the first card of the entry
        assertEquals("a1", IslandViewModel.nextEventTarget(ordered, "b1", forward = true)?.key)
        assertEquals("a1", IslandViewModel.nextEventTarget(ordered, "b1", forward = false)?.key)
    }

    @Test
    fun nextTargetOnlySiblings() {
        val ordered = listOf(appCard("a1", "app.a"), appCard("a2", "app.a"))
        assertEquals(null, IslandViewModel.nextEventTarget(ordered, "a1", forward = true))
    }

    // these tests use runCurrent(), not advanceUntilIdle()
    @Test
    fun notificationAndSiblingNavigation() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        notifRepo.postNotification(appCard("b1", "app.b"))
        testDispatcher.scheduler.runCurrent()

        fun selectedPackage() =
            viewModel.visibleNotifications.value[viewModel.selectedIndex.value].packageName

        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.a" }
        )
        assertTrue(viewModel.switchSibling(forward = true))
        assertEquals("app.a", selectedPackage())

        assertTrue(viewModel.switchNotification(forward = true))
        assertEquals("app.b", selectedPackage())
        // a card with no sibling has nobody to switch to
        assertFalse(viewModel.switchSibling(forward = true))

        assertTrue(viewModel.switchNotification(forward = true))
        assertEquals("app.a", selectedPackage())
    }

    @Test
    fun pickerOpensExpandedClosesOnPick() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        notifRepo.postNotification(appCard("b1", "app.b"))
        testDispatcher.scheduler.runCurrent()

        // collapsed: nothing to open
        assertFalse(viewModel.openSiblingPicker())

        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.a" }
        )
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.openSiblingPicker())
        assertTrue(viewModel.siblingPickerOpen.value)

        assertTrue(viewModel.selectSibling("a2"))
        assertFalse(viewModel.siblingPickerOpen.value)
        assertEquals(
            "a2",
            viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key
        )
        assertFalse("A card that is gone cannot be selected", viewModel.selectSibling("nope"))

        // a card with no sibling has no picker
        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.b" }
        )
        assertFalse(viewModel.openSiblingPicker())
    }

    @Test
    fun pickerClosesOnChangeOrGone() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        notifRepo.postNotification(appCard("b1", "app.b"))
        testDispatcher.scheduler.runCurrent()

        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.a" }
        )
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        // another app's card selected: closes
        assertTrue(viewModel.openSiblingPicker())
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.switchNotification(forward = true))
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.siblingPickerOpen.value)

        // the only sibling goes away: nothing left to pick from, closes
        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.a" }
        )
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.openSiblingPicker())
        testDispatcher.scheduler.runCurrent()
        val keepKey = viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key
        val dropKey = listOf("a1", "a2").first { it != keepKey }
        notifRepo.removeNotification(dropKey)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.siblingPickerOpen.value)
    }

    @Test
    fun collapseWaitsForPicker() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.openSiblingPicker())
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(20_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue("An open picker must not auto-collapse the island", viewModel.expanded.value)

        viewModel.closeSiblingPicker()
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(6_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse("The countdown starts again once the picker is closed", viewModel.expanded.value)
    }

    @Test
    fun pickerClosedWhileTypingReply() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        viewModel.setInputActive(true)
        assertFalse(viewModel.openSiblingPicker())
        viewModel.setInputActive(false)
        assertTrue(viewModel.openSiblingPicker())
    }

    @Test
    fun collapsingClosesTheSiblingPicker() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.openSiblingPicker())
        viewModel.collapse()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.siblingPickerOpen.value)
    }

    @Test
    fun expandedMessagePausesCollapse() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "a long message"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        viewModel.setAutoCollapsePaused(true)
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(20_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue("Reading a long message must not auto-collapse the island", viewModel.expanded.value)

        viewModel.setAutoCollapsePaused(false)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(4_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue("The countdown restarts from zero once the text is folded", viewModel.expanded.value)

        testDispatcher.scheduler.advanceTimeBy(1_500L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
    }
}
