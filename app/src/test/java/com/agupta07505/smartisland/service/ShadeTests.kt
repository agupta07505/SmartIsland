/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShadeSuppressorTest {
    private var now = 1_000L
    private var settings = SmartIslandSettings.Default.copy(enabled = true, hideFromNotificationShade = true)
    private var active: Array<StatusBarNotification>? = emptyArray()
    private val cancelled = mutableListOf<String>()

    private fun TestScope.suppressor() = ShadeSuppressor(this, { settings }, { active }, { cancelled += it }, { now })

    private fun sbn(key: String, packageName: String = "com.example.chat"): StatusBarNotification {
        val sbn = mockk<StatusBarNotification>()
        every { sbn.key } returns key
        every { sbn.packageName } returns packageName
        return sbn
    }

    @Test
    fun suppressCancelsAtOnceAndMarksTheKey() = runTest {
        val s = suppressor()
        s.suppress("k")
        assertEquals(listOf("k"), cancelled)
        assertEquals(1_000L, s.lastMarked("k"))
    }

    @Test
    fun secondHideInWindowWaits() = runTest {
        val s = suppressor()
        s.suppress("k")
        now += 100L
        s.suppress("k")
        assertEquals(listOf("k"), cancelled)
        assertEquals(1_100L, s.lastMarked("k"))
    }

    @Test
    fun aHideAfterTheWindowCancelsAgain() = runTest {
        val s = suppressor()
        s.suppress("k")
        now += 300L
        s.suppress("k")
        assertEquals(listOf("k", "k"), cancelled)
    }

    @Test
    fun stubbornCancelledThreeRetries() = runTest {
        active = arrayOf(sbn("k"))
        val s = suppressor()
        s.suppress("k")
        advanceUntilIdle()
        assertEquals(4, cancelled.size)
    }

    @Test
    fun noRetryOnceTheNotificationIsGone() = runTest {
        active = emptyArray()
        val s = suppressor()
        s.suppress("k")
        advanceUntilIdle()
        assertEquals(listOf("k"), cancelled)
    }

    @Test
    fun hidingOffMidRetryFreesKey() = runTest {
        active = arrayOf(sbn("k"))
        val s = suppressor()
        s.suppress("k")
        settings = settings.copy(hideFromNotificationShade = false)
        advanceUntilIdle()
        assertEquals(listOf("k"), cancelled)
        assertEquals(0L, s.lastMarked("k"))
    }

    @Test
    fun nothingWhenIslandOrHidingOff() = runTest {
        val s = suppressor()
        settings = settings.copy(enabled = false)
        s.suppress("k")
        settings = settings.copy(enabled = true, hideFromNotificationShade = false)
        s.suppress("k")
        assertTrue(cancelled.isEmpty())
        assertEquals(0L, s.lastMarked("k"))
    }

    @Test
    fun disabledAppStaysInShade() = runTest {
        active = arrayOf(sbn("k", "com.example.off"))
        settings = settings.copy(disabledNotificationPackages = setOf("com.example.off"))
        val s = suppressor()
        s.suppress("k")
        advanceUntilIdle()
        assertTrue(cancelled.isEmpty())
        assertEquals(0L, s.lastMarked("k"))
    }

    @Test
    fun clearFreesKeyClearAllFreesAll() = runTest {
        val s = suppressor()
        s.suppress("a")
        s.suppress("b")
        s.clear("a")
        assertEquals(0L, s.lastMarked("a"))
        assertEquals(1_000L, s.lastMarked("b"))
        s.clearAll()
        assertEquals(0L, s.lastMarked("b"))
    }
}

class ShadeSyncRulesTest {
    @Test
    fun shadePersistence() {
        assertTrue(ShadeSyncRules.isPersistentOrLoud(IslandMode.Music) { true })
        assertTrue(ShadeSyncRules.isPersistentOrLoud(IslandMode.Timer) { true })
        assertTrue(ShadeSyncRules.isPersistentOrLoud(IslandMode.IncomingCall) { true })

        assertTrue(ShadeSyncRules.isPersistentOrLoud(IslandMode.Notification) { false })
        assertFalse(ShadeSyncRules.isPersistentOrLoud(IslandMode.Notification) { true })

        assertFalse(ShadeSyncRules.isPersistentOrLoud(IslandMode.Battery) { false })
        assertFalse(ShadeSyncRules.isPersistentOrLoud(IslandMode.Flashlight) { false })

        var reads = 0
        ShadeSyncRules.isPersistentOrLoud(IslandMode.Music) { reads++; false }
        ShadeSyncRules.isPersistentOrLoud(IslandMode.Battery) { reads++; false }
        assertEquals(0, reads)
        ShadeSyncRules.isPersistentOrLoud(IslandMode.Notification) { reads++; false }
        assertEquals(1, reads)
    }

    @Test
    fun shadeImportNotificationsOnly() {
        assertTrue(ShadeSyncRules.isShadeImportMode(IslandMode.Notification))
        assertFalse(ShadeSyncRules.isShadeImportMode(IslandMode.DownloadUpload))
        assertFalse(ShadeSyncRules.isShadeImportMode(IslandMode.Music))
    }

    @Test
    fun theSameAppImportAlsoTakesTransfers() {
        assertTrue(ShadeSyncRules.isSameAppImportMode(IslandMode.Notification))
        assertTrue(ShadeSyncRules.isSameAppImportMode(IslandMode.DownloadUpload))
        assertFalse(ShadeSyncRules.isSameAppImportMode(IslandMode.Music))
    }

    @Test
    fun theStartupPassesKeepTheirWaits() {
        assertArrayEquals(longArrayOf(1_500L, 3_500L), ShadeSyncRules.INITIAL_RESYNC_DELAYS_MS)
    }
}
