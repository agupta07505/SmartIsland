/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuRuntimePermissionsTest {
    @Test
    fun grantsNearbyDevicesAndNotifications() {
        val commands = ShizukuManager.runtimePermissionCommands("com.example")
        assertTrue("pm grant com.example android.permission.BLUETOOTH_CONNECT" in commands)
        assertTrue("pm grant com.example android.permission.POST_NOTIFICATIONS" in commands)
    }

    @Test
    fun neverGrantsLocationOrVpn() {
        val all = ShizukuManager.runtimePermissionCommands("com.example").joinToString(" ")
        ShizukuManager.OPTIONAL_PERMISSIONS.forEach { assertFalse(all.contains(it)) }
        assertFalse(all.contains("VPN", ignoreCase = true))
    }
}
