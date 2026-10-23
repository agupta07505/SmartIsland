/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.executeSwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillGestureActionTest {

    @Test
    fun nextOrPreviousNotificationDoesNotDismissOnSingleNotification() {
        var dismissed = false
        var pageSelected = -1

        executeSwipeAction(
            action = SwipeAction.NextNotification,
            currentNotification = null,
            context = null,
            onDismiss = { dismissed = true },
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { pageSelected = it },
            notificationsSize = 1,
            currentIndex = 0
        )

        assertFalse("Swipe NextNotification on single notification must not dismiss", dismissed)
        assertEquals(-1, pageSelected)

        executeSwipeAction(
            action = SwipeAction.PreviousNotification,
            currentNotification = null,
            context = null,
            onDismiss = { dismissed = true },
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { pageSelected = it },
            notificationsSize = 1,
            currentIndex = 0
        )

        assertFalse("Swipe PreviousNotification on single notification must not dismiss", dismissed)
        assertEquals(-1, pageSelected)
    }

    @Test
    fun dismissCurrentCallsOnDismiss() {
        var dismissed = false

        executeSwipeAction(
            action = SwipeAction.DismissCurrent,
            currentNotification = null,
            context = null,
            onDismiss = { dismissed = true },
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = {},
            notificationsSize = 1,
            currentIndex = 0
        )

        assertTrue("Swipe DismissCurrent must trigger onDismiss", dismissed)
    }

    @Test
    fun nextNotificationCyclesIndexWhenMultipleNotificationsExist() {
        var selectedPage = -1

        executeSwipeAction(
            action = SwipeAction.NextNotification,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { selectedPage = it },
            notificationsSize = 3,
            currentIndex = 0
        )

        assertEquals(1, selectedPage)

        executeSwipeAction(
            action = SwipeAction.PreviousNotification,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { selectedPage = it },
            notificationsSize = 3,
            currentIndex = 0
        )

        assertEquals(2, selectedPage)
    }

    @Test
    fun expandActionTriggersToggle() {
        var toggled = false

        executeSwipeAction(
            action = SwipeAction.Expand,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = { toggled = true },
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = {},
            notificationsSize = 1,
            currentIndex = 0
        )

        assertTrue("Swipe Expand must trigger onToggle", toggled)
    }

    @Test
    fun nextNotificationCyclesEndlesslyBothDirections() {
        var selectedPage = -1

        // 2 notifications, currently at index 1 -> swipe next should wrap to index 0
        executeSwipeAction(
            action = SwipeAction.NextNotification,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { selectedPage = it },
            notificationsSize = 2,
            currentIndex = 1
        )
        assertEquals(0, selectedPage)

        // 2 notifications, currently at index 0 -> swipe prev should wrap to index 1
        executeSwipeAction(
            action = SwipeAction.PreviousNotification,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { selectedPage = it },
            notificationsSize = 2,
            currentIndex = 0
        )
        assertEquals(1, selectedPage)
    }

    @Test
    fun nextTrackAndPreviousTrackDoNotChangeNotificationIndex() {
        var selectedPage = -1

        executeSwipeAction(
            action = SwipeAction.NextTrack,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { selectedPage = it },
            notificationsSize = 3,
            currentIndex = 0
        )
        assertEquals("NextTrack must not switch notifications", -1, selectedPage)

        executeSwipeAction(
            action = SwipeAction.PreviousTrack,
            currentNotification = null,
            context = null,
            onDismiss = {},
            onDismissAll = {},
            onToggle = {},
            onOpenNotification = {},
            onOpenFloatingWindow = {},
            onOpenNotificationShade = {},
            onPageSelected = { selectedPage = it },
            notificationsSize = 3,
            currentIndex = 1
        )
        assertEquals("PreviousTrack must not switch notifications", -1, selectedPage)
    }

    @Test
    fun eventSwitchActionsAskForNextOrPreviousEvent() {
        val requests = mutableListOf<Boolean>()
        var pageSelected = -1
        for (action in listOf(SwipeAction.NextEvent, SwipeAction.PreviousEvent)) {
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
                onPageSelected = { pageSelected = it },
                notificationsSize = 3,
                currentIndex = 0,
                onSwitchEvent = { requests += it }
            )
        }
        assertEquals(listOf(true, false), requests)
        assertEquals("Event switching is the view model's job, not a plain page change", -1, pageSelected)
    }

    private fun run(
        action: SwipeAction,
        onPageSelected: (Int) -> Unit = {},
        onSwitchNotification: ((Boolean) -> Boolean)? = null,
        onSwitchSibling: (Boolean) -> Boolean = { false }
    ) = executeSwipeAction(
        action = action,
        currentNotification = null,
        context = null,
        onDismiss = {},
        onDismissAll = {},
        onToggle = {},
        onOpenNotification = {},
        onOpenFloatingWindow = {},
        onOpenNotificationShade = {},
        onPageSelected = onPageSelected,
        notificationsSize = 4,
        currentIndex = 1,
        onSwitchNotification = onSwitchNotification,
        onSwitchSibling = onSwitchSibling
    )

    @Test
    fun nextAndPreviousNotificationUseEntrySwitchWhenProvided() {
        val requests = mutableListOf<Boolean>()
        var pageSelected = -1
        run(SwipeAction.NextNotification, { pageSelected = it }, { requests += it; true })
        run(SwipeAction.PreviousNotification, { pageSelected = it }, { requests += it; true })
        run(SwipeAction.NextPrevious, { pageSelected = it }, { requests += it; true })
        assertEquals(listOf(true, false, true), requests)
        assertEquals("Entry switching must not fall back to a plain page change", -1, pageSelected)
    }

    @Test
    fun entrySwitchReturningFalseDoesNotChangePageEither() {
        var pageSelected = -1
        run(SwipeAction.NextNotification, { pageSelected = it }, { false })
        run(SwipeAction.PreviousNotification, { pageSelected = it }, { false })
        assertEquals(-1, pageSelected)
    }

    @Test
    fun siblingActionsAskForNextOrPreviousSiblingOnly() {
        val siblingRequests = mutableListOf<Boolean>()
        var pageSelected = -1
        var entryCalls = 0
        for (action in listOf(SwipeAction.NextSibling, SwipeAction.PreviousSibling)) {
            run(
                action,
                onPageSelected = { pageSelected = it },
                onSwitchNotification = { entryCalls++; true },
                onSwitchSibling = { siblingRequests += it; true }
            )
        }
        assertEquals(listOf(true, false), siblingRequests)
        assertEquals(0, entryCalls)
        assertEquals(-1, pageSelected)
    }

    @Test
    fun siblingActionWithoutSiblingDoesNothing() {
        var pageSelected = -1
        run(SwipeAction.NextSibling, { pageSelected = it }, { true }, { false })
        assertEquals(-1, pageSelected)
    }

    @Test
    fun siblingActionsSurviveIdRoundTrip() {
        assertEquals(SwipeAction.NextSibling, SwipeAction.fromId("NextSibling"))
        assertEquals(SwipeAction.PreviousSibling, SwipeAction.fromId("previoussibling"))
    }

    @Test
    fun togglePillCallsOnlyTheToggleCallback() {
        var toggled = 0
        var other = false
        executeSwipeAction(
            action = SwipeAction.TogglePill,
            currentNotification = null,
            context = null,
            onDismiss = { other = true },
            onDismissAll = { other = true },
            onToggle = { other = true },
            onOpenNotification = { other = true },
            onOpenFloatingWindow = { other = true },
            onOpenNotificationShade = { other = true },
            onPageSelected = { other = true },
            notificationsSize = 0,
            currentIndex = 0,
            onTogglePillHidden = { toggled++ }
        )
        assertEquals(1, toggled)
        assertFalse(other)
    }
}
