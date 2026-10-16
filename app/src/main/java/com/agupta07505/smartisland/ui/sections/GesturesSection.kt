/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import com.agupta07505.smartisland.ui.components.SmartIslandSwitch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.SwipeAction
import kotlinx.coroutines.launch

private enum class ConfigurableGesture(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val allowedActions: List<SwipeAction>,
    val isPillGesture: Boolean
) {
    // Pill (Collapsed) Gestures
    PillSwipeLeft(
        titleRes = R.string.pill_swipe_left_title,
        descRes = R.string.pill_swipe_left_desc,
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        allowedActions = listOf(
            SwipeAction.PreviousNotification,
            SwipeAction.NextNotification,
            SwipeAction.PreviousTrack,
            SwipeAction.NextTrack,
            SwipeAction.PlayPause,
            SwipeAction.DismissCurrent,
            SwipeAction.NotificationShade,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.None
        ),
        isPillGesture = true
    ),
    PillSwipeRight(
        titleRes = R.string.pill_swipe_right_title,
        descRes = R.string.pill_swipe_right_desc,
        icon = Icons.AutoMirrored.Rounded.ArrowForward,
        allowedActions = listOf(
            SwipeAction.NextNotification,
            SwipeAction.PreviousNotification,
            SwipeAction.NextTrack,
            SwipeAction.PreviousTrack,
            SwipeAction.PlayPause,
            SwipeAction.DismissCurrent,
            SwipeAction.NotificationShade,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.None
        ),
        isPillGesture = true
    ),
    PillSwipeUp(
        titleRes = R.string.pill_swipe_up_title,
        descRes = R.string.pill_swipe_up_desc,
        icon = Icons.Rounded.ArrowUpward,
        allowedActions = listOf(
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
            SwipeAction.Expand,
            SwipeAction.NotificationShade,
            SwipeAction.OpenApp,
            SwipeAction.FloatingWindow,
            SwipeAction.DismissCurrent,
            SwipeAction.None
        ),
        isPillGesture = true
    ),

    // Expanded Card Gestures
    ExpandedSwipeUp(
        titleRes = R.string.swipe_gesture_up_title,
        descRes = R.string.swipe_gesture_up_desc,
        icon = Icons.Rounded.ArrowUpward,
        allowedActions = listOf(
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
            SwipeAction.FloatingWindow,
            SwipeAction.NotificationShade,
            SwipeAction.DismissCurrent,
            SwipeAction.DismissAll,
            SwipeAction.Collapse,
            SwipeAction.OpenApp,
            SwipeAction.None
        ),
        isPillGesture = false
    )
}

@Composable
fun GesturesSection(
    settings: SmartIslandSettings = SmartIslandSettings(),
    repository: SmartIslandSettingsRepository? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeGestureDialog by remember { mutableStateOf<ConfigurableGesture?>(null) }

    val tabs = listOf(
        stringResource(R.string.gesture_1_tap_tab),
        stringResource(R.string.gesture_2_swipe_up_tab),
        stringResource(R.string.gesture_3_hold_swipe_up_tab),
        stringResource(R.string.gesture_4_swipe_down_tab),
        stringResource(R.string.gesture_5_swipe_horizontal_tab)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==================== CARD 1: PILL SWIPE ACTIONS (COLLAPSED) ====================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = null,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Master Toggle for Pill Swipes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (settings.enablePillSwipeActions)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.enablePillSwipeActions) Icons.Rounded.Swipe else Icons.Rounded.Block,
                                contentDescription = null,
                                tint = if (settings.enablePillSwipeActions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.enable_pill_swipe_gestures_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.enable_pill_swipe_gestures_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SmartIslandSwitch(
                        checked = settings.enablePillSwipeActions,
                        onCheckedChange = { isEnabled ->
                            coroutineScope.launch {
                                repository?.setEnablePillSwipeActions(isEnabled)
                            }
                        }
                    )
                }

                if (!settings.enablePillSwipeActions) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Swipe actions on the collapsed pill are disabled. Tapping still expands the pill.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    text = stringResource(R.string.pill_swipe_customization_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // List of 4 Pill Gestures
                val pillGestures = ConfigurableGesture.entries.filter { it.isPillGesture }
                pillGestures.forEachIndexed { index, gesture ->
                    val currentAction = getActionForGesture(gesture, settings)
                    val isActionDisabled = !settings.enablePillSwipeActions || currentAction == SwipeAction.None

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeGestureDialog = gesture }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    gesture.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            val gestureLabel = when (gesture) {
                                ConfigurableGesture.PillSwipeLeft -> "Swipe Left"
                                ConfigurableGesture.PillSwipeRight -> "Swipe Right"
                                ConfigurableGesture.PillSwipeUp -> "Swipe Up"
                                ConfigurableGesture.PillSwipeDown -> "Swipe Down"
                                else -> stringResource(gesture.titleRes)
                            }
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text(
                                    text = gestureLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isActionDisabled) {
                                        stringResource(R.string.swipe_action_none_desc)
                                    } else {
                                        getSwipeActionDescription(currentAction)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Current Action Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActionDisabled) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isActionDisabled) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActionDisabled) Icons.Rounded.Block else getSwipeActionIcon(currentAction),
                                    contentDescription = null,
                                    tint = if (isActionDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (!settings.enablePillSwipeActions) {
                                        stringResource(R.string.swipe_action_none_title)
                                    } else {
                                        getSwipeActionTitle(currentAction)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isActionDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (index < pillGestures.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    }
                }
            }
        }

        // ==================== CARD 2: EXPANDED CARD SWIPE ACTIONS ====================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = null,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Master Toggle for Expanded Swipes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (settings.enableSwipeActions)
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.enableSwipeActions) Icons.Rounded.Tune else Icons.Rounded.Block,
                                contentDescription = null,
                                tint = if (settings.enableSwipeActions) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.enable_swipe_gestures_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.enable_swipe_gestures_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SmartIslandSwitch(
                        checked = settings.enableSwipeActions,
                        onCheckedChange = { isEnabled ->
                            coroutineScope.launch {
                                repository?.setEnableSwipeActions(isEnabled)
                            }
                        }
                    )
                }

                if (!settings.enableSwipeActions) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.swipe_action_none_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    text = stringResource(R.string.expanded_swipe_customization_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // List of 3 Expanded Gestures
                val expandedGestures = ConfigurableGesture.entries.filter { !it.isPillGesture }
                expandedGestures.forEachIndexed { index, gesture ->
                    val currentAction = getActionForGesture(gesture, settings)
                    val isActionDisabled = !settings.enableSwipeActions || currentAction == SwipeAction.None

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeGestureDialog = gesture }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    gesture.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            val gestureLabel = when (gesture) {
                                ConfigurableGesture.ExpandedSwipeUp -> "Swipe Up"
                                ConfigurableGesture.ExpandedSwipeHoldUp -> "Hold & Swipe Up"
                                ConfigurableGesture.ExpandedSwipeDown -> "Swipe Down"
                                else -> stringResource(gesture.titleRes)
                            }
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text(
                                    text = gestureLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isActionDisabled) {
                                        stringResource(R.string.swipe_action_none_desc)
                                    } else {
                                        getSwipeActionDescription(currentAction)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Current Action Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActionDisabled) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isActionDisabled) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActionDisabled) Icons.Rounded.Block else getSwipeActionIcon(currentAction),
                                    contentDescription = null,
                                    tint = if (isActionDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (!settings.enableSwipeActions) {
                                        stringResource(R.string.swipe_action_none_title)
                                    } else {
                                        getSwipeActionTitle(currentAction)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isActionDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (index < expandedGestures.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    }
                }
            }
        }

        // ==================== CARD 3: QUICK REFERENCE & TUTORIAL ====================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = null,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Gesture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.gestures_guide_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.gestures_guide_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Summary Clickable Gesture Badges
                val swipeUpAction = getActionForGesture(ConfigurableGesture.ExpandedSwipeUp, settings)
                val swipeHoldAction = getActionForGesture(ConfigurableGesture.ExpandedSwipeHoldUp, settings)
                val swipeDownAction = getActionForGesture(ConfigurableGesture.ExpandedSwipeDown, settings)
                val pillSwipeLeftAction = getActionForGesture(ConfigurableGesture.PillSwipeLeft, settings)
                val pillSwipeRightAction = getActionForGesture(ConfigurableGesture.PillSwipeRight, settings)

                val swipeUpSub = if (!settings.enableSwipeActions || swipeUpAction == SwipeAction.None) stringResource(R.string.swipe_action_none_title) else getSwipeActionTitle(swipeUpAction)
                val swipeHoldSub = if (!settings.enableSwipeActions || swipeHoldAction == SwipeAction.None) stringResource(R.string.swipe_action_none_title) else getSwipeActionTitle(swipeHoldAction)
                val swipeDownSub = if (!settings.enableSwipeActions || swipeDownAction == SwipeAction.None) stringResource(R.string.swipe_action_none_title) else getSwipeActionTitle(swipeDownAction)
                val pillHorizontalSub = if (!settings.enablePillSwipeActions || (pillSwipeLeftAction == SwipeAction.None && pillSwipeRightAction == SwipeAction.None)) {
                    stringResource(R.string.swipe_action_none_title)
                } else {
                    "${getSwipeActionTitle(pillSwipeLeftAction)} / ${getSwipeActionTitle(pillSwipeRightAction)}"
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GestureSummaryBadge(
                            icon = Icons.Rounded.TouchApp,
                            label = stringResource(R.string.gesture_1_tap_title),
                            sub = stringResource(R.string.gesture_1_tap_sub),
                            color = MaterialTheme.colorScheme.primary,
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        GestureSummaryBadge(
                            icon = Icons.Rounded.ArrowUpward,
                            label = stringResource(R.string.gesture_2_swipe_up_title),
                            sub = swipeUpSub,
                            color = if (swipeUpAction == SwipeAction.None || !settings.enableSwipeActions) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GestureSummaryBadge(
                            icon = Icons.Rounded.DeleteSweep,
                            label = stringResource(R.string.gesture_3_hold_swipe_up_title),
                            sub = swipeHoldSub,
                            color = if (swipeHoldAction == SwipeAction.None || !settings.enableSwipeActions) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        GestureSummaryBadge(
                            icon = Icons.Rounded.ArrowDownward,
                            label = stringResource(R.string.gesture_4_swipe_down_title),
                            sub = swipeDownSub,
                            color = if (swipeDownAction == SwipeAction.None || !settings.enableSwipeActions) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            isSelected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GestureSummaryBadge(
                        icon = Icons.Rounded.Swipe,
                        label = "5. Swipe Left / Right",
                        sub = pillHorizontalSub,
                        color = if ((pillSwipeLeftAction == SwipeAction.None && pillSwipeRightAction == SwipeAction.None) || !settings.enablePillSwipeActions) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                        isSelected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Scrollable Tabs for Selecting Each Gesture Guide
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Detailed Text Step-by-Step Instructions for Selected Gesture
        when (selectedTab) {
            0 -> GestureDetailCard(
                gestureNumber = "1 / 5",
                gestureName = "Tap Island",
                actionBadge = stringResource(R.string.gesture_1_tap_sub),
                badgeColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Rounded.TouchApp,
                overview = stringResource(R.string.gesture_1_tap_desc),
                steps = listOf(
                    "Touch Position: Tap once anywhere on the black collapsed island pill at the top of your screen.",
                    "Finger Motion: Tap cleanly without dragging or swiping.",
                    "Visual Response: The pill smoothly expands into the full card showing notification details, action buttons, and media scrubber.",
                    "To Collapse Back: Tap anywhere on the empty screen background or tap outside the expanded card to collapse it back into the pill."
                ),
                proTip = "When 'Hide when idle' is enabled, tapping the camera cutout region still awakens the pill and opens your favorite app shortcuts."
            )
            1 -> {
                val action = getActionForGesture(ConfigurableGesture.ExpandedSwipeUp, settings)
                val isOff = !settings.enableSwipeActions || action == SwipeAction.None
                GestureDetailCard(
                    gestureNumber = "2 / 5",
                    gestureName = "Swipe Up",
                    actionBadge = if (isOff) stringResource(R.string.swipe_action_none_title) else getSwipeActionTitle(action),
                    badgeColor = if (isOff) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.ArrowUpward,
                    overview = if (isOff) stringResource(R.string.swipe_action_none_desc) else stringResource(R.string.gesture_2_swipe_up_desc),
                    steps = listOf(
                        "Touch Position: Touch anywhere on the expanded notification card.",
                        "Finger Motion: Quickly flick or swipe your finger upward toward the top bezel of your device.",
                        "Threshold: Drag upward by at least 35dp and release your finger.",
                        "Current Action: ${if (isOff) "Disabled (no action will trigger)" else getSwipeActionTitle(action) + " — " + getSwipeActionDescription(action)}"
                    ),
                    proTip = "You can customize or disable this action at any time in the 'Expanded Card Swipe Actions' section above."
                )
            }
            2 -> {
                val action = getActionForGesture(ConfigurableGesture.ExpandedSwipeHoldUp, settings)
                val isOff = !settings.enableSwipeActions || action == SwipeAction.None
                GestureDetailCard(
                    gestureNumber = "3 / 5",
                    gestureName = "Hold & Swipe Up",
                    actionBadge = if (isOff) stringResource(R.string.swipe_action_none_title) else getSwipeActionTitle(action),
                    badgeColor = if (isOff) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.DeleteSweep,
                    overview = if (isOff) stringResource(R.string.swipe_action_none_desc) else stringResource(R.string.gesture_3_hold_swipe_up_desc),
                    steps = listOf(
                        "Touch Position: Touch and hold your finger on the expanded island card.",
                        "Hold Duration: Keep your finger down for 300ms until you feel a distinct haptic vibration pulse.",
                        "Finger Motion: As soon as you feel the vibration, immediately swipe your finger upward toward the top of the screen and release.",
                        "Current Action: ${if (isOff) "Disabled (no action will trigger)" else getSwipeActionTitle(action) + " — " + getSwipeActionDescription(action)}"
                    ),
                    proTip = "The haptic vibration confirms that the hold mode is engaged. You can configure this to clear all, collapse, or disable it completely."
                )
            }
            3 -> {
                val action = getActionForGesture(ConfigurableGesture.ExpandedSwipeDown, settings)
                val isOff = !settings.enableSwipeActions || action == SwipeAction.None
                GestureDetailCard(
                    gestureNumber = "4 / 5",
                    gestureName = "Swipe Down",
                    actionBadge = if (isOff) stringResource(R.string.swipe_action_none_title) else getSwipeActionTitle(action),
                    badgeColor = if (isOff) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.ArrowDownward,
                    overview = if (isOff) stringResource(R.string.swipe_action_none_desc) else stringResource(R.string.gesture_4_swipe_down_desc),
                    steps = listOf(
                        "Touch Position: Touch the expanded notification card.",
                        "Finger Motion: Drag or swipe downward toward the center of your screen by at least 35dp.",
                        "Release: Release your finger once the downward drag threshold is reached.",
                        "Current Action: ${if (isOff) "Disabled (no action will trigger)" else getSwipeActionTitle(action) + " — " + getSwipeActionDescription(action)}"
                    ),
                    proTip = "Choose between Floating Window, Notification Shade, Dismissing, or disabling it to prevent accidental window launches."
                )
            }
            4 -> {
                val leftAction = getActionForGesture(ConfigurableGesture.PillSwipeLeft, settings)
                val rightAction = getActionForGesture(ConfigurableGesture.PillSwipeRight, settings)
                val isLeftOff = !settings.enablePillSwipeActions || leftAction == SwipeAction.None
                val isRightOff = !settings.enablePillSwipeActions || rightAction == SwipeAction.None
                val allOff = isLeftOff && isRightOff
                GestureDetailCard(
                    gestureNumber = "5 / 5",
                    gestureName = "Swipe Left / Right",
                    actionBadge = if (allOff) stringResource(R.string.swipe_action_none_title) else "Left / Right",
                    badgeColor = if (allOff) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.Swipe,
                    overview = if (allOff) stringResource(R.string.swipe_action_none_desc) else "Flick or drag horizontally across the collapsed pill to switch between active notifications or media tracks.",
                    steps = listOf(
                        "Touch Position: Place your finger on the small collapsed island pill at the top of your screen.",
                        "Finger Motion: Flick or drag horizontally to the left or to the right.",
                        "Swipe Left Action: ${if (isLeftOff) "Disabled" else getSwipeActionTitle(leftAction) + " — " + getSwipeActionDescription(leftAction)}",
                        "Swipe Right Action: ${if (isRightOff) "Disabled" else getSwipeActionTitle(rightAction) + " — " + getSwipeActionDescription(rightAction)}"
                    ),
                    proTip = "You can customize Left and Right swipes separately in the Collapsed Pill Swipe Actions card above (e.g. Previous/Next Track or Previous/Next Notification)."
                )
            }
        }
    }

    // Action Selection Dialog
    activeGestureDialog?.let { gesture ->
        val currentAction = getActionForGesture(gesture, settings)
        AlertDialog(
            onDismissRequest = { activeGestureDialog = null },
            icon = {
                Icon(
                    imageVector = gesture.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.swipe_action_dialog_title, stringResource(gesture.titleRes)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    gesture.allowedActions.forEach { action ->
                        val isSelected = currentAction == action
                        val isNone = action == SwipeAction.None

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        saveActionForGesture(gesture, action, repository, coroutineScope)
                                        activeGestureDialog = null
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isNone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isNone)
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getSwipeActionIcon(action),
                                    contentDescription = null,
                                    tint = if (isNone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getSwipeActionTitle(action),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isNone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = getSwipeActionDescription(action),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeGestureDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

private fun getActionForGesture(gesture: ConfigurableGesture, settings: SmartIslandSettings): SwipeAction = when (gesture) {
    ConfigurableGesture.PillSwipeLeft -> SwipeAction.fromId(settings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
    ConfigurableGesture.PillSwipeRight -> SwipeAction.fromId(settings.pillSwipeRightAction, SwipeAction.NextNotification)
    ConfigurableGesture.PillSwipeUp -> SwipeAction.fromId(settings.pillSwipeUpAction, SwipeAction.DismissCurrent)
    ConfigurableGesture.PillSwipeDown -> SwipeAction.fromId(settings.pillSwipeDownAction, SwipeAction.Expand)
    ConfigurableGesture.ExpandedSwipeUp -> SwipeAction.fromId(settings.swipeUpAction, SwipeAction.DismissCurrent)
    ConfigurableGesture.ExpandedSwipeHoldUp -> SwipeAction.fromId(settings.swipeHoldUpAction, SwipeAction.DismissAll)
    ConfigurableGesture.ExpandedSwipeDown -> SwipeAction.fromId(settings.swipeDownAction, SwipeAction.FloatingWindow)
}

private fun saveActionForGesture(
    gesture: ConfigurableGesture,
    action: SwipeAction,
    repository: SmartIslandSettingsRepository?,
    coroutineScope: kotlinx.coroutines.CoroutineScope
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
        }
    }
}

@Composable
private fun getSwipeActionTitle(action: SwipeAction): String = when (action) {
    SwipeAction.DismissCurrent -> stringResource(R.string.swipe_action_dismiss_current_title)
    SwipeAction.DismissAll -> stringResource(R.string.swipe_action_dismiss_all_title)
    SwipeAction.Collapse -> stringResource(R.string.swipe_action_collapse_title)
    SwipeAction.Expand -> stringResource(R.string.swipe_action_expand_title)
    SwipeAction.OpenApp -> stringResource(R.string.swipe_action_open_app_title)
    SwipeAction.FloatingWindow -> stringResource(R.string.swipe_action_floating_window_title)
    SwipeAction.NotificationShade -> stringResource(R.string.swipe_action_notification_shade_title)
    SwipeAction.NextPrevious, SwipeAction.NextNotification -> stringResource(R.string.swipe_action_next_notification_title)
    SwipeAction.PreviousNotification -> stringResource(R.string.swipe_action_prev_notification_title)
    SwipeAction.NextTrack -> stringResource(R.string.swipe_action_next_track_title)
    SwipeAction.PreviousTrack -> stringResource(R.string.swipe_action_prev_track_title)
    SwipeAction.PlayPause -> stringResource(R.string.swipe_action_play_pause_title)
    SwipeAction.None -> stringResource(R.string.swipe_action_none_title)
}

@Composable
private fun getSwipeActionDescription(action: SwipeAction): String = when (action) {
    SwipeAction.DismissCurrent -> stringResource(R.string.swipe_action_dismiss_current_desc)
    SwipeAction.DismissAll -> stringResource(R.string.swipe_action_dismiss_all_desc)
    SwipeAction.Collapse -> stringResource(R.string.swipe_action_collapse_desc)
    SwipeAction.Expand -> stringResource(R.string.swipe_action_expand_desc)
    SwipeAction.OpenApp -> stringResource(R.string.swipe_action_open_app_desc)
    SwipeAction.FloatingWindow -> stringResource(R.string.swipe_action_floating_window_desc)
    SwipeAction.NotificationShade -> stringResource(R.string.swipe_action_notification_shade_desc)
    SwipeAction.NextPrevious, SwipeAction.NextNotification -> stringResource(R.string.swipe_action_next_notification_desc)
    SwipeAction.PreviousNotification -> stringResource(R.string.swipe_action_prev_notification_desc)
    SwipeAction.NextTrack -> stringResource(R.string.swipe_action_next_track_desc)
    SwipeAction.PreviousTrack -> stringResource(R.string.swipe_action_prev_track_desc)
    SwipeAction.PlayPause -> stringResource(R.string.swipe_action_play_pause_desc)
    SwipeAction.None -> stringResource(R.string.swipe_action_none_desc)
}

private fun getSwipeActionIcon(action: SwipeAction): ImageVector = when (action) {
    SwipeAction.DismissCurrent -> Icons.Rounded.Close
    SwipeAction.DismissAll -> Icons.Rounded.DeleteSweep
    SwipeAction.Collapse -> Icons.Rounded.ExpandLess
    SwipeAction.Expand -> Icons.Rounded.ExpandMore
    SwipeAction.OpenApp -> Icons.AutoMirrored.Rounded.OpenInNew
    SwipeAction.FloatingWindow -> Icons.Rounded.PictureInPicture
    SwipeAction.NotificationShade -> Icons.Rounded.Notifications
    SwipeAction.NextPrevious, SwipeAction.NextNotification -> Icons.AutoMirrored.Rounded.ArrowForward
    SwipeAction.PreviousNotification -> Icons.AutoMirrored.Rounded.ArrowBack
    SwipeAction.NextTrack -> Icons.Rounded.SkipNext
    SwipeAction.PreviousTrack -> Icons.Rounded.SkipPrevious
    SwipeAction.PlayPause -> Icons.Rounded.PlayArrow
    SwipeAction.None -> Icons.Rounded.Block
}

@Composable
private fun GestureSummaryBadge(
    icon: ImageVector,
    label: String,
    sub: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.08f),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
                Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GestureDetailCard(
    gestureNumber: String,
    gestureName: String,
    actionBadge: String,
    badgeColor: Color,
    icon: ImageVector,
    overview: String,
    steps: List<String>,
    proTip: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = null,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = gestureNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                        Text(
                            text = gestureName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = actionBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Overview
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Step-by-Step Instructions
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "How to Perform This Gesture:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Pro Tip Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = "Pro Tip",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PRO TIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = proTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
