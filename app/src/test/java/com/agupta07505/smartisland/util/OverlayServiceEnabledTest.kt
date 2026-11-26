/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The one check behind the home screen, the listener service, the recovery job and the diagnostics report */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverlayServiceEnabledTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val overlay get() = ComponentName(context.packageName, AppComponents.OVERLAY_SERVICE)

    private fun enabledServices(value: String) {
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, value)
    }

    @Test
    fun offWhenNothingIsStored() {
        assertFalse(context.isOverlayAccessibilityServiceEnabled())
    }

    @Test
    fun onWhenItIsTheOnlyEntry() {
        enabledServices(overlay.flattenToString())
        assertTrue(context.isOverlayAccessibilityServiceEnabled())
    }

    @Test
    fun onWhenListedAmongOthers() {
        enabledServices("com.other/.First:${overlay.flattenToString()}:com.more/.Second")
        assertTrue(context.isOverlayAccessibilityServiceEnabled())
    }

    @Test
    fun onForTheShortFormOfTheSameComponent() {
        enabledServices(overlay.flattenToShortString())
        assertTrue(context.isOverlayAccessibilityServiceEnabled())
    }

    @Test
    fun offForAnotherServiceOfThisApp() {
        enabledServices(ComponentName(context.packageName, AppComponents.LISTENER_SERVICE).flattenToString())
        assertFalse(context.isOverlayAccessibilityServiceEnabled())
    }

    @Test
    fun offForSameClassOtherApp() {
        enabledServices(ComponentName("com.other", AppComponents.OVERLAY_SERVICE).flattenToString())
        assertFalse(context.isOverlayAccessibilityServiceEnabled())
    }

    @Test
    fun offWhenEntriesAreMalformed() {
        enabledServices(":garbage::no-slash:")
        assertFalse(context.isOverlayAccessibilityServiceEnabled())
    }
}
