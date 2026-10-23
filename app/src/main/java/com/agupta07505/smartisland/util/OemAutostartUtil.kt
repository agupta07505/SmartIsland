/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context

object OemAutostartUtil {

    /**
     * Opens the manufacturer's Autostart / background management screen, so OEM power managers (MIUI Powerkeeper,
     * ColorOS, Samsung Care, Vivo) stop killing background accessibility services. Never throws.
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
