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
}

