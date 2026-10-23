/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import androidx.lifecycle.viewModelScope

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMusicPillHidesInForegroundAndReappearsInBackground() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)

        val musicNotif = IslandNotification(
            key = "music_1",
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Blinding Lights",
            text = "The Weeknd",
            mode = IslandMode.Music,
            mediaIsPlaying = true,
            timeMillis = System.currentTimeMillis()
        )

        notifRepo.postNotification(musicNotif)
        // runCurrent, not advanceUntilIdle: that would also run the live idle timeout and put the playing card away
        testDispatcher.scheduler.runCurrent()

        // Initially Spotify is in background -> music notification is visible
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(IslandMode.Music, viewModel.mode.value)

        // User opens Spotify (Spotify in foreground) -> music notification hides from pill
        viewModel.foregroundPackage.value = "com.spotify.music"
        // runCurrent, not advanceUntilIdle: that would also run the live idle timeout and put the playing card away
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(IslandMode.Empty, viewModel.mode.value)

        // User leaves Spotify (Chrome in foreground) -> music notification reappears on pill!
        viewModel.foregroundPackage.value = "com.android.chrome"
        // runCurrent, not advanceUntilIdle: that would also run the live idle timeout and put the playing card away
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(IslandMode.Music, viewModel.mode.value)
    }

    @Test
    fun everyLiveCardOfTheForegroundAppHidesButCallsAndOtherAppsStay() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        fun card(key: String, pkg: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = pkg, appName = pkg, title = key, text = key,
            mode = mode, mediaIsPlaying = mode == IslandMode.Music, timeMillis = System.currentTimeMillis()
        )
        notifRepo.postNotification(card("nav", "com.maps", IslandMode.Navigation))
        notifRepo.postNotification(card("timer", "com.maps", IslandMode.Timer))
        notifRepo.postNotification(card("call", "com.maps", IslandMode.IncomingCall))
        notifRepo.postNotification(card("other", "com.other", IslandMode.Navigation))
        testDispatcher.scheduler.runCurrent()
        assertEquals(4, viewModel.visibleNotifications.value.size)

        viewModel.foregroundPackage.value = "com.maps"
        testDispatcher.scheduler.runCurrent()
        assertEquals(
            setOf("call", "other"),
            viewModel.visibleNotifications.value.map { it.key }.toSet()
        )

        // The app leaves the screen: its cards come back, closed (nothing auto-expands).
        viewModel.foregroundPackage.value = "com.home"
        testDispatcher.scheduler.runCurrent()
        assertEquals(4, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.expanded.value)
    }

    private fun pictureCard(key: String, pictures: Int) = IslandNotification(
        key = key, packageName = "com.chat", appName = "Chat", title = key, text = "text",
        mode = IslandMode.Notification, timeMillis = System.currentTimeMillis(),
        media = List(pictures) { com.agupta07505.smartisland.model.NotificationMedia(mockk<android.graphics.Bitmap>(relaxed = true)) }
    )

    @Test
    fun swipeDownGoesThroughEveryStepOfACardWithSeveralPicturesAndSwipeUpWalksBack() = runTest {
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), notifRepo)
        notifRepo.postNotification(pictureCard("a", 3))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.First, viewModel.cardDepth.value)

        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Preview, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Reader, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        // Several pictures: the image selector, not a picture; only a tap opens one at full size.
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Selector, viewModel.cardDepth.value)
        assertEquals(null, viewModel.expandedMedia.value)
        assertFalse(viewModel.cardGoDeeper())
        viewModel.setCardSteps("a", com.agupta07505.smartisland.model.CardSteps.pickFromSelector(
            com.agupta07505.smartisland.model.CardSteps(reader = true, selector = true), 1
        ))
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Picture, viewModel.cardDepth.value)
        assertEquals("a" to 1, viewModel.expandedMedia.value)

        // Swipe up: full picture -> selector -> preview -> first.
        assertTrue(viewModel.cardGoBack())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Selector, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoBack())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Preview, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoBack())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.First, viewModel.cardDepth.value)
        assertFalse(viewModel.cardGoBack())
    }

    @Test
    fun aCardWithOnePictureSkipsThePreview() = runTest {
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), notifRepo)
        notifRepo.postNotification(pictureCard("a", 1))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Reader, viewModel.cardDepth.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.PictureMid, viewModel.cardDepth.value)
        assertEquals("a" to 0, viewModel.expandedMedia.value)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Picture, viewModel.cardDepth.value)
    }

    @Test
    fun aCardWithoutPicturesOrALongTextHasNoDeeperStepAndACollapseStartsOverAtTheFirstState() = runTest {
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), notifRepo)
        notifRepo.postNotification(chat("a", "short"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.cardGoDeeper())

        // A long text (the card reports it is cut off) opens the reader, and collapsing starts over.
        viewModel.reportCardTextExpandable("a", true)
        assertTrue(viewModel.cardGoDeeper())
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.Reader, viewModel.cardDepth.value)
        assertFalse(viewModel.cardGoDeeper())
        viewModel.collapse()
        testDispatcher.scheduler.runCurrent()
        assertEquals(com.agupta07505.smartisland.model.CardDepth.First, viewModel.cardDepth.value)
    }

    @Test
    fun testInputActivePausesAutoCollapseAndCollapseResetsInputActive() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)

        // A card to show (an expanded island with nothing in it closes by itself)
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()

        // Expand island
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        // Activate text input (e.g. user clicked reply)
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.isInputActive.value)

        // Advance time past the 5000ms auto-collapse delay
        testDispatcher.scheduler.advanceTimeBy(6000L)
        testDispatcher.scheduler.runCurrent()

        // Island should STILL be expanded because input is active!
        assertTrue(viewModel.expanded.value)

        // Deactivate input
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()
        org.junit.Assert.assertFalse(viewModel.isInputActive.value)

        // Advance 5000ms now that input is inactive
        testDispatcher.scheduler.advanceTimeBy(5500L)
        testDispatcher.scheduler.runCurrent()

        // Island should now have auto-collapsed
        org.junit.Assert.assertFalse(viewModel.expanded.value)
    }

    @Test
    fun testExplicitCollapseResetsInputActive() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()

        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.isInputActive.value)

        viewModel.collapse()
        testDispatcher.scheduler.runCurrent()
        org.junit.Assert.assertFalse(viewModel.expanded.value)
        org.junit.Assert.assertFalse(viewModel.isInputActive.value)
    }

    @Test
    fun testAutoExpandEnabledExpandsIslandOnNewNotification() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        val notif = IslandNotification(
            key = "msg_1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Hello!",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )

        notifRepo.postNotification(notif, autoExpand = true)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.expanded.value)
        assertEquals(0, viewModel.selectedIndex.value)
    }

    @Test
    fun testAutoExpandDisabledKeepsIslandCollapsedOnNewNotification() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = false)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        val notif = IslandNotification(
            key = "msg_2",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Bob",
            text = "Hey there",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )

        notifRepo.postNotification(notif, autoExpand = true)
        testDispatcher.scheduler.runCurrent()

        // Should NOT be expanded because autoExpandOnNotification is false
        org.junit.Assert.assertFalse(viewModel.expanded.value)
        // Notification is still captured and selected for compact pill
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(0, viewModel.selectedIndex.value)
    }

    @Test
    fun playingMusicPutAwayComesBackOnANewTrackButNotOnATick() {
        val track1 = card("music", IslandMode.Music).copy(title = "Song A", text = "Artist", mediaIsPlaying = true)
        val tick = track1.copy(mediaPositionMs = 42_000L)
        val track2 = track1.copy(title = "Song B")
        assertEquals(IslandViewModel.liveAwayHash(track1), IslandViewModel.liveAwayHash(tick))
        assertTrue(IslandViewModel.liveAwayHash(track1) != IslandViewModel.liveAwayHash(track2))
        // Other live cards still match by key alone.
        val nav1 = card("nav", IslandMode.Navigation).copy(title = "Turn left")
        assertEquals(0, IslandViewModel.liveAwayHash(nav1))
        assertEquals(0, IslandViewModel.liveAwayHash(nav1.copy(title = "Turn right")))
    }

    @Test
    fun hiddenPillKeepsOnlyCalls() {
        val list = listOf(
            card("chat"),
            card("call", IslandMode.IncomingCall),
            card("music", IslandMode.Music),
            card("timer", IslandMode.Timer)
        )
        assertEquals(listOf("call"), IslandViewModel.applyPillHidden(list, true).map { it.key })
        // Shown: nothing is filtered.
        assertEquals(list.map { it.key }, IslandViewModel.applyPillHidden(list, false).map { it.key })
        // No call: the island is empty, which is what shows the idle pill.
        assertTrue(IslandViewModel.applyPillHidden(list.filterNot { it.mode == IslandMode.IncomingCall }, true).isEmpty())
    }

    @Test
    fun togglePillHiddenFlipsTheStateAndSetIsIdempotent() = runTest {
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), SmartIslandNotificationRepository())
        assertFalse(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertTrue(viewModel.pillHidden.value)
        viewModel.setPillHidden(true)
        assertTrue(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertFalse(viewModel.pillHidden.value)
    }

    @Test
    fun pillIsShownByDefaultAndOnlyALongPressChangesIt() = runTest {
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), SmartIslandNotificationRepository())
        // Shown by default, in portrait and landscape alike: rotation no longer touches the state.
        assertFalse(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertTrue(viewModel.pillHidden.value)
        viewModel.togglePillHidden()
        assertFalse(viewModel.pillHidden.value)
    }

    @Test
    fun filterVisibleHidesDisabledEventsAndTheForegroundAppsMusic() {
        val settings = com.agupta07505.smartisland.data.SmartIslandSettings.Default
            .copy(disabledEvents = setOf("timer"))
        fun card(key: String, pkg: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = pkg, appName = pkg, title = key, text = key,
            mode = mode, timeMillis = 0L
        )
        val list = listOf(
            card("timer", "com.clock", IslandMode.Timer),
            card("chat", "com.chat", IslandMode.Notification),
            card("music", "com.spotify.music", IslandMode.Music)
        )

        val visible = IslandViewModel.filterVisible(list, "com.spotify.music", settings)

        assertEquals(listOf("chat"), visible.map { it.key })
        assertEquals(listOf("chat", "music"), IslandViewModel.filterVisible(list, null, settings).map { it.key })
    }

    @Test
    fun filterVisibleKeepsTheForegroundAppsLiveCardsWhenTheOptionIsOn() {
        val settings = com.agupta07505.smartisland.data.SmartIslandSettings.Default
            .copy(showLiveEventsInForeground = true)
        fun card(key: String, pkg: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = pkg, appName = pkg, title = key, text = key,
            mode = mode, timeMillis = 0L
        )
        val list = listOf(
            card("music", "com.spotify.music", IslandMode.Music),
            card("timer", "com.spotify.music", IslandMode.Timer),
            card("chat", "com.chat", IslandMode.Notification)
        )

        assertEquals(
            listOf("music", "timer", "chat"),
            IslandViewModel.filterVisible(list, "com.spotify.music", settings).map { it.key }
        )
        // Option off (default): the foreground app's live cards stay hidden.
        assertEquals(
            listOf("chat"),
            IslandViewModel.filterVisible(
                list, "com.spotify.music",
                com.agupta07505.smartisland.data.SmartIslandSettings.Default
            ).map { it.key }
        )
    }

    @Test
    fun filterVisibleKeepsLockScreenHiddenEventsOutOfThePillOnlyWhileLocked() {
        val settings = com.agupta07505.smartisland.data.SmartIslandSettings.Default
            .copy(hideOnLockScreenEvents = setOf("timer"))
        fun card(key: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = "pkg.$key", appName = key, title = key, text = key,
            mode = mode, timeMillis = 0L
        )
        val list = listOf(
            card("timer", IslandMode.Timer),
            card("chat", IslandMode.Notification),
            card("call", IslandMode.IncomingCall)
        )

        assertEquals(listOf("chat", "call"), IslandViewModel.filterVisible(list, null, settings, locked = true).map { it.key })
        assertEquals(listOf("timer", "chat", "call"), IslandViewModel.filterVisible(list, null, settings, locked = false).map { it.key })
        // The default (no argument) is unlocked.
        assertEquals(listOf("timer", "chat", "call"), IslandViewModel.filterVisible(list, null, settings).map { it.key })
    }

    @Test
    fun filterVisibleWithTheUniversalLockScreenSwitchHidesEverythingButCalls() {
        val settings = com.agupta07505.smartisland.data.SmartIslandSettings.Default.copy(hideAllOnLockScreen = true)
        fun card(key: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = "pkg.$key", appName = key, title = key, text = key,
            mode = mode, timeMillis = 0L
        )
        val list = listOf(
            card("timer", IslandMode.Timer),
            card("chat", IslandMode.Notification),
            card("call", IslandMode.IncomingCall)
        )
        assertEquals(listOf("call"), IslandViewModel.filterVisible(list, null, settings, locked = true).map { it.key })
    }

    @Test
    fun filterVisibleHidesNotificationCardsWhileTheUniversalNotificationsSwitchIsOff() {
        val settings = com.agupta07505.smartisland.data.SmartIslandSettings.Default.copy(notificationsEnabled = false)
        fun card(key: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = "pkg.$key", appName = key, title = key, text = key,
            mode = mode, timeMillis = 0L
        )
        val list = listOf(card("chat", IslandMode.Notification), card("timer", IslandMode.Timer))
        assertEquals(listOf("timer"), IslandViewModel.filterVisible(list, null, settings).map { it.key })
    }

    @Test
    fun filterVisibleHidesAnAppsCardsOnTheLockScreenOnlyAndNeverItsCalls() {
        val settings = com.agupta07505.smartisland.data.SmartIslandSettings.Default
            .copy(hideOnLockScreenPackages = setOf("com.chat"))
        fun card(key: String, pkg: String, mode: IslandMode) = IslandNotification(
            key = key, packageName = pkg, appName = pkg, title = key, text = key,
            mode = mode, timeMillis = 0L
        )
        val list = listOf(
            card("msg", "com.chat", IslandMode.Notification),
            card("call", "com.chat", IslandMode.IncomingCall),
            card("other", "com.other", IslandMode.Notification)
        )
        assertEquals(listOf("call", "other"), IslandViewModel.filterVisible(list, null, settings, locked = true).map { it.key })
        assertEquals(listOf("msg", "call", "other"), IslandViewModel.filterVisible(list, null, settings, locked = false).map { it.key })
    }

    private fun chat(key: String, text: String = key) = IslandNotification(
        key = key, packageName = "com.chat", appName = "Chat", title = key, text = text,
        mode = IslandMode.Notification, timeMillis = 1L
    )

    @Test
    fun staticNotificationsRevertToIdleAfterTheIdleTimeout() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // The default idle timeout (5 s): still shown just before it (plus the extra second), gone just after.
        testDispatcher.scheduler.advanceTimeBy(com.agupta07505.smartisland.util.IslandTimings.STATIC_IDLE_TIMEOUT_MS + IslandViewModel.IDLE_TIMER_EXTRA_MS - 100L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(200L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(IslandMode.Empty, viewModel.mode.value)
        // The card itself is kept; only the island view is idle.
        assertEquals(1, notifRepo.notifications.value.size)
    }

    @Test
    fun otherActiveElementsKeepStaticNotificationsOnTheIsland() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        notifRepo.postNotification(
            IslandNotification(
                key = "timer", packageName = "com.clock", appName = "Clock", title = "Timer", text = "1:00",
                mode = IslandMode.Timer, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        // While the live card is on the island the static one stays (the live timer is 6 s, see below).
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    private fun music(playing: Boolean) = IslandNotification(
        key = "music", packageName = "com.music", appName = "Music", title = "Song", text = "Artist",
        mode = IslandMode.Music, mediaIsPlaying = playing, timeMillis = 1L
    )

    @Test
    fun pausedMediaRevertsToIdleAfterTenSecondsAndResumeBringsItBack() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = false))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(1, notifRepo.notifications.value.size)

        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // Paused again: a fresh countdown starts.
        notifRepo.postNotification(music(playing = false))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun pausedLiveCardsIdleOutButRunningOnesKeepTheIsland() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        fun stopwatch(paused: Boolean) = IslandNotification(
            key = "sw", packageName = "com.clock", appName = "Clock", title = "Stopwatch", text = "0:12",
            mode = IslandMode.Stopwatch, recordingPaused = paused, timeMillis = 1L
        )
        notifRepo.postNotification(stopwatch(paused = false))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        notifRepo.postNotification(stopwatch(paused = true))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun aRunningCardKeepsPausedMediaOnTheIsland() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = false))
        notifRepo.postNotification(
            IslandNotification(
                key = "nav", packageName = "com.maps", appName = "Maps", title = "Turn left", text = "200 m",
                mode = IslandMode.Navigation, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun updatedStaticNotificationReappearsAfterIdleTimeout() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        notifRepo.postNotification(chat("a", "second"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun tappingTheIdleCutoutPutsAwayEveryCardLiveOnesIncluded() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        fun nav(text: String) = IslandNotification(
            key = "nav", packageName = "com.maps", appName = "Maps", title = "Turn left", text = text,
            mode = IslandMode.Navigation, timeMillis = 1L
        )
        notifRepo.postNotification(nav("200 m"))
        notifRepo.postNotification(music(playing = true))
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(3, viewModel.visibleNotifications.value.size)

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.hasIdleStoredCards.value)

        // A live card that only ticks stays away; a brand new event shows.
        notifRepo.postNotification(nav("150 m"))
        notifRepo.postNotification(chat("b", "new"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf("b"), viewModel.visibleNotifications.value.map { it.key })
    }

    @Test
    fun tappingTheIdleCutoutAgainBringsEveryStoredCardBack() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun liveCardsBroughtBackByATapGetNoAutoTimer() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        viewModel.onIdleDotTapped() // reveal by the user
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // The user opened it: a playing (live) card stays as long as he wants.
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun aLiveCardThatArrivesByItselfGoesBackToTheIdleCutoutAfterTheTimer() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(300L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.hasIdleStoredCards.value)
        // It is only put away: still in the repository, and a tap on the idle cutout brings it back for good.
        assertEquals(1, notifRepo.notifications.value.size)
        viewModel.revealIdleCards()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun theAutoCollapseWaitsWhileAFingerHoldsTheFloatingWindowPullAndRestartsOnRelease() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        // The finger lands on the pull and stays there well past the 5 s countdown: the island stays open.
        viewModel.setAutoCollapseHeld(true)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(20_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        // Letting go starts the countdown again from zero: still open a little before 5 s, closed after.
        viewModel.setAutoCollapseHeld(false)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(4_500L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        org.junit.Assert.assertFalse(viewModel.expanded.value)
    }

    @Test
    fun theLiveTimerWaitsWhileTheIslandIsExpanded() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setAutoCollapsePaused(true)
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun callsNeverTimeOut() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(
            IslandNotification(
                key = "call", packageName = "com.dialer", appName = "Phone", title = "Ann", text = "Ongoing call",
                mode = IslandMode.IncomingCall, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun theOpenShadeHidesEveryEventAndComesBackWhenItCloses() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
    fun anAutoExpandEventWhileTheShadeIsOpenDoesNotOpenAnEmptyIsland() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()
        viewModel.setShadeOpen(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(chat("a", "hello"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        // The shade closes: the card is in the pill, closed, and does not open by itself.
        viewModel.setShadeOpen(false)
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun anIslandOpenedByAnEventIsAutoExpandedUntilTheUserTouchesIt() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(
                autoExpandOnNotification = true,
                autoExpandPackages = setOf("com.chat")
            )
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(chat("a", "hello"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertTrue(viewModel.autoExpanded.value)

        // The first touch on the island ends the pass-through state; the island stays open.
        viewModel.markUserEngaged()
        assertTrue(viewModel.expanded.value)
        assertFalse(viewModel.autoExpanded.value)
    }

    @Test
    fun aManualExpandIsNotAutoExpandedAndCollapseClearsTheFlag() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
    fun expandAndToggleDoNothingUnderTheShadeOrWithoutACard() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        // No card at all: opening by tap or swipe must not give an empty island.
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
    fun anExpandedIslandWhoseListBecomesEmptyClosesAfterAShortWait() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
    fun tappingTheIdleCutoutWhileExpandedCollapsesAndPutsEverythingAway() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(2_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun tappingTheIdleCutoutWhileExpandedClosesInTwoSteps() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        val plan = com.agupta07505.smartisland.model.IdleCloseSequence.plan(
            viewModel.settings.value.animationSpeed(viewModel.orientation.value.isLandscape)
        )

        // Step 1: the island closes (and the secondary merges); the pill and its card stay.
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.idleClosing.value)
        assertFalse(viewModel.idleLeaving.value)
        assertTrue(viewModel.idleTapActive.value)
        testDispatcher.scheduler.advanceTimeBy(plan.step1Ms / 2L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertTrue(viewModel.idleClosing.value)

        // Step 2: only when step 1 is over the cards go, the pill leaves on its own spring.
        testDispatcher.scheduler.advanceTimeBy(plan.step1Ms / 2L + 20L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertFalse(viewModel.idleClosing.value)
        assertTrue(viewModel.idleLeaving.value)

        // Everything is back to rest once the exit has played.
        testDispatcher.scheduler.advanceTimeBy(plan.step2Ms + IslandViewModel.IDLE_TAP_ACTIVE_MARGIN_MS + 100L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleLeaving.value)
        assertFalse(viewModel.idleTapActive.value)
    }

    @Test
    fun anIslandOpenedAgainDuringStepOneKeepsItsCards() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(50L)
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.advanceTimeBy(2_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.idleClosing.value)
        assertFalse(viewModel.idleLeaving.value)
    }

    @Test
    fun tappingTheIdleCutoutWithACollapsedCardLeavesAtOnceOnTheExitSpring() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleClosing.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.idleLeaving.value)
    }

    @Test
    fun revealingStoredCardsRaisesTheTapFlagForTheOpeningOnly() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleTapActive.value)

        viewModel.revealIdleCards()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.idleTapActive.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleTapActive.value)
    }

    @Test
    fun anEventSwipeRevealDoesNotRaiseTheTapFlag() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()

        viewModel.revealIdleCards(tapAnimation = false)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleTapActive.value)
    }

    private fun transfer(kind: com.agupta07505.smartisland.model.TransferKind, state: com.agupta07505.smartisland.model.TransferState) =
        IslandNotification(
            key = "t-$kind", packageName = "com.files", appName = "Files", title = "file.zip", text = "",
            mode = IslandMode.DownloadUpload, transferKind = kind, transferState = state, timeMillis = 1L
        )

    @Test
    fun pausedDownloadsAndUploadsIdleOutAndActiveOnesWaitForTheLiveTimer() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        val kinds = com.agupta07505.smartisland.model.TransferKind.DOWNLOAD to com.agupta07505.smartisland.model.TransferKind.UPLOAD
        val active = com.agupta07505.smartisland.model.TransferState.ACTIVE
        val paused = com.agupta07505.smartisland.model.TransferState.PAUSED

        notifRepo.postNotification(transfer(kinds.first, active))
        notifRepo.postNotification(transfer(kinds.second, active))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)

        notifRepo.postNotification(transfer(kinds.first, paused))
        notifRepo.postNotification(transfer(kinds.second, paused))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(2, notifRepo.notifications.value.size)
    }

    @Test
    fun tappingIdleCutoutBringsStoredCardsBackAndTheyIdleOutAgain() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.hasIdleStoredCards.value)

        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.hasIdleStoredCards.value)

        viewModel.revealIdleCards()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.hasIdleStoredCards.value)

        // Normal behaviour: the idle countdown runs again.
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun newNotificationPopsIntoPrimaryForOnePointEightSecondsThenPriorityReturns() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)

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

        // No popup over a live event: navigation keeps the primary cutout, the message waits in the secondary one.
        assertEquals(listOf("nav", "msg"), viewModel.visibleNotifications.value.map { it.key })
        assertEquals(0, viewModel.selectedIndex.value)

        testDispatcher.scheduler.advanceTimeBy(1_900L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("nav", "msg"), viewModel.visibleNotifications.value.map { it.key })
        assertEquals(0, viewModel.selectedIndex.value)
        assertEquals(IslandMode.Navigation, viewModel.mode.value)
    }

    private fun card(key: String, mode: IslandMode = IslandMode.Notification, silent: Boolean = false) =
        IslandNotification(
            key = key, packageName = "pkg.$key", appName = key, title = key, text = "",
            mode = mode, timeMillis = 1L, isSilent = silent
        )

    @Test
    fun nextEventTargetWrapsAndSkipsSilentNotifications() {
        val ordered = listOf(card("a"), card("silent", silent = true), card("b"), card("c"))
        assertEquals("b", IslandViewModel.nextEventTarget(ordered, "a", forward = true)?.key)
        assertEquals("c", IslandViewModel.nextEventTarget(ordered, "b", forward = true)?.key)
        assertEquals("a", IslandViewModel.nextEventTarget(ordered, "c", forward = true)?.key)
        assertEquals("c", IslandViewModel.nextEventTarget(ordered, "a", forward = false)?.key)
        assertEquals("a", IslandViewModel.nextEventTarget(ordered, "b", forward = false)?.key)
    }

    @Test
    fun nextEventTargetKeepsSilentLiveCards() {
        // A paused player is low importance on purpose: only plain notifications count as silent.
        val ordered = listOf(card("a"), card("music", IslandMode.Music, silent = true))
        assertEquals("music", IslandViewModel.nextEventTarget(ordered, "a", forward = true)?.key)
    }

    @Test
    fun nextEventTargetHasNowhereToGoWithoutOtherLoudCards() {
        assertEquals(null, IslandViewModel.nextEventTarget(emptyList(), null, forward = true))
        assertEquals(null, IslandViewModel.nextEventTarget(listOf(card("a")), "a", forward = true))
        val onlySilentElsewhere = listOf(card("a"), card("s", silent = true))
        assertEquals(null, IslandViewModel.nextEventTarget(onlySilentElsewhere, "a", forward = false))
    }

    @Test
    fun nextEventTargetStartsAtEitherEndWhenNothingIsSelected() {
        val ordered = listOf(card("a"), card("b"), card("c"))
        assertEquals("a", IslandViewModel.nextEventTarget(ordered, null, forward = true)?.key)
        assertEquals("c", IslandViewModel.nextEventTarget(ordered, null, forward = false)?.key)
        // Selected card vanished from the list: same as nothing selected.
        assertEquals("a", IslandViewModel.nextEventTarget(ordered, "gone", forward = true)?.key)
    }

    private fun appCard(key: String, pkg: String) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "",
        mode = IslandMode.Notification, timeMillis = 1L
    )

    @Test
    fun nextEventTargetTreatsSiblingsAsOneEntry() {
        val ordered = listOf(appCard("a1", "app.a"), appCard("a2", "app.a"), appCard("b1", "app.b"))
        // From either card of app.a the next entry is app.b, never the other app.a card.
        assertEquals("b1", IslandViewModel.nextEventTarget(ordered, "a1", forward = true)?.key)
        assertEquals("b1", IslandViewModel.nextEventTarget(ordered, "a2", forward = true)?.key)
        assertEquals("b1", IslandViewModel.nextEventTarget(ordered, "a2", forward = false)?.key)
        // Coming back lands on the first card of the entry.
        assertEquals("a1", IslandViewModel.nextEventTarget(ordered, "b1", forward = true)?.key)
        assertEquals("a1", IslandViewModel.nextEventTarget(ordered, "b1", forward = false)?.key)
    }

    @Test
    fun nextEventTargetHasNowhereToGoWithOnlySiblings() {
        val ordered = listOf(appCard("a1", "app.a"), appCard("a2", "app.a"))
        assertEquals(null, IslandViewModel.nextEventTarget(ordered, "a1", forward = true))
    }

    // These tests use runCurrent(), not advanceUntilIdle(): advanceUntilIdle() would also run the 10 s
    // idle timeout, which hides ordinary notification cards before the assertions.
    @Test
    fun switchNotificationSkipsSiblingsAndSwitchSiblingStaysInTheApp() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
        // A card with no sibling has nobody to switch to.
        assertFalse(viewModel.switchSibling(forward = true))

        assertTrue(viewModel.switchNotification(forward = true))
        assertEquals("app.a", selectedPackage())
    }

    @Test
    fun siblingPickerOpensOnlyWhenExpandedWithSiblingsAndSelectionClosesIt() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        notifRepo.postNotification(appCard("b1", "app.b"))
        testDispatcher.scheduler.runCurrent()

        // Collapsed: nothing to open.
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

        // A card with no sibling has no picker.
        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.b" }
        )
        assertFalse(viewModel.openSiblingPicker())
    }

    @Test
    fun siblingPickerClosesWhenTheSelectedCardChangesOrLosesItsSiblings() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(appCard("a2", "app.a"))
        notifRepo.postNotification(appCard("b1", "app.b"))
        testDispatcher.scheduler.runCurrent()

        viewModel.setSelectedNotificationIndex(
            viewModel.visibleNotifications.value.indexOfFirst { it.packageName == "app.a" }
        )
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        // Another app's card selected: closes.
        assertTrue(viewModel.openSiblingPicker())
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.switchNotification(forward = true))
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.siblingPickerOpen.value)

        // The only sibling goes away: nothing left to pick from, closes.
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
    fun autoCollapseWaitsWhileTheSiblingPickerIsOpen() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
    fun siblingPickerDoesNotOpenWhileAReplyIsBeingTyped() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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
    fun autoCollapseWaitsWhileAMessageIsExpandedThenStartsAgain() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
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

    private fun call(ringing: Boolean) = IslandNotification(
        key = "call", packageName = "com.dialer", appName = "Phone", title = "Ann", text = "",
        mode = IslandMode.IncomingCall, timeMillis = 5L,
        actionIntents = if (ringing) {
            listOf(
                com.agupta07505.smartisland.model.IslandNotificationAction("Answer", null),
                com.agupta07505.smartisland.model.IslandNotificationAction("Decline", null)
            )
        } else {
            listOf(com.agupta07505.smartisland.model.IslandNotificationAction("Hang up", null))
        }
    )

    @Test
    fun anAutoExpandEventDoesNotTakeTheCardWhileTypingAndNeverTakesItLater() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(chat("b"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        // An open island is never interrupted: the event does not take the card when typing ends either.
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun anAutoExpandEventDoesNotInterruptAnOpenIsland() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        notifRepo.postNotification(chat("b"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun aRingingCallStillTakesAnOpenIsland() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertEquals("call", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun aRingingCallTakesTheCardWhileTypingAndTypingResumesWhenItIsAnswered() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertEquals("call", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
        // The reply box is disposed when the card changes.
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals("call", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        // Answered: the call is ongoing now and the answer button collapsed the island.
        notifRepo.postNotification(call(ringing = false))
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
        assertEquals("a", viewModel.replyDrafts.resume.value)

        // The card opens its box: typing is active again and the request is used up.
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        assertEquals(null, viewModel.replyDrafts.resume.value)
    }

    @Test
    fun typingResumesWhenTheCallIsDeclined() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()

        notifRepo.removeNotification("call")
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.replyDrafts.resume.value)
    }

    @Test
    fun typingDoesNotResumeWhileTheCallIsStillRinging() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        viewModel.setInputActive(false)
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(5_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertEquals(null, viewModel.replyDrafts.resume.value)
    }

    @Test
    fun typingDoesNotResumeWhenTheRepliedToNotificationIsGone() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setInputActive(true)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        viewModel.setInputActive(false)
        testDispatcher.scheduler.runCurrent()

        notifRepo.removeNotification("a")
        notifRepo.removeNotification("call")
        viewModel.collapse()
        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertEquals(null, viewModel.replyDrafts.resume.value)
    }

    @Test
    fun noEventExpandsTheIslandOnTheLockScreenWhenThatSwitchIsOff() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true, autoExpandOnLockScreen = false)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        viewModel.isLocked.value = true
        notifRepo.postNotification(chat("a"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertFalse("Locked with the switch off: the card stays in the pill", viewModel.expanded.value)

        // Unlocked again: the same kind of event opens the island as usual.
        viewModel.isLocked.value = false
        notifRepo.postNotification(chat("b"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun eventsStillExpandTheIslandOnTheLockScreenWhenThatSwitchIsOn() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true, autoExpandOnLockScreen = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        viewModel.isLocked.value = true
        notifRepo.postNotification(chat("a"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun aRingingCallStillExpandsTheIslandOnTheLockScreenWithThatSwitchOff() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true, autoExpandOnLockScreen = false)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        viewModel.isLocked.value = true
        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    // Auto-expand rules

    private fun settingsFlow(settings: com.agupta07505.smartisland.data.SmartIslandSettings) =
        kotlinx.coroutines.flow.MutableStateFlow(settings)

    private fun batteryCard(category: String) = IslandNotification(
        key = "system_battery", packageName = "com.android.systemui", appName = "System", title = "Plugged in",
        text = "65%", category = category, mode = IslandMode.Battery, timeMillis = 10L
    )

    private fun appCard(key: String, pkg: String, silent: Boolean) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = key,
        mode = IslandMode.Notification, timeMillis = 2L, isSilent = silent
    )

    @Test
    fun theChargerCardEndingDoesNotOpenAnOlderNotification() = runTest {
        // The recording: an old notification sits in the stack, the charger card opens the island, the card ends.
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("old"))
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(batteryCard("battery_plugged_in"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("system_battery", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        notifRepo.removeNotification("system_battery")
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
    }

    @Test
    fun anOpenCardTheUserIsReadingStaysWhenAnotherCardEnds() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        notifRepo.postNotification(chat("a"))
        notifRepo.postNotification(batteryCard("battery_plugged_in"))
        testDispatcher.scheduler.runCurrent()
        // Open the island first (that also ends the battery card's popup and settles the order), then look at the chat.
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        viewModel.selectedIndex.value = viewModel.visibleNotifications.value.indexOfFirst { it.key == "a" }
        testDispatcher.scheduler.runCurrent()
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)

        notifRepo.removeNotification("system_battery")
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals("a", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    @Test
    fun anAppAtItsDefaultOnlyAutoExpandsLoudNotifications() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(appCard("quiet", "com.news", silent = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)

        notifRepo.postNotification(appCard("loud", "com.news", silent = false), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun anAppSwitchedOnAutoExpandsSilentOnesAndAnAppSwitchedOffNeverDoes() = runTest {
        val on = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { on.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandPackages = setOf("com.news"))
        )
        val onNotifs = SmartIslandNotificationRepository()
        val onVm = IslandViewModel(on, onNotifs)
        testDispatcher.scheduler.runCurrent()
        onNotifs.postNotification(appCard("quiet", "com.news", silent = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(onVm.expanded.value)

        val off = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { off.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(noAutoExpandPackages = setOf("com.news"))
        )
        val offNotifs = SmartIslandNotificationRepository()
        val offVm = IslandViewModel(off, offNotifs)
        testDispatcher.scheduler.runCurrent()
        offNotifs.postNotification(appCard("loud", "com.news", silent = false), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertFalse(offVm.expanded.value)
    }

    @Test
    fun anEventWithAutoExpandOffOnlyShowsInThePill() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(noAutoExpandEvents = setOf("battery_plugged_in"))
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(batteryCard("battery_plugged_in"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // Another event is not affected.
        notifRepo.postNotification(batteryCard("battery_full").copy(key = "system_full"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun aRingingCallAlwaysAutoExpandsEvenWithEveryEventSwitchOff() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns settingsFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(
                noAutoExpandEvents = com.agupta07505.smartisland.model.IslandEvent.entries.map { it.id }.toSet()
            )
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()
        notifRepo.postNotification(call(ringing = true), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun testTimerAndStopwatchAreSelectedWithoutAutoExpanding() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns
            kotlinx.coroutines.flow.MutableStateFlow(
                com.agupta07505.smartisland.data.SmartIslandSettings(
                    autoExpandOnNotification = false
                )
            )

        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(
            IslandNotification(
                key = "existing",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Existing notification",
                text = "Hello",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Notification
            )
        )
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(
            IslandNotification(
                key = "timer",
                packageName = "com.android.deskclock",
                appName = "Clock",
                title = "Timer",
                text = "00:10",
                timeMillis = System.currentTimeMillis() + 10_000L,
                mode = IslandMode.Timer
            ),
            autoExpand = true
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.selectedIndex.value)
        org.junit.Assert.assertFalse(viewModel.expanded.value)

        notifRepo.postNotification(
            IslandNotification(
                key = "stopwatch",
                packageName = "com.android.deskclock",
                appName = "Clock",
                title = "Stopwatch running",
                text = "",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Stopwatch
            ),
            autoExpand = true
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.selectedIndex.value)
        org.junit.Assert.assertFalse(viewModel.expanded.value)
    }
}
