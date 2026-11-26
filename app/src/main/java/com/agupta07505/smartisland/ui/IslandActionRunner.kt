/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import androidx.compose.foundation.layout.size
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.util.HapticStrength

/**
 * Runs a swipe / long-press action with the island's current callbacks, 1 place for the action logic instead of a
 * copy per gesture branch, see [executeSwipeAction]
 */
internal class IslandActionRunner(
    private val context: Context,
    private val wiring: IslandWiring
) {
    fun run(action: SwipeAction, notification: IslandNotification?) {
        executeSwipeAction(
            action = action,
            currentNotification = notification,
            context = context,
            hapticStrength = HapticStrength.fromId(wiring.view.settings.hapticStrength),
            onDismiss = wiring.actions.card.dismiss,
            onDismissAll = wiring.actions.card.dismissAll,
            onToggle = wiring.actions.expand.toggle,
            onOpenNotification = wiring.actions.card.open,
            onOpenFloatingWindow = wiring.actions.system.openFloatingWindow,
            onOpenNotificationShade = wiring.actions.system.openNotificationShade,
            onPageSelected = wiring.actions.card.selectPage,
            notificationsSize = wiring.view.notifications.size,
            currentIndex = wiring.view.selectedIndex,
            onSwitchEvent = wiring.actions.card.switchEvent,
            onSwitchNotification = wiring.actions.card.switchNotification,
            onSwitchSibling = wiring.actions.sibling.switchSibling
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
            onTogglePillHidden = wiring.actions.idle.togglePillHidden
        )
    }
}
