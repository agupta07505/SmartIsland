/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.os.Build
import com.agupta07505.smartisland.util.runCatchingLogged

internal object PostedAppInfo {
    private const val TAG = "SmartIslandNotificationListener"

    /** The posting app's [ApplicationInfo] that the framework puts in the notification's extras. */
    fun of(notification: Notification): ApplicationInfo? =
        runCatchingLogged(TAG, "Notification app info unreadable") {
            // Notification.EXTRA_BUILDER_APPLICATION_INFO is hidden API, so the key is spelled out
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notification.extras.getParcelable("android.appInfo", ApplicationInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                notification.extras.getParcelable("android.appInfo") as? ApplicationInfo
            }
        }

    /** The name of a sub-app that posts through this package (Quick Share through Google Play services), if any. */
    fun subAppName(notification: Notification, appName: String): String? =
        notification.extras.getString("android.substName")?.trim()?.takeIf { it.isNotEmpty() && it != appName }
}
