/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OemAutostartUtilTest {

    @Test
    fun testOpenAutostartSettingsFallback() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.agupta07505.smartisland"
        every { context.startActivity(any()) } answers { }

        val result = OemAutostartUtil.openAutostartSettings(context)
        assertNotNull(result)
    }

    @Test
    fun backgroundLockStepOnlyOnTheThreeAggressiveSkins() {
        assertTrue(OemIntentFactory.needsBackgroundLockStep(OemDeviceType.XIAOMI_REDMI_POCO))
        assertTrue(OemIntentFactory.needsBackgroundLockStep(OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemIntentFactory.needsBackgroundLockStep(OemDeviceType.VIVO_IQOO))
        listOf(OemDeviceType.SAMSUNG, OemDeviceType.GOOGLE_PIXEL, OemDeviceType.GENERIC, OemDeviceType.AUTO)
            .forEach { assertFalse(it.name, OemIntentFactory.needsBackgroundLockStep(it)) }
    }

    @Test
    fun miuiOpensAutostartFirstThenThePerAppBatterySaver() {
        val targets = OemIntentFactory.backgroundTargets(OemDeviceType.XIAOMI_REDMI_POCO)
        assertEquals("com.miui.permcenter.autostart.AutoStartManagementActivity", targets.first().className)
        val saver = targets.last()
        assertEquals("com.miui.powerkeeper", saver.packageName)
        assertTrue(saver.passPackage)
    }

    @Test
    fun colorOsAndVivoHaveVendorPanelsInPreferenceOrder() {
        val oppo = OemIntentFactory.backgroundTargets(OemDeviceType.REALME_OPPO_ONEPLUS)
        assertEquals("com.coloros.safecenter", oppo.first().packageName)
        assertTrue(oppo.any { it.packageName == "com.oneplus.security" })
        val vivo = OemIntentFactory.backgroundTargets(OemDeviceType.VIVO_IQOO)
        assertEquals("com.vivo.permissionmanager.activity.BgStartUpManagerActivity", vivo.first().className)
        assertTrue(OemIntentFactory.backgroundTargets(OemDeviceType.GOOGLE_PIXEL).isEmpty())
    }

    @Test
    fun backgroundLockFallsBackToAppInfoWithoutVendorPanels() {
        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns "com.agupta07505.smartisland"
        every { context.startActivity(any()) } answers { }

        assertTrue(OemAutostartUtil.openBackgroundLockSettings(context, OemDeviceType.GOOGLE_PIXEL.name))
        io.mockk.verify(exactly = 1) { context.startActivity(any()) }
    }
}
