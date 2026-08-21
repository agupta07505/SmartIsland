/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.util.isCallEnded
import com.agupta07505.smartisland.util.isDownloadComplete
import com.agupta07505.smartisland.util.isScreenRecordingComplete
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode
import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.data.INotificationHistoryRepository
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.NotificationHistoryEntry
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class SmartIslandNotificationListenerService : NotificationListenerService() {
    // Keys we have canceled ourselves to make island-only. Keeps island copy alive.
    private val suppressedKeys = ConcurrentHashMap<String, Long>()
    @Volatile private var currentSettings = SmartIslandSettings.Default
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        android.util.Log.e(TAG, "Unhandled notification-listener coroutine failure", error)
    }
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)
    private val mainScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + coroutineExceptionHandler)

    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    @Inject lateinit var historyRepository: INotificationHistoryRepository

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                repository.settings.collect { settings ->
                    currentSettings = settings
                    if (!settings.enabled || !settings.hideFromNotificationShade) {
                        suppressedKeys.clear()
                    } else {
                        cleanupSuppressedKeys()
                    }
                    if (settings.disabledNotificationPackages.isNotEmpty()) {
                        val currentIslandNotifications = notificationRepository.notifications.value
                        currentIslandNotifications
                            .filter { it.packageName in settings.disabledNotificationPackages }
                            .forEach { notificationRepository.removeNotification(it.key) }
                    }
                }
            }
        }
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Command collector failed") {
                notificationRepository.commands.collect { command ->
                    runCatchingLogged(TAG, "Notification command failed") {
                        when (command) {
                            is SmartIslandCommand.CancelNotification -> {
                                forceCancelNotification(command.key)
                            }
                            is SmartIslandCommand.SeekTo -> {
                                bestControllerFor(command.packageName)
                                    ?.transportControls
                                    ?.seekTo(command.positionMs)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        isSystemConnected = false
        pendingRemovals.values.forEach { it.cancel() }
        pendingRemovals.clear()
        pendingSuppressionJobs.values.forEach { it.cancel() }
        pendingSuppressionJobs.clear()
        suppressedKeys.clear()
        iconCache.evictAll()
        serviceScope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    private val pendingRemovals = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val pendingSuppressionJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatchingLogged(TAG, "onNotificationPosted callback failed") {
        if (sbn.packageName == packageName) return@runCatchingLogged

        val notification = sbn.notification

        // ── Group summary handling ────────────────────────────────────────────────────────────
        // Apps like WhatsApp post two kinds of notifications per conversation:
        //   1. Child notifications  (individual messages) — these are cancelled via the block below
        //   2. A group SUMMARY notification (FLAG_GROUP_SUMMARY) — this is what causes WhatsApp to
        //      still appear in the system shade even after all children have been cancelled.
        //
        // `shouldSuppressFromIsland` correctly returns true for group summaries (so they're never
        // added to the island), but that also prevents the cancellation block below from running,
        // leaving the summary untouched in the system shade.
        //
        // Fix: intercept group summaries for third-party apps and cancel them from the system shade
        // immediately, BEFORE falling through to the normal island-or-ignore logic.
        val isGroupSummary = (notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) {
            if (currentSettings.enabled &&
                currentSettings.hideFromNotificationShade &&
                sbn.packageName !in currentSettings.disabledNotificationPackages &&
                com.agupta07505.smartisland.util.NotificationFilter.isThirdPartyApp(
                    sbn.packageName,
                    packageManager
                )
            ) {
                android.util.Log.d(TAG, "Cancelling group summary from system shade: ${sbn.key} pkg=${sbn.packageName}")
                suppressSystemNotification(sbn.key) // adds to suppressedKeys + cancels with retry
            }
            // Group summaries are never added to the island — stop processing here.
            pendingRemovals.remove(sbn.key)?.cancel()
            return@runCatchingLogged
        }

        // ── Regular notification: suppress from system shade if it belongs in the island ──────
        // We do this synchronously — before the coroutine is even scheduled — so the notification
        // never appears in the system shade. cancelNotification() is used exclusively;
        // snoozeNotification() is deliberately avoided because it moves to a "snoozed" shade
        // section instead of removing the notification entirely.
        try {
            if (currentSettings.enabled &&
                currentSettings.hideFromNotificationShade &&
                !com.agupta07505.smartisland.util.NotificationFilter.shouldSuppressFromIsland(
                    sbn,
                    packageManager,
                    currentSettings.liveActivitiesEnabled,
                    currentSettings.navigationEnabled,
                    currentSettings.disabledNotificationPackages,
                    currentSettings.deviceType
                )
            ) {
                val modeQuick = notification.toIslandMode(
                    sbn,
                    currentSettings.liveActivitiesEnabled,
                    currentSettings.navigationEnabled,
                    currentSettings.deviceType
                )
                if (shouldBeIslandOnly(notification, modeQuick)) {
                    markSuppressed(sbn.key)
                    runCatchingLogged(TAG, "Immediate cancel failed") { cancelNotification(sbn.key) }
                    android.util.Log.d(TAG, "Immediate island-only suppress: ${sbn.key}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Immediate suppress exception", e)
        }

        // Cancel any pending removal job for this key to keep island copy
        pendingRemovals.remove(sbn.key)?.cancel()

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationPosted async failed") {
                if (shouldSuppressFromIsland(sbn)) return@runSuspendCatchingLogged

                val settings = repository.settings.first()
                currentSettings = settings
                if (!settings.enabled) return@runSuspendCatchingLogged

                android.util.Log.d(TAG, "Processing island-only async: key=${sbn.key}")
                handleNotificationPosted(sbn, settings)
            }
        }
        }
    }

    /**
     * Use the reason-aware overload so we can distinguish between:
     *  - REASON_LISTENER_CANCEL: we suppressed it ourselves → keep island copy, keep suppressedKeys
     *  - Any other reason (user dismissed, app canceled, etc.) → remove from island and suppressedKeys
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
        runCatchingLogged(TAG, "onNotificationRemoved callback failed") {
        android.util.Log.d(TAG, "onNotificationRemoved: key=${sbn.key} pkg=${sbn.packageName} reason=$reason")

        pendingRemovals.remove(sbn.key)?.cancel()

        val job = serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationRemoved handling failed") {
                delay(350L)
                if (sbn.packageName == packageName) return@runSuspendCatchingLogged

                val now = SystemClock.elapsedRealtime()
                val lastSuppressedTime = suppressedKeys[sbn.key] ?: 0L
                val isRecentInitialSuppression = (now - lastSuppressedTime) < INITIAL_SUPPRESSION_WINDOW_MS

                if (reason == REASON_LISTENER_CANCEL && isRecentInitialSuppression) {
                    // Smart Island just suppressed this notification from system shade < 1.5s ago.
                    // Keep the island copy alive during initial suppression.
                    android.util.Log.d(TAG, "Recent listener-cancel (<1.5s), keeping island: ${sbn.key}")
                    return@runSuspendCatchingLogged
                }

                // Removed by posting app, user, framework timeout, or after initial suppression window.
                android.util.Log.d(TAG, "Genuinely removed, cleaning up: ${sbn.key}")
                clearSuppressed(sbn.key)
                notificationRepository.removeNotification(sbn.key)
            }
            pendingRemovals.remove(sbn.key)
        }
        pendingRemovals[sbn.key] = job
        }
    }

    // Keep the no-arg override as a fallback (some OEMs may only call this one).
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        runCatchingLogged(TAG, "onNotificationRemoved fallback callback failed") {
        android.util.Log.d(TAG, "onNotificationRemoved (no reason): key=${sbn.key} pkg=${sbn.packageName}")

        pendingRemovals.remove(sbn.key)?.cancel()

        val job = serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationRemoved (no reason) handling failed") {
                delay(350L)
                if (sbn.packageName == packageName) return@runSuspendCatchingLogged

                val now = SystemClock.elapsedRealtime()
                val lastSuppressedTime = suppressedKeys[sbn.key] ?: 0L
                val isRecentInitialSuppression = (now - lastSuppressedTime) < INITIAL_SUPPRESSION_WINDOW_MS

                if (isRecentInitialSuppression) {
                    android.util.Log.d(TAG, "Suppressed key recently (<1.5s), keeping island: ${sbn.key}")
                    return@runSuspendCatchingLogged
                }

                android.util.Log.d(TAG, "Removing from island repo: ${sbn.key}")
                clearSuppressed(sbn.key)
                notificationRepository.removeNotification(sbn.key)
            }
            pendingRemovals.remove(sbn.key)
        }
        pendingRemovals[sbn.key] = job
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isSystemConnected = true
        runCatchingLogged(TAG, "onListenerConnected callback failed") {
            android.util.Log.d(TAG, "onListenerConnected")
            serviceScope.launch {
                runSuspendCatchingLogged(TAG, "ListenerConnected failed") {
                    val settings = repository.settings.first()
                    currentSettings = settings
                    if (!settings.enabled) return@runSuspendCatchingLogged

                    val overlayReady = ensureOverlayServiceRunning()
                    val active = activeNotifications
                        .filter { it.packageName != packageName }
                        .filterNot { shouldSuppressFromIsland(it) }

                    android.util.Log.d(TAG, "ListenerConnected: ${active.size} active, overlayReady=$overlayReady")

                    active.forEach { sbn ->
                        val mode = sbn.notification.toIslandMode(
                            sbn,
                            settings.liveActivitiesEnabled,
                            settings.navigationEnabled,
                            settings.deviceType
                        )
                        if (settings.hideFromNotificationShade &&
                            shouldBeIslandOnly(sbn.notification, mode)
                        ) {
                            suppressSystemNotification(sbn.key)
                        }
                        handleNotificationPosted(sbn, settings)
                    }
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        isSystemConnected = false
        super.onListenerDisconnected()
        runCatchingLogged(TAG, "Notification-listener self-rebind failed") {
            requestRebind(
                android.content.ComponentName(
                    this,
                    SmartIslandNotificationListenerService::class.java
                )
            )
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = android.content.ComponentName(this, SmartIslandOverlayService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = android.text.TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val cn = android.content.ComponentName.unflattenFromString(splitter.next())
            if (cn != null && cn == expected) return true
        }
        return false
    }

    private fun ensureOverlayServiceRunning(): Boolean {
        return isAccessibilityServiceEnabled() || Settings.canDrawOverlays(this)
    }

    private fun isIncomingCall(notification: Notification): Boolean {
        return notification.actions?.any { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("answer") || label.contains("accept") || label.contains("take")
        } == true
    }

    private fun handleNotificationPosted(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings
    ) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification
        if (shouldSuppressFromIsland(sbn)) return

        val extras = notification.extras
        val mode = notification.toIslandMode(
            sbn,
            settings.liveActivitiesEnabled,
            settings.navigationEnabled,
            settings.deviceType
        )
        android.util.Log.d(TAG, "handleNotificationPosted: mode=$mode key=${sbn.key} title=${extras.getCharSequence(Notification.EXTRA_TITLE)}")

        val shouldIslandOnly = shouldBeIslandOnly(notification, mode)

        if (settings.hideFromNotificationShade && shouldIslandOnly) {
            // Ensure the notification is removed from the system shade.
            // - If posted via onNotificationPosted, the synchronous cancel already ran; this
            //   triggers the async retry loop inside suppressSystemNotification for reliability.
            // - If arriving via onListenerConnected, this is the first (and only) suppress call.
            suppressSystemNotification(sbn.key)
        }

        val mediaInfo = if (mode == IslandMode.Music) findMediaInfo(notification, sbn.packageName) else null
        val appName = runCatchingLogged(TAG, "GetApplicationInfo failed") {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } ?: sbn.packageName

        val isNewNotif = notificationRepository.notifications.value.none { it.key == sbn.key }

        val existingNotif = notificationRepository.notifications.value.find { it.key == sbn.key || (it.mode == IslandMode.IncomingCall && it.packageName == sbn.packageName) }
        val actions = notification.actions?.mapNotNull { action ->
            action.title?.toString()?.let { title ->
                val remoteInput = action.remoteInputs?.firstOrNull()
                val isReply = remoteInput != null || title.lowercase().contains("reply")
                IslandNotificationAction(
                    title = title,
                    pendingIntent = action.actionIntent,
                    isQuickReply = isReply,
                    remoteInputKey = remoteInput?.resultKey ?: "key_text_reply"
                )
            }
        }.orEmpty()
        val isNowRinging = actions.any { it.title.lowercase().let { t -> t.contains("answer") || t.contains("accept") || t.contains("take") } }

        val computedTimeMillis = when {
            mode == IslandMode.IncomingCall && existingNotif != null && existingNotif.isCallRinging && !isNowRinging -> {
                System.currentTimeMillis()
            }
            existingNotif != null && mode == IslandMode.IncomingCall && !isNowRinging && existingNotif.timeMillis > 0 -> {
                existingNotif.timeMillis
            }
            mode == IslandMode.Timer -> {
                val remSec = com.agupta07505.smartisland.util.TimerStopwatchParser.parseTimerRemainingSeconds(notification)
                if (remSec != null && remSec > 0) {
                    System.currentTimeMillis() + remSec * 1000L
                } else if (notification.`when` > System.currentTimeMillis()) {
                    notification.`when`
                } else {
                    System.currentTimeMillis()
                }
            }
            mode == IslandMode.Stopwatch -> {
                val elSec = com.agupta07505.smartisland.util.TimerStopwatchParser.parseStopwatchElapsedSeconds(notification)
                if (elSec != null && elSec > 0) {
                    System.currentTimeMillis() - elSec * 1000L
                } else if (existingNotif != null && existingNotif.mode == IslandMode.Stopwatch && existingNotif.timeMillis > 0) {
                    existingNotif.timeMillis
                } else if (notification.`when` in 1..System.currentTimeMillis()) {
                    notification.`when`
                } else {
                    System.currentTimeMillis()
                }
            }
            notification.`when` != 0L -> notification.`when`
            else -> sbn.postTime
        }

        val notifTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: (if (mode == IslandMode.Stopwatch) "Stopwatch" else if (mode == IslandMode.Timer) "Timer" else "")
        val notifText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: notification.tickerText?.toString()
            ?: (if (mode == IslandMode.Stopwatch) "Running" else if (mode == IslandMode.Timer) "Running" else "")

        notificationRepository.postNotification(
            IslandNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = notifTitle,
                text = notifText,
                timeMillis = computedTimeMillis,
                icon = loadAppIconBitmap(sbn.packageName),
                largeIcon = mediaInfo?.artwork ?: notification.loadLargeIconBitmap(),
                actionIntents = actions,
                category = notification.category,
                progress = extras.getInt(Notification.EXTRA_PROGRESS, 0),
                progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
                mediaPositionMs = mediaInfo?.positionMs,
                mediaDurationMs = mediaInfo?.durationMs,
                mediaIsPlaying = mediaInfo?.isPlaying == true,
                mediaToken = runCatchingLogged(TAG, "GetMediaToken failed") {
                    val ex = notification.extras
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        ex.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        ex.getParcelable(Notification.EXTRA_MEDIA_SESSION)
                    }
                },
                mode = mode,
                contentIntent = notification.contentIntent
            ),
            autoExpand = shouldIslandOnly && settings.autoExpandOnNotification
        )

        if (settings.enableNotificationHistory) {
            serviceScope.launch {
                runSuspendCatchingLogged(TAG, "Failed to record notification history") {
                    historyRepository.saveEntry(
                        NotificationHistoryEntry(
                            notificationKey = sbn.key,
                            packageName = sbn.packageName,
                            appName = appName,
                            title = notifTitle,
                            text = notifText,
                            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                            postTimeMillis = computedTimeMillis,
                            category = notification.category,
                            channelId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) notification.channelId else null,
                            mode = mode.name,
                            actionTitles = actions.map { it.title }
                        )
                    )
                    historyRepository.cleanupOldEntries(settings.notificationHistoryRetentionHours)
                }
            }
        }

        if (isNewNotif && (mode == IslandMode.Notification || shouldIslandOnly) && mode != IslandMode.DownloadUpload) {
            playNotificationSound(sbn.packageName)
        }

        if (mode == IslandMode.Music) {
            val existing = notificationRepository.notifications.value
            existing.filter { it.packageName == sbn.packageName && it.key != sbn.key }
                .forEach { notificationRepository.removeNotification(it.key) }
        }

        if (mode == IslandMode.IncomingCall) {
            val isEnded = notification.isCallEnded()
            if (isEnded) {
                pendingRemovals.remove(sbn.key)?.cancel()
                clearSuppressed(sbn.key)
                notificationRepository.removeNotification(sbn.key)
            }
        }

        if (mode == IslandMode.DownloadUpload) {
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
            val isDone = notification.isDownloadComplete() || (progressMax > 0 && progress >= progressMax)
            if (isDone) {
                pendingRemovals.remove(sbn.key)?.cancel()
                val job = serviceScope.launch {
                    delay(3500L)
                    clearSuppressed(sbn.key)
                    notificationRepository.removeNotification(sbn.key)
                }
                pendingRemovals[sbn.key] = job
            }
        }

        if (mode == IslandMode.ScreenRecording) {
            val isDone = notification.isScreenRecordingComplete()
            if (isDone) {
                pendingRemovals.remove(sbn.key)?.cancel()
                val job = serviceScope.launch {
                    delay(3500L)
                    clearSuppressed(sbn.key)
                    notificationRepository.removeNotification(sbn.key)
                }
                pendingRemovals[sbn.key] = job
            }
        }

        if (mode == IslandMode.Timer) {
            val isDone = com.agupta07505.smartisland.util.TimerStopwatchParser.isTimerFinished(notification)
            if (isDone) {
                pendingRemovals.remove(sbn.key)?.cancel()
                val job = serviceScope.launch {
                    delay(5000L)
                    clearSuppressed(sbn.key)
                    notificationRepository.removeNotification(sbn.key)
                }
                pendingRemovals[sbn.key] = job
            }
        }
    }

    internal fun shouldSuppressFromIsland(sbn: StatusBarNotification): Boolean {
        return com.agupta07505.smartisland.util.NotificationFilter.shouldSuppressFromIsland(
            sbn,
            packageManager,
            currentSettings.liveActivitiesEnabled,
            currentSettings.navigationEnabled,
            currentSettings.disabledNotificationPackages,
            currentSettings.deviceType
        )
    }

    internal fun shouldBeIslandOnly(notification: Notification, mode: IslandMode): Boolean {
        if (mode == IslandMode.IncomingCall) {
            if (!isIncomingCall(notification)) return false // ongoing call stays in system
        }
        if (mode == IslandMode.Music || mode == IslandMode.Navigation || mode == IslandMode.Timer || mode == IslandMode.Stopwatch) {
            return false // Media/Music, Navigation, Timer & Stopwatch notifications must NOT be cancelled from system shade by default
        }
        // All others: island-only
        return true
    }

    // Kept for tests
    internal fun isHighPriorityNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val ranking = Ranking()
        val rankingMap = currentRanking
        val isHigh = rankingMap != null && rankingMap.getRanking(sbn.key, ranking) && ranking.importance >= android.app.NotificationManager.IMPORTANCE_HIGH
        return isHigh || notification.fullScreenIntent != null
    }

    /**
     * Suppress a notification from the system shade so it only appears in the island.
     *
     * Uses ONLY cancelNotification() — snoozeNotification() is deliberately avoided
     * because it moves the notification to a "snoozed" section in the system shade
     * rather than removing it, which causes notifications to appear in both the
     * system shade AND the island.
     *
     * Retries up to 3 times with increasing delays for reliability on devices where
     * the first cancel attempt may not take effect immediately.
     */
    private fun suppressSystemNotification(key: String) {
        if (!currentSettings.enabled || !currentSettings.hideFromNotificationShade) return
        val activeSbn = runCatchingLogged(TAG, "Failed to get active notifications for key lookup") {
            activeNotifications.find { it.key == key }
        }
        if (activeSbn != null && activeSbn.packageName in currentSettings.disabledNotificationPackages) return

        val now = SystemClock.elapsedRealtime()
        val lastSuppressedTime = suppressedKeys[key] ?: 0L
        val isRecentlySuppressed = (now - lastSuppressedTime) < 300L

        markSuppressed(key)

        // Cancel any pending suppression retry job for this key
        pendingSuppressionJobs.remove(key)?.cancel()

        // Synchronous attempt for fastest possible suppression (if not throttled)
        if (!isRecentlySuppressed) {
            runCatchingLogged(TAG, "sync cancel failed") { cancelNotification(key) }
        }

        // Asynchronous retry with delays for reliability
        val job = mainScope.launch {
            runSuspendCatchingLogged(TAG, "Notification suppression retries failed") {
                repeat(3) { attempt ->
                    delay(100L * (attempt + 1)) // 100, 200, 300ms
                    if (!currentSettings.enabled ||
                        !currentSettings.hideFromNotificationShade
                    ) {
                        clearSuppressed(key)
                        return@runSuspendCatchingLogged
                    }
                    val stillActive = activeNotifications.any { it.key == key }
                    if (!stillActive) {
                        android.util.Log.d(TAG, "Successfully suppressed after ${attempt + 1} attempts: $key")
                        return@runSuspendCatchingLogged
                    }
                    android.util.Log.d(TAG, "Still active after attempt ${attempt + 1}, retrying: $key")
                    runCatchingLogged(TAG, "cancel retry $attempt failed") {
                        cancelNotification(key)
                    }
                }
                android.util.Log.w(TAG, "Failed to suppress after retries: $key")
            }
            pendingSuppressionJobs.remove(key)
        }
        pendingSuppressionJobs[key] = job
    }

    private fun forceCancelNotification(key: String) {
        // This is an explicit user action, so the island copy must disappear as well.
        clearSuppressed(key)
        runCatchingLogged(TAG, "forceCancel failed") { cancelNotification(key) }
        notificationRepository.removeNotification(key)
    }

    private fun playNotificationSound(packageName: String) {
        if (currentSettings.disabledSoundPackages.contains(packageName)) return
        runCatchingLogged(TAG, "Failed to play notification sound") {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (audioManager?.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) {
                val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, soundUri)
                ringtone?.play()
            }
        }
    }

    private fun markSuppressed(key: String) {
        val now = SystemClock.elapsedRealtime()
        suppressedKeys[key] = now
        cleanupSuppressedKeys(now)
    }

    private fun isSuppressed(key: String): Boolean {
        cleanupSuppressedKeys()
        return suppressedKeys.containsKey(key)
    }

    private fun clearSuppressed(key: String) {
        pendingSuppressionJobs.remove(key)?.cancel()
        suppressedKeys.remove(key)
    }

    private fun cleanupSuppressedKeys(now: Long = SystemClock.elapsedRealtime()) {
        val cutoff = now - SUPPRESSED_KEY_TTL_MS
        suppressedKeys.entries.forEach { entry ->
            if (entry.value < cutoff) {
                suppressedKeys.remove(entry.key, entry.value)
            }
        }
        val overflow = suppressedKeys.size - MAX_SUPPRESSED_KEYS
        if (overflow > 0) {
            suppressedKeys.entries
                .sortedBy { it.value }
                .take(overflow)
                .forEach { suppressedKeys.remove(it.key, it.value) }
        }
    }

    private val iconCache = android.util.LruCache<String, Bitmap>(50)

    private fun loadAppIconBitmap(packageName: String): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        return runCatchingLogged(TAG, "LoadAppIconBitmap failed") {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = ICON_BITMAP_SIZE, height = ICON_BITMAP_SIZE).also { iconCache.put(packageName, it) }
        }
    }

    private fun Notification.loadLargeIconBitmap(): Bitmap? {
        val extraLarge = extras.get(Notification.EXTRA_LARGE_ICON)
        extraLarge.toBitmapOrNull()?.let { return it }
        val extraLargeBig = extras.get(Notification.EXTRA_LARGE_ICON_BIG)
        extraLargeBig.toBitmapOrNull()?.let { return it }
        val largeIconObj = getLargeIcon()
        runCatchingLogged(TAG, "LoadLargeIconBitmap failed") {
            largeIconObj?.loadDrawable(this@SmartIslandNotificationListenerService)
                ?.toBitmap(width = LARGE_ICON_BITMAP_SIZE, height = LARGE_ICON_BITMAP_SIZE)
        }?.let { return it }
        return runCatchingLogged(TAG, "LoadMessagingStyleAvatar failed") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (!messages.isNullOrEmpty()) {
                    val lastMessageBundle = messages.lastOrNull() as? android.os.Bundle
                    val senderPerson = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        lastMessageBundle?.getParcelable("sender_person", android.app.Person::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        lastMessageBundle?.getParcelable("sender_person") as? android.app.Person
                    }
                    senderPerson?.icon?.loadDrawable(this@SmartIslandNotificationListenerService)
                        ?.toBitmap(width = LARGE_ICON_BITMAP_SIZE, height = LARGE_ICON_BITMAP_SIZE)
                } else null
            } else null
        }
    }

    private fun Any?.toBitmapOrNull(): Bitmap? {
        return when (this) {
            is Bitmap -> this
            is Icon -> runCatchingLogged(TAG, "Icon toBitmapOrNull failed") {
                loadDrawable(this@SmartIslandNotificationListenerService)
                    ?.toBitmap(width = 128, height = 128)
            }
            else -> null
        }
    }

    private fun controllersFor(packageName: String): List<MediaController> =
        activeMediaControllers.filter { it.packageName == packageName }

    private fun bestControllerFor(packageName: String): MediaController? {
        val matches = controllersFor(packageName)
        return matches.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: matches.firstOrNull()
    }

    private fun findMediaInfo(notification: Notification, packageName: String): MediaInfo? {
        notification.mediaSessionController()?.extractMediaInfo()?.let { return it }
        val controller = bestControllerFor(packageName) ?: return null
        return controller.extractMediaInfo()
    }

    private fun Notification.mediaSessionController(): MediaController? {
        val token = runCatchingLogged(TAG, "GetMediaSessionToken failed") {
            val ex = extras
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ex.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                ex.getParcelable(Notification.EXTRA_MEDIA_SESSION)
            }
        } ?: return null
        return runCatchingLogged(TAG, "MediaController init failed") { MediaController(this@SmartIslandNotificationListenerService, token) }
    }

    private fun MediaController.extractMediaInfo(): MediaInfo {
        val metadata = this.metadata
        val playbackState = this.playbackState
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 }
        val positionMs = playbackState?.estimatedPosition()
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        return MediaInfo(artwork, positionMs, durationMs, playbackState?.state == PlaybackState.STATE_PLAYING)
    }

    private val activeMediaControllers: List<MediaController>
        get() = runCatchingLogged(TAG, "GetActiveSessions failed") {
            val componentName = android.content.ComponentName(this, SmartIslandNotificationListenerService::class.java)
            mediaSessionManager.getActiveSessions(componentName)
        } ?: emptyList()

    private val mediaSessionManager by lazy {
        getSystemService(MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
    }

    private fun PlaybackState.estimatedPosition(): Long? {
        if (position < 0) return null
        if (state != PlaybackState.STATE_PLAYING) return position
        val elapsed = android.os.SystemClock.elapsedRealtime() - lastPositionUpdateTime
        return (position + (elapsed * playbackSpeed).toLong()).coerceAtLeast(0L)
    }

    private data class MediaInfo(val artwork: Bitmap?, val positionMs: Long?, val durationMs: Long?, val isPlaying: Boolean)

    companion object {
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        private const val TAG = "SmartIslandNotificationListener"
        private const val ICON_BITMAP_SIZE = 96
        private const val LARGE_ICON_BITMAP_SIZE = 128
        private const val MAX_SUPPRESSED_KEYS = 100
        private const val SUPPRESSED_KEY_TTL_MS = 10 * 60 * 1000L
        private const val INITIAL_SUPPRESSION_WINDOW_MS = 1500L
    }
}
