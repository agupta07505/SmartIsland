/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandTimings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRemovalRulesTest {
    @Test
    fun replyGraceHoldsUntilTime() {
        assertTrue(NotificationRemovalRules.isInReplyGrace(100L, 101L))
        assertFalse(NotificationRemovalRules.isInReplyGrace(101L, 101L))
        assertFalse(NotificationRemovalRules.isInReplyGrace(100L, null))
    }

    @Test
    fun recentMarkNeedsKnownKey() {
        assertTrue(NotificationRemovalRules.isRecentlyMarked(1_299L, 1_000L, 300L))
        assertFalse(NotificationRemovalRules.isRecentlyMarked(1_300L, 1_000L, 300L))
        assertFalse(NotificationRemovalRules.isRecentlyMarked(10_000L, 0L, NotificationRemovalRules.INITIAL_SUPPRESSION_WINDOW_MS))
    }


    @Test
    fun cancelUsesRemovalReason() {
        assertTrue(NotificationRemovalRules.isOwnRecentCancel(NotificationListenerService.REASON_LISTENER_CANCEL, true))
        assertFalse(NotificationRemovalRules.isOwnRecentCancel(NotificationListenerService.REASON_LISTENER_CANCEL, false))
        assertFalse(NotificationRemovalRules.isOwnRecentCancel(NotificationListenerService.REASON_CLICK, true))
        assertFalse(NotificationRemovalRules.isOwnRecentCancel(NotificationListenerService.REASON_APP_CANCEL, true))
        assertTrue(NotificationRemovalRules.isOwnRecentCancel(null, true))
        assertFalse(NotificationRemovalRules.isOwnRecentCancel(null, false))
    }


    @Test
    fun theWindowsKeepTheirValues() {
        assertEquals(1_500L, NotificationRemovalRules.INITIAL_SUPPRESSION_WINDOW_MS)
        assertEquals(300L, NotificationRemovalRules.RECENT_SUPPRESSION_WINDOW_MS)
    }
}

class PostedNotificationRulesTest {
    private fun sound(
        initial: Boolean = false,
        removed: Boolean = true,
        new: Boolean = true,
        mode: IslandMode = IslandMode.Notification,
        islandOnly: Boolean = false,
        recording: Boolean = false
    ) = PostedNotificationRules.shouldPlayIslandSound(initial, removed, new, mode, islandOnly, recording)

    @Test
    fun notificationSoundPolicy() {
        assertTrue(sound())

        assertFalse(sound(removed = false))

        assertFalse(sound(new = false))
        assertFalse(sound(initial = true))

        assertFalse(sound(mode = IslandMode.Music))
        assertTrue(sound(mode = IslandMode.Music, islandOnly = true))
        assertTrue(sound(mode = IslandMode.IncomingCall, islandOnly = true))

        assertFalse(sound(mode = IslandMode.DownloadUpload, islandOnly = true))
        assertFalse(sound(recording = true))
        assertFalse(sound(mode = IslandMode.ScreenRecording, islandOnly = true, recording = true))
    }

    @Test fun musicReplacementPerApp() {
        assertTrue(PostedNotificationRules.isReplacedMusicCard(IslandMode.Music, "p", "old", "p", "new"))
        assertFalse(PostedNotificationRules.isReplacedMusicCard(IslandMode.Music, "p", "new", "p", "new"))
        assertFalse(PostedNotificationRules.isReplacedMusicCard(IslandMode.Music, "q", "old", "p", "new"))
        assertFalse(PostedNotificationRules.isReplacedMusicCard(IslandMode.Notification, "p", "old", "p", "new"))
    }

    @Test
    fun transferEndStates() {
        assertTrue(PostedNotificationRules.isTransferDone(true, false, 0, 0))
        assertTrue(PostedNotificationRules.isTransferDone(false, true, 0, 0))
        assertTrue(PostedNotificationRules.isTransferDone(false, false, 100, 100))
        assertTrue(PostedNotificationRules.isTransferDone(false, false, 120, 100))
        assertFalse(PostedNotificationRules.isTransferDone(false, false, 40, 100))
        assertFalse(PostedNotificationRules.isTransferDone(false, false, 0, 0))
        assertFalse(PostedNotificationRules.isTransferDone(false, false, 5, 0))
    }


    @Test fun lingerFailedImportedNormal() {
        assertEquals(IslandTimings.TRANSFER_FAILED_MS, PostedNotificationRules.transferLingerMs(true, true))
        assertEquals(IslandTimings.TRANSFER_FAILED_MS, PostedNotificationRules.transferLingerMs(true, false))
        assertEquals(IslandTimings.TRANSFER_DONE_IMPORTED_MS, PostedNotificationRules.transferLingerMs(false, true))
        assertEquals(IslandTimings.TRANSFER_DONE_MS, PostedNotificationRules.transferLingerMs(false, false))
    }

    @Test fun timerDoneWhenShadeStopped() {
        assertTrue(PostedNotificationRules.isTimerDone(true, null))
        assertTrue(PostedNotificationRules.isTimerDone(true, 0L))
        assertFalse(PostedNotificationRules.isTimerDone(true, 3_000L))
        assertFalse(PostedNotificationRules.isTimerDone(false, null))
        assertFalse(PostedNotificationRules.isTimerDone(false, 0L))
    }
}

class SuppressedKeysTest {
    private var now = 1_000L
    private fun keys(ttlMs: Long = 100L, maxKeys: Int = 3) = SuppressedKeys({ now }, ttlMs, maxKeys)

    @Test
    fun markedKeyIsHeldWithItsTime() {
        val k = keys()
        k.mark("a")
        assertTrue(k.contains("a"))
        assertEquals(1_000L, k.lastMarked("a"))
    }

    @Test
    fun unknownKeyReadsAsZero() {
        assertEquals(0L, keys().lastMarked("missing"))
    }

    @Test
    fun keyExpiresAfterTtl() {
        val k = keys()
        k.mark("a")
        now += 101
        assertFalse(k.contains("a"))
    }

    @Test
    fun lastMarkedDoesNotSweepExpiredKeys() {
        val k = keys()
        k.mark("a")
        now += 500
        assertEquals(1_000L, k.lastMarked("a"))
    }

    @Test
    fun keyAtTheTtlBoundaryStays() {
        val k = keys()
        k.mark("a")
        now += 100
        assertTrue(k.contains("a"))
    }

    @Test
    fun oldestKeysGoWhenOverTheCap() {
        val k = keys()
        for (key in listOf("a", "b", "c", "d")) {
            k.mark(key)
            now += 1
        }
        assertEquals(3, k.size)
        assertFalse(k.contains("a"))
        assertTrue(k.contains("d"))
    }

    @Test
    fun markingAgainRefreshesTheTime() {
        val k = keys()
        k.mark("a")
        now += 60
        k.mark("a")
        now += 60
        assertTrue(k.contains("a"))
    }

    @Test
    fun removeAndClearDropKeys() {
        val k = keys()
        k.mark("a")
        k.mark("b")
        k.remove("a")
        assertFalse(k.contains("a"))
        k.clear()
        assertEquals(0, k.size)
    }
}
