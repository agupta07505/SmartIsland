/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Test

class ShizukuManagerTest {

    @Test
    fun testIsInstalledWhenNotPresent() {
        val context = mockk<Context>()
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.getPackageInfo("moe.shizuku.privileged.api", 0) } throws RuntimeException("Not installed")

        val installed = ShizukuManager.isInstalled(context)
        assertFalse(installed)
    }

    @Test
    fun testIsBinderAvailableWithoutCrash() {
        // Without Shizuku service running, isBinderAvailable should return false safely
        val available = ShizukuManager.isBinderAvailable()
        assertFalse(available)
    }

    @Test
    fun testHasPermissionWithoutCrash() {
        // Without Shizuku service running, hasPermission should return false safely
        val hasPerm = ShizukuManager.hasPermission()
        assertFalse(hasPerm)
    }
}
