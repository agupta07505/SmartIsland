/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.Notification
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
    fun cardPriorityOrder() {
        val ringingList = listOf(
            card("note", IslandMode.Notification),
            card("music", IslandMode.Music),
            card("nav", IslandMode.Navigation),
            card("call", IslandMode.IncomingCall)
        )
        assertEquals(listOf("call", "nav", "music", "note"), IslandPriority.order(ringingList).map { it.key })

        val paused = card("paused", IslandMode.Music).copy(mediaIsPlaying = false)
        val playing = card("playing", IslandMode.Music).copy(mediaIsPlaying = true)
        val download = card("download", IslandMode.DownloadUpload)
        assertEquals(
            listOf("playing", "paused", "download"),
            IslandPriority.order(listOf(paused, download, playing)).map { it.key }
        )

        val newestList = listOf(
            card("old", IslandMode.Notification, 100L),
            card("newest", IslandMode.Notification, 300L),
            card("mid", IslandMode.Notification, 200L)
        )
        assertEquals(listOf("newest", "mid", "old"), IslandPriority.order(newestList).map { it.key })

        val rankedList = listOf(
            card("old", IslandMode.Notification, 100L),
            card("battery", IslandMode.Battery, 150L),
            card("new", IslandMode.Notification, 300L),
            card("music", IslandMode.Music, 50L)
        )
        // music (a live event) still leads, and the battery event takes the secondary cutout right behind it
        assertEquals(listOf("music", "battery", "new", "old"), IslandPriority.order(rankedList).map { it.key })

        val batteryList = listOf(
            card("note", IslandMode.Notification, 100L),
            card("battery", IslandMode.Battery),
            card("timer", IslandMode.Timer),
            card("music", IslandMode.Music)
        )
        assertEquals(listOf("timer", "battery", "music", "note"), IslandPriority.order(batteryList).map { it.key })
    }

    @Test
    fun shortEventsShareTheNotificationRank() {
        val rank = IslandPriority.rank(IslandMode.Notification)
        assertEquals(rank, IslandPriority.rank(IslandMode.Battery))
        assertEquals(rank, IslandPriority.rank(IslandMode.Bluetooth))
        assertTrue(IslandPriority.rank(IslandMode.DownloadUpload) < rank)
    }

    @Test
    fun orderIsPlainPriority() {
        val list = listOf(card("bluetooth", IslandMode.Bluetooth), card("note", IslandMode.Notification, 100L))
        assertEquals(listOf("bluetooth", "note"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun notificationsKeepPrimaryAndBatteryTakesSecondary() {
        val list = listOf(
            card("note", IslandMode.Notification, 100L),
            card("other", IslandMode.Notification, 200L),
            card("battery", IslandMode.Battery)
        )
        assertEquals(listOf("other", "battery", "note"), IslandPriority.order(list).map { it.key })
    }

    @Test
    fun liveRanksAboveNotifications() {
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

    @Test
    fun openingBesideLiveCardRevealsStoredCards() {
        val recording = listOf(card("rec", IslandMode.ScreenRecording))
        val notes = listOf(card("note", IslandMode.Notification))
        assertTrue(IslandPriority.revealsStoredOnOpen(recording, hasStoredAway = true))
        assertFalse(IslandPriority.revealsStoredOnOpen(recording, hasStoredAway = false))
        assertFalse(IslandPriority.revealsStoredOnOpen(notes, hasStoredAway = true))
    }

    @Test
    fun tapPutAwayStaysForPausableCardsOnly() {
        assertTrue(IslandPriority.staysAwayAfterTap(card("m", IslandMode.Music)))
        assertTrue(IslandPriority.staysAwayAfterTap(card("s", IslandMode.Stopwatch)))
        assertTrue(IslandPriority.staysAwayAfterTap(card("t", IslandMode.Timer).copy(timerRemainingSeconds = 30L)))
        assertFalse(IslandPriority.staysAwayAfterTap(card("t", IslandMode.Timer).copy(timerRemainingSeconds = 0L)))
        assertTrue(IslandPriority.staysAwayAfterTap(card("d", IslandMode.DownloadUpload).copy(transferState = TransferState.PAUSED)))
        assertFalse(IslandPriority.staysAwayAfterTap(card("d", IslandMode.DownloadUpload).copy(transferState = TransferState.FAILED)))
        assertFalse(IslandPriority.staysAwayAfterTap(card("c", IslandMode.IncomingCall)))
        assertFalse(IslandPriority.staysAwayAfterTap(card("n", IslandMode.Notification)))
    }

    @Test
    fun finishedTimerReentry() {
        // the island's own timer never fills timerRemainingSeconds, the flag says it ran out
        assertFalse(IslandPriority.staysAwayAfterTap(card("t", IslandMode.Timer).copy(timerFinished = true)))
        assertTrue(IslandPriority.staysAwayAfterTap(card("t", IslandMode.Timer)))
    }

    private fun stateCard(key: String, mode: IslandMode, category: String? = null) =
        card(key, mode).copy(category = category)

    @Test
    fun pulseRankKeepsTiersApart() {
        assertEquals(0, IslandPriority.pulseRank(card("c", IslandMode.IncomingCall)))
        assertEquals(10, IslandPriority.pulseRank(card("nav", IslandMode.Navigation)))
        assertEquals(15, IslandPriority.pulseRank(stateCard("b", IslandMode.Battery, "battery_low")))
        assertEquals(15, IslandPriority.pulseRank(stateCard("b", IslandMode.Battery, "battery_saver")))
        assertEquals(15, IslandPriority.pulseRank(stateCard("w", IslandMode.Bluetooth, "wifi_disconnected")))
        assertEquals(15, IslandPriority.pulseRank(stateCard("v", IslandMode.Bluetooth, "vpn_disconnected")))
        assertEquals(15, IslandPriority.pulseRank(stateCard("a", IslandMode.Bluetooth, "airplane_on")))
        assertEquals(15, IslandPriority.pulseRank(stateCard("a", IslandMode.Bluetooth, "airplane_off")))
        assertEquals(15, IslandPriority.pulseRank(card("t", IslandMode.Timer).copy(timerFinished = true)))
        assertEquals(20, IslandPriority.pulseRank(card("t", IslandMode.Timer)))
        assertEquals(20, IslandPriority.pulseRank(card("h", IslandMode.Hotspot)))
        assertEquals(30, IslandPriority.pulseRank(card("m", IslandMode.Music).copy(mediaIsPlaying = true)))
        assertEquals(31, IslandPriority.pulseRank(card("m", IslandMode.Music)))
        assertEquals(40, IslandPriority.pulseRank(card("d", IslandMode.DownloadUpload)))
        assertEquals(50, IslandPriority.pulseRank(card("n", IslandMode.Notification)))
        assertEquals(45, IslandPriority.pulseRank(stateCard("b", IslandMode.Battery, "battery_plugged_in")))
        assertEquals(45, IslandPriority.pulseRank(stateCard("b", IslandMode.Battery, "battery_plugged_out")))
        assertEquals(45, IslandPriority.pulseRank(stateCard("b", IslandMode.Battery, "battery_full")))
        assertEquals(45, IslandPriority.pulseRank(stateCard("u", IslandMode.Bluetooth, "usb_connected")))
        assertEquals(45, IslandPriority.pulseRank(stateCard("h", IslandMode.Bluetooth, "headphones_disconnected")))
        assertEquals(60, IslandPriority.pulseRank(stateCard("w", IslandMode.Bluetooth, "wifi")))
        assertEquals(60, IslandPriority.pulseRank(stateCard("r", IslandMode.Bluetooth, "ringer_silent")))
    }

    @Test
    fun cardRankIsUnchangedByThePulseTiers() {
        assertEquals(50, IslandPriority.rank(stateCard("b", IslandMode.Battery, "battery_low")))
        assertEquals(50, IslandPriority.rank(stateCard("b", IslandMode.Battery, "battery_plugged_in")))
        assertEquals(20, IslandPriority.rank(card("t", IslandMode.Timer).copy(timerFinished = true)))
    }
}

class NotificationCountsTest {
    private fun entry(
        key: String,
        pkg: String,
        label: String = pkg,
        category: String? = null,
        style: Boolean = false,
        ongoing: Boolean = false,
        summary: Boolean = false
    ) = CountEntry(key, pkg, label, category, style, ongoing, summary)

    @Test
    fun countedNotifications() {
        val cases = listOf(
            entry("1", "x.chat", category = "msg") to true,
            entry("2", "x.mail", category = "email") to true,
            entry("1", "x.chat", style = true) to true,
            entry("1", "com.whatsapp") to true,
            entry("2", "com.google.android.gm") to true,
            entry("1", "com.example.shop") to false,
            entry("2", "com.example.shop", category = "promo") to false,
            entry("1", "com.whatsapp", category = "call") to false,
            entry("2", "org.telegram.messenger", category = "progress") to false
        )
        for ((notification, expected) in cases) {
            assertEquals("$notification", expected, NotificationCounts.isCounted(notification))
        }
    }

    @Test
    fun ongoingAndGroupSummariesNeverCount() {
        assertFalse(NotificationCounts.isCounted(entry("1", "com.whatsapp", category = "msg", ongoing = true)))
        assertFalse(NotificationCounts.isCounted(entry("2", "com.whatsapp", category = "msg", summary = true)))
    }

    @Test
    fun notificationSummary() {
        val summary = NotificationCounts.summarize(
            listOf(
                entry("a", "com.google.android.gm", "Gmail"),
                entry("b", "com.whatsapp", "WhatsApp"),
                entry("c", "com.whatsapp", "WhatsApp"),
                entry("d", "com.whatsapp", "WhatsApp"),
                entry("e", "org.telegram.messenger", "Telegram"),
                entry("f", "com.example.shop", "Shop")
            )
        )
        assertEquals(5, summary.total)
        assertEquals(listOf("WhatsApp" to 3, "Gmail" to 1, "Telegram" to 1), summary.apps.map { it.label to it.count })

        assertEquals(NotificationSummary.Empty, NotificationCounts.summarize(emptyList()))
        assertEquals(0, NotificationCounts.summarize(listOf(entry("1", "com.example.shop"))).total)

        val shade = listOf(entry("a", "com.whatsapp"), entry("b", "com.whatsapp"))
        val island = listOf(entry("b", "com.whatsapp"), entry("c", "com.whatsapp"))
        val merged = NotificationCounts.merge(shade, island)
        assertEquals(listOf("a", "b", "c"), merged.map { it.key })
        assertEquals(3, NotificationCounts.summarize(merged).total)
    }

    @Test
    fun badgeTextCapsAtNinetyNine() {
        assertEquals("", NotificationCounts.badgeText(0))
        assertEquals("7", NotificationCounts.badgeText(7))
        assertEquals("99", NotificationCounts.badgeText(99))
        assertEquals("99+", NotificationCounts.badgeText(100))
    }

    @Test
    fun notificationCountRows() {
        val overflowApps = (1..5).map { AppCount("p$it", "App$it", 6 - it) } // 5,4,3,2,1
        val overflowRows = NotificationCounts.rows(NotificationSummary(overflowApps.sumOf { it.count }, overflowApps))
        assertEquals(3, overflowRows.shown.size)
        assertEquals(2, overflowRows.moreApps)
        assertEquals(3, overflowRows.moreNotifications)

        val exactApps = listOf(AppCount("p1", "A", 2), AppCount("p2", "B", 1))
        val exactRows = NotificationCounts.rows(NotificationSummary(3, exactApps))
        assertEquals(0, exactRows.moreApps)
        assertEquals(0, exactRows.moreNotifications)
    }

    @Test
    fun islandHeightGrowsWithRowsAndTheMoreLine() {
        val one = NotificationCounts.islandContentHeightDp(NotificationRows(listOf(AppCount("a", "A", 1)), 0, 0))
        val three = NotificationCounts.islandContentHeightDp(NotificationRows(List(3) { AppCount("a$it", "A", 1) }, 0, 0))
        val threeMore = NotificationCounts.islandContentHeightDp(NotificationRows(List(3) { AppCount("a$it", "A", 1) }, 2, 1))
        assertTrue(one < three)
        assertTrue(three < threeMore)
    }
}
