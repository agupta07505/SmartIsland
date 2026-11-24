/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.testutil.MainDispatcherRule
import com.agupta07505.smartisland.testutil.callCard
import com.agupta07505.smartisland.testutil.chatCard
import com.agupta07505.smartisland.testutil.musicCard
import com.agupta07505.smartisland.testutil.testCard
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
abstract class IslandViewModelTestBase {

    protected val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    /**
     * A view model over a fresh notification repository and a relaxed settings mock. [settings] stubs the settings
     * flow, leaving it out keeps the mock's relaxed default
     */
    /** The view model's wall clock, fixed: card times are built from it, so the tests never depend on the real time */
    protected val clockMs = 1_700_000_000_000L

    protected fun newViewModel(
        settings: SmartIslandSettings? = null
    ): Pair<IslandViewModel, SmartIslandNotificationRepository> {
        val settingsRepo = mockk<SmartIslandSettingsRepository>(relaxed = true)
        if (settings != null) every { settingsRepo.settings } returns MutableStateFlow(settings)
        val notifRepo = SmartIslandNotificationRepository()
        return IslandViewModel(settingsRepo, notifRepo, wallClockMs = { clockMs }) to notifRepo
    }

    protected fun chat(key: String, text: String = key) = chatCard(key, text)

    protected fun music(playing: Boolean) = musicCard(playing)

    protected fun card(
        key: String,
        mode: IslandMode = IslandMode.Notification,
        silent: Boolean = false,
        pkg: String = "pkg.$key"
    ) = testCard(key = key, mode = mode, packageName = pkg, silent = silent)

    protected fun appCard(key: String, pkg: String) = testCard(key = key, packageName = pkg, appName = pkg)

    protected fun appCard(key: String, pkg: String, silent: Boolean) =
        testCard(key = key, packageName = pkg, appName = pkg, text = key, timeMillis = 2L, silent = silent)

    protected fun call(ringing: Boolean) = callCard(ringing)

    /** Lets the pulse of a card that stayed on the pill finish, a pulse blocks the next card from opening the island */
    protected fun letPulseFinish() {
        testDispatcher.scheduler.advanceTimeBy(IslandTimings.LIVE_PULSE_MS + IslandTimings.PULSE_GAP_MS + 100L)
        testDispatcher.scheduler.runCurrent()
    }
}
