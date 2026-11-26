/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.data.INotificationHistoryRepository
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CallActionRole
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.NotificationMedia
import com.agupta07505.smartisland.model.RepliedCards
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.model.TransferState
import com.agupta07505.smartisland.model.VoipCallDetector
import com.agupta07505.smartisland.service.NotificationCardReader.cardText
import com.agupta07505.smartisland.service.NotificationCardReader.cardTitle
import com.agupta07505.smartisland.service.NotificationCardReader.miuiClockValues
import com.agupta07505.smartisland.service.NotificationCardReader.miuiFocusActions
import com.agupta07505.smartisland.service.NotificationCardReader.readCustomViews
import com.agupta07505.smartisland.service.NotificationCardReader.resolveActions
import com.agupta07505.smartisland.service.NotificationCardReader.standardActionsOf
import com.agupta07505.smartisland.util.AppEventRules
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.CallCardRules
import com.agupta07505.smartisland.util.CallStyleExtras
import com.agupta07505.smartisland.util.ClockPackageRules
import com.agupta07505.smartisland.util.DemoEvents
import com.agupta07505.smartisland.util.DemoNotification
import com.agupta07505.smartisland.util.EnvironmentDiagnostics
import com.agupta07505.smartisland.util.MediaCardPatch
import com.agupta07505.smartisland.util.MediaSessionWatcher
import com.agupta07505.smartisland.util.MediaSnapshot
import com.agupta07505.smartisland.util.NotificationCooldownManager
import com.agupta07505.smartisland.util.NotificationCountTracker
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.NotificationMediaReader
import com.agupta07505.smartisland.util.NotificationTrace
import com.agupta07505.smartisland.util.NotificationViewInspector
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.RecordingClock
import com.agupta07505.smartisland.util.StalePostGuard
import com.agupta07505.smartisland.util.TimerStopwatchParser
import com.agupta07505.smartisland.util.TransferClassifier
import com.agupta07505.smartisland.util.TransferDetector
import com.agupta07505.smartisland.util.TransferExpiry
import com.agupta07505.smartisland.util.TransferInput
import com.agupta07505.smartisland.util.TransferVerdict
import com.agupta07505.smartisland.util.expandedText
import com.agupta07505.smartisland.util.isCallEnded
import com.agupta07505.smartisland.util.isOverlayAccessibilityServiceEnabled
import com.agupta07505.smartisland.util.isScreenRecordingComplete
import com.agupta07505.smartisland.util.readerTextWithoutPictures
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode
import com.agupta07505.smartisland.util.transferKind
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
    @Volatile private var currentSettings = SmartIslandSettings.Default
    private val admission by lazy { NotificationAdmission(this) { currentSettings } }
    private val shadeHost = object : ShadeSyncHost {
        override fun readActive(): Array<StatusBarNotification>? = activeNotifications
        override var settings: SmartIslandSettings
            get() = currentSettings
            set(value) { currentSettings = value }
        override fun overlayReady() = ensureOverlayServiceRunning()
        override fun accessibilityEnabled() = isOverlayAccessibilityServiceEnabled()
        // qualified: the plain name would resolve to this member and call itself
        override fun alreadyShown(sbn: StatusBarNotification) =
            this@SmartIslandNotificationListenerService.alreadyShown(sbn)
        override fun hideSystemCopy(key: String) = shadeSuppressor.suppress(key)
        override fun dropCard(key: String) = dropStaleCard(key)
        override fun post(sbn: StatusBarNotification, settings: SmartIslandSettings) {
            handleNotificationPosted(sbn, settings, isInitialSync = true)
        }
    }
    private val shadeSync by lazy {
        NotificationShadeSync(this, repository, notificationRepository, admission, shadeHost)
    }
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        AppLogRecorder.e(TAG, "unhandled listener coroutine failure", error)
    }
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)
    private val mainScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + coroutineExceptionHandler)
    private val shadeSuppressor = ShadeSuppressor(
        scope = mainScope,
        settings = { currentSettings },
        readActive = { activeNotifications },
        cancel = { cancelNotification(it) },
        clock = SystemClock::elapsedRealtime
    )

    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    @Inject lateinit var historyRepository: INotificationHistoryRepository
    private val historyRecorder by lazy { NotificationHistoryRecorder(serviceScope) { historyRepository } }
    private val cooldownReleaseJobs = ConcurrentHashMap<String, Job>()
    private val autoExpandSlot = AutoExpandSlot(
        SystemClock::elapsedRealtime, AUTO_EXPAND_DEBOUNCE_MS, AUTO_EXPAND_BURST_MS
    )

    private val viewButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            NotificationViewInspector.click(intent.data)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLogRecorder.restore(applicationContext)
        ContextCompat.registerReceiver(
            this,
            viewButtonReceiver,
            IntentFilter(NotificationViewInspector.ACTION_CLICK).apply {
                addDataScheme(NotificationViewInspector.SCHEME)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                var wasEnabled: Boolean? = null
                repository.listenerSettings.collect { settings ->
                    currentSettings = settings
                    if (settings.enabled && wasEnabled == false && isSystemConnected) {
                        serviceScope.launch {
                            runSuspendCatchingLogged(TAG, "Shade sync after enabling failed") {
                                shadeSync.initialSync(trigger = "island enabled", onlyMissing = true)
                            }
                        }
                    }
                    wasEnabled = settings.enabled
                    AppLogRecorder.applySetting(applicationContext, settings.recordLogs)
                    tracer.logRelevantSettings(settings)
                    if (!settings.enabled || !settings.hideFromNotificationShade) {
                        shadeSuppressor.forgetKeys()
                    } else {
                        shadeSuppressor.dropExpiredKeys()
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
        // The notification-count idle widget counts the shade and the island's own cards
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Count collector failed") {
                notificationRepository.notifications.collect { counts.schedule() }
            }
        }
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Command collector failed") {
                notificationRepository.commands.collect { command ->
                    runCatchingLogged(TAG, "Notification command failed") { handleCommand(command) }
                }
            }
        }
    }

    private fun handleCommand(command: SmartIslandCommand) {
        when (command) {
            is SmartIslandCommand.CancelNotification -> {
                forceCancelNotification(command.key)
            }
            is SmartIslandCommand.TimerStopped -> {
                clocks.forgetTimer(command.packageName)
            }
            is SmartIslandCommand.ReplySent -> {
                replyGraceUntil[command.key] = SystemClock.elapsedRealtime() + REPLY_GRACE_MS
                repliedCards.markReplied(command.key, System.currentTimeMillis())
            }
            is SmartIslandCommand.ImportShadeNotifications -> {
                shadeSync.importShadeNotifications()
            }
            is SmartIslandCommand.SyncShade -> {
                if (isSystemConnected) {
                    serviceScope.launch {
                        runSuspendCatchingLogged(TAG, "Shade sync failed") {
                            shadeSync.initialSync(trigger = "overlay started", onlyMissing = true)
                        }
                    }
                }
            }
            is SmartIslandCommand.ImportSameAppNotifications -> {
                shadeSync.importSameAppNotifications(command.packageName)
            }
            is SmartIslandCommand.SeekTo -> {
                media.bestControllerFor(command.packageName)
                    ?.transportControls
                    ?.seekTo(command.positionMs)
            }
            is SmartIslandCommand.SkipNext -> {
                mediaControllerFor(command.packageName)?.transportControls?.skipToNext()
            }
            is SmartIslandCommand.SkipPrevious -> {
                mediaControllerFor(command.packageName)?.transportControls?.skipToPrevious()
            }
            is SmartIslandCommand.PlayPause -> {
                val controller = mediaControllerFor(command.packageName)
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

    /** The named app's session, or any playing one when the command names no app. */
    private fun mediaControllerFor(packageName: String?): MediaController? =
        packageName?.let { media.bestControllerFor(it) } ?: media.anyPlayingController()

    private fun stopMediaWatcher() {
        mediaWatcher?.stop()
        mediaWatcher = null
    }

    private var mediaWatcher: MediaSessionWatcher? = null

    private fun startMediaWatcher() {
        if (mediaWatcher != null) return
        mediaWatcher = MediaSessionWatcher(
            context = this,
            listener = ComponentName(this, SmartIslandNotificationListenerService::class.java),
            onSnapshot = ::applyMediaSnapshot
        ).also { it.start() }
    }

    private fun applyMediaSnapshot(snapshot: MediaSnapshot) {
        if (!currentSettings.enabled) return
        notificationRepository.notifications.value
            .filter { MediaCardPatch.appliesTo(it, snapshot) }
            .forEach { card ->
                notificationRepository.updateNotification(card.key) { old ->
                    MediaCardPatch.apply(old, snapshot)
                }
            }
    }

    private val callPostGuard = StalePostGuard()

    private val callEnded = CallEndedCards(
        scope = serviceScope,
        repository = { notificationRepository }
    )

    private val voip = VoipCardCoordinator(
        context = this,
        scope = serviceScope,
        host = object : VoipCardHost {
            override val repository: INotificationRepository get() = notificationRepository
            override val ownPackageName: String get() = packageName
            override val settings: SmartIslandSettings get() = currentSettings
            override val isConnected: Boolean get() = isSystemConnected

            override fun activeNotifications(): List<StatusBarNotification>? =
                this@SmartIslandNotificationListenerService.activeNotifications?.toList()

            override fun endCard(key: String) {
                callEnded.remove(key)
            }

            override fun handlePost(sbn: StatusBarNotification, settings: SmartIslandSettings) {
                handleNotificationPosted(sbn, settings)
            }
        }
    )

    private val clocks = NotificationClockReader()
    private val tracer = NotificationTracer()
    private var recordingWatchdog: Job? = null
    private var transferExpiryJob: Job? = null
    private val replyGraceUntil = ConcurrentHashMap<String, Long>()
    private val repliedCards = RepliedCards(REPLY_GRACE_MS)
    private val replyReader by lazy {
        SuggestedReplyReader(
            settings = { currentSettings },
            repliedCards = repliedCards,
            repository = { notificationRepository },
            readActive = { activeNotifications },
            currentRanking = { currentRanking }
        )
    }

    private fun startTransferExpirySweep() {
        if (transferExpiryJob?.isActive == true) return
        transferExpiryJob = serviceScope.launch {
            while (true) {
                delay(30_000L)
                val minutes = currentSettings.transferExpiryMinutes
                val keys = TransferExpiry.keysToExpire(
                    notificationRepository.notifications.value, System.currentTimeMillis(), minutes
                )
                keys.forEach { key ->
                    latePictures.cancel(key)
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
                // Sample cards from the Demo tab have no notification behind them on purpose
                val cards = notificationRepository.notifications.value.filter {
                    (it.mode == IslandMode.ScreenRecording || it.mode == IslandMode.AudioRecording) &&
                        !DemoEvents.isDemoKey(it.key)
                }
                if (cards.isEmpty()) {
                    clocks.clearRecordings()
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
                        clocks.forgetRecording(card.key)
                        notificationRepository.removeNotification(card.key)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        AppLogRecorder.w("ListenerLifecycle", "listener destroyed (pid=${Process.myPid()})")
        try {
            unregisterReceiver(viewButtonReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was already unregistered
        }
        NotificationViewInspector.forgetAll()
        recordingWatchdog?.cancel()
        transferExpiryJob?.cancel()
        recordingWatchdog = null
        stopMediaWatcher()
        clocks.clear()
        voip.stop()
        isSystemConnected = false
        soundPlayer.reset()
        autoExpandSlot.reset()
        cooldownReleaseJobs.values.forEach { it.cancel() }
        cooldownReleaseJobs.clear()
        NotificationCooldownManager.clear()
        callPostGuard.clear()
        pendingRemovals.values.forEach { it.cancel() }
        pendingRemovals.clear()
        shadeSuppressor.clearAll()
        icons.clear()
        serviceScope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    private val pendingRemovals = ConcurrentHashMap<String, Job>()

    private val finishedCards = FinishedCardRemover(
        scope = serviceScope,
        repository = { notificationRepository },
        pendingRemovals = pendingRemovals,
        clearSuppressed = ::clearSuppressed
    )

    private val shownPostTimes = ConcurrentHashMap<String, Long>()

    private fun noteShown(sbn: StatusBarNotification) {
        if (shownPostTimes.size > SHOWN_POSTS_MAX) shownPostTimes.clear()
        shownPostTimes[sbn.key] = sbn.postTime
    }

    private fun alreadyShown(sbn: StatusBarNotification): Boolean = shownPostTimes[sbn.key] == sbn.postTime

    private val appLabels by lazy { AppLabelCache(packageManager) }
    private val counts by lazy {
        NotificationCountRefresher(
            scope = serviceScope,
            ownPackage = packageName,
            appLabels = appLabels,
            isConnected = { isSystemConnected },
            readShade = { activeNotifications?.toList() },
            islandNotifications = { notificationRepository.notifications.value }
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        onNotificationPosted(sbn, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        runCatchingLogged(TAG, "onNotificationPosted callback failed") {
        if (sbn.packageName == packageName && !DemoNotification.isDemo(sbn)) return@runCatchingLogged
        counts.schedule()

        val notification = sbn.notification
        callPostGuard.notePosted(sbn.key, sbn.postTime)
        tracer.tracePost(
            this, currentSettings, sbn, rankingMap,
            suppressionReason = { suppressionReasonFor(sbn, rankingMap) },
            eventFor = ::listenerEventFor
        )

        // Group summaries never reach the island, so the cancel below skips them. Cancel them here.
        val isGroupSummary = NotificationModeRules.isGroupSummary(
            notification.flags, NotificationFilter.isCallNotification(notification)
        )
        if (isGroupSummary) {
            if (currentSettings.enabled &&
                currentSettings.hideFromNotificationShade &&
                sbn.packageName !in currentSettings.disabledNotificationPackages &&
                NotificationFilter.isThirdPartyApp(
                    sbn.packageName,
                    packageManager
                )
            ) {
                shadeSuppressor.suppress(sbn.key)
            }
            tracer.traceStage(sbn, "DROPPED", "stage=group-summary shadeCancel=${currentSettings.hideFromNotificationShade}")
            pendingRemovals.remove(sbn.key)?.cancel()
            return@runCatchingLogged
        }

        // Suppress from the shade synchronously, before the coroutine is scheduled
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
                    tracer.traceStage(sbn, "SHADE-CANCEL", "mode=$modeQuick (island-only, hidden from the system shade)")
                    runCatchingLogged(TAG, "Immediate cancel failed") { cancelNotification(sbn.key) }
                }
            }
        } catch (e: Exception) {
            AppLogRecorder.w(TAG, "immediate suppress failed", e)
        }

        pendingRemovals.remove(sbn.key)?.cancel()

        endSupersededRecordingCard(sbn)

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "NotificationPosted async failed") {
                val asyncReason = suppressionReasonFor(sbn, rankingMap)
                if (asyncReason != null) {
                    tracer.traceStage(sbn, "DROPPED", "stage=async-filter reason=$asyncReason")
                    return@runSuspendCatchingLogged
                }

                val settings = repository.listenerSettings.first()
                currentSettings = settings
                if (!settings.enabled) {
                    tracer.traceStage(sbn, "DROPPED", "stage=async-settings reason=island-disabled")
                    return@runSuspendCatchingLogged
                }

                handleNotificationPosted(sbn, settings)
            }
        }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
        runCatchingLogged(TAG, "onNotificationRemoved callback failed") {
        counts.schedule()
        tracer.traceStage(sbn, "SYSTEM-REMOVED", "reason=$reason(${NotificationModeRules.removalReasonName(reason)}) onIsland=${notificationRepository.notifications.value.any { it.key == sbn.key }}")

        scheduleRemoval(sbn, reason, "NotificationRemoved handling failed") {
            tracer.traceStage(sbn, "ISLAND-REMOVED", "reason=$reason(${NotificationModeRules.removalReasonName(reason)})")
            NotificationTrace.forget(sbn.key)
            latePictures.cancel(sbn.key)
        }
        }
    }

    /**
     * Takes the card for a removed notification off the island after the mode's delay, unless the removal was
     * the app's own reply or shade cancel. [forgetExtras] runs first, only for a removal that goes through.
     * A removal without a [reason] cannot be told from our own shade cancel.
     */
    private fun scheduleRemoval(
        sbn: StatusBarNotification,
        reason: Int?,
        failureLabel: String,
        forgetExtras: () -> Unit = {}
    ) {
        pendingRemovals.remove(sbn.key)?.cancel()
        val endedAtMs = System.currentTimeMillis()
        if (reason != null && reason != REASON_LISTENER_CANCEL) callPostGuard.noteRemoved(sbn.key, sbn.postTime)

        val job = serviceScope.launch {
            runSuspendCatchingLogged(TAG, failureLabel) {
                delay(removalDelayMs(sbn.key))
                if (sbn.packageName == packageName && !DemoNotification.isDemo(sbn)) return@runSuspendCatchingLogged

                val now = SystemClock.elapsedRealtime()
                if (NotificationRemovalRules.isInReplyGrace(now, replyGraceUntil[sbn.key])) {
                    return@runSuspendCatchingLogged
                }
                replyGraceUntil.remove(sbn.key)
                repliedCards.forget(sbn.key)
                val lastSuppressedTime = shadeSuppressor.lastMarked(sbn.key)
                val isRecentInitialSuppression = NotificationRemovalRules.isRecentlyMarked(
                    now, lastSuppressedTime, NotificationRemovalRules.INITIAL_SUPPRESSION_WINDOW_MS
                )

                if (NotificationRemovalRules.isOwnRecentCancel(reason, isRecentInitialSuppression)) {
                    // Suppressed from the shade less than 1.5 s ago
                    return@runSuspendCatchingLogged
                }

                forgetExtras()
                clearSuppressed(sbn.key)
                clocks.forgetRecording(sbn.key)
                NotificationViewInspector.forget(sbn.key)
                callEnded.remove(sbn.key, endedAtMs)
            }
            pendingRemovals.remove(sbn.key)
        }
        pendingRemovals[sbn.key] = job
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
        tracer.traceStage(sbn, "ISLAND-REMOVED", "reason=recording-ended-by-repost heldMode=${held.mode} newMode=$modeNow")
        pendingRemovals.remove(sbn.key)?.cancel()
        clearSuppressed(sbn.key)
        clocks.forgetRecording(sbn.key)
        notificationRepository.removeNotification(sbn.key)
    }

    // Keep the no-arg override as a fallback (some OEMs may only call this one).
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        runCatchingLogged(TAG, "onNotificationRemoved fallback callback failed") {
        counts.schedule()
        scheduleRemoval(sbn, reason = null, "NotificationRemoved (no reason) handling failed")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isSystemConnected = true
        counts.schedule()
        AppLogRecorder.i(
            "ListenerLifecycle",
            "onListenerConnected pid=${Process.myPid()} " +
                EnvironmentDiagnostics.snapshot(this)
        )
        voip.start()
        startRecordingWatchdog()
        startTransferExpirySweep()
        startMediaWatcher()
        try {
            serviceScope.launch {
                runSuspendCatchingLogged(TAG, "ListenerConnected failed") {
                    shadeSync.initialSync(trigger = "listener connected", onlyMissing = false)
                }
            }
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "onListenerConnected callback failed", e)
        }
    }

    override fun onListenerDisconnected() {
        isSystemConnected = false
        NotificationCountTracker.clear()
        stopMediaWatcher()
        AppLogRecorder.w(
            "ListenerLifecycle",
            "onListenerDisconnected: the system unbound the listener, requesting a rebind. While it is disconnected no notification or call reaches the island. " +
                EnvironmentDiagnostics.snapshot(this)
        )
        super.onListenerDisconnected()
        try {
            requestRebind(
                ComponentName(
                    this,
                    SmartIslandNotificationListenerService::class.java
                )
            )
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "Notification-listener self-rebind failed", e)
        }
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        super.onInterruptionFilterChanged(interruptionFilter)
        try {
            val isDndActive = interruptionFilter != INTERRUPTION_FILTER_ALL &&
                interruptionFilter != INTERRUPTION_FILTER_UNKNOWN
            if (isDndActive) {
                purgeDndBlockedNotifications()
            }
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "onInterruptionFilterChanged callback failed", e)
        }
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        super.onNotificationRankingUpdate(rankingMap)
        try {
            val filter = runCatchingLogged(TAG, "Failed to get filter in ranking update") {
                currentInterruptionFilter
            }?.takeIf { it != INTERRUPTION_FILTER_UNKNOWN }
                ?: runCatchingLogged(TAG, "Failed to get filter from NotificationManager") {
                    (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.currentInterruptionFilter
                } ?: INTERRUPTION_FILTER_ALL

            val isDndActive = filter != INTERRUPTION_FILTER_ALL && filter != INTERRUPTION_FILTER_UNKNOWN
            if (isDndActive) {
                purgeDndBlockedNotifications(rankingMap)
            }
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "onNotificationRankingUpdate callback failed", e)
        }
        replyReader.refresh(rankingMap)
    }

    private fun purgeDndBlockedNotifications(rankingMap: RankingMap? = null) {
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
                        notificationRepository.removeNotification(islandNotif.key)
                    }
                } else if (islandNotif.mode == IslandMode.Notification) {
                    notificationRepository.removeNotification(islandNotif.key)
                }
            }
        }
    }

    /** A card whose notification is gone from the shade: stops what was still waiting on its key. */
    private fun dropStaleCard(key: String) {
        pendingRemovals.remove(key)?.cancel()
        latePictures.cancel(key)
        clearSuppressed(key)
        clocks.forgetRecording(key)
    }

    private fun ensureOverlayServiceRunning(): Boolean {
        return isOverlayAccessibilityServiceEnabled() || Settings.canDrawOverlays(this)
    }

    internal fun handleNotificationPosted(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings,
        isInitialSync: Boolean = false
    ) {
        if (sbn.packageName == packageName && !DemoNotification.isDemo(sbn)) return
        val notification = sbn.notification
        val handleReason = suppressionReasonFor(sbn)
        if (handleReason != null) {
            tracer.traceStage(sbn, "DROPPED", "stage=handle-filter reason=$handleReason initialSync=$isInitialSync")
            return
        }

        val extras = notification.extras
        val mode = notification.toIslandMode(
            sbn,
            settings.liveActivitiesEnabled,
            settings.navigationEnabled,
            settings.deviceType
        )
        if (isThrottledByCooldown(sbn, settings, mode, isInitialSync)) return

        val shouldIslandOnly = shouldBeIslandOnly(notification, mode)
        val removedFromShade = hideFromShade(sbn, notification, settings, mode, shouldIslandOnly)

        val mediaInfo = if (mode == IslandMode.Music) media.findInfo(notification, sbn.packageName) else null
        val appName = postingAppName(sbn)

        val isNewNotif = notificationRepository.notifications.value.none { it.key == sbn.key }

        val existingNotif = notificationRepository.notifications.value.find { it.key == sbn.key
            || (it.mode == IslandMode.IncomingCall && it.packageName == sbn.packageName) }
        val isMiuiRom = OemDeviceRules.isMiuiOrHyperOs(settings.deviceType)
        val baseActions = (standardActionsOf(icons, sbn, notification,
            mode) + if (isMiuiRom) miuiFocusActions(extras) else emptyList())
            .distinctBy { it.title.trim().lowercase() to it.pendingIntent }

        // Some clocks and recorders keep time and buttons in a custom view
        val isShadeFixCard = (mode == IslandMode.Timer || mode == IslandMode.Stopwatch) &&
            ClockPackageRules.usesShadeFixes(sbn.packageName)
        val viewInfo = readCustomViews(this, sbn, notification, mode, isShadeFixCard)
        val actions = resolveActions(sbn, mode, baseActions, viewInfo, isShadeFixCard)
        val isNowRinging = actions.any { VoipCallDetector.classifyCallAction(it.title) == CallActionRole.Answer } ||
            (mode == IslandMode.IncomingCall && CallStyleExtras.looksRinging(notification))
        // While the outgoing call is dialing, i.e. before the other side picks up, there is no timer
        val connectingText = if (mode == IslandMode.IncomingCall && !isNowRinging) {
            CallStyleExtras.connectingStatus(notification)
        } else {
            null
        }
        val isNowConnecting = connectingText != null

        val recordingState = clocks.recordingStateFor(sbn, notification, extras, mode, viewInfo, actions,
            isShadeFixCard)

        val timer = readTimerPost(sbn, notification, mode, actions, viewInfo, existingNotif, isShadeFixCard)
        val miuiClock = if (isMiuiRom) miuiClockValues(notification, mode, existingNotif) else MiuiClockValues()

        val computedTimeMillis = clocks.cardTimeMillis(
            sbn, notification, extras, mode, existingNotif, recordingState, isNowRinging, isNowConnecting,
            timer.frozenMs, timer.viewRunningMs, timer.viewShownMs, timer.paused, isShadeFixCard, isMiuiRom
        )

        val notifTitle = cardTitle(mediaInfo, extras, notification, mode)
        val notifText = cardText(mediaInfo, extras, notification, mode)

        val transferVerdict = if (mode == IslandMode.DownloadUpload) {
            TransferDetector.evaluate(
                TransferInput.fromNotification(notification, sbn.packageName, false)
            )
        } else {
            null
        }

        tracer.traceStage(
            sbn, "ISLAND-POST",
            "mode=$mode islandOnly=$shouldIslandOnly removedFromShade=$removedFromShade new=$isNewNotif" +
                " ringing=$isNowRinging initialSync=$isInitialSync" +
                " autoExpandSetting=${settings.autoExpandOnNotification}"
        )
        val pictures = readPictures(sbn, notification, mode, notifText)
        if (isStalePost(sbn, mode)) return
        if (mode == IslandMode.IncomingCall) {
            replaceSupersededCallCards(sbn, actions, isNowRinging, isNowConnecting, connectingText)
        }
        // the system delivering the very same post again
        val repostOfShownPost = !isInitialSync && alreadyShown(sbn)
        if (repostOfShownPost) {
            tracer.traceStage(sbn, "NO-AUTO-EXPAND", "stage=repost-of-shown-post mode=$mode postTime=${sbn.postTime} (already shown once, no second auto-expand)")
        }
        noteShown(sbn)
        // the timer ran out (a timer whose shade view still counts down has not). Just finished = the card
        // shown for this key was still running, so the post is a re-announcement and not a repost
        val timerFinished = mode == IslandMode.Timer && PostedNotificationRules.isTimerDone(
            TimerStopwatchParser.isTimerFinished(notification), timer.viewRunningMs
        )
        val timerJustFinished = timerFinished &&
            existingNotif?.takeIf { it.key == sbn.key }?.timerFinished != true
        val content = PostedContent(
            sbn, notification, mode, appName, notifTitle, notifText, computedTimeMillis, actions, pictures
        )
        val card = buildIslandNotification(
            content = content,
            mediaInfo = mediaInfo,
            transferVerdict = transferVerdict,
            recordingState = recordingState,
            timer = timer,
            miuiClock = miuiClock,
            connectingText = connectingText,
            isShadeFixCard = isShadeFixCard,
            timerFinished = timerFinished
        )
        val cardToPost = if (mode == IslandMode.Notification) {
            card.copy(
                suggestedReplies = replyReader.repliesFor(notification, sbn.key, sbn.postTime),
                systemCopyAction = replyReader.copyActionFor(sbn.key)
            )
        } else {
            card
        }
        notificationRepository.postNotification(
            cardToPost,
            autoExpand = shouldAutoExpand(
                sbn, settings, mode, isMiuiRom, isInitialSync, repostOfShownPost, shouldIslandOnly,
                timerJustFinished
            )
        )

        afterPost(
            content, settings, isInitialSync, removedFromShade, isNewNotif, shouldIslandOnly, timer.viewRunningMs
        )
    }

    private class PostedContent(
        val sbn: StatusBarNotification,
        val notification: Notification,
        val mode: IslandMode,
        val appName: String,
        val title: String,
        val text: String,
        val timeMillis: Long,
        val actions: List<IslandNotificationAction>,
        val pictures: List<NotificationMedia>
    )

    private class TimerPost(
        val paused: Boolean,
        val viewRunningMs: Long?,
        val viewShownMs: Long?,
        val frozenMs: Long?
    )

    private fun postingAppName(sbn: StatusBarNotification): String {
        // Try the app info attached to the notification before showing the package name.
        val packageAppName = runCatchingLogged(TAG, "GetApplicationInfo failed") {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } ?: PostedAppInfo.of(sbn.notification)?.let {
            runCatchingLogged(TAG, "Label from notification app info failed") {
                packageManager.getApplicationLabel(it).toString()
            }
        } ?: sbn.packageName
        // The posting app's own name unless it posts for a sub-app, as the system's notification header does
        return PostedAppInfo.subAppName(sbn.notification, packageAppName) ?: packageAppName
    }

    private fun readTimerPost(
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode,
        actions: List<IslandNotificationAction>,
        viewInfo: NotificationViewInspector.Result?,
        existingNotif: IslandNotification?,
        isShadeFixCard: Boolean
    ): TimerPost {
        // a paused timer's text often shows its full length
        val paused = clocks.isTimerPausedPost(sbn, notification, mode, actions, isShadeFixCard)
        val viewRunningMs: Long? = if (mode == IslandMode.Timer && !paused) {
            viewInfo?.countdownRemainingMs
        } else {
            null
        }
        val viewShownMs: Long? = if (mode == IslandMode.Timer) {
            viewInfo?.shownTimeSeconds?.let { it * 1000L }
        } else {
            null
        }
        val frozenMs = clocks.frozenTimerMs(
            sbn, notification, mode, existingNotif, viewInfo, isShadeFixCard, paused, viewShownMs
        )
        return TimerPost(paused, viewRunningMs, viewShownMs, frozenMs)
    }

    private fun afterPost(
        content: PostedContent,
        settings: SmartIslandSettings,
        isInitialSync: Boolean,
        removedFromShade: Boolean,
        isNewNotif: Boolean,
        shouldIslandOnly: Boolean,
        timerViewRunningMs: Long?
    ) {
        val sbn = content.sbn
        val notification = content.notification
        val mode = content.mode
        val extras = notification.extras
        if (mode == IslandMode.Notification && content.pictures.isEmpty()
            && mediaReader.expectsPicture(notification, content.text)) {
            latePictures.schedule(sbn, notification, content.text)
        }

        if (settings.enableNotificationHistory && mode != IslandMode.Music) {
            historyRecorder.save(
                sbn, notification, settings, mode, content.appName, content.title, content.text, content.timeMillis,
                content.actions
            )
        }

        // The island only makes a sound when it replaces the system notification
        val isRecordingEvent = mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording ||
            isRecordingNotification(sbn, notification)
        if (PostedNotificationRules.shouldPlayIslandSound(
                isInitialSync, removedFromShade, isNewNotif, mode, shouldIslandOnly, isRecordingEvent
            )
        ) {
            soundPlayer.play(sbn)
        }

        when (mode) {
            IslandMode.Music -> replacePreviousMusicCard(sbn)
            IslandMode.IncomingCall -> finishIncomingCallPost(sbn, notification)
            IslandMode.DownloadUpload -> finishedCards.scheduleFinishedTransferRemoval(sbn, notification, extras,
                isInitialSync)
            IslandMode.ScreenRecording, IslandMode.AudioRecording ->
                finishedCards.scheduleFinishedRecordingRemoval(sbn, notification)
            IslandMode.Timer -> finishedCards.scheduleFinishedTimerRemoval(sbn, notification, timerViewRunningMs)
            else -> Unit
        }
    }

    private fun isThrottledByCooldown(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings,
        mode: IslandMode,
        isInitialSync: Boolean
    ): Boolean {
        val extras = sbn.notification.extras
        // Quiet imports of what is already in the shade are not new alerts
        val cooldownText = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)}".lowercase()
        val transferRelated = TransferClassifier.isCooldownExemptTransfer(sbn.packageName, cooldownText)
        if (!isInitialSync && NotificationCooldownManager.shouldThrottle(sbn, settings, mode,
            transferRelated = transferRelated)) {
            scheduleCooldownRelease(sbn.packageName, settings.notificationCooldownDurationMinutes)
            tracer.traceStage(
                sbn, "DROPPED",
                "stage=cooldown-throttle reason=anti-spam threshold=${settings.notificationCooldownThreshold}" +
                    " durationMin=${settings.notificationCooldownDurationMinutes} (buffered, released after the cooldown)"
            )
            return true
        }
        return false
    }

    private fun hideFromShade(
        sbn: StatusBarNotification,
        notification: Notification,
        settings: SmartIslandSettings,
        mode: IslandMode,
        shouldIslandOnly: Boolean
    ): Boolean {
        val removedFromShade = settings.hideFromNotificationShade &&
            shouldIslandOnly &&
            shouldHideFromShade(notification, mode, sbn.packageName)
        if (removedFromShade) {
            shadeSuppressor.suppress(sbn.key)
        }
        return removedFromShade
    }

    private fun readPictures(
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode,
        notifText: String
    ): List<NotificationMedia> {
        return if (mode == IslandMode.Notification) {
            runCatchingLogged(TAG, "Notification picture could not be read") {
                // Fast read only: the card must not wait for Shizuku's shell
                mediaReader.read(sbn.key, notification, notifText, sbn.packageName, allowFallbacks = false)
            }.orEmpty()
        } else {
            emptyList()
        }
    }

    private fun isStalePost(sbn: StatusBarNotification, mode: IslandMode): Boolean {
        if (callPostGuard.isStale(sbn.key, sbn.postTime)) {
            // a newer post for this key, its removal
            tracer.traceStage(sbn, "DROPPED", "stage=stale-post mode=$mode postTime=${sbn.postTime} (newer post, removal or dismissal already seen)")
            return true
        }
        return false
    }

    private fun replaceSupersededCallCards(
        sbn: StatusBarNotification,
        actions: List<IslandNotificationAction>,
        isNowRinging: Boolean,
        isNowConnecting: Boolean,
        connectingText: String?
    ) {
        val replaced = CallCardRules.supersededKeys(
            notificationRepository.notifications.value, sbn.key, sbn.packageName, isNowRinging
        )
        replaced.forEach { oldKey ->
            pendingRemovals.remove(oldKey)?.cancel()
            clearSuppressed(oldKey)
            voip.forgetPromoted(oldKey)
            notificationRepository.removeNotification(oldKey)
        }
        AppLogRecorder.i(
            "CallCard",
            "pkg=${sbn.packageName} key=${sbn.key} ringing=$isNowRinging connecting=$isNowConnecting" +
                " buttons=${actions.map { it.title }} status='${connectingText.orEmpty()}'" +
                " replacedOldCards=$replaced"
        )
    }

    private fun buildIslandNotification(
        content: PostedContent,
        mediaInfo: MediaInfo?,
        transferVerdict: TransferVerdict?,
        recordingState: RecordingClock.State?,
        timer: TimerPost,
        miuiClock: MiuiClockValues,
        connectingText: String?,
        isShadeFixCard: Boolean,
        timerFinished: Boolean = false
    ): IslandNotification {
        val sbn = content.sbn
        val notification = content.notification
        val mode = content.mode
        val isStopwatch = mode == IslandMode.Stopwatch
        val barProgress = notification.extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val barProgressMax = notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val textPercent = if (barProgressMax <= 0) transferVerdict?.percent else null
        return IslandNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = content.appName,
            title = content.title,
            text = content.text,
            timeMillis = content.timeMillis,
            lastPostedMs = System.currentTimeMillis(),
            icon = PostedAppInfo.subAppName(notification, appLabels.labelOf(sbn.packageName))
                ?.let { icons.subApp(notification, it, sbn.packageName) }
                ?: icons.app(sbn.packageName, PostedAppInfo.of(notification)),
            smallIcon = if (mode == IslandMode.Music) icons.small(notification, sbn.packageName) else null,
            largeIcon = mediaInfo?.artwork ?: icons.large(notification),
            actionIntents = content.actions,
            category = notification.category,
            progress = textPercent ?: barProgress,
            progressMax = if (textPercent != null) 100 else barProgressMax,
            mediaPositionMs = mediaInfo?.positionMs,
            mediaDurationMs = mediaInfo?.durationMs,
            mediaIsPlaying = mediaInfo?.isPlaying == true,
            mediaToken = runCatchingLogged(TAG, "GetMediaToken failed") {
                val ex = notification.extras
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ex.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    ex.getParcelable(Notification.EXTRA_MEDIA_SESSION)
                }
            },
            mode = mode,
            contentIntent = notification.contentIntent,
            stopwatchElapsedSeconds = miuiClock.stopwatchElapsedSeconds,
            stopwatchChronometerBase = miuiClock.stopwatchChronometerBase,
            timerRemainingSeconds = miuiClock.timerRemainingSeconds,
            timerTotalMs = if (mode == IslandMode.Timer) TimerStopwatchParser.parseTimerTotalMs(notification) else null,
            exactClock = isShadeFixCard && mode == IslandMode.Timer,
            timerFinished = timerFinished,
            transferKind = if (mode == IslandMode.DownloadUpload) {
                notification.transferKind(sbn.packageName)
            } else {
                TransferKind.DOWNLOAD
            },
            transferState = transferVerdict?.state ?: TransferState.ACTIVE,
            transferDetail = transferVerdict?.detail.orEmpty(),
            recordingFrozenMs = recordingState?.frozenMs ?: timer.frozenMs,
            recordingPaused = (recordingState?.frozenMs != null &&
                (isStopwatch || !notification.isScreenRecordingComplete())) ||
                (mode == IslandMode.Timer && (timer.paused ||
                    TimerStopwatchParser.isTimerPaused(notification))),
            isSilent = mode == IslandMode.Notification && isSilentNotification(sbn),
            callStatusText = connectingText.orEmpty(),
            media = content.pictures,
            readerText = if (content.pictures.isNotEmpty()) {
                runCatchingLogged(TAG, "Picture reader text could not be read") {
                    notification.readerTextWithoutPictures(content.text)
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
        )
    }

    private fun shouldAutoExpand(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings,
        mode: IslandMode,
        isMiuiRom: Boolean,
        isInitialSync: Boolean,
        repostOfShownPost: Boolean,
        shouldIslandOnly: Boolean,
        timerJustFinished: Boolean = false
    ): Boolean = AutoExpandDecision.shouldAutoExpand(
        isMiuiRom = isMiuiRom,
        isInitialSync = isInitialSync,
        mode = mode,
        repostOfShownPost = repostOfShownPost,
        timerJustFinished = timerJustFinished,
        shouldIslandOnly = shouldIslandOnly,
        autoExpandOnNotification = settings.autoExpandOnNotification,
        mayAutoExpand = {
            AppEventRules.mayAutoExpand(
                globalAutoExpand = settings.autoExpandOnNotification,
                noAutoExpandPackages = settings.noAutoExpandPackages,
                autoExpandPackages = settings.autoExpandPackages,
                noExpandPackages = settings.noExpandPackages,
                packageName = sbn.packageName,
                mode = mode,
                isLoud = !(mode == IslandMode.Notification && isSilentNotification(sbn))
            )
        },
        claimSlot = { autoExpandSlot.claim(sbn.key) }
    )

    private fun replacePreviousMusicCard(sbn: StatusBarNotification) {
        notificationRepository.notifications.value
            .filter {
                PostedNotificationRules.isReplacedMusicCard(it.mode, it.packageName, it.key, sbn.packageName, sbn.key)
            }
            .forEach { notificationRepository.removeNotification(it.key) }
    }

    private fun finishIncomingCallPost(sbn: StatusBarNotification, notification: Notification) {
        voip.onVoipCallPost(sbn.key)
        if (notification.isCallEnded()) {
            pendingRemovals.remove(sbn.key)?.cancel()
            clearSuppressed(sbn.key)
            callEnded.remove(sbn.key)
        }
    }

    internal fun isHiddenByDoNotDisturb(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = admission.isHiddenByDoNotDisturb(sbn, rankingMap, filterOverride)

    internal fun isBlockedByDoNotDisturb(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = admission.isBlockedByDoNotDisturb(sbn, rankingMap, filterOverride)

    internal fun isSilentNotification(sbn: StatusBarNotification, rankingMap: RankingMap? = null): Boolean =
        admission.isSilent(sbn, rankingMap)

    internal fun shouldSuppressFromIsland(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = admission.shouldSuppress(sbn, rankingMap, filterOverride)

    /** Rule that keeps [sbn] off the island, or null when it may be shown. */
    internal fun suppressionReasonFor(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): String? = admission.suppressionReason(sbn, rankingMap, filterOverride)

    private fun listenerEventFor(notification: Notification, mode: IslandMode, packageName: String): IslandEvent? =
        admission.listenerEventFor(notification, mode, packageName)

    private fun shouldHideFromShade(notification: Notification, mode: IslandMode, packageName: String): Boolean =
        admission.shouldHideFromShade(notification, mode, packageName)

    internal fun shouldBeIslandOnly(notification: Notification, mode: IslandMode): Boolean =
        admission.shouldBeIslandOnly(notification, mode)

    private val latePictures by lazy {
        LatePictureReader(serviceScope, callPostGuard, { notificationRepository }, { mediaReader })
    }

    private fun forceCancelNotification(key: String) {
        // This is an explicit user action, so the island copy must disappear as well
        callPostGuard.noteRemoved(key, System.currentTimeMillis())
        latePictures.cancel(key)
        clearSuppressed(key)
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "forceCancel failed", e)
        }
        notificationRepository.removeNotification(key)
    }

    private fun isRecordingNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val extras = notification.extras
        val text = "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)} " +
            "${extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)}"
        val device = OemDeviceRules.resolveEffectiveDevice(currentSettings.deviceType)
        return OemDeviceRules.isScreenRecording(sbn.packageName, text, device) ||
            OemDeviceRules.isAudioRecording(sbn.packageName, text)
    }

    private fun removalDelayMs(key: String): Long {
        val card = notificationRepository.notifications.value.firstOrNull { it.key == key }
        return NotificationModeRules.removalDelayMs(card?.mode)
    }

    private fun markSuppressed(key: String) = shadeSuppressor.mark(key)

    private fun clearSuppressed(key: String) = shadeSuppressor.clear(key)

    private val icons by lazy { NotificationIconLoader(applicationContext) }

    private val media by lazy {
        NotificationMediaLookup(applicationContext, ComponentName(this,
            SmartIslandNotificationListenerService::class.java))
    }

    private val soundPlayer by lazy {
        NotificationSoundPlayer(applicationContext, { currentSettings }, { currentRanking },
            { isBlockedByDoNotDisturb(it) })
    }

    private val mediaReader by lazy { NotificationMediaReader(applicationContext) }

    private fun scheduleCooldownRelease(packageName: String, durationMinutes: Int) {
        // throttled posts arrive on several threads: one release job per app
        cooldownReleaseJobs.compute(packageName) { _, running ->
            if (running?.isActive == true) running else serviceScope.launch {
                delay(durationMinutes * 60_000L)
                val buffered = NotificationCooldownManager.pollBufferedNotification(packageName)
                if (buffered != null) {
                    val current = repository.listenerSettings.first()
                    if (current.enabled) {
                        handleNotificationPosted(buffered, current)
                    }
                }
                cooldownReleaseJobs.remove(packageName)
            }
        }
    }

    companion object {
        private const val SHOWN_POSTS_MAX = 400
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        private const val TAG = "SmartIslandNotificationListener"
        private const val REPLY_GRACE_MS = 6000L
        private const val AUTO_EXPAND_DEBOUNCE_MS = 1500L
        private const val AUTO_EXPAND_BURST_MS = 250L
    }
}
