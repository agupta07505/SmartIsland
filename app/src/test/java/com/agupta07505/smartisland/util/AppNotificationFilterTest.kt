/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.IslandMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNotificationFilterTest {

    @Test
    fun testSensitiveSettingsAppIneligible() {
        val mockPm = mockk<PackageManager>()
        assertFalse(NotificationFilter.isAppEligibleForIsland("com.android.settings", mockPm))
        assertFalse(NotificationFilter.isAppEligibleForIsland("com.android.systemui", mockPm))
        assertFalse(NotificationFilter.isAppEligibleForIsland("android", mockPm))
        assertFalse(NotificationFilter.isAppEligibleForIsland("com.agupta07505.smartisland", mockPm))
    }

    @Test
    fun testThirdPartyAppEligible() {
        val mockPm = mockk<PackageManager>()
        assertTrue(NotificationFilter.isAppEligibleForIsland("org.telegram.messenger", mockPm))
        assertTrue(NotificationFilter.isAppEligibleForIsland("com.whatsapp", mockPm))
        assertTrue(NotificationFilter.isAppEligibleForIsland("com.spotify.music", mockPm))
        assertTrue(NotificationFilter.isAppEligibleForIsland("com.android.chrome", mockPm))
    }

    @Test
    fun testSuppressedWhenPackageInDisabledSet() {
        val mockPm = mockk<PackageManager>()
        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = Notification.CATEGORY_MESSAGE
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alice"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Hello"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.whatsapp"
        every { mockSbn.notification } returns mockNotif

        val disabledPackages = setOf("com.whatsapp")
        val isSuppressed = NotificationFilter.shouldSuppressFromIsland(
            sbn = mockSbn,
            packageManager = mockPm,
            disabledNotificationPackages = disabledPackages
        )

        assertTrue(isSuppressed)
    }

    @Test
    fun testNotSuppressedWhenPackageNotInDisabledSet() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = 0 }
        every { mockPm.getApplicationInfo("com.whatsapp", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = Notification.CATEGORY_MESSAGE
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alice"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Hello"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.whatsapp"
        every { mockSbn.notification } returns mockNotif

        val disabledPackages = setOf("org.telegram.messenger")
        val isSuppressed = NotificationFilter.shouldSuppressFromIsland(
            sbn = mockSbn,
            packageManager = mockPm,
            disabledNotificationPackages = disabledPackages
        )

        assertFalse(isSuppressed)
    }

    @Test
    fun testProgressNotificationMapsToDownloadUploadMode() {
        val mockNotif = mockk<Notification>()
        mockNotif.category = Notification.CATEGORY_PROGRESS
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 100
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 45
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "video_2026.mp4"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Downloading..."
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        mockNotif.extras = extras

        assertEquals(IslandMode.DownloadUpload, mockNotif.toIslandMode())
    }

    @Test
    fun testChromeDownloadNotSuppressed() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
        every { mockPm.getApplicationInfo("com.android.chrome", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = Notification.FLAG_ONGOING_EVENT
        mockNotif.category = Notification.CATEGORY_PROGRESS
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "app-release.apk"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Downloading..."
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 100
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 45
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.android.chrome"
        every { mockSbn.notification } returns mockNotif

        assertTrue(NotificationFilter.isAppEligibleForIsland("com.android.chrome", mockPm))
        assertFalse(NotificationFilter.shouldSuppressFromIsland(mockSbn, mockPm))
    }

    @Test
    fun testTorchNotificationSuppression() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
        every { mockPm.getApplicationInfo("com.miui.securitycenter", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = null
        mockNotif.actions = null
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Torch is on"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to turn off"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.miui.securitycenter"
        every { mockSbn.notification } returns mockNotif

        assertTrue(NotificationFilter.shouldSuppressFromIsland(mockSbn, mockPm))
    }
}
