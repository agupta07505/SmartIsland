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
        packageManager: PackageManager,
        liveActivitiesEnabled: Boolean = true,
        navigationEnabled: Boolean = true,
        disabledNotificationPackages: Set<String> = emptySet(),
        deviceType: String? = null
    ): Boolean {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val mode = notification.toIslandMode(sbn, liveActivitiesEnabled, navigationEnabled, deviceType)

        // Hotspot, Screen Recording, Timer, Stopwatch notifications are allowed even if posted by system/OEM frameworks
        if (mode == IslandMode.Hotspot || mode == IslandMode.ScreenRecording || mode == IslandMode.IncomingCall || mode == IslandMode.Timer || mode == IslandMode.Stopwatch) {
            if (packageName == "com.agupta07505.smartisland") return true
            if (packageName in disabledNotificationPackages) return true
        } else {
            if (packageName in SYSTEM_LEVEL_PACKAGES) return true
            if (packageName in disabledNotificationPackages) return true
            if (isSystemLevelCategory(notification)) return true
            if (isSystemLevelPackage(packageName, packageManager)) return true
        }

        // Suppress group summary notifications (they are handled separately in the service:
        // cancelled from the system shade but never shown in the island)
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) return true

        // Suppress if both title and text are null or blank (except for dedicated hardware/clock modes)
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: notification.tickerText?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank() && mode != IslandMode.Stopwatch && mode != IslandMode.Timer && mode != IslandMode.ScreenRecording) return true

        // Suppress external torch / flashlight notifications from entering Smart Island,
        // because Smart Island natively manages physical torch state via CameraManager.TorchCallback.
        val titleText = "$title $text".lowercase()
        val isTorchNotification = listOf("flashlight", "torch", "flash light").any { titleText.contains(it) }
        if (isTorchNotification && packageName != "com.agupta07505.smartisland") {
            return true
        }

        val isOngoing = (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0

        // Suppress background message syncing / polling notifications (e.g. Snapchat, WhatsApp, Telegram "Syncing messages", "Checking for messages")
        val isMessageSync = isMessageSyncNotification(titleText)
        if (isOngoing && isMessageSync) {
            return true
        }

        // Suppress ongoing notifications that are not calls, media/music playback, live activities, navigation, downloads/uploads, hotspot, screen recording, timer, or stopwatch
        if (isOngoing) {
            val isProgressNotification = !isMessageSync && (
                notification.category == Notification.CATEGORY_PROGRESS ||
                (notification.extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0) > 0
            )
            if (!isProgressNotification && mode != IslandMode.IncomingCall && mode != IslandMode.Music && mode != IslandMode.LiveActivity && mode != IslandMode.Navigation && mode != IslandMode.DownloadUpload && mode != IslandMode.Hotspot && mode != IslandMode.ScreenRecording && mode != IslandMode.Timer && mode != IslandMode.Stopwatch) {
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

    /**
     * Returns true if the app package is eligible to be configured and shown in Smart Island.
     * System-level packages and sensitive system settings (like com.android.settings) return false.
     */
    fun isAppEligibleForIsland(packageName: String, packageManager: PackageManager): Boolean {
        if (packageName in SYSTEM_LEVEL_PACKAGES) return false
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

        // User-facing apps (Chrome, DownloadManager, Play Store) are not internal OS components
        if (packageName == "com.android.chrome" ||
            packageName == "com.android.providers.downloads" ||
            packageName == "com.android.vending"
        ) {
            return false
        }

        val flags = runCatchingLogged("NotificationFilter", "Failed to get flags for package $packageName") {
            packageManager.getApplicationInfo(packageName, 0).flags
        } ?: 0
        val isSystemFlag = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (!isSystemFlag) return false

        // System-level packages are internal OS frameworks & UI, not browser or user apps
        return packageName == "android" ||
            packageName.startsWith("com.android.systemui") ||
            packageName.startsWith("com.android.settings") ||
            packageName.startsWith("com.android.keyguard") ||
            packageName.startsWith("com.android.permissioncontroller") ||
            packageName.startsWith("com.android.shell")
    }
}

fun Notification.toIslandMode(
    sbn: StatusBarNotification? = null,
    liveActivitiesEnabled: Boolean = true,
    navigationEnabled: Boolean = true,
    deviceType: String? = null
): IslandMode {
    val packageName = sbn?.packageName.orEmpty()
    val effectiveDevice = OemDeviceRules.resolveEffectiveDevice(deviceType)

    val titleText = "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)} ${extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)}".lowercase()

    // 0. Timer & Stopwatch (Dedicated Clock/Timer support)
    if (TimerStopwatchParser.isStopwatch(this, packageName)) {
        return IslandMode.Stopwatch
    }
    if (TimerStopwatchParser.isTimer(this, packageName)) {
        return IslandMode.Timer
    }

    // 1. Screen & Voice Recording (High priority, OEM-aware)
    val isRecordingPackageOrKeyword = OemDeviceRules.isScreenRecording(packageName, titleText, effectiveDevice)
    val isScreenRecordingActive = isRecordingPackageOrKeyword && !isScreenRecordingComplete()
    if (isScreenRecordingActive) {
        return IslandMode.ScreenRecording
    }

    // 2. Hotspot & Tethering status (OEM-aware)
    val isHotspot = OemDeviceRules.isHotspot(packageName, titleText)
    if (isHotspot) {
        return IslandMode.Hotspot
    }

    // 3. Incoming & Ongoing Phone Calls (OEM-aware)
    val isCallStyle = extras?.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$CallStyle"
    val actionLabels = actions.orEmpty().map { it.title?.toString()?.lowercase().orEmpty() }
    val hasIncomingCallActionPair =
        actionLabels.any { it.contains("answer") || it.contains("accept") || it.contains("take") } &&
            actionLabels.any {
                it.contains("decline") ||
                    it.contains("reject") ||
                    it.contains("hang up") ||
                    it.contains("dismiss")
            }
    val isInCallApp = OemDeviceRules.isInCallPackage(packageName, effectiveDevice)
    val isCallEvent = (category == Notification.CATEGORY_CALL || isCallStyle || hasIncomingCallActionPair || (isInCallApp && hasIncomingCallActionPair)) && !isCallEnded()
    if (isCallEvent) {
        return IslandMode.IncomingCall
    }

    // 4. Turn-by-Turn Map Navigation
    if (navigationEnabled && sbn != null) {
        if (NavigationParser.parse(sbn) != null) {
            return IslandMode.Navigation
        }
    }

    // 5. Live Activities (Food delivery, ride-hailing)
    if (liveActivitiesEnabled && sbn != null) {
        if (LiveActivityParser.parse(sbn) != null) {
            return IslandMode.LiveActivity
        }
    }

    // 6. Downloads, Uploads & Progress Transfers
    val isMessageSync = isMessageSyncNotification(titleText)
    val isProgressCategory = category == Notification.CATEGORY_PROGRESS
    val progressMax = extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0
    val isIndeterminate = extras?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) == true
    val activeTransferKeywords = listOf("downloading", "uploading", "exporting", "transferring", "saving file", "downloading file", "uploading file").any { titleText.contains(it) }
    val genericTransferKeywords = listOf("download", "upload", "export", "transfer", "file", "apk", "pdf", "mp4", "zip", "media").any { titleText.contains(it) }
    val isDownloadOrUpload = !isMessageSync && (
        isProgressCategory ||
        (progressMax > 0 && (genericTransferKeywords || activeTransferKeywords)) ||
        (isIndeterminate && (genericTransferKeywords || activeTransferKeywords))
    )
    if (isDownloadOrUpload) {
        return IslandMode.DownloadUpload
    }

    // 7. Media & Music Playback
    val hasMediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
    if (category == Notification.CATEGORY_TRANSPORT || hasMediaSession) {
        return IslandMode.Music
    }

    return IslandMode.Notification
}

fun Notification.isDownloadComplete(): Boolean {
    val extras = extras ?: return false
    val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
    val progressCurrent = extras.getInt(Notification.EXTRA_PROGRESS, 0)
    val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

    if (progressMax > 0 && progressCurrent >= progressMax) return true

    val titleText = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)} ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}".lowercase()
    val completionKeywords = listOf(
        "complete", "completed", "finished", "downloaded", "uploaded", "exported",
        "saved", "successful", "successfully", "done", "tap to open", "tap to view",
        "file saved", "sent successfully"
    )
    if (completionKeywords.any { titleText.contains(it) }) return true

    // If progress bar is gone (progressMax == 0 && !isIndeterminate) and text does not say "downloading/uploading", it's complete
    val isCurrentlyActiveText = listOf("downloading", "uploading", "exporting", "transferring", "saving", "fetching", "sending").any { titleText.contains(it) }
    if (progressMax == 0 && !isIndeterminate && !isCurrentlyActiveText) {
        return true
    }

    return false
}

fun Notification.isScreenRecordingComplete(): Boolean {
    val extras = extras ?: return false
    val titleText = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)} ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}".lowercase()
    return OemDeviceRules.isScreenRecordingComplete(this, titleText)
}

fun Notification.isCallEnded(): Boolean {
    val extras = extras ?: return false
    val titleText = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)} ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}".lowercase()
    val isOngoing = (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0

    val callEndedKeywords = listOf(
        "call ended", "call finished", "call declined", "call rejected",
        "missed call", "missed video call", "call disconnected", "hung up",
        "call duration", "ended"
    )
    if (callEndedKeywords.any { titleText.contains(it) }) return true
    if (!isOngoing && category == Notification.CATEGORY_CALL) return true

    return false
}

fun isMessageSyncNotification(text: String): Boolean {
    val syncPhrases = listOf(
        "syncing messages", "syncing message", "checking for messages", "checking for new messages",
        "syncing chats", "syncing chat", "updating messages", "updating chat", "updating chats",
        "waiting for messages", "waiting for message", "looking for messages", "looking for new messages",
        "connecting to chat", "connecting to messages", "refreshing messages", "refreshing chats",
        "sync in progress", "message sync", "chat sync", "syncing...", "checking messages",
        "checking message", "syncing snaps", "checking snaps", "syncing snapchat", "synchronizing messages",
        "synchronizing chats", "synchronizing...", "backup in progress", "syncing backup",
        "checking for updates", "checking for chats", "connecting..."
    )
    return syncPhrases.any { text.contains(it) }
}
