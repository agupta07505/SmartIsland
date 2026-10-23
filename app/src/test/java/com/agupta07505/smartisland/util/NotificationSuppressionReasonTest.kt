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
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [NotificationFilter.suppressionReason] names the rule; [NotificationFilter.shouldSuppressFromIsland] must stay
 * exactly "reason != null" so the diagnostic log never disagrees with the real decision.
 */
class NotificationSuppressionReasonTest {

    private fun message(
        pkg: String,
        flags: Int = 0,
        category: String = Notification.CATEGORY_MESSAGE,
        template: String? = null
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
    fun systemLevelPackageListIsNamed() {
        val reason = NotificationFilter.suppressionReason(message("com.android.systemui"), pm())
        assertEquals("system-level-package-list", reason)
    }

    @Test
    fun ordinaryMessageHasNoReason() {
        assertNull(NotificationFilter.suppressionReason(message("com.whatsapp"), pm()))
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
    fun ordinaryGroupSummaryIsStillDropped() {
        val sbn = message("com.whatsapp", flags = Notification.FLAG_GROUP_SUMMARY)
        assertEquals("group-summary", NotificationFilter.suppressionReason(sbn, pm()))
    }

    @Test
    fun callNotificationIsRecognisedByCategoryOrCallStyle() {
        assertTrue(NotificationFilter.isCallNotification(message("com.whatsapp", category = Notification.CATEGORY_CALL).notification))
        assertTrue(
            NotificationFilter.isCallNotification(
                message("com.whatsapp", template = "android.app.Notification\$CallStyle").notification
            )
        )
        assertFalse(NotificationFilter.isCallNotification(message("com.whatsapp").notification))
    }
}
