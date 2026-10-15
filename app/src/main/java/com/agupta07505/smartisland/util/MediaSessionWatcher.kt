/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * What a media session says right now. A media app re-posts its notification a second or two after a
 * track change, but the media session itself reports the change at once; the island follows the session.
 */
data class MediaSnapshot(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val artwork: Bitmap?,
    val durationMs: Long?,
    val positionMs: Long?,
    val isPlaying: Boolean,
    /** The session this snapshot came from; null when unknown (then cards are matched by package only). */
    val sessionToken: MediaSession.Token? = null
)

/**
 * True while a session is playing or only briefly between playing states (buffering, connecting, skipping).
 * Reading those transient states as "not playing" would drop the card's rank for a moment and make
 * the primary and secondary cutouts swap and swap back.
 */
fun isPlayingLike(state: Int?): Boolean = when (state) {
    PlaybackState.STATE_PLAYING,
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING,
    PlaybackState.STATE_FAST_FORWARDING,
    PlaybackState.STATE_REWINDING,
    PlaybackState.STATE_SKIPPING_TO_NEXT,
    PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
    PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> true
    else -> false
}

/** Pure: how a [MediaSnapshot] changes a stored Music card. Anything the session does not know stays as it was. */
object MediaCardPatch {
    /** True when this card is one the snapshot can update. */
    fun appliesTo(card: IslandNotification, snapshot: MediaSnapshot): Boolean {
        if (card.mode != IslandMode.Music || card.packageName != snapshot.packageName) return false
        // An app with two sessions: each session only updates its own card.
        val cardToken = card.mediaToken
        val snapToken = snapshot.sessionToken
        return cardToken == null || snapToken == null || cardToken == snapToken
    }

    fun apply(old: IslandNotification, s: MediaSnapshot): IslandNotification {
        val newTitle = s.title?.takeIf { it.isNotBlank() }
        val trackChanged = newTitle != null && old.title.isNotBlank() && newTitle != old.title
        return old.copy(
            title = newTitle ?: old.title,
            // A new track with no artist known yet must not keep the previous artist.
            text = s.artist?.takeIf { it.isNotBlank() } ?: if (trackChanged) "" else old.text,
            // Same for the picture: the previous track's cover is wrong; the notification brings the new one a moment later.
            largeIcon = s.artwork ?: if (trackChanged) null else old.largeIcon,
            mediaDurationMs = s.durationMs ?: if (trackChanged) null else old.mediaDurationMs,
            mediaPositionMs = s.positionMs ?: old.mediaPositionMs,
            mediaIsPlaying = s.isPlaying
        )
    }
}

/**
 * Listens to every active media session and reports a [MediaSnapshot] when its track or play state changes.
 * Needs notification-listener access (the same one the island already has). All callbacks run on the main thread; a
 * track change usually raises the metadata and the playback callbacks back to back, so they are merged into one report.
 */
class MediaSessionWatcher(
    context: Context,
    private val listener: ComponentName,
    private val onSnapshot: (MediaSnapshot) -> Unit
) {
    private class Watched(
        val controller: MediaController,
        var metadata: MediaMetadata?,
        var state: PlaybackState?
    ) {
        var callback: MediaController.Callback? = null
        var report: Runnable? = null
        /** What the island was last told, to tell a real pause from a blip. */
        var lastReportedPlaying = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val watched = HashMap<MediaSession.Token, Watched>()
    private var started = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        sync(controllers.orEmpty())
    }

    fun start() {
        if (started) return
        val mgr = manager ?: return
        runCatchingLogged(TAG, "Media session watcher could not start") {
            mgr.addOnActiveSessionsChangedListener(sessionsListener, listener, handler)
            started = true
            sync(mgr.getActiveSessions(listener))
        }
    }

    fun stop() {
        if (started) {
            runCatchingLogged(TAG, "Media session watcher could not stop") {
                manager?.removeOnActiveSessionsChangedListener(sessionsListener)
            }
        }
        started = false
        watched.values.toList().forEach { release(it) }
        watched.clear()
    }

    private fun sync(controllers: List<MediaController>) {
        val live = controllers.associateBy { it.sessionToken }
        watched.keys.filter { it !in live }.toList().forEach { token -> watched.remove(token)?.let { release(it) } }
        for ((token, controller) in live) {
            if (token in watched) continue
            val w = runCatchingLogged(TAG, "Media session could not be read") {
                // One read each when the session appears; afterwards the callbacks hand over every change.
                Watched(controller, controller.metadata, controller.playbackState)
            } ?: continue
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    w.metadata = metadata
                    scheduleReport(w)
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    w.state = state
                    scheduleReport(w)
                }

                override fun onSessionDestroyed() {
                    watched.remove(controller.sessionToken)?.let { release(it) }
                }
            }
            w.callback = callback
            runCatchingLogged(TAG, "Media session callback could not be registered") {
                controller.registerCallback(callback, handler)
            }
            watched[token] = w
        }
    }

    private fun release(w: Watched) {
        w.report?.let { handler.removeCallbacks(it) }
        w.report = null
        w.callback?.let { cb -> runCatchingLogged(TAG, "Media session callback could not be removed") { w.controller.unregisterCallback(cb) } }
        w.callback = null
    }

    private fun scheduleReport(w: Watched) {
        if (w.report != null) return
        val task = Runnable {
            w.report = null
            val snapshot = snapshotOf(w)
            w.lastReportedPlaying = snapshot.isPlaying
            onSnapshot(snapshot)
        }
        w.report = task
        // A drop out of "playing" waits a moment: track changes and audio-focus blips pass through
        // paused states for a second or less and must not reorder the cutouts.
        val dropping = w.lastReportedPlaying && !isPlayingLike(w.state?.state)
        handler.postDelayed(task, if (dropping) PAUSE_CONFIRM_MS else MERGE_MS)
    }

    private fun snapshotOf(w: Watched): MediaSnapshot {
        val md = w.metadata
        val st = w.state
        return MediaSnapshot(
            packageName = w.controller.packageName,
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            artwork = md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0L },
            positionMs = st?.let { estimatedPositionMs(it) },
            isPlaying = isPlayingLike(st?.state),
            sessionToken = w.controller.sessionToken
        )
    }

    private companion object {
        const val TAG = "MediaSessionWatcher"
        /** Metadata and playback callbacks of one track change arrive within a few ms of each other. */
        const val MERGE_MS = 30L
        /** How long a drop out of "playing" must hold before the island believes it. */
        const val PAUSE_CONFIRM_MS = 1200L
    }
}

/** Where a session's playhead is now, from its last report (no call into the app). Null when the position is unknown. */
fun estimatedPositionMs(state: PlaybackState): Long? {
    if (state.position < 0) return null
    if (state.state != PlaybackState.STATE_PLAYING) return state.position
    val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
    return (state.position + (elapsed * state.playbackSpeed).toLong()).coerceAtLeast(0L)
}
