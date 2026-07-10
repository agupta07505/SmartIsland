/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.toIslandMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPriorityTest {

    @org.junit.Before
    fun setUp() {
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @org.junit.After
    fun tearDown() {
        io.mockk.unmockkStatic(android.util.Log::class)
    }

    @Test
    fun testShouldIgnoreForSmartIslandSystemCategory() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        
        notification.category = Notification.CATEGORY_SYSTEM
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.app"

        assertTrue(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun testShouldIgnoreForSmartIslandSystemPackage() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.android.systemui"

        assertTrue(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun testShouldIgnoreForSmartIslandSystemFlagPackage() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        val appInfo = mockk<ApplicationInfo>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.android.customsettings"
        
        appInfo.flags = ApplicationInfo.FLAG_SYSTEM
        every { pm.getApplicationInfo("com.android.customsettings", 0) } returns appInfo

        assertTrue(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun testNonSystemIsNotIgnored() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        val appInfo = mockk<ApplicationInfo>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        notification.flags = 0
        val extras = mockk<android.os.Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Title"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Text"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        notification.extras = extras
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.chat"
        
        appInfo.flags = 0
        every { pm.getApplicationInfo("com.example.chat", 0) } returns appInfo

        assertFalse(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun testNonSystemPackageInfoFailureIsNotIgnored() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        notification.flags = 0
        val extras = mockk<android.os.Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Title"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Text"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        notification.extras = extras
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.chat"
        
        every { pm.getApplicationInfo("com.example.chat", 0) } throws PackageManager.NameNotFoundException()

        assertFalse(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun testOngoingProgressNotificationIsNotIgnored() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        val appInfo = mockk<ApplicationInfo>()
        val extras = mockk<android.os.Bundle>()

        notification.category = Notification.CATEGORY_PROGRESS
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.extras = extras
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Downloading"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "42%"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        notification.actions = null
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.downloader"
        appInfo.flags = 0
        every { pm.getApplicationInfo("com.example.downloader", 0) } returns appInfo

        assertFalse(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun testGenericMediaLabelsWithoutMediaSessionRemainNotifications() {
        val testLabels = listOf("PAUSE", "Next Track", "PLAY", "previous song")
        for (label in testLabels) {
            val notification = mockk<Notification>()
            notification.category = Notification.CATEGORY_MESSAGE
            val action = mockk<Notification.Action>()
            action.title = label
            notification.actions = arrayOf(action)

            val mode = notification.toIslandMode()
            assertEquals(IslandMode.Notification, mode)
        }
    }
}
