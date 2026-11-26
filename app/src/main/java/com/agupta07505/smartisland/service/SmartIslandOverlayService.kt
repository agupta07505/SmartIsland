/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.data.collapsedCompanionSizeDp
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.model.BackStepRules
import com.agupta07505.smartisland.model.ExemptMode
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.cutoutCardCount
import com.agupta07505.smartisland.model.hasSecondaryCutout
import com.agupta07505.smartisland.ui.CollapsedGroupSpan
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.ui.collapsedGroupSpan
import com.agupta07505.smartisland.ui.collapsedTopShiftDp
import com.agupta07505.smartisland.ui.expanded.SystemNight
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.AppWindow
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.ForegroundApp
import com.agupta07505.smartisland.util.FullscreenProbe
import com.agupta07505.smartisland.util.FullscreenProbeRunner
import com.agupta07505.smartisland.util.HotspotSettingsAutomation
import com.agupta07505.smartisland.util.InAppStopwatch
import com.agupta07505.smartisland.util.InAppTimer
import com.agupta07505.smartisland.util.ShadeDetector
import com.agupta07505.smartisland.util.SilentActionGuard
import com.agupta07505.smartisland.util.StatusBarReapplier
import com.agupta07505.smartisland.util.SystemAnimScale
import com.agupta07505.smartisland.util.TorchController
import com.agupta07505.smartisland.util.UsageForegroundSource
import com.agupta07505.smartisland.util.UtilityBridge
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartIslandOverlayService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    private var islandView: OverlayComposeView? = null
    private var fullscreenProbe: FullscreenProbeRunner? = null
    private var fullscreenHide: OverlayFullscreenHide? = null
    private val overlayOwners = OverlayViewTreeOwners()
    private lateinit var systemEventReceiver: SystemEventReceiver
    private val viewModelHolder = OverlayViewModelHolder()
    private val viewModel: IslandViewModel get() = viewModelHolder.get()
    private var isLockScreenActive: Boolean = false
    private var systemEventReceiverRegistered = false
    private var screenStateReceiverRegistered = false
    private val foreground = OverlayForeground(this) { destroyed }
    private val isTouchableRegionSupported = MutableStateFlow(false)
    @Volatile private var destroyed = false
    private var isWindowExpanded: Boolean = false
    private var widgetWindowOpen: Boolean = false
    private var widgetWindowJob: Job? = null
    private var collapseJob: Job? = null
    private var lastParams: WindowManager.LayoutParams? = null

    private var eventMonitors: OverlayEventMonitors? = null

    private val serviceScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, error ->
                AppLogRecorder.e(TAG, "unhandled overlay coroutine failure", error)
            }
    )

    // lazy: the repositories are injected in onCreate, a field initializer would read them before that
    private val torch by lazy {
        OverlayTorchController(
            context = this,
            scope = serviceScope,
            repository = repository,
            notificationRepository = notificationRepository,
            viewModelHolder = viewModelHolder,
            isDestroyed = { destroyed },
        )
    }

    private val faceUnlock = OverlayFaceUnlockController(
        context = this,
        scope = serviceScope,
        viewModelHolder = viewModelHolder,
    )

    private val statusBarReapplier by lazy {
        StatusBarReapplier(
            scope = serviceScope,
            interactive = { (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive != false }
        )
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatchingLogged(TAG, "Screen-state callback failed") {
                if (destroyed || !viewModelHolder.ready) return@runCatchingLogged
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        overlayOwners.resume()
                        isLockScreenActive = isKeyguardLocked()
                        faceUnlock.onScreenOn(isLockScreenActive)
                        statusBarReapplier.trigger("screen on")
                        statusBarReapplier.startWatcher()
                        applyWindowLayout()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        overlayOwners.pause()
                        viewModel.widgetIsland.close(snap = true)
                        statusBarReapplier.stopWatcher()
                        // The shade closes with the screen; its window event may come late
                        shade.shadeOpen = false
                        viewModel.setShadeOpen(false)
                        isLockScreenActive = true
                        faceUnlock.onScreenOff()
                        applyWindowLayout()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        isLockScreenActive = false
                        faceUnlock.onUserPresent()
                        statusBarReapplier.trigger("unlock")
                        applyWindowLayout()
                    }
                }
            }
        }
    }

    // Fallback sync: check if keyguard locked state changed on window changes
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        runCatchingLogged(TAG, "onAccessibilityEvent failed") {
            if (destroyed || !viewModelHolder.ready) return@runCatchingLogged
            val eventType = event?.eventType
            val nowMs = SystemClock.elapsedRealtime()
            if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            ) {
                // the status bar window comes and goes with the window list
                fullscreenHide?.refresh()
            }
            if ((eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) &&
                event?.packageName?.toString()?.contains("settings", ignoreCase = true) == true
            ) {
                HotspotSettingsAutomation.onWindow(rootInActiveWindow, nowMs)
            }
            if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                nowMs - lastKeyguardPollMs < KEYGUARD_POLL_MS
            ) {
                return@runCatchingLogged
            }
            lastKeyguardPollMs = nowMs
            val locked = isKeyguardLocked()
            if (isLockScreenActive != locked) {
                isLockScreenActive = locked
                if (locked) viewModel.widgetIsland.close(snap = true)
                applyWindowLayout()
            }

            var foregroundFromWindows = false
            if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            ) {
                // Our own window changing size (every expand and collapse) raises these too
                val ownEvent = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event?.packageName?.toString() == packageName
                if (!ownEvent) {
                    // Bursts are coalesced (see refreshWindowsThrottled)
                    foregroundFromWindows = refreshWindowsThrottled(locked)
                }
            }

            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val openedPackage = event.packageName?.toString()
                if (!openedPackage.isNullOrEmpty() &&
                    openedPackage != packageName &&
                    openedPackage != "com.android.systemui" &&
                    !inputMethods.isInputMethodPackage(openedPackage)
                ) {
                    // another app's window came up
                    viewModel.widgetIsland.close(snap = false, graceMs = WIDGET_ISLAND_EVENT_GRACE_MS)
                    // a recorder's Save/Stop opened its app
                    if (SilentActionGuard.consume(openedPackage)) {
                        windowHandler.postDelayed({
                            if (!destroyed) performGlobalAction(GLOBAL_ACTION_BACK)
                        }, SILENT_BACK_DELAY_MS)
                        return@runCatchingLogged
                    }
                    if (!foregroundFromWindows) viewModel.foregroundPackage.value = openedPackage
                    applyWindowLayout()
                    // Dialogs also trigger window changes, so leave notification dismissal to explicit actions.
                }
            }
        }
    }

    private fun refreshForegroundApp(
        locked: Boolean,
        windowList: List<AccessibilityWindowInfo>
    ): Boolean {
        imeTopPx = runCatchingLogged(TAG, "Reading the keyboard window failed") {
            val bounds = Rect()
            windowList.filter { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                .minOfOrNull { w -> w.getBoundsInScreen(bounds); bounds.top }
        } ?: Int.MAX_VALUE
        val appWindows = runCatchingLogged(TAG, "Reading windows for the foreground app failed") {
            windowList.map { w ->
                AppWindow(
                    packageName = if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        windowPackage(w)
                    } else null,
                    isApplicationType = w.type == AccessibilityWindowInfo.TYPE_APPLICATION,
                    isActive = w.isActive,
                    isFocused = w.isFocused
                )
            }
        } ?: return false
        val ignoredPackage: (String) -> Boolean = { pkg ->
            pkg == packageName || pkg == ShadeDetector.SYSTEM_UI || inputMethods.isInputMethodPackage(pkg)
        }
        var picked = ForegroundApp.pick(appWindows, ignoredPackage)
        var fromUsage = false
        if (picked == null && !locked && appWindows.none { it.isApplicationType }) {
            picked = usageForeground.current(ignoredPackage)
            fromUsage = picked != null
        }
        if (picked == null && !locked &&
            appWindows.any { it.isApplicationType && it.packageName.isNullOrEmpty() && (it.isFocused || it.isActive) }
        ) return false
        val next = picked ?: if (locked) null else viewModel.foregroundPackage.value
        if (next != viewModel.foregroundPackage.value) {
            viewModel.foregroundPackage.value = next
            applyWindowLayout()
        }
        return true
    }

    /** Top of the keyboard window, Int.MAX_VALUE with none: touches on it are typing, not a tap away from the island. */
    @Volatile private var imeTopPx = Int.MAX_VALUE
    private var lastKeyguardPollMs = 0L
    private var lastWindowRefreshMs = 0L
    private var windowRefreshPending = false
    private val windowHandler by lazy { Handler(Looper.getMainLooper()) }
    private val geometry: OverlayTouchGeometry by lazy {
        OverlayTouchGeometry(
            service = this,
            viewModelHolder = viewModelHolder,
            rotation = rotation,
            touchRegionSupported = { isTouchableRegionSupported.value },
        )
    }
    private val launcher by lazy {
        OverlayAppLauncher(
            service = this,
            notificationRepository = notificationRepository,
            serviceScope = serviceScope,
            windowHandler = windowHandler,
            viewModelHolder = viewModelHolder,
            isReady = { !destroyed && viewModelHolder.ready },
        )
    }
    private val windowRefreshRunnable = Runnable {
        windowRefreshPending = false
        if (destroyed || !viewModelHolder.ready) return@Runnable
        runCatchingLogged(TAG, "Trailing window refresh failed") {
            val locked = isKeyguardLocked()
            refreshWindowsNow(locked)
        }
    }

    private val windowPackageCache = HashMap<Int, String>()

    private val usageForeground by lazy { UsageForegroundSource(applicationContext) }

    private fun windowPackage(w: AccessibilityWindowInfo): String? {
        windowPackageCache[w.id]?.let { return it }
        val pkg = w.root?.packageName?.toString() ?: return null
        if (windowPackageCache.size >= WINDOW_PACKAGE_CACHE_MAX) windowPackageCache.clear()
        windowPackageCache[w.id] = pkg
        return pkg
    }

    private fun refreshWindowsThrottled(locked: Boolean): Boolean {
        val now = SystemClock.elapsedRealtime()
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
        if (destroyed || !viewModelHolder.ready) return@Runnable
        runCatchingLogged(TAG, "Settle window scan failed") {
            val locked = isKeyguardLocked()
            refreshWindowsNow(locked, schedule = false)
        }
    }

    private fun scheduleSettleScans() {
        windowHandler.removeCallbacks(settleScanRunnable)
        windowHandler.postDelayed(settleScanRunnable, SETTLE_SCAN_FIRST_MS)
        windowHandler.postDelayed(settleScanRunnable, SETTLE_SCAN_SECOND_MS)
    }

    private fun refreshWindowsNow(locked: Boolean, schedule: Boolean = true): Boolean {
        lastWindowRefreshMs = SystemClock.elapsedRealtime()
        if (schedule) scheduleSettleScans()
        // 1 read of the window list for both checks
        val list = runCatchingLogged(TAG, "Reading the window list failed") { windows.toList() } ?: return false
        shade.refreshShadeState(locked, list)
        return refreshForegroundApp(locked, list)
    }

    private val shade = OverlayShadeTracker(
        service = this,
        viewModel = { viewModel },
        windowPackage = { windowPackage(it) },
    )
    private val inputMethods = InputMethodPackageCache(this)

    /** The press of Back the expanded island took, so its repeats and its release do not reach the app below. */
    private var backKeyTaken = false

    /**
     * Back key (a hardware key or the 3-button bar). It closes the widget island, and an expanded island that owns
     * Back (one the user opened, held with the white pill or with a selected text) takes it: a selection first, then
     * the card's steps, then the island itself. The back gesture reaches the focused overlay window instead
     * ([OverlayComposeView])
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode != KeyEvent.KEYCODE_BACK || destroyed || !viewModelHolder.ready) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return backKeyTaken
                viewModel.widgetIsland.close(snap = false)
                backKeyTaken = viewModel.handleBack()
                return backKeyTaken
            }
            KeyEvent.ACTION_UP -> {
                val taken = backKeyTaken
                backKeyTaken = false
                return taken
            }
        }
        return false
    }

    override fun onInterrupt() {
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        SystemNight.update(newConfig.uiMode)
        // wrapped: a throw here would make Android disable the service automatically
        runCatchingLogged(TAG, "onConfigurationChanged failed") {
            if (destroyed || !viewModelHolder.ready) return@runCatchingLogged
            // Only a real turn hides the island
            val turned = newConfig.orientation != rotation.lastConfigOrientation
            rotation.lastConfigOrientation = newConfig.orientation
            geometry.invalidateCutout()
            statusBarReapplier.trigger("configuration change")
            if (turned) {
                // the window list has no status bar for a moment while the screen turns
                fullscreenHide?.holdForRotation()
                rotation.beginRotationVeil(quarterTurn = true)
            }
            rotation.refreshOrientation()
            applyWindowLayout()
            // The same settling delay applies after a configuration change.
            rotation.scheduleOrientationRefresh()
        }
    }

    private val rotation: OverlayRotationController = OverlayRotationController(
        service = this,
        scope = serviceScope,
        viewModelHolder = viewModelHolder,
        isDestroyed = { destroyed },
        invalidateCutout = { geometry.invalidateCutout() },
        applyWindowParams = { updateWindowLayoutParams(isWindowExpanded, it) },
    )

    override fun onCreate() {
        super.onCreate()
        destroyed = false
        // the system animator duration scale (Developer options / Remove animations)
        SystemAnimScale.start(this)

        runCatchingLogged(TAG, "createNotificationChannel failed") {
            foreground.createChannel()
        }

        val resolvedWindowManager = runCatchingLogged(TAG, "WindowManager initialization failed") {
            getSystemService(WindowManager::class.java)
        }
        if (resolvedWindowManager == null) {
            AppLogRecorder.e(TAG, "WindowManager unavailable, overlay cannot start")
            return
        }
        windowManager = resolvedWindowManager

        // Developer options test of fullscreen detection, off until the switch is turned on
        val probe = FullscreenProbeRunner(this, resolvedWindowManager) { islandView }
        fullscreenProbe = probe
        serviceScope.launch {
            FullscreenProbe.enabled.collect { on -> if (on) probe.start() else probe.stop() }
        }

        val initializedViewModel = runCatchingLogged(TAG, "Overlay ViewModel initialization failed") {
            // Lifecycle must be restored before the service-owned ViewModel is created
            overlayOwners.resume()
            ViewModelProvider(
                overlayOwners,
                IslandViewModel.provideFactory(repository, notificationRepository)
            )[IslandViewModel::class.java]
        }
        if (initializedViewModel == null) {
            AppLogRecorder.e(TAG, "overlay ViewModel unavailable, overlay cannot start")
            return
        }
        viewModelHolder.set(initializedViewModel)
        rotation.lastConfigOrientation = resources.configuration.orientation
        rotation.refreshOrientation()
        rotation.registerRotationListener()
        registerSystemReceivers()
        torch.start()
        startEventMonitors()
        faceUnlock.start(isLockScreenActive)
        startStateCollectors()
        startLayoutRefreshCollectors()
        startFullscreenHide(resolvedWindowManager)
    }

    private fun startFullscreenHide(wm: WindowManager) {
        val hide = OverlayFullscreenHide(
            service = this,
            windowManager = wm,
            scope = serviceScope,
            viewModel = viewModel,
            isDestroyed = { destroyed },
            isLockScreenActive = { isLockScreenActive },
            relayout = {
                applyWindowLayout()
                islandView?.requestLayout()
            }
        )
        fullscreenHide = hide
        hide.start()
    }

    private fun registerSystemReceivers() {
        systemEventReceiver = SystemEventReceiver(
            notificationRepository,
            settingsProvider = { viewModel.displaySettings.value },
            isBatteryCardExpanded = {
                viewModel.expanded.value &&
                    viewModel.currentShownNotification()?.mode == IslandMode.Battery
            }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }

        // Android 13+ requires an explicit export flag
        runCatchingLogged(TAG, "registerReceiver failed") {
            registerNotExported(systemEventReceiver, filter)
            systemEventReceiverRegistered = true
        }

        isLockScreenActive = isKeyguardLocked()

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatchingLogged(TAG, "registerReceiver screenStateReceiver failed") {
            registerNotExported(screenStateReceiver, screenFilter)
            screenStateReceiverRegistered = true
        }
    }

    /** Not exported where the flag exists (Android 13+); older versions only have the plain call. */
    private fun registerNotExported(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    private fun startEventMonitors() {
        eventMonitors = OverlayEventMonitors(
            context = this,
            scope = serviceScope,
            repository = notificationRepository,
            settings = { viewModel.displaySettings.value },
            shadeOpen = viewModel.shadeOpen
        ).also { it.start() }
    }

    private fun startStateCollectors() {
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Exempt-app visibility collector failed") {
                // The island's window is hidden (off) or dead
                viewModel.islandExemption.collectLatest { mode ->
                    AppLogRecorder.i(
                        "Exempt",
                        "mode=$mode foreground=${viewModel.foregroundPackage.value} " +
                            "off=${viewModel.settingsSnapshot().islandExemptApps} static=${viewModel.settingsSnapshot().islandExemptAppsStatic} " +
                            "offLandscape=${viewModel.settingsSnapshot().islandExemptAppsLandscape}"
                    )
                    if (!islandActive()) return@collectLatest
                    applyWindowLayout()
                }
            }
        }

        launchRelayoutOn("Face ID collector failed", FaceIdController.state)

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                repository.settings.collect { settings ->
                    if (destroyed) return@collect
                    AppLogRecorder.applySetting(applicationContext, settings.recordLogs)
                    // Independent of the island switch: the hide has its own setting
                    statusBarReapplier.onSettings(settings.statusBarHidden, settings.statusBarPlan())
                    updateOverlaySession(settings.enabled)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Cutout placement collector failed") {
                viewModel.settings.map {
                    listOf(it.cameraCutoutStyle, it.cameraCutoutX, it.cameraCutoutY, it.cameraCutoutWidth,
                        it.cameraCutoutHeight)
                }.distinctUntilChanged().collect {
                    if (destroyed) return@collect
                    rotation.refreshOrientation()
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded-state collector failed") {
                viewModel.expanded.collectLatest { expanded ->
                    if (!islandActive()) return@collectLatest
                    collapseJob?.cancel()
                    if (expanded) {
                        isWindowExpanded = true
                        updateWindowLayoutParams(true, viewModel.displaySettings.value)
                    } else {
                        collapseJob = serviceScope.launch {
                            delay(AUTO_COLLAPSE_DELAY_MS)
                            isWindowExpanded = false
                            updateWindowLayoutParams(false, viewModel.displaySettings.value)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Widget-island collector failed") {
                // The widget island is drawn in the full window and a tap outside it closes it
                viewModel.widgetIsland.open.collectLatest { open ->
                    if (!islandActive()) return@collectLatest
                    widgetWindowJob?.cancel()
                    if (open) {
                        widgetWindowOpen = true
                        applyWindowLayout()
                        islandView?.requestLayout()
                    } else {
                        // Touches go through to the app again at once (the region is the pill's again)
                        islandView?.requestLayout()
                        widgetWindowJob = serviceScope.launch {
                            if (!viewModel.widgetIsland.snapPending) delay(WIDGET_WINDOW_LINGER_MS)
                            widgetWindowOpen = false
                            applyWindowLayout()
                        }
                    }
                }
            }
        }
    }

    /** Collectors that only re-apply the window layout when their state changes. */
    private fun startLayoutRefreshCollectors() {
        launchRelayoutOn("Shade visibility collector failed", viewModel.shadeOpen)
        launchRelayoutOn("Rotation-morph collector failed", viewModel.rotationMorphActive, requestLayout = true)
        // The window flag and the touchable region depend on it: re-apply when it flips
        launchRelayoutOn("Auto-expanded collector failed", viewModel.autoExpanded, requestLayout = true)
        launchRelayoutOn(
            "Hold and selection collector failed",
            combine(viewModel.autoCollapsePinnedState, viewModel.textSelecting) { held, selecting -> held || selecting }
                .distinctUntilChanged()
        )
        launchRelayoutOn("Pill-hidden collector failed", viewModel.pillHidden)
        launchRelayoutOn("Notifications-state collector failed", viewModel.visibleNotifications)

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Effective-settings collector failed") {
                viewModel.displaySettings.collectLatest { effective ->
                    faceUnlock.configure(effective)
                    if (destroyed || !effective.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, effective)
                }
            }
        }

        // The dot is a separate touch target
        launchRelayoutOn(
            "Landscape idle-dot collector failed",
            combine(
                viewModel.idleDot,
                viewModel.visibleNotifications.map { it.isNotEmpty() }.distinctUntilChanged(),
                viewModel.landscapeWidgetPill
            ) { dot, hasCards, widgetPill -> Triple(dot, hasCards, widgetPill) }
        )

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Back-owner collector failed") {
                viewModel.expanded
                    .collectLatest { open ->
                        if (!islandActive()) return@collectLatest
                        applyWindowLayout()
                        if (open) islandView?.post { islandView?.requestFocus() }
                    }
            }
        }

        launchRelayoutOn("Input-active collector failed", viewModel.isInputActive)
    }

    /** Re-applies the window layout each time [trigger] changes, while the island is on. */
    private fun launchRelayoutOn(failureLabel: String, trigger: Flow<*>, requestLayout: Boolean = false) {
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, failureLabel) {
                trigger.collectLatest {
                    if (!islandActive()) return@collectLatest
                    applyWindowLayout()
                    if (requestLayout) islandView?.requestLayout()
                }
            }
        }
    }

    private fun islandActive(): Boolean = !destroyed && viewModel.displaySettings.value.enabled

    private fun applyWindowLayout() = updateWindowLayoutParams(isWindowExpanded, viewModel.displaySettings.value)

    private fun isKeyguardLocked(): Boolean =
        (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    private fun landscapeNow(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onServiceConnected() {
        SystemNight.update(resources.configuration.uiMode)
        super.onServiceConnected()
        isSystemConnected = true
        SystemServiceRecovery.resetAccessibilityRefreshAttempt()
        launcher.installUtilityBridge()
        InAppStopwatch.restore(this)
        InAppTimer.restore(this)
        AppLogRecorder.i("OverlayLifecycle", "overlay service bound (pid=${Process.myPid()})")
        if (destroyed || !viewModelHolder.ready) return
        // SystemUI may have restarted while the service was down.
        statusBarReapplier.trigger("service connected")
        statusBarReapplier.startWatcher()
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Service reconnect failed") {
                val settings = repository.settings.first()
                updateOverlaySession(settings.enabled)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isSystemConnected = false
        fullscreenProbe?.stop()
        fullscreenHide?.stop()
        SystemServiceRecovery.resetAccessibilityRefreshAttempt()
        UtilityBridge.clear()
        AppLogRecorder.w("OverlayLifecycle", "overlay service unbound by the system")
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        runCatchingLogged(TAG, "onTaskRemoved recovery failed") {
            if (!destroyed &&
                viewModelHolder.ready &&
                viewModel.displaySettings.value.enabled
            ) {
                foreground.ensureStarted()
                ensureCollapsedWindow()
            }
        }
    }

    override fun onDestroy() {
        if (destroyed) return
        AppLogRecorder.w("OverlayLifecycle", "overlay service destroyed (pid=${Process.myPid()})")
        destroyed = true
        isSystemConnected = false
        UtilityBridge.clear()
        SystemAnimScale.stop(this)
        fullscreenProbe?.stop()
        fullscreenHide?.stop()
        rotation.unregisterRotationListener()
        statusBarReapplier.stopWatcher()
        StatusBarReapplier.clearShared()
        TorchController.stopPattern()
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
        torch.stop()

        eventMonitors?.stop()
        eventMonitors = null
        faceUnlock.stop()

        removeCollapsedWindow()
        foreground.stopSafely()
        runCatchingLogged(TAG, "Overlay owners destroy failed") {
            overlayOwners.destroy()
        }
        super.onDestroy()
    }

    private fun updateOverlaySession(enabled: Boolean) {
        if (enabled) startOverlaySession() else stopOverlaySession()
    }

    private fun startOverlaySession() {
        if (destroyed || !::windowManager.isInitialized || !viewModelHolder.ready) return
        foreground.ensureStarted()
        ensureCollapsedWindow()
        updateWindowLayoutParams(
            isWindowExpanded,
            viewModel.settingsSnapshot().forOrientation(viewModel.orientation.value)
        )
        // The listener's own import runs when it connects
        if (!shadeSyncRequested) {
            shadeSyncRequested = true
            notificationRepository.sendCommand(SmartIslandCommand.SyncShade)
        }
    }

    private var shadeSyncRequested = false

    private fun stopOverlaySession() {
        shadeSyncRequested = false
        removeCollapsedWindow()
        foreground.stopSafely()
        if (viewModelHolder.ready) {
            viewModel.collapse()
        }
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
            !viewModelHolder.ready
        ) return
        try {
            islandView = OverlayComposeView(this) {
                // Back while the window has focus (the back gesture), see onKeyEvent for the key of a 3-button bar
                !destroyed && viewModelHolder.ready && viewModel.handleBack()
            }.apply {
                val isLocked = isKeyguardLocked()
                isLockScreenActive = isLocked
                val isLandscape = landscapeNow()
                val isHidden = hideOverlay(viewModel.displaySettings.value, isLocked, isLandscape)
                visibility = if (isHidden) View.GONE else View.VISIBLE

                installOverlayViewTreeOwners()
                isFocusable = true
                isFocusableInTouchMode = true
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    OverlayIslandContent(
                        context = this@SmartIslandOverlayService,
                        viewModel = viewModel,
                        isTouchableRegionSupported = isTouchableRegionSupported,
                        statusBarHeight = { statusBarHeight },
                        launcher = launcher,
                        onReorderPinnedApps = { order ->
                            serviceScope.launch { repository.setShortcutPackages(order) }
                        },
                        onOpenNotificationShade = {
                            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                            viewModel.collapse()
                        },
                    )
                }

                // hidden by a fullscreen app: only the cutout is touchable, and a tap there shows the island. The view
                // takes these touches itself, the content would answer them as taps on the island otherwise
                hiddenNow = { viewModel.fullscreenHidden.value }
                hiddenTouch = { event -> fullscreenHide?.onHiddenTouch(this, event) }

                // a touch outside the expanded island's region, it also reaches the app below
                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_OUTSIDE &&
                        // Leave island text selected while typing in the app below.
                        event.rawY < imeTopPx
                    ) {
                        if (!viewModel.autoExpanded.value) viewModel.clearTextSelection()
                    }
                    false
                }

                touchableRegion.setupTouchableRegion(this)
            }
            runCatchingLogged(TAG, "windowManager.addView failed") {
                windowManager.addView(islandView, collapsedParams(viewModel.displaySettings.value))
            } ?: run {
                islandView = null
            }
        } catch (e: Exception) {
            // TODO(errors): addView below is already wrapped in runCatchingLogged, so only the view setup above lands here
            AppLogRecorder.e(TAG, "ensureCollapsedWindow failed", e)
            islandView = null
        }
    }

    private val touchableRegion by lazy {
        OverlayTouchableRegion(
            service = this,
            viewModelHolder = viewModelHolder,
            geometry = geometry,
            rotation = rotation,
            isTouchableRegionSupported = isTouchableRegionSupported,
            onListenerRegistered = {
                if (viewModelHolder.ready && !isWindowExpanded) {
                    updateWindowLayoutParams(false, viewModel.displaySettings.value)
                }
            },
        )
    }

    private class CollapsedGroup(val span: CollapsedGroupSpan, val groupWidthPx: Float, val windowXPx: Int)

    /** Where the collapsed pill, with the companion circle in split mode, sits across the screen. */
    private fun collapsedGroup(
        settings: SmartIslandSettings,
        isLandscape: Boolean,
        screenWidthPx: Float,
        density: Float,
        isSplitMode: Boolean
    ): CollapsedGroup {
        val isCircleLeft = geometry.circleLeftNow(settings, isLandscape)
        val mainWidthPx = settings.width * density
        val circleSizePx = collapsedCompanionSizeDp(settings) * density
        val compactGapPx = 8f * density
        // landscape: the cutout may touch the screen edge, the camera sits right at it
        val edgePaddingPx = if (settings.idleCutout || (isLandscape && !settings.enableNotchMode)) 0f else 8f * density
        val groupWidthPx = mainWidthPx + if (isSplitMode) compactGapPx + circleSizePx else 0f

        val span = collapsedGroupSpan(
            geometry.placedMainLeftPx(settings, isLandscape, screenWidthPx, density, mainWidthPx),
            screenWidthPx, edgePaddingPx, mainWidthPx, circleSizePx, compactGapPx, isSplitMode, isCircleLeft,
            if (settings.enableNotchMode) settings.xOffset * density else null
        )
        return CollapsedGroup(span, groupWidthPx, span.windowXPx(screenWidthPx))
    }

    private fun overlayLayoutParams(
        width: Int,
        height: Int,
        flags: Int,
        x: Int,
        y: Int,
        softInputMode: Int = 0
    ): WindowManager.LayoutParams = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        this.x = x
        this.y = y
        this.softInputMode = softInputMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // Hide the window itself so the cutout and any closing card disappear together.
    private fun hideOverlay(settings: SmartIslandSettings, locked: Boolean, landscape: Boolean): Boolean =
        (settings.hideInShade && viewModel.shadeOpen.value) || (locked && !settings.showOnLockScreen) ||
            (landscape && !settings.showInLandscape) || viewModel.exemptionNow() == ExemptMode.Off

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        if (destroyed || !::windowManager.isInitialized || !viewModelHolder.ready) return
        val view = islandView ?: return
        val density = resources.displayMetrics.density
        val screenWidthPx = rotation.screenWidthPxNow().toFloat()

        val isLocked = isKeyguardLocked()
        isLockScreenActive = isLocked
        viewModel.isLocked.value = isLocked

        val isLandscape = landscapeNow()
        val visibleCount = viewModel.visibleNotifications.value.size
        val isHidden = hideOverlay(settings, isLocked, isLandscape)
        val isStatic = !isHidden && viewModel.exemptionNow() == ExemptMode.Static

        val targetVisibility = if (isHidden) View.GONE else View.VISIBLE
        if (view.visibility != targetVisibility) {
            view.visibility = targetVisibility
        }

        val isSplitMode = !isStatic && (hasSecondaryCutout(
            cutoutCardCount(
                viewModel.visibleNotifications.value, viewModel.expanded.value,
                viewModel.selectedIndex.value
            )
        ))
        val group = collapsedGroup(settings, isLandscape, screenWidthPx, density, isSplitMode)
        val span = group.span
        val groupWidthPx = group.groupWidthPx
        val windowXPx = group.windowXPx

        val topShiftDp = collapsedTopShiftDp(
            isLandscape, settings.enableNotchMode, settings.yOffset
        )
        val dotRect = geometry.idleDotRectPx(
            settings, visibleCount > 0 || viewModel.landscapeWidgetPill.value, span.startPx, span.endPx,
            (settings.height + 16f + topShiftDp) * density, density, screenWidthPx.toInt(),
            isLandscape = isLandscape
        )
        // the landscape edge layout needs the tall window for the cutout target, also while a fullscreen app hides it
        val fullscreenEdgeTarget = isLandscape && viewModel.fullscreenHidden.value &&
            geometry.fullscreenCutoutTouchRectPx(settings, true) != null
        val edgeNow = isTouchableRegionSupported.value &&
            (geometry.edgeTouchRectPx(settings, isLandscape, visibleCount) != null || fullscreenEdgeTarget)
        val preGrown = isTouchableRegionSupported.value && (visibleCount > 0 || edgeNow) && !isHidden && !isStatic
        // the square scan is taller than the collapsed window
        val faceIdState = FaceIdController.state.value
        val fullWindow = expanded || widgetWindowOpen || viewModel.rotationMorphActive.value ||
            (faceIdState.active && faceIdState.style != FaceIdController.Style.Cutout)
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
        val passiveOpen = viewModel.autoExpanded.value
        // An untouched auto-opened card leaves keyboard focus with the app underneath.
        val focusFlags = when {
            !BackStepRules.windowFocusable(isInput, expanded, passiveOpen) -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            BackStepRules.windowTakesKeyboard(isInput) -> 0
            // Focused for Back alone: the keyboard of the app below stays where it is
            else -> WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }
        val watchOutside = if (expanded && !passiveOpen && !isInput && isTouchableRegionSupported.value) {
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else 0
        val decorativeDot = settings.idleCutout && !settings.enableAppShortcuts && !fullWindow &&
            geometry.edgeHiddenTouchRectPx(settings, isLandscape, visibleCount) == null
        val noTouch = if (isStatic || decorativeDot) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0
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

        val params = overlayLayoutParams(w, h, currentFlags, currentX, currentY, currentSoftInputMode)
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
        val screenWidthPx = rotation.screenWidthPxNow().toFloat()
        val isLandscapeNow = landscapeNow()
        val topShiftDp = collapsedTopShiftDp(
            isLandscapeNow, settings.enableNotchMode, settings.yOffset
        )
        val isSplitMode = viewModelHolder.ready && hasSecondaryCutout(
            cutoutCardCount(
                viewModel.visibleNotifications.value, viewModel.expanded.value,
                viewModel.selectedIndex.value
            )
        )
        val group = collapsedGroup(settings, isLandscapeNow, screenWidthPx, density, isSplitMode)
        val groupWidthPx = group.groupWidthPx
        val windowXPx = group.windowXPx

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

        return overlayLayoutParams(
            w,
            ((settings.height + 16f + topShiftDp) * density).toInt(),
            currentFlags,
            x = if (isTouchableRegionSupported.value) 0 else windowXPx,
            y = if (settings.enableNotchMode || isLandscapeNow) 0 else settings.yOffset.dpToPx()
        ).also {
            lastParams = it
        }
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun AbstractComposeView.installOverlayViewTreeOwners() {
        setViewTreeLifecycleOwner(overlayOwners)
        setViewTreeViewModelStoreOwner(overlayOwners)
        setViewTreeSavedStateRegistryOwner(overlayOwners)
    }

    companion object {
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        private const val TAG = "SmartIslandOverlayService"
        private const val SILENT_BACK_DELAY_MS = 300L
        private const val AUTO_COLLAPSE_DELAY_MS = 750L
        private const val WIDGET_WINDOW_LINGER_MS = 700L
        private const val WIDGET_ISLAND_EVENT_GRACE_MS = 500L
        private const val KEYGUARD_POLL_MS = 1_000L
        private const val WINDOW_REFRESH_MIN_MS = 120L
        private const val SETTLE_SCAN_FIRST_MS = 400L
        private const val SETTLE_SCAN_SECOND_MS = 1_100L
        private const val WINDOW_PACKAGE_CACHE_MAX = 96
    }
}
