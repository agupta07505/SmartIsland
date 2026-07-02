package com.agupta07505.smartisland.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.agupta07505.smartisland.ui.IslandOverlayView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SmartIslandOverlayService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private lateinit var repository: SmartIslandSettingsRepository
    private var islandView: ComposeView? = null
    private var outsideTapView: View? = null
    private val overlayOwners = OverlayViewTreeOwners()
    private val settingsState = MutableStateFlow(SmartIslandSettings.Default)
    private val expandedState = MutableStateFlow(false)
    private val modeState = MutableStateFlow(IslandMode.Empty)
    private val notificationState = MutableStateFlow<IslandNotification?>(null)

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = SmartIslandSettingsRepository(applicationContext)
        overlayOwners.resume()
        startForeground(NOTIFICATION_ID, buildServiceNotification())
        pendingNotification?.let { applyNotification(it) }

        lifecycleScope.launch {
            repository.settings.collect { settings ->
                settingsState.value = settings
                if (!settings.enabled) {
                    stopSelf()
                } else if (Settings.canDrawOverlays(this@SmartIslandOverlayService)) {
                    ensureCollapsedWindow()
                    updateCollapsedLayout(settings)
                }
            }
        }
    }

    override fun onDestroy() {
        removeCollapsedWindow()
        removeOutsideTapWindow()
        overlayOwners.destroy()
        if (instance?.get() == this) instance = null
        super.onDestroy()
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
                    modeFlow = modeState,
                    notificationFlow = notificationState,
                    onToggleExpanded = { if (expandedState.value) collapse() else expand() }
                )
            }
        }

        windowManager.addView(islandView, collapsedParams(settingsState.value))
    }

    private fun expand() {
        if (expandedState.value || !Settings.canDrawOverlays(this)) return
        ensureOutsideTapWindow()
        bringIslandToFront()
        expandedState.value = true
    }

    private fun collapse() {
        if (!expandedState.value) return
        expandedState.value = false
        lifecycleScope.launch {
            delay(260)
            removeOutsideTapWindow()
        }
    }

    private fun updateCollapsedLayout(settings: SmartIslandSettings) {
        val view = islandView ?: return
        runCatching { windowManager.updateViewLayout(view, collapsedParams(settings)) }
    }

    private fun removeCollapsedWindow() {
        islandView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        islandView = null
    }

    private fun ensureOutsideTapWindow() {
        if (outsideTapView != null) return
        outsideTapView = View(this).apply {
            setOnClickListener { collapse() }
        }
        windowManager.addView(outsideTapView, fullScreenParams())
    }

    private fun removeOutsideTapWindow() {
        outsideTapView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        outsideTapView = null
    }

    private fun bringIslandToFront() {
        val view = islandView ?: return
        runCatching {
            windowManager.removeView(view)
            windowManager.addView(view, collapsedParams(settingsState.value))
        }
    }

    private fun collapsedParams(settings: SmartIslandSettings): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = settings.xOffset.dpToPx()
            y = settings.yOffset.dpToPx()
        }
    }

    private fun fullScreenParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
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

    private fun applyNotification(notification: IslandNotification) {
        notificationState.value = notification
        modeState.value = IslandMode.Notification
    }

    private fun showDemoMode(mode: IslandMode) {
        modeState.value = mode
        if (mode == IslandMode.Notification) {
            notificationState.value = IslandNotification(
                packageName = packageName,
                appName = "Smart Island",
                title = "ArchiveTune",
                text = "A new notification is ready in the island.",
                timeMillis = System.currentTimeMillis()
            )
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
        private var pendingNotification: IslandNotification? = null

        fun updateNotification(notification: IslandNotification) {
            pendingNotification = notification
            instance?.get()?.applyNotification(notification)
        }

        fun showDemo(mode: IslandMode) {
            instance?.get()?.showDemoMode(mode)
        }
    }
}

@Composable
private fun OverlayIsland(
    settingsFlow: StateFlow<SmartIslandSettings>,
    expandedFlow: StateFlow<Boolean>,
    modeFlow: StateFlow<IslandMode>,
    notificationFlow: StateFlow<IslandNotification?>,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by settingsFlow.collectAsState()
    val expanded by expandedFlow.collectAsState()
    val mode by modeFlow.collectAsState()
    val notification by notificationFlow.collectAsState()

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        mode = mode,
        notification = notification,
        onToggleExpanded = onToggleExpanded,
        modifier = modifier
    )
}
