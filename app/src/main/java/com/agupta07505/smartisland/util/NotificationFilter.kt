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
import com.agupta07505.smartisland.model.IslandMode

object NotificationFilter {
    private val SYSTEM_LEVEL_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.agupta07505.smartisland"
    )

    fun shouldSuppressFromIsland(
        sbn: StatusBarNotification,
        packageManager: PackageManager
    ): Boolean {
        val packageName = sbn.packageName
        if (packageName in SYSTEM_LEVEL_PACKAGES) return true

        val notification = sbn.notification
        if (isSystemLevelCategory(notification)) return true
        if (isSystemLevelPackage(packageName, packageManager)) return true

        // Suppress group summary notifications (they are handled separately in the service:
        // cancelled from the system shade but never shown in the island)
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) return true

        // Suppress if both title and text are null or blank
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return true

        // Suppress ongoing notifications that are not calls and not media/music playback
        val isOngoing = (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0
        if (isOngoing) {
            val mode = notification.toIslandMode()
            val isProgressNotification = notification.category == Notification.CATEGORY_PROGRESS
            if (!isProgressNotification && mode != IslandMode.IncomingCall && mode != IslandMode.Music) {
                return true
            }
        }

        return false
    }

    /**
     * Returns true if the package belongs to a third-party (user-installed) app, meaning it is
     * not a system-level package. Used to decide whether a group summary should be cancelled
     * from the system shade.
     */
    fun isThirdPartyApp(packageName: String, packageManager: PackageManager): Boolean {
        return !isSystemLevelPackage(packageName, packageManager)
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

fun Notification.toIslandMode(): IslandMode {
    val isCallStyle = extras?.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$CallStyle"
    val actionLabels = actions.orEmpty().map { it.title?.toString()?.lowercase().orEmpty() }
    val hasIncomingCallActionPair =
        actionLabels.any { it.contains("answer") } &&
            actionLabels.any {
                it.contains("decline") ||
                    it.contains("reject") ||
                    it.contains("hang up")
            }
    val hasMediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
    val hasMediaAction = actionLabels.any {
        it.contains("play") ||
            it.contains("pause") ||
            it.contains("next") ||
            it.contains("previous")
    }

    return when {
        // Missed calls are historical notifications, not active incoming calls.
        category == Notification.CATEGORY_CALL || isCallStyle || hasIncomingCallActionPair -> {
            IslandMode.IncomingCall
        }

        // CATEGORY_PROGRESS is used by downloads, uploads, and other progress work.
        // Only classify action-based media notifications when a media session exists.
        category == Notification.CATEGORY_TRANSPORT ||
            hasMediaSession -> IslandMode.Music

        else -> IslandMode.Notification
    }
}
