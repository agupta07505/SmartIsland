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
                        runCatchingLogged(TAG, "CancelNotification failed") {
                            // user explicitly dismissed from island -> also remove from system if still there
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
        super.onDestroy()
    }

    private val pendingRemovals = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        android.util.Log.d(TAG, "onNotificationPosted: package=${sbn.packageName}, id=${sbn.id} key=${sbn.key}")

        // Cancel any pending removal for this key to prevent the notification from being deleted from island
        pendingRemovals.remove(sbn.key)?.cancel()

        serviceScope.launch {
            runCatchingLogged(TAG, "NotificationPosted handling failed") {
                if (shouldSuppressFromIsland(sbn)) {
                    android.util.Log.d(TAG, "Filtered out by NotificationFilter: ${sbn.key}")
                    return@runCatchingLogged
                }

                val settings = repository.settings.first()
                if (!settings.enabled) {
                    android.util.Log.d(TAG, "Island disabled, skipping island-only logic")
                    return@runCatchingLogged
                }

                // PERFECT FIX: Always handle for island-only, regardless of overlay state.
                // Overlay readiness is only used for auto-expand animation, not for suppression decision.
                val overlayReady = ensureOverlayServiceRunning()
                android.util.Log.d(TAG, "Processing for island-only: key=${sbn.key} overlayReady=$overlayReady")
                handleNotificationPosted(sbn, overlayReadyForExpand = overlayReady)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        android.util.Log.d(TAG, "onNotificationRemoved: package=${sbn.packageName}, id=${sbn.id} key=${sbn.key}")

        // Cancel any existing removal job for this key
        pendingRemovals.remove(sbn.key)?.cancel()

        val job = serviceScope.launch {
            delay(350L) // debounce
            runCatchingLogged(TAG, "NotificationRemoved handling failed") {
                if (sbn.packageName == packageName) return@runCatchingLogged
                if (suppressedKeys.remove(sbn.key)) {
                    // We canceled it ourselves to make it island-only -> keep in island repo
                    android.util.Log.d(TAG, "Suppress key removed, keeping in island: ${sbn.key}")
                    return@runCatchingLogged
                }
                android.util.Log.d(TAG, "Removing from island repo (user cleared/system): ${sbn.key}")
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
                val settings = repository.settings.first()
                if (!settings.enabled) return@runCatchingLogged

                val overlayReady = ensureOverlayServiceRunning()
                val notifications = activeNotifications
                    .filter { it.packageName != packageName }
                    .filterNot { shouldSuppressFromIsland(it) }

                android.util.Log.d(TAG, "onListenerConnected: found ${notifications.size} active, overlayReady=$overlayReady")

                if (notifications.isEmpty()) return@runCatchingLogged

                // PERFECT FIX: On reconnect, also force island-only for all existing notifications
                notifications.forEach { sbn ->
                    handleNotificationPosted(sbn, overlayReadyForExpand = overlayReady)
                }
            }
        }
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
        // We check accessibility, but for island-only we still suppress even if false.
        // Returning true/false only affects auto-expand animation flag.
        return isAccessibilityServiceEnabled() || Settings.canDrawOverlays(this)
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification, overlayReadyForExpand: Boolean) {
        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        if (shouldSuppressFromIsland(sbn)) return

        val extras = notification.extras
        val mode = notification.toIslandMode()
        android.util.Log.d(TAG, "handleNotificationPosted: mode=$mode, key=${sbn.key} title=${extras.getCharSequence(Notification.EXTRA_TITLE)}")

        // PERFECT FIX: Determine if this notification should be island-only
        val shouldBeIslandOnly = shouldBeIslandOnly(notification, mode)

        if (shouldBeIslandOnly) {
            // Immediately remove from system tray so it only lives in SmartIsland
            suppressSystemNotification(sbn.key)

            // Second attempt after short delay to handle OEMs that re-post instantly
            serviceScope.launch {
                delay(150L)
                if (activeNotifications.any { it.key == sbn.key }) {
                    android.util.Log.d(TAG, "Retry suppress for key still in active: ${sbn.key}")
                    suppressSystemNotification(sbn.key)
                }
            }
        }

        val mediaInfo = if (mode == IslandMode.Music) findMediaInfo(notification, sbn.packageName) else null
        val appName = runCatchingLogged(TAG, "GetApplicationInfo failed") {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } ?: sbn.packageName

        // Auto-expand only if overlay is ready and it's island-only; otherwise still show in island list
        val autoExpand = shouldBeIslandOnly

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
            autoExpand = autoExpand
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

    /**
     * PERFECT FIX: Island-only determination.
     * Returns true for all notifications that should appear ONLY in SmartIsland, not in system shade.
     * Exception: ongoing call (not incoming) stays in system to detect call end.
     */
    private fun shouldBeIslandOnly(notification: Notification, mode: IslandMode): Boolean {
        if (mode == IslandMode.IncomingCall) {
            val isIncoming = notification.actions?.any { action ->
                val label = action.title?.toString()?.lowercase().orEmpty()
                label.contains("answer") || label.contains("accept") || label.contains("take")
            } == true
            if (!isIncoming) {
                // Ongoing call in progress – keep in system tray
                return false
            }
        }
        // For all other modes (Notification, Music, IncomingCall-incoming), island-only
        return true
    }

    // Kept for backward compatibility but no longer used for suppression decision
    internal fun isHighPriorityNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val ranking = Ranking()
        val rankingMap = currentRanking
        val isHighImportance = rankingMap != null &&
            rankingMap.getRanking(sbn.key, ranking) &&
            ranking.importance >= NotificationManager.IMPORTANCE_HIGH
        return isHighImportance || notification.fullScreenIntent != null
    }

    /**
     * PERFECT island-only suppression with retries and fallback.
     */
    private fun suppressSystemNotification(key: String) {
        suppressedKeys.add(key)
        val result = runCatchingLogged(TAG, "cancelNotification failed") {
            cancelNotification(key)
        }
        if (result == null) {
            // cancelNotification threw or returned null -> try alternative
            android.util.Log.w(TAG, "cancelNotification failed for $key, trying cancelNotifications/snooze")

            // Try batch cancel (API 21+ can still use single key cancel, but try anyway)
            runCatchingLogged(TAG, "cancelNotifications batch failed") {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    // No direct batch with single key in listener, but cancelAll could be too aggressive,
                    // so we reuse cancelNotification again in case previous was transient
                    cancelNotification(key)
                }
            }

            // Snooze as last resort (Android O+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                runCatchingLogged(TAG, "snoozeNotification failed") {
                    snoozeNotification(key, 60_000L * 60L * 24L) // snooze for a day, effectively hide
                }
            }
        }
        // Even if cancel technically failed, we keep key in suppressedKeys for debounce logic.
        // Only remove if we are sure cancel never works after retries.
        // Do NOT remove here; removal happens in onNotificationRemoved debounce.

        android.util.Log.d(TAG, "Suppressed from system tray: $key, success=${result != null}")
    }

    private fun forceCancelNotification(key: String) {
        suppressedKeys.add(key) // to distinguish if user dismissed from island
        runCatchingLogged(TAG, "forceCancelNotification failed") {
            cancelNotification(key)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // also try snooze to ensure hidden
                snoozeNotification(key, 60_000L * 60L * 24L)
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
