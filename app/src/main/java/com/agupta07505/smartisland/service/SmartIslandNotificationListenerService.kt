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
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
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
import com.agupta07505.smartisland.util.NotificationCooldownManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private var lastHistoryCleanupTime = 0L
    private val cooldownReleaseJobs = ConcurrentHashMap<String, Job>()
    private var lastSoundPlayedTimeMs = 0L
    private var lastAutoExpandTimeMs = 0L

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
                            is SmartIslandCommand.SkipNext -> {
                                val controller = command.packageName?.let { bestControllerFor(it) }
                                    ?: activeMediaControllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                                    ?: activeMediaControllers.firstOrNull()
                                controller?.transportControls?.skipToNext()
                            }
                            is SmartIslandCommand.SkipPrevious -> {
                                val controller = command.packageName?.let { bestControllerFor(it) }
                                    ?: activeMediaControllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                                    ?: activeMediaControllers.firstOrNull()
                                controller?.transportControls?.skipToPrevious()
                            }
                            is SmartIslandCommand.PlayPause -> {
                                val controller = command.packageName?.let { bestControllerFor(it) }
                                    ?: activeMediaControllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                                    ?: activeMediaControllers.firstOrNull()
                                if (controller != null) {
                                    val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                                    if (isPlaying) {
                                        controller.transportControls.pause()
                                    } else {
                                        controller.transportControls.play()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        isSystemConnected = false
        stopSessionWatch()
        lastSoundPlayedTimeMs = 0L
        lastAutoExpandTimeMs = 0L
        cooldownReleaseJobs.values.forEach { it.cancel() }
        cooldownReleaseJobs.clear()
        NotificationCooldownManager.clear()
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
        onNotificationPosted(sbn, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
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
                !shouldSuppressFromIsland(sbn, rankingMap)
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
                if (shouldSuppressFromIsland(sbn, rankingMap)) return@runSuspendCatchingLogged

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
        startSessionWatch()
        runCatchingLogged(TAG, "onListenerConnected callback failed") {
            android.util.Log.d(TAG, "onListenerConnected")
            serviceScope.launch {
                runSuspendCatchingLogged(TAG, "ListenerConnected failed") {
                    val settings = repository.settings.first()
                    currentSettings = settings
                    if (!settings.enabled) return@runSuspendCatchingLogged

                    val overlayReady = ensureOverlayServiceRunning()
                    val active = runCatchingLogged(TAG, "Failed to get active notifications") {
                        activeNotifications?.toList()
                    }?.filter { it.packageName != packageName }
                        ?.filterNot { shouldSuppressFromIsland(it) }
                        .orEmpty()

                    android.util.Log.d(TAG, "ListenerConnected: ${active.size} active, overlayReady=$overlayReady")

                    active.forEach { sbn ->
                        val mode = sbn.notification.toIslandMode(
                            sbn,
                            settings.liveActivitiesEnabled,
                            settings.navigationEnabled,
                            settings.deviceType
                        )
                        // On initial listener connect or rebind, only restore persistent ongoing activities.
                        // Stale standard notifications from the notification shade are not imported to avoid sudden floods.
                        if (!isPersistentIslandMode(mode)) {
                            return@forEach
                        }
                        if (settings.hideFromNotificationShade &&
                            shouldBeIslandOnly(sbn.notification, mode)
                        ) {
                            suppressSystemNotification(sbn.key)
                        }
                        handleNotificationPosted(sbn, settings, isInitialSync = true)
                    }
                }
            }
        }
    }

    // Fallback for players whose notification is missing, blocked, or lacks a media session token:
    // read playback straight from the active MediaSessions and show it as a synthetic Music entry.
    private val sessionCallbacks = ConcurrentHashMap<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private val sessionStaleJobs = ConcurrentHashMap<String, Job>()
    private var sessionsListener: android.media.session.MediaSessionManager.OnActiveSessionsChangedListener? = null

    private fun startSessionWatch() {
        mainScope.launch {
            runCatchingLogged(TAG, "Session watch failed") {
                val mgr = mediaSessionManager ?: return@runCatchingLogged
                if (sessionsListener == null) {
                    val listener = android.media.session.MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                        trackSessions(controllers.orEmpty())
                    }
                    mgr.addOnActiveSessionsChangedListener(
                        listener,
                        android.content.ComponentName(this@SmartIslandNotificationListenerService, SmartIslandNotificationListenerService::class.java)
                    )
                    sessionsListener = listener
                }
                trackSessions(activeMediaControllers)
            }
        }
    }

    private fun stopSessionWatch() {
        runCatchingLogged(TAG, "Session unwatch failed") {
            sessionsListener?.let { mediaSessionManager?.removeOnActiveSessionsChangedListener(it) }
        }
        sessionsListener = null
        sessionCallbacks.values.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        sessionCallbacks.clear()
        sessionStaleJobs.values.forEach { it.cancel() }
        sessionStaleJobs.clear()
    }

    private fun trackSessions(controllers: List<MediaController>) {
        android.util.Log.d(TAG, "trackSessions: ${controllers.map { it.packageName }}")
        val live = controllers.map { it.sessionToken }.toSet()
        sessionCallbacks.keys.filter { it !in live }.forEach { token ->
            sessionCallbacks.remove(token)?.let { (controller, callback) ->
                controller.unregisterCallback(callback)
                dropSessionEntry(controller.packageName)
            }
        }
        controllers.filter { it.packageName != packageName && !sessionCallbacks.containsKey(it.sessionToken) }.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = syncSession(controller)
                override fun onMetadataChanged(metadata: MediaMetadata?) = syncSession(controller)
                override fun onSessionDestroyed() = dropSessionEntry(controller.packageName)
            }
            controller.registerCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))
            sessionCallbacks[controller.sessionToken] = controller to callback
            syncSession(controller)
        }
    }

    private fun syncSession(controller: MediaController) {
        runCatchingLogged(TAG, "Session sync failed") {
            val settings = currentSettings
            val pkg = controller.packageName
            val key = sessionKey(pkg)
            if (!settings.enabled || pkg in settings.disabledNotificationPackages) return@runCatchingLogged dropSessionEntry(pkg)

            // A real media notification from this app already drives the island.
            val entries = notificationRepository.notifications.value
            if (entries.any { it.packageName == pkg && it.mode == IslandMode.Music && it.key != key }) {
                return@runCatchingLogged dropSessionEntry(pkg)
            }

            val state = controller.playbackState?.state
            android.util.Log.d(TAG, "syncSession: pkg=$pkg state=$state title=${controller.metadata?.description?.title}")
            val playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
            sessionStaleJobs.remove(pkg)?.cancel()
            if (!playing) {
                if (entries.none { it.key == key }) return@runCatchingLogged
                // Keep a paused entry briefly so it can be resumed from the island, then let it go.
                sessionStaleJobs[pkg] = serviceScope.launch {
                    delay(SESSION_PAUSE_GRACE_MS)
                    notificationRepository.removeNotification(key)
                    sessionStaleJobs.remove(pkg)
                }
            }

            val metadata = controller.metadata
            val description = metadata?.description
            val title = description?.title?.toString()
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: return@runCatchingLogged
            val text = description?.subtitle?.toString()
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: ""
            val appName = runCatchingLogged(TAG, "GetApplicationInfo failed") {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } ?: pkg
            val info = controller.extractMediaInfo()
            val isNew = entries.none { it.key == key }
            notificationRepository.postNotification(
                IslandNotification(
                    key = key,
                    packageName = pkg,
                    appName = appName,
                    title = title,
                    text = text,
                    timeMillis = System.currentTimeMillis(),
                    icon = loadAppIconBitmap(pkg),
                    largeIcon = info.artwork ?: description?.iconBitmap,
                    mediaPositionMs = info.positionMs,
                    mediaDurationMs = info.durationMs,
                    mediaIsPlaying = info.isPlaying,
                    mediaToken = controller.sessionToken,
                    mode = IslandMode.Music,
                    contentIntent = controller.sessionActivity
                ),
                autoExpand = isNew && playing && settings.autoExpandOnNotification
            )
        }
    }

    private fun dropSessionEntry(packageName: String) {
        sessionStaleJobs.remove(packageName)?.cancel()
        val key = sessionKey(packageName)
        if (notificationRepository.notifications.value.any { it.key == key }) {
            notificationRepository.removeNotification(key)
        }
    }

    private fun sessionKey(packageName: String) = "session|$packageName"

    override fun onListenerDisconnected() {
        stopSessionWatch()
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

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        super.onInterruptionFilterChanged(interruptionFilter)
        runCatchingLogged(TAG, "onInterruptionFilterChanged callback failed") {
            val isDndActive = interruptionFilter != INTERRUPTION_FILTER_ALL &&
                interruptionFilter != INTERRUPTION_FILTER_UNKNOWN
            android.util.Log.d(TAG, "onInterruptionFilterChanged: filter=$interruptionFilter, isDndActive=$isDndActive")
            if (isDndActive) {
                purgeDndBlockedNotifications()
            }
        }
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        super.onNotificationRankingUpdate(rankingMap)
        runCatchingLogged(TAG, "onNotificationRankingUpdate callback failed") {
            val filter = runCatchingLogged(TAG, "Failed to get filter in ranking update") {
                currentInterruptionFilter
            }?.takeIf { it != INTERRUPTION_FILTER_UNKNOWN }
                ?: runCatchingLogged(TAG, "Failed to get filter from NotificationManager") {
                    (getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager)?.currentInterruptionFilter
                } ?: INTERRUPTION_FILTER_ALL

            val isDndActive = filter != INTERRUPTION_FILTER_ALL && filter != INTERRUPTION_FILTER_UNKNOWN
            if (isDndActive) {
                purgeDndBlockedNotifications(rankingMap)
            }
        }
    }

    private fun purgeDndBlockedNotifications(rankingMap: RankingMap? = null) {
        serviceScope.launch {
            val activeMap = runCatchingLogged(TAG, "Failed to get active notifications for DND purge") {
                activeNotifications?.associateBy { it.key }
            }.orEmpty()

            val currentIsland = notificationRepository.notifications.value
            currentIsland.forEach { islandNotif ->
                val sbn = activeMap[islandNotif.key]
                if (sbn != null) {
                    if (isBlockedByDoNotDisturb(sbn, rankingMap)) {
                        android.util.Log.d(TAG, "DND active: purging blocked notification ${islandNotif.key}")
                        notificationRepository.removeNotification(islandNotif.key)
                    }
                } else if (islandNotif.mode == IslandMode.Notification) {
                    android.util.Log.d(TAG, "DND active: purging untracked notification ${islandNotif.key}")
                    notificationRepository.removeNotification(islandNotif.key)
                }
            }
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
        settings: SmartIslandSettings,
        isInitialSync: Boolean = false
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

        // Check intelligent anti-spam notification cooldown
        if (NotificationCooldownManager.shouldThrottle(sbn, settings, mode)) {
            android.util.Log.d(TAG, "Notification throttled by cooldown: pkg=${sbn.packageName} key=${sbn.key}")
            scheduleCooldownRelease(sbn.packageName, settings.notificationCooldownDurationMinutes)
            return
        }

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
                } else if (existingNotif != null && existingNotif.mode == IslandMode.Timer && existingNotif.timeMillis > System.currentTimeMillis()) {
                    existingNotif.timeMillis
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
            autoExpand = if (!isInitialSync && shouldIslandOnly && settings.autoExpandOnNotification) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastAutoExpandTimeMs >= AUTO_EXPAND_DEBOUNCE_MS) {
                    lastAutoExpandTimeMs = now
                    true
                } else {
                    false
                }
            } else {
                false
            }
        )

        if (settings.enableNotificationHistory && mode != IslandMode.Music) {
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
                    val now = System.currentTimeMillis()
                    if (now - lastHistoryCleanupTime > 15 * 60 * 1000L) {
                        lastHistoryCleanupTime = now
                        historyRepository.cleanupOldEntries(settings.notificationHistoryRetentionHours)
                    }
                }
            }
        }

        if (!isInitialSync && isNewNotif && (mode == IslandMode.Notification || shouldIslandOnly) && mode != IslandMode.DownloadUpload) {
            playNotificationSound(sbn)
        }

        if (mode == IslandMode.Notification) {
            val existing = notificationRepository.notifications.value
            existing.filter { it.mode == IslandMode.Notification && it.packageName == sbn.packageName && it.key != sbn.key }
                .forEach { notificationRepository.removeNotification(it.key) }
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

    internal fun isBlockedByDoNotDisturb(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean {
        val filter = filterOverride ?: runCatchingLogged(TAG, "Failed to get interruption filter") {
            currentInterruptionFilter
        }?.takeIf { it != INTERRUPTION_FILTER_UNKNOWN }
            ?: runCatchingLogged(TAG, "Failed to get interruption filter from NotificationManager") {
                (getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager)?.currentInterruptionFilter
            } ?: INTERRUPTION_FILTER_ALL

        // If filter is ALL or UNKNOWN, Do Not Disturb is OFF
        if (filter == INTERRUPTION_FILTER_ALL || filter == INTERRUPTION_FILTER_UNKNOWN) {
            return false
        }

        val notification = sbn.notification ?: return true
        val mode = notification.toIslandMode(
            sbn,
            currentSettings.liveActivitiesEnabled,
            currentSettings.navigationEnabled,
            currentSettings.deviceType
        )

        // Music playback and ongoing system hardware states (Hotspot, ScreenRecording)
        // are not intrusive notification alerts and must not be blocked by DND.
        if (mode == IslandMode.Music || mode == IslandMode.Hotspot || mode == IslandMode.ScreenRecording) {
            return false
        }

        val rMap = rankingMap ?: runCatchingLogged(TAG, "Failed to get current ranking") { currentRanking }
        if (rMap != null) {
            val ranking = Ranking()
            if (rMap.getRanking(sbn.key, ranking)) {
                // matchesInterruptionFilter() returns true if allowed by DND policy, false if blocked
                return !ranking.matchesInterruptionFilter()
            }
        }

        if (filter == INTERRUPTION_FILTER_NONE) {
            return true
        }

        if (filter == INTERRUPTION_FILTER_ALARMS) {
            return notification.category != Notification.CATEGORY_ALARM &&
                mode != IslandMode.Timer &&
                mode != IslandMode.Stopwatch
        }

        // Priority only mode: standard notifications are blocked by default if not verified
        return true
    }

    internal fun shouldSuppressFromIsland(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean {
        if (isBlockedByDoNotDisturb(sbn, rankingMap, filterOverride)) {
            android.util.Log.d(TAG, "Suppressed by Do Not Disturb: key=${sbn.key} pkg=${sbn.packageName}")
            return true
        }
        val pm = runCatching { packageManager }.getOrNull() ?: return false
        return com.agupta07505.smartisland.util.NotificationFilter.shouldSuppressFromIsland(
            sbn,
            pm,
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
                    val stillActive = runCatchingLogged(TAG, "Failed checking stillActive") {
                        activeNotifications?.any { it.key == key }
                    } ?: false
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

    private fun playNotificationSound(sbn: StatusBarNotification) {
        if (currentSettings.disabledSoundPackages.contains(sbn.packageName)) return
        if (isBlockedByDoNotDisturb(sbn)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastSoundPlayedTimeMs < SOUND_DEBOUNCE_MS) return
        lastSoundPlayedTimeMs = now
        runCatchingLogged(TAG, "Failed to play notification sound for ${sbn.packageName}") {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null || audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

            val notification = sbn.notification
            var soundUri: Uri? = null
            var audioAttributes: AudioAttributes? = null

            // 1. Check Notification Channel (Android 8.0+, API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ranking = Ranking()
                val hasRanking = currentRanking?.getRanking(sbn.key, ranking) == true
                val channel = if (hasRanking) ranking.channel else null

                if (channel != null) {
                    // Silent channels (IMPORTANCE_NONE, IMPORTANCE_MIN, IMPORTANCE_LOW) must not play sound
                    if (channel.importance < NotificationManager.IMPORTANCE_DEFAULT) {
                        return
                    }
                    val chSound = channel.sound
                    if (chSound == null || chSound == Uri.EMPTY || chSound.toString().isEmpty()) {
                        // Channel is explicitly configured without sound (Silent)
                        return
                    }
                    soundUri = chSound
                    audioAttributes = channel.audioAttributes
                }
            }

            // 2. Fallback to notification payload if channel was not present
            if (soundUri == null) {
                @Suppress("DEPRECATION")
                val notifSound = notification.sound
                @Suppress("DEPRECATION")
                val defaults = notification.defaults

                if (notifSound != null && notifSound != Uri.EMPTY && notifSound.toString().isNotEmpty()) {
                    soundUri = notifSound
                } else if ((defaults and Notification.DEFAULT_SOUND) != 0) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                audioAttributes = notification.audioAttributes
            }

            if (soundUri == null) {
                return
            }

            // 3. Resolve and play the exact notification sound
            var ringtone: Ringtone? = runCatching {
                RingtoneManager.getRingtone(applicationContext, soundUri)
            }.getOrNull()

            // Fallback to default tone if custom URI failed to load
            if (ringtone == null) {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = runCatching {
                    RingtoneManager.getRingtone(applicationContext, fallbackUri)
                }.getOrNull()
            }

            if (ringtone != null) {
                val attrs = audioAttributes ?: AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                ringtone.audioAttributes = attrs
                ringtone.play()
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
            val mgr = mediaSessionManager ?: return emptyList()
            val componentName = android.content.ComponentName(this, SmartIslandNotificationListenerService::class.java)
            mgr.getActiveSessions(componentName)
        } ?: emptyList()

    private val mediaSessionManager: android.media.session.MediaSessionManager? by lazy {
        runCatching { getSystemService(MEDIA_SESSION_SERVICE) as? android.media.session.MediaSessionManager }.getOrNull()
    }

    private fun PlaybackState.estimatedPosition(): Long? {
        if (position < 0) return null
        if (state != PlaybackState.STATE_PLAYING) return position
        val elapsed = android.os.SystemClock.elapsedRealtime() - lastPositionUpdateTime
        return (position + (elapsed * playbackSpeed).toLong()).coerceAtLeast(0L)
    }

    private data class MediaInfo(val artwork: Bitmap?, val positionMs: Long?, val durationMs: Long?, val isPlaying: Boolean)

    private fun scheduleCooldownRelease(packageName: String, durationMinutes: Int) {
        if (cooldownReleaseJobs[packageName]?.isActive == true) return
        val job = serviceScope.launch {
            delay(durationMinutes * 60_000L)
            val buffered = NotificationCooldownManager.pollBufferedNotification(packageName)
            if (buffered != null) {
                val current = repository.settings.first()
                if (current.enabled) {
                    android.util.Log.d(TAG, "Releasing buffered cooldown notification: pkg=$packageName key=${buffered.key}")
                    handleNotificationPosted(buffered, current)
                }
            }
            cooldownReleaseJobs.remove(packageName)
        }
        cooldownReleaseJobs[packageName] = job
    }

    private fun isPersistentIslandMode(mode: IslandMode): Boolean {
        return mode == IslandMode.Music ||
            mode == IslandMode.IncomingCall ||
            mode == IslandMode.Timer ||
            mode == IslandMode.Stopwatch ||
            mode == IslandMode.Navigation ||
            mode == IslandMode.LiveActivity ||
            mode == IslandMode.DownloadUpload ||
            mode == IslandMode.Hotspot ||
            mode == IslandMode.ScreenRecording
    }

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
        private const val SOUND_DEBOUNCE_MS = 1200L
        private const val AUTO_EXPAND_DEBOUNCE_MS = 1500L
        private const val SESSION_PAUSE_GRACE_MS = 60_000L
    }
}
