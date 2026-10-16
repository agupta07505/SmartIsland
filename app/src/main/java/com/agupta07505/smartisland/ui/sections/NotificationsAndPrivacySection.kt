/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.sections

import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.bounceClick
import kotlin.math.roundToInt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.OemDeviceType
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsAndPrivacySection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val detectedDevice = remember { OemDeviceRules.detectCurrentDevice() }
    val effectiveDevice = remember(settings.deviceType) { OemDeviceRules.resolveEffectiveDevice(settings.deviceType) }
    var isDeviceMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group 0: Device Profile & OEM Compatibility Rules
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.oem_rules_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.oem_rules_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Device Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDeviceMenuExpanded,
                    onExpandedChange = { isDeviceMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentSelectionText = if (settings.deviceType == "AUTO") {
                        stringResource(R.string.auto_detect_device, detectedDevice.displayName)
                    } else {
                        effectiveDevice.displayName
                    }

                    OutlinedTextField(
                        value = currentSelectionText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_device_oem)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeviceMenuExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isDeviceMenuExpanded,
                        onDismissRequest = { isDeviceMenuExpanded = false }
                    ) {
                        OemDeviceType.entries.forEach { deviceTypeOption ->
                            val isSelected = (settings.deviceType == "AUTO" && deviceTypeOption == OemDeviceType.AUTO) ||
                                (settings.deviceType == deviceTypeOption.name)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (deviceTypeOption == OemDeviceType.AUTO) stringResource(R.string.auto_detect_device, detectedDevice.displayName) else deviceTypeOption.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    scope.launch { repository.setDeviceType(deviceTypeOption.name) }
                                    isDeviceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Active Rules Summary Chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.active_rules_title, effectiveDevice.displayName),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.active_rules_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Group 1: Lock Screen & Privacy
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.lock_screen_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.lock_screen_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_show_lock_screen_title),
                    subtitle = stringResource(R.string.toggle_show_lock_screen_desc),
                    icon = Icons.Rounded.Lock,
                    checked = settings.showOnLockScreen,
                    onCheckedChange = { scope.launch { repository.setShowOnLockScreen(it) } }
                )

                if (settings.showOnLockScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.privacy_level_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PrivacySegmentButton(
                                label = stringResource(R.string.privacy_app_icon_only),
                                selected = settings.lockScreenPrivacy == "AppIconOnly",
                                onClick = { scope.launch { repository.setLockScreenPrivacy("AppIconOnly") } },
                                modifier = Modifier.weight(1f)
                            )
                            PrivacySegmentButton(
                                label = stringResource(R.string.privacy_full_content),
                                selected = settings.lockScreenPrivacy == "FullContent",
                                onClick = { scope.launch { repository.setLockScreenPrivacy("FullContent") } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Group 2: Display & Expansion Behavior
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.display_rules_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.display_rules_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_auto_expand_title),
                    subtitle = stringResource(R.string.toggle_auto_expand_desc),
                    icon = Icons.Rounded.NotificationsActive,
                    checked = settings.autoExpandOnNotification,
                    onCheckedChange = { scope.launch { repository.setAutoExpandOnNotification(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_quick_actions_title),
                    subtitle = stringResource(R.string.toggle_quick_actions_desc),
                    icon = Icons.Rounded.TouchApp,
                    checked = settings.showNotificationActions,
                    onCheckedChange = { scope.launch { repository.setShowNotificationActions(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_mirror_only_title),
                    subtitle = stringResource(R.string.toggle_mirror_only_desc),
                    icon = Icons.Rounded.VisibilityOff,
                    checked = settings.hideFromNotificationShade,
                    onCheckedChange = { scope.launch { repository.setHideFromNotificationShade(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_show_in_landscape_title),
                    subtitle = stringResource(R.string.toggle_show_in_landscape_desc),
                    icon = Icons.Rounded.ScreenRotation,
                    checked = settings.showInLandscape,
                    onCheckedChange = { scope.launch { repository.setShowInLandscape(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_auto_hide_idle_title),
                    subtitle = stringResource(R.string.toggle_auto_hide_idle_desc),
                    icon = Icons.Rounded.Visibility,
                    checked = settings.hideWhenIdle,
                    onCheckedChange = { scope.launch { repository.setHideWhenIdle(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_auto_hide_pill_title),
                    subtitle = stringResource(R.string.toggle_auto_hide_pill_desc),
                    icon = Icons.Rounded.Timer,
                    checked = settings.autoHidePill,
                    onCheckedChange = { scope.launch { repository.setAutoHidePill(it) } }
                )

                AnimatedVisibility(
                    visible = settings.autoHidePill,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 5, 10, 15, 30).forEach { sec ->
                                val isSelected = settings.autoHideTimeoutSeconds == sec
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch { repository.setAutoHideTimeoutSeconds(sec) }
                                    },
                                    label = { Text("${sec}s", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        SliderSettingItem(
                            label = stringResource(R.string.auto_hide_timeout_label),
                            value = settings.autoHideTimeoutSeconds.toFloat(),
                            range = 1f..60f,
                            step = 1f,
                            suffix = "s",
                            onValueChange = { newVal ->
                                scope.launch { repository.setAutoHideTimeoutSeconds(newVal.roundToInt()) }
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_bluetooth_battery_title),
                    subtitle = stringResource(R.string.toggle_bluetooth_battery_desc),
                    icon = Icons.Rounded.BluetoothConnected,
                    checked = settings.showBluetoothBattery,
                    onCheckedChange = { scope.launch { repository.setShowBluetoothBattery(it) } }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_battery_mode_title),
                    subtitle = stringResource(R.string.toggle_battery_mode_desc),
                    icon = Icons.Rounded.BatteryChargingFull,
                    checked = settings.enableBatteryMode,
                    onCheckedChange = { scope.launch { repository.setEnableBatteryMode(it) } }
                )
            }
        }

        // Group 3: Real-Time Services (Live Activities & Maps)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.live_nav_services_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.live_nav_services_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_live_activities_title),
                    subtitle = stringResource(R.string.toggle_live_activities_desc),
                    icon = Icons.Rounded.DirectionsCar,
                    checked = settings.liveActivitiesEnabled,
                    onCheckedChange = { scope.launch { repository.setLiveActivitiesEnabled(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_navigation_title),
                    subtitle = stringResource(R.string.toggle_navigation_desc),
                    icon = Icons.Rounded.Map,
                    checked = settings.navigationEnabled,
                    onCheckedChange = { scope.launch { repository.setNavigationEnabled(it) } }
                )
            }
        }

        // Shared Installed Apps Provider
        val installedApps by produceState(initialValue = emptyList<LaunchableApp>(), context) {
            value = withContext(Dispatchers.IO) {
                AppShortcutProvider.installedApps(context)
                    .filter { NotificationFilter.isAppEligibleForIsland(it.packageName, context.packageManager) }
            }
        }

        // Group 3.5: Notification Cooldown & Anti-Spam
        var cooldownAppQuery by remember { mutableStateOf("") }
        val cooldownFilteredApps = remember(installedApps, cooldownAppQuery) {
            if (cooldownAppQuery.isBlank()) installedApps
            else installedApps.filter {
                it.label.contains(cooldownAppQuery, ignoreCase = true) ||
                    it.packageName.contains(cooldownAppQuery, ignoreCase = true)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.notification_cooldown_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.notification_cooldown_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                ToggleRowItem(
                    title = stringResource(R.string.toggle_enable_cooldown_title),
                    subtitle = stringResource(R.string.toggle_enable_cooldown_desc),
                    icon = Icons.Rounded.HourglassBottom,
                    checked = settings.enableNotificationCooldown,
                    onCheckedChange = { scope.launch { repository.setEnableNotificationCooldown(it) } }
                )

                AnimatedVisibility(
                    visible = settings.enableNotificationCooldown,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Cooldown Duration
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.cooldown_duration_title, settings.notificationCooldownDurationMinutes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 3, 5, 10, 15).forEach { min ->
                                    val isSelected = settings.notificationCooldownDurationMinutes == min
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch { repository.setNotificationCooldownDurationMinutes(min) }
                                        },
                                        label = { Text("${min}m", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            SliderSettingItem(
                                label = stringResource(R.string.cooldown_duration_slider),
                                value = settings.notificationCooldownDurationMinutes.toFloat(),
                                range = 1f..30f,
                                step = 1f,
                                suffix = " min",
                                onValueChange = { newVal ->
                                    scope.launch { repository.setNotificationCooldownDurationMinutes(newVal.roundToInt()) }
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // 2. Trigger Threshold (Alert count in 30 seconds)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.cooldown_threshold_title, settings.notificationCooldownThreshold),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 4, 5, 8, 10).forEach { count ->
                                    val isSelected = settings.notificationCooldownThreshold == count
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch { repository.setNotificationCooldownThreshold(count) }
                                        },
                                        label = { Text("$count", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            SliderSettingItem(
                                label = stringResource(R.string.cooldown_threshold_slider),
                                value = settings.notificationCooldownThreshold.toFloat(),
                                range = 2f..10f,
                                step = 1f,
                                suffix = " alerts",
                                onValueChange = { newVal ->
                                    scope.launch { repository.setNotificationCooldownThreshold(newVal.roundToInt()) }
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // 3. Excluded Apps (Whitelist)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.cooldown_excluded_apps_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.cooldown_excluded_apps_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = cooldownAppQuery,
                                onValueChange = { cooldownAppQuery = it },
                                placeholder = { Text(stringResource(R.string.search_installed_apps)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val excludedCount = settings.notificationCooldownExcludedPackages.size
                                Text(
                                    text = stringResource(R.string.cooldown_excluded_count, excludedCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val targets = installedApps.map { it.packageName }.toSet()
                                            scope.launch {
                                                repository.setNotificationCooldownExcludedPackages(targets)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_exclude_all), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                repository.setNotificationCooldownExcludedPackages(emptySet())
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_clear_exclusions), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            val displayApps = if (cooldownAppQuery.isBlank()) cooldownFilteredApps.take(15) else cooldownFilteredApps
                            displayApps.forEach { app ->
                                val isExcluded = app.packageName in settings.notificationCooldownExcludedPackages

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FilterChip(
                                        selected = isExcluded,
                                        onClick = {
                                            scope.launch {
                                                repository.toggleNotificationCooldownExcludedPackage(app.packageName)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = if (isExcluded) stringResource(R.string.chip_excluded) else stringResource(R.string.chip_cooldown_active),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // State for managing app alerts & exclusions
        var showAppManagerDialog by remember { mutableStateOf(false) }
        var selectedAppForOptions by remember { mutableStateOf<LaunchableApp?>(null) }

        // Identify customized apps (apps where user changed notification, sound, or cooldown)
        val customizedApps = remember(
            installedApps,
            settings.disabledNotificationPackages,
            settings.disabledSoundPackages,
            settings.notificationCooldownExcludedPackages
        ) {
            installedApps.filter { app ->
                app.packageName in settings.disabledNotificationPackages ||
                    app.packageName in settings.disabledSoundPackages ||
                    app.packageName in settings.notificationCooldownExcludedPackages
            }
        }

        // Group 4: Per-App Notification, Alert & Sound Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_alerts_manager_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_alerts_manager_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Stats row
                val activeCount = installedApps.count { it.packageName !in settings.disabledNotificationPackages }
                val excludedCount = settings.disabledNotificationPackages.size
                val mutedCount = settings.disabledSoundPackages.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$activeCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (excludedCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$excludedCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (excludedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Excluded",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (excludedCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (mutedCount > 0) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$mutedCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (mutedCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Muted",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mutedCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Overview of customized apps (if any)
                if (customizedApps.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.customized_apps_title, customizedApps.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        customizedApps.take(4).forEach { app ->
                            val isExcluded = app.packageName in settings.disabledNotificationPackages
                            val isMuted = app.packageName in settings.disabledSoundPackages
                            val isCooldownBypassed = app.packageName in settings.notificationCooldownExcludedPackages

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .clickable { selectedAppForOptions = app }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val icon = rememberAppIcon(app.packageName)
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.firstOrNull()?.uppercase() ?: "A",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isExcluded) {
                                            Text(
                                                text = stringResource(R.string.tag_excluded_from_island),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (isMuted) {
                                            Text(
                                                text = stringResource(R.string.tag_sound_muted),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (isCooldownBypassed) {
                                            Text(
                                                text = stringResource(R.string.tag_cooldown_bypassed),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Configure",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (customizedApps.size > 4) {
                            Text(
                                text = "+${customizedApps.size - 4} more customized apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showAppManagerDialog = true }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.all_apps_enabled_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Primary Button to open management dialog
                FilledTonalButton(
                    onClick = { showAppManagerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_manage_apps),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Dialog 1: App Alerts Management List Dialog
        if (showAppManagerDialog) {
            var query by remember { mutableStateOf("") }
            var filterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Customized, 2: Excluded

            val filteredApps = remember(installedApps, query, filterTab, settings.disabledNotificationPackages, settings.disabledSoundPackages, settings.notificationCooldownExcludedPackages) {
                installedApps.filter { app ->
                    val matchesQuery = query.isBlank() ||
                        app.label.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)

                    val matchesTab = when (filterTab) {
                        1 -> app.packageName in settings.disabledNotificationPackages ||
                             app.packageName in settings.disabledSoundPackages ||
                             app.packageName in settings.notificationCooldownExcludedPackages
                        2 -> app.packageName in settings.disabledNotificationPackages
                        else -> true
                    }

                    matchesQuery && matchesTab
                }
            }

            AlertDialog(
                onDismissRequest = { showAppManagerDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.app_alerts_manager_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search box
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search_installed_apps)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Filter chips row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterTab == 0,
                                onClick = { filterTab = 0 },
                                label = { Text(stringResource(R.string.filter_tab_all, installedApps.size), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = filterTab == 1,
                                onClick = { filterTab = 1 },
                                label = { Text(stringResource(R.string.filter_tab_customized, customizedApps.size), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = filterTab == 2,
                                onClick = { filterTab = 2 },
                                label = { Text(stringResource(R.string.filter_tab_excluded, settings.disabledNotificationPackages.size), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        // Batch actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredApps.size} apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val targets = filteredApps.map { it.packageName }.toSet()
                                        scope.launch {
                                            repository.setDisabledNotificationPackages(settings.disabledNotificationPackages - targets)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(stringResource(R.string.btn_select_all), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val targets = filteredApps.map { it.packageName }.toSet()
                                        scope.launch {
                                            repository.setDisabledNotificationPackages(settings.disabledNotificationPackages + targets)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(stringResource(R.string.btn_deselect_all), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // LazyColumn of apps (fast & virtualized)
                        if (filteredApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (query.isBlank()) stringResource(R.string.no_installed_apps_found)
                                           else stringResource(R.string.no_apps_matching_query, query),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredApps, key = { it.packageName }) { app ->
                                    val isNotificationEnabled = app.packageName !in settings.disabledNotificationPackages
                                    val isSoundMuted = app.packageName in settings.disabledSoundPackages

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedAppForOptions = app }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val icon = rememberAppIcon(app.packageName)
                                            if (icon != null) {
                                                Image(
                                                    bitmap = icon,
                                                    contentDescription = app.label,
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = app.label.firstOrNull()?.uppercase() ?: "A",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (!isNotificationEnabled) {
                                                        Text(
                                                            text = stringResource(R.string.tag_excluded_from_island),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (isSoundMuted) "Active • Muted" else "Active",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isSoundMuted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Switch(
                                                checked = isNotificationEnabled,
                                                onCheckedChange = { enabled ->
                                                    val updated = if (enabled) {
                                                        settings.disabledNotificationPackages - app.packageName
                                                    } else {
                                                        settings.disabledNotificationPackages + app.packageName
                                                    }
                                                    scope.launch { repository.setDisabledNotificationPackages(updated) }
                                                }
                                            )

                                            IconButton(
                                                onClick = { selectedAppForOptions = app },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Tune,
                                                    contentDescription = "Options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppManagerDialog = false }) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog 2: Detailed App Options Modal (Notifications On/Off/Exclude, Sound On/Off, Cooldown Bypass)
        selectedAppForOptions?.let { app ->
            val isNotificationEnabled = app.packageName !in settings.disabledNotificationPackages
            val isSoundEnabled = app.packageName !in settings.disabledSoundPackages
            val isCooldownBypassed = app.packageName in settings.notificationCooldownExcludedPackages

            AlertDialog(
                onDismissRequest = { selectedAppForOptions = null },
                icon = {
                    val icon = rememberAppIcon(app.packageName)
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.label.firstOrNull()?.uppercase() ?: "A",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.app_options_title, app.label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option 1: Smart Island Notifications (Allow / Exclude from Island)
                        ToggleRowItem(
                            title = stringResource(R.string.option_island_notifications),
                            subtitle = if (isNotificationEnabled)
                                stringResource(R.string.option_island_notifications_desc)
                            else
                                stringResource(R.string.tag_excluded_from_island),
                            icon = if (isNotificationEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            iconColor = if (isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            checked = isNotificationEnabled,
                            onCheckedChange = { enabled ->
                                val updated = if (enabled) {
                                    settings.disabledNotificationPackages - app.packageName
                                } else {
                                    settings.disabledNotificationPackages + app.packageName
                                }
                                scope.launch { repository.setDisabledNotificationPackages(updated) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Option 2: Island Alert Sound (Sound on / Muted)
                        ToggleRowItem(
                            title = stringResource(R.string.option_island_sound),
                            subtitle = if (isSoundEnabled)
                                stringResource(R.string.option_island_sound_desc)
                            else
                                stringResource(R.string.tag_sound_muted),
                            icon = if (isSoundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            checked = isSoundEnabled && isNotificationEnabled,
                            onCheckedChange = { enabled ->
                                val updated = if (enabled) {
                                    settings.disabledSoundPackages - app.packageName
                                } else {
                                    settings.disabledSoundPackages + app.packageName
                                }
                                scope.launch { repository.setDisabledSoundPackages(updated) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Option 3: Anti-Spam Cooldown Bypass (Exclude from cooldown)
                        ToggleRowItem(
                            title = stringResource(R.string.option_cooldown_bypass),
                            subtitle = stringResource(R.string.option_cooldown_bypass_desc),
                            icon = Icons.Rounded.HourglassBottom,
                            checked = isCooldownBypassed,
                            onCheckedChange = {
                                scope.launch {
                                    repository.toggleNotificationCooldownExcludedPackage(app.packageName)
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedAppForOptions = null }) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                repository.setDisabledNotificationPackages(settings.disabledNotificationPackages - app.packageName)
                                repository.setDisabledSoundPackages(settings.disabledSoundPackages - app.packageName)
                                repository.setNotificationCooldownExcludedPackages(settings.notificationCooldownExcludedPackages - app.packageName)
                            }
                        }
                    ) {
                        Text(
                            stringResource(R.string.btn_reset_default),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ToggleRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (checked) iconColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) iconColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PrivacySegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(0.5.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .bounceClick(onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(150)
    fun get(pkg: String): ImageBitmap? = cache.get(pkg)
    fun put(pkg: String, bmp: ImageBitmap) { cache.put(pkg, bmp) }
}

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { mutableStateOf(AppIconCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    val bmp = drawable.toBitmap(width = 72, height = 72)
                    bmp.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) {
                AppIconCache.put(packageName, loaded)
                iconBitmap = loaded
            }
        }
    }
    return iconBitmap
}
