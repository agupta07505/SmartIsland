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
    fun messageAndMailCategoriesCount() {
        assertTrue(NotificationCounts.isCounted(entry("1", "x.chat", category = "msg")))
        assertTrue(NotificationCounts.isCounted(entry("2", "x.mail", category = "email")))
    }

    @Test
    fun messagingStyleCountsWithoutACategory() {
        assertTrue(NotificationCounts.isCounted(entry("1", "x.chat", style = true)))
    }

    @Test
    fun knownMessagingAppsCountWithoutCategoryOrStyle() {
        assertTrue(NotificationCounts.isCounted(entry("1", "com.whatsapp")))
        assertTrue(NotificationCounts.isCounted(entry("2", "com.google.android.gm")))
    }

    @Test
    fun otherAppsDoNotCount() {
        assertFalse(NotificationCounts.isCounted(entry("1", "com.example.shop")))
        assertFalse(NotificationCounts.isCounted(entry("2", "com.example.shop", category = "promo")))
    }

    @Test
    fun ongoingAndGroupSummariesNeverCount() {
        assertFalse(NotificationCounts.isCounted(entry("1", "com.whatsapp", category = "msg", ongoing = true)))
        assertFalse(NotificationCounts.isCounted(entry("2", "com.whatsapp", category = "msg", summary = true)))
    }

    @Test
    fun aCallFromAMessagingAppIsNotAMessage() {
        assertFalse(NotificationCounts.isCounted(entry("1", "com.whatsapp", category = "call")))
        assertFalse(NotificationCounts.isCounted(entry("2", "org.telegram.messenger", category = "progress")))
    }

    @Test
    fun summarizeCountsPerAppBiggestFirst() {
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
    }

    @Test
    fun summarizeOfNothingIsEmpty() {
        assertEquals(NotificationSummary.Empty, NotificationCounts.summarize(emptyList()))
        assertEquals(0, NotificationCounts.summarize(listOf(entry("1", "com.example.shop"))).total)
    }

    @Test
    fun mergeCountsAKeyOnceAndKeepsIslandOnlyCards() {
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
    fun rowsListThreeAppsAndSumUpTheRest() {
        val apps = (1..5).map { AppCount("p$it", "App$it", 6 - it) } // 5,4,3,2,1
        val rows = NotificationCounts.rows(NotificationSummary(apps.sumOf { it.count }, apps))
        assertEquals(3, rows.shown.size)
        assertEquals(2, rows.moreApps)
        assertEquals(3, rows.moreNotifications)
    }

    @Test
    fun rowsHaveNoMoreLineWhenEverythingFits() {
        val apps = listOf(AppCount("p1", "A", 2), AppCount("p2", "B", 1))
        val rows = NotificationCounts.rows(NotificationSummary(3, apps))
        assertEquals(0, rows.moreApps)
        assertEquals(0, rows.moreNotifications)
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
