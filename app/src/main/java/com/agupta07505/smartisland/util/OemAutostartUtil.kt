/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OemAutostartUtil {

    /**
     * Attempts to open manufacturer-specific Autostart / Background App Management screen
     * to prevent OEM power managers (MIUI Powerkeeper, ColorOS, Samsung Care, Vivo)
     * from killing background accessibility services.
     * 100% crash-proof against un-mocked JVM stubs and missing OEM intents.
     */
    fun openAutostartSettings(context: Context, deviceType: String? = null): Boolean {
        return try {
            val device = OemDeviceRules.resolveEffectiveDevice(deviceType)
            val intents = OemDeviceRules.getAutostartIntents(context, device)

            for (intent in intents) {
                if (context.safeStartActivity(intent, errorMessage = null)) {
                    return true
                }
            }

            false
        } catch (_: Throwable) {
            false
        }
    }
}
