/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

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
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.agupta07505.smartisland.model.FIXED_EXPANDED_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.SwipeAction
import kotlinx.coroutines.launch

private enum class ConfigurableGesture(
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
        allowedActions = listOf(
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
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
            SwipeAction.NextEvent,
            SwipeAction.PreviousEvent,
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

    // Long press category: one real function, hide / show the collapsed pill.
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

@Composable
fun GesturesSection(
    settings: SmartIslandSettings = SmartIslandSettings(),
    repository: SmartIslandSettingsRepository? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var showTour by rememberSaveable { mutableStateOf(false) }
    var activeGestureDialog by rememberSaveable { mutableStateOf<ConfigurableGesture?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NewUserTourCard(onStart = { showTour = true })

        if (repository != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                GestureSensitivityRow(settings = settings, repository = repository)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                HapticStrengthRow(settings = settings, repository = repository)
            }

            // Floating window: on / off for the white pull handle, and System or LMO Freeform.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                FloatingWindowRow(settings = settings, repository = repository)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.enable_pill_swipe_gestures_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val pillGestures = ConfigurableGesture.entries.filter { it.isPillGesture && !it.isLongPress && it != ConfigurableGesture.PillSwipeDown }
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
                                    fontWeight = FontWeight.Bold,
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

        LongPressCard(
            action = getActionForGesture(ConfigurableGesture.PillLongPress, settings),
            onClick = { activeGestureDialog = ConfigurableGesture.PillLongPress }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.enable_swipe_gestures_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val expandedGestures = ConfigurableGesture.entries.filter { !it.isPillGesture && !it.isLongPress && it != ConfigurableGesture.ExpandedSwipeDown }
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
                                ConfigurableGesture.ExpandedSwipeLeft -> "Swipe Left"
                                ConfigurableGesture.ExpandedSwipeRight -> "Swipe Right"
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
                                    fontWeight = FontWeight.Bold,
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

        GestureReferenceCard(settings = settings)
    }

    if (showTour) NewUserTour(settings = settings, onFinish = { showTour = false })

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
                    fontWeight = FontWeight.Bold
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
    ConfigurableGesture.PillSwipeDown -> FIXED_PILL_SWIPE_DOWN_ACTION
    ConfigurableGesture.ExpandedSwipeUp -> SwipeAction.fromId(settings.swipeUpAction, SwipeAction.DismissCurrent)
    ConfigurableGesture.ExpandedSwipeHoldUp -> SwipeAction.fromId(settings.swipeHoldUpAction, SwipeAction.DismissAll)
    ConfigurableGesture.ExpandedSwipeDown -> FIXED_EXPANDED_SWIPE_DOWN_ACTION
    ConfigurableGesture.ExpandedSwipeLeft -> SwipeAction.fromId(settings.swipeLeftAction, SwipeAction.None)
    ConfigurableGesture.ExpandedSwipeRight -> SwipeAction.fromId(settings.swipeRightAction, SwipeAction.None)
    ConfigurableGesture.PillLongPress -> SwipeAction.fromId(settings.longPressAction, SwipeAction.TogglePill)
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
    SwipeAction.NextPrevious, SwipeAction.NextNotification -> stringResource(R.string.swipe_action_next_notification_title)
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

@Composable
private fun LongPressCard(
    action: SwipeAction,
    onClick: () -> Unit
) {
    val isOff = action == SwipeAction.None
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.long_press_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.long_press_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
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
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.long_press_gesture_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = getSwipeActionDescription(action),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isOff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = getSwipeActionTitle(action),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
