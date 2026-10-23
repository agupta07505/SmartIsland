/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.util.AppEventRules
import com.agupta07505.smartisland.util.CallActionRole
import com.agupta07505.smartisland.util.CallStyleExtras
import com.agupta07505.smartisland.util.RecordingClock
import com.agupta07505.smartisland.util.VoipCallDetector
import com.agupta07505.smartisland.util.isCallEnded
import com.agupta07505.smartisland.util.isDownloadComplete
import com.agupta07505.smartisland.util.isScreenRecordingComplete
import com.agupta07505.smartisland.util.transferKind
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode
import com.agupta07505.smartisland.util.toVoipFacts
import android.app.Notification
import android.app.NotificationManager
import com.agupta07505.smartisland.util.expandedText
import com.agupta07505.smartisland.util.readerTextWithoutPictures
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
    // Keys the listener cancelled itself to make them island-only; their island copy stays alive.
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
    @Volatile private var lastSoundPlayedTimeMs = 0L
    @Volatile private var lastAutoExpandTimeMs = 0L

    private val viewButtonReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            com.agupta07505.smartisland.util.NotificationViewInspector.click(intent.data)
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.agupta07505.smartisland.util.AppLogRecorder.restore(applicationContext)
        com.agupta07505.smartisland.util.AppLogRecorder.i("ListenerLifecycle", "onCreate: notification listener service created (pid=${android.os.Process.myPid()})")
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            viewButtonReceiver,
            android.content.IntentFilter(com.agupta07505.smartisland.util.NotificationViewInspector.ACTION_CLICK).apply {
                addDataScheme(com.agupta07505.smartisland.util.NotificationViewInspector.SCHEME)
            },
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                var wasEnabled: Boolean? = null
                repository.settings.collect { settings ->
                    currentSettings = settings
                    // The island was switched on while the listener is already connected (it connects as soon as access is granted,
                    // long before the island is enabled): import the shade now.
                    if (settings.enabled && wasEnabled == false && isSystemConnected) {
                        serviceScope.launch {
                            runSuspendCatchingLogged(TAG, "Shade sync after enabling failed") {
                                initialShadeSync(trigger = "island enabled", onlyMissing = true)
                            }
                        }
                    }
                    wasEnabled = settings.enabled
                    com.agupta07505.smartisland.util.AppLogRecorder.applySetting(applicationContext, settings.recordLogs)
                    logRelevantSettings(settings)
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
        // The notification-count idle widget counts the shade and the island's own cards: a card the island drops changes the count too.
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Count collector failed") {
                notificationRepository.notifications.collect { scheduleCountRefresh() }
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
                            is SmartIslandCommand.TimerStopped -> {
                                lastTimerFrozen.remove(command.packageName)
                                lastTimerShown.remove(command.packageName)
                                lastTimerTargets.remove(command.packageName)
                            }
                            is SmartIslandCommand.ReplySent -> {
                                replyGraceUntil[command.key] = SystemClock.elapsedRealtime() + REPLY_GRACE_MS
                            }
                            is SmartIslandCommand.ImportShadeNotifications -> {
                                importShadeNotifications()
                            }
                            is SmartIslandCommand.SyncShade -> {
                                if (isSystemConnected) {
                                    serviceScope.launch {
                                        runSuspendCatchingLogged(TAG, "Shade sync failed") {
                                            initialShadeSync(trigger = "overlay started", onlyMissing = true)
                                        }
                                    }
                                }
                            }
                            is SmartIslandCommand.ImportSameAppNotifications -> {
                                importSameAppNotifications(command.packageName)
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

    private var mediaWatcher: com.agupta07505.smartisland.util.MediaSessionWatcher? = null

    private fun startMediaWatcher() {
        if (mediaWatcher != null) return
        mediaWatcher = com.agupta07505.smartisland.util.MediaSessionWatcher(
            context = this,
            listener = android.content.ComponentName(this, SmartIslandNotificationListenerService::class.java),
            onSnapshot = ::applyMediaSnapshot
        ).also { it.start() }
    }

    private fun applyMediaSnapshot(snapshot: com.agupta07505.smartisland.util.MediaSnapshot) {
        if (!currentSettings.enabled) return
        notificationRepository.notifications.value
            .filter { com.agupta07505.smartisland.util.MediaCardPatch.appliesTo(it, snapshot) }
            .forEach { card ->
                notificationRepository.updateNotification(card.key) { old ->
                    com.agupta07505.smartisland.util.MediaCardPatch.apply(old, snapshot)
                }
            }
    }

    private var voipMonitor: com.agupta07505.smartisland.util.VoipCallMonitor? = null
    private val voipPromotedKeys = ConcurrentHashMap.newKeySet<String>()

    private val callPostGuard = com.agupta07505.smartisland.util.StalePostGuard()

    private fun startVoipMonitor() {
        if (voipMonitor != null) return
        voipMonitor = com.agupta07505.smartisland.util.VoipCallMonitor(
            context = this,
            scope = serviceScope,
            onChanged = { reconcileVoipCall() }
        ).also { it.start() }
    }

    private fun reconcileVoipCall() {
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "VoIP reconcile failed") {
                val settings = currentSettings
                val enabled = settings.enabled &&
                    settings.isEventEnabled(com.agupta07505.smartisland.model.IslandEvent.CALL.id)
                com.agupta07505.smartisland.util.AppLogRecorder.i(
                    "VoipReconcile",
                    "signalActive=${com.agupta07505.smartisland.util.VoipCallSignal.active}" +
                        " voiceCapture=${com.agupta07505.smartisland.util.VoipCallSignal.voiceCapture}" +
                        " islandEnabled=${settings.enabled} callEventEnabled=${settings.isEventEnabled(com.agupta07505.smartisland.model.IslandEvent.CALL.id)}" +
                        " listenerConnected=$isSystemConnected"
                )
                if (!com.agupta07505.smartisland.util.VoipCallSignal.active || !enabled) {
                    com.agupta07505.smartisland.util.AppLogRecorder.i("VoipReconcile", "-> clearing VoIP cards (signal off or call event disabled)")
                    clearVoipCards()
                    return@runSuspendCatchingLogged
                }
                if (!isSystemConnected) {
                    com.agupta07505.smartisland.util.AppLogRecorder.w("VoipReconcile", "-> skipped: notification listener not connected, no way to read the call notification")
                    return@runSuspendCatchingLogged
                }

                val candidates = runCatchingLogged(TAG, "Failed to get active notifications") {
                    activeNotifications?.toList()
                }.orEmpty().filter {
                    it.packageName != packageName &&
                        ((it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 ||
            com.agupta07505.smartisland.util.NotificationFilter.isCallNotification(it.notification))
                }
                val bestKey = com.agupta07505.smartisland.util.VoipCallDetector.pickBest(
                    candidates.map { it.key to it.notification.toVoipFacts(it.packageName) }
                )
                val best = candidates.firstOrNull { it.key == bestKey }
                if (com.agupta07505.smartisland.util.AppLogRecorder.isRecording) {
                    // Every active notification with its score: shows why the call's own one lost or never qualified.
                    candidates.forEach { c ->
                        com.agupta07505.smartisland.util.AppLogRecorder.i(
                            "VoipReconcile",
                            "candidate pkg=${c.packageName} key=${c.key} " +
                                com.agupta07505.smartisland.util.VoipCallDetector.explain(c.notification.toVoipFacts(c.packageName)) +
                                if (c.key == bestKey) " <== BEST" else ""
                        )
                    }
                    com.agupta07505.smartisland.util.AppLogRecorder.i(
                        "VoipReconcile",
                        "${candidates.size} candidates, best=${bestKey ?: "none"} -> " + when {
                            best != null -> "promoting the app's own call notification to a call card"
                            com.agupta07505.smartisland.util.VoipCallSignal.voiceCapture -> "no qualifying notification, showing the button-less 'Call in progress' card"
                            else -> "no qualifying notification and no voice capture: no card"
                        }
                    )
                }
                // The scan above can take seconds on a busy shade; the call may be over by now.
                if (!com.agupta07505.smartisland.util.VoipCallSignal.active) {
                    com.agupta07505.smartisland.util.AppLogRecorder.i("VoipReconcile", "-> call ended during the scan, dropping the result")
                    clearVoipCards()
                    return@runSuspendCatchingLogged
                }
                if (best != null) {
                    notificationRepository.removeNotification(VOIP_CARD_KEY)
                    handleNotificationPosted(best, settings)
                } else if (com.agupta07505.smartisland.util.VoipCallSignal.voiceCapture) {
                    notificationRepository.postNotification(
                        IslandNotification(
                            key = VOIP_CARD_KEY,
                            packageName = packageName,
                            appName = "Voice call",
                            title = "Call in progress",
                            text = "",
                            timeMillis = com.agupta07505.smartisland.util.VoipCallSignal.sinceMillis
                                .takeIf { it > 0 } ?: System.currentTimeMillis(),
                            mode = IslandMode.IncomingCall
                        )
                    )
                } else {
                    notificationRepository.removeNotification(VOIP_CARD_KEY)
                }
            }
        }
    }

    private fun clearVoipCards() {
        notificationRepository.removeNotification(VOIP_CARD_KEY)
        voipPromotedKeys.toList().forEach { notificationRepository.removeNotification(it) }
        voipPromotedKeys.clear()
    }

    private val recordingStates = ConcurrentHashMap<String, RecordingClock.State>()

    private val lastClocks = ConcurrentHashMap<String, RecordingClock.State>()

    private val lastTimerTargets = ConcurrentHashMap<String, Long>()

    private val lastTimerFrozen = ConcurrentHashMap<String, Long>()

    private val lastTimerShown = ConcurrentHashMap<String, Long>()
    private var recordingWatchdog: Job? = null
    private var transferExpiryJob: Job? = null
    private val replyGraceUntil = ConcurrentHashMap<String, Long>()

    private fun startTransferExpirySweep() {
        if (transferExpiryJob?.isActive == true) return
        transferExpiryJob = serviceScope.launch {
            while (true) {
                delay(30_000L)
                val minutes = currentSettings.transferExpiryMinutes
                val keys = com.agupta07505.smartisland.util.TransferExpiry.keysToExpire(
                    notificationRepository.notifications.value, System.currentTimeMillis(), minutes
                )
                keys.forEach { key ->
                    android.util.Log.d(TAG, "Transfer card expired (no update for $minutes min): $key")
                    latePictureJobs.remove(key)?.cancel()
                    clearSuppressed(key)
                    notificationRepository.removeNotification(key)
                }
            }
        }
    }

    private fun startRecordingWatchdog() {
        if (recordingWatchdog?.isActive == true) return
        recordingWatchdog = serviceScope.launch {
            while (true) {
                delay(500L)
                // Sample cards from the Demo tab have no notification behind them on purpose.
                val cards = notificationRepository.notifications.value.filter {
                    (it.mode == IslandMode.ScreenRecording || it.mode == IslandMode.AudioRecording) &&
                        !com.agupta07505.smartisland.util.DemoEvents.isDemoKey(it.key)
                }
                if (cards.isEmpty()) {
                    recordingStates.clear()
                    continue
                }
                val active = runCatchingLogged(TAG, "Recording watchdog could not read notifications") {
                    activeNotifications?.toList()
                } ?: continue
                val activeByKey = active.associateBy { it.key }
                cards.forEach { card ->
                    val backing = activeByKey[card.key]
                    if (backing == null || backing.notification.isScreenRecordingComplete()) {
                        pendingRemovals.remove(card.key)?.cancel()
                        recordingStates.remove(card.key)
                        notificationRepository.removeNotification(card.key)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        com.agupta07505.smartisland.util.AppLogRecorder.w("ListenerLifecycle", "onDestroy: notification listener service destroyed (pid=${android.os.Process.myPid()})")
        runCatching { unregisterReceiver(viewButtonReceiver) }
        com.agupta07505.smartisland.util.NotificationViewInspector.forgetAll()
        recordingWatchdog?.cancel()
        transferExpiryJob?.cancel()
        recordingWatchdog = null
        mediaWatcher?.stop()
        mediaWatcher = null
        recordingStates.clear()
        lastClocks.clear()
        lastTimerTargets.clear()
        lastTimerFrozen.clear()
        lastTimerShown.clear()
        voipMonitor?.stop()
        voipMonitor = null
        isSystemConnected = false
        lastSoundPlayedTimeMs = 0L
        lastAutoExpandTimeMs = 0L
        cooldownReleaseJobs.values.forEach { it.cancel() }
        cooldownReleaseJobs.clear()
        NotificationCooldownManager.clear()
        callPostGuard.clear()
        pendingRemovals.values.forEach { it.cancel() }
        pendingRemovals.clear()
        pendingSuppressionJobs.values.forEach { it.cancel() }
        pendingSuppressionJobs.clear()
        suppressedKeys.clear()
        iconCache.evictAll()
        smallIconCache.evictAll()
        serviceScope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    private val pendingRemovals = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private val shownPostTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun noteShown(sbn: StatusBarNotification) {
        if (shownPostTimes.size > SHOWN_POSTS_MAX) shownPostTimes.clear()
        shownPostTimes[sbn.key] = sbn.postTime
    }

    private fun alreadyShown(sbn: StatusBarNotification): Boolean = shownPostTimes[sbn.key] == sbn.postTime
    private val pendingSuppressionJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private var countRefreshJob: Job? = null
    private val appLabels = ConcurrentHashMap<String, String>()

    private fun scheduleCountRefresh() {
        countRefreshJob?.cancel()
        countRefreshJob = serviceScope.launch {
            delay(COUNT_REFRESH_DEBOUNCE_MS)
            refreshCountEntries()
        }
    }

    private fun refreshCountEntries() {
        if (!isSystemConnected) return
        val shade = runCatchingLogged(TAG, "Count: reading the shade failed") { activeNotifications?.toList() }
            .orEmpty()
            .filter { it.packageName != packageName }
            .map { it.toCountEntry() }
        // The island may have cancelled its shade copy (hide from shade): its own cards still count.
        val island = notificationRepository.notifications.value
            .filter { it.mode == IslandMode.Notification && it.packageName != packageName }
            .map {
                com.agupta07505.smartisland.model.CountEntry(
                    key = it.key,
                    packageName = it.packageName,
                    appLabel = it.appName.ifBlank { appLabelOf(it.packageName) },
                    category = it.category
                )
            }
        com.agupta07505.smartisland.util.NotificationCountTracker.update(
            com.agupta07505.smartisland.model.NotificationCounts.merge(shade, island)
        )
    }

    private fun StatusBarNotification.toCountEntry(): com.agupta07505.smartisland.model.CountEntry {
        val n = notification
        val flags = n?.flags ?: 0
        return com.agupta07505.smartisland.model.CountEntry(
            key = key,
            packageName = packageName,
            appLabel = appLabelOf(packageName),
            category = n?.category,
            isMessagingStyle = n?.extras?.getString(Notification.EXTRA_TEMPLATE) == Notification.MessagingStyle::class.java.name,
            ongoing = (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0,
            groupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
        )
    }

    private fun appLabelOf(pkg: String): String = appLabels.getOrPut(pkg) {
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        onNotificationPosted(sbn, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        runCatchingLogged(TAG, "onNotificationPosted callback failed") {
        if (sbn.packageName == packageName) return@runCatchingLogged
        scheduleCountRefresh()

        val notification = sbn.notification
        callPostGuard.notePosted(sbn.key, sbn.postTime)
        tracePost(sbn, rankingMap)

        // Apps like WhatsApp post two kinds of notifications per conversation.
        val isGroupSummary = (notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0 &&
            !com.agupta07505.smartisland.util.NotificationFilter.isCallNotification(notification)
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
                suppressSystemNotification(sbn.key)
            }
            traceStage(sbn, "DROPPED", "stage=group-summary shadeCancel=${currentSettings.hideFromNotificationShade}")
            pendingRemovals.remove(sbn.key)?.cancel()
            return@runCatchingLogged
        }

        // Suppress from the shade synchronously, before the coroutine is scheduled, so the notification never appears
        // there.
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
                if (shouldHideFromShade(notification, modeQuick, sbn.packageName)) {
                    markSuppressed(sbn.key)
                    traceStage(sbn, "SHADE-CANCEL", "mode=$modeQuick (island-only, hidden from the system shade)")
                    runCatchingLogged(TAG, "Immediate cancel failed") { cancelNotification(sbn.key) }
                    android.util.Log.d(TAG, "Immediate island-only suppress: ${sbn.key}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Immediate suppress exception", e)
        }

        pendingRemovals.remove(sbn.key)?.cancel()

        // SystemUI ends a screen recording by re-posting the SAME key as the "saved" notification
        // (no removal event), so the recording card has to be ended here, before any filter can drop the new post.
        endSupersededRecordingCard(sbn)

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationPosted async failed") {
                val asyncReason = suppressionReasonFor(sbn, rankingMap)
                if (asyncReason != null) {
                    traceStage(sbn, "DROPPED", "stage=async-filter reason=$asyncReason")
                    return@runSuspendCatchingLogged
                }

                val settings = repository.settings.first()
                currentSettings = settings
                if (!settings.enabled) {
                    traceStage(sbn, "DROPPED", "stage=async-settings reason=island-disabled")
                    return@runSuspendCatchingLogged
                }

                android.util.Log.d(TAG, "Processing island-only async: key=${sbn.key}")
                handleNotificationPosted(sbn, settings)
            }
        }
        }
    }

    /**
     * Reason-aware overload: REASON_LISTENER_CANCEL (our own suppression) keeps the island copy and suppressedKeys;
     * any other reason removes the card and the key.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
        runCatchingLogged(TAG, "onNotificationRemoved callback failed") {
        android.util.Log.d(TAG, "onNotificationRemoved: key=${sbn.key} pkg=${sbn.packageName} reason=$reason")
        scheduleCountRefresh()
        traceStage(sbn, "SYSTEM-REMOVED", "reason=$reason(${removalReasonName(reason)}) onIsland=${notificationRepository.notifications.value.any { it.key == sbn.key }}")

        pendingRemovals.remove(sbn.key)?.cancel()
        // Our own shade cancel keeps the island copy (and a cooldown release may re-handle it later).
        if (reason != REASON_LISTENER_CANCEL) callPostGuard.noteRemoved(sbn.key, sbn.postTime)

        val job = serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationRemoved handling failed") {
                delay(removalDelayMs(sbn.key))
                if (sbn.packageName == packageName) return@runSuspendCatchingLogged

                val now = SystemClock.elapsedRealtime()
                if (now < (replyGraceUntil[sbn.key] ?: 0L)) {
                    android.util.Log.d(TAG, "Removal right after an island reply, keeping the card: ${sbn.key}")
                    return@runSuspendCatchingLogged
                }
                replyGraceUntil.remove(sbn.key)
                val lastSuppressedTime = suppressedKeys[sbn.key] ?: 0L
                val isRecentInitialSuppression = (now - lastSuppressedTime) < INITIAL_SUPPRESSION_WINDOW_MS

                if (reason == REASON_LISTENER_CANCEL && isRecentInitialSuppression) {
                    // Suppressed from the shade less than 1.5 s ago: keep the island copy during the initial suppression.
                    android.util.Log.d(TAG, "Recent listener-cancel (<1.5s), keeping island: ${sbn.key}")
                    return@runSuspendCatchingLogged
                }

                android.util.Log.d(TAG, "Genuinely removed, cleaning up: ${sbn.key}")
                traceStage(sbn, "ISLAND-REMOVED", "reason=$reason(${removalReasonName(reason)})")
                com.agupta07505.smartisland.util.NotificationTrace.forget(sbn.key)
                latePictureJobs.remove(sbn.key)?.cancel()
                clearSuppressed(sbn.key)
                recordingStates.remove(sbn.key)
                com.agupta07505.smartisland.util.NotificationViewInspector.forget(sbn.key)
                notificationRepository.removeNotification(sbn.key)
            }
            pendingRemovals.remove(sbn.key)
        }
        pendingRemovals[sbn.key] = job
        }
    }

    private fun endSupersededRecordingCard(sbn: StatusBarNotification) {
        val held = notificationRepository.notifications.value
            .firstOrNull { it.key == sbn.key && it.mode == IslandMode.ScreenRecording } ?: return
        val modeNow = sbn.notification.toIslandMode(
            sbn,
            currentSettings.liveActivitiesEnabled,
            currentSettings.navigationEnabled,
            currentSettings.deviceType
        )
        if (modeNow == IslandMode.ScreenRecording) return
        traceStage(sbn, "ISLAND-REMOVED", "reason=recording-ended-by-repost heldMode=${held.mode} newMode=$modeNow")
        pendingRemovals.remove(sbn.key)?.cancel()
        clearSuppressed(sbn.key)
        recordingStates.remove(sbn.key)
        notificationRepository.removeNotification(sbn.key)
    }

    private fun removalReasonName(reason: Int): String = when (reason) {
        REASON_CLICK -> "click"
        REASON_CANCEL -> "cancel"
        REASON_CANCEL_ALL -> "cancel-all"
        REASON_LISTENER_CANCEL -> "listener-cancel"
        REASON_APP_CANCEL -> "app-cancel"
        REASON_TIMEOUT -> "timeout"
        else -> "other"
    }

    // Keep the no-arg override as a fallback (some OEMs may only call this one).
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        runCatchingLogged(TAG, "onNotificationRemoved fallback callback failed") {
        android.util.Log.d(TAG, "onNotificationRemoved (no reason): key=${sbn.key} pkg=${sbn.packageName}")
        scheduleCountRefresh()

        pendingRemovals.remove(sbn.key)?.cancel()
        // Without a reason it cannot be told from our own shade cancel, so it is not recorded as a removal for the stale-post guard.

        val job = serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationRemoved (no reason) handling failed") {
                delay(removalDelayMs(sbn.key))
                if (sbn.packageName == packageName) return@runSuspendCatchingLogged

                val now = SystemClock.elapsedRealtime()
                if (now < (replyGraceUntil[sbn.key] ?: 0L)) {
                    android.util.Log.d(TAG, "Removal right after an island reply, keeping the card: ${sbn.key}")
                    return@runSuspendCatchingLogged
                }
                replyGraceUntil.remove(sbn.key)
                val lastSuppressedTime = suppressedKeys[sbn.key] ?: 0L
                val isRecentInitialSuppression = (now - lastSuppressedTime) < INITIAL_SUPPRESSION_WINDOW_MS

                if (isRecentInitialSuppression) {
                    android.util.Log.d(TAG, "Suppressed key recently (<1.5s), keeping island: ${sbn.key}")
                    return@runSuspendCatchingLogged
                }

                android.util.Log.d(TAG, "Removing from island repo: ${sbn.key}")
                clearSuppressed(sbn.key)
                recordingStates.remove(sbn.key)
                com.agupta07505.smartisland.util.NotificationViewInspector.forget(sbn.key)
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
        scheduleCountRefresh()
        com.agupta07505.smartisland.util.AppLogRecorder.i(
            "ListenerLifecycle",
            "onListenerConnected pid=${android.os.Process.myPid()} " +
                com.agupta07505.smartisland.util.EnvironmentDiagnostics.snapshot(this)
        )
        startVoipMonitor()
        startRecordingWatchdog()
        startTransferExpirySweep()
        startMediaWatcher()
        runCatchingLogged(TAG, "onListenerConnected callback failed") {
            android.util.Log.d(TAG, "onListenerConnected")
            serviceScope.launch {
                runSuspendCatchingLogged(TAG, "ListenerConnected failed") {
                    initialShadeSync(trigger = "listener connected", onlyMissing = false)
                }
            }
        }
    }

    private suspend fun initialShadeSync(trigger: String, onlyMissing: Boolean) {
        val settings = repository.settings.first()
        currentSettings = settings
        if (!settings.enabled) {
            com.agupta07505.smartisland.util.AppLogRecorder.i("ListenerLifecycle", "initial sync ($trigger) skipped: the island is switched off")
            return
        }

        val overlayReady = ensureOverlayServiceRunning()
        com.agupta07505.smartisland.util.AppLogRecorder.i(
            "ListenerLifecycle",
            "initial sync ($trigger): overlayReady=$overlayReady (accessibilityService=${isAccessibilityServiceEnabled()}, canDrawOverlays=${Settings.canDrawOverlays(this@SmartIslandNotificationListenerService)})" +
                " shadeCount=${runCatching { activeNotifications?.size }.getOrNull()}"
        )
        val active = runCatchingLogged(TAG, "Failed to get active notifications") {
            activeNotifications?.toList()
        }?.filter { it.packageName != packageName }
            ?.filterNot { shouldSuppressFromIsland(it) }
            ?.filter { !onlyMissing || it.key !in notificationRepository.notifications.value.mapTo(HashSet()) { n -> n.key } }
            ?.sortedBy { it.postTime }
            .orEmpty()

        android.util.Log.d(TAG, "initial sync ($trigger): ${active.size} active, overlayReady=$overlayReady")

        active.forEach { sbn ->
            val mode = sbn.notification.toIslandMode(
                sbn,
                settings.liveActivitiesEnabled,
                settings.navigationEnabled,
                settings.deviceType
            )
            // On listener connect or rebind, restore persistent ongoing activities and read
            // every non-silent notification that sits in the shade. Silent ones stay out.
            val isLoudNotification = mode == IslandMode.Notification && !isSilentNotification(sbn)
            if (!isPersistentIslandMode(mode) && !isLoudNotification) {
                return@forEach
            }
            if (settings.hideFromNotificationShade &&
                shouldHideFromShade(sbn.notification, mode, sbn.packageName)
            ) {
                suppressSystemNotification(sbn.key)
            }
            handleNotificationPosted(sbn, settings, isInitialSync = true)
        }
        sweepStaleShadeCards()
        // The very first pass can run before the media sessions and their cards are ready (a process the system just
        // started): whatever persistent card (media, timer, call ...) is in the shade but still missing from the
        // island is imported again a little later, so the island does not stay empty until the next event.
        for (waitMs in INITIAL_RESYNC_DELAYS_MS) {
            delay(waitMs)
            resyncMissingPersistentCards()
        }
    }

    private fun resyncMissingPersistentCards() {
        val settings = currentSettings
        if (!settings.enabled) return
        val stored = notificationRepository.notifications.value.mapTo(HashSet()) { it.key }
        val missing = runCatchingLogged(TAG, "Failed to get active notifications for the startup re-sync") {
            activeNotifications?.toList()
        }.orEmpty()
            .filter { it.packageName != packageName && it.key !in stored }
            .filterNot { shouldSuppressFromIsland(it) }
            .filter { sbn ->
                val mode = sbn.notification.toIslandMode(sbn, settings.liveActivitiesEnabled, settings.navigationEnabled, settings.deviceType)
                // The same rule as the first pass: persistent live cards and loud notifications.
                isPersistentIslandMode(mode) || (mode == IslandMode.Notification && !isSilentNotification(sbn))
            }
            .sortedBy { it.postTime }
        if (missing.isEmpty()) return
        com.agupta07505.smartisland.util.AppLogRecorder.i(
            "ListenerLifecycle",
            "startup re-sync imported ${missing.size} card(s) the first pass missed: ${missing.map { it.key }}"
        )
        missing.forEach { handleNotificationPosted(it, settings, isInitialSync = true) }
    }

    private fun sweepStaleShadeCards() {
        val activeKeys = runCatchingLogged(TAG, "Stale card sweep could not read the shade") {
            activeNotifications?.mapTo(HashSet()) { it.key }
        } ?: return // the shade could not be read: drop nothing
        val stale = com.agupta07505.smartisland.util.StaleCardSweep.keysToDrop(notificationRepository.notifications.value, activeKeys)
        stale.forEach { key ->
            pendingRemovals.remove(key)?.cancel()
            latePictureJobs.remove(key)?.cancel()
            clearSuppressed(key)
            recordingStates.remove(key)
            notificationRepository.removeNotification(key)
        }
        if (stale.isNotEmpty()) {
            com.agupta07505.smartisland.util.AppLogRecorder.i("ListenerLifecycle", "stale card sweep dropped ${stale.size}: $stale")
        }
    }

    override fun onListenerDisconnected() {
        isSystemConnected = false
        com.agupta07505.smartisland.util.NotificationCountTracker.clear()
        mediaWatcher?.stop()
        mediaWatcher = null
        com.agupta07505.smartisland.util.AppLogRecorder.w(
            "ListenerLifecycle",
            "onListenerDisconnected: the system unbound the listener, requesting a rebind. While it is disconnected no notification or call reaches the island. " +
                com.agupta07505.smartisland.util.EnvironmentDiagnostics.snapshot(this)
        )
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
        // "Show the island during Do Not Disturb": cards stay; only the sound is held back.
        if (currentSettings.showDuringDnd) return
        serviceScope.launch {
            val activeMap = runCatchingLogged(TAG, "Failed to get active notifications for DND purge") {
                activeNotifications?.associateBy { it.key }
            }.orEmpty()

            val currentIsland = notificationRepository.notifications.value
            currentIsland.forEach { islandNotif ->
                val sbn = activeMap[islandNotif.key]
                if (sbn != null) {
                    if (isHiddenByDoNotDisturb(sbn, rankingMap)) {
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

    private var lastLoggedSettingsSignature: String? = null

    private fun logRelevantSettings(s: SmartIslandSettings) {
        if (!com.agupta07505.smartisland.util.AppLogRecorder.isRecording) return
        val signature = "enabled=${s.enabled} hideFromShade=${s.hideFromNotificationShade}" +
            " disabledPackages=${s.disabledNotificationPackages.sorted()}" +
            " disabledEvents=${s.disabledEvents.sorted()} disabledEventsLandscape=${s.disabledEventsLandscape.sorted()}" +
            " cooldown=${s.enableNotificationCooldown}(threshold=${s.notificationCooldownThreshold}," +
            "minutes=${s.notificationCooldownDurationMinutes},excluded=${s.notificationCooldownExcludedPackages.sorted()})" +
            " showInLandscape=${s.showInLandscape} showOnLockScreen=${s.showOnLockScreen} autoExpand=${s.autoExpandOnNotification}"
        if (signature == lastLoggedSettingsSignature) return
        lastLoggedSettingsSignature = signature
        com.agupta07505.smartisland.util.AppLogRecorder.i("SettingsTrace", signature)
    }

    private fun rankingImportance(sbn: StatusBarNotification, rankingMap: RankingMap?): Int? = runCatching {
        val rMap = rankingMap ?: currentRanking ?: return@runCatching null
        val ranking = Ranking()
        if (rMap.getRanking(sbn.key, ranking)) ranking.importance else null
    }.getOrNull()

    private fun tracePost(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        if (!com.agupta07505.smartisland.util.AppLogRecorder.isRecording) return
        runCatching {
            val n = sbn.notification
            val s = currentSettings
            val detailed = com.agupta07505.smartisland.util.NotificationTrace.isDetailed(sbn.packageName, n)
            val reason = suppressionReasonFor(sbn, rankingMap)
            val mode = n.toIslandMode(sbn, s.liveActivitiesEnabled, s.navigationEnabled, s.deviceType)
            val signature = "$reason|$mode|${n.flags}"
            if (!com.agupta07505.smartisland.util.NotificationTrace.shouldLog(sbn.key, signature, detailed)) return
            val event = listenerEventFor(n, mode, sbn.packageName)
            val dndFilter = runCatching { currentInterruptionFilter }.getOrNull()
            com.agupta07505.smartisland.util.AppLogRecorder.i(
                "NotifTrace",
                "POSTED " + com.agupta07505.smartisland.util.NotificationTrace.brief(sbn, rankingImportance(sbn, rankingMap)) +
                    " mode=$mode filter=${reason ?: "passed"} dndFilter=$dndFilter" +
                    " | islandEnabled=${s.enabled} hideFromShade=${s.hideFromNotificationShade}" +
                    " appDisabled=${sbn.packageName in s.disabledNotificationPackages}" +
                    " event=${event?.id} eventEnabled=${event?.let { s.isEventEnabled(it.id) }}" +
                    " eventOffSomewhere=${event?.let { s.isEventDisabledSomewhere(it.id) }}" +
                    " cooldown=${s.enableNotificationCooldown}(threshold=${s.notificationCooldownThreshold}," +
                    "excluded=${sbn.packageName in s.notificationCooldownExcludedPackages})"
            )
        }
    }

    private fun traceStage(sbn: StatusBarNotification, stage: String, detail: String = "") {
        if (!com.agupta07505.smartisland.util.AppLogRecorder.isRecording) return
        runCatching {
            val detailed = com.agupta07505.smartisland.util.NotificationTrace.isDetailed(sbn.packageName, sbn.notification)
            if (!com.agupta07505.smartisland.util.NotificationTrace.shouldLog(sbn.key, "$stage|$detail", detailed)) return
            com.agupta07505.smartisland.util.AppLogRecorder.i(
                "NotifTrace",
                "$stage pkg=${sbn.packageName} key=${sbn.key}${if (detail.isEmpty()) "" else " $detail"}"
            )
        }
    }

    private fun isIncomingCall(notification: Notification): Boolean =
        CallStyleExtras.isRinging(notification)

    private fun handleNotificationPosted(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings,
        isInitialSync: Boolean = false
    ) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification
        val handleReason = suppressionReasonFor(sbn)
        if (handleReason != null) {
            traceStage(sbn, "DROPPED", "stage=handle-filter reason=$handleReason initialSync=$isInitialSync")
            return
        }

        val extras = notification.extras
        val mode = notification.toIslandMode(
            sbn,
            settings.liveActivitiesEnabled,
            settings.navigationEnabled,
            settings.deviceType
        )
        android.util.Log.d(TAG, "handleNotificationPosted: mode=$mode key=${sbn.key} title=${extras.getCharSequence(Notification.EXTRA_TITLE)}")

        // Quiet imports of what is already in the shade are not new alerts, so they neither count
        // towards the spam threshold nor get buffered.
        val cooldownText = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)}".lowercase()
        val transferRelated = com.agupta07505.smartisland.util.TransferClassifier.isCooldownExemptTransfer(sbn.packageName, cooldownText)
        if (!isInitialSync && NotificationCooldownManager.shouldThrottle(sbn, settings, mode, transferRelated = transferRelated)) {
            android.util.Log.d(TAG, "Notification throttled by cooldown: pkg=${sbn.packageName} key=${sbn.key}")
            scheduleCooldownRelease(sbn.packageName, settings.notificationCooldownDurationMinutes)
            traceStage(
                sbn, "DROPPED",
                "stage=cooldown-throttle reason=anti-spam threshold=${settings.notificationCooldownThreshold}" +
                    " durationMin=${settings.notificationCooldownDurationMinutes} (buffered, released after the cooldown)"
            )
            return
        }

        val shouldIslandOnly = shouldBeIslandOnly(notification, mode)

        val removedFromShade = settings.hideFromNotificationShade &&
            shouldIslandOnly &&
            shouldHideFromShade(notification, mode, sbn.packageName)
        if (removedFromShade) {
            // Ensure the shade copy is gone: after onNotificationPosted the sync cancel already ran and this starts the async retry loop;
            // from onListenerConnected it is the only suppress call.
            suppressSystemNotification(sbn.key)
        }

        val mediaInfo = if (mode == IslandMode.Music) findMediaInfo(notification, sbn.packageName) else null
        val appName = runCatchingLogged(TAG, "GetApplicationInfo failed") {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } ?: sbn.packageName

        val isNewNotif = notificationRepository.notifications.value.none { it.key == sbn.key }

        val existingNotif = notificationRepository.notifications.value.find { it.key == sbn.key || (it.mode == IslandMode.IncomingCall && it.packageName == sbn.packageName) }
        val standardActions = notification.actions?.mapNotNull { action ->
            action.title?.toString()?.let { title ->
                val remoteInput = action.remoteInputs?.firstOrNull()
                val isReply = remoteInput != null || title.lowercase().contains("reply")
                IslandNotificationAction(
                    title = title,
                    pendingIntent = action.actionIntent,
                    isQuickReply = isReply,
                    remoteInputKey = remoteInput?.resultKey ?: "key_text_reply",
                    icon = if (mode == IslandMode.Music) loadActionIcon(action, sbn.packageName) else null
                )
            }
        }.orEmpty().let { base ->
            // Apps that hand CallStyle buttons to the system without a label would lose them above.
            if (mode == IslandMode.IncomingCall) CallStyleExtras.withCallIntents(notification, base) else base
        }

        val isMiuiRom = com.agupta07505.smartisland.util.OemDeviceRules.isMiuiOrHyperOs(settings.deviceType)

        @Suppress("DEPRECATION")
        val focusActions: List<IslandNotificationAction> = if (isMiuiRom) {
            extras.getBundle("miui.focus.actions")
                ?.let { focusBundle ->
                    runCatching {
                        focusBundle.keySet().mapNotNull { actionKey ->
                            val focusAction =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    focusBundle.getParcelable(
                                        actionKey,
                                        Notification.Action::class.java
                                    )
                                } else {
                                    focusBundle.getParcelable<Notification.Action>(
                                        actionKey
                                    )
                                } ?: return@mapNotNull null

                            focusAction.title?.toString()?.let { title ->
                                val remoteInput = focusAction.remoteInputs?.firstOrNull()

                                IslandNotificationAction(
                                    title = title,
                                    pendingIntent = focusAction.actionIntent,
                                    isQuickReply = remoteInput != null ||
                                        title.lowercase().contains("reply"),
                                    remoteInputKey =
                                        remoteInput?.resultKey ?: "key_text_reply"
                                )
                            }
                        }
                    }.getOrNull()
                }.orEmpty()
        } else {
            emptyList()
        }

        val baseActions = (standardActions + focusActions)
            .distinctBy { it.title.trim().lowercase() to it.pendingIntent }

        // Some clocks and recorders keep time and buttons in a custom view. Phone makers' clocks keep their old handling.
        val isShadeFixCard = (mode == IslandMode.Timer || mode == IslandMode.Stopwatch) &&
            com.agupta07505.smartisland.util.TimerStopwatchParser.usesShadeFixes(sbn.packageName)
        val usesCustomViews = mode == IslandMode.Stopwatch || isShadeFixCard ||
            mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording
        val viewInfo = if (usesCustomViews) {
            runCatchingLogged(TAG, "Custom notification views could not be read") {
                com.agupta07505.smartisland.util.NotificationViewInspector.inspect(this, notification, sbn.key)
            }
        } else {
            null
        }
        val actions = if (
            viewInfo != null && viewInfo.buttons.isNotEmpty() &&
            (mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording) &&
            baseActions.none {
                it.pendingIntent != null &&
                    com.agupta07505.smartisland.ui.expanded.classifyAction(it.title) in
                    arrayOf(
                        com.agupta07505.smartisland.ui.expanded.ActionRole.Pause,
                        com.agupta07505.smartisland.ui.expanded.ActionRole.Resume
                    )
            }
        ) {
            baseActions.filter { it.pendingIntent != null } + viewInfo.buttons
        } else if (isShadeFixCard) {
            com.agupta07505.smartisland.util.ShadeActions.mirror(
                baseActions,
                viewInfo?.buttons.orEmpty(),
                if (mode == IslandMode.Stopwatch) {
                    com.agupta07505.smartisland.util.TimerStopwatchParser.ClockKind.Stopwatch
                } else {
                    com.agupta07505.smartisland.util.TimerStopwatchParser.ClockKind.Timer
                }
            ).also { shown ->
                com.agupta07505.smartisland.util.AppLogRecorder.d(
                    "ShadeButtons",
                    "pkg=${sbn.packageName} mode=$mode standard=${baseActions.map { it.title }}" +
                        " viewShown=${viewInfo?.buttons?.map { it.title }}" +
                        " viewHidden=${viewInfo?.hiddenButtons} island=${shown.map { it.title }}"
                )
            }
        } else {
            baseActions
        }
        val isNowRinging = actions.any { VoipCallDetector.classifyCallAction(it.title) == CallActionRole.Answer } ||
            (mode == IslandMode.IncomingCall && CallStyleExtras.looksRinging(notification))
        // An outgoing call that is still dialing: no timer until the other side picks up.
        val connectingText = if (mode == IslandMode.IncomingCall && !isNowRinging) {
            CallStyleExtras.connectingStatus(notification)
        } else {
            null
        }
        val isNowConnecting = connectingText != null

        // Recorder and stopwatch cards keep a pause-aware clock instead of trusting `Notification.when`.
        val isRecorder = mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording
        val isStopwatch = mode == IslandMode.Stopwatch
        val recordingState = if (isRecorder || isStopwatch) {
            val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val rawBig = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val rawSub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val nowMs = System.currentTimeMillis()
            val clockKey = "${sbn.packageName}|${mode.name}"
            val clockPaused = if (isStopwatch) {
                // Stays paused across the app's cancel-and-repost until it offers Pause again.
                RecordingClock.resolvePaused(
                    detected = com.agupta07505.smartisland.util.TimerStopwatchParser.isStopwatchPaused(notification) ||
                        (isShadeFixCard && com.agupta07505.smartisland.util.ShadeActions.offersResume(actions)),
                    lastForApp = recordingStates[sbn.key] ?: lastClocks[clockKey],
                    actionTitles = actions.map { it.title }
                )
            } else {
                RecordingClock.isPaused(actions.map { it.title }, "$rawTitle $rawText $rawBig")
            }
            val previousState = RecordingClock.continuation(recordingStates[sbn.key], lastClocks[clockKey], clockPaused)
            // `when` is only the post time; the view's chronometer has the real elapsed time.
            val viewStartMs = viewInfo?.chronometerElapsedMs
                ?.takeIf { !clockPaused }
                ?.let { nowMs - it }
            val chronometerBase = viewStartMs ?: if (!extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) &&
                notification.`when` in 1..nowMs &&
                // `when` is trusted as the start only before the first clock.
                (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) ||
                    (isStopwatch && previousState == null))
            ) notification.`when` else null
            val stockStopwatch = isShadeFixCard && isStopwatch
            val showsChronometer = !extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) &&
                extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) && notification.`when` in 1..nowMs
            val exactRunningBase: Long? = if (stockStopwatch && !clockPaused) {
                viewStartMs ?: notification.`when`.takeIf { showsChronometer }
            } else {
                null
            }
            // Stopped chronometer corrected to the post time.
            val exactFrozenMs: Long? = if (stockStopwatch && clockPaused) {
                viewInfo?.takeIf { !it.chronometerCountDown }?.let { cv ->
                    cv.chronometerBaseMs?.let { base ->
                        val applyDelayMs = (nowMs - sbn.postTime).coerceAtLeast(0L)
                        com.agupta07505.smartisland.util.TimerClock.elapsedFromChronometer(
                            base,
                            SystemClock.elapsedRealtime() - applyDelayMs,
                            cv.shownTimeSeconds?.takeIf { cv.shownFromChronometer },
                            applyDelayMs
                        )
                    }
                }
            } else {
                null
            }
            if (stockStopwatch) {
                com.agupta07505.smartisland.util.AppLogRecorder.d(
                    "StopwatchClock",
                    "pkg=${sbn.packageName} paused=$clockPaused exactFrozenMs=$exactFrozenMs exactRunningBase=$exactRunningBase" +
                        " shownSec=${viewInfo?.shownTimeSeconds} chronoBase=${viewInfo?.chronometerBaseMs}" +
                        " postAgeMs=${nowMs - sbn.postTime}"
                )
            }
            RecordingClock.next(
                previous = previousState,
                nowMillis = nowMs,
                paused = clockPaused,
                finished = !isStopwatch && notification.isScreenRecordingComplete(),
                parsedMs = if (stockStopwatch) {
                    if (clockPaused) {
                        RecordingClock.frozenReading(
                            paused = true,
                            shownMs = viewInfo?.shownTimeSeconds?.let { it * 1000L }?.let { shown ->
                                if (viewInfo?.shownFromChronometer == true && !viewInfo.chronometerCountDown) {
                                    RecordingClock.withoutPostAge(shown, nowMs - sbn.postTime)
                                } else {
                                    shown
                                }
                            },
                            parsedMs = RecordingClock.parseElapsedMs(rawTitle, rawText, rawBig, rawSub)
                        )
                    } else if (exactRunningBase != null) {
                        null
                    } else {
                        RecordingClock.parseElapsedMs(rawTitle, rawText, rawBig, rawSub)
                    }
                } else {
                    RecordingClock.parseElapsedMs(rawTitle, rawText, rawBig, rawSub)
                },
                chronometerBaseMs = exactRunningBase ?: chronometerBase,
                fallbackFrozenMs = if (clockPaused) {
                    RecordingClock.withoutPostAge(viewInfo?.chronometerElapsedMs, nowMs - sbn.postTime)
                } else {
                    viewInfo?.chronometerElapsedMs
                },
                exactFrozenMs = exactFrozenMs,
                exactStart = exactRunningBase != null
            ).also {
                recordingStates[sbn.key] = it
                lastClocks[clockKey] = it
            }
        } else {
            null
        }

        // A paused timer's text often shows its full length, so the frozen value is used instead.
        val timerPaused = mode == IslandMode.Timer &&
            (com.agupta07505.smartisland.util.TimerStopwatchParser.isTimerPaused(notification) ||
                (isShadeFixCard && com.agupta07505.smartisland.util.ShadeActions.offersResume(actions)) ||
                // Stays paused across the app's cancel-and-repost until it offers Pause again.
                (isShadeFixCard && lastTimerFrozen[sbn.packageName] != null &&
                    !com.agupta07505.smartisland.util.ShadeActions.offersPause(actions) &&
                    !com.agupta07505.smartisland.util.TimerStopwatchParser.isTimerFinished(notification)))
        val timerViewRunningMs: Long? = if (mode == IslandMode.Timer && !timerPaused) {
            viewInfo?.countdownRemainingMs
        } else {
            null
        }
        val timerViewShownMs: Long? = if (mode == IslandMode.Timer) {
            viewInfo?.shownTimeSeconds?.let { it * 1000L }
        } else {
            null
        }
        val timerFrozenMs: Long? = if (timerPaused) {
            val nowMs = System.currentTimeMillis()
            val pauseAtMs = sbn.postTime.coerceIn(nowMs - 10_000L, nowMs)
            val runningTarget = existingNotif
                ?.takeIf { it.mode == IslandMode.Timer && !it.recordingPaused && it.timeMillis > pauseAtMs }
                ?.timeMillis
                ?: lastTimerTargets[sbn.packageName]?.takeIf { it > pauseAtMs }
            val keptFrozen = existingNotif
                ?.takeIf { it.mode == IslandMode.Timer && it.recordingPaused }
                ?.recordingFrozenMs
            val clockTextSec = com.agupta07505.smartisland.util.TimerStopwatchParser.parseTimerRemainingSeconds(notification)
            val chronoView = viewInfo?.takeIf { isShadeFixCard && it.chronometerCountDown }
            val shadeExactMs = chronoView?.let { cv ->
                cv.chronometerBaseMs?.let { base ->
                    val applyDelayMs = (nowMs - sbn.postTime).coerceAtLeast(0L)
                    com.agupta07505.smartisland.util.TimerClock.frozenFromChronometer(
                        base,
                        SystemClock.elapsedRealtime() - applyDelayMs,
                        cv.shownTimeSeconds?.takeIf { cv.shownFromChronometer },
                        applyDelayMs
                    )
                }
            }
            com.agupta07505.smartisland.util.TimerFreeze.remainingMs(
                nowMs = pauseAtMs,
                rememberedFrozenMs = lastTimerFrozen[sbn.packageName],
                runningTargetMs = runningTarget,
                keptFrozenMs = keptFrozen,
                textRemainingMs = timerViewShownMs?.takeIf { it > 0L } ?: clockTextSec?.let { it * 1000L },
                shadeShownMs = if (isShadeFixCard && viewInfo?.shownFromChronometer == true) timerViewShownMs else null,
                shadeExactMs = shadeExactMs,
                shownMs = if (isShadeFixCard) timerViewShownMs?.takeIf { it > 0L } else null,
                previousShownMs = lastTimerShown[sbn.packageName]
            ).also {
                lastTimerFrozen[sbn.packageName] = it
                if (isShadeFixCard) timerViewShownMs?.takeIf { shown -> shown > 0L }?.let { shown -> lastTimerShown[sbn.packageName] = shown }
                if (isShadeFixCard) {
                    com.agupta07505.smartisland.util.AppLogRecorder.d(
                        "TimerFreeze",
                        "pkg=${sbn.packageName} frozenMs=$it exactFromChronometer=$shadeExactMs" +
                            " shownSec=${viewInfo?.shownTimeSeconds} chronoBase=${viewInfo?.chronometerBaseMs}" +
                            " postAgeMs=${nowMs - sbn.postTime}"
                    )
                }
            }
        } else {
            if (mode == IslandMode.Timer) {
                lastTimerFrozen.remove(sbn.packageName)
                lastTimerShown.remove(sbn.packageName)
            }
            null
        }

        val stopwatchElapsedSeconds: Long?
        val stopwatchChronometerBase: Long?
        val timerRemainingSeconds: Long?

        if (isMiuiRom) {
            val stopwatchIsPaused = mode == IslandMode.Stopwatch &&
                com.agupta07505.smartisland.util.TimerStopwatchParser.isStopwatchPaused(notification)

            stopwatchElapsedSeconds = if (stopwatchIsPaused) {
                val previousStopwatch = existingNotif?.takeIf { it.mode == IslandMode.Stopwatch }
                val parsedElapsed = com.agupta07505.smartisland.util.TimerStopwatchParser
                    .parseStopwatchElapsedSeconds(notification)

                parsedElapsed
                    ?: previousStopwatch?.stopwatchElapsedSeconds
                    ?: previousStopwatch?.let { previous ->
                        ((System.currentTimeMillis() - previous.timeMillis) / 1000L).coerceAtLeast(0L)
                    }
            } else {
                null
            }

            stopwatchChronometerBase = if (mode == IslandMode.Stopwatch && !stopwatchIsPaused) {
                runCatching {
                    notification.extras?.getLong("android.chronometerBase", 0L)
                }.getOrNull()?.takeIf { it > 0L }
            } else {
                null
            }

            val nowMillis = System.currentTimeMillis()
            val timerIsPaused = mode == IslandMode.Timer &&
                com.agupta07505.smartisland.util.TimerStopwatchParser.isTimerPaused(notification)

            timerRemainingSeconds = if (timerIsPaused) {
                val previousTimer = existingNotif?.takeIf { it.mode == IslandMode.Timer }

                previousTimer?.timerRemainingSeconds
                    ?: previousTimer?.let { old ->
                        ((old.timeMillis - nowMillis + 999L) / 1000L).coerceAtLeast(0L)
                    }
                    ?: notification.`when`.takeIf { it > nowMillis }?.let { deadline ->
                        ((deadline - nowMillis + 999L) / 1000L).coerceAtLeast(0L)
                    }
            } else {
                null
            }
        } else {
            stopwatchElapsedSeconds = null
            stopwatchChronometerBase = null
            timerRemainingSeconds = null
        }

        val voipLive = com.agupta07505.smartisland.util.VoipCallSignal.active
        val computedTimeMillis = when {
            recordingState != null -> recordingState.startMillis
            // A running call's chronometer start is the real call start.
            mode == IslandMode.IncomingCall && !isNowRinging &&
                extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) &&
                notification.`when` in 1..System.currentTimeMillis() -> notification.`when`
            // The call was answered (incoming) or picked up (outgoing): the clock starts now.
            mode == IslandMode.IncomingCall && existingNotif != null && !isNowRinging && !isNowConnecting &&
                (existingNotif.isCallRinging || existingNotif.isCallConnecting) -> {
                System.currentTimeMillis()
            }
            existingNotif != null && mode == IslandMode.IncomingCall && !isNowRinging && existingNotif.timeMillis > 0 -> {
                existingNotif.timeMillis
            }
            mode == IslandMode.IncomingCall && !isNowRinging && voipLive && existingNotif == null &&
                com.agupta07505.smartisland.util.VoipCallSignal.sinceMillis > 0 ->
                com.agupta07505.smartisland.util.VoipCallSignal.sinceMillis
            mode == IslandMode.Timer && timerFrozenMs != null -> System.currentTimeMillis() + timerFrozenMs
            // The shade's own count-down chronometer is the exact time left.
            mode == IslandMode.Timer && timerViewRunningMs != null -> {
                val target = System.currentTimeMillis() + timerViewRunningMs
                lastTimerTargets[sbn.packageName] = target
                target
            }
            // No count-down chronometer, but the shade prints the time ("04:59") in the view.
            mode == IslandMode.Timer && !timerPaused && (timerViewShownMs ?: 0L) > 0L -> {
                // Whole-second text: the real time left is up to a second more than it prints.
                val target = com.agupta07505.smartisland.util.TimerClock
                    .targetFromShownSeconds(System.currentTimeMillis(), timerViewShownMs!! / 1000L)
                lastTimerTargets[sbn.packageName] = target
                target
            }
            mode == IslandMode.Timer -> {
                if (isMiuiRom) {
                    val preciseDeadline = com.agupta07505.smartisland.util.TimerStopwatchParser
                        .parseTimerDeadlineMillis(notification)
                    if (preciseDeadline != null) {
                        lastTimerTargets[sbn.packageName] = preciseDeadline
                        preciseDeadline
                    } else {
                        val remSec = com.agupta07505.smartisland.util.TimerStopwatchParser
                            .parseTimerRemainingSeconds(notification)
                        val target = if (remSec != null && remSec > 0) {
                            System.currentTimeMillis() + remSec * 1000L
                        } else if (notification.`when` > System.currentTimeMillis()) {
                            notification.`when`
                        } else if (existingNotif != null && existingNotif.mode == IslandMode.Timer && existingNotif.timeMillis > System.currentTimeMillis()) {
                            existingNotif.timeMillis
                        } else {
                            System.currentTimeMillis()
                        }
                        lastTimerTargets[sbn.packageName] = target
                        target
                    }
                } else {
                    val remSec = com.agupta07505.smartisland.util.TimerStopwatchParser.parseTimerRemainingSeconds(notification)
                    // A stock / third-party clock's own deadline is exact to the millisecond; rounding it to seconds
                    // would put the island up to half a second off the shade.
                    val exactDeadline = if (isShadeFixCard) {
                        com.agupta07505.smartisland.util.TimerStopwatchParser.parseTimerDeadlineExact(notification)
                    } else {
                        null
                    }
                    val target = if (exactDeadline != null) {
                        exactDeadline
                    } else if (remSec != null && remSec > 0) {
                        if (isShadeFixCard) {
                            com.agupta07505.smartisland.util.TimerClock
                                .targetFromShownSeconds(System.currentTimeMillis(), remSec)
                        } else {
                            System.currentTimeMillis() + remSec * 1000L
                        }
                    } else if (notification.`when` > System.currentTimeMillis()) {
                        notification.`when`
                    } else if (existingNotif != null && existingNotif.mode == IslandMode.Timer && existingNotif.timeMillis > System.currentTimeMillis()) {
                        existingNotif.timeMillis
                    } else {
                        System.currentTimeMillis()
                    }
                    lastTimerTargets[sbn.packageName] = target
                    target
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

        // A media card's title and artist come from the live session when it has them: a re-post that was built from
        // the previous track must not put the old title back over the one the session reported a moment ago.
        val notifTitle = mediaInfo?.title?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: (if (mode == IslandMode.IncomingCall) CallStyleExtras.callerName(notification) else null)
            ?: (if (mode == IslandMode.Stopwatch) "Stopwatch" else if (mode == IslandMode.Timer) "Timer" else "")
        val notifText = mediaInfo?.artist?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: notification.tickerText?.toString()
            ?: (if (mode == IslandMode.Stopwatch) "Running" else if (mode == IslandMode.Timer) "Running" else "")

        // Transfer cards: score the notification once for state, speed/ETA text and a progress
        // value when the app has no real progress bar (Android 16 progress style, text-only apps).
        val transferVerdict = if (mode == IslandMode.DownloadUpload) {
            com.agupta07505.smartisland.util.TransferDetector.evaluate(
                com.agupta07505.smartisland.util.TransferInput.fromNotification(notification, sbn.packageName, false)
            )
        } else {
            null
        }
        val barProgress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val barProgressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val textPercent = if (barProgressMax <= 0) transferVerdict?.percent else null

        traceStage(
            sbn, "ISLAND-POST",
            "mode=$mode islandOnly=$shouldIslandOnly removedFromShade=$removedFromShade new=$isNewNotif" +
                " ringing=$isNowRinging initialSync=$isInitialSync" +
                " autoExpandSetting=${settings.autoExpandOnNotification}"
        )
        val pictures = if (mode == IslandMode.Notification) {
            runCatchingLogged(TAG, "Notification picture could not be read") {
                // Fast read only: the card must not wait for Shizuku's shell. A picture that is not ready is asked for again below.
                mediaReader.read(sbn.key, notification, notifText, sbn.packageName, allowFallbacks = false)
            }.orEmpty()
        } else {
            emptyList()
        }
        if (callPostGuard.isStale(sbn.key, sbn.postTime)) {
            // A newer post for this key, its removal, or the user's dismissal was already seen: this snapshot is out of date and
            // must not bring the card back (any slow handler could otherwise re-add a gone card).
            traceStage(sbn, "DROPPED", "stage=stale-post mode=$mode postTime=${sbn.postTime} (newer post, removal or dismissal already seen)")
            return
        }
        if (mode == IslandMode.IncomingCall) {
            // The ongoing / dialing call replaces the app's older call cards (the ringing one), which the
            // system never reports as removed because the island cancelled that notification itself.
            val replaced = com.agupta07505.smartisland.util.CallCardRules.supersededKeys(
                notificationRepository.notifications.value, sbn.key, sbn.packageName, isNowRinging
            )
            replaced.forEach { oldKey ->
                pendingRemovals.remove(oldKey)?.cancel()
                clearSuppressed(oldKey)
                voipPromotedKeys.remove(oldKey)
                notificationRepository.removeNotification(oldKey)
            }
            com.agupta07505.smartisland.util.AppLogRecorder.i(
                "CallCard",
                "pkg=${sbn.packageName} key=${sbn.key} ringing=$isNowRinging connecting=$isNowConnecting" +
                    " buttons=${actions.map { it.title }} status='${connectingText.orEmpty()}'" +
                    " replacedOldCards=$replaced"
            )
        }
        // The system delivering the very same post again (a group update, a re-delivery after the card had left the
        // island) is not a new alert: the card may come back quietly, but it must not open the island by itself a
        // second time.
        val repostOfShownPost = !isInitialSync && alreadyShown(sbn)
        if (repostOfShownPost) {
            traceStage(sbn, "NO-AUTO-EXPAND", "stage=repost-of-shown-post mode=$mode postTime=${sbn.postTime} (already shown once, no second auto-expand)")
        }
        noteShown(sbn)
        notificationRepository.postNotification(
            IslandNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = notifTitle,
                text = notifText,
                timeMillis = computedTimeMillis,
                lastPostedMs = System.currentTimeMillis(),
                icon = loadAppIconBitmap(sbn.packageName),
                smallIcon = if (mode == IslandMode.Music) notification.loadSmallIconBitmap(sbn.packageName) else null,
                largeIcon = mediaInfo?.artwork ?: notification.loadLargeIconBitmap(),
                actionIntents = actions,
                category = notification.category,
                progress = textPercent ?: barProgress,
                progressMax = if (textPercent != null) 100 else barProgressMax,
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
                contentIntent = notification.contentIntent,
                stopwatchElapsedSeconds = stopwatchElapsedSeconds,
                stopwatchChronometerBase = stopwatchChronometerBase,
                timerRemainingSeconds = timerRemainingSeconds,
                exactClock = isShadeFixCard && mode == IslandMode.Timer,
                transferKind = if (mode == IslandMode.DownloadUpload) {
                    notification.transferKind(sbn.packageName)
                } else {
                    TransferKind.DOWNLOAD
                },
                transferState = transferVerdict?.state ?: com.agupta07505.smartisland.model.TransferState.ACTIVE,
                transferDetail = transferVerdict?.detail.orEmpty(),
                recordingFrozenMs = recordingState?.frozenMs ?: timerFrozenMs,
                recordingPaused = (recordingState?.frozenMs != null &&
                    (isStopwatch || !notification.isScreenRecordingComplete())) ||
                    (mode == IslandMode.Timer && (timerPaused ||
                        com.agupta07505.smartisland.util.TimerStopwatchParser.isTimerPaused(notification))),
                // Only plain notifications can be "silent"; live cards are usually low importance on purpose.
                isSilent = mode == IslandMode.Notification && isSilentNotification(sbn),
                callStatusText = connectingText.orEmpty(),
                media = pictures,
                readerText = if (pictures.isNotEmpty()) {
                    runCatchingLogged(TAG, "Picture reader text could not be read") {
                        notification.readerTextWithoutPictures(notifText)
                    }.orEmpty()
                } else {
                    ""
                },
                bigText = if (mode == IslandMode.Notification) {
                    runCatchingLogged(TAG, "Expanded text could not be read") {
                        notification.expandedText()
                    }.orEmpty()
                } else {
                    ""
                }
            ),
            autoExpand = if (isMiuiRom) {
                when {
                    isInitialSync -> false
                    mode == IslandMode.Timer || mode == IslandMode.Stopwatch -> true
                    shouldIslandOnly && settings.autoExpandOnNotification -> {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastAutoExpandTimeMs >= AUTO_EXPAND_DEBOUNCE_MS) {
                            lastAutoExpandTimeMs = now
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            } else {
                if (!isInitialSync && !repostOfShownPost && shouldIslandOnly && AppEventRules.mayAutoExpand(
                        globalAutoExpand = settings.autoExpandOnNotification,
                        noAutoExpandPackages = settings.noAutoExpandPackages,
                        autoExpandPackages = settings.autoExpandPackages,
                        noExpandPackages = settings.noExpandPackages,
                        packageName = sbn.packageName,
                        mode = mode,
                        isLoud = !(mode == IslandMode.Notification && isSilentNotification(sbn))
                    )
                ) {
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
            }
        )

        if (mode == IslandMode.Notification && pictures.isEmpty() && mediaReader.expectsPicture(notification, notifText)) {
            scheduleLatePicture(sbn, notification, notifText)
        }

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

        // The island only makes a sound when it replaces the system notification. While the shade copy stays, the
        // system plays its own sound and a second one would double it.
        val isRecordingEvent = mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording ||
            isRecordingNotification(sbn, notification)
        if (!isInitialSync && removedFromShade && isNewNotif && (mode == IslandMode.Notification || shouldIslandOnly) &&
            mode != IslandMode.DownloadUpload && !isRecordingEvent
        ) {
            playNotificationSound(sbn)
        }

        // Every ordinary notification keeps its own card, keyed by the system notification key.

        if (mode == IslandMode.Music) {
            val existing = notificationRepository.notifications.value
            // Only replace the previous player card; the app's other cards (a download, a
            // timer, a chat message) must stay.
            existing.filter { it.mode == IslandMode.Music && it.packageName == sbn.packageName && it.key != sbn.key }
                .forEach { notificationRepository.removeNotification(it.key) }
        }

        if (mode == IslandMode.IncomingCall && com.agupta07505.smartisland.util.VoipCallSignal.active) {
            voipPromotedKeys.add(sbn.key)
            notificationRepository.removeNotification(VOIP_CARD_KEY)
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
            val failed = com.agupta07505.smartisland.util.TransferDetector.evaluate(
                com.agupta07505.smartisland.util.TransferInput.fromNotification(notification, sbn.packageName, false)
            ).state == com.agupta07505.smartisland.model.TransferState.FAILED
            val isDone = failed || notification.isDownloadComplete() || (progressMax > 0 && progress >= progressMax)
            if (isDone) {
                pendingRemovals.remove(sbn.key)?.cancel()
                val lingerMs = if (failed) {
                    com.agupta07505.smartisland.util.IslandTimings.TRANSFER_FAILED_MS
                } else if (isInitialSync) {
                    com.agupta07505.smartisland.util.IslandTimings.TRANSFER_DONE_IMPORTED_MS
                } else {
                    com.agupta07505.smartisland.util.IslandTimings.TRANSFER_DONE_MS
                }
                val job = serviceScope.launch {
                    delay(lingerMs)
                    clearSuppressed(sbn.key)
                    notificationRepository.removeNotification(sbn.key)
                }
                pendingRemovals[sbn.key] = job
            }
        }

        if (mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording) {
            val isDone = notification.isScreenRecordingComplete()
            if (isDone) {
                pendingRemovals.remove(sbn.key)?.cancel()
                val job = serviceScope.launch {
                    delay(com.agupta07505.smartisland.util.IslandTimings.RECORDING_DONE_MS)
                    clearSuppressed(sbn.key)
                    notificationRepository.removeNotification(sbn.key)
                }
                pendingRemovals[sbn.key] = job
            }
        }

        if (mode == IslandMode.Timer) {
            // A timer whose shade view still counts down is not finished, whatever its text says.
            val isDone = com.agupta07505.smartisland.util.TimerStopwatchParser.isTimerFinished(notification) &&
                (timerViewRunningMs ?: 0L) <= 0L
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

    /**
     * True when Do Not Disturb / Bedtime mode keeps [sbn] off the island: the notification is
     * silenced by the DND policy AND that policy also hides notifications from the shade.
     *
     * The island is a visual surface. A mode that only mutes sound (the notification still lands
     * in the shade, as in a "no sound, shade allowed" Bedtime setup) must not hide the island or
     * the live activities built from notifications; the sound is still held back separately
     * through [isBlockedByDoNotDisturb].
     */
    internal fun isHiddenByDoNotDisturb(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean {
        if (!isBlockedByDoNotDisturb(sbn, rankingMap, filterOverride)) return false
        return dndHidesNotificationList()
    }

    private fun dndHidesNotificationList(): Boolean {
        // The "hide from the shade" effect only exists from Android 9 (API 28); before that DND
        // never removes notifications from the shade, so nothing is hidden.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return false
        val policy = runCatchingLogged(TAG, "Failed to read the DND policy") {
            (getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notificationPolicy
        } ?: return false
        return (policy.suppressedVisualEffects and NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST) != 0
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
        if (mode == IslandMode.Music || mode == IslandMode.Hotspot || mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording) {
            return false
        }

        val rMap = rankingMap ?: runCatchingLogged(TAG, "Failed to get current ranking") { currentRanking }
        if (rMap != null) {
            val ranking = Ranking()
            if (rMap.getRanking(sbn.key, ranking)) {
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

    /**
     * True when the system lists the notification as "Silent": no sound, no heads-up (importance below DEFAULT).
     * Unknown ranking counts as not silent.
     */
    internal fun isSilentNotification(sbn: StatusBarNotification, rankingMap: RankingMap? = null): Boolean {
        val rMap = rankingMap ?: runCatchingLogged(TAG, "Failed to get current ranking") { currentRanking }
            ?: return false
        val ranking = Ranking()
        if (!rMap.getRanking(sbn.key, ranking)) return false
        val importance = ranking.importance
        return importance != NotificationManager.IMPORTANCE_UNSPECIFIED &&
            importance < NotificationManager.IMPORTANCE_DEFAULT
    }

    private fun importShadeNotifications() {
        val settings = currentSettings
        if (!settings.enabled) return
        val active = runCatchingLogged(TAG, "Failed to get active notifications for shade import") {
            activeNotifications?.toList()
        }.orEmpty()
        val stored = notificationRepository.notifications.value.mapTo(HashSet()) { it.key }
        active
            .filter { it.packageName != packageName && it.key !in stored && !alreadyShown(it) }
            .filter { ((it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 ||
            com.agupta07505.smartisland.util.NotificationFilter.isCallNotification(it.notification)) }
            .filterNot { shouldSuppressFromIsland(it) }
            .filterNot { isSilentNotification(it) }
            .filter { sbn ->
                sbn.notification.toIslandMode(
                    sbn,
                    settings.liveActivitiesEnabled,
                    settings.navigationEnabled,
                    settings.deviceType
                ) == IslandMode.Notification
            }
            .sortedBy { it.postTime }
            .forEach { handleNotificationPosted(it, settings, isInitialSync = true) }
    }

    private fun importSameAppNotifications(pkg: String) {
        val settings = currentSettings
        if (!settings.enabled || pkg == packageName) return
        val active = runCatchingLogged(TAG, "Failed to get active notifications for same-app import") {
            activeNotifications?.toList()
        }.orEmpty()
        val stored = notificationRepository.notifications.value.mapTo(HashSet()) { it.key }
        active
            .filter { it.packageName == pkg && it.key !in stored && !alreadyShown(it) }
            .filter { ((it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 ||
            com.agupta07505.smartisland.util.NotificationFilter.isCallNotification(it.notification)) }
            .filterNot { shouldSuppressFromIsland(it) }
            .filter { sbn ->
                val mode = sbn.notification.toIslandMode(
                    sbn,
                    settings.liveActivitiesEnabled,
                    settings.navigationEnabled,
                    settings.deviceType
                )
                (mode == IslandMode.Notification && !isSilentNotification(sbn)) ||
                    mode == IslandMode.DownloadUpload
            }
            .sortedBy { it.postTime }
            .forEach { handleNotificationPosted(it, settings, isInitialSync = true) }
    }

    internal fun shouldSuppressFromIsland(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = suppressionReasonFor(sbn, rankingMap, filterOverride) != null

    /** The rule that keeps [sbn] off the island, or null when it may be shown; the diagnostic log prints its name. */
    internal fun suppressionReasonFor(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): String? {
        if (!currentSettings.showDuringDnd && isHiddenByDoNotDisturb(sbn, rankingMap, filterOverride)) {
            android.util.Log.d(TAG, "Suppressed by Do Not Disturb: key=${sbn.key} pkg=${sbn.packageName}")
            return "do-not-disturb"
        }
        val pm = runCatching { packageManager }.getOrNull() ?: return null
        // An event switched off in every displayable profile must stay in the normal shade: neither shown in the
        // island nor cancelled.
        val disabled = currentSettings.disabledEvents
        if (disabled.isNotEmpty()) {
            val mode = sbn.notification.toIslandMode(
                sbn,
                currentSettings.liveActivitiesEnabled,
                currentSettings.navigationEnabled,
                currentSettings.deviceType
            )
            val event = listenerEventFor(sbn.notification, mode, sbn.packageName)
            if (event != null && currentSettings.isEventDisabledEverywhere(event.id)) {
                return "event-disabled-everywhere(${event.id})"
            }
        }
        return com.agupta07505.smartisland.util.NotificationFilter.suppressionReason(
            sbn,
            pm,
            currentSettings.liveActivitiesEnabled,
            currentSettings.navigationEnabled,
            currentSettings.disabledNotificationPackages,
            currentSettings.deviceType
        )
    }

    private fun listenerEventFor(notification: Notification, mode: IslandMode, packageName: String): com.agupta07505.smartisland.model.IslandEvent? =
        com.agupta07505.smartisland.model.IslandEvent.forListenerMode(
            mode,
            transferKind = if (mode == IslandMode.DownloadUpload) {
                notification.transferKind(packageName)
            } else {
                TransferKind.DOWNLOAD
            }
        )

    private fun shouldHideFromShade(notification: Notification, mode: IslandMode, packageName: String): Boolean {
        if (!shouldBeIslandOnly(notification, mode)) return false
        val event = listenerEventFor(notification, mode, packageName) ?: return true
        return !currentSettings.isEventDisabledSomewhere(event.id)
    }

    internal fun shouldBeIslandOnly(notification: Notification, mode: IslandMode): Boolean {
        if (mode == IslandMode.IncomingCall) {
            if (!isIncomingCall(notification)) return false
        }
        if (mode == IslandMode.Music || mode == IslandMode.Navigation || mode == IslandMode.Timer || mode == IslandMode.Stopwatch ||
            mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording
        ) {
            return false // Media, Navigation, Timer, Stopwatch and recorder notifications must NOT be cancelled from system shade (their buttons live there)
        }
        return true
    }

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

        pendingSuppressionJobs.remove(key)?.cancel()

        if (!isRecentlySuppressed) {
            runCatchingLogged(TAG, "sync cancel failed") { cancelNotification(key) }
        }

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

    private val latePictureJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private fun scheduleLatePicture(sbn: StatusBarNotification, notification: Notification, notifText: String) {
        val key = sbn.key
        latePictureJobs.remove(key)?.cancel()
        val job = serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runSuspendCatchingLogged(TAG, "Late picture job failed") {
                for (waitMs in com.agupta07505.smartisland.util.PictureRetryPlan.DELAYS_MS) {
                    delay(waitMs)
                    if (callPostGuard.isStale(key, sbn.postTime)) return@runSuspendCatchingLogged
                    if (notificationRepository.notifications.value.none { it.key == key }) return@runSuspendCatchingLogged
                    val found = runCatchingLogged(TAG, "Late picture read failed") {
                        mediaReader.read(key, notification, notifText, sbn.packageName, allowFallbacks = true)
                    }.orEmpty()
                    if (found.isNotEmpty()) {
                        val reader = runCatchingLogged(TAG, "Late picture reader text failed") {
                            notification.readerTextWithoutPictures(notifText)
                        }.orEmpty()
                        notificationRepository.updateNotification(key) { it.copy(media = found, readerText = reader) }
                        return@runSuspendCatchingLogged
                    }
                }
                com.agupta07505.smartisland.util.AppLogRecorder.w(TAG, "no picture for $key after the last retry")
            }
        }
        latePictureJobs[key] = job
        job.invokeOnCompletion { latePictureJobs.remove(key, job) }
    }

    private fun forceCancelNotification(key: String) {
        // This is an explicit user action, so the island copy must disappear as well.
        // A handler still working on an older post of this key (slow picture read) must not bring the card back.
        callPostGuard.noteRemoved(key, System.currentTimeMillis())
        latePictureJobs.remove(key)?.cancel()
        clearSuppressed(key)
        runCatchingLogged(TAG, "forceCancel failed") { cancelNotification(key) }
        notificationRepository.removeNotification(key)
    }

    private fun isRecordingNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val extras = notification.extras
        val text = "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)} " +
            "${extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)}"
        val device = com.agupta07505.smartisland.util.OemDeviceRules.resolveEffectiveDevice(currentSettings.deviceType)
        return com.agupta07505.smartisland.util.OemDeviceRules.isScreenRecording(sbn.packageName, text, device) ||
            com.agupta07505.smartisland.util.OemDeviceRules.isAudioRecording(sbn.packageName, text)
    }

    private fun removalDelayMs(key: String): Long {
        val card = notificationRepository.notifications.value.firstOrNull { it.key == key }
        return if (card?.mode == IslandMode.ScreenRecording || card?.mode == IslandMode.AudioRecording) 0L else 350L
    }

    private fun playNotificationSound(sbn: StatusBarNotification) {
        if (!AppEventRules.maySound(currentSettings.disabledSoundPackages, sbn.packageName)) return
        // Do Not Disturb always keeps the island quiet, even when it is set to show cards during DND.
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

            // Notification channel (API 26+).
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
                        return
                    }
                    soundUri = chSound
                    audioAttributes = channel.audioAttributes
                }
            }

            // Fall back to the notification payload when the channel has no sound.
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

            // Play the exact notification sound.
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

    private val mediaReader by lazy { com.agupta07505.smartisland.util.NotificationMediaReader(applicationContext) }

    private fun loadAppIconBitmap(packageName: String): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        return runCatchingLogged(TAG, "LoadAppIconBitmap failed") {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = ICON_BITMAP_SIZE, height = ICON_BITMAP_SIZE).also { iconCache.put(packageName, it) }
        }
    }

    private val smallIconCache = android.util.LruCache<String, Bitmap>(30)

    private fun Notification.loadSmallIconBitmap(packageName: String): Bitmap? {
        smallIconCache.get(packageName)?.let { return it }
        return runCatchingLogged(TAG, "LoadSmallIconBitmap failed") {
            getSmallIcon()?.loadDrawable(this@SmartIslandNotificationListenerService)
                ?.toBitmap(width = ICON_BITMAP_SIZE, height = ICON_BITMAP_SIZE)
                ?.also { smallIconCache.put(packageName, it) }
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

    private val actionIconCache = object : LinkedHashMap<String, Bitmap?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap?>?): Boolean = size > 48
    }

    private fun loadActionIcon(action: Notification.Action, packageName: String): Bitmap? {
        val icon = action.getIcon() ?: return null
        val key = "$packageName|$icon"
        synchronized(actionIconCache) {
            if (actionIconCache.containsKey(key)) return actionIconCache[key]
        }
        val bitmap = runCatchingLogged(TAG, "Action icon could not be loaded") {
            // A resource icon without a package name belongs to the posting app: loading it from this app's resources fails.
            val needsAppContext = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P &&
                icon.type == Icon.TYPE_RESOURCE && icon.resPackage.isNullOrEmpty()
            val context = if (needsAppContext) createPackageContext(packageName, 0) else this
            val drawable = icon.loadDrawable(context) ?: return@runCatchingLogged null
            val size = 96
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(android.graphics.Canvas(out))
            out
        }
        synchronized(actionIconCache) { actionIconCache[key] = bitmap }
        return bitmap
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
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        return MediaInfo(artwork, positionMs, durationMs, com.agupta07505.smartisland.util.isPlayingLike(playbackState?.state), title, artist)
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

    private data class MediaInfo(
        val artwork: Bitmap?,
        val positionMs: Long?,
        val durationMs: Long?,
        val isPlaying: Boolean,
        val title: String? = null,
        val artist: String? = null
    )

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
            mode == IslandMode.ScreenRecording ||
            mode == IslandMode.AudioRecording
    }

    companion object {
        private const val SHOWN_POSTS_MAX = 400
        private const val COUNT_REFRESH_DEBOUNCE_MS = 150L
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        private const val TAG = "SmartIslandNotificationListener"
        private const val VOIP_CARD_KEY = "voip_call_audio"
        private const val ICON_BITMAP_SIZE = 96
        private const val LARGE_ICON_BITMAP_SIZE = 128
        private const val MAX_SUPPRESSED_KEYS = 100
        private const val SUPPRESSED_KEY_TTL_MS = 10 * 60 * 1000L
        private const val INITIAL_SUPPRESSION_WINDOW_MS = 1500L
        private const val REPLY_GRACE_MS = 6000L
        private const val SOUND_DEBOUNCE_MS = 1200L
        private const val AUTO_EXPAND_DEBOUNCE_MS = 1500L

        private val INITIAL_RESYNC_DELAYS_MS = longArrayOf(1_500L, 3_500L)
    }
}
