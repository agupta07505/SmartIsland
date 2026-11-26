/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LockScreenPreset
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private fun CoroutineScope.saveToggle(save: suspend (Boolean) -> Unit): (Boolean) -> Unit =
    { checked -> launch { save(checked) } }

@Composable
internal fun LockScreenCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    SettingsGroupCard(spacing = 12.dp) {
        SettingsGroupTitle(stringResource(R.string.lock_screen_title))
        SettingsDescription(stringResource(R.string.lock_screen_desc))

        SettingsDivider(0.3f)

        LockScreenPresetBlock(settings = settings, repository = repository, scope = scope)

        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.show_lock_screen",
            title = stringResource(R.string.toggle_show_lock_screen_title),
            subtitle = stringResource(R.string.toggle_show_lock_screen_desc),
            icon = Icons.Rounded.Lock,
            checked = settings.showOnLockScreen,
            onCheckedChange = scope.saveToggle(repository::setShowOnLockScreen)
        )

        if (settings.showOnLockScreen) {
            PrivacyLevelPicker(settings = settings, repository = repository, scope = scope)
        }

        if (settings.showOnLockScreen) {
            SettingsDivider(0.2f)

            ToggleRowItem(
                searchKey = "np.hide_all_lock_screen",
                title = stringResource(R.string.toggle_hide_all_lock_screen_title),
                subtitle = stringResource(R.string.toggle_hide_all_lock_screen_desc),
                icon = Icons.Rounded.VisibilityOff,
                checked = settings.hideAllOnLockScreen,
                onCheckedChange = scope.saveToggle(repository::setHideAllOnLockScreen)
            )
            // The per-event switches only matter while the universal one is off
            if (!settings.hideAllOnLockScreen) {
                LockScreenEventList(settings = settings, repository = repository)
            }
        }

        SettingsDivider(0.2f)

        ForegroundEventList(settings = settings, repository = repository)

        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.hide_in_fullscreen",
            title = stringResource(R.string.toggle_hide_in_fullscreen_title),
            subtitle = stringResource(R.string.toggle_hide_in_fullscreen_desc),
            icon = Icons.Rounded.Fullscreen,
            checked = settings.hideInFullscreen,
            onCheckedChange = scope.saveToggle(repository::setHideInFullscreen)
        )

        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.hide_in_shade",
            title = stringResource(R.string.toggle_hide_in_shade_title),
            subtitle = stringResource(R.string.toggle_hide_in_shade_desc),
            icon = Icons.Rounded.VisibilityOff,
            checked = settings.hideInShade,
            onCheckedChange = scope.saveToggle(repository::setHideInShade)
        )

        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.network_access",
            title = stringResource(R.string.toggle_network_access_title),
            subtitle = stringResource(R.string.toggle_network_access_desc),
            icon = Icons.Rounded.Public,
            checked = settings.allowNetworkChecks,
            onCheckedChange = scope.saveToggle(repository::setAllowNetworkChecks)
        )

        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.transfer_speed",
            title = stringResource(R.string.toggle_transfer_speed_title),
            subtitle = stringResource(R.string.toggle_transfer_speed_desc),
            icon = Icons.Rounded.Speed,
            checked = settings.detectTransfersBySpeed,
            onCheckedChange = scope.saveToggle(repository::setDetectTransfersBySpeed)
        )
    }
}

@Composable
private fun LockScreenPresetBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier.fillMaxWidth().searchAnchor("np.lock_screen_preset"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.lock_screen_preset_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        val currentPreset = LockScreenPreset.from(settings)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                LockScreenPreset.ShowAll to R.string.lock_screen_preset_show_all,
                LockScreenPreset.MessagesOnly to R.string.lock_screen_preset_messages,
                LockScreenPreset.CallsOnly to R.string.lock_screen_preset_calls,
                LockScreenPreset.Off to R.string.lock_screen_preset_nothing
            ).forEach { (preset, label) ->
                PrivacySegmentButton(
                    label = stringResource(label),
                    selected = currentPreset == preset,
                    onClick = { scope.launch { repository.applyLockScreenPreset(preset) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        SettingsDescription(stringResource(R.string.lock_screen_preset_desc))
    }
}

@Composable
private fun PrivacyLevelPicker(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
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

@Composable
internal fun DisplayRulesCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    SettingsGroupCard(spacing = 12.dp) {
        SettingsGroupTitle(stringResource(R.string.display_rules_title))
        SettingsDescription(stringResource(R.string.display_rules_desc))

        SettingsDivider(0.3f)

        ToggleRowItem(
            searchKey = "np.auto_expand",
            title = stringResource(R.string.toggle_auto_expand_title),
            subtitle = stringResource(R.string.toggle_auto_expand_desc),
            icon = Icons.Rounded.NotificationsActive,
            checked = settings.autoExpandOnNotification,
            onCheckedChange = scope.saveToggle(repository::setAutoExpandOnNotification)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.auto_expand_lock_screen",
            title = stringResource(R.string.toggle_auto_expand_lock_screen_title),
            subtitle = stringResource(R.string.toggle_auto_expand_lock_screen_desc),
            icon = Icons.Rounded.Lock,
            checked = settings.autoExpandOnLockScreen,
            onCheckedChange = scope.saveToggle(repository::setAutoExpandOnLockScreen)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.suggested_replies",
            title = stringResource(R.string.toggle_suggested_replies_title),
            subtitle = stringResource(R.string.toggle_suggested_replies_desc),
            icon = Icons.Rounded.NotificationsActive,
            checked = settings.showSuggestedReplies,
            onCheckedChange = scope.saveToggle(repository::setShowSuggestedReplies)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.copy_code_chip",
            title = stringResource(R.string.toggle_copy_code_chip_title),
            subtitle = stringResource(R.string.toggle_copy_code_chip_desc),
            icon = Icons.Rounded.ContentCopy,
            checked = settings.showCopyCodeChip,
            onCheckedChange = scope.saveToggle(repository::setShowCopyCodeChip)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.show_during_dnd",
            title = stringResource(R.string.toggle_show_during_dnd_title),
            subtitle = stringResource(R.string.toggle_show_during_dnd_desc),
            icon = Icons.Rounded.DoNotDisturbOn,
            checked = settings.showDuringDnd,
            onCheckedChange = scope.saveToggle(repository::setShowDuringDnd)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.quick_actions",
            title = stringResource(R.string.toggle_quick_actions_title),
            subtitle = stringResource(R.string.toggle_quick_actions_desc),
            icon = Icons.Rounded.TouchApp,
            checked = settings.showNotificationActions,
            onCheckedChange = scope.saveToggle(repository::setShowNotificationActions)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.mirror_only",
            title = stringResource(R.string.toggle_mirror_only_title),
            subtitle = stringResource(R.string.toggle_mirror_only_desc),
            icon = Icons.Rounded.VisibilityOff,
            checked = settings.hideFromNotificationShade,
            onCheckedChange = scope.saveToggle(repository::setHideFromNotificationShade)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.hide_when_idle",
            title = stringResource(R.string.toggle_auto_hide_idle_title),
            subtitle = stringResource(R.string.toggle_auto_hide_idle_desc),
            icon = Icons.Rounded.VisibilityOff,
            checked = settings.hideWhenIdle,
            onCheckedChange = scope.saveToggle(repository::setHideWhenIdle)
        )
        SettingsDivider(0.2f)

        ToggleRowItem(
            searchKey = "np.bluetooth_battery",
            title = stringResource(R.string.toggle_bluetooth_battery_title),
            subtitle = stringResource(R.string.toggle_bluetooth_battery_desc),
            icon = Icons.Rounded.BluetoothConnected,
            checked = settings.showBluetoothBattery,
            onCheckedChange = scope.saveToggle(repository::setShowBluetoothBattery)
        )

    }
}

@Composable
private fun ForegroundEventList(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    val events = IslandEvent.FOREGROUND_SWITCHABLE
    val shownCount = events.count { it.id in settings.showInForegroundEvents }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .searchAnchor("np.live_events_foreground")
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = !open }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.toggle_live_events_foreground_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsDescription(stringResource(R.string.toggle_live_events_foreground_desc))
            }
            Text(
                text = "$shownCount / ${events.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (open) {
            events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = event.id in settings.showInForegroundEvents,
                        onCheckedChange = { shown ->
                            scope.launch { repository.setEventShownInForeground(event.id, shown) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LockScreenEventList(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    val events = IslandEvent.entries.filter { it.canHideOnLockScreen }
    val hiddenCount = events.count { it.id in settings.hideOnLockScreenEvents }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .searchAnchor("np.hide_events_lock_screen")
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = !open }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lock_screen_per_event_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsDescription(stringResource(R.string.lock_screen_per_event_desc))
            }
            Text(
                text = "$hiddenCount / ${events.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (open) {
            events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = event.id in settings.hideOnLockScreenEvents,
                        onCheckedChange = { hide ->
                            scope.launch { repository.setEventHiddenOnLockScreen(event.id, hide) }
                        }
                    )
                }
            }
        }
    }
}
