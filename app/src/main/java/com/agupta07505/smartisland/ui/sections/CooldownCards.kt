/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.util.NotificationCooldownManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun CooldownCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    installedApps: List<LaunchableApp>,
    cooldownAppQuery: String,
    onCooldownAppQueryChange: (String) -> Unit,
    cooldownFilteredApps: List<LaunchableApp>
) {
    SettingsGroupCard(spacing = 12.dp) {
        SettingsGroupTitle(stringResource(R.string.notification_cooldown_title))
        SettingsDescription(stringResource(R.string.notification_cooldown_desc))

        SettingsDivider(0.3f)

        ToggleRowItem(
            searchKey = "np.enable_cooldown",
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
                CooldownSlider(
                    title = stringResource(R.string.cooldown_duration_title, settings.notificationCooldownDurationMinutes),
                    label = stringResource(R.string.cooldown_duration_slider),
                    value = settings.notificationCooldownDurationMinutes,
                    range = 1..30,
                    presets = listOf(1, 2, 3, 5, 10, 15),
                    suffix = " min",
                    presetLabel = { "${it}m" },
                    onChange = { scope.launch { repository.setNotificationCooldownDurationMinutes(it) } }
                )

                SettingsDivider(0.2f)

                CooldownSlider(
                    title = stringResource(R.string.cooldown_threshold_title, settings.notificationCooldownThreshold),
                    label = stringResource(R.string.cooldown_threshold_slider),
                    value = settings.notificationCooldownThreshold,
                    range = 2..10,
                    presets = listOf(2, 3, 4, 5, 8, 10),
                    suffix = " alerts",
                    presetLabel = Int::toString,
                    onChange = { scope.launch { repository.setNotificationCooldownThreshold(it) } }
                )

                SettingsDivider(0.2f)

                CooldownSystemEventsBlock(settings = settings, repository = repository, scope = scope)

                SettingsDivider(0.2f)

                CooldownExcludedAppsBlock(
                    settings = settings,
                    repository = repository,
                    scope = scope,
                    installedApps = installedApps,
                    cooldownAppQuery = cooldownAppQuery,
                    onCooldownAppQueryChange = onCooldownAppQueryChange,
                    cooldownFilteredApps = cooldownFilteredApps
                )
            }
        }
    }
}

@Composable
private fun CooldownSlider(
    title: String,
    label: String,
    value: Int,
    range: IntRange,
    presets: List<Int>,
    suffix: String,
    presetLabel: (Int) -> String,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = value == preset,
                    onClick = { onChange(preset) },
                    label = { Text(presetLabel(preset), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        SliderSettingItem(
            label = label,
            value = value.toFloat(),
            range = range.first.toFloat()..range.last.toFloat(),
            step = 1f,
            suffix = suffix,
            onValueChange = { onChange(it.roundToInt()) }
        )
    }
}

@Composable
private fun CooldownSystemEventsBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    // System events (battery, Wi-Fi, VPN, Bluetooth, flashlight)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.cooldown_system_events_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        SettingsDescription(stringResource(R.string.cooldown_system_events_desc))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(
                    R.string.cooldown_system_events_count,
                    settings.notificationCooldownSystemEvents.size
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val all = NotificationCooldownManager.SYSTEM_EVENTS
                            .map { it.id }.toSet()
                        scope.launch { repository.setNotificationCooldownSystemEvents(all) }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.btn_select_all_events), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { scope.launch { repository.setNotificationCooldownSystemEvents(emptySet()) } },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.btn_clear_events), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        NotificationCooldownManager.SYSTEM_EVENTS.forEach { event ->
            val inCooldown = event.id in settings.notificationCooldownSystemEvents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.shortLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HintText(event.label)
                }
                FilterChip(
                    selected = inCooldown,
                    onClick = {
                        scope.launch { repository.toggleNotificationCooldownSystemEvent(event.id) }
                    },
                    label = {
                        Text(
                            text = if (inCooldown) stringResource(R.string.chip_cooldown_active) else stringResource(R.string.chip_always_alert),
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

@Composable
private fun CooldownExcludedAppsBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    installedApps: List<LaunchableApp>,
    cooldownAppQuery: String,
    onCooldownAppQueryChange: (String) -> Unit,
    cooldownFilteredApps: List<LaunchableApp>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.cooldown_excluded_apps_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        SettingsDescription(stringResource(R.string.cooldown_excluded_apps_desc))

        OutlinedTextField(
            value = cooldownAppQuery,
            onValueChange = onCooldownAppQueryChange,
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
                    Text(stringResource(R.string.btn_clear_exclusions), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        val displayApps = if (cooldownAppQuery.isBlank()) cooldownFilteredApps.take(15) else cooldownFilteredApps
        displayApps.forEach { app ->
        CooldownAppRow(app = app, settings = settings, repository = repository, scope = scope)
        }
    }
}

@Composable
private fun CooldownAppRow(
    app: LaunchableApp,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
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
            HintText(app.packageName)
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
