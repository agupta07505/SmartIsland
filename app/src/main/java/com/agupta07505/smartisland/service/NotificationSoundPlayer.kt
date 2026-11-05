/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.util.AppEventRules
import com.agupta07505.smartisland.util.runCatchingLogged

private const val TAG = "SmartIslandNotificationListener"
private const val SOUND_DEBOUNCE_MS = 1200L

internal class NotificationSoundPlayer(
    private val context: Context,
    private val settings: () -> SmartIslandSettings,
    private val ranking: () -> NotificationListenerService.RankingMap?,
    private val blockedByDoNotDisturb: (StatusBarNotification) -> Boolean
) {
    @Volatile private var lastPlayedMs = 0L

    fun reset() {
        lastPlayedMs = 0L
    }

    fun play(sbn: StatusBarNotification) {
        if (!AppEventRules.maySound(settings().disabledSoundPackages, sbn.packageName)) return
        if (blockedByDoNotDisturb(sbn)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayedMs < SOUND_DEBOUNCE_MS) return
        lastPlayedMs = now
        runCatchingLogged(TAG, "Failed to play notification sound for ${sbn.packageName}") {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null || audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

            val notification = sbn.notification
            var soundUri: Uri? = null
            var audioAttributes: AudioAttributes? = null

            val channelRanking = NotificationListenerService.Ranking()
            if (ranking()?.getRanking(sbn.key, channelRanking) == true) {
                val channel = channelRanking.channel
                if (channel != null) {
                    if (channel.importance < NotificationManager.IMPORTANCE_DEFAULT) return
                    val channelSound = channel.sound
                    if (channelSound == null || channelSound == Uri.EMPTY || channelSound.toString().isEmpty()) return
                    soundUri = channelSound
                    audioAttributes = channel.audioAttributes
                }
            }

            if (soundUri == null) {
                @Suppress("DEPRECATION")
                val payloadSound = notification.sound
                @Suppress("DEPRECATION")
                val defaults = notification.defaults
                if (payloadSound != null && payloadSound != Uri.EMPTY && payloadSound.toString().isNotEmpty()) {
                    soundUri = payloadSound
                } else if ((defaults and Notification.DEFAULT_SOUND) != 0) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                audioAttributes = notification.audioAttributes
            }
            if (soundUri == null) return

            var ringtone: Ringtone? = runCatchingLogged(TAG, "Notification sound could not be loaded") {
                RingtoneManager.getRingtone(context, soundUri)
            }
            if (ringtone == null) {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = runCatchingLogged(TAG, "Default notification sound could not be loaded") {
                    RingtoneManager.getRingtone(context, fallback)
                }
            }
            ringtone?.let {
                it.audioAttributes = audioAttributes ?: AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                it.play()
            }
        }
    }
}
