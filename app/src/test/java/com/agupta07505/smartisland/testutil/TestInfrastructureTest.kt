/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.testutil

import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pins the shared test infrastructure: the builders must equal the literals the tests used before they existed */
@OptIn(ExperimentalCoroutinesApi::class)
class TestInfrastructureTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun mainDispatcherScheduling() {
        var ran = false
        CoroutineScope(Dispatchers.Main).launch { ran = true }
        assertFalse(ran)
        main.scheduler.advanceUntilIdle()
        assertTrue(ran)
    }

    @Test
    fun defaultCardIsFixedCard() {
        assertEquals(
            IslandNotification(
                key = "a", packageName = "pkg.a", appName = "a", title = "a", text = "",
                mode = IslandMode.Notification, timeMillis = 1L, isSilent = false
            ),
            testCard("a")
        )
    }

    @Test
    fun chatCardMatchesItsLiteral() {
        assertEquals(
            IslandNotification(
                key = "c", packageName = "com.chat", appName = "Chat", title = "c", text = "hi",
                mode = IslandMode.Notification, timeMillis = 1L
            ),
            chatCard("c", "hi")
        )
    }

    @Test
    fun musicCardMatchesItsLiteral() {
        assertEquals(
            IslandNotification(
                key = "music", packageName = "com.music", appName = "Music", title = "Song", text = "Artist",
                mode = IslandMode.Music, mediaIsPlaying = true, timeMillis = 1L
            ),
            musicCard(playing = true)
        )
    }

    @Test
    fun callCardMatchesLiteral() {
        fun literal(actions: List<IslandNotificationAction>) = IslandNotification(
            key = "call", packageName = "com.dialer", appName = "Phone", title = "Ann", text = "",
            mode = IslandMode.IncomingCall, timeMillis = 5L, actionIntents = actions
        )
        assertEquals(
            literal(listOf(IslandNotificationAction("Answer", null), IslandNotificationAction("Decline", null))),
            callCard(ringing = true)
        )
        assertEquals(literal(listOf(IslandNotificationAction("Hang up", null))), callCard(ringing = false))
    }

    @Test
    fun testSettingsUseProdDefaults() {
        assertEquals(SmartIslandSettings(), testSettings())
        assertEquals(SmartIslandSettings(enabled = true), testSettings { copy(enabled = true) })
    }
}
