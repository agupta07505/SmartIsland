/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.FIXED_EXPANDED_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.SwipeAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private fun horizontalPillActions(left: Boolean) = listOf(
    SwipeAction.NextEvent,
    SwipeAction.PreviousEvent,
    if (left) SwipeAction.PreviousNotification else SwipeAction.NextNotification,
    if (left) SwipeAction.NextNotification else SwipeAction.PreviousNotification,
    if (left) SwipeAction.PreviousTrack else SwipeAction.NextTrack,
    if (left) SwipeAction.NextTrack else SwipeAction.PreviousTrack,
    SwipeAction.PlayPause,
    SwipeAction.DismissCurrent,
    SwipeAction.NotificationShade,
    SwipeAction.OpenApp,
    SwipeAction.FloatingWindow,
    SwipeAction.None
)

internal enum class ConfigurableGesture(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val allowedActions: List<SwipeAction>,
    val isPillGesture: Boolean,
    /** Own category ("Long press"): never listed with the pill or expanded swipes. */
    val isLongPress: Boolean = false
) {
    PillSwipeLeft(
        titleRes = R.string.pill_swipe_left_title,
        descRes = R.string.pill_swipe_left_desc,
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        allowedActions = horizontalPillActions(left = true),
        isPillGesture = true
    ),
    PillSwipeRight(
        titleRes = R.string.pill_swipe_right_title,
        descRes = R.string.pill_swipe_right_desc,
        icon = Icons.AutoMirrored.Rounded.ArrowForward,
        allowedActions = horizontalPillActions(left = false),
        isPillGesture = true
    ),
    PillSwipeUp(
        titleRes = R.string.pill_swipe_up_title,
        descRes = R.string.pill_swipe_up_desc,
        icon = Icons.Rounded.ArrowUpward,
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
            SwipeAction.DismissCurrent,
            SwipeAction.DismissAll,
            SwipeAction.NotificationShade,
            SwipeAction.None
        ),
        isPillGesture = true
    ),
    PillSwipeDown(
        titleRes = R.string.pill_swipe_down_title,
        descRes = R.string.pill_swipe_down_desc,
        icon = Icons.Rounded.ExpandMore,
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
            SwipeAction.Expand,
            SwipeAction.NotificationShade,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.DismissCurrent,
            SwipeAction.None
        ),
        isPillGesture = true
    ),

    ExpandedSwipeUp(
        titleRes = R.string.swipe_gesture_up_title,
        descRes = R.string.swipe_gesture_up_desc,
        icon = Icons.Rounded.ArrowUpward,
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
            SwipeAction.NextSibling,
            SwipeAction.PreviousSibling,
            SwipeAction.DismissCurrent,
            SwipeAction.DismissAll,
            SwipeAction.Collapse,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.NotificationShade,
            SwipeAction.None
        ),
        isPillGesture = false
    ),
    ExpandedSwipeHoldUp(
        titleRes = R.string.swipe_gesture_hold_up_title,
        descRes = R.string.swipe_gesture_hold_up_desc,
        icon = Icons.Rounded.DeleteSweep,
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
            SwipeAction.NextSibling,
            SwipeAction.PreviousSibling,
            SwipeAction.DismissAll,
            SwipeAction.DismissCurrent,
            SwipeAction.Collapse,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.NotificationShade,
            SwipeAction.None
        ),
        isPillGesture = false
    ),
    ExpandedSwipeDown(
        titleRes = R.string.swipe_gesture_down_title,
        descRes = R.string.swipe_gesture_down_desc,
        icon = Icons.Rounded.ArrowDownward,
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
            SwipeAction.NextSibling,
            SwipeAction.PreviousSibling,
            SwipeAction.FloatingWindow,
            SwipeAction.NotificationShade,
            SwipeAction.DismissCurrent,
            SwipeAction.DismissAll,
            SwipeAction.Collapse,
            SwipeAction.OpenApp,
            SwipeAction.None
        ),
        isPillGesture = false
    ),
    ExpandedSwipeLeft(
        titleRes = R.string.swipe_gesture_left_title,
        descRes = R.string.swipe_gesture_left_desc,
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        allowedActions = listOf(
            SwipeAction.PreviousEvent,
            SwipeAction.NextEvent,
            SwipeAction.PreviousSibling,
            SwipeAction.NextSibling,
            SwipeAction.DismissCurrent,
            SwipeAction.DismissAll,
            SwipeAction.Collapse,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.NotificationShade,
            SwipeAction.None
        ),
        isPillGesture = false
    ),
    ExpandedSwipeRight(
        titleRes = R.string.swipe_gesture_right_title,
        descRes = R.string.swipe_gesture_right_desc,
        icon = Icons.AutoMirrored.Rounded.ArrowForward,
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
            SwipeAction.NextSibling,
            SwipeAction.PreviousSibling,
            SwipeAction.DismissCurrent,
            SwipeAction.DismissAll,
            SwipeAction.Collapse,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.NotificationShade,
            SwipeAction.None
        ),
        isPillGesture = false
    ),

    PillLongPress(
        titleRes = R.string.long_press_gesture_title,
        descRes = R.string.long_press_gesture_desc,
        icon = Icons.Rounded.TouchApp,
        allowedActions = listOf(
            SwipeAction.TogglePill,
            SwipeAction.None
        ),
        isPillGesture = false,
        isLongPress = true
    )
}

internal fun getActionForGesture(gesture: ConfigurableGesture,
        settings: SmartIslandSettings): SwipeAction = when (gesture) {
    ConfigurableGesture.PillSwipeLeft -> SwipeAction.fromId(settings.pillSwipeLeftAction,
        SwipeAction.PreviousNotification)
    ConfigurableGesture.PillSwipeRight -> SwipeAction.fromId(settings.pillSwipeRightAction,
        SwipeAction.NextNotification)
    ConfigurableGesture.PillSwipeUp -> SwipeAction.fromId(settings.pillSwipeUpAction, SwipeAction.DismissCurrent)
    ConfigurableGesture.PillSwipeDown -> FIXED_PILL_SWIPE_DOWN_ACTION
    ConfigurableGesture.ExpandedSwipeUp -> SwipeAction.fromId(settings.swipeUpAction, SwipeAction.DismissCurrent)
    ConfigurableGesture.ExpandedSwipeHoldUp -> SwipeAction.fromId(settings.swipeHoldUpAction, SwipeAction.DismissAll)
    ConfigurableGesture.ExpandedSwipeDown -> FIXED_EXPANDED_SWIPE_DOWN_ACTION
    ConfigurableGesture.ExpandedSwipeLeft -> SwipeAction.fromId(settings.swipeLeftAction, SwipeAction.None)
    ConfigurableGesture.ExpandedSwipeRight -> SwipeAction.fromId(settings.swipeRightAction, SwipeAction.None)
    ConfigurableGesture.PillLongPress -> SwipeAction.fromId(settings.longPressAction, SwipeAction.TogglePill)
}

internal fun saveActionForGesture(
    gesture: ConfigurableGesture,
    action: SwipeAction,
    repository: SmartIslandSettingsRepository?,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        when (gesture) {
            ConfigurableGesture.PillSwipeLeft -> repository?.setPillSwipeLeftAction(action.id)
            ConfigurableGesture.PillSwipeRight -> repository?.setPillSwipeRightAction(action.id)
            ConfigurableGesture.PillSwipeUp -> repository?.setPillSwipeUpAction(action.id)
            ConfigurableGesture.PillSwipeDown -> repository?.setPillSwipeDownAction(action.id)
            ConfigurableGesture.ExpandedSwipeUp -> repository?.setSwipeUpAction(action.id)
            ConfigurableGesture.ExpandedSwipeHoldUp -> repository?.setSwipeHoldUpAction(action.id)
            ConfigurableGesture.ExpandedSwipeDown -> repository?.setSwipeDownAction(action.id)
            ConfigurableGesture.ExpandedSwipeLeft -> repository?.setSwipeLeftAction(action.id)
            ConfigurableGesture.ExpandedSwipeRight -> repository?.setSwipeRightAction(action.id)
            ConfigurableGesture.PillLongPress -> repository?.setLongPressAction(action.id)
        }
    }
}

@Composable
internal fun getSwipeActionTitle(action: SwipeAction): String = when (action) {
    SwipeAction.DismissCurrent -> stringResource(R.string.swipe_action_dismiss_current_title)
    SwipeAction.DismissAll -> stringResource(R.string.swipe_action_dismiss_all_title)
    SwipeAction.Collapse -> stringResource(R.string.swipe_action_collapse_title)
    SwipeAction.Expand -> stringResource(R.string.swipe_action_expand_title)
    SwipeAction.OpenApp -> stringResource(R.string.swipe_action_open_app_title)
    SwipeAction.FloatingWindow -> stringResource(R.string.swipe_action_floating_window_title)
    SwipeAction.NotificationShade -> stringResource(R.string.swipe_action_notification_shade_title)
    SwipeAction.NextPrevious, SwipeAction.NextNotification ->
        stringResource(R.string.swipe_action_next_notification_title)
    SwipeAction.PreviousNotification -> stringResource(R.string.swipe_action_prev_notification_title)
    SwipeAction.NextEvent -> stringResource(R.string.swipe_action_next_event_title)
    SwipeAction.PreviousEvent -> stringResource(R.string.swipe_action_prev_event_title)
    SwipeAction.NextSibling -> stringResource(R.string.swipe_action_next_sibling_title)
    SwipeAction.PreviousSibling -> stringResource(R.string.swipe_action_prev_sibling_title)
    SwipeAction.NextTrack -> stringResource(R.string.swipe_action_next_track_title)
    SwipeAction.PreviousTrack -> stringResource(R.string.swipe_action_prev_track_title)
    SwipeAction.PlayPause -> stringResource(R.string.swipe_action_play_pause_title)
    SwipeAction.TogglePill -> stringResource(R.string.swipe_action_toggle_pill_title)
    SwipeAction.None -> stringResource(R.string.swipe_action_none_title)
}

@Composable
internal fun getSwipeActionDescription(action: SwipeAction): String = when (action) {
    SwipeAction.DismissCurrent -> stringResource(R.string.swipe_action_dismiss_current_desc)
    SwipeAction.DismissAll -> stringResource(R.string.swipe_action_dismiss_all_desc)
    SwipeAction.Collapse -> stringResource(R.string.swipe_action_collapse_desc)
    SwipeAction.Expand -> stringResource(R.string.swipe_action_expand_desc)
    SwipeAction.OpenApp -> stringResource(R.string.swipe_action_open_app_desc)
    SwipeAction.FloatingWindow -> stringResource(R.string.swipe_action_floating_window_desc)
    SwipeAction.NotificationShade -> stringResource(R.string.swipe_action_notification_shade_desc)
    SwipeAction.NextPrevious, SwipeAction.NextNotification ->
        stringResource(R.string.swipe_action_next_notification_desc)
    SwipeAction.PreviousNotification -> stringResource(R.string.swipe_action_prev_notification_desc)
    SwipeAction.NextEvent -> stringResource(R.string.swipe_action_next_event_desc)
    SwipeAction.PreviousEvent -> stringResource(R.string.swipe_action_prev_event_desc)
    SwipeAction.NextSibling -> stringResource(R.string.swipe_action_next_sibling_desc)
    SwipeAction.PreviousSibling -> stringResource(R.string.swipe_action_prev_sibling_desc)
    SwipeAction.NextTrack -> stringResource(R.string.swipe_action_next_track_desc)
    SwipeAction.PreviousTrack -> stringResource(R.string.swipe_action_prev_track_desc)
    SwipeAction.PlayPause -> stringResource(R.string.swipe_action_play_pause_desc)
    SwipeAction.TogglePill -> stringResource(R.string.swipe_action_toggle_pill_desc)
    SwipeAction.None -> stringResource(R.string.swipe_action_none_desc)
}

internal fun getSwipeActionIcon(action: SwipeAction): ImageVector = when (action) {
    SwipeAction.DismissCurrent -> Icons.Rounded.Close
    SwipeAction.DismissAll -> Icons.Rounded.DeleteSweep
    SwipeAction.Collapse -> Icons.Rounded.ExpandLess
    SwipeAction.Expand -> Icons.Rounded.ExpandMore
    SwipeAction.OpenApp -> Icons.AutoMirrored.Rounded.OpenInNew
    SwipeAction.FloatingWindow -> Icons.Rounded.PictureInPicture
    SwipeAction.NotificationShade -> Icons.Rounded.Notifications
    SwipeAction.NextPrevious, SwipeAction.NextNotification -> Icons.AutoMirrored.Rounded.ArrowForward
    SwipeAction.PreviousNotification -> Icons.AutoMirrored.Rounded.ArrowBack
    SwipeAction.NextEvent -> Icons.AutoMirrored.Rounded.ArrowForward
    SwipeAction.PreviousEvent -> Icons.AutoMirrored.Rounded.ArrowBack
    SwipeAction.NextSibling -> Icons.AutoMirrored.Rounded.ArrowForward
    SwipeAction.PreviousSibling -> Icons.AutoMirrored.Rounded.ArrowBack
    SwipeAction.NextTrack -> Icons.Rounded.SkipNext
    SwipeAction.PreviousTrack -> Icons.Rounded.SkipPrevious
    SwipeAction.PlayPause -> Icons.Rounded.PlayArrow
    SwipeAction.TogglePill -> Icons.Rounded.VisibilityOff
    SwipeAction.None -> Icons.Rounded.Block
}
