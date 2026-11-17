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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.SettingsCard
import kotlinx.coroutines.launch

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
            SettingsCard {
                GestureSensitivityRow(settings = settings, repository = repository)
            }

            SettingsCard {
                HapticStrengthRow(settings = settings, repository = repository)
            }

            SettingsCard {
                FloatingWindowRow(settings = settings, repository = repository)
            }
        }

        PillSwipeCard(
            settings = settings,
            onEnabledChange = { isEnabled ->
                coroutineScope.launch {
                    repository?.setEnablePillSwipeActions(isEnabled)
                }
            },
            onGestureClick = { activeGestureDialog = it }
        )

        LongPressCard(
            action = getActionForGesture(ConfigurableGesture.PillLongPress, settings),
            onClick = { activeGestureDialog = ConfigurableGesture.PillLongPress }
        )

        ExpandedSwipeCard(
            settings = settings,
            onEnabledChange = { isEnabled ->
                coroutineScope.launch {
                    repository?.setEnableSwipeActions(isEnabled)
                }
            },
            onGestureClick = { activeGestureDialog = it }
        )

        GestureReferenceCard(settings = settings)
    }

    if (showTour) NewUserTour(settings = settings, onFinish = { showTour = false })

    activeGestureDialog?.let { gesture ->
        GestureActionDialog(
            gesture = gesture,
            currentAction = getActionForGesture(gesture, settings),
            onSelect = { action ->
                saveActionForGesture(gesture, action, repository, coroutineScope)
                activeGestureDialog = null
            },
            onDismiss = { activeGestureDialog = null }
        )
    }
}

@Composable
internal fun SwipeDisabledNotice(text: String) {
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
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun GestureActionDialog(
    gesture: ConfigurableGesture,
    currentAction: SwipeAction,
    onSelect: (SwipeAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                                onClick = { onSelect(action) },
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun LongPressCard(
    action: SwipeAction,
    onClick: () -> Unit
) {
    val isOff = action == SwipeAction.None
    SettingsCard {
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
                    color = (if (isOff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        .copy(alpha = 0.12f)
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
