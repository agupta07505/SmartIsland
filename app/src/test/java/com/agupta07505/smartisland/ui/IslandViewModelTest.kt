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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk

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
            timeMillis = System.currentTimeMillis()
        )

        notifRepo.postNotification(musicNotif)
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially Spotify is in background -> music notification is visible
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(IslandMode.Music, viewModel.mode.value)

        // User opens Spotify (Spotify in foreground) -> music notification hides from pill
        viewModel.foregroundPackage.value = "com.spotify.music"
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(IslandMode.Empty, viewModel.mode.value)

        // User leaves Spotify (Chrome in foreground) -> music notification reappears on pill!
        viewModel.foregroundPackage.value = "com.android.chrome"
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertEquals(IslandMode.Music, viewModel.mode.value)
    }

    @Test
    fun testInputActivePausesAutoCollapseAndCollapseResetsInputActive() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)

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

    /** Rule_Book §3.4: a message that asks to auto-expand during a call waits as the companion bubble. */
    @Test
    fun testEphemeralAutoExpandIsIgnoredWhileACallHoldsTheMainPill() = runTest {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(settingsRepo, notifRepo)
        testDispatcher.scheduler.runCurrent()

        val call = IslandNotification(
            key = "call_1",
            packageName = "com.android.dialer",
            appName = "Phone",
            title = "Alice",
            text = "Ongoing call",
            category = "call",
            mode = IslandMode.IncomingCall,
            timeMillis = System.currentTimeMillis()
        )
        notifRepo.postNotification(call)
        testDispatcher.scheduler.runCurrent()

        val message = IslandNotification(
            key = "msg_3",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Bob",
            text = "Are you free?",
            mode = IslandMode.Notification,
            timeMillis = System.currentTimeMillis()
        )
        notifRepo.postNotification(message, autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()

        // The call keeps the main pill and stays collapsed; the message waits as the companion bubble.
        org.junit.Assert.assertFalse(viewModel.expanded.value)
        assertEquals(listOf("call_1", "msg_3"), viewModel.visibleNotifications.value.map { it.key })
        assertEquals(0, viewModel.selectedIndex.value)
    }

    private fun autoExpandViewModel(): Pair<IslandViewModel, SmartIslandNotificationRepository> {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        io.mockk.every { settingsRepo.settings } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.agupta07505.smartisland.data.SmartIslandSettings(autoExpandOnNotification = true)
        )
        val notifRepo = SmartIslandNotificationRepository()
        return IslandViewModel(settingsRepo, notifRepo) to notifRepo
    }

    private fun message(key: String) = IslandNotification(
        key = key,
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        title = "Alice",
        text = "Hello!",
        mode = IslandMode.Notification,
        timeMillis = System.currentTimeMillis()
    )

    @Test
    fun aSecureForegroundAppKeepsAMessageInTheSmallPill() = runTest {
        val (viewModel, notifRepo) = autoExpandViewModel()
        viewModel.setForegroundSecure(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(message("secure_1"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()

        org.junit.Assert.assertFalse(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun inLandscapeAlertsNeitherAutoExpandNorOpenOnTap() = runTest {
        val (viewModel, notifRepo) = autoExpandViewModel()
        viewModel.setLandscape(true)
        testDispatcher.scheduler.runCurrent()

        notifRepo.postNotification(message("land_1"), autoExpand = true)
        testDispatcher.scheduler.runCurrent()
        org.junit.Assert.assertFalse(viewModel.expanded.value)

        viewModel.toggleExpanded()
        testDispatcher.scheduler.runCurrent()
        org.junit.Assert.assertFalse(viewModel.expanded.value)

        // Back to portrait, a tap opens it again.
        viewModel.setLandscape(false)
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
    }

    @Test
    fun aLockedPhoneMasksAPrivateMessage() = runTest {
        val (viewModel, notifRepo) = autoExpandViewModel()
        viewModel.setLocked(true)
        notifRepo.postNotification(
            message("lock_1").copy(lockscreenVisibility = com.agupta07505.smartisland.model.LockscreenVisibility.PRIVATE)
        )
        testDispatcher.scheduler.runCurrent()
        val masked = viewModel.visibleNotifications.value.single()
        assertTrue(masked.isMasked)
        assertEquals("1 New Message", masked.title)

        viewModel.setLocked(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals("Alice", viewModel.visibleNotifications.value.single().title)
    }
}
