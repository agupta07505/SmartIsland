/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import com.agupta07505.smartisland.util.isPlayingLike
import com.agupta07505.smartisland.util.runCatchingLogged

private const val TAG = "SmartIslandNotificationListener"

internal data class MediaInfo(
    val artwork: Bitmap?,
    val positionMs: Long?,
    val durationMs: Long?,
    val isPlaying: Boolean,
    val title: String? = null,
    val artist: String? = null
)

internal class NotificationMediaLookup(
    private val context: Context,
    private val listener: ComponentName
) {
    private val sessionManager: MediaSessionManager? by lazy {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    val activeControllers: List<MediaController>
        get() = runCatchingLogged(TAG, "GetActiveSessions failed") {
            sessionManager?.getActiveSessions(listener)
        } ?: emptyList()

    fun bestControllerFor(packageName: String): MediaController? {
        val matches = activeControllers.filter { it.packageName == packageName }
        return matches.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: matches.firstOrNull()
    }

    fun anyPlayingController(): MediaController? {
        val all = activeControllers
        return all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: all.firstOrNull()
    }

    fun findInfo(notification: Notification, packageName: String): MediaInfo? {
        notification.sessionController()?.info()?.let { return it }
        return bestControllerFor(packageName)?.info()
    }

    private fun Notification.sessionController(): MediaController? {
        val token = runCatchingLogged(TAG, "GetMediaSessionToken failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
            }
        } ?: return null
        return runCatchingLogged(TAG, "MediaController init failed") { MediaController(context, token) }
    }

    private fun MediaController.info(): MediaInfo {
        val metadata = metadata
        val state = playbackState
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        return MediaInfo(
            artwork = artwork,
            positionMs = state?.estimatedPosition(),
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 },
            isPlaying = isPlayingLike(state?.state),
            title = title,
            artist = artist
        )
    }

    private fun PlaybackState.estimatedPosition(): Long? {
        if (position < 0) return null
        if (state != PlaybackState.STATE_PLAYING) return position
        val elapsed = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        return (position + (elapsed * playbackSpeed).toLong()).coerceAtLeast(0L)
    }
}
