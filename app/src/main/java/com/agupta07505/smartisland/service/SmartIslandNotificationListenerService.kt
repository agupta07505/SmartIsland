package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import java.lang.ref.WeakReference

class SmartIslandNotificationListenerService : NotificationListenerService() {
    private val suppressedKeys = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
    }

    override fun onDestroy() {
        if (instance?.get() == this) instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching { handleNotificationPosted(sbn) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        runCatching {
            if (sbn.packageName == packageName) return
            if (suppressedKeys.remove(sbn.key)) {
                // Do not remove since we canceled it ourselves to suppress the system heads-up pop-up
                return
            }
            SmartIslandOverlayService.removeNotification(sbn.key)
        }
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        val extras = notification.extras
        val mode = notification.toIslandMode()
        val mediaInfo = if (mode == IslandMode.Music) findMediaInfo(notification, sbn.packageName) else null
        val appName = runCatching {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName)

        // Check if heads-up notification (importance >= IMPORTANCE_HIGH)
        var isHeadsUp = false
        val ranking = Ranking()
        val rankingMap = currentRanking
        if (rankingMap != null && rankingMap.getRanking(sbn.key, ranking)) {
            isHeadsUp = ranking.importance >= NotificationManager.IMPORTANCE_HIGH
        }

        // If it is an ongoing call, do not cancel it (do not treat as heads-up)
        // so that it stays in the system tray and we receive the removal event when it ends.
        if (mode == IslandMode.IncomingCall) {
            val isIncoming = notification.actions?.any { action ->
                val label = action.title?.toString()?.lowercase().orEmpty()
                label.contains("answer") || label.contains("accept") || label.contains("take")
            } == true
            if (!isIncoming) {
                isHeadsUp = false
            }
        }

        SmartIslandOverlayService.updateNotification(
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
                actions = notification.actions?.mapNotNull { it.title?.toString() }.orEmpty(),
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
                mode = mode,
                contentIntent = notification.contentIntent
            ),
            autoExpand = isHeadsUp
        )

        if (isHeadsUp) {
            suppressedKeys.add(sbn.key)
            cancelNotification(sbn.key)
        }
    }

    private fun Notification.toIslandMode(): IslandMode {
        return when (category) {
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_MISSED_CALL -> IslandMode.IncomingCall
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_PROGRESS -> IslandMode.Music
            else -> {
                val hasMediaAction = actions?.any { action ->
                    val label = action.title?.toString()?.lowercase().orEmpty()
                    label.contains("play") ||
                        label.contains("pause") ||
                        label.contains("next") ||
                        label.contains("previous")
                } == true
                if (hasMediaAction) IslandMode.Music else IslandMode.Notification
            }
        }
    }

    private fun loadAppIconBitmap(packageName: String): Bitmap? {
        return runCatching {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96)
        }.getOrNull()
    }

    private fun Notification.loadLargeIconBitmap(): Bitmap? {
        extras.get(Notification.EXTRA_LARGE_ICON).toBitmapOrNull()?.let { return it }
        extras.get(Notification.EXTRA_LARGE_ICON_BIG).toBitmapOrNull()?.let { return it }
        return runCatching {
            getLargeIcon()?.loadDrawable(this@SmartIslandNotificationListenerService)
                ?.toBitmap(width = 128, height = 128)
        }.getOrNull()
    }

    private fun Any?.toBitmapOrNull(): Bitmap? {
        return when (this) {
            is Bitmap -> this
            is Icon -> runCatching {
                loadDrawable(this@SmartIslandNotificationListenerService)
                    ?.toBitmap(width = 128, height = 128)
            }.getOrNull()
            else -> null
        }
    }

    private fun findMediaInfo(notification: Notification, packageName: String): MediaInfo? {
        notification.mediaSessionController()?.extractMediaInfo()?.let { return it }
        val controller = activeMediaControllers.firstOrNull { it.packageName == packageName } ?: return null
        return controller.extractMediaInfo()
    }

    private fun Notification.mediaSessionController(): MediaController? {
        val token = runCatching {
            extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        }.getOrNull() ?: return null
        return runCatching { MediaController(this@SmartIslandNotificationListenerService, token) }.getOrNull()
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
        get() = runCatching { mediaSessionManager.getActiveSessions(null) }.getOrDefault(emptyList())

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
        private var instance: WeakReference<SmartIslandNotificationListenerService>? = null

        fun cancelSystemNotification(key: String) {
            val service = instance?.get()
            if (service != null) {
                runCatching {
                    service.cancelNotification(key)
                }
            }
        }
    }
}
