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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.agupta07505.smartisland.util.runCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
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

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()
    )

    // Monitor screen state and unlock events to show/hide the island accordingly
    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    overlayOwners.resume()
                    isLockScreenActive = keyguardManager.isKeyguardLocked
                    updateWindowLayoutParams(viewModel.expanded.value, viewModel.settings.value)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    overlayOwners.pause()
                    isLockScreenActive = true
                    updateWindowLayoutParams(viewModel.expanded.value, viewModel.settings.value)
                }
                Intent.ACTION_USER_PRESENT -> {
                    isLockScreenActive = false
                    updateWindowLayoutParams(viewModel.expanded.value, viewModel.settings.value)
                }
            }
        }
    }

    // Fallback sync: check if keyguard locked state changed on window changes
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val locked = keyguardManager.isKeyguardLocked
        if (isLockScreenActive != locked) {
            isLockScreenActive = locked
            updateWindowLayoutParams(viewModel.expanded.value, viewModel.settings.value)
        }
    }

    override fun onInterrupt() {
        // Required override, no-op
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateWindowLayoutParams(viewModel.expanded.value, viewModel.settings.value)
    }

    override fun onCreate() {
        super.onCreate()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // CRASH FIX: resume lifecycle BEFORE ViewModel
        overlayOwners.resume()

        viewModel = ViewModelProvider(
            overlayOwners,
            IslandViewModel.provideFactory(repository, notificationRepository)
        )[IslandViewModel::class.java]
        
        systemEventReceiver = SystemEventReceiver(notificationRepository)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        
        // CRASH FIX: Android 13+/14+ requires explicit export flag for system broadcasts
        runCatchingLogged(TAG, "registerReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(systemEventReceiver, filter)
            }
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        isLockScreenActive = keyguardManager.isKeyguardLocked

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatchingLogged(TAG, "registerReceiver screenStateReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenStateReceiver, screenFilter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(screenStateReceiver, screenFilter)
            }
        }

        serviceScope.launch {
            repository.settings.collect { settings ->
                if (!settings.enabled) {
                    disableSelf()
                } else {
                    ensureCollapsedWindow()
                    updateWindowLayoutParams(viewModel.expanded.value, settings)
                }
            }
        }

        serviceScope.launch {
            viewModel.expanded.collectLatest { expanded ->
                if (expanded) {
                    updateWindowLayoutParams(true, viewModel.settings.value)
                } else {
                    kotlinx.coroutines.delay(AUTO_COLLAPSE_DELAY_MS)
                    updateWindowLayoutParams(false, viewModel.settings.value)
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        runCatchingLogged(TAG, "unregisterReceiver failed") {
            unregisterReceiver(systemEventReceiver)
        }
        runCatchingLogged(TAG, "unregisterReceiver screenStateReceiver failed") {
            unregisterReceiver(screenStateReceiver)
        }
        repeat(3) {
            if (islandView != null) {
                removeCollapsedWindow()
                if (islandView == null) return@repeat
                Thread.sleep(100)
            }
        }
        overlayOwners.destroy()
        super.onDestroy()
    }

    private val statusBarHeight: Float
        get() {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val heightPx = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val heightDp = heightPx / resources.displayMetrics.density
            return if (heightDp > 0f) heightDp else 24f
        }

    private fun ensureCollapsedWindow() {
        if (islandView != null) return
        try {
            islandView = ComposeView(this).apply {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                val isLocked = keyguardManager.isKeyguardLocked
                isLockScreenActive = isLocked
                val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val isHidden = (!viewModel.settings.value.showOnLockScreen && isLocked) || isLandscape
                visibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE

                installOverlayViewTreeOwners()
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    OverlayIsland(
                        viewModel = this@SmartIslandOverlayService.viewModel,
                        statusBarHeight = statusBarHeight,
                        onOpenNotification = { notification -> openNotification(notification) },
                        onLaunchApp = { packageName -> launchApp(packageName) },
                        onOpenFloatingWindow = { openCurrentNotificationInFloatingWindow() }
                    )
                }

                setupTouchableRegion(this)
            }
            runCatchingLogged(TAG, "windowManager.addView failed") {
                windowManager.addView(islandView, collapsedParams(viewModel.settings.value))
            } ?: run {
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
                    android.util.Log.d(TAG, "onComputeInternalInsets callback: isExpanded=$isExpanded")
                    if (isExpanded) {
                        // When expanded, let the entire frame intercept touches so clicking outside collapses it
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                    } else {
                        // PILL-ONLY TOUCHABLE REGION:
                        // Restrict touch interception to ONLY the pill bounds + padding.
                        // Since the window starts at y = 0, we offset the touchable region
                        // vertically by yOffset. Touches outside the pill (status bar zone
                        // and top offset area) pass through to the system natively, which
                        // handles left/right notification and quick settings pull-down.
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        
                        val density = resources.displayMetrics.density
                        val screenWidth = resources.displayMetrics.widthPixels
                        val settingsVal = viewModel.settings.value
                        val pillWidthPx = (settingsVal.width + 12f) * density
                        val pillHeightPx = (settingsVal.height + 16f) * density
                        
                        val left = ((screenWidth - pillWidthPx) / 2f + settingsVal.xOffset * density).toInt()
                        val top = (settingsVal.yOffset * density).toInt()
                        val right = (left + pillWidthPx).toInt()
                        val bottom = (top + pillHeightPx).toInt()
                        
                        android.util.Log.d(TAG, "onComputeInternalInsets: region set to ($left, $top, $right, $bottom)")
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
                    android.util.Log.d(TAG, "OnComputeInternalInsetsListener successfully registered on live ViewTreeObserver")
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
        }
    }

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        val view = islandView ?: return
        val density = resources.displayMetrics.density
        
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked
        isLockScreenActive = isLocked
        viewModel.isLocked.value = isLocked
        
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isHidden = (!settings.showOnLockScreen && isLocked) || isLandscape

        view.visibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE

        val h = if (expanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            ((settings.height + 16f) * density).toInt()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = settings.yOffset.dpToPx()
        }
        runCatchingLogged(TAG, "Failed to update view layout") { 
            windowManager.updateViewLayout(view, params) 
        }
    }

    private fun removeCollapsedWindow() {
        val view = islandView ?: return
        val removed = runCatchingLogged(TAG, "Failed to remove view") {
            windowManager.removeView(view)
        } != null
        if (removed) {
            islandView = null
        } else {
            android.util.Log.w(TAG, "removeCollapsedWindow failed")
        }
    }

    private fun collapsedParams(settings: SmartIslandSettings): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            ((settings.height + 16f) * density).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = settings.yOffset.dpToPx()
        }
    }

    private fun openNotification(notification: IslandNotification) {
        if (notification.contentIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                runCatchingLogged(TAG, "Failed to send content intent with options") {
                    notification.contentIntent.send(this, 0, null, null, null, null, options)
                }
            } else {
                runCatchingLogged(TAG, "Failed to send content intent") {
                    notification.contentIntent.send()
                }
            }
        } else {
            runCatchingLogged(TAG, "Failed to launch package activity") {
                val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "Opening ${notification.appName} (Demo)", Toast.LENGTH_SHORT).show()
                }
            }
        }
        notificationRepository.removeNotification(notification.key)
        notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
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
        viewModel.collapse()
    }

    private fun openCurrentNotificationInFloatingWindow() {
        val list = viewModel.notifications.value
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

    companion object {
        private const val TAG = "SmartIslandOverlayService"
        private const val NOTIFICATION_ID = 8105
        private const val WINDOWING_MODE_FREEFORM = 5
        private const val OVERLAY_CHANNEL_ID = "smart_island_overlay"
        private const val OVERLAY_CHANNEL_NAME = "Smart Island overlay"
        private const val AUTO_COLLAPSE_DELAY_MS = 500L
    }
}
