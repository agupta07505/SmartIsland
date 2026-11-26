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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNotificationFilterTest {

    @Test
    fun appPackageEligibility() {
        val settingsMockPm = mockk<PackageManager>()
        val texts = listOf(
            "com.android.settings",
            "com.android.systemui",
            "android",
            "com.agupta07505.smartisland"
        )
        for (text in texts) {
            assertFalse(text, NotificationFilter.isAppEligibleForIsland(text, settingsMockPm))
        }

        val thirdPartyMockPm = mockk<PackageManager>()
        for (text in listOf("org.telegram.messenger", "com.whatsapp", "com.spotify.music")) {
            assertTrue(text, NotificationFilter.isAppEligibleForIsland(text, thirdPartyMockPm))
        }
        assertTrue(NotificationFilter.isAppEligibleForIsland("com.android.chrome", thirdPartyMockPm))

        val bluetoothMockPm = mockk<PackageManager>()
        assertFalse(NotificationFilter.isAppEligibleForIsland("com.android.bluetooth", bluetoothMockPm))
    }

    @Test
    fun suppressedWhenPackageInDisabledSet() {
        val mockPm = mockk<PackageManager>()
        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = Notification.CATEGORY_MESSAGE
        val extras = mockk<Bundle>(relaxed = true)
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
    fun notSuppressedOutsideDisabled() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = 0 }
        every { mockPm.getApplicationInfo("com.whatsapp", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = Notification.CATEGORY_MESSAGE
        val extras = mockk<Bundle>(relaxed = true)
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
    fun progressMapsToDownloadMode() {
        val mockNotif = mockk<Notification>()
        mockNotif.category = Notification.CATEGORY_PROGRESS
        val extras = mockk<Bundle>(relaxed = true)
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
    fun chromeDownloadNotSuppressed() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
        every { mockPm.getApplicationInfo("com.android.chrome", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = Notification.FLAG_ONGOING_EVENT
        mockNotif.category = Notification.CATEGORY_PROGRESS
        val extras = mockk<Bundle>(relaxed = true)
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
    fun torchNotificationSuppression() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
        every { mockPm.getApplicationInfo("com.miui.securitycenter", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>(relaxed = true)
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = null
        mockNotif.actions = null
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Torch is on"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to turn off"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.miui.securitycenter"
        every { mockSbn.notification } returns mockNotif
        every { mockSbn.key } returns "torch_key"

        assertTrue(NotificationFilter.shouldSuppressFromIsland(mockSbn, mockPm))
    }

    @Test
    fun downloadCompleteDetection() {
        val completedNotif = mockk<Notification>(relaxed = true)
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 100
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 100
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "file.apk"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Download complete"
        completedNotif.extras = extras

        assertTrue(completedNotif.isDownloadComplete())

        val finishedNoProgressNotif = mockk<Notification>(relaxed = true)
        val extras2 = mockk<Bundle>(relaxed = true)
        every { extras2.getCharSequence(any()) } returns null
        every { extras2.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras2.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras2.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras2.getCharSequence(Notification.EXTRA_TITLE) } returns "File downloaded"
        every { extras2.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to view"
        finishedNoProgressNotif.extras = extras2

        assertTrue(finishedNoProgressNotif.isDownloadComplete())
    }

    @Test
    fun screenRecordingCompleteDetection() {
        val completedNotif = mockk<Notification>(relaxed = true)
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Screen recording"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Screen recording saved"
        completedNotif.flags = 0
        completedNotif.extras = extras

        assertTrue(completedNotif.isScreenRecordingComplete())

        val activeNotif = mockk<Notification>(relaxed = true)
        val extras2 = mockk<Bundle>(relaxed = true)
        every { extras2.getCharSequence(any()) } returns null
        every { extras2.getCharSequence(Notification.EXTRA_TITLE) } returns "Screen recording"
        every { extras2.getCharSequence(Notification.EXTRA_TEXT) } returns "Recording screen..."
        activeNotif.flags = Notification.FLAG_ONGOING_EVENT
        activeNotif.extras = extras2

        assertFalse(activeNotif.isScreenRecordingComplete())
    }

    @Test
    fun callEndedDetection() {
        val endedNotif = mockk<Notification>(relaxed = true)
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Call ended"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Call duration 01:24"
        endedNotif.flags = 0
        endedNotif.category = Notification.CATEGORY_CALL
        endedNotif.extras = extras

        assertTrue(endedNotif.isCallEnded())
    }

    @Test
    fun runningCallTimerNotEnded() {
        val live = mockk<Notification>(relaxed = true)
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Mama"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Ongoing voice call"
        every { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) } returns true
        live.flags = 0   // not flagged ongoing
        live.category = Notification.CATEGORY_CALL
        live.extras = extras

        assertFalse(live.isCallEnded())
    }

    @Test
    fun messageSyncNotDownloadMode() {
        val syncNotif = mockk<Notification>(relaxed = true)
        syncNotif.category = null
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Snapchat"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Syncing messages..."
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns true
        syncNotif.extras = extras

        assertEquals(IslandMode.Notification, syncNotif.toIslandMode())
    }

    @Test
    fun ongoingSyncSuppressed() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = 0 }
        every { mockPm.getApplicationInfo("com.snapchat.android", 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE
        mockNotif.category = null
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Snapchat"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Checking for new messages..."
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns true
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.snapchat.android"
        every { mockSbn.notification } returns mockNotif

        val isSuppressed = NotificationFilter.shouldSuppressFromIsland(
            sbn = mockSbn,
            packageManager = mockPm
        )

        assertTrue(isSuppressed)
    }

    @Test
    fun wearableStatusSuppressed() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = 0 }
        every { mockPm.getApplicationInfo(any(), 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0
        mockNotif.category = null
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(any()) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Galaxy Watch"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Watch connected via Bluetooth"
        mockNotif.extras = extras

        every { mockSbn.packageName } returns "com.samsung.accessory"
        every { mockSbn.notification } returns mockNotif

        val isSuppressed = NotificationFilter.shouldSuppressFromIsland(
            sbn = mockSbn,
            packageManager = mockPm
        )

        assertTrue(isSuppressed)
    }

    @Test
    fun wearableConnectionHelper() {
        assertTrue(NotificationFilter.isWearableConnectionNotification("com.samsung.accessory", "connected"))
        assertTrue(NotificationFilter.isWearableConnectionNotification("com.samsung.android.app.watchmanager", "sync complete"))
        assertTrue(NotificationFilter.isWearableConnectionNotification("com.google.android.wearable.app", "disconnected"))
        assertTrue(NotificationFilter.isWearableConnectionNotification("com.some.app", "galaxy watch connected via bluetooth"))
        assertTrue(NotificationFilter.isWearableConnectionNotification("com.some.app", "pixel watch connected"))
        assertFalse(NotificationFilter.isWearableConnectionNotification("com.whatsapp", "Hey, watch this video!"))
        assertFalse(NotificationFilter.isWearableConnectionNotification("org.telegram.messenger", "I connected my new PC"))
    }

    @Test
    fun syncSuppressedEvenIfNotOngoing() {
        val mockPm = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { flags = 0 }
        every { mockPm.getApplicationInfo(any(), 0) } returns appInfo

        val mockSbn = mockk<StatusBarNotification>()
        val mockNotif = mockk<Notification>()
        mockNotif.flags = 0 // not ongoing
        mockNotif.category = null
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "WhatsApp"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Checking for new messages..."
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_INFO_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        mockNotif.extras = extras
        mockNotif.tickerText = null
        mockNotif.actions = emptyArray()

        every { mockSbn.packageName } returns "com.whatsapp"
        every { mockSbn.notification } returns mockNotif

        val isSuppressed = NotificationFilter.shouldSuppressFromIsland(
            sbn = mockSbn,
            packageManager = mockPm
        )

        assertTrue(isSuppressed)
    }
}

/**
 * [NotificationFilter.suppressionReason] names the rule; [NotificationFilter.shouldSuppressFromIsland] must stay
 * exactly "reason != null" so the diagnostic log never disagrees with the real decision
 */
class NotificationSuppressionReasonTest {

    private fun message(
        pkg: String,
        flags: Int = 0,
        category: String = Notification.CATEGORY_MESSAGE,
        template: String? = null,
        tag: String? = null
    ): StatusBarNotification {
        val notif = mockk<Notification>()
        notif.flags = flags
        notif.category = category
        val extras = mockk<Bundle>(relaxed = true)
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alice"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Hello"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns template
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        notif.extras = extras
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns pkg
        every { sbn.tag } returns tag
        every { sbn.notification } returns notif
        return sbn
    }

    private fun pm(): PackageManager {
        val pm = mockk<PackageManager>()
        every { pm.getApplicationInfo(any<String>(), any<Int>()) } returns ApplicationInfo().apply { flags = 0 }
        return pm
    }

    @Test
    fun disabledPackageIsNamedAsSuch() {
        val reason = NotificationFilter.suppressionReason(
            message("com.whatsapp"), pm(), disabledNotificationPackages = setOf("com.whatsapp")
        )
        assertEquals("app-disabled-in-settings", reason)
    }

    @Test
    fun suppressionReasons() {
        val reason = NotificationFilter.suppressionReason(message("com.android.systemui"), pm())
        assertEquals("system-level-package-list", reason)

        val demo = message("com.agupta07505.smartisland", tag = DemoNotification.TAG)
        assertNull(NotificationFilter.suppressionReason(demo, pm()))

        val other = message("com.agupta07505.smartisland", tag = "other")
        assertEquals("system-level-package-list", NotificationFilter.suppressionReason(other, pm()))
        assertEquals("system-level-package-list", NotificationFilter.suppressionReason(message("com.agupta07505.smartisland"), pm()))

        assertNull(NotificationFilter.suppressionReason(message("com.whatsapp"), pm()))

        val sbn = message("com.whatsapp", flags = Notification.FLAG_GROUP_SUMMARY)
        assertEquals("group-summary", NotificationFilter.suppressionReason(sbn, pm()))
    }

    @Test
    fun booleanAndReasonAlwaysAgree() {
        val cases = listOf(
            message("com.whatsapp") to emptySet<String>(),
            message("com.whatsapp") to setOf("com.whatsapp"),
            message("com.android.systemui") to emptySet(),
            message("com.agupta07505.smartisland") to emptySet(),
            message("org.telegram.messenger") to setOf("org.telegram.messenger")
        )
        for ((sbn, disabled) in cases) {
            val reason = NotificationFilter.suppressionReason(sbn, pm(), disabledNotificationPackages = disabled)
            val suppressed = NotificationFilter.shouldSuppressFromIsland(sbn, pm(), disabledNotificationPackages = disabled)
            assertEquals("pkg=${sbn.packageName}", reason != null, suppressed)
            if (reason == null) assertFalse(suppressed) else assertTrue(reason.isNotBlank() && suppressed)
            assertNotNull(sbn)
        }
    }

    @Test
    fun callRecognisedByCategoryOrStyle() {
        assertTrue(NotificationFilter.isCallNotification(message("com.whatsapp", category = Notification.CATEGORY_CALL).notification))
        assertTrue(
            NotificationFilter.isCallNotification(
                message("com.whatsapp", template = "android.app.Notification\$CallStyle").notification
            )
        )
        assertFalse(NotificationFilter.isCallNotification(message("com.whatsapp").notification))
    }
}
