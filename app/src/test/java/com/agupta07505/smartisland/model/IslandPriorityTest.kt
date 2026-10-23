/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandPriorityTest {

    private fun card(key: String, mode: IslandMode, timeMillis: Long = 0L) = IslandNotification(
        key = key,
        packageName = "pkg.$key",
        appName = key,
        title = key,
        text = "",
        timeMillis = timeMillis,
        mode = mode
    )

    @Test
    fun ordersNavigationLiveMusicDownloadsNotifications() {
        val list = listOf(
            card("note", IslandMode.Notification),
            card("download", IslandMode.DownloadUpload),
            card("music", IslandMode.Music),
            card("live", IslandMode.LiveActivity),
            card("nav", IslandMode.Navigation)
        )
        assertEquals(
            listOf("nav", "live", "music", "download", "note"),
            IslandPriority.order(list).map { it.key }
        )
    }

    @Test
    fun equalRanksKeepArrivalOrder() {
        val list = listOf(
            card("a", IslandMode.Notification),
            card("b", IslandMode.Notification),
            card("music", IslandMode.Music)
        )
        assertEquals(listOf("music", "a", "b"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun secondPriorityIsTheSecondCard() {
        val list = listOf(card("note", IslandMode.Notification), card("music", IslandMode.Music), card("nav", IslandMode.Navigation))
        val ordered = IslandPriority.order(list)
        assertEquals("nav", ordered[0].key)
        assertEquals("music", ordered[1].key)
    }

    @Test
    fun popupGoesFirstWhenNothingIsLive() {
        val list = listOf(card("a", IslandMode.Notification, 200L), card("b", IslandMode.Notification, 100L))
        assertEquals(listOf("b", "a"), IslandPriority.order(list, "b").map { it.key })
    }

    @Test
    fun aNotificationNeverTakesThePrimaryCutoutFromALiveEvent() {
        val list = listOf(card("nav", IslandMode.Navigation), card("music", IslandMode.Music), card("note", IslandMode.Notification))
        assertEquals(listOf("nav", "music", "note"), IslandPriority.order(list, "note").map { it.key })
    }

    @Test
    fun unknownPopupKeyIsIgnored() {
        val list = listOf(card("note", IslandMode.Notification), card("nav", IslandMode.Navigation))
        assertEquals(listOf("nav", "note"), IslandPriority.order(list, "gone").map { it.key })
    }

    @Test
    fun onlyShortEventsPopUp() {
        assertTrue(IslandPriority.popsUp(IslandMode.Notification))
        assertTrue(IslandPriority.popsUp(IslandMode.Battery))
        assertTrue(IslandPriority.popsUp(IslandMode.Bluetooth))
        assertFalse(IslandPriority.popsUp(IslandMode.Music))
        assertFalse(IslandPriority.popsUp(IslandMode.Navigation))
        assertFalse(IslandPriority.popsUp(IslandMode.DownloadUpload))
    }

    @Test
    fun playingMusicOutranksPausedMusic() {
        val paused = card("paused", IslandMode.Music).copy(mediaIsPlaying = false)
        val playing = card("playing", IslandMode.Music).copy(mediaIsPlaying = true)
        val download = card("download", IslandMode.DownloadUpload)
        assertEquals(
            listOf("playing", "paused", "download"),
            IslandPriority.order(listOf(paused, download, playing)).map { it.key }
        )
    }

    @Test
    fun ordinaryNotificationsAreNewestFirst() {
        val list = listOf(
            card("old", IslandMode.Notification, 100L),
            card("newest", IslandMode.Notification, 300L),
            card("mid", IslandMode.Notification, 200L)
        )
        assertEquals(listOf("newest", "mid", "old"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun newestFirstDoesNotMoveOtherKindsOrBreakPriority() {
        val list = listOf(
            card("old", IslandMode.Notification, 100L),
            card("battery", IslandMode.Battery, 150L),
            card("new", IslandMode.Notification, 300L),
            card("music", IslandMode.Music, 50L)
        )
        // Music (a live event) still leads, and the battery event takes the secondary cutout right behind it.
        assertEquals(listOf("music", "battery", "new", "old"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun batteryTakesTheSecondaryCutoutBehindALiveEvent() {
        val list = listOf(
            card("note", IslandMode.Notification, 100L),
            card("battery", IslandMode.Battery),
            card("timer", IslandMode.Timer),
            card("music", IslandMode.Music)
        )
        assertEquals(listOf("timer", "battery", "music", "note"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun notificationsKeepThePrimaryCutoutAndBatteryTakesTheSecondaryWhenNothingIsLive() {
        val list = listOf(
            card("note", IslandMode.Notification, 100L),
            card("other", IslandMode.Notification, 200L),
            card("battery", IslandMode.Battery)
        )
        assertEquals(listOf("other", "battery", "note"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun aBatteryEventNeverPopsOverALiveEvent() {
        val nav = card("nav", IslandMode.Navigation)
        val battery = card("battery", IslandMode.Battery)
        val note = card("note", IslandMode.Notification)
        assertFalse(IslandPriority.popsUp(battery, listOf(nav, battery)))
        assertTrue(IslandPriority.popsUp(battery, listOf(note, battery)))
        assertFalse(IslandPriority.popsUp(note, listOf(nav, note)))
        assertTrue(IslandPriority.popsUp(note, listOf(note, battery)))
        // Even if its popup key is set, the live event keeps the primary cutout.
        assertEquals(listOf("nav", "battery"), IslandPriority.order(listOf(nav, battery), "battery").map { it.key })
    }

    @Test
    fun liveEventsAreTheOnesRankedAboveNotifications() {
        assertTrue(IslandPriority.isLive(IslandMode.IncomingCall))
        assertTrue(IslandPriority.isLive(IslandMode.Music))
        assertTrue(IslandPriority.isLive(IslandMode.DownloadUpload))
        assertFalse(IslandPriority.isLive(IslandMode.Notification))
        assertFalse(IslandPriority.isLive(IslandMode.Battery))
        assertFalse(IslandPriority.isLive(IslandMode.Bluetooth))
        assertFalse(IslandPriority.isLive(IslandMode.Empty))
    }

    @Test
    fun equalTimesKeepArrivalOrder() {
        val list = listOf(card("a", IslandMode.Notification, 5L), card("b", IslandMode.Notification, 5L))
        assertEquals(listOf("a", "b"), IslandPriority.order(list).map { it.key })
    }
}
