/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
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
    }

    @Test
    fun testSuppressedWhenPackageInDisabledSet() {
        val mockPm = mockk<PackageManager>()
        val mockSbn = mockk<StatusBarNotification>()
        every { mockSbn.packageName } returns "com.whatsapp"

        val disabledPackages = setOf("com.whatsapp")
        val isSuppressed = NotificationFilter.shouldSuppressFromIsland(
            sbn = mockSbn,
            packageManager = mockPm,
            disabledNotificationPackages = disabledPackages
        )

        assertTrue(isSuppressed)
    }
}
