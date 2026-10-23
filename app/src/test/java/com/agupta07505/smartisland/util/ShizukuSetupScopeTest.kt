/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuSetupScopeTest {
    private val pkg = "com.example"

    private fun commands(scope: ShizukuManager.SetupScope) = ShizukuManager.setupCommands(
        scope = scope,
        pkg = pkg,
        accessibilityClass = "$pkg/A",
        notificationClass = "$pkg/N",
        mergedAccessibilityServices = "$pkg/A",
        mergedNotificationListeners = "$pkg/N"
    )

    @Test
    fun necessaryCoversWhatTheIslandNeedsToWork() {
        val all = commands(ShizukuManager.SetupScope.Necessary).joinToString("\n")
        listOf(
            "ACCESS_RESTRICTED_SETTINGS", "SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS", "BIND_ACCESSIBILITY_SERVICE",
            "enabled_accessibility_services", "allow_listener", "RECEIVE_SENSITIVE_NOTIFICATIONS",
            "POST_NOTIFICATIONS", "BLUETOOTH_CONNECT", "deviceidle whitelist"
        ).forEach { assertTrue("missing $it", all.contains(it)) }
    }

    @Test
    fun restrictedSettingsComesFirst() {
        assertTrue(commands(ShizukuManager.SetupScope.Necessary).first().contains("ACCESS_RESTRICTED_SETTINGS"))
    }

    @Test
    fun necessaryLeavesOutTheOptionalPermissions() {
        val all = commands(ShizukuManager.SetupScope.Necessary).joinToString("\n")
        listOf("LOCATION", "allow_dnd", "GET_USAGE_STATS").forEach {
            assertFalse("unexpected $it", all.contains(it))
        }
    }

    @Test
    fun allIsTheNecessaryOnesPlusTheOptionalOnes() {
        val necessary = commands(ShizukuManager.SetupScope.Necessary)
        val everything = commands(ShizukuManager.SetupScope.All)
        assertEquals(necessary, everything.take(necessary.size))
        assertEquals(ShizukuManager.optionalCommands(pkg), everything.drop(necessary.size))
    }

    @Test
    fun optionalCoversUsageDndAndLocation() {
        val optional = ShizukuManager.optionalCommands(pkg).joinToString("\n")
        listOf("GET_USAGE_STATS", "allow_dnd", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION").forEach {
            assertTrue("missing $it", optional.contains(it))
        }
    }
}
