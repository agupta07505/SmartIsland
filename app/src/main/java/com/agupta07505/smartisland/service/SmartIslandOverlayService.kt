package com.agupta07505.smartisland.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.IslandOverlayView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SmartIslandOverlayService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private lateinit var repository: SmartIslandSettingsRepository
    private var islandView: ComposeView? = null
    private val overlayOwners = OverlayViewTreeOwners()
    private val settingsState = MutableStateFlow(SmartIslandSettings.Default)
    private val expandedState = MutableStateFlow(false)
    private val modeState = MutableStateFlow(IslandMode.Empty)
    private val notificationsState = MutableStateFlow<List<IslandNotification>>(emptyList())
    private val selectedIndexState = MutableStateFlow(0)

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = SmartIslandSettingsRepository(applicationContext)
        overlayOwners.resume()
        startForeground(NOTIFICATION_ID, buildServiceNotification())
        pendingNotifications.forEach { applyNotification(it, autoExpand = false) }

        lifecycleScope.launch {
            repository.settings.collect { settings ->
                settingsState.value = settings
                if (!settings.enabled) {
                    stopSelf()
                } else if (Settings.canDrawOverlays(this@SmartIslandOverlayService)) {
                    ensureCollapsedWindow()
                    updateWindowLayoutParams(expandedState.value, settings)
                }
            }
        }

        lifecycleScope.launch {
            expandedState.collect { expanded ->
                if (expanded) {
                    updateWindowLayoutParams(true, settingsState.value)
                    startAutoCollapseTimer()
                } else {
                    stopAutoCollapseTimer()
                    // Delay window resizing to allow the collapse animation to play out smoothly
                    // in the transparent expanded window before wrapping.
                    kotlinx.coroutines.delay(500)
                    if (!expandedState.value) {
                        updateWindowLayoutParams(false, settingsState.value)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        removeCollapsedWindow()
        overlayOwners.destroy()
        if (instance?.get() == this) instance = null
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
        if (!Settings.canDrawOverlays(this)) return

        islandView = ComposeView(this).apply {
            installOverlayViewTreeOwners()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverlayIsland(
                    settingsFlow = settingsState,
                    expandedFlow = expandedState,
                    notificationsFlow = notificationsState,
                    selectedIndexFlow = selectedIndexState,
                    statusBarHeight = statusBarHeight,
                    onPageSelected = { index ->
                        setSelectedNotificationIndex(index)
                    },
                    onOpenNotification = { notification ->
                        openNotification(notification)
                    },
                    onToggleExpanded = { toggleExpanded() }
                )
            }
            try {
                val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
                val addListenerMethod = android.view.ViewTreeObserver::class.java.getMethod("addOnComputeInternalInsetsListener", listenerClass)
                val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    arrayOf(listenerClass)
                ) { _, method, args ->
                    // CRITICAL: Wrap entire body in try-catch. Any exception escaping
                    // a Proxy becomes UndeclaredThrowableException and crashes the app
                    // during ViewRootImpl layout traversals.
                    try {
                        if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                            val info = args[0] ?: return@newProxyInstance null
                            val setTouchableInsetsMethod = info.javaClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
                            if (!expandedState.value) {
                                setTouchableInsetsMethod.invoke(info, 3) // 3 is TOUCHABLE_INSETS_REGION

                                // Try multiple field names for touchableRegion across OEM ROMs
                                val touchableRegionField = sequenceOf("touchableRegion", "mTouchableRegion")
                                    .mapNotNull { name ->
                                        runCatching { info.javaClass.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()
                                    }
                                    .firstOrNull()

                                if (touchableRegionField != null) {
                                    val touchableRegion = touchableRegionField.get(info) as? android.graphics.Region
                                    if (touchableRegion != null) {
                                        val density = resources.displayMetrics.density
                                        val w = ((settingsState.value.width + 48f) * density).toInt()
                                        val h = ((settingsState.value.height + 36f) * density).toInt()
                                        val xOffsetPx = (settingsState.value.xOffset * density).toInt()
                                        val yOffsetPx = (settingsState.value.yOffset * density).toInt()

                                        val screenWidth = resources.displayMetrics.widthPixels
                                        val left = (screenWidth - w) / 2 + xOffsetPx
                                        val right = left + w
                                        val top = 0
                                        val bottom = h

                                        touchableRegion.set(left, top, right, bottom)
                                    }
                                }
                            } else {
                                setTouchableInsetsMethod.invoke(info, 0) // 0 is TOUCHABLE_INSETS_FRAME
                            }
                        }
                    } catch (_: Throwable) {
                        // Silently ignore — touchable region is best-effort, not critical
                    }
                    null
                }
                addListenerMethod.invoke(viewTreeObserver, proxyListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        windowManager.addView(islandView, collapsedParams(settingsState.value))
    }

    private fun expand() {
        if (expandedState.value || !Settings.canDrawOverlays(this)) return
        expandedState.value = true
    }

    private fun collapse() {
        if (!expandedState.value) return
        expandedState.value = false
    }

    private fun toggleExpanded() {
        if (expandedState.value) collapse() else expand()
    }

    private var autoCollapseJob: kotlinx.coroutines.Job? = null

    private fun startAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        autoCollapseJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(5000)
            collapse()
        }
    }

    private fun stopAutoCollapseTimer() {
        autoCollapseJob?.cancel()
        autoCollapseJob = null
    }

    fun resetAutoCollapseTimer() {
        if (expandedState.value) {
            startAutoCollapseTimer()
        }
    }

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        val view = islandView ?: return
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        // Width is always MATCH_PARENT to prevent horizontal resize shifts/jumps
        val w = WindowManager.LayoutParams.MATCH_PARENT
        val h = if (expanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            // Add statusBarHeight so the pill (translated down by yOffset) stays within the touch window
            ((settings.height + 36f) * density).toInt() + statusBarHeight.dpToPx()
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
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun removeCollapsedWindow() {
        islandView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        islandView = null
    }

    private fun collapsedParams(settings: SmartIslandSettings): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            // Add statusBarHeight so the pill (translated down by statusBarHeight) stays within the touch window
            ((settings.height + 36f) * density).toInt() + statusBarHeight.dpToPx(),
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
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Smart Island is running")
            .setContentText("Floating island overlay is active.")
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun applyNotification(notification: IslandNotification, autoExpand: Boolean) {
        val currentList = notificationsState.value.toMutableList()
        val index = currentList.indexOfFirst { it.key == notification.key }
        if (index >= 0) {
            currentList[index] = notification
        } else {
            currentList.add(notification)
        }
        notificationsState.value = currentList
        if (index < 0) {
            selectedIndexState.value = currentList.size - 1
        }
        updateActiveMode()
        if (autoExpand) {
            expand()
        }
    }

    fun removeNotification(key: String) {
        val currentList = notificationsState.value.toMutableList()
        val index = currentList.indexOfFirst { it.key == key }
        if (index >= 0) {
            currentList.removeAt(index)
            notificationsState.value = currentList
            val currentSelected = selectedIndexState.value
            if (currentSelected >= currentList.size) {
                selectedIndexState.value = (currentList.size - 1).coerceAtLeast(0)
            }
            updateActiveMode()
        }
    }

    private fun updateActiveMode() {
        val list = notificationsState.value
        val index = selectedIndexState.value
        if (list.isNotEmpty() && index in list.indices) {
            modeState.value = list[index].mode
        } else {
            modeState.value = IslandMode.Empty
        }
    }

    fun setSelectedNotificationIndex(index: Int) {
        val list = notificationsState.value
        if (index in list.indices) {
            selectedIndexState.value = index
            updateActiveMode()
            resetAutoCollapseTimer()
        }
    }

    private fun openNotification(notification: IslandNotification) {
        if (notification.contentIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                runCatching {
                    notification.contentIntent.send(this, 0, null, null, null, null, options)
                }
            } else {
                runCatching {
                    notification.contentIntent.send()
                }
            }
        } else {
            runCatching {
                val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    android.widget.Toast.makeText(this, "Opening ${notification.appName} (Demo)", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        removeNotification(notification.key)
        SmartIslandNotificationListenerService.cancelSystemNotification(notification.key)
        collapse()
    }

    private fun showDemoMode(mode: IslandMode) {
        val demoNotification = when (mode) {
            IslandMode.Notification -> IslandNotification(
                key = "demo_notif",
                packageName = "org.telegram.messenger",
                appName = "Telegram",
                title = "Alice Smith",
                text = "Hey! Are we still meeting for lunch today?",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Notification,
                actionIntents = listOf(
                    IslandNotificationAction("Reply", null),
                    IslandNotificationAction("Mark Read", null)
                )
            )
            IslandMode.IncomingCall -> IslandNotification(
                key = "demo_call",
                packageName = "com.google.android.dialer",
                appName = "Phone",
                title = "John Doe",
                text = "Incoming Call",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.IncomingCall,
                actionIntents = listOf(
                    IslandNotificationAction("Decline", null),
                    IslandNotificationAction("Answer", null)
                )
            )
            IslandMode.Music -> IslandNotification(
                key = "demo_music",
                packageName = "com.spotify.music",
                appName = "Spotify",
                title = "Starlight",
                text = "Muse - Black Holes and Revelations",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Music,
                mediaIsPlaying = true,
                mediaDurationMs = 240000L,
                mediaPositionMs = 45000L,
                actionIntents = listOf(
                    IslandNotificationAction("Previous", null),
                    IslandNotificationAction("Play", null),
                    IslandNotificationAction("Next", null)
                )
            )
            IslandMode.Empty -> null
        }

        if (demoNotification != null) {
            applyNotification(demoNotification, autoExpand = true)
        }
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun ComposeView.installOverlayViewTreeOwners() {
        setViewTreeLifecycleOwner(overlayOwners)
        setViewTreeViewModelStoreOwner(overlayOwners)
        setViewTreeSavedStateRegistryOwner(overlayOwners)
    }

    companion object {
        private const val NOTIFICATION_ID = 8105
        private var instance: WeakReference<SmartIslandOverlayService>? = null
        private val pendingNotifications = mutableListOf<IslandNotification>()

        fun updateNotification(notification: IslandNotification, autoExpand: Boolean = false) {
            val existingIndex = pendingNotifications.indexOfFirst { it.key == notification.key }
            if (existingIndex >= 0) {
                pendingNotifications[existingIndex] = notification
            } else {
                pendingNotifications.add(notification)
            }
            instance?.get()?.applyNotification(notification, autoExpand)
        }

        fun removeNotification(key: String) {
            pendingNotifications.removeAll { it.key == key }
            instance?.get()?.removeNotification(key)
        }

        fun showDemo(mode: IslandMode) {
            instance?.get()?.showDemoMode(mode)
        }

        fun resetTimer() {
            instance?.get()?.resetAutoCollapseTimer()
        }
    }
}

@Composable
private fun OverlayIsland(
    settingsFlow: StateFlow<SmartIslandSettings>,
    expandedFlow: StateFlow<Boolean>,
    notificationsFlow: StateFlow<List<IslandNotification>>,
    selectedIndexFlow: StateFlow<Int>,
    statusBarHeight: Float,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by settingsFlow.collectAsState()
    val expanded by expandedFlow.collectAsState()
    val notifications by notificationsFlow.collectAsState()
    val selectedIndex by selectedIndexFlow.collectAsState()

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        notifications = notifications,
        selectedIndex = selectedIndex,
        onPageSelected = onPageSelected,
        onOpenNotification = onOpenNotification,
        onToggleExpanded = onToggleExpanded,
        statusBarHeight = statusBarHeight,
        modifier = modifier
    )
}
