/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.buddy.runtime.MascotEventMapper
import com.agupta07505.smartisland.buddy.runtime.NoteSnapshot
import com.agupta07505.smartisland.buddy.MascotRuntime
import com.agupta07505.smartisland.buddy.runtime.MascotEvents
import com.agupta07505.smartisland.buddy.runtime.MascotMoment
import com.agupta07505.smartisland.ui.PillLayout
import com.agupta07505.smartisland.ui.suppressedFade
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.PermissionRecoveryNotifier
import com.agupta07505.smartisland.util.AlertWidthBudget
import com.agupta07505.smartisland.util.CutoutBox
import com.agupta07505.smartisland.util.CutoutInsets
import com.agupta07505.smartisland.util.DisplayBounds
import com.agupta07505.smartisland.util.GatedAction
import com.agupta07505.smartisland.util.KeyguardGate
import com.agupta07505.smartisland.util.LandscapeAnchoring
import com.agupta07505.smartisland.util.PermissionGate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.ui.OverlayIsland
import com.agupta07505.smartisland.ui.expanded.sendIntentWithOptions
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartIslandOverlayService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    private var islandView: ComposeView? = null
    private val overlayOwners = OverlayViewTreeOwners()
    private lateinit var systemEventReceiver: SystemEventReceiver
    private lateinit var viewModel: IslandViewModel
    private var isLockScreenActive: Boolean = false
    private var systemEventReceiverRegistered = false
    private var screenStateReceiverRegistered = false
    private var torchCallbackRegistered = false
    private var foregroundStarted = false
    private val isTouchableRegionSupported = MutableStateFlow(false)
    @Volatile private var destroyed = false
    private var isWindowExpanded: Boolean = false
    private var collapseJob: kotlinx.coroutines.Job? = null
    private var lastParams: WindowManager.LayoutParams? = null
    private var lastBlurRadiusPx = 0
    // Screen-capture FLAG_SECURE, immersive suppression, face-unlock clearance, secure-app guess (Rule_Book 8.2, 10.1).
    private var guards: OverlayGuardController? = null
    // Camera / microphone privacy indicator (Rule_Book 4.15, 4.16).
    private var privacyMonitor: PrivacyIndicatorMonitor? = null
    // Landscape: the collapsed pill docks on the lateral cutout instead of the top centre (Rule_Book 9.2).
    private val landscapeAnchored = MutableStateFlow(false)
    @Volatile private var permissionLost = false

    private val torchCallback = object : android.hardware.camera2.CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (destroyed || !::viewModel.isInitialized) return
            MascotEvents.post(if (enabled) MascotMoment.TorchOn else MascotMoment.TorchOff)
            if (enabled) {
                notificationRepository.postNotification(
                    IslandNotification(
                        key = "system_flashlight",
                        packageName = "com.android.systemui",
                        appName = "Flashlight",
                        title = "Flashlight ON",
                        text = "Tap to turn off",
                        mode = com.agupta07505.smartisland.model.IslandMode.Flashlight,
                        timeMillis = System.currentTimeMillis(),
                        actionIntents = listOf(
                            com.agupta07505.smartisland.model.IslandNotificationAction("Turn Off", null)
                        )
                    ),
                    autoExpand = true
                )
            } else {
                notificationRepository.removeNotification("system_flashlight")
            }
        }
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        SupervisorJob() +
            Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, error ->
                android.util.Log.e(TAG, "Unhandled overlay coroutine failure", error)
            }
    )

    private var unlockWatchJob: kotlinx.coroutines.Job? = null
    private var lastUnlockAnnouncedMs = 0L

    /**
     * Tells the buddy the phone was unlocked. Some ROMs never deliver ACTION_USER_PRESENT to this app, so
     * after the screen turns on we also watch the keyguard for a few minutes at most (a cheap check four
     * times a second, only while it is showing) and announce the moment it goes away. A phone with no lock
     * is announced at once. Whichever signal comes first wins; the other is ignored.
     */
    private fun watchForUnlock() {
        unlockWatchJob?.cancel()
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isKeyguardLocked != true) {
            announceUnlock()
            return
        }
        unlockWatchJob = serviceScope.launch {
            repeat(UNLOCK_WATCH_MAX_TICKS) {
                kotlinx.coroutines.delay(UNLOCK_WATCH_INTERVAL_MS)
                if (keyguardManager.isKeyguardLocked != true) {
                    isLockScreenActive = false
                    if (::viewModel.isInitialized) {
                        updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                    }
                    announceUnlock()
                    return@launch
                }
            }
        }
    }

    private val mascotMapper = MascotEventMapper()
    private var timerWatchJob: kotlinx.coroutines.Job? = null
    private var watchedTimerKey: String? = null
    private var watchedTimerEnd = 0L

    /**
     * Feeds the buddy what happens on the island: transfers finishing or failing, timers starting and
     * running out, recording, music starting. Charging, low battery, the torch and unlock are posted where
     * they happen. Only runs while Buddy is on.
     */
    private fun startMascotObservers() {
        serviceScope.launch {
            notificationRepository.notifications.collect { list ->
                if (destroyed || !viewModel.settings.value.buddyEnabled) {
                    mascotMapper.reset()
                    cancelTimerWatch(post = false)
                    return@collect
                }
                val snapshots = list.map {
                    NoteSnapshot(
                        key = it.key, mode = it.mode, title = it.title, text = it.text,
                        progress = it.progress, progressMax = it.progressMax,
                        mediaIsPlaying = it.mediaIsPlaying, timeMillis = it.timeMillis
                    )
                }
                val moments = mascotMapper.onChange(
                    snapshots, android.os.SystemClock.elapsedRealtime() / 1000.0, System.currentTimeMillis()
                )
                for (moment in moments) {
                    if (moment == MascotMoment.Music) {
                        // Once the song's strip has settled, if the music still plays.
                        launch {
                            kotlinx.coroutines.delay(MUSIC_GROOVE_DELAY_MS)
                            val stillPlaying = notificationRepository.notifications.value.any {
                                it.mode == com.agupta07505.smartisland.model.IslandMode.Music && it.mediaIsPlaying
                            }
                            if (stillPlaying) MascotEvents.post(moment)
                        }
                    } else {
                        MascotEvents.post(moment)
                    }
                }
                watchTimer(list)
            }
        }
    }

    /** A running timer's last seconds are watched: the buddy comes out over its mark when 5s remain. */
    private fun watchTimer(list: List<IslandNotification>) {
        val timer = list.firstOrNull { it.mode == com.agupta07505.smartisland.model.IslandMode.Timer }
        if (timer == null) {
            cancelTimerWatch(post = true)
            return
        }
        val end = timer.timeMillis
        if (watchedTimerKey == timer.key && kotlin.math.abs(watchedTimerEnd - end) < TIMER_END_JITTER_MS) return
        // New, or its end moved (paused, resumed, changed): watch it afresh.
        cancelTimerWatch(post = watchedTimerKey != null)
        watchedTimerKey = timer.key
        watchedTimerEnd = end
        val untilWatch = end - System.currentTimeMillis() - (com.agupta07505.smartisland.buddy.motion.MascotMotion.COUNTDOWN_LEAD_SEC * 1000f).toLong()
        if (untilWatch < -4000L) return
        timerWatchJob = serviceScope.launch {
            kotlinx.coroutines.delay(maxOf(0L, untilWatch))
            val current = notificationRepository.notifications.value.firstOrNull { it.key == timer.key }
            if (current != null && kotlin.math.abs(current.timeMillis - end) < TIMER_END_JITTER_MS) {
                MascotEvents.post(MascotMoment.CountdownStart)
            }
        }
    }

    private fun cancelTimerWatch(post: Boolean) {
        timerWatchJob?.cancel()
        timerWatchJob = null
        if (watchedTimerKey != null && post) MascotEvents.post(MascotMoment.CountdownCancel)
        watchedTimerKey = null
        watchedTimerEnd = 0L
    }

    private fun announceUnlock() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastUnlockAnnouncedMs < UNLOCK_DEDUPE_MS) return
        lastUnlockAnnouncedMs = now
        MascotEvents.post(MascotMoment.Unlock)
    }

    // Monitor screen state and unlock events to show/hide the island accordingly
    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatchingLogged(TAG, "Screen-state callback failed") {
                if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        overlayOwners.resume()
                        viewModel.setScreenOn(true)
                        isLockScreenActive = keyguardManager?.isKeyguardLocked == true
                        guards?.biometric?.onScreenOn(isLockScreenActive)
                        viewModel.setLocked(isLockScreenActive)
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.settings.value
                        )
                        watchForUnlock()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        unlockWatchJob?.cancel()
                        // Two layers (Rule_Book 4.3, 5): the paused lifecycle stops the frame clock, and the
                        // screen-off state takes every looping animation and ticker out of the composition.
                        viewModel.setScreenOn(false)
                        overlayOwners.pause()
                        isLockScreenActive = true
                        guards?.biometric?.onScreenOff()
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.settings.value
                        )
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        isLockScreenActive = false
                        // Unmasks private alerts; the pill and the card crossfade to their text (Rule_Book §8.1).
                        viewModel.setLocked(false)
                        announceUnlock()
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.settings.value
                        )
                    }
                }
            }
        }
    }

    // Fallback sync: check if keyguard locked state changed on window changes.
    // Wrapped: an uncaught throw here makes Android auto-disable the
    // AccessibilityService, which is exactly the "turns off by itself" symptom.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        runCatchingLogged(TAG, "onAccessibilityEvent failed") {
            if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val locked = keyguardManager?.isKeyguardLocked == true
            if (isLockScreenActive != locked) {
                isLockScreenActive = locked
                updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
            }

            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val openedPackage = event.packageName?.toString()
                if (!openedPackage.isNullOrEmpty() &&
                    openedPackage != packageName &&
                    openedPackage != "com.android.systemui"
                ) {
                    viewModel.foregroundPackage.value = openedPackage
                    guards?.onForegroundPackage(openedPackage)
                    val hasNonMusicNotifications = notificationRepository.notifications.value.any {
                        it.packageName == openedPackage && it.mode != com.agupta07505.smartisland.model.IslandMode.Music
                    }
                    if (hasNonMusicNotifications) {
                        notificationRepository.removeNotificationsForPackage(openedPackage)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required override, no-op
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Wrapped: a throw here would make Android disable the service automatically.
        runCatchingLogged(TAG, "onConfigurationChanged failed") {
            if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
            viewModel.setLandscape(newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
            updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
        }
    }

    /**
     * The overlay can no longer be drawn (Rule_Book §7.1): the accessibility grant behind
     * `TYPE_ACCESSIBILITY_OVERLAY` was revoked, so any further window call would throw. Tears the overlay down
     * and stops the service before that can happen. [reason] is for the log; this is also the single place to
     * raise a "Restore Smart Island permissions" notification from.
     */
    private fun onPermissionLost(reason: String) {
        if (destroyed || permissionLost) return
        permissionLost = true
        android.util.Log.w(TAG, "Overlay permission lost: $reason; stopping the overlay")
        removeCollapsedWindow()
        stopForegroundSafely()
        if (::viewModel.isInitialized) viewModel.collapse()
        runCatchingLogged(TAG, "stopSelf failed") { stopSelf() }
    }

    /**
     * Runs a WindowManager call. A SecurityException or BadTokenException while the grant is really gone means
     * the permission was revoked; any other failure (an early call before the service connects, an OEM quirk)
     * is only logged, as before.
     */
    private inline fun guardWindowCall(what: String, block: () -> Unit): Boolean = try {
        block()
        true
    } catch (e: Exception) {
        android.util.Log.e(TAG, what, e)
        val revoked = (e is SecurityException || e is WindowManager.BadTokenException) &&
            !PermissionGate.isOverlayServiceEnabled(this)
        if (revoked) onPermissionLost(what)
        false
    }

    /** Pushes the overlay window's own cutout insets (Rule_Book §9.1) into the shared detection, in screen px. */
    private fun attachCutoutInsetsListener(view: android.view.View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            runCatchingLogged(TAG, "Reading cutout insets failed") { recordCutoutInsets(v, insets) }
            ViewCompat.onApplyWindowInsets(v, insets)
        }
    }

    private fun recordCutoutInsets(view: android.view.View, insets: WindowInsetsCompat) {
        val rects = insets.displayCutout?.boundingRects.orEmpty()
        if (rects.isEmpty()) return
        val origin = IntArray(2)
        view.getLocationOnScreen(origin)
        val bounds = DisplayBounds.bounds(this)
        CutoutInsets.update(
            rects.map { CutoutBox(it.left + origin[0], it.top + origin[1], it.right + origin[0], it.bottom + origin[1]) },
            bounds.width(),
            bounds.height()
        )
    }

    /** Sensitive actions wait for the keyguard to be dismissed (Rule_Book §8.3); the island collapses meanwhile. */
    private fun gated(action: GatedAction, block: () -> Unit) {
        KeyguardGate.run(this, action, onCollapse = { if (::viewModel.isInitialized) viewModel.collapse() }, block = block)
    }

    override fun onCreate() {
        super.onCreate()
        destroyed = false

        runCatchingLogged(TAG, "createNotificationChannel failed") {
            createNotificationChannel()
        }

        val resolvedWindowManager = runCatchingLogged(TAG, "WindowManager initialization failed") {
            getSystemService(WindowManager::class.java)
        }
        if (resolvedWindowManager == null) {
            android.util.Log.e(TAG, "WindowManager is unavailable; overlay cannot start")
            return
        }
        windowManager = resolvedWindowManager

        val initializedViewModel = runCatchingLogged(TAG, "Overlay ViewModel initialization failed") {
            // Lifecycle must be restored before the service-owned ViewModel is created.
            overlayOwners.resume()
            ViewModelProvider(
                overlayOwners,
                IslandViewModel.provideFactory(repository, notificationRepository)
            )[IslandViewModel::class.java]
        }
        if (initializedViewModel == null) {
            android.util.Log.e(TAG, "Overlay ViewModel is unavailable; overlay cannot start")
            return
        }
        viewModel = initializedViewModel
        viewModel.setScreenOn(
            (getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isInteractive != false
        )
        viewModel.setLandscape(resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
        startMascotObservers()
        guards = OverlayGuardController(
            context = this,
            windowManager = windowManager,
            viewModel = viewModel,
            notificationRepository = notificationRepository,
            scope = serviceScope,
            onWindowFlagsChanged = {
                if (!destroyed) updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
            },
            // Rule_Book 8.2 Secure App Passthrough: a secure foreground app blocks Ephemeral auto-expand.
            onForegroundSecure = { secure -> viewModel.setForegroundSecure(secure) }
        )
        privacyMonitor = PrivacyIndicatorMonitor(
            context = this,
            repository = notificationRepository,
            foregroundPackage = { viewModel.foregroundPackage.value },
            isLocked = { isLockScreenActive }
        )
        
        systemEventReceiver = SystemEventReceiver(notificationRepository, settingsProvider = { viewModel.settings.value })
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        val bluetoothFilter = IntentFilter().apply {
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }

        // Power events come from the system server and are fine with NOT_EXPORTED.
        // Bluetooth events are sent by the Bluetooth app (not a system uid), so they need an
        // exported receiver; the broadcast permission keeps other apps from spoofing them.
        runCatchingLogged(TAG, "registerReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(
                    systemEventReceiver,
                    bluetoothFilter,
                    bluetoothBroadcastPermission(),
                    null,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(systemEventReceiver, filter)
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(systemEventReceiver, bluetoothFilter, bluetoothBroadcastPermission(), null)
            }
            systemEventReceiverRegistered = true
        }
        // Hotspot on/off (Rule_Book 4.8), with or without a tethering notification. The broadcast is protected,
        // so only the system can send it; it comes from the Wi-Fi stack, hence exported. It is sticky, so
        // registering delivers the current state at once.
        runCatchingLogged(TAG, "registerReceiver hotspot failed") {
            val hotspotFilter = IntentFilter(com.agupta07505.smartisland.util.HotspotUtil.ACTION_WIFI_AP_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventReceiver, hotspotFilter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(systemEventReceiver, hotspotFilter)
            }
            systemEventReceiverRegistered = true
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isLockScreenActive = keyguardManager?.isKeyguardLocked == true
        viewModel.setLocked(isLockScreenActive)

        // New cutout insets move or resize the idle pill, so the window follows.
        serviceScope.launch {
            CutoutInsets.snapshot.collect {
                if (!destroyed && ::viewModel.isInitialized) {
                    updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                }
            }
        }

        // A buddy that tucks away or comes back changes how wide the pill is, so the window and touch region follow.
        serviceScope.launch {
            MascotRuntime.controller.tucked.collect {
                if (!destroyed && ::viewModel.isInitialized) {
                    updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                }
            }
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatchingLogged(TAG, "registerReceiver screenStateReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenStateReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(screenStateReceiver, screenFilter)
            }
            screenStateReceiverRegistered = true
        }

        runCatchingLogged(TAG, "registerTorchCallback failed") {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            cameraManager?.registerTorchCallback(torchCallback, android.os.Handler(android.os.Looper.getMainLooper()))
            torchCallbackRegistered = true
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                repository.settings.collect { settings ->
                    if (destroyed) return@collect
                    if (!settings.enabled) {
                        stopOverlaySession()
                    } else {
                        startOverlaySession(settings)
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded-state collector failed") {
                viewModel.expanded.collectLatest { expanded ->
                    if (destroyed || !viewModel.settings.value.enabled) {
                        return@collectLatest
                    }
                    collapseJob?.cancel()
                    if (expanded) {
                        isWindowExpanded = true
                        updateWindowLayoutParams(true, viewModel.settings.value)
                    } else {
                        collapseJob = serviceScope.launch {
                            kotlinx.coroutines.delay(AUTO_COLLAPSE_DELAY_MS)
                            isWindowExpanded = false
                            updateWindowLayoutParams(false, viewModel.settings.value)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Notifications-state collector failed") {
                viewModel.visibleNotifications.collectLatest {
                    if (destroyed || !viewModel.settings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Input-active collector failed") {
                viewModel.isInputActive.collectLatest {
                    if (destroyed || !viewModel.settings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isSystemConnected = true
        PermissionRecoveryNotifier.cancelIfRestored(this)
        if (destroyed || !::viewModel.isInitialized) return
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Service reconnect failed") {
                val settings = repository.settings.first()
                if (settings.enabled) {
                    startOverlaySession(settings)
                } else {
                    stopOverlaySession()
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isSystemConnected = false
        runCatchingLogged(TAG, "Unbind permission check failed") {
            if (!PermissionGate.isOverlayServiceEnabled(this)) onPermissionLost("overlay access revoked (unbound)")
        }
        // Return true so Android system knows to re-bind the accessibility service automatically
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        runCatchingLogged(TAG, "onTaskRemoved recovery failed") {
            if (!destroyed &&
                ::viewModel.isInitialized &&
                viewModel.settings.value.enabled
            ) {
                ensureForegroundStarted()
                ensureCollapsedWindow()
            }
        }
    }

    private fun bluetoothBroadcastPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.Manifest.permission.BLUETOOTH_CONNECT
        } else {
            android.Manifest.permission.BLUETOOTH
        }

    override fun onDestroy() {
        if (destroyed) return
        // The island was meant to be on, so the accessibility service (its overlay) was revoked or killed.
        if (::viewModel.isInitialized && viewModel.settings.value.enabled) {
            PermissionRecoveryNotifier.notify(this, PermissionRecoveryNotifier.Reason.OverlayStopped)
        }
        destroyed = true
        guards?.stop()
        guards = null
        privacyMonitor?.stop()
        privacyMonitor = null
        isSystemConnected = false
        serviceScope.cancel()

        if (::systemEventReceiver.isInitialized && systemEventReceiverRegistered) {
            runCatchingLogged(TAG, "unregisterReceiver failed") {
                unregisterReceiver(systemEventReceiver)
            }
            systemEventReceiverRegistered = false
        }
        if (screenStateReceiverRegistered) {
            runCatchingLogged(TAG, "unregisterReceiver screenStateReceiver failed") {
                unregisterReceiver(screenStateReceiver)
            }
            screenStateReceiverRegistered = false
        }
        if (torchCallbackRegistered) {
            runCatchingLogged(TAG, "unregisterTorchCallback failed") {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                cameraManager?.unregisterTorchCallback(torchCallback)
            }
            torchCallbackRegistered = false
        }

        removeCollapsedWindow()
        stopForegroundSafely()
        runCatchingLogged(TAG, "Overlay owners destroy failed") {
            overlayOwners.destroy()
        }
        super.onDestroy()
    }

    private fun startOverlaySession(settings: SmartIslandSettings) {
        if (destroyed || permissionLost || !::windowManager.isInitialized || !::viewModel.isInitialized) return
        ensureForegroundStarted()
        ensureCollapsedWindow()
        guards?.start()
        privacyMonitor?.start()
        updateWindowLayoutParams(isWindowExpanded, settings)
    }

    private fun stopOverlaySession() {
        guards?.stop()
        privacyMonitor?.stop()
        removeCollapsedWindow()
        stopForegroundSafely()
        if (::viewModel.isInitialized) {
            viewModel.collapse()
        }
    }

    private fun ensureForegroundStarted() {
        if (foregroundStarted || destroyed) return
        runCatchingLogged(TAG, "startForeground failed") {
            // Rule_Book 7.2: typed foreground service, matching the manifest's foregroundServiceType="specialUse".
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                overlayForegroundServiceType(Build.VERSION.SDK_INT)
            )
            foregroundStarted = true
        }
    }

    private fun stopForegroundSafely() {
        if (!foregroundStarted) return
        runCatchingLogged(TAG, "stopForeground failed") {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        foregroundStarted = false
    }

    private val statusBarHeight: Float
        get() {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val heightPx = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val heightDp = heightPx / resources.displayMetrics.density
            return if (heightDp > 0f) heightDp else 24f
        }

    private fun ensureCollapsedWindow() {
        if (destroyed ||
            islandView != null ||
            !::windowManager.isInitialized ||
            !::viewModel.isInitialized
        ) return
        try {
            islandView = ComposeView(this).apply {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isLocked = keyguardManager?.isKeyguardLocked == true
                isLockScreenActive = isLocked
                val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val isHidden = (!viewModel.settings.value.showOnLockScreen && isLocked) || (isLandscape && !viewModel.settings.value.showInLandscape)
                visibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE

                installOverlayViewTreeOwners()
                isFocusable = true
                isFocusableInTouchMode = true
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    val touchRegion by isTouchableRegionSupported.collectAsState()
                    val anchored by landscapeAnchored.collectAsState()
                    // Docked in landscape the window is only as wide as the pill, so the content centres in it.
                    val fullWidth = touchRegion && !anchored
                    val suppressed by this@SmartIslandOverlayService.viewModel.isSuppressed.collectAsState()
                    OverlayIsland(
                        viewModel = this@SmartIslandOverlayService.viewModel,
                        statusBarHeight = statusBarHeight,
                        onOpenNotification = { notification -> gated(GatedAction.OpenApp) { openNotification(notification) } },
                        onLaunchApp = { packageName -> gated(GatedAction.OpenApp) { launchApp(packageName) } },
                        onOpenFloatingWindow = { gated(GatedAction.OpenApp) { openCurrentNotificationInFloatingWindow() } },
                        onOpenNotificationShade = {
                            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                            viewModel.collapse()
                        },
                        isFullWidth = fullWidth,
                        modifier = androidx.compose.ui.Modifier.suppressedFade(suppressed)
                    )
                }

                setupTouchableRegion(this)
                attachCutoutInsetsListener(this)
            }
            val added = guardWindowCall("windowManager.addView failed") {
                windowManager.addView(islandView, collapsedParams(viewModel.settings.value))
            }
            if (added) {
                islandView?.let { ViewCompat.requestApplyInsets(it) }
            } else {
                islandView = null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "ensureCollapsedWindow fatal", e)
            islandView = null
        }
    }

    // Use reflection to set up OnComputeInternalInsetsListener since it is a hidden system API.
    // This allows the overlay window to pass through touches outside the pill boundary.
    // Keep the suppression local: this best-effort workaround is guarded by
    // runCatchingLogged so unsupported devices fall back without crashing.
    @SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
    private fun setupTouchableRegion(view: ComposeView) {
        android.util.Log.d(TAG, "setupTouchableRegion: starting registration for view=$view")
        runCatchingLogged(TAG, "Failed to setup touchable region") {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val insetsClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")
            
            val setTouchableInsetsMethod = insetsClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
            val touchableRegionField = insetsClass.getDeclaredField("touchableRegion").apply {
                isAccessible = true
            }
            
            // InternalInsetsInfo touchable insets options
            val TOUCHABLE_INSETS_FRAME = 0
            val TOUCHABLE_INSETS_REGION = 3
            
            // Create a dynamic proxy implementation of OnComputeInternalInsetsListener
            val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                    val insets = args[0]
                    val isExpanded = viewModel.expanded.value
                    val isGone = view.visibility == android.view.View.GONE
                    val settingsVal = viewModel.settings.value
                    val notificationsCount = viewModel.visibleNotifications.value.size
                    val isIdleHidden = PillLayout.windowIdleHidden(settingsVal, notificationsCount)
                    android.util.Log.d(TAG, "onComputeInternalInsets callback: isExpanded=$isExpanded isGone=$isGone isIdleHidden=$isIdleHidden")
                    if (isGone || isIdleHidden) {
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        val region = touchableRegionField.get(insets) as android.graphics.Region
                        region.setEmpty()
                    } else if (isExpanded || landscapeAnchored.value) {
                        // Docked in landscape the window is just the pill group, so all of it is the pill.
                        // When expanded, let the entire frame intercept touches so clicking outside collapses it
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                    } else {
                        // PILL-ONLY TOUCHABLE REGION:
                        // Restrict touch interception to ONLY the pill bounds + padding.
                        // The region is local to this already-offset window. Touches outside
                        // the visible collapsed group pass through to the system.
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        
                        val density = resources.displayMetrics.density
                        val screenWidth = DisplayBounds.widthPx(this@SmartIslandOverlayService)
                        val isSplitMode = notificationsCount >= 2 && !settingsVal.enableNotchMode
                        val isCircleLeft = settingsVal.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT

                        val mainWidthPx = settingsVal.width * density
                        val circleSizePx = settingsVal.height * density
                        val compactGapPx = 8f * density
                        val edgePaddingPx = 8f * density
                        val touchPaddingXPx = 16f * density
                        val pillHeightPx = (settingsVal.height + 16f) * density
                        val groupWidthPx = mainWidthPx + if (isSplitMode) compactGapPx + circleSizePx else 0f

                        val desiredMainLeftPx = screenWidth / 2f +
                            settingsVal.xOffset * density - mainWidthPx / 2f
                        val (minMainLeftPx, maxMainLeftPx) = when {
                            !isSplitMode -> edgePaddingPx to (screenWidth - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx)
                            isCircleLeft -> (edgePaddingPx + circleSizePx + compactGapPx) to (screenWidth - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx + circleSizePx + compactGapPx)
                            else -> edgePaddingPx to (screenWidth - edgePaddingPx - groupWidthPx).coerceAtLeast(edgePaddingPx)
                        }
                        val mainLeftPx = desiredMainLeftPx.coerceIn(minMainLeftPx, maxMainLeftPx)
                        val groupStartPx = if (isCircleLeft && isSplitMode) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx
                        val groupEndPx = if (!isCircleLeft && isSplitMode) mainLeftPx + mainWidthPx + compactGapPx + circleSizePx else mainLeftPx + mainWidthPx
                        val left = (groupStartPx - touchPaddingXPx).toInt().coerceAtLeast(0)
                        val top = 0
                        val right = (groupEndPx + touchPaddingXPx).toInt().coerceAtMost(screenWidth)
                        val bottom = pillHeightPx.toInt()
                        
                        android.util.Log.d(TAG, "onComputeInternalInsets: region set to ($left, $top, $right, $bottom), isSplitMode=$isSplitMode")
                        val region = touchableRegionField.get(insets) as android.graphics.Region
                        region.set(left, top, right, bottom)
                    }
                }
                null
            }
            
            val registerListener = {
                val observer = view.viewTreeObserver
                android.util.Log.d(TAG, "registerListener lambda: viewTreeObserver=$observer, isAlive=${observer.isAlive}")
                if (observer.isAlive) {
                    val addListenerMethod = observer.javaClass.getMethod(
                        "addOnComputeInternalInsetsListener",
                        listenerClass
                    )
                    addListenerMethod.invoke(observer, proxyListener)
                    isTouchableRegionSupported.value = true
                    android.util.Log.d(TAG, "OnComputeInternalInsetsListener successfully registered on live ViewTreeObserver")
                    if (::viewModel.isInitialized && !isWindowExpanded) {
                        updateWindowLayoutParams(false, viewModel.settings.value)
                    }
                }
            }
            
            // ViewTreeObserver changes when the view is attached to a window.
            // We must register the listener on the live ViewTreeObserver of the attached window.
            android.util.Log.d(TAG, "setupTouchableRegion: isAttachedToWindow=${view.isAttachedToWindow}")
            if (view.isAttachedToWindow) {
                registerListener()
            } else {
                view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: android.view.View) {
                        android.util.Log.d(TAG, "onViewAttachedToWindow: registering listener now")
                        registerListener()
                    }
                    override fun onViewDetachedFromWindow(v: android.view.View) {
                        android.util.Log.d(TAG, "onViewDetachedFromWindow called")
                    }
                })
            }
        } ?: run {
            isTouchableRegionSupported.value = false
            android.util.Log.w(TAG, "Touchable region reflection unsupported or blocked, falling back to physical bounds")
        }
    }

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        if (destroyed || !::windowManager.isInitialized || !::viewModel.isInitialized) return
        val view = islandView ?: return
        val density = resources.displayMetrics.density
        val screenWidthPx = DisplayBounds.widthPx(this).toFloat()
        
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked == true
        isLockScreenActive = isLocked
        viewModel.isLocked.value = isLocked
        if (!isLocked) guards?.biometric?.onUnlocked()
        
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isIdleHidden = PillLayout.windowIdleHidden(settings, viewModel.visibleNotifications.value.size)
        val isHidden = (!settings.showOnLockScreen && isLocked) || (isLandscape && !settings.showInLandscape) || isIdleHidden

        val targetVisibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE
        if (view.visibility != targetVisibility) {
            view.visibility = targetVisibility
        }

        val isSplitMode = (viewModel.visibleNotifications.value.size >= 2) && !settings.enableNotchMode
        val isCircleLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
        val mainWidthPx = PillLayout.effectiveWidthDp(settings, cutoutHoleWidthDp(), visibleCount(), MascotRuntime.controller.tucked.value, contentWingDp(), AlertWidthBudget.maxAlertPillWidthDp(this, settings)) * density
        val circleSizePx = settings.height * density
        val compactGapPx = 8f * density
        val edgePaddingPx = 8f * density
        val groupWidthPx = mainWidthPx + if (isSplitMode) compactGapPx + circleSizePx else 0f
        
        val desiredMainLeftPx = screenWidthPx / 2f + settings.xOffset * density - mainWidthPx / 2f
        val (minMainLeftPx, maxMainLeftPx) = when {
            !isSplitMode -> edgePaddingPx to (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx)
            isCircleLeft -> (edgePaddingPx + circleSizePx + compactGapPx) to (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx + circleSizePx + compactGapPx)
            else -> edgePaddingPx to (screenWidthPx - edgePaddingPx - groupWidthPx).coerceAtLeast(edgePaddingPx)
        }
        val mainLeftPx = desiredMainLeftPx.coerceIn(minMainLeftPx, maxMainLeftPx)
        val groupStartPx = if (isCircleLeft && isSplitMode) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx
        val groupEndPx = if (!isCircleLeft && isSplitMode) mainLeftPx + mainWidthPx + compactGapPx + circleSizePx else mainLeftPx + mainWidthPx
        val groupCenterPx = (groupStartPx + groupEndPx) / 2f
        val windowXPx = (groupCenterPx - screenWidthPx / 2f).toInt()

        // Rule_Book 9.2: sideways, the collapsed pill (and its companion bubble) docks on the lateral cutout.
        val lateralAnchor = if (isLandscape && !expanded && !settings.enableNotchMode) {
            CameraCutoutDetector.lateralAnchor(this)
        } else null
        val anchoredPosition = lateralAnchor?.let { anchor ->
            LandscapeAnchoring.windowPosition(
                anchor = anchor,
                screenWidthPx = screenWidthPx.toInt(),
                screenHeightPx = DisplayBounds.heightPx(this),
                mainWidthPx = mainWidthPx,
                groupWidthPx = groupWidthPx,
                mainOffsetInGroupPx = if (isCircleLeft && isSplitMode) circleSizePx + compactGapPx else 0f,
                pillHeightPx = settings.height * density,
                windowPaddingPx = 16f * density,
                marginPx = LANDSCAPE_HOLE_MARGIN_DP * density
            )
        }
        landscapeAnchored.value = anchoredPosition != null

        val h = if (expanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            ((settings.height + 16f) * density).toInt()
        }
        val w = if (expanded || (isTouchableRegionSupported.value && anchoredPosition == null)) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            (groupWidthPx + 32f * density).toInt()
        }
        val isInput = viewModel.isInputActive.value && expanded
        val focusFlags = if (isInput) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // The idle dashboard (Large) blurs what is behind the island (Android 12+; no-op where the
        // compositor has background blur disabled). The overlay is full-screen only while expanded.
        val blurRadiusPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            expanded && viewModel.visibleNotifications.value.isEmpty() && !isInput
        ) (BACKDROP_BLUR_RADIUS_DP * density).toInt() else 0
        val blurFlag = if (blurRadiusPx > 0) WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0
        val currentFlags = focusFlags or blurFlag or (guards?.extraWindowFlags() ?: 0) or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        val currentX = anchoredPosition?.xPx ?: if (expanded || isTouchableRegionSupported.value) 0 else windowXPx
        val currentY = anchoredPosition?.yPx ?: if (settings.enableNotchMode) 0 else settings.yOffset.dpToPx()
        val currentGravity = if (anchoredPosition != null) Gravity.TOP or Gravity.LEFT else Gravity.TOP or Gravity.CENTER_HORIZONTAL
        val currentSoftInputMode = if (isInput) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        } else {
            0
        }

        val lp = lastParams
        if (lp != null &&
            lp.width == w &&
            lp.height == h &&
            lp.flags == currentFlags &&
            lastBlurRadiusPx == blurRadiusPx &&
            lp.x == currentX &&
            lp.y == currentY &&
            lp.gravity == currentGravity &&
            lp.softInputMode == currentSoftInputMode
        ) {
            return
        }

        val params = WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            currentFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = currentGravity
            x = currentX
            y = currentY
            softInputMode = currentSoftInputMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = blurRadiusPx
            }
        }
        lastBlurRadiusPx = blurRadiusPx
        lastParams = params
        guardWindowCall("Failed to update view layout") {
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun removeCollapsedWindow() {
        val view = islandView ?: return
        // Clear the reference before removal so repeated teardown calls are harmless,
        // even when an OEM WindowManager throws while detaching an already-removed view.
        islandView = null
        lastParams = null
        isWindowExpanded = false
        collapseJob?.cancel()
        collapseJob = null
        if (!::windowManager.isInitialized) return
        runCatchingLogged(TAG, "Failed to remove view") {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            }
        }
    }

    /** What the overlay composable counts: the notifications actually shown (foreground music is filtered out). */
    /** Extra room the active mode's right-hand pill content needs beside the camera hole. */
    private fun contentWingDp(): Float =
        if (::viewModel.isInitialized)
            PillLayout.contentWingDp(viewModel.visibleNotifications.value.getOrNull(viewModel.selectedIndex.value))
        else 0f

    private fun visibleCount(): Int = if (::viewModel.isInitialized) viewModel.visibleNotifications.value.size else 0

    /** The camera hole's true width in dp, or null on devices without a hardware cutout. */
    private fun cutoutHoleWidthDp(): Float? =
        CameraCutoutDetector.detect(this).let { if (it.hasHardwareCutout) it.rawWidthDp else null }

    private fun collapsedParams(settings: SmartIslandSettings): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val screenWidthPx = DisplayBounds.widthPx(this).toFloat()
        val isSplitMode = if (settings.enableNotchMode) false else (if (::viewModel.isInitialized) viewModel.visibleNotifications.value.size >= 2 else false)
        val isCircleLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
        val mainWidthPx = PillLayout.effectiveWidthDp(settings, cutoutHoleWidthDp(), visibleCount(), MascotRuntime.controller.tucked.value, contentWingDp(), AlertWidthBudget.maxAlertPillWidthDp(this, settings)) * density
        val circleSizePx = settings.height * density
        val compactGapPx = 8f * density
        val edgePaddingPx = 8f * density
        val groupWidthPx = mainWidthPx + if (isSplitMode) compactGapPx + circleSizePx else 0f
        
        val desiredMainLeftPx = screenWidthPx / 2f + settings.xOffset * density - mainWidthPx / 2f
        val (minMainLeftPx, maxMainLeftPx) = when {
            !isSplitMode -> edgePaddingPx to (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx)
            isCircleLeft -> (edgePaddingPx + circleSizePx + compactGapPx) to (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx + circleSizePx + compactGapPx)
            else -> edgePaddingPx to (screenWidthPx - edgePaddingPx - groupWidthPx).coerceAtLeast(edgePaddingPx)
        }
        val mainLeftPx = desiredMainLeftPx.coerceIn(minMainLeftPx, maxMainLeftPx)
        val groupStartPx = if (isCircleLeft && isSplitMode) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx
        val groupEndPx = if (!isCircleLeft && isSplitMode) mainLeftPx + mainWidthPx + compactGapPx + circleSizePx else mainLeftPx + mainWidthPx
        val groupCenterPx = (groupStartPx + groupEndPx) / 2f
        val windowXPx = (groupCenterPx - screenWidthPx / 2f).toInt()
        
        val w = if (isTouchableRegionSupported.value) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            (groupWidthPx + 32f * density).toInt()
        }
        val currentFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or (guards?.extraWindowFlags() ?: 0) or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        return WindowManager.LayoutParams(
            w,
            ((settings.height + 16f) * density).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            currentFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (isTouchableRegionSupported.value) 0 else windowXPx
            y = if (settings.enableNotchMode) 0 else settings.yOffset.dpToPx()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }.also {
            lastParams = it
        }
    }

    private fun openNotification(notification: IslandNotification) {
        if (notification.contentIntent != null) {
            sendIntentWithOptions(this, notification.contentIntent)
        } else {
            runCatchingLogged(TAG, "Failed to launch package activity") {
                when (notification.mode) {
                    com.agupta07505.smartisland.model.IslandMode.Bluetooth -> {
                        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    com.agupta07505.smartisland.model.IslandMode.Battery -> {
                        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        } else {
                            val altIntent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(altIntent)
                        }
                    }
                    com.agupta07505.smartisland.model.IslandMode.PrivacyIndicator -> {
                        com.agupta07505.smartisland.ui.expanded.openPrivacyDashboard(this)
                    }
                    com.agupta07505.smartisland.model.IslandMode.Hotspot -> {
                        val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    com.agupta07505.smartisland.model.IslandMode.Timer,
                    com.agupta07505.smartisland.model.IslandMode.Stopwatch -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                            ?: Intent(android.provider.AlarmClock.ACTION_SHOW_TIMERS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        if (launchIntent.resolveActivity(packageManager) != null) {
                            startActivity(launchIntent)
                        } else {
                            val clockIntent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(clockIntent)
                        }
                    }
                    else -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        } else {
                            Toast.makeText(this, "Opening ${notification.appName} (Demo)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        if (notification.mode != com.agupta07505.smartisland.model.IslandMode.Music) {
            notificationRepository.removeNotificationsForPackage(notification.packageName)
        }
        viewModel.collapse()
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(this, "App is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatchingLogged(TAG, "Failed to launch shortcut app") {
            startActivity(launchIntent)
        }
        notificationRepository.removeNotificationsForPackage(packageName)
        viewModel.collapse()
    }

    private fun openCurrentNotificationInFloatingWindow() {
        val list = viewModel.visibleNotifications.value
        val index = viewModel.selectedIndex.value
        if (list.isNotEmpty() && index in list.indices) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Toast.makeText(this, "Floating window requires Android 7+.", Toast.LENGTH_SHORT).show()
                viewModel.collapse()
                return
            }
            val notification = list[index]
            val options = ActivityOptions.makeBasic()
            runCatchingLogged(TAG, "Failed to set launch bounds") {
                val displayBounds = DisplayBounds.bounds(this)
                val screenWidth = displayBounds.width()
                val screenHeight = displayBounds.height()
                val w = (screenWidth * 0.90f).toInt()
                val h = (screenHeight * 0.65f).toInt()
                val left = (screenWidth - w) / 2
                val top = (screenHeight - h) / 2
                options.setLaunchBounds(android.graphics.Rect(left, top, left + w, top + h))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatchingLogged(TAG, "Failed to set background activity start mode") {
                    options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
            }
            val bundle = options.toBundle() ?: android.os.Bundle()
            bundle.putInt("android.activity.windowingMode", WINDOWING_MODE_FREEFORM)

            val fillInIntent = Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            if (notification.contentIntent != null) {
                runCatchingLogged(TAG, "Failed to send content intent") {
                    notification.contentIntent.send(this, 0, fillInIntent, null, null, null, bundle)
                }
            } else {
                runCatchingLogged(TAG, "Failed to launch package activity") {
                    val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        startActivity(launchIntent, bundle)
                    } else {
                        Toast.makeText(this, "Opening ${notification.appName} in floating window (Demo)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            notificationRepository.removeNotification(notification.key)
            notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
        }
        viewModel.collapse()
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun ComposeView.installOverlayViewTreeOwners() {
        setViewTreeLifecycleOwner(overlayOwners)
        setViewTreeViewModelStoreOwner(overlayOwners)
        setViewTreeSavedStateRegistryOwner(overlayOwners)
    }

    /**
     * Silent, minimized channel for the foreground notification (Rule_Book 7.2): no sound, vibration, light or
     * badge, and IMPORTANCE_MIN so it folds into the shade's silent section. Android never raises or lowers an
     * existing channel's importance for us, so the old IMPORTANCE_LOW channel is replaced, not updated.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                OVERLAY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the Smart Island overlay running"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.deleteNotificationChannel(LEGACY_OVERLAY_CHANNEL_ID)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.agupta07505.smartisland.MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setContentTitle("Smart Island is active")
            .setContentText("Tap to open Smart Island")
            .setSmallIcon(R.drawable.ic_stat_smart_island)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        /**
         * The type passed to startForeground (Rule_Book 7.2). Android 14+ requires an explicit type, which must
         * be the manifest's `specialUse`; Android 10-13 have no such constant, so they take the manifest's types.
         * Before Android 10 the type is ignored.
         */
        internal fun overlayForegroundServiceType(sdkInt: Int): Int = when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            sdkInt >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            else -> 0
        }

        private const val TAG = "SmartIslandOverlayService"
        private const val BACKDROP_BLUR_RADIUS_DP = 24f
        private const val NOTIFICATION_ID = 8105
        private const val WINDOWING_MODE_FREEFORM = 5
        private const val OVERLAY_CHANNEL_ID = "smart_island_overlay_silent"
        private const val LEGACY_OVERLAY_CHANNEL_ID = "smart_island_overlay"
        private const val OVERLAY_CHANNEL_NAME = "Smart Island overlay"
        private const val AUTO_COLLAPSE_DELAY_MS = 220L
        /** Air between the lateral camera hole and the docked pill's outer end (Rule_Book 9.2). */
        private const val LANDSCAPE_HOLE_MARGIN_DP = 6f
        private const val UNLOCK_WATCH_INTERVAL_MS = 250L
        private const val MUSIC_GROOVE_DELAY_MS = 1200L
        private const val TIMER_END_JITTER_MS = 1500L
        private const val UNLOCK_WATCH_MAX_TICKS = 960
        private const val UNLOCK_DEDUPE_MS = 3000L
    }
}
