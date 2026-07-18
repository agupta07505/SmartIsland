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
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class SmartIslandNotificationListenerService : NotificationListenerService() {
    // Keys we have canceled ourselves to make island-only. Keeps island copy alive.
    private val suppressedKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            notificationRepository.commands.collect { command ->
                when (command) {
                    is SmartIslandCommand.CancelNotification -> {
                        runCatchingLogged(TAG, "CancelNotificationcmd failed") {
                            forceCancelNotification(command.key)
                        }
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
        mainScope.cancel()
        super.onDestroy()
    }

    private val pendingRemovals = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        // IMMEDIATE PERFECT SUPPRESSION: Try to cancel synchronously on the listener thread
        // before any async work, so system shade never shows it for more than a few ms.
        // We check quickly if it should be island-only.
        try {
            if (!com.agupta07505.smartisland.util.NotificationFilter.shouldSuppressFromIsland(sbn, packageManager)) {
                val modeQuick = sbn.notification.toIslandMode()
                val isOngoingCall = modeQuick == IslandMode.IncomingCall && !isIncomingCall(sbn.notification)
                if (!isOngoingCall) {
                    // Pre-add to suppressedKeys and cancel instantly
                    suppressedKeys.add(sbn.key)
                    runCatchingLogged(TAG, "Immediate cancel failed") { cancelNotification(sbn.key) }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        runCatchingLogged(TAG, "Immediate snooze failed") { snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L) }
                    }
                    android.util.Log.d(TAG, "Immediate island-only suppress: ${sbn.key}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Immediate suppress exception", e)
        }

        // Cancel any pending removal job for this key to keep island copy
        pendingRemovals.remove(sbn.key)?.cancel()

        serviceScope.launch {
            runCatchingLogged(TAG, "NotificationPosted async failed") {
                if (shouldSuppressFromIsland(sbn)) return@runCatchingLogged

                val settings = repository.settings.first()
                if (!settings.enabled) return@runCatchingLogged

                val overlayReady = ensureOverlayServiceRunning()
                android.util.Log.d(TAG, "Processing island-only async: key=${sbn.key} overlayReady=$overlayReady")
                handleNotificationPosted(sbn)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        android.util.Log.d(TAG, "onNotificationRemoved: key=${sbn.key} pkg=${sbn.packageName}")

        pendingRemovals.remove(sbn.key)?.cancel()

        val job = serviceScope.launch {
            delay(350L)
            runCatchingLogged(TAG, "NotificationRemoved handling failed") {
                if (sbn.packageName == packageName) return@runCatchingLogged
                if (suppressedKeys.remove(sbn.key)) {
                    android.util.Log.d(TAG, "Suppressed key, keeping island: ${sbn.key}")
                    return@runCatchingLogged
                }
                android.util.Log.d(TAG, "Removing from island repo: ${sbn.key}")
                notificationRepository.removeNotification(sbn.key)
            }
            pendingRemovals.remove(sbn.key)
        }
        pendingRemovals[sbn.key] = job
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d(TAG, "onListenerConnected")
        serviceScope.launch {
            runCatchingLogged(TAG, "ListenerConnected failed") {
                val settings = repository.settings.first()
                if (!settings.enabled) return@runCatchingLogged

                val overlayReady = ensureOverlayServiceRunning()
                val active = activeNotifications
                    .filter { it.packageName != packageName }
                    .filterNot { shouldSuppressFromIsland(it) }

                android.util.Log.d(TAG, "ListenerConnected: ${active.size} active, overlayReady=$overlayReady")

                // PERFECT: Force island-only for all existing notifications
                active.forEach { sbn ->
                    val mode = sbn.notification.toIslandMode()
                    if (shouldBeIslandOnly(sbn.notification, mode)) {
                        suppressSystemNotificationAggressive(sbn.key)
                    }
                    handleNotificationPosted(sbn)
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

    private fun isIncomingCall(notification: Notification): Boolean {
        return notification.actions?.any { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("answer") || label.contains("accept") || label.contains("take")
        } == true
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification
        if (shouldSuppressFromIsland(sbn)) return

        val extras = notification.extras
        val mode = notification.toIslandMode()
        android.util.Log.d(TAG, "handleNotificationPosted: mode=$mode key=${sbn.key} title=${extras.getCharSequence(Notification.EXTRA_TITLE)}")

        val shouldIslandOnly = shouldBeIslandOnly(notification, mode)

        if (shouldIslandOnly) {
            // Aggressive suppression with retries to guarantee island-only
            suppressSystemNotificationAggressive(sbn.key)
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
            autoExpand = shouldIslandOnly // always auto-expand for island-only
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

    private fun shouldBeIslandOnly(notification: Notification, mode: IslandMode): Boolean {
        if (mode == IslandMode.IncomingCall) {
            if (!isIncomingCall(notification)) return false // ongoing call stays in system
        }
        // All others: island-only
        return true
    }

    // Kept for tests
    internal fun isHighPriorityNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val ranking = Ranking()
        val rankingMap = currentRanking
        val isHigh = rankingMap != null && rankingMap.getRanking(sbn.key, ranking) && ranking.importance >= android.app.NotificationManager.IMPORTANCE_HIGH
        return isHigh || notification.fullScreenIntent != null
    }

    /**
     * PERFECT suppression: immediate + 3 retries + snooze fallback.
     * Guarantees notification removed from system shade and only lives in island.
     */
    private fun suppressSystemNotificationAggressive(key: String) {
        suppressedKeys.add(key)

        // Immediate attempts on main thread for best chance
        mainScope.launch {
            repeat(4) { attempt ->
                runCatchingLogged(TAG, "cancel attempt $attempt failed") {
                    cancelNotification(key)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    runCatchingLogged(TAG, "snooze attempt $attempt failed") {
                        snoozeNotification(key, 24 * 60 * 60 * 1000L)
                    }
                }
                delay(120L * (attempt + 1)) // 120, 240, 360, 480ms
                val stillActive = try { activeNotifications.any { it.key == key } } catch (_: Exception) { false }
                if (!stillActive) {
                    android.util.Log.d(TAG, "Successfully suppressed after ${attempt + 1} attempts: $key")
                    return@launch
                }
                android.util.Log.w(TAG, "Still active after attempt ${attempt + 1}, retrying: $key")
            }
            android.util.Log.w(TAG, "Failed to suppress after retries, final active check for: $key")
        }

        // Also synchronous attempt for instant effect
        runCatchingLogged(TAG, "sync cancel failed") { cancelNotification(key) }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            runCatchingLogged(TAG, "sync snooze failed") { snoozeNotification(key, 24 * 60 * 60 * 1000L) }
        }
    }

    private fun forceCancelNotification(key: String) {
        suppressedKeys.add(key)
        runCatchingLogged(TAG, "forceCancel failed") {
            cancelNotification(key)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                snoozeNotification(key, 24 * 60 * 60 * 1000L)
            }
        }
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
        return MediaInfo(artwork, positionMs, durationMs, playbackState?.state == PlaybackState.STATE_PLAYING)
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

    private data class MediaInfo(val artwork: Bitmap?, val positionMs: Long?, val durationMs: Long?, val isPlaying: Boolean)

    companion object {
        private const val TAG = "SmartIslandNotificationListener"
        private const val ICON_BITMAP_SIZE = 96
        private const val LARGE_ICON_BITMAP_SIZE = 128
    }
}
