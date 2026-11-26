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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.glass.Switch

@Composable
internal fun PillSwipeCard(
    settings: SmartIslandSettings,
    onEnabledChange: (Boolean) -> Unit,
    onGestureClick: (ConfigurableGesture) -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PillSwipeHeader(settings = settings, onEnabledChange = onEnabledChange)

            if (!settings.enablePillSwipeActions) {
                SwipeDisabledNotice(text = "Swipe actions on the collapsed pill are disabled. Tapping still expands the pill.")
            }

            SettingsDivider(0.3f)

            Text(
                text = stringResource(R.string.pill_swipe_customization_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val pillGestures = ConfigurableGesture.entries.filter { it.isPillGesture && !it.isLongPress
                && it != ConfigurableGesture.PillSwipeDown }
            pillGestures.forEachIndexed { index, gesture ->
                PillGestureRow(
                    gesture = gesture,
                    settings = settings,
                    onClick = { onGestureClick(gesture) }
                )

                if (index < pillGestures.size - 1) {
                    SettingsDivider(0.15f)
                }
            }
        }
    }
}

@Composable
private fun PillSwipeHeader(settings: SmartIslandSettings, onEnabledChange: (Boolean) -> Unit) {
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
                SettingsGroupTitle(stringResource(R.string.enable_pill_swipe_gestures_title))
                SettingsDescription(stringResource(R.string.enable_pill_swipe_gestures_desc))
            }
        }
        Switch(
            checked = settings.enablePillSwipeActions,
            onCheckedChange = onEnabledChange
        )
    }
}

@Composable
private fun PillGestureRow(gesture: ConfigurableGesture, settings: SmartIslandSettings, onClick: () -> Unit) {
    val currentAction = getActionForGesture(gesture, settings)
    val isActionDisabled = !settings.enablePillSwipeActions || currentAction == SwipeAction.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
}

@Composable
internal fun ExpandedSwipeCard(
    settings: SmartIslandSettings,
    onEnabledChange: (Boolean) -> Unit,
    onGestureClick: (ConfigurableGesture) -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ExpandedSwipeHeader(settings = settings, onEnabledChange = onEnabledChange)

            if (!settings.enableSwipeActions) {
                SwipeDisabledNotice(text = stringResource(R.string.swipe_action_none_desc))
            }

            SettingsDivider(0.3f)

            Text(
                text = stringResource(R.string.expanded_swipe_customization_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val expandedGestures = ConfigurableGesture.entries.filter { !it.isPillGesture && !it.isLongPress
                && it != ConfigurableGesture.ExpandedSwipeDown }
            expandedGestures.forEachIndexed { index, gesture ->
                ExpandedGestureRow(
                    gesture = gesture,
                    settings = settings,
                    onClick = { onGestureClick(gesture) }
                )

                if (index < expandedGestures.size - 1) {
                    SettingsDivider(0.15f)
                }
            }
        }
    }
}

@Composable
private fun ExpandedSwipeHeader(settings: SmartIslandSettings, onEnabledChange: (Boolean) -> Unit) {
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
                SettingsGroupTitle(stringResource(R.string.enable_swipe_gestures_title))
                SettingsDescription(stringResource(R.string.enable_swipe_gestures_desc))
            }
        }
        Switch(
            checked = settings.enableSwipeActions,
            onCheckedChange = onEnabledChange
        )
    }
}

@Composable
private fun ExpandedGestureRow(gesture: ConfigurableGesture, settings: SmartIslandSettings, onClick: () -> Unit) {
    val currentAction = getActionForGesture(gesture, settings)
    val isActionDisabled = !settings.enableSwipeActions || currentAction == SwipeAction.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
}
