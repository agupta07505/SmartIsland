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
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModelSuppressionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun note(key: String, mode: IslandMode, category: String? = null) = IslandNotification(
        key = key, packageName = "pkg.$key", appName = key, title = key, text = "",
        mode = mode, category = category, timeMillis = System.currentTimeMillis()
    )

    @Test
    fun immersiveSuppressesMusicAndRestoresItAfterwards() = runTest {
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), notifRepo)
        notifRepo.postNotification(note("music", IslandMode.Music))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        viewModel.setImmersive(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isImmersive.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.isSuppressed.value)

        viewModel.setImmersive(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("music"), viewModel.visibleNotifications.value.map { it.key })
        assertFalse(viewModel.isSuppressed.value)
    }

    @Test
    fun anIncomingCallBreaksThroughSoTheIslandIsNotSuppressed() = runTest {
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), notifRepo)
        viewModel.setImmersive(true)
        notifRepo.postNotification(note("music", IslandMode.Music))
        notifRepo.postNotification(note("call", IslandMode.IncomingCall, "call"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("call"), viewModel.visibleNotifications.value.map { it.key })
        assertFalse(viewModel.isSuppressed.value)
    }

    @Test
    fun biometricClearanceHoldsIdleAndCollapses() = runTest {
        val notifRepo = SmartIslandNotificationRepository()
        val viewModel = IslandViewModel(mockk<SmartIslandSettingsRepository>(relaxed = true), notifRepo)
        notifRepo.postNotification(note("music", IslandMode.Music))
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        viewModel.setBiometricClearance(true)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertFalse(viewModel.expanded.value)
        // IDLE, not SUPPRESSED: the hardware mask stays.
        assertFalse(viewModel.isSuppressed.value)

        viewModel.setBiometricClearance(false)
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf("music"), viewModel.visibleNotifications.value.map { it.key })
    }
}
