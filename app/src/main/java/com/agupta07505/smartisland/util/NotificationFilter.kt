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
import android.service.notification.StatusBarNotification

object NotificationFilter {
    private val SYSTEM_LEVEL_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    fun shouldSuppressFromIsland(
        sbn: StatusBarNotification,
        packageManager: PackageManager
    ): Boolean {
        val notification = sbn.notification
        if (isSystemLevelCategory(notification)) return true
        if (isSystemLevelPackage(sbn.packageName, packageManager)) return true
        return false
    }

    private fun isSystemLevelCategory(notification: Notification): Boolean {
        return notification.category == Notification.CATEGORY_SYSTEM ||
            notification.category == Notification.CATEGORY_STATUS ||
            notification.category == Notification.CATEGORY_SERVICE ||
            notification.category == Notification.CATEGORY_ERROR
    }

    private fun isSystemLevelPackage(packageName: String, packageManager: PackageManager): Boolean {
        if (packageName in SYSTEM_LEVEL_PACKAGES) return true
        val flags = runCatchingLogged("NotificationFilter", "Failed to get flags for package $packageName") {
            packageManager.getApplicationInfo(packageName, 0).flags
        } ?: 0
        return (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
            (packageName.startsWith("android") || packageName.startsWith("com.android."))
    }
}
