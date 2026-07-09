/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.SmartIslandApp
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.IslandOverlayView
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.launch

class SmartIslandOverlayService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private lateinit var repository: SmartIslandSettingsRepository
    private lateinit var notificationRepository: com.agupta07505.smartisland.data.INotificationRepository
    private var islandView: ComposeView? = null
    private val overlayOwners = OverlayViewTreeOwners()
    private lateinit var systemEventReceiver: SystemEventReceiver
    private lateinit var viewModel: IslandViewModel

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val app = application as SmartIslandApp
        repository = app.settingsRepository
        notificationRepository = app.notificationRepository

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
        registerReceiver(systemEventReceiver, filter)

        overlayOwners.resume()
        startForeground(NOTIFICATION_ID, buildServiceNotification())

        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                if (!settings.enabled) {
                    stopSelf()
                } else if (Settings.canDrawOverlays(this@SmartIslandOverlayService)) {
                    ensureCollapsedWindow()
                    updateWindowLayoutParams(viewModel.expanded.value, settings)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.expanded.collect { expanded ->
                if (expanded) {
                    updateWindowLayoutParams(true, viewModel.settings.value)
                } else {
                    kotlinx.coroutines.delay(500)
                    if (!viewModel.expanded.value) {
                        updateWindowLayoutParams(false, viewModel.settings.value)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(systemEventReceiver)
        removeCollapsedWindow()
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

    @SuppressLint("PrivateApi")
    private fun ensureCollapsedWindow() {
        if (islandView != null) return
        if (!Settings.canDrawOverlays(this)) return

        islandView = ComposeView(this).apply {
            installOverlayViewTreeOwners()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverlayIsland(
                    viewModel = this@SmartIslandOverlayService.viewModel,
                    statusBarHeight = statusBarHeight,
                    onOpenNotification = { notification -> openNotification(notification) },
                    onOpenFloatingWindow = { openCurrentNotificationInFloatingWindow() }
                )
            }
            try {
                if (Build.VERSION.SDK_INT >= 35) {
                    // OnComputeInternalInsetsListener is restricted in V+
                    // Fallback: FLAG_NOT_TOUCH_MODAL already set – full overlay touchable
                }
                val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
                val addListenerMethod = android.view.ViewTreeObserver::class.java.getMethod("addOnComputeInternalInsetsListener", listenerClass)
                val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    arrayOf(listenerClass)
                ) { _, method, args ->
                    try {
                        if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                            val info = args[0] ?: return@newProxyInstance null
                            val setTouchableInsetsMethod = info.javaClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
                            if (!viewModel.expanded.value) {
                                setTouchableInsetsMethod.invoke(info, 3) // TOUCHABLE_INSETS_REGION
                                val touchableRegionField = sequenceOf("touchableRegion", "mTouchableRegion")
                                    .mapNotNull { name ->
                                        runCatchingLogged(TAG, "Failed to get field $name") {
                                            info.javaClass.getDeclaredField(name).apply { isAccessible = true }
                                        }
                                    }
                                    .firstOrNull()

                                if (touchableRegionField != null) {
                                    val touchableRegion = touchableRegionField.get(info) as? android.graphics.Region
                                    if (touchableRegion != null) {
                                         val density = resources.displayMetrics.density
                                         val w = ((viewModel.settings.value.width + 16f) * density).toInt()
                                         val h = ((viewModel.settings.value.height + 16f) * density).toInt()
                                         val xOffsetPx = (viewModel.settings.value.xOffset * density).toInt()

                                         val screenWidth = resources.displayMetrics.widthPixels
                                         val left = (screenWidth - w) / 2 + xOffsetPx
                                         val right = left + w
                                         val top = 0
                                         val bottom = h

                                         touchableRegion.set(left, top, right, bottom)
                                    }
                                }
                            } else {
                                setTouchableInsetsMethod.invoke(info, 0) // TOUCHABLE_INSETS_FRAME
                            }
                        }
                    } catch (_: Throwable) {
                    }
                    null
                }
                addListenerMethod.invoke(viewTreeObserver, proxyListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        windowManager.addView(islandView, collapsedParams(viewModel.settings.value))
    }

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        val view = islandView ?: return
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val w = WindowManager.LayoutParams.MATCH_PARENT
        val h = if (expanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            ((settings.height + 16f) * density).toInt()
        }
        val params = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = settings.yOffset.dpToPx()
        }
        runCatchingLogged(TAG, "Failed to update view layout") { windowManager.updateViewLayout(view, params) }
    }

    private fun removeCollapsedWindow() {
        islandView?.let { view ->
            runCatchingLogged(TAG, "Failed to remove view") { windowManager.removeView(view) }
        }
        islandView = null
    }

    private fun collapsedParams(settings: SmartIslandSettings): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            ((settings.height + 16f) * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = settings.yOffset.dpToPx()
        }
    }

    private fun buildServiceNotification(): android.app.Notification {
        val channelId = "smart_island_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smart Island overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Smart Island is running")
            .setContentText("Floating island overlay is active.")
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
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

    @SuppressLint("PrivateApi")
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
            val setModeResult = runCatchingLogged(TAG, "Failed to invoke setLaunchWindowingMode") {
                val method = options.javaClass.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                method.invoke(options, WINDOWING_MODE_FREEFORM)
            }
            if (setModeResult == null) {
                Toast.makeText(this, "Freeform windowing mode is not supported on this device.", Toast.LENGTH_SHORT).show()
                viewModel.collapse()
                return
            }
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
            val bundle = options.toBundle()
            if (notification.contentIntent != null) {
                runCatchingLogged(TAG, "Failed to send content intent") {
                    notification.contentIntent.send(this, 0, null, null, null, null, bundle)
                }
            } else {
                runCatchingLogged(TAG, "Failed to launch package activity") {
                    val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    }
}

@Composable
private fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        notifications = notifications,
        selectedIndex = selectedIndex,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        statusBarHeight = statusBarHeight,
        modifier = modifier
    )
}
