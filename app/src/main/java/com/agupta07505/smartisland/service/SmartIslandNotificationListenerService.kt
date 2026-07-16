/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class SmartIslandNotificationListenerService : NotificationListenerService() {
    private val suppressedKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            notificationRepository.commands.collect { command ->
                when (command) {
                    is SmartIslandCommand.CancelNotification -> {
                        runCatchingLogged(TAG, "CancelNotification failed") { cancelNotification(command.key) }
                    }
                    is SmartIslandCommand.SeekTo -> {
                        val controller = bestControllerFor(command.packageName)
                        if (controller != null) {
                            runCatchingLogged(TAG, "SeekTo failed") { controller.transportControls.seekTo(command.positionMs) }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private val pendingRemovals = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        android.util.Log.d(TAG, "onNotificationPosted: package=${sbn.packageName}, id=${sbn.id}")
        
        // Cancel any pending removal for this key to prevent the notification from being deleted
        pendingRemovals.remove(sbn.key)?.cancel()
        
        serviceScope.launch {
            runCatchingLogged(TAG, "NotificationPosted handling failed") {
                val canProcess = canProcessNotifications()
                val shouldSuppress = shouldSuppressFromIsland(sbn)
                android.util.Log.d(TAG, "onNotificationPosted async: canProcess=$canProcess, shouldSuppress=$shouldSuppress")
                if (!canProcess) return@runCatchingLogged
                if (shouldSuppress) return@runCatchingLogged
                val overlayStarted = ensureOverlayServiceRunning()
                android.util.Log.d(TAG, "onNotificationPosted async: overlayStarted=$overlayStarted")
                handleNotificationPosted(sbn, allowHeadsUpSuppression = overlayStarted)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        android.util.Log.d(TAG, "onNotificationRemoved: package=${sbn.packageName}, id=${sbn.id}")
        
        // Cancel any existing removal job for this key
        pendingRemovals.remove(sbn.key)?.cancel()
        
        val job = serviceScope.launch {
            kotlinx.coroutines.delay(350L) // 350ms debounce window
            runCatchingLogged(TAG, "NotificationRemoved handling failed") {
                if (sbn.packageName == packageName) return@runCatchingLogged
                if (suppressedKeys.remove(sbn.key)) {
                    // Do not remove since we canceled it ourselves to suppress the system heads-up pop-up
                    return@runCatchingLogged
                }
                notificationRepository.removeNotification(sbn.key)
            }
            pendingRemovals.remove(sbn.key)
        }
        pendingRemovals[sbn.key] = job
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d(TAG, "onListenerConnected called")
        serviceScope.launch {
            runCatchingLogged(TAG, "ListenerConnected handling failed") {
                val canProcess = canProcessNotifications()
                android.util.Log.d(TAG, "onListenerConnected async: canProcess=$canProcess")
                if (!canProcess) return@runCatchingLogged
                val notifications = activeNotifications
                    .filter { it.packageName != packageName }
                    .filterNot { shouldSuppressFromIsland(it) }
                android.util.Log.d(TAG, "onListenerConnected async: found ${notifications.size} active notifications")
                if (notifications.isEmpty()) return@runCatchingLogged
                notifications.forEach { handleNotificationPosted(it, allowHeadsUpSuppression = false) }
                ensureOverlayServiceRunning()
            }
        }
    }

    private suspend fun canProcessNotifications(): Boolean {
        val settings = repository.settings.first()
        val canDraw = Settings.canDrawOverlays(this)
        val a11yEnabled = isAccessibilityServiceEnabled()
        android.util.Log.d(TAG, "canProcessNotifications: settings.enabled=${settings.enabled}, canDrawOverlays=$canDraw, accessibilityEnabled=$a11yEnabled")
        return settings.enabled && (canDraw || a11yEnabled)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = android.content.ComponentName(this, SmartIslandOverlayService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun ensureOverlayServiceRunning(): Boolean {
        return isAccessibilityServiceEnabled()
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification, allowHeadsUpSuppression: Boolean) {
        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        if (shouldSuppressFromIsland(sbn)) return

        val extras = notification.extras
        val mode = notification.toIslandMode()
        android.util.Log.d(TAG, "handleNotificationPosted: mode=$mode, title=${extras.getCharSequence(Notification.EXTRA_TITLE)}")
        val isHeadsUp = shouldSuppressSystemHeadsUp(sbn, notification, mode, allowHeadsUpSuppression)
        if (isHeadsUp) {
            suppressSystemNotification(sbn.key)
        }

        val mediaInfo = if (mode == IslandMode.Music) findMediaInfo(notification, sbn.packageName) else null
        val appName = runCatchingLogged(TAG, "GetApplicationInfo failed") {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } ?: sbn.packageName

        notificationRepository.postNotification(
            IslandNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
                timeMillis = if (notification.`when` != 0L) notification.`when` else sbn.postTime,
                icon = loadAppIconBitmap(sbn.packageName),
                largeIcon = mediaInfo?.artwork ?: notification.loadLargeIconBitmap(),
                actionIntents = notification.actions?.mapNotNull { action ->
                    action.title?.toString()?.let { title ->
                        IslandNotificationAction(title = title, pendingIntent = action.actionIntent)
                    }
                }.orEmpty(),
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
            autoExpand = isHeadsUp
        )

        if (mode == IslandMode.Music) {
            val existing = notificationRepository.notifications.value
            existing.filter { it.packageName == sbn.packageName && it.key != sbn.key }
                .forEach { notificationRepository.removeNotification(it.key) }
        }
    }

    internal fun shouldSuppressFromIsland(sbn: StatusBarNotification): Boolean {
        return com.agupta07505.smartisland.util.NotificationFilter.shouldSuppressFromIsland(sbn, packageManager)
    }

    private fun shouldSuppressSystemHeadsUp(
        sbn: StatusBarNotification,
        notification: Notification,
        mode: IslandMode,
        allowHeadsUpSuppression: Boolean
    ): Boolean {
        if (!allowHeadsUpSuppression) return false

        var shouldSuppress = isHighPriorityNotification(sbn, notification)

        // If it is an ongoing call, do not cancel it (do not treat as heads-up)
        // so that it stays in the system tray and we receive the removal event when it ends.
        if (mode == IslandMode.IncomingCall) {
            val isIncoming = notification.actions?.any { action ->
                val label = action.title?.toString()?.lowercase().orEmpty()
                label.contains("answer") || label.contains("accept") || label.contains("take")
            } == true
            if (!isIncoming) {
                shouldSuppress = false
            }
        }
        return shouldSuppress
    }

    internal fun isHighPriorityNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val ranking = Ranking()
        val rankingMap = currentRanking
        val isHighImportance = rankingMap != null &&
            rankingMap.getRanking(sbn.key, ranking) &&
            ranking.importance >= NotificationManager.IMPORTANCE_HIGH
        return isHighImportance || notification.fullScreenIntent != null
    }

    private fun suppressSystemNotification(key: String) {
        suppressedKeys.add(key)
        val canceled = runCatchingLogged(TAG, "CancelNotification in suppress failed") {
            cancelNotification(key)
        } != null
        if (!canceled) suppressedKeys.remove(key)
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
        android.util.Log.d(TAG, "loadLargeIconBitmap: extraLargeIcon type=${extraLarge?.javaClass?.name}")
        extraLarge.toBitmapOrNull()?.let { 
            android.util.Log.d(TAG, "loadLargeIconBitmap: loaded from EXTRA_LARGE_ICON")
            return it 
        }
        val extraLargeBig = extras.get(Notification.EXTRA_LARGE_ICON_BIG)
        android.util.Log.d(TAG, "loadLargeIconBitmap: extraLargeIconBig type=${extraLargeBig?.javaClass?.name}")
        extraLargeBig.toBitmapOrNull()?.let { 
            android.util.Log.d(TAG, "loadLargeIconBitmap: loaded from EXTRA_LARGE_ICON_BIG")
            return it 
        }
        val largeIconObj = getLargeIcon()
        android.util.Log.d(TAG, "loadLargeIconBitmap: getLargeIcon() = $largeIconObj")
        runCatchingLogged(TAG, "LoadLargeIconBitmap failed") {
            largeIconObj?.loadDrawable(this@SmartIslandNotificationListenerService)
                ?.toBitmap(width = LARGE_ICON_BITMAP_SIZE, height = LARGE_ICON_BITMAP_SIZE)
        }?.let { 
            android.util.Log.d(TAG, "loadLargeIconBitmap: loaded via getLargeIcon()")
            return it 
        }
        return runCatchingLogged(TAG, "LoadMessagingStyleAvatar failed") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                android.util.Log.d(TAG, "loadLargeIconBitmap: EXTRA_MESSAGES size=${messages?.size}")
                if (!messages.isNullOrEmpty()) {
                    val lastMessageBundle = messages.lastOrNull() as? android.os.Bundle
                    val senderPerson = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        lastMessageBundle?.getParcelable("sender_person", android.app.Person::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        lastMessageBundle?.getParcelable("sender_person") as? android.app.Person
                    }
                    android.util.Log.d(TAG, "loadLargeIconBitmap: senderPerson = $senderPerson, icon = ${senderPerson?.icon}")
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
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0 }
        val positionMs = playbackState?.estimatedPosition()
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        return MediaInfo(
            artwork = artwork,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        )
    }

    private val activeMediaControllers: List<MediaController>
        get() = runCatchingLogged(TAG, "GetActiveSessions failed") {
            val componentName = android.content.ComponentName(this, SmartIslandNotificationListenerService::class.java)
            mediaSessionManager.getActiveSessions(componentName)
        } ?: emptyList()

    private val mediaSessionManager by lazy {
        getSystemService(MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
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
        val isPlaying: Boolean
    )

    companion object {
        private const val TAG = "SmartIslandNotificationListener"
        private const val ICON_BITMAP_SIZE = 96
        private const val LARGE_ICON_BITMAP_SIZE = 128
    }
}

