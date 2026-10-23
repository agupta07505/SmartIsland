/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.size
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.util.HapticStrength

/**
 * Runs a swipe / long-press action with the island's current callbacks. One place for the action logic, instead of a
 * copy in every gesture branch; see [executeSwipeAction] for what each action does.
 */
internal class IslandActionRunner(
    private val context: android.content.Context,
    private val cb: IslandCallbacks
) {
    fun run(action: SwipeAction, notification: IslandNotification?) {
        executeSwipeAction(
            action = action,
            currentNotification = notification,
            context = context,
            hapticStrength = HapticStrength.fromId(cb.currentSettings.hapticStrength),
            onDismiss = cb.currentOnDismiss,
            onDismissAll = cb.currentOnDismissAll,
            onToggle = cb.currentOnToggle,
            onOpenNotification = cb.currentOnOpenNotification,
            onOpenFloatingWindow = cb.currentOnOpenFloatingWindow,
            onOpenNotificationShade = cb.currentOnOpenNotificationShade,
            onPageSelected = cb.currentOnPageSelected,
            notificationsSize = cb.currentNotifications.size,
            currentIndex = cb.currentSelectedIndex,
            onSwitchEvent = cb.currentOnSwitchEvent,
            onSwitchNotification = cb.currentOnSwitchNotification,
            onSwitchSibling = cb.currentOnSwitchSibling
        )
    }

    /** Long press on a pill: no card, no context (the hold already buzzed), only the pill hide / show action works. */
    fun runLongPress(action: SwipeAction) {
        executeSwipeAction(
            action = action,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = {},
            notificationsSize = 0,
            currentIndex = 0,
            onTogglePillHidden = cb.currentOnTogglePillHidden
        )
    }
}
