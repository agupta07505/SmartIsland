/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModelVisibilityTest : IslandViewModelTestBase() {

    @Test
    fun musicPillHidesInForeground() = runTest {
        val (viewModel, notifRepo) = newViewModel()

        val musicNotif = IslandNotification(
            key = "music_1",
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Blinding Lights",
            text = "The Weeknd",
            mode = IslandMode.Music,
            mediaIsPlaying = true,
            timeMillis = clockMs
        )

        notifRepo.postNotification(musicNotif)
        // runCurrent, not advanceUntilIdle
        testDispatcher.scheduler.runCurrent()

        // initially Spotify is in background -> music notification is visible
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(IslandMode.Music, viewModel.mode.value)

        // user opens Spotify (Spotify in foreground) -> music notification hides from pill
        viewModel.foregroundPackage.value = "com.spotify.music"
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(IslandMode.Empty, viewModel.mode.value)

        viewModel.foregroundPackage.value = "com.android.chrome"
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(IslandMode.Music, viewModel.mode.value)
    }

    @Test
    fun foregroundVisibilityExceptions() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        fun card(key: String, pkg: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = pkg, appName = pkg, title = key, text = key,
            mode = mode, mediaIsPlaying = mode == IslandMode.Music, timeMillis = clockMs
        )
        notifRepo.postNotification(card("nav", "com.maps", IslandMode.Navigation))
        notifRepo.postNotification(card("timer", "com.maps", IslandMode.Timer))
        // a ringing call (it has an Answer button); an ongoing call hides in its own app like any live card
        notifRepo.postNotification(
            card("call", "com.maps", IslandMode.IncomingCall).copy(
                actionIntents = listOf(IslandNotificationAction("Answer", null))
            )
        )
        notifRepo.postNotification(card("other", "com.other", IslandMode.Navigation))
        testDispatcher.scheduler.runCurrent()
        assertEquals(4, viewModel.visibleNotifications.value.size)

        viewModel.foregroundPackage.value = "com.maps"
        testDispatcher.scheduler.runCurrent()
        assertEquals(
            setOf("call", "other"),
            viewModel.visibleNotifications.value.map { it.key }.toSet()
        )

        // the app leaves the screen: its cards come back, closed (nothing auto-expands)
        viewModel.foregroundPackage.value = "com.home"
        testDispatcher.scheduler.runCurrent()
        assertEquals(4, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun putAwayMusicStaysAway() {
        val track1 = card("music", IslandMode.Music).copy(title = "Song A", text = "Artist", mediaIsPlaying = true)
        val tick = track1.copy(mediaPositionMs = 42_000L)
        val track2 = track1.copy(title = "Song B", text = "Other artist")
        assertEquals(IslandCardFeed.liveAwayHash(track1), IslandCardFeed.liveAwayHash(tick))
        assertEquals(IslandCardFeed.liveAwayHash(track1), IslandCardFeed.liveAwayHash(track2))
        // other live cards match by key alone too
        val nav1 = card("nav", IslandMode.Navigation).copy(title = "Turn left")
        assertEquals(0, IslandCardFeed.liveAwayHash(nav1))
        assertEquals(0, IslandCardFeed.liveAwayHash(nav1.copy(title = "Turn right")))
    }

    @Test
    fun hiddenPillKeepsUserCalls() {
        val list = listOf(
            card("chat"),
            // dialing: still needs the user
            card("call", IslandMode.IncomingCall).copy(callStatusText = "Calling..."),
            // ongoing: a live card like a timer, hidden with the pill
            card("ongoing", IslandMode.IncomingCall),
            card("music", IslandMode.Music),
            card("timer", IslandMode.Timer)
        )
        assertEquals(listOf("call"), IslandCardFeed.applyPillHidden(list, true).map { it.key })
        // shown: nothing is filtered
        assertEquals(list.map { it.key }, IslandCardFeed.applyPillHidden(list, false).map { it.key })
        // no ringing or dialing call: the island is empty, which is what shows the idle pill
        assertTrue(IslandCardFeed.applyPillHidden(list.filterNot { it.key == "call" }, true).isEmpty())
    }

    @Test
    fun toggleHiddenFlipsAndSets() = runTest {
        val viewModel = newViewModel().first
        assertFalse(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertTrue(viewModel.pillHidden.value)
        viewModel.setPillHidden(true)
        assertTrue(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertFalse(viewModel.pillHidden.value)
    }

    @Test
    fun pillShownUntilLongPress() = runTest {
        val viewModel = newViewModel().first
        assertFalse(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertTrue(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertFalse(viewModel.pillHidden.value)
    }

    @Test
    fun landscapeHoldKeepsPortrait() = runTest {
        val viewModel = newViewModel().first
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.orientation.value = OrientationState(isLandscape = true)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setPillHidden(true)
        assertTrue(viewModel.pillHidden.value)
        viewModel.orientation.value = OrientationState(isLandscape = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.pillHidden.value)
        viewModel.orientation.value = OrientationState(isLandscape = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.pillHidden.value)
    }

    @Test
    fun foregroundMusicFilter() {
        val settings = SmartIslandSettings.Default
            .copy(disabledEvents = setOf("timer"))
        val list = listOf(
            card("timer", IslandMode.Timer, pkg = "com.clock"),
            card("chat", IslandMode.Notification, pkg = "com.chat"),
            card("music", IslandMode.Music, pkg = "com.spotify.music")
        )

        val visible = IslandCardFeed.filterVisible(list, "com.spotify.music", settings)

        assertEquals(listOf("chat"), visible.map { it.key })
        assertEquals(listOf("chat", "music"), IslandCardFeed.filterVisible(list, null, settings).map { it.key })
    }

    @Test
    fun foregroundLiveCardsFollowTheirOwnSwitch() {
        val settings = SmartIslandSettings.Default
            .copy(showInForegroundEvents = setOf("music", "download"))
        val list = listOf(
            card("music", IslandMode.Music, pkg = "com.app"),
            card("nav", IslandMode.Navigation, pkg = "com.app"),
            card("timer", IslandMode.Timer, pkg = "com.app"),
            card("chat", IslandMode.Notification, pkg = "com.chat")
        )

        // navigation is not switched on, the timer has no switch at all
        assertEquals(
            listOf("music", "chat"),
            IslandCardFeed.filterVisible(list, "com.app", settings).map { it.key }
        )
    }

    @Test
    fun foregroundDefaultsHideMusicAndNavigationOnly() {
        val list = listOf(
            card("music", IslandMode.Music, pkg = "com.app"),
            card("nav", IslandMode.Navigation, pkg = "com.app"),
            card("rec", IslandMode.ScreenRecording, pkg = "com.app"),
            card("hotspot", IslandMode.Hotspot, pkg = "com.app"),
            card("live", IslandMode.LiveActivity, pkg = "com.app"),
            card("timer", IslandMode.Timer, pkg = "com.app"),
            card("stopwatch", IslandMode.Stopwatch, pkg = "com.app")
        )

        assertEquals(
            listOf("rec", "hotspot", "live"),
            IslandCardFeed.filterVisible(list, "com.app", SmartIslandSettings.Default).map { it.key }
        )
    }

    @Test
    fun filterKeepsHiddenOutLocked() {
        val settings = SmartIslandSettings.Default
            .copy(hideOnLockScreenEvents = setOf("timer"))
        val list = listOf(
            card("timer", IslandMode.Timer),
            card("chat", IslandMode.Notification),
            card("call", IslandMode.IncomingCall)
        )

        assertEquals(listOf("chat", "call"), IslandCardFeed.filterVisible(list, null, settings, locked = true).map { it.key })
        assertEquals(listOf("timer", "chat", "call"), IslandCardFeed.filterVisible(list, null, settings, locked = false).map { it.key })
        // the default (no argument) is unlocked
        assertEquals(listOf("timer", "chat", "call"), IslandCardFeed.filterVisible(list, null, settings).map { it.key })
    }

    @Test
    fun filterLockSwitchKeepsCalls() {
        val settings = SmartIslandSettings.Default.copy(hideAllOnLockScreen = true)
        val list = listOf(
            card("timer", IslandMode.Timer),
            card("chat", IslandMode.Notification),
            card("call", IslandMode.IncomingCall)
        )
        assertEquals(listOf("call"), IslandCardFeed.filterVisible(list, null, settings, locked = true).map { it.key })
    }

    @Test
    fun filterNotifOffHidesCards() {
        val settings = SmartIslandSettings.Default.copy(notificationsEnabled = false)
        val list = listOf(card("chat", IslandMode.Notification), card("timer", IslandMode.Timer))
        assertEquals(listOf("timer"), IslandCardFeed.filterVisible(list, null, settings).map { it.key })
    }

    @Test
    fun filterLockedHidesAppCards() {
        val settings = SmartIslandSettings.Default
            .copy(hideOnLockScreenPackages = setOf("com.chat"))
        val list = listOf(
            card("msg", IslandMode.Notification, pkg = "com.chat"),
            card("call", IslandMode.IncomingCall, pkg = "com.chat"),
            card("other", IslandMode.Notification, pkg = "com.other")
        )
        assertEquals(listOf("call", "other"), IslandCardFeed.filterVisible(list, null, settings, locked = true).map { it.key })
        assertEquals(listOf("msg", "call", "other"), IslandCardFeed.filterVisible(list, null, settings, locked = false).map { it.key })
    }

    @Test
    fun shadeHidesEventsThenReturns() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        viewModel.setShadeOpen(true)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(2, notifRepo.notifications.value.size)

        viewModel.setShadeOpen(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun shadeBlocksEmptyExpansion() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true)
        )
        testDispatcher.scheduler.runCurrent()
        viewModel.setShadeOpen(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(chat("a", "hello"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        // the shade closes: the card is in the pill, closed, and does not open by itself
        viewModel.setShadeOpen(false)
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun shadeOrNoCardBlocksExpand() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        testDispatcher.scheduler.runCurrent()

        // no card at all: opening by tap or swipe must not give an empty island
        viewModel.toggleExpanded()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)

        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        viewModel.setShadeOpen(true)
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun lockScreenAutoExpandDisabled() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true, autoExpandOnLockScreen = false)
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.isLocked.value = true
        notifRepo.postNotification(chat("a"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Locked with the switch off: the card stays in the pill", viewModel.expanded.value)

        // unlocked again: the same kind of event opens the island as usual
        letPulseFinish()
        viewModel.isLocked.value = false
        notifRepo.postNotification(chat("b"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun lockScreenAutoExpandEnabled() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true, autoExpandOnLockScreen = true)
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.isLocked.value = true
        notifRepo.postNotification(chat("a"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun lockedRingingCallExpands() = runTest {
        val (viewModel, notifRepo) = newViewModel(
            SmartIslandSettings(autoExpandOnNotification = true, autoExpandOnLockScreen = false)
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.isLocked.value = true
        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }
}
