/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

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
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
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
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.util.LandscapeCameraDetector
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

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
    private var widgetWindowOpen: Boolean = false
    private var widgetWindowJob: kotlinx.coroutines.Job? = null
    private var collapseJob: kotlinx.coroutines.Job? = null
    private var lastParams: WindowManager.LayoutParams? = null

    private val torchCallback = object : android.hardware.camera2.CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (destroyed || !::viewModel.isInitialized) return
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
                    autoExpand = !com.agupta07505.smartisland.util.NotificationCooldownManager.shouldThrottleSystemEvent(
                        com.agupta07505.smartisland.model.IslandEvent.FLASHLIGHT.id,
                        viewModel.displaySettings.value
                    )
                )
            } else {
                notificationRepository.removeNotification("system_flashlight")
            }
        }
    }

    private var connectivityMonitor: com.agupta07505.smartisland.util.ConnectivityEventMonitor? = null
    private var wiredDeviceMonitor: com.agupta07505.smartisland.util.WiredDeviceEventMonitor? = null
    private var transferSpeedMonitor: com.agupta07505.smartisland.util.TransferSpeedMonitor? = null
    private var faceUnlockWatcher: com.agupta07505.smartisland.util.FaceUnlockWatcher? = null

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        SupervisorJob() +
            Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, error ->
                android.util.Log.e(TAG, "Unhandled overlay coroutine failure", error)
            }
    )

    // Face ID scan: driven by screen events, the unlock broadcast and the system log
    private val faceUnlockCoordinator = com.agupta07505.smartisland.util.FaceUnlockCoordinator(
        object : com.agupta07505.smartisland.util.FaceUnlockCoordinator.Output {
            override fun startScan() = startScan(com.agupta07505.smartisland.util.FaceIdController.REAL_WAIT_MS)
            override fun startScan(waitMs: Int) {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "scan started (gives the system up to ${waitMs} ms to answer)")
                com.agupta07505.smartisland.util.FaceIdController.startForUnlock(
                    com.agupta07505.smartisland.util.FaceIdController.styleOf(viewModel.settings.value.faceIdStyle),
                    waitMs
                )
            }
            override fun startAppScan() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "app scan started (app Face ID style)")
                com.agupta07505.smartisland.util.FaceIdController.startForApp(
                    com.agupta07505.smartisland.util.FaceIdController.styleOf(viewModel.settings.value.appFaceIdStyle)
                )
            }
            override fun startAppScanEarly() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "app scan started early (blinking waits for the real scan)")
                com.agupta07505.smartisland.util.FaceIdController.startForApp(
                    com.agupta07505.smartisland.util.FaceIdController.styleOf(viewModel.settings.value.appFaceIdStyle),
                    anchored = false
                )
            }
            override fun anchor() = com.agupta07505.smartisland.util.FaceIdController.anchor()
            override fun accept() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "face accepted: tick plays")
                com.agupta07505.smartisland.util.FaceIdController.accept()
            }
            override fun reject() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "scan ended without a match: no-face ending plays")
                com.agupta07505.smartisland.util.FaceIdController.reject()
            }
            override fun fingerprint() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "fingerprint unlock: fingerprint animation plays")
                com.agupta07505.smartisland.util.FaceIdController.fingerprint()
            }
            override fun startFingerprintScan() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "fingerprint unlock from the dark screen: fingerprint animation plays")
                com.agupta07505.smartisland.util.FaceIdController.startFingerprint(
                    com.agupta07505.smartisland.util.FaceIdController.styleOf(viewModel.settings.value.faceIdStyle)
                )
            }
            override fun startAppFingerprintScan() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "fingerprint unlock in an app prompt: fingerprint animation plays")
                com.agupta07505.smartisland.util.FaceIdController.startFingerprint(
                    com.agupta07505.smartisland.util.FaceIdController.styleOf(viewModel.settings.value.appFaceIdStyle)
                )
            }
            override fun cancel() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "scan cancelled")
                com.agupta07505.smartisland.util.FaceIdController.cancel()
            }
            override fun stop() {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "scan stopped")
                com.agupta07505.smartisland.util.FaceIdController.stop()
            }

            override fun isScanning(): Boolean = com.agupta07505.smartisland.util.FaceIdController.state.value.active
            override fun later(delayMs: Long, block: () -> Unit) {
                serviceScope.launch {
                    kotlinx.coroutines.delay(delayMs)
                    block()
                }
            }
            override fun note(text: String) {
                com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", text)
            }
        }
    )

    private fun handleMiuiAppLock(event: com.agupta07505.smartisland.util.MiuiAppLockEvent) {
        val now = android.os.SystemClock.elapsedRealtime()
        when (event) {
            com.agupta07505.smartisland.util.MiuiAppLockEvent.PromptShown,
            com.agupta07505.smartisland.util.MiuiAppLockEvent.ScanStart -> {
                val power = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                faceUnlockCoordinator.syncState(
                    screenOn = power?.isInteractive != false,
                    locked = keyguard?.isKeyguardLocked == true
                )
                if (event == com.agupta07505.smartisland.util.MiuiAppLockEvent.PromptShown) {
                    faceUnlockCoordinator.onAppLockRequired()
                } else {
                    faceUnlockCoordinator.onLog(
                        com.agupta07505.smartisland.util.FaceUnlockLogEvent.Start,
                        now,
                        strong = true,
                        fromApp = true
                    )
                }
            }
            com.agupta07505.smartisland.util.MiuiAppLockEvent.FaceMatched -> faceUnlockCoordinator.onAppUnlocked(now)
            com.agupta07505.smartisland.util.MiuiAppLockEvent.FingerprintMatched ->
                faceUnlockCoordinator.onUnlockType(com.agupta07505.smartisland.util.UnlockType.Fingerprint, now)
        }
    }

    private fun configureFaceUnlock(effective: com.agupta07505.smartisland.data.SmartIslandSettings) {
        val on = effective.enabled &&
            effective.isEventEnabled(com.agupta07505.smartisland.model.IslandEvent.FACE_UNLOCK.id)
        val shizuku = com.agupta07505.smartisland.util.ShizukuManager.hasPermission()
        val logAvailable = on && shizuku
        // app scans show up only in the system log, so they need Shizuku
        val appOn = effective.enabled && effective.appFaceIdEnabled && shizuku
        faceUnlockCoordinator.configure(
            enabled = on,
            mode = com.agupta07505.smartisland.util.FaceUnlockMode.fromId(effective.faceUnlockMode),
            logAvailable = logAvailable,
            appEnabled = appOn,
            fingerprintEnabled = effective.fingerprintAnimationEnabled
        )
        val watcher = faceUnlockWatcher
        if ((logAvailable || appOn) && watcher != null) watcher.start() else watcher?.stop()
        val summary = "lockScreen=$on appFaceId=${effective.appFaceIdEnabled} fingerprint=${effective.fingerprintAnimationEnabled} shizuku=$shizuku appOn=$appOn " +
            "watcherRunning=${watcher?.running == true} mode=${effective.faceUnlockMode}"
        if (summary != lastFaceUnlockConfig) {
            lastFaceUnlockConfig = summary
            com.agupta07505.smartisland.util.AppLogRecorder.i("FaceUnlock", "config: $summary")
        }
    }

    private var lastFaceUnlockConfig: String? = null

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatchingLogged(TAG, "Screen-state callback failed") {
                if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        overlayOwners.resume()
                        isLockScreenActive = keyguardManager?.isKeyguardLocked == true
                        com.agupta07505.smartisland.util.AppLogRecorder.i(
                            "FaceUnlock",
                            "screen-on broadcast received: locked=$isLockScreenActive"
                        )
                        // Shizuku may have come up since the last check.
                        configureFaceUnlock(viewModel.displaySettings.value)
                        faceUnlockCoordinator.onScreenOn(isLockScreenActive, android.os.SystemClock.elapsedRealtime())
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.displaySettings.value
                        )
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        overlayOwners.pause()
                        viewModel.closeWidgetIsland(snap = true)
                        // The shade closes with the screen; its window event may come late.
                        shadeOpen = false
                        viewModel.setShadeOpen(false)
                        isLockScreenActive = true
                        // HyperOS can deliver this late, after the screen has woken again
                        val stillOn = (getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isInteractive == true
                        if (!stillOn) faceUnlockCoordinator.onScreenOff()
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.displaySettings.value
                        )
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        isLockScreenActive = false
                        faceUnlockCoordinator.onUserPresent(android.os.SystemClock.elapsedRealtime())
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.displaySettings.value
                        )
                    }
                }
            }
        }
    }

    // Fallback sync: check if keyguard locked state changed on window changes.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        runCatchingLogged(TAG, "onAccessibilityEvent failed") {
            if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
            // Content-change events arrive in the hundreds per second while any app draws (a playing video or music
            // app, a scrolling list, this island's own animation).
            val eventType = event?.eventType
            val nowMs = android.os.SystemClock.elapsedRealtime()
            if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                nowMs - lastKeyguardPollMs < KEYGUARD_POLL_MS
            ) {
                return@runCatchingLogged
            }
            lastKeyguardPollMs = nowMs
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val locked = keyguardManager?.isKeyguardLocked == true
            if (isLockScreenActive != locked) {
                isLockScreenActive = locked
                if (locked) viewModel.closeWidgetIsland(snap = true)
                updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
            }

            var foregroundFromWindows = false
            if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            ) {
                // Our own window changing size (every expand and collapse) raises these too: not worth a window scan.
                val ownEvent = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event?.packageName?.toString() == packageName
                if (!ownEvent) {
                    // Bursts are coalesced (see refreshWindowsThrottled); a skipped event counts as "handled" so the
                    // opened-window fallback below does not override the trailing scan.
                    foregroundFromWindows = refreshWindowsThrottled(locked)
                }
            }

            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val openedPackage = event.packageName?.toString()
                if (!openedPackage.isNullOrEmpty() &&
                    openedPackage != packageName &&
                    openedPackage != "com.android.systemui" &&
                    !isInputMethodPackage(openedPackage)
                ) {
                    // Another app's window came up (an app switch, a back that landed in another screen, a dialog):
                    // the widget island closes.
                    viewModel.closeWidgetIsland(snap = false, graceMs = WIDGET_ISLAND_EVENT_GRACE_MS)
                    // A recorder's Save/Stop opened its app: go back so the user stays where they were.
                    if (com.agupta07505.smartisland.util.SilentActionGuard.consume(openedPackage)) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!destroyed) performGlobalAction(GLOBAL_ACTION_BACK)
                        }, SILENT_BACK_DELAY_MS)
                        return@runCatchingLogged
                    }
                    // The window list decides the foreground app; the opened window's package is only the fallback
                    // when the list cannot be read (a dialog or floating window must not replace the real app).
                    android.util.Log.i("SmartIslandExempt", "event opened=$openedPackage fromWindows=$foregroundFromWindows")
                    if (!foregroundFromWindows) viewModel.foregroundPackage.value = openedPackage
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                    // Only ordinary alerts are dropped; running timers, recorders, calls, downloads
                    // and navigation stay on the island (the repository enforces this).
                    notificationRepository.removeNotificationsForPackage(openedPackage)
                }
            }
        }
    }

    private fun refreshForegroundApp(
        locked: Boolean,
        windowList: List<android.view.accessibility.AccessibilityWindowInfo>
    ): Boolean {
        val appWindows = runCatchingLogged(TAG, "Reading windows for the foreground app failed") {
            windowList.map { w ->
                com.agupta07505.smartisland.util.AppWindow(
                    packageName = if (w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION) {
                        windowPackage(w)
                    } else null,
                    isApplicationType = w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION,
                    isActive = w.isActive,
                    isFocused = w.isFocused
                )
            }
        } ?: return false
        val ignoredPackage: (String) -> Boolean = { pkg ->
            pkg == packageName || pkg == com.agupta07505.smartisland.util.ShadeDetector.SYSTEM_UI || isInputMethodPackage(pkg)
        }
        var picked = com.agupta07505.smartisland.util.ForegroundApp.pick(appWindows, ignoredPackage)
        // Some apps (ChatGPT, Authenticator) hide their window from the accessibility window list, so there is no
        // application window at all and no window event with their package.
        var fromUsage = false
        if (picked == null && !locked && appWindows.none { it.isApplicationType }) {
            picked = usageForeground.current(ignoredPackage)
            fromUsage = picked != null
        }
        // The app window the user is in exists but its root (so its package) cannot be read yet: the caller falls
        // back to the package of the window event, instead of keeping the previous app (which left an exempt app not
        // recognised).
        if (picked == null && !locked &&
            appWindows.any { it.isApplicationType && it.packageName.isNullOrEmpty() && (it.isFocused || it.isActive) }
        ) return false
        val next = picked ?: if (locked) null else viewModel.foregroundPackage.value
        android.util.Log.i(
            "SmartIslandExempt",
            "scan picked=$picked usage=$fromUsage next=$next was=${viewModel.foregroundPackage.value} locked=$locked windows=" +
                appWindows.joinToString { "[${it.packageName} app=${it.isApplicationType} act=${it.isActive} foc=${it.isFocused}]" }
        )
        if (next != viewModel.foregroundPackage.value) {
            com.agupta07505.smartisland.util.AppLogRecorder.i(
                "Foreground",
                "app=$next (was ${viewModel.foregroundPackage.value}) windows=" + appWindows.joinToString {
                    "[pkg=${it.packageName} app=${it.isApplicationType} active=${it.isActive} focused=${it.isFocused}]"
                }
            )
            viewModel.foregroundPackage.value = next
            updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
        }
        return true
    }

    private var lastKeyguardPollMs = 0L
    private var lastWindowRefreshMs = 0L
    private var windowRefreshPending = false
    private val windowHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private val windowRefreshRunnable = Runnable {
        windowRefreshPending = false
        if (destroyed || !::viewModel.isInitialized) return@Runnable
        runCatchingLogged(TAG, "Trailing window refresh failed") {
            val locked = (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
            refreshWindowsNow(locked)
        }
    }

    private val windowPackageCache = HashMap<Int, String>()

    private val usageForeground by lazy { com.agupta07505.smartisland.util.UsageForegroundSource(applicationContext) }

    private fun windowPackage(w: android.view.accessibility.AccessibilityWindowInfo): String? {
        windowPackageCache[w.id]?.let { return it }
        val pkg = w.root?.packageName?.toString() ?: return null
        if (windowPackageCache.size >= WINDOW_PACKAGE_CACHE_MAX) windowPackageCache.clear()
        windowPackageCache[w.id] = pkg
        return pkg
    }

    private fun refreshWindowsThrottled(locked: Boolean): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val since = now - lastWindowRefreshMs
        if (since >= WINDOW_REFRESH_MIN_MS) {
            windowHandler.removeCallbacks(windowRefreshRunnable)
            windowRefreshPending = false
            return refreshWindowsNow(locked)
        }
        if (!windowRefreshPending) {
            windowRefreshPending = true
            windowHandler.postDelayed(windowRefreshRunnable, WINDOW_REFRESH_MIN_MS - since)
        }
        return true
    }

    private val settleScanRunnable = Runnable {
        if (destroyed || !::viewModel.isInitialized) return@Runnable
        runCatchingLogged(TAG, "Settle window scan failed") {
            val locked = (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
            refreshWindowsNow(locked, schedule = false)
        }
    }

    private fun scheduleSettleScans() {
        windowHandler.removeCallbacks(settleScanRunnable)
        windowHandler.postDelayed(settleScanRunnable, SETTLE_SCAN_FIRST_MS)
        windowHandler.postDelayed(settleScanRunnable, SETTLE_SCAN_SECOND_MS)
    }

    private fun refreshWindowsNow(locked: Boolean, schedule: Boolean = true): Boolean {
        lastWindowRefreshMs = android.os.SystemClock.elapsedRealtime()
        if (schedule) scheduleSettleScans()
        // One read of the window list for both checks.
        val list = runCatchingLogged(TAG, "Reading the window list failed") { windows.toList() } ?: return false
        refreshShadeState(locked, list)
        return refreshForegroundApp(locked, list)
    }

    private var shadeOpen = false
    private var shadeTitlesLocale: String? = null
    private var shadeTitlesCache: com.agupta07505.smartisland.util.ShadeTitles = com.agupta07505.smartisland.util.ShadeTitles.ENGLISH

    private fun refreshShadeState(
        locked: Boolean,
        windowList: List<android.view.accessibility.AccessibilityWindowInfo>
    ) {
        val windowsNow = runCatchingLogged(TAG, "Reading windows for the shade failed") {
            windowList.mapNotNull { w ->
                if (w.type != android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM) return@mapNotNull null
                val pkg = windowPackage(w)
                if (pkg != com.agupta07505.smartisland.util.ShadeDetector.SYSTEM_UI) return@mapNotNull null
                val bounds = android.graphics.Rect()
                w.getBoundsInScreen(bounds)
                com.agupta07505.smartisland.util.ShadeWindow(
                    packageName = pkg,
                    title = w.title?.toString(),
                    isSystemType = true,
                    isActive = w.isActive,
                    isFocused = w.isFocused,
                    top = bounds.top,
                    bottom = bounds.bottom
                )
            }
        } ?: return
        val open = com.agupta07505.smartisland.util.ShadeDetector.isShadeOpen(
            windows = windowsNow,
            displayHeight = resources.displayMetrics.heightPixels,
            keyguardLocked = locked,
            titles = shadeTitles()
        )
        if (open == shadeOpen) return
        shadeOpen = open
        // The window list is logged on every change so the detection can be tuned from the diagnostic log.
        com.agupta07505.smartisland.util.AppLogRecorder.i(
            "Shade",
            "open=$open locked=$locked systemUiWindows=" + windowsNow.joinToString {
                "[title=${it.title} active=${it.isActive} focused=${it.isFocused} top=${it.top} bottom=${it.bottom}]"
            }
        )
        viewModel.setShadeOpen(open)
    }

    private fun shadeTitles(): com.agupta07505.smartisland.util.ShadeTitles {
        val locale = java.util.Locale.getDefault().toLanguageTag()
        if (locale == shadeTitlesLocale) return shadeTitlesCache
        val systemUi = com.agupta07505.smartisland.util.ShadeDetector.SYSTEM_UI
        val res = runCatching { packageManager.getResourcesForApplication(systemUi) }.getOrNull()
        fun read(name: String): String? {
            if (res == null) return null
            val id = res.getIdentifier(name, "string", systemUi)
            if (id == 0) return null
            return runCatching { res.getString(id) }.getOrNull()?.trim { it == '.' || it.isWhitespace() }
                ?.takeIf { it.isNotBlank() }
        }
        val english = com.agupta07505.smartisland.util.ShadeTitles.ENGLISH
        val titles = com.agupta07505.smartisland.util.ShadeTitles(
            shade = listOfNotNull(
                read("accessibility_desc_notification_shade"),
                read("accessibility_desc_quick_settings")
            ) + english.shade,
            lockScreen = listOfNotNull(read("accessibility_desc_lock_screen")) + english.lockScreen
        )
        shadeTitlesLocale = locale
        shadeTitlesCache = titles
        return titles
    }

    private var inputMethodPackages: Set<String> = emptySet()
    private var inputMethodPackagesReadAt = 0L

    private fun isInputMethodPackage(pkg: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - inputMethodPackagesReadAt > INPUT_METHOD_CACHE_MS) {
            inputMethodPackagesReadAt = now
            inputMethodPackages = runCatchingLogged(TAG, "Reading input methods failed") {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
                    ?.enabledInputMethodList
                    ?.map { it.packageName }
                    ?.toSet()
            } ?: inputMethodPackages
        }
        return pkg in inputMethodPackages
    }

    /**
     * The back key (a hardware key or the 3-button bar) closes the widget island. The key still reaches the app: this
     * never consumes it.
     */
    override fun onKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_DOWN &&
            !destroyed && ::viewModel.isInitialized
        ) {
            viewModel.closeWidgetIsland(snap = false)
        }
        return false
    }

    override fun onInterrupt() {
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // The expanded island's "system" look follows the phone's dark mode.
        com.agupta07505.smartisland.ui.expanded.SystemNight.update(newConfig.uiMode)
        // Wrapped: a throw here would make Android disable the service automatically.
        runCatchingLogged(TAG, "onConfigurationChanged failed") {
            if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
            // Only a real turn hides the island (font size, dark mode and locale changes also land here).
            val turned = newConfig.orientation != lastConfigOrientation
            lastConfigOrientation = newConfig.orientation
            if (turned) beginRotationVeil()
            refreshOrientation()
            updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
            // The same settling delay applies after a configuration change.
            scheduleOrientationRefresh()
        }
    }

    // Turning the phone from landscape to reverse landscape keeps the orientation "landscape", so
    // Android may not send a configuration change. Watching the display rotation itself catches it.
    private var displayListener: android.hardware.display.DisplayManager.DisplayListener? = null
    private var lastRotation: Int = -1
    private var lastConfigOrientation: Int = android.content.res.Configuration.ORIENTATION_UNDEFINED

    // Rotation veil (model/RotationVeil.kt): the island is hidden from the turn until the orientation has settled.
    private var rotationVeilActive = false
    private var rotationVeilStartedAt = 0L
    private var rotationVeilCapJob: kotlinx.coroutines.Job? = null

    private fun beginRotationVeil() {
        if (destroyed || !::viewModel.isInitialized) return
        // A turn closes the widget island at once (the pill it grew out of changes size and place).
        viewModel.closeWidgetIsland(snap = true)
        // "Remove animations": no fade, so nothing is hidden either.
        if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) return
        if (!rotationVeilActive) {
            rotationVeilActive = true
            rotationVeilStartedAt = android.os.SystemClock.elapsedRealtime()
            // Safety net: whatever the refresh does, the island is back after the maximum.
            rotationVeilCapJob?.cancel()
            rotationVeilCapJob = serviceScope.launch {
                kotlinx.coroutines.delay(com.agupta07505.smartisland.model.RotationVeil.MAX_HIDDEN_MS)
                endRotationVeil()
            }
        }
        viewModel.rotationVeil.value = true
    }

    private fun endRotationVeil() {
        rotationVeilCapJob?.cancel()
        rotationVeilCapJob = null
        if (!rotationVeilActive) return
        rotationVeilActive = false
        if (::viewModel.isInitialized) viewModel.rotationVeil.value = false
    }

    private fun registerRotationListener() {
        if (displayListener != null) return
        runCatchingLogged(TAG, "registerRotationListener failed") {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
            // Start from the real rotation, so the first display event (refresh rate, brightness) is not a turn.
            lastRotation = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation ?: -1
            val listener = object : android.hardware.display.DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}
                override fun onDisplayRemoved(displayId: Int) {}
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId != android.view.Display.DEFAULT_DISPLAY) return
                    runCatchingLogged(TAG, "rotation change failed") {
                        if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
                        val rotation = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation
                            ?: return@runCatchingLogged
                        if (rotation == lastRotation) return@runCatchingLogged
                        lastRotation = rotation
                        beginRotationVeil()
                        scheduleOrientationRefresh()
                    }
                }
            }
            dm.registerDisplayListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
            displayListener = listener
        }
    }

    private var orientationRefreshJob: kotlinx.coroutines.Job? = null

    private fun scheduleOrientationRefresh() {
        orientationRefreshJob?.cancel()
        orientationRefreshJob = serviceScope.launch {
            for (waitMs in com.agupta07505.smartisland.model.RotationVeil.REFRESH_GAPS_MS) {
                if (waitMs > 0L) kotlinx.coroutines.delay(waitMs)
                if (destroyed || !::viewModel.isInitialized) {
                    endRotationVeil()
                    return@launch
                }
                runCatchingLogged(TAG, "orientation refresh failed") {
                    refreshOrientation()
                    // Applied directly too, so the window never waits on the settings flow.
                    val raw = viewModel.orientation.value
                    val effective = viewModel.settingsSnapshot().forOrientation(
                        raw, hasContent = viewModel.visibleNotifications.value.isNotEmpty()
                    )
                    updateWindowLayoutParams(isWindowExpanded, effective)
                }
            }
            // The settled read is done: bring the island back at its final position.
            if (rotationVeilActive) {
                val hiddenFor = android.os.SystemClock.elapsedRealtime() - rotationVeilStartedAt
                kotlinx.coroutines.delay(com.agupta07505.smartisland.model.RotationVeil.liftDelayMs(hiddenFor))
                endRotationVeil()
            }
        }
    }

    private fun unregisterRotationListener() {
        orientationRefreshJob?.cancel()
        orientationRefreshJob = null
        endRotationVeil()
        val listener = displayListener ?: return
        displayListener = null
        runCatchingLogged(TAG, "unregisterRotationListener failed") {
            (getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
                .unregisterDisplayListener(listener)
        }
    }

    private fun refreshOrientation() {
        if (!::viewModel.isInitialized) return
        val isLandscape = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        viewModel.orientation.value = LandscapeCameraDetector.read(this, isLandscape)
    }

    override fun onCreate() {
        super.onCreate()
        destroyed = false
        // The system animator duration scale (Developer options / Remove animations), kept current while the overlay runs.
        com.agupta07505.smartisland.util.SystemAnimScale.start(this)

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
        lastConfigOrientation = resources.configuration.orientation
        refreshOrientation()
        registerRotationListener()
        
        systemEventReceiver = SystemEventReceiver(
            notificationRepository,
            settingsProvider = { viewModel.displaySettings.value },
            isBatteryCardExpanded = {
                viewModel.expanded.value &&
                    viewModel.currentShownNotification()?.mode == com.agupta07505.smartisland.model.IslandMode.Battery
            }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }
        
        // Android 13+ requires an explicit export flag. NOT_EXPORTED still receives system broadcasts and blocks
        // spoofed intents from other apps.
        runCatchingLogged(TAG, "registerReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(systemEventReceiver, filter)
            }
            systemEventReceiverRegistered = true
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isLockScreenActive = keyguardManager?.isKeyguardLocked == true

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

        connectivityMonitor = com.agupta07505.smartisland.util.ConnectivityEventMonitor(
            context = this,
            scope = serviceScope,
            repository = notificationRepository,
            settingsProvider = { viewModel.displaySettings.value }
        ).also { it.start() }

        wiredDeviceMonitor = com.agupta07505.smartisland.util.WiredDeviceEventMonitor(
            context = this,
            scope = serviceScope,
            repository = notificationRepository,
            settingsProvider = { viewModel.displaySettings.value }
        ).also { it.start() }

        // Last-resort transfer detection for apps whose own notification is blocked or missing.
        transferSpeedMonitor = com.agupta07505.smartisland.util.TransferSpeedMonitor(
            context = this,
            scope = serviceScope,
            repository = notificationRepository,
            enabled = {
                val s = viewModel.displaySettings.value
                s.enabled && s.detectTransfersBySpeed &&
                    (s.isEventEnabled(com.agupta07505.smartisland.model.IslandEvent.DOWNLOAD.id) ||
                        s.isEventEnabled(com.agupta07505.smartisland.model.IslandEvent.UPLOAD.id))
            }
        ).also { it.start() }

        faceUnlockWatcher = com.agupta07505.smartisland.util.FaceUnlockWatcher(
            serviceScope,
            onUnlock = { type, _ ->
                serviceScope.launch {
                    val power = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    faceUnlockCoordinator.syncState(
                        screenOn = power?.isInteractive != false,
                        locked = keyguard?.isKeyguardLocked == true
                    )
                    faceUnlockCoordinator.onUnlockType(type, android.os.SystemClock.elapsedRealtime())
                }
            },
            onBouncer = {
                serviceScope.launch { faceUnlockCoordinator.onBouncerShown() }
            },
            onAppLockRequired = {
                // "requireUnlock = true" is also logged when a locked app posts a notification, so it must not show
                // the scanner.
                com.agupta07505.smartisland.util.AppLogRecorder.i(
                    "FaceUnlock",
                    "app lock line seen: waiting for the real prompt before showing the scanner"
                )
            },
            onPromptDismissed = {
                serviceScope.launch { faceUnlockCoordinator.onAppPromptDismissed() }
            },
            onAppUnlocked = {
                serviceScope.launch { faceUnlockCoordinator.onAppUnlocked(android.os.SystemClock.elapsedRealtime()) }
            },
            onMiuiAppLock = { miuiEvent ->
                serviceScope.launch { handleMiuiAppLock(miuiEvent) }
            }
        ) { event, line ->
            val strong = com.agupta07505.smartisland.util.FaceUnlockLog.isStrongSuccess(line)
            val fromApp = com.agupta07505.smartisland.util.FaceUnlockLog.isAppScanStart(line)
            serviceScope.launch {
                if (fromApp || event == com.agupta07505.smartisland.util.FaceUnlockLogEvent.Start) {
                    // the screen-on broadcast trails the scan: read the real state
                    val power = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val interactive = power?.isInteractive != false
                    val keyguardLocked = keyguard?.isKeyguardLocked == true
                    faceUnlockCoordinator.syncState(screenOn = interactive, locked = keyguardLocked)
                    if (!fromApp) {
                        com.agupta07505.smartisland.util.AppLogRecorder.i(
                            "FaceUnlock",
                            "face start line: interactive=$interactive locked=$keyguardLocked"
                        )
                    }
                }
                faceUnlockCoordinator.onLog(event, android.os.SystemClock.elapsedRealtime(), strong, fromApp)
            }
        }
        faceUnlockCoordinator.syncState(
            screenOn = (getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isInteractive != false,
            locked = isLockScreenActive
        )

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Exempt-app visibility collector failed") {
                // The island's window is hidden (off) or dead (static: no touches, nothing on it) while an exempt app is in the foreground.
                viewModel.islandExemption.collectLatest { mode ->
                    // Plain logcat too: it ends up in the exported report even when "record logs" is off.
                    android.util.Log.i(
                        "SmartIslandExempt",
                        "mode=$mode foreground=${viewModel.foregroundPackage.value} off=${viewModel.settingsSnapshot().islandExemptApps}"
                    )
                    com.agupta07505.smartisland.util.AppLogRecorder.i(
                        "Exempt",
                        "mode=$mode foreground=${viewModel.foregroundPackage.value} " +
                            "off=${viewModel.settingsSnapshot().islandExemptApps} static=${viewModel.settingsSnapshot().islandExemptAppsStatic} " +
                            "offLandscape=${viewModel.settingsSnapshot().islandExemptAppsLandscape}"
                    )
                    if (destroyed || !viewModel.displaySettings.value.enabled) return@collectLatest
                    // Back from an exempt app: the rotation / app switch may have changed the geometry meanwhile.
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Face ID collector failed") {
                com.agupta07505.smartisland.util.FaceIdController.state.collectLatest {
                    if (destroyed || !viewModel.displaySettings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                repository.settings.collect { settings ->
                    if (destroyed) return@collect
                    com.agupta07505.smartisland.util.AppLogRecorder.applySetting(applicationContext, settings.recordLogs)
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
                    if (destroyed || !viewModel.displaySettings.value.enabled) {
                        return@collectLatest
                    }
                    collapseJob?.cancel()
                    if (expanded) {
                        isWindowExpanded = true
                        updateWindowLayoutParams(true, viewModel.displaySettings.value)
                    } else {
                        collapseJob = serviceScope.launch {
                            kotlinx.coroutines.delay(AUTO_COLLAPSE_DELAY_MS)
                            isWindowExpanded = false
                            updateWindowLayoutParams(false, viewModel.displaySettings.value)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Widget-island collector failed") {
                // The widget island is drawn in the full window and a tap outside it closes it, so the window grows with it. It keeps its
                // size for the island's closing animation, then goes back to the pill's row.
                viewModel.widgetIslandOpen.collectLatest { open ->
                    if (destroyed || !viewModel.displaySettings.value.enabled) return@collectLatest
                    widgetWindowJob?.cancel()
                    if (open) {
                        widgetWindowOpen = true
                        updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                        islandView?.requestLayout()
                    } else {
                        // Touches go through to the app again at once (the region is the pill's again), the window shrinks after the animation
                        // (at once after a snap close: nothing is animating).
                        islandView?.requestLayout()
                        widgetWindowJob = serviceScope.launch {
                            if (!viewModel.widgetIslandSnapPending) kotlinx.coroutines.delay(WIDGET_WINDOW_LINGER_MS)
                            widgetWindowOpen = false
                            updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Auto-expanded collector failed") {
                // The window flag and the touchable region depend on it: re-apply when it flips.
                viewModel.autoExpanded.collectLatest {
                    if (destroyed || !viewModel.displaySettings.value.enabled) return@collectLatest
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                    islandView?.requestLayout()
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Notifications-state collector failed") {
                viewModel.visibleNotifications.collectLatest {
                    if (destroyed || !viewModel.displaySettings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Effective-settings collector failed") {
                // Fires when the user edits a profile or the phone rotates and the other profile applies.
                viewModel.displaySettings.collectLatest { effective ->
                    configureFaceUnlock(effective)
                    if (destroyed || !effective.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, effective)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Landscape idle-dot collector failed") {
                // The dot is a separate touch target: the window height and touch region change when
                // it appears (a card arrives) or moves (rotation, circle settings, camera cutout).
                kotlinx.coroutines.flow.combine(
                    viewModel.idleDot,
                    viewModel.visibleNotifications.map { it.isNotEmpty() }.distinctUntilChanged(),
                    viewModel.landscapeWidgetPill
                ) { dot, hasCards, widgetPill -> Triple(dot, hasCards, widgetPill) }.collectLatest {
                    if (destroyed || !viewModel.displaySettings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Input-active collector failed") {
                viewModel.isInputActive.collectLatest {
                    if (destroyed || !viewModel.displaySettings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)
                }
            }
        }
    }

    override fun onServiceConnected() {
        com.agupta07505.smartisland.ui.expanded.SystemNight.update(resources.configuration.uiMode)
        super.onServiceConnected()
        isSystemConnected = true
        com.agupta07505.smartisland.util.SystemServiceRecovery.resetAccessibilityRefreshAttempt()
        installUtilityBridge()
        com.agupta07505.smartisland.util.InAppStopwatch.restore(this)
        com.agupta07505.smartisland.util.InAppTimer.restore(this)
        com.agupta07505.smartisland.util.AppLogRecorder.i("OverlayLifecycle", "onServiceConnected: accessibility overlay service bound (pid=${android.os.Process.myPid()})")
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
        com.agupta07505.smartisland.util.SystemServiceRecovery.resetAccessibilityRefreshAttempt()
        com.agupta07505.smartisland.util.UtilityBridge.clear()
        com.agupta07505.smartisland.util.AppLogRecorder.w("OverlayLifecycle", "onUnbind: accessibility overlay service unbound by the system")
        // Return true so Android system knows to re-bind the accessibility service automatically
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        runCatchingLogged(TAG, "onTaskRemoved recovery failed") {
            if (!destroyed &&
                ::viewModel.isInitialized &&
                viewModel.displaySettings.value.enabled
            ) {
                ensureForegroundStarted()
                ensureCollapsedWindow()
            }
        }
    }

    override fun onDestroy() {
        if (destroyed) return
        com.agupta07505.smartisland.util.AppLogRecorder.w("OverlayLifecycle", "onDestroy: overlay service destroyed (pid=${android.os.Process.myPid()})")
        destroyed = true
        isSystemConnected = false
        com.agupta07505.smartisland.util.UtilityBridge.clear()
        com.agupta07505.smartisland.util.SystemAnimScale.stop(this)
        unregisterRotationListener()
        serviceScope.cancel()
        windowHandler.removeCallbacks(windowRefreshRunnable)
        windowHandler.removeCallbacks(settleScanRunnable)
        windowRefreshPending = false
        windowPackageCache.clear()

        if (::systemEventReceiver.isInitialized) {
            runCatchingLogged(TAG, "SystemEventReceiver release failed") { systemEventReceiver.release() }
        }
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

        connectivityMonitor?.stop()
        connectivityMonitor = null
        wiredDeviceMonitor?.stop()
        wiredDeviceMonitor = null
        transferSpeedMonitor?.stop()
        transferSpeedMonitor = null
        faceUnlockWatcher?.stop()
        faceUnlockWatcher = null

        removeCollapsedWindow()
        stopForegroundSafely()
        runCatchingLogged(TAG, "Overlay owners destroy failed") {
            overlayOwners.destroy()
        }
        super.onDestroy()
    }

    private fun startOverlaySession(settings: SmartIslandSettings) {
        if (destroyed || !::windowManager.isInitialized || !::viewModel.isInitialized) return
        ensureForegroundStarted()
        ensureCollapsedWindow()
        // `settings` is the raw stored profile; resolve it for the current orientation synchronously.
        updateWindowLayoutParams(
            isWindowExpanded,
            settings.forOrientation(viewModel.orientation.value)
        )
        // The listener's own import runs when it connects, which can be before this overlay exists: ask it to import
        // again.
        if (!shadeSyncRequested) {
            shadeSyncRequested = true
            notificationRepository.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.SyncShade)
        }
    }

    private var shadeSyncRequested = false

    private fun stopOverlaySession() {
        shadeSyncRequested = false
        removeCollapsedWindow()
        stopForegroundSafely()
        if (::viewModel.isInitialized) {
            viewModel.collapse()
        }
    }

    private fun ensureForegroundStarted() {
        if (foregroundStarted || destroyed) return
        runCatchingLogged(TAG, "startForeground failed") {
            startForeground(NOTIFICATION_ID, buildNotification())
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

    @SuppressLint("ClickableViewAccessibility")
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
                val isHidden = (!viewModel.displaySettings.value.showOnLockScreen && isLocked) || (isLandscape && !viewModel.displaySettings.value.showInLandscape) ||
                    viewModel.exemptionNow() == com.agupta07505.smartisland.model.ExemptMode.Off
                visibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE

                installOverlayViewTreeOwners()
                isFocusable = true
                isFocusableInTouchMode = true
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    val fullWidth by isTouchableRegionSupported.collectAsState()
                    val fontSettings by viewModel.settings.collectAsState()
                    val islandFont = remember(
                        fontSettings.islandFont, fontSettings.fontMixEnabled,
                        fontSettings.fontMixLight, fontSettings.fontMixRegular, fontSettings.fontMixMedium
                    ) {
                        // One font, or up to three mixed by weight (Settings > Island font > Mix fonts).
                        com.agupta07505.smartisland.ui.islandTextFamily(this@SmartIslandOverlayService, fontSettings)
                    }
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.agupta07505.smartisland.ui.expanded.LocalReorderPinnedApps provides { order ->
                            serviceScope.launch { repository.setShortcutPackages(order) }
                        }
                    ) {
                    ProvideTextStyle(TextStyle(fontFamily = islandFont)) {
                        OverlayIsland(
                            viewModel = this@SmartIslandOverlayService.viewModel,
                            statusBarHeight = statusBarHeight,
                            onOpenNotification = { notification -> openNotification(notification) },
                            onLaunchApp = { packageName -> launchApp(packageName) },
                            onAppAction = { packageName, action -> launchAppAction(packageName, action) },
                            onOpenFloatingWindow = { openCurrentNotificationInFloatingWindow() },
                            onOpenNotificationShade = {
                                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                                viewModel.collapse()
                            },
                            isFullWidth = fullWidth
                        )
                    }
                    }
                }

                // A touch outside the island's region while it is auto-expanded and untouched: close it. The touch itself
                // went to the app below.
                setOnTouchListener { _, event ->
                    if (event.actionMasked == android.view.MotionEvent.ACTION_OUTSIDE &&
                        viewModel.autoExpanded.value
                    ) {
                        viewModel.collapseAfterOutsideTouch()
                    }
                    false
                }

                setupTouchableRegion(this)
            }
            runCatchingLogged(TAG, "windowManager.addView failed") {
                windowManager.addView(islandView, collapsedParams(viewModel.displaySettings.value))
            } ?: run {
                islandView = null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "ensureCollapsedWindow fatal", e)
            islandView = null
        }
    }

    // Reflection on the hidden OnComputeInternalInsetsListener lets the overlay window pass touches outside the pill
    // through.
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
            
            val TOUCHABLE_INSETS_FRAME = 0
            val TOUCHABLE_INSETS_REGION = 3
            
            // equals/hashCode/toString must be answered too: returning null for a primitive
            // result makes the proxy throw when the framework compares or hashes the listener.
            val proxyHolder = arrayOfNulls<Any>(1)
            val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "equals" && args != null && args.size == 1) {
                    return@newProxyInstance args[0] === proxyHolder[0]
                }
                if (method.name == "hashCode") {
                    return@newProxyInstance System.identityHashCode(proxyHolder[0])
                }
                if (method.name == "toString") {
                    return@newProxyInstance "SmartIslandInsetsListener"
                }
                if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                    val insets = args[0]
                    val isExpanded = viewModel.expanded.value
                    // Gone, or a static exempt-app pill: no touch is caught, they all reach the app below.
                    val isGone = view.visibility == android.view.View.GONE ||
                        viewModel.exemptionNow() == com.agupta07505.smartisland.model.ExemptMode.Static
                    val settingsVal = viewModel.displaySettings.value
                    val notificationsCount = viewModel.visibleNotifications.value.size
                    val isIdleHidden = settingsVal.hideWhenIdle && notificationsCount == 0 && !settingsVal.enableAppShortcuts
                    if (isGone || isIdleHidden) {
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        val region = touchableRegionField.get(insets) as android.graphics.Region
                        region.setEmpty()
                    } else if (isExpanded) {
                        val surface = viewModel.expandedSurfaceBounds.value
                        if (viewModel.autoExpanded.value && surface != null && !viewModel.isInputActive.value) {
                            // Opened by itself and not touched yet: only the island catches touches.
                            setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                            val pad = (8 * resources.displayMetrics.density).toInt()
                            val region = touchableRegionField.get(insets) as android.graphics.Region
                            region.set(
                                (surface.left - pad).coerceAtLeast(0), (surface.top - pad).coerceAtLeast(0),
                                surface.right + pad, surface.bottom + pad
                            )
                            // The collapsed cutouts (the secondary one beside the pill) are separate from the card, so they sit
                            // outside its bounds: without this a tap on them went to the app below and counted as an outside touch.
                            region.union(
                                collapsedGroupTouchRect(
                                    settingsVal,
                                    resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                ).rect
                            )
                        } else {
                            // Used by the user: the whole frame intercepts touches, so tapping outside collapses it
                            setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                        }
                    } else if (viewModel.widgetIslandOpen.value) {
                        // The semi-expanded widget island is open: the whole frame intercepts touches, so a tap outside closes it.
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                    } else {
                        // The region is local to this already-offset window: touches outside the visible collapsed group pass through to the system.
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        
                        val density = resources.displayMetrics.density
                        val screenWidth = resources.displayMetrics.widthPixels
                        val isLandscapeNow = resources.configuration.orientation ==
                            android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        val group = collapsedGroupTouchRect(settingsVal, isLandscapeNow)
                        val left = group.rect.left
                        val top = group.rect.top
                        val right = group.rect.right
                        val bottom = group.rect.bottom
                        val groupStartPx = group.startPx
                        val groupEndPx = group.endPx
                        val pillHeightPx = group.heightPx
                        
                        val region = touchableRegionField.get(insets) as android.graphics.Region
                        region.set(left, top, right, bottom)
                        // Portrait: a long press on the camera cutout (when the pill's centre band does not reach it)
                        // hides / shows the pill.
                        cutoutTouchRect(settingsVal, isLandscapeNow, bottom)?.let { region.union(it) }
                        // Landscape idle dot (separate tap target while a card is showing).
                        idleDotRectPx(
                            settingsVal, notificationsCount > 0 || viewModel.landscapeWidgetPill.value, groupStartPx, groupEndPx, pillHeightPx, density, screenWidth,
                            isLandscape = isLandscapeNow
                        )?.let { region.union(it) }
                    }
                }
                null
            }
            proxyHolder[0] = proxyListener

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
                        updateWindowLayoutParams(false, viewModel.displaySettings.value)
                    }
                }
            }
            
            // Register on the live ViewTreeObserver of the attached window (it changes on attach).
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

    private var cachedCutoutKey = -1
    private var cachedCutout: com.agupta07505.smartisland.util.DetectedCutoutInfo? = null

    private fun detectedCutout(): com.agupta07505.smartisland.util.DetectedCutoutInfo {
        val key = resources.displayMetrics.widthPixels
        cachedCutout?.let { if (key == cachedCutoutKey) return it }
        return com.agupta07505.smartisland.util.CameraCutoutDetector.detect(this).also {
            cachedCutout = it
            cachedCutoutKey = key
        }
    }

    private fun cutoutTouchRect(settingsVal: SmartIslandSettings, isLandscapeNow: Boolean, windowHeightPx: Int): android.graphics.Rect? {
        if (!isTouchableRegionSupported.value) return null
        val area = com.agupta07505.smartisland.util.CameraCutoutDetector.areaFor(settingsVal, detectedCutout(), isLandscapeNow) ?: return null
        val overlaps = com.agupta07505.smartisland.model.centreBandOverlapsCutout(
            bandCentreXDp = settingsVal.xOffset,
            bandWidthDp = settingsVal.width * com.agupta07505.smartisland.model.PILL_CENTER_ZONE_SHARE,
            bandTopDp = settingsVal.yOffset,
            bandHeightDp = settingsVal.height,
            cutoutCentreXDp = area.centreXDp,
            cutoutWidthDp = area.widthDp,
            cutoutTopDp = area.topDp,
            cutoutHeightDp = area.heightDp
        )
        if (overlaps) return null
        val target = com.agupta07505.smartisland.model.cutoutTouchTarget(area)
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val left = (screenWidth / 2f + (target.centreXDp - target.widthDp / 2f) * density).toInt().coerceAtLeast(0)
        val right = (screenWidth / 2f + (target.centreXDp + target.widthDp / 2f) * density).toInt().coerceAtMost(screenWidth)
        val top = ((target.topDp - settingsVal.yOffset) * density).toInt().coerceAtLeast(0)
        val bottom = (((target.topDp + target.heightDp) - settingsVal.yOffset) * density).toInt().coerceAtMost(windowHeightPx)
        return if (right > left && bottom > top) android.graphics.Rect(left, top, right, bottom) else null
    }

    private class CollapsedGroupTouch(
        val rect: android.graphics.Rect,
        val startPx: Float,
        val endPx: Float,
        val heightPx: Float
    )

    private fun collapsedGroupTouchRect(
        settingsVal: SmartIslandSettings,
        isLandscapeNow: Boolean
    ): CollapsedGroupTouch {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val isSplitMode = com.agupta07505.smartisland.model.hasSecondaryCutout(
            com.agupta07505.smartisland.model.cutoutCardCount(
                viewModel.visibleNotifications.value, expanded = false
            ),
            settingsVal.enableNotchMode
        )
        val isCircleLeft = (settingsVal.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT)
        val mainWidthPx = settingsVal.width * density
        val circleSizePx = com.agupta07505.smartisland.data.collapsedCompanionSizeDp(settingsVal) * density
        val compactGapPx = 8f * density
        val edgePaddingPx = 8f * density
        val touchPaddingXPx = 16f * density
        val topShiftPx = com.agupta07505.smartisland.ui.collapsedTopShiftDp(
            isLandscapeNow, settingsVal.enableNotchMode, settingsVal.yOffset
        ) * density
        val pillHeightPx = (settingsVal.height + 16f) * density + topShiftPx
        val groupWidthPx = mainWidthPx + if (isSplitMode) compactGapPx + circleSizePx else 0f
        val desiredMainLeftPx = screenWidth / 2f + settingsVal.xOffset * density - mainWidthPx / 2f
        val (minMainLeftPx, maxMainLeftPx) = when {
            !isSplitMode -> edgePaddingPx to (screenWidth - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx)
            isCircleLeft -> (edgePaddingPx + circleSizePx + compactGapPx) to
                (screenWidth - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx + circleSizePx + compactGapPx)
            else -> edgePaddingPx to (screenWidth - edgePaddingPx - groupWidthPx).coerceAtLeast(edgePaddingPx)
        }
        val mainLeftPx = desiredMainLeftPx.coerceIn(minMainLeftPx, maxMainLeftPx)
        val groupStartPx = if (isCircleLeft && isSplitMode) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx
        val groupEndPx = if (!isCircleLeft && isSplitMode) mainLeftPx + mainWidthPx + compactGapPx + circleSizePx else mainLeftPx + mainWidthPx
        return CollapsedGroupTouch(
            rect = android.graphics.Rect(
                (groupStartPx - touchPaddingXPx).toInt().coerceAtLeast(0),
                topShiftPx.toInt(),
                (groupEndPx + touchPaddingXPx).toInt().coerceAtMost(screenWidth),
                pillHeightPx.toInt()
            ),
            startPx = groupStartPx,
            endPx = groupEndPx,
            heightPx = pillHeightPx
        )
    }

    private fun idleDotRectPx(
        settings: SmartIslandSettings,
        hasCards: Boolean,
        groupStartPx: Float,
        groupEndPx: Float,
        groupBottomPx: Float,
        density: Float,
        screenWidthPx: Int,
        isLandscape: Boolean
    ): android.graphics.Rect? {
        val dot = viewModel.idleDot.value ?: return null
        // This rectangle serves the collapsed window only, and only landscape has a dot (portrait uses the pill's centre).
        val shown = com.agupta07505.smartisland.model.shouldShowIdleDot(dot = dot, hasCards = hasCards, isLandscape = isLandscape)
        if (!shown) return null
        val slackPx = 8f * density
        val centerPx = screenWidthPx / 2f + dot.centerXDp * density
        val halfPx = dot.sizeDp * density / 2f
        return android.graphics.Rect(
            (centerPx - halfPx - slackPx).toInt().coerceAtLeast(0),
            (dot.topDp * density - slackPx).toInt().coerceAtLeast(0),
            (centerPx + halfPx + slackPx).toInt().coerceAtMost(screenWidthPx),
            ((dot.topDp + dot.sizeDp) * density + slackPx).toInt()
        )
    }

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        if (destroyed || !::windowManager.isInitialized || !::viewModel.isInitialized) return
        val view = islandView ?: return
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels.toFloat()
        
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked == true
        isLockScreenActive = isLocked
        viewModel.isLocked.value = isLocked
        
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val visibleCount = viewModel.visibleNotifications.value.size
        val isHidden = (!settings.showOnLockScreen && isLocked) || (isLandscape && !settings.showInLandscape) ||
            viewModel.exemptionNow() == com.agupta07505.smartisland.model.ExemptMode.Off
        // Static exempt app (portrait): the collapsed pill stays where it is, but takes no touches and shows nothing.
        val isStatic = !isHidden && viewModel.exemptionNow() == com.agupta07505.smartisland.model.ExemptMode.Static

        val targetVisibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE
        if (view.visibility != targetVisibility) {
            view.visibility = targetVisibility
        }

        val isSplitMode = !isStatic && com.agupta07505.smartisland.model.hasSecondaryCutout(
            com.agupta07505.smartisland.model.cutoutCardCount(
                viewModel.visibleNotifications.value, viewModel.expanded.value,
                viewModel.selectedIndex.value
            ),
            settings.enableNotchMode
        )
        val isCircleLeft = (settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT)
        val mainWidthPx = settings.width * density
        val circleSizePx = com.agupta07505.smartisland.data.collapsedCompanionSizeDp(settings) * density
        val compactGapPx = 8f * density
        // Landscape: the cutout may touch the screen edge, the camera sits right at it.
        val edgePaddingPx = if (isLandscape && !settings.enableNotchMode) 0f else 8f * density
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

        // Landscape: the window starts at the screen top and the collapsed cutouts are lowered
        // inside it (see collapsedTopShiftDp), so the expanded island never inherits a
        // mid-screen camera offset.
        val topShiftDp = com.agupta07505.smartisland.ui.collapsedTopShiftDp(
            isLandscape, settings.enableNotchMode, settings.yOffset
        )
        val dotRect = idleDotRectPx(
            settings, visibleCount > 0 || viewModel.landscapeWidgetPill.value, groupStartPx, groupEndPx,
            (settings.height + 16f + topShiftDp) * density, density, screenWidthPx.toInt(),
            isLandscape = isLandscape
        )
        // While a card is on the island the window is already full height when collapsed (the touchable region keeps every touch
        // outside the pill going to the app below, as it does for the full width). Growing the window on the tap took a frame or
        // two, and the opening card was clipped to the small collapsed window before the jump to its real height.
        val preGrown = isTouchableRegionSupported.value && visibleCount > 0 && !isHidden && !isStatic
        // the square scan is taller than the collapsed window
        val faceIdState = com.agupta07505.smartisland.util.FaceIdController.state.value
        val fullWindow = expanded || widgetWindowOpen ||
            (faceIdState.active && faceIdState.style != com.agupta07505.smartisland.util.FaceIdController.Style.Cutout)
        val h = if (fullWindow || preGrown) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            maxOf(
                ((settings.height + 16f + topShiftDp) * density).toInt(),
                dotRect?.bottom ?: 0
            )
        }
        val w = if (fullWindow || isTouchableRegionSupported.value) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            (groupWidthPx + 32f * density).toInt()
        }
        val isInput = viewModel.isInputActive.value && expanded
        val focusFlags = if (isInput) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // Auto-expanded and untouched: ask to hear touches outside the island's region (they reach the app below).
        val watchOutside = if (expanded && !isInput && isTouchableRegionSupported.value && viewModel.autoExpanded.value) {
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else 0
        // A static pill is never touched: the window ignores touches altogether (this also covers devices where the touchable
        // region cannot be set and the window is as wide as the pill).
        val noTouch = if (isStatic) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0
        val currentFlags = focusFlags or watchOutside or noTouch or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        val currentX = if (fullWindow || isTouchableRegionSupported.value) 0 else windowXPx
        val currentY = if (settings.enableNotchMode || isLandscape) 0 else settings.yOffset.dpToPx()
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
            lp.x == currentX &&
            lp.y == currentY &&
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = currentX
            y = currentY
            softInputMode = currentSoftInputMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        lastParams = params
        runCatchingLogged(TAG, "Failed to update view layout") { 
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

    private fun collapsedParams(settings: SmartIslandSettings): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels.toFloat()
        val isLandscapeNow = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val topShiftDp = com.agupta07505.smartisland.ui.collapsedTopShiftDp(
            isLandscapeNow, settings.enableNotchMode, settings.yOffset
        )
        val isSplitMode = ::viewModel.isInitialized && com.agupta07505.smartisland.model.hasSecondaryCutout(
            com.agupta07505.smartisland.model.cutoutCardCount(
                viewModel.visibleNotifications.value, viewModel.expanded.value,
                viewModel.selectedIndex.value
            ),
            settings.enableNotchMode
        )
        val isCircleLeft = (settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT)
        val mainWidthPx = settings.width * density
        val circleSizePx = com.agupta07505.smartisland.data.collapsedCompanionSizeDp(settings) * density
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
        val currentFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        return WindowManager.LayoutParams(
            w,
            ((settings.height + 16f + topShiftDp) * density).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            currentFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (isTouchableRegionSupported.value) 0 else windowXPx
            y = if (settings.enableNotchMode || isLandscapeNow) 0 else settings.yOffset.dpToPx()
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
                            Toast.makeText(this, "Can't open ${notification.appName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        // Drops the app's ordinary alerts only; ongoing cards (timer, recorder, call, ...) stay.
        notificationRepository.removeNotificationsForPackage(notification.packageName)
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

    private fun installUtilityBridge() {
        val bridge = com.agupta07505.smartisland.util.UtilityBridge
        bridge.collapse = {
            windowHandler.post { if (!destroyed && ::viewModel.isInitialized) viewModel.collapse() }
        }
        bridge.globalAction = { action -> runCatchingLogged(TAG, "Global action $action failed") { performGlobalAction(action) } ?: false }
        bridge.openIntentFloating = { intent ->
            val settings = viewModel.settings.value
            when (com.agupta07505.smartisland.model.floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)) {
                com.agupta07505.smartisland.model.FloatingWindowRoute.Off -> false
                com.agupta07505.smartisland.model.FloatingWindowRoute.System -> {
                    runCatchingLogged(TAG, "Failed to open a page in a floating window") {
                        startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), systemFreeformBundle())
                        true
                    } ?: false
                }
                // LMO Freeform opens an activity by package and class, so the page's intent is resolved to its
                // activity first.
                com.agupta07505.smartisland.model.FloatingWindowRoute.Lmo -> {
                    val lmo = com.agupta07505.smartisland.util.LmoFreeform
                    val component = if (lmo.isInstalled(this)) {
                        intent.component ?: runCatching {
                            packageManager.resolveActivity(intent, 0)?.activityInfo?.let { android.content.ComponentName(it.packageName, it.name) }
                        }.getOrNull()
                    } else null
                    if (component == null) {
                        false
                    } else {
                        runCatchingLogged(TAG, "Failed to ask LMO Freeform for a page") {
                            sendBroadcast(lmo.startIntent(component))
                            true
                        } ?: false
                    }
                }
            }
        }
        bridge.openFloating = { packageName ->
            val settings = viewModel.settings.value
            val route = com.agupta07505.smartisland.model.floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)
            if (route == com.agupta07505.smartisland.model.FloatingWindowRoute.Off) {
                false
            } else {
                openAppInFloatingWindow(packageName)
                true
            }
        }
    }

    private fun launchAppAction(packageName: String, action: com.agupta07505.smartisland.model.AppShortcutAction) {
        when (action) {
            com.agupta07505.smartisland.model.AppShortcutAction.AppInfo -> openAppInfo(packageName)
            com.agupta07505.smartisland.model.AppShortcutAction.FloatingWindow -> openAppInFloatingWindow(packageName)
            com.agupta07505.smartisland.model.AppShortcutAction.SplitScreen -> openAppInSplitScreen(packageName)
        }
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatchingLogged(TAG, "Failed to open app info") { startActivity(intent) }
        viewModel.collapse()
    }

    private fun openAppInFloatingWindow(packageName: String) {
        val settings = viewModel.settings.value
        val launched = when (com.agupta07505.smartisland.model.floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)) {
            com.agupta07505.smartisland.model.FloatingWindowRoute.Off -> {
                Toast.makeText(this, getString(R.string.shortcut_floating_window_off), Toast.LENGTH_SHORT).show()
                false
            }
            com.agupta07505.smartisland.model.FloatingWindowRoute.Lmo -> openPackageInLmoFreeform(packageName)
            com.agupta07505.smartisland.model.FloatingWindowRoute.System -> openPackageInSystemFreeform(packageName)
        }
        if (launched) notificationRepository.removeNotificationsForPackage(packageName)
        viewModel.collapse()
    }

    private fun openPackageInSystemFreeform(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(this, "App is no longer available", Toast.LENGTH_SHORT).show()
            return false
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val started = runCatchingLogged(TAG, "Failed to launch app in a floating window") {
            startActivity(launchIntent, systemFreeformBundle())
        } != null
        return started
    }

    private fun openPackageInLmoFreeform(packageName: String): Boolean {
        val lmo = com.agupta07505.smartisland.util.LmoFreeform
        if (!lmo.isInstalled(this)) {
            Toast.makeText(this, "LMO Freeform is not installed on this device.", Toast.LENGTH_SHORT).show()
            return false
        }
        val component = lmo.launcherComponent(packageManager, packageName)
        if (component == null) {
            Toast.makeText(this, "App is no longer available", Toast.LENGTH_SHORT).show()
            return false
        }
        val sent = runCatchingLogged(TAG, "Failed to ask LMO Freeform for a window") {
            sendBroadcast(lmo.startIntent(component))
        } != null
        return sent
    }

    private fun openAppInSplitScreen(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(this, "App is no longer available", Toast.LENGTH_SHORT).show()
            viewModel.collapse()
            return
        }
        val plan = com.agupta07505.smartisland.model.splitScreenPlan(viewModel.foregroundPackage.value, homePackageNames(), packageName)
        viewModel.collapse()
        when (plan) {
            com.agupta07505.smartisland.model.SplitPlan.PairWithForeground -> {
                // The open app goes to one half first; the chosen app is then started next to it once the system has made room.
                if (!performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)) {
                    Toast.makeText(this, getString(R.string.shortcut_split_unavailable), Toast.LENGTH_SHORT).show()
                    return
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                windowHandler.postDelayed({
                    runCatchingLogged(TAG, "Failed to launch app next to the open one") { startActivity(launchIntent) }
                }, SPLIT_LAUNCH_DELAY_MS)
            }
            com.agupta07505.smartisland.model.SplitPlan.LaunchThenSystemPicker -> {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatchingLogged(TAG, "Failed to launch app for split screen") { startActivity(launchIntent) }
                // Once the app is in front, Android splits the screen with it and shows its own app selection for the other half.
                windowHandler.postDelayed({
                    if (!performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)) {
                        Toast.makeText(this, getString(R.string.shortcut_split_unavailable), Toast.LENGTH_SHORT).show()
                    }
                }, SPLIT_PICKER_DELAY_MS)
            }
        }
        notificationRepository.removeNotificationsForPackage(packageName)
    }

    private fun homePackageNames(): Set<String> = runCatchingLogged(TAG, "Failed to list home apps") {
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
    } ?: emptySet()

    private fun openCurrentNotificationInFloatingWindow() {
        // Floating window off in settings: the handle is already gone, and the Floating window swipe action does nothing.
        val route = com.agupta07505.smartisland.model.floatingWindowRoute(
            viewModel.settings.value.floatingWindowEnabled,
            viewModel.settings.value.floatingWindowMode
        )
        if (route == com.agupta07505.smartisland.model.FloatingWindowRoute.Off) return
        // The card on screen: selectedIndex indexes the island's own list (filtered, priority-ordered), not the raw repository list.
        val notification = viewModel.currentShownNotification()
        if (notification != null) {
            val launched = when (route) {
                com.agupta07505.smartisland.model.FloatingWindowRoute.Lmo -> openInLmoFreeform(notification)
                else -> openInSystemFreeform(notification)
            }
            // Only an ordinary alert is consumed by opening it. Cancelling a running timer,
            // recorder, call or download notification would kill its buttons.
            if (launched && notification.mode == com.agupta07505.smartisland.model.IslandMode.Notification) {
                notificationRepository.removeNotification(notification.key)
                notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
            }
        }
        viewModel.collapse()
    }

    private fun systemFreeformBundle(): android.os.Bundle {
        val options = ActivityOptions.makeBasic()
        runCatchingLogged(TAG, "Failed to set launch bounds") {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
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
        return bundle
    }

    private fun openInSystemFreeform(notification: IslandNotification): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(this, "Floating window requires Android 7+.", Toast.LENGTH_SHORT).show()
            return false
        }
        val bundle = systemFreeformBundle()

        val fillInIntent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        var started = false
        if (notification.contentIntent != null) {
            runCatchingLogged(TAG, "Failed to send content intent") {
                notification.contentIntent.send(this, 0, fillInIntent, null, null, null, bundle)
                started = true
            }
        } else {
            runCatchingLogged(TAG, "Failed to launch package activity") {
                val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    startActivity(launchIntent, bundle)
                    started = true
                } else {
                    Toast.makeText(this, "Can't open ${notification.appName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return started
    }

    private fun openInLmoFreeform(notification: IslandNotification): Boolean {
        val lmo = com.agupta07505.smartisland.util.LmoFreeform
        if (!lmo.isInstalled(this)) {
            Toast.makeText(this, "LMO Freeform is not installed on this device.", Toast.LENGTH_SHORT).show()
            return false
        }
        val pending = notification.contentIntent
        val canMoveTask = pending != null && com.agupta07505.smartisland.util.ShizukuManager.hasPermission()
        if (!canMoveTask) return openInLmoFallback(notification)
        // Task snapshot first, so the chat's task can be told apart from tasks the app already had.
        serviceScope.launch {
            // Preferred: open the chat straight on LMO's own display, so it never covers the foreground app.
            if (openInLmoDisplay(notification, pending!!)) return@launch
            val beforeOut = com.agupta07505.smartisland.util.ShizukuManager.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
            val beforeTasks = lmo.parseTasks(beforeOut)
            val before = beforeTasks.map { it.taskId }.toSet()
            val sent = runCatchingLogged(TAG, "Failed to open the notification's chat") {
                val fill = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    }.toBundle()
                } else null
                pending!!.send(this@SmartIslandOverlayService, 0, fill, null, null, null, options)
            } != null
            if (!sent) {
                openInLmoFallback(notification)
                return@launch
            }
            // The chat is on screen full size from here until LMO takes the task, so find the new task as fast
            // as possible: poll back to back (no fixed 250 ms wait first) for up to LMO_TASK_POLL_WINDOW_MS.
            var task: com.agupta07505.smartisland.util.LmoFreeform.TaskInfo? = null
            var pollsUsed = 0
            val pollDeadline = android.os.SystemClock.elapsedRealtime() + LMO_TASK_POLL_WINDOW_MS
            while (task == null && (pollsUsed == 0 || android.os.SystemClock.elapsedRealtime() < pollDeadline)) {
                pollsUsed++
                val after = lmo.parseTasks(
                    com.agupta07505.smartisland.util.ShizukuManager.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
                )
                task = lmo.pickTask(after, before, notification.packageName)
                    ?.takeIf { it.taskId !in before }
                if (task == null) kotlinx.coroutines.delay(LMO_TASK_POLL_MS)
            }
            // No new task appeared: the app reused one it had, so take its front-most task.
            if (task == null) {
                val reusedOut = com.agupta07505.smartisland.util.ShizukuManager.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
                task = lmo.pickTask(lmo.parseTasks(reusedOut), before, notification.packageName)
            }
            val found = task
            if (found == null) {
                Log.w(TAG, "LMO: no task found for ${notification.packageName}; falling back to launching it in LMO")
                openInLmoFallback(notification)
            } else {
                runCatchingLogged(TAG, "Failed to ask LMO Freeform to move task ${found.taskId}") {
                    sendBroadcast(lmo.startIntent(found.component, found.userId, found.taskId))
                }
            }
        }
        return true
    }

    private suspend fun openInLmoDisplay(notification: IslandNotification, pending: PendingIntent): Boolean {
        val lmo = com.agupta07505.smartisland.util.LmoFreeform
        val shizuku = com.agupta07505.smartisland.util.ShizukuManager
        val pkg = notification.packageName
        val component = lmo.pendingIntentActivity(pending) ?: lmo.launcherComponent(packageManager, pkg)
        if (component == null) {
            return false
        }
        val knownDisplays = lmo.parseDisplayIds(shizuku.runShellForOutput(lmo.LIST_DISPLAYS_COMMAND).getOrDefault(""), pkg)
        val before = lmo.parseTasks(shizuku.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault(""))
            .map { it.taskId }.toSet()
        runCatchingLogged(TAG, "Failed to ask LMO Freeform for a window") {
            sendBroadcast(lmo.startIntent(component))
        } ?: return false
        var displayId: Int? = null
        val deadline = android.os.SystemClock.elapsedRealtime() + LMO_DISPLAY_WAIT_MS
        while (displayId == null && android.os.SystemClock.elapsedRealtime() < deadline) {
            val ids = lmo.parseDisplayIds(shizuku.runShellForOutput(lmo.LIST_DISPLAYS_COMMAND).getOrDefault(""), pkg)
            displayId = (ids - knownDisplays).maxOrNull()
            if (displayId == null) kotlinx.coroutines.delay(LMO_TASK_POLL_MS)
        }
        if (displayId == null) {
            return false
        }
        val options = ActivityOptions.makeBasic().apply {
            setLaunchDisplayId(displayId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
        }.toBundle()
        val sent = runCatchingLogged(TAG, "Failed to send the chat to LMO's display") {
            pending.send(this, 0, Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null, null, null, options)
        } != null
        if (!sent) {
            return true // the window exists; opening the chat again on display 0 would only cover the foreground app
        }
        // Did Android honour the display? A new task of this app on display 0 means it did not.
        kotlinx.coroutines.delay(500)
        val out = shizuku.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
        val displays = lmo.taskDisplays(out)
        val tasks = lmo.parseTasks(out).filter { it.packageName == pkg }
        val misplaced = tasks.filter { it.taskId !in before && displays[it.taskId] == 0 }.maxByOrNull { it.taskId }
        if (misplaced != null) {
            runCatchingLogged(TAG, "Failed to move task ${misplaced.taskId}") {
                sendBroadcast(lmo.startIntent(misplaced.component, misplaced.userId, misplaced.taskId))
            }
        }
        return true
    }

    private fun openInLmoFallback(notification: IslandNotification): Boolean {
        val lmo = com.agupta07505.smartisland.util.LmoFreeform
        val component = notification.contentIntent?.let { lmo.pendingIntentActivity(it) }
            ?: lmo.launcherComponent(packageManager, notification.packageName)
        if (component == null) {
            Toast.makeText(this, "Can't open ${notification.appName}", Toast.LENGTH_SHORT).show()
            return false
        }
        return runCatchingLogged(TAG, "Failed to ask LMO Freeform for a window") {
            sendBroadcast(lmo.startIntent(component))
        } != null
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun ComposeView.installOverlayViewTreeOwners() {
        setViewTreeLifecycleOwner(overlayOwners)
        setViewTreeViewModelStoreOwner(overlayOwners)
        setViewTreeSavedStateRegistryOwner(overlayOwners)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                OVERLAY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Smart Island overlay running"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    companion object {
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        private const val TAG = "SmartIslandOverlayService"
        private const val LMO_TASK_POLL_WINDOW_MS = 3_000L
        private const val LMO_TASK_POLL_MS = 30L
        private const val LMO_DISPLAY_WAIT_MS = 2_000L
        private const val NOTIFICATION_ID = 8105
        private const val SILENT_BACK_DELAY_MS = 300L
        private const val WINDOWING_MODE_FREEFORM = 5
        private const val SPLIT_LAUNCH_DELAY_MS = 450L
        private const val SPLIT_PICKER_DELAY_MS = 600L
        private const val OVERLAY_CHANNEL_ID = "smart_island_overlay"
        private const val OVERLAY_CHANNEL_NAME = "Smart Island overlay"
        private const val AUTO_COLLAPSE_DELAY_MS = 750L
        private const val WIDGET_WINDOW_LINGER_MS = 700L
        private const val WIDGET_ISLAND_EVENT_GRACE_MS = 500L
        private const val INPUT_METHOD_CACHE_MS = 60_000L
        private const val KEYGUARD_POLL_MS = 1_000L
        private const val WINDOW_REFRESH_MIN_MS = 120L
        private const val SETTLE_SCAN_FIRST_MS = 400L
        private const val SETTLE_SCAN_SECOND_MS = 1_100L
        private const val WINDOW_PACKAGE_CACHE_MAX = 96
    }
}
