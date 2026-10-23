/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.expanded.trySendFirstAction
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics

/**
 * Expand and collapse buzz when the island's state actually changes, so the Expand/Collapse swipe actions must not
 * add a second buzz of their own.
 */
internal fun swipeNeedsOwnHaptic(action: SwipeAction): Boolean =
    action != SwipeAction.None && action != SwipeAction.Expand && action != SwipeAction.Collapse

private fun trySkipMedia(
    context: android.content.Context?,
    notification: IslandNotification?,
    forward: Boolean
): Boolean {
    if (context == null) return false

    // Direct notification action PendingIntent (e.g. Spotify, YouTube Music, podcasts)
    val actionSent = if (forward) {
        notification.trySendFirstAction(context, "next", "skip", "forward")
    } else {
        notification.trySendFirstAction(context, "previous", "prev", "rewind")
    }
    if (actionSent) return true

    // Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (forward) controller.transportControls.skipToNext() else controller.transportControls.skipToPrevious()
            true
        }.getOrDefault(false)
        if (success) return true
    }

    // Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        if (forward) {
            repo.sendCommand(SmartIslandCommand.SkipNext(notification?.packageName))
        } else {
            repo.sendCommand(SmartIslandCommand.SkipPrevious(notification?.packageName))
        }
    }

    // Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = if (forward) android.view.KeyEvent.KEYCODE_MEDIA_NEXT else android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }

    return true
}

private fun tryPlayPauseMedia(
    context: android.content.Context?,
    notification: IslandNotification?
) {
    if (context == null) return

    // Direct notification action PendingIntent
    val actionSent = notification.trySendFirstAction(context, "play", "pause", "resume", "toggle")
    if (actionSent) return

    // Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (notification.mediaIsPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        }.getOrDefault(false)
        if (success) return
    }

    // Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        repo.sendCommand(SmartIslandCommand.PlayPause(notification?.packageName))
    }

    // Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }
}

internal fun executeSwipeAction(
    action: SwipeAction,
    currentNotification: IslandNotification?,
    context: android.content.Context? = null,
    hapticStrength: HapticStrength = HapticStrength.NORMAL,
    onDismiss: () -> Unit,
    onDismissAll: () -> Unit,
    onToggle: () -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit,
    onPageSelected: (Int) -> Unit,
    notificationsSize: Int,
    currentIndex: Int,
    onSwitchEvent: (forward: Boolean) -> Unit = {},
    /**
     * Entry-skipping switch (all cards of one app count as one entry). Returns false when there is only one entry.
     * When null, the legacy one-card-at-a-time page change is used.
     */
    onSwitchNotification: ((forward: Boolean) -> Boolean)? = null,
    /** Expanded-island sibling switch; returns false when the current card has no sibling. */
    onSwitchSibling: (forward: Boolean) -> Boolean = { false },
    /** Long-press toggle: hide / show the collapsed pill. */
    onTogglePillHidden: () -> Unit = {}
) {
    if (context != null && swipeNeedsOwnHaptic(action)) {
        IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
    }
    when (action) {
        SwipeAction.DismissCurrent -> onDismiss()
        SwipeAction.DismissAll -> onDismissAll()
        SwipeAction.Collapse -> onToggle()
        SwipeAction.Expand -> onToggle()
        SwipeAction.OpenApp -> {
            if (currentNotification != null) {
                onOpenNotification(currentNotification)
            }
        }
        SwipeAction.FloatingWindow -> onOpenFloatingWindow()
        SwipeAction.NotificationShade -> onOpenNotificationShade()
        SwipeAction.NextPrevious, SwipeAction.NextNotification -> {
            if (onSwitchNotification != null) {
                if (!onSwitchNotification(true)) {
                    trySkipMedia(context, currentNotification, forward = true)
                }
            } else if (notificationsSize > 1) {
                val nextIndex = (currentIndex + 1) % notificationsSize
                onPageSelected(nextIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = true)
            }
        }
        SwipeAction.PreviousNotification -> {
            if (onSwitchNotification != null) {
                if (!onSwitchNotification(false)) {
                    trySkipMedia(context, currentNotification, forward = false)
                }
            } else if (notificationsSize > 1) {
                val prevIndex = (currentIndex - 1 + notificationsSize) % notificationsSize
                onPageSelected(prevIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = false)
            }
        }
        SwipeAction.NextEvent -> onSwitchEvent(true)
        SwipeAction.PreviousEvent -> onSwitchEvent(false)
        // No sibling: nothing happens (never a track skip, never a jump to another app).
        SwipeAction.NextSibling -> { onSwitchSibling(true) }
        SwipeAction.PreviousSibling -> { onSwitchSibling(false) }
        SwipeAction.NextTrack -> {
            trySkipMedia(context, currentNotification, forward = true)
        }
        SwipeAction.PreviousTrack -> {
            trySkipMedia(context, currentNotification, forward = false)
        }
        SwipeAction.PlayPause -> {
            tryPlayPauseMedia(context, currentNotification)
        }
        SwipeAction.TogglePill -> onTogglePillHidden()
        SwipeAction.None -> {
        }
    }
}
