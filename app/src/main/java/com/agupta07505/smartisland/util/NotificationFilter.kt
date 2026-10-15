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
import com.agupta07505.smartisland.model.TransferKind

object NotificationFilter {
    private val SYSTEM_LEVEL_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.bluetooth",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.agupta07505.smartisland"
    )

    /**
     * System packages whose transfer cards are still wanted: the Bluetooth stack posts the
     * progress notification of an incoming or outgoing file transfer.
     */
    private val TRANSFER_ALLOWED_SYSTEM_PACKAGES = setOf("com.android.bluetooth")

    fun shouldSuppressFromIsland(
        sbn: StatusBarNotification,
        packageManager: PackageManager,
        liveActivitiesEnabled: Boolean = true,
        navigationEnabled: Boolean = true,
        disabledNotificationPackages: Set<String> = emptySet(),
        deviceType: String? = null
    ): Boolean = suppressionReason(
        sbn, packageManager, liveActivitiesEnabled, navigationEnabled, disabledNotificationPackages, deviceType
    ) != null

    /**
     * The rule that keeps a notification off the island, or null when it may be shown. Same rules,
     * in the same order, as [shouldSuppressFromIsland] (which is defined by this function); the
     * name is what the diagnostic log prints so a missed notification can be traced to its rule.
     */
    fun suppressionReason(
        sbn: StatusBarNotification,
        packageManager: PackageManager,
        liveActivitiesEnabled: Boolean = true,
        navigationEnabled: Boolean = true,
        disabledNotificationPackages: Set<String> = emptySet(),
        deviceType: String? = null
    ): String? {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val mode = notification.toIslandMode(sbn, liveActivitiesEnabled, navigationEnabled, deviceType)

        // Hotspot, Screen Recording, Timer, Stopwatch notifications are allowed even if posted by system/OEM frameworks
        if (mode == IslandMode.Hotspot || mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording || mode == IslandMode.IncomingCall || mode == IslandMode.Timer || mode == IslandMode.Stopwatch) {
            if (packageName == "com.agupta07505.smartisland") return "own-package"
            if (packageName in disabledNotificationPackages) return "app-disabled-in-settings"
        } else {
            // Transfers (downloads, backups, file operations) often come from foreground services
            // with the SERVICE or STATUS category, and Bluetooth file transfers from the Bluetooth
            // stack itself, so those two checks are relaxed for them.
            val isTransfer = mode == IslandMode.DownloadUpload
            val transferFromAllowedSystem = isTransfer && packageName in TRANSFER_ALLOWED_SYSTEM_PACKAGES
            if (packageName in SYSTEM_LEVEL_PACKAGES && !transferFromAllowedSystem) return "system-level-package-list"
            if (packageName in disabledNotificationPackages) return "app-disabled-in-settings"
            if (isSystemLevelCategory(notification, isTransfer)) return "system-level-category(${notification.category})"
            if (!transferFromAllowedSystem && isSystemLevelPackage(packageName, packageManager)) return "system-level-package"
        }

        // Suppress group summary notifications (they are handled separately in the service:
        // cancelled from the system shade but never shown in the island)
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        // WhatsApp posts its ringing / ongoing call as a CallStyle notification that carries the
        // group-summary flag (group "call_notification_group"); dropping it hid every WhatsApp call.
        if (isGroupSummary && !isCallNotification(notification)) return "group-summary"

        // Suppress if both title and text are null or blank (except for dedicated hardware/clock modes)
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: notification.tickerText?.toString()
        // Chat and inbox styles can leave title/text empty and keep their content in other extras.
        val hasPlainContent = !title.isNullOrBlank() || !text.isNullOrBlank()
        val hasStyledContent = !hasPlainContent && extras != null && (
            !extras.getCharSequence(Notification.EXTRA_TITLE_BIG).isNullOrBlank() ||
                !extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE).isNullOrBlank() ||
                !extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).isNullOrBlank() ||
                (extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.isNotEmpty() == true) ||
                (extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.isNotEmpty() == true)
            )
        // A CallStyle call keeps the caller in its person extra and may leave title and text empty.
        val isCallWithPerson = mode == IslandMode.IncomingCall && CallStyleExtras.callerName(notification) != null
        if (!hasStyledContent && !isCallWithPerson && title.isNullOrBlank() && text.isNullOrBlank() && mode != IslandMode.Stopwatch && mode != IslandMode.Timer && mode != IslandMode.ScreenRecording && mode != IslandMode.AudioRecording) return "empty-title-and-text"

        // Suppress external torch / flashlight notifications from entering Smart Island,
        // because Smart Island natively manages physical torch state via CameraManager.TorchCallback.
        val titleText = "$title $text".lowercase()
        val isTorchNotification = listOf("flashlight", "torch", "flash light").any { titleText.contains(it) }
        if (isTorchNotification && packageName != "com.agupta07505.smartisland") {
            return "torch-notification"
        }

        val isOngoing = (notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0

        // Suppress background message syncing / polling notifications (e.g. Snapchat, WhatsApp, Telegram "Syncing messages", "Checking for messages")
        // A backup that says "backup in progress" is a transfer card (toIslandMode already decided that), not a message poll:
        // (WhatsApp's chat backup, for one, must reach the island.)
        val isMessageSync = isMessageSyncNotification(titleText) && mode != IslandMode.DownloadUpload
        if (isMessageSync) {
            return "message-sync-text"
        }

        // Suppress wearable / watch connection and sync status notifications
        if (isWearableConnectionNotification(packageName, titleText)) {
            return "wearable-connection"
        }

        // Suppress ongoing notifications that are not calls, media/music playback, live activities, navigation, downloads/uploads, hotspot, screen recording, timer, stopwatch, or alarms
        if (isOngoing) {
            val isProgressNotification = !isMessageSync && (
                notification.category == Notification.CATEGORY_PROGRESS ||
                (notification.extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0) > 0
            )
            val isAlarmNotification = TimerStopwatchParser.isAlarm(notification, packageName)
            if (!isProgressNotification && !isAlarmNotification && mode != IslandMode.IncomingCall && mode != IslandMode.Music && mode != IslandMode.LiveActivity && mode != IslandMode.Navigation && mode != IslandMode.DownloadUpload && mode != IslandMode.Hotspot && mode != IslandMode.ScreenRecording && mode != IslandMode.AudioRecording && mode != IslandMode.Timer && mode != IslandMode.Stopwatch) {
                return "ongoing-not-a-live-card"
            }
        }

        return null
    }

    /** True for a call notification: category "call" or a CallStyle template. Such a post may carry the group-summary flag and must still reach the island. */
    fun isCallNotification(notification: Notification): Boolean =
        notification.category == Notification.CATEGORY_CALL ||
            notification.extras?.getString(Notification.EXTRA_TEMPLATE)?.endsWith("CallStyle") == true

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

    /**
     * Returns true if the notification is a connection status or sync notification
     * from a wearable/smartwatch companion app or Bluetooth sync event.
     */
    fun isWearableConnectionNotification(packageName: String, titleText: String): Boolean {
        val isWearablePackage = packageName.startsWith("com.samsung.android.app.watchmanager") ||
            packageName.startsWith("com.samsung.accessory") ||
            packageName.startsWith("com.samsung.android.gear") ||
            packageName.startsWith("com.samsung.android.waterplugin") ||
            packageName.startsWith("com.google.android.wearable") ||
            packageName.contains("watchmanager") ||
            packageName.contains("gearplugin")

        val connectionPhrases = listOf(
            "connected", "connecting", "disconnected", "disconnecting", "paired",
            "syncing", "sync complete", "bluetooth connection"
        )

        if (isWearablePackage && connectionPhrases.any { titleText.contains(it) }) {
            return true
        }

        val watchConnectionPhrases = listOf(
            "watch connected", "connected to watch", "watch disconnected", "disconnected from watch",
            "connected via bluetooth", "connected to galaxy watch", "galaxy watch connected",
            "pixel watch connected", "connected to your watch", "watch is connected",
            "wearable connected", "connected to wearable", "device connected via bluetooth"
        )

        return watchConnectionPhrases.any { titleText.contains(it) }
    }

    private fun isSystemLevelCategory(notification: Notification, isTransfer: Boolean = false): Boolean {
        if (notification.category == Notification.CATEGORY_SYSTEM ||
            notification.category == Notification.CATEGORY_ERROR
        ) {
            return true
        }
        if (isTransfer) return false
        return notification.category == Notification.CATEGORY_STATUS ||
            notification.category == Notification.CATEGORY_SERVICE
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
    val clockKey = sbn?.let { TimerStopwatchParser.clockKey(it) }
    if (TimerStopwatchParser.isStopwatch(this, packageName, clockKey)) {
        return IslandMode.Stopwatch
    }
    if (TimerStopwatchParser.isTimer(this, packageName, clockKey)) {
        return IslandMode.Timer
    }

    // 1a. A voice recorder's cloud backup uses the recorder's own notification and would read as a
    // recording in progress. Treat it as a transfer before the recording rules run.
    val recorderHasProgress = (extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0) > 0
    if (TransferClassifier.isRecorderTransfer(packageName, titleText, recorderHasProgress)) {
        return IslandMode.DownloadUpload
    }

    // 1. Screen & Audio Recording (High priority, OEM-aware). Audio recorders get their own mode.
    if (OemDeviceRules.isAudioRecording(packageName, titleText) && !isScreenRecordingComplete()) {
        return IslandMode.AudioRecording
    }
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
    val isCallStyle = CallStyleExtras.isCallTemplate(this)
    val actionLabels = actions.orEmpty().map { it.title?.toString()?.lowercase().orEmpty() }
    val roles = actionLabels.map { VoipCallDetector.classifyCallAction(it) }
    // The answer side matches whole words, so "Retake" or "Mistake" do not read as "take".
    val hasIncomingCallActionPair = CallActionRole.Answer in roles &&
        actionLabels.any {
            it.contains("decline") ||
                it.contains("reject") ||
                it.contains("hang up") ||
                it.contains("dismiss")
        }
    val isInCallApp = OemDeviceRules.isInCallPackage(packageName, effectiveDevice)
    // While the system is in a VoIP call (audio mode), an ongoing notification that looks like a
    // call (mute / hang-up buttons, chronometer, call wording) is that call's card, even when the
    // app set no call category.
    val isVoipCall = VoipCallSignal.active && sbn != null &&
        VoipCallDetector.isVoipCall(toVoipFacts(packageName))
    // Answer + hang-up buttons in any supported language, not only English. Only trusted together
    // with a call signal, because words like "accept" or "responder" also label invites and replies.
    val hasLocalizedCallPair = CallActionRole.Answer in roles && CallActionRole.HangUp in roles &&
        (category == Notification.CATEGORY_CALL || isCallStyle || fullScreenIntent != null)
    // A call-named channel (voip_notification, incoming_calls ...) marks a VoIP app's own call
    // notification when it sets neither a call category nor CallStyle. Needs a ring or call signal
    // too, so a channel that only happens to contain "call" cannot claim ordinary notifications.
    val isCallChannel = CallStyleExtras.channelLooksLikeCall(this) &&
        (fullScreenIntent != null || CallStyleExtras.looksRinging(this) || roles.any { it != CallActionRole.Other } ||
            VoipCallDetector.textLooksLikeCall(titleText))
    val isCallEvent = (category == Notification.CATEGORY_CALL || isCallStyle || hasIncomingCallActionPair || hasLocalizedCallPair ||
        isCallChannel || (isInCallApp && hasIncomingCallActionPair) || isVoipCall) && !isCallEnded()
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

    // 5b. A media player always wins over the transfer scoring below. Video apps, popup /
    // picture-in-picture players and background-play clients keep a progress bar plus Pause / Stop
    // buttons, which would otherwise read as "Downloading".
    val isMediaPlayer = category == Notification.CATEGORY_TRANSPORT ||
        extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true ||
        MediaTransportDetector.hasTransportButtons(actions.orEmpty().map { it.title?.toString().orEmpty() })
    if (isMediaPlayer) {
        return IslandMode.Music
    }

    // 6. Downloads, Uploads & Progress Transfers. TransferDetector scores every signal: progress
    // bar, CATEGORY_PROGRESS, Android 16 progress style, wording in many languages, speed/size/ETA
    // text, channel names, download managers, cloud apps, Pause/Cancel buttons.
    // A "backup in progress" or a determinate bar is a real transfer even if the wording also
    // looks like a messenger's background sync.
    val progressMax = extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0
    val isMessageSync = isMessageSyncNotification(titleText) && progressMax <= 0 && !titleText.contains("backup")
    if (TransferDetector.evaluate(TransferInput.fromNotification(this, packageName, isMessageSync)).isTransfer) {
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
    if (TransferClassifier.COMPLETE_WORDS_I18N.any { titleText.contains(it) }) return true
    // Past-tense words also appear in running counters ("3 of 10 backed up"), so they only count
    // once the progress bar is gone.
    val stillRunning = (progressMax > 0 && progressCurrent < progressMax) || isIndeterminate
    val pastTenseKeywords = listOf("backed up", "synced", "copied", "moved", "deleted", "installed", "restored")
    if (!stillRunning && pastTenseKeywords.any { titleText.contains(it) }) return true

    // No bar (Android 16 progress style, or an app that only writes "45% - 2 MB/s" in the text):
    // a speed, an ETA or a percentage below 100 means it is still running; 100% means it is done.
    val facts = TransferTextAnalyzer.analyze(titleText)
    if (progressMax == 0 && !isIndeterminate) {
        val percentNow = facts.percent
        if (percentNow != null && percentNow >= 100) return true
        if (facts.speed != null || facts.eta != null || (percentNow != null && percentNow < 100)) return false
    }

    // If progress bar is gone (progressMax == 0 && !isIndeterminate) and text does not say "downloading/uploading", it's complete
    val isCurrentlyActiveText = (TransferClassifier.ACTIVE_WORDS + TransferClassifier.ACTIVE_WORDS_I18N + listOf("saving", "fetching"))
        .any { titleText.contains(it) }
    if (progressMax == 0 && !isIndeterminate && !isCurrentlyActiveText) {
        return true
    }

    return false
}

/** What kind of transfer a progress notification describes (see TransferClassifier). */
fun Notification.transferKind(packageName: String): TransferKind {
    val extras = extras ?: return TransferKind.DOWNLOAD
    val text = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)} ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}"
    return TransferClassifier.classify(packageName, text)
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
        "missed call", "missed video call", "missed voice call", "missed group call", "call disconnected", "hung up",
        "call duration"
    )
    if (callEndedKeywords.any { titleText.contains(it) }) return true
    // "ended" only as a whole word, so "extended" or "attended" do not end a call.
    if (Regex("""(?<![\p{L}])ended(?![\p{L}])""").containsMatchIn(titleText)) return true

    // A ringing call (WhatsApp, Telegram, Signal ...) is usually a heads-up / full-screen
    // notification that is NOT flagged ongoing, so "not ongoing" must not mean "ended" for it.
    if (CallStyleExtras.looksRinging(this)) return false
    // A running call timer in the notification is a live call, even when the app does not flag it
    // ongoing: the card for it must not be dropped as "ended".
    if (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)) return false
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
