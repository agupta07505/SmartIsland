/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.UsbOff
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.VpnKeyOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal fun eventSearchKey(event: IslandEvent): String = "event.${event.group}.${event.id}"

internal fun iconFor(event: IslandEvent): ImageVector = when (event) {
    IslandEvent.PLUGGED_IN -> Icons.Rounded.Power
    IslandEvent.PLUGGED_OUT -> Icons.Rounded.PowerOff
    IslandEvent.FULLY_CHARGED -> Icons.Rounded.BatteryFull
    IslandEvent.LOW_BATTERY -> Icons.Rounded.BatteryAlert
    IslandEvent.BATTERY_SAVER -> Icons.Rounded.BatterySaver
    IslandEvent.WIFI -> Icons.Rounded.Wifi
    IslandEvent.VPN -> Icons.Rounded.VpnKey
    IslandEvent.WIFI_DISCONNECTED -> Icons.Rounded.WifiOff
    IslandEvent.VPN_DISCONNECTED -> Icons.Rounded.VpnKeyOff
    IslandEvent.BLUETOOTH -> Icons.Rounded.BluetoothConnected
    IslandEvent.USB_CONNECTED -> Icons.Rounded.Usb
    IslandEvent.USB_DISCONNECTED -> Icons.Rounded.UsbOff
    IslandEvent.HEADPHONES_CONNECTED -> Icons.Rounded.Headphones
    IslandEvent.HEADPHONES_DISCONNECTED -> Icons.Rounded.HeadsetOff
    IslandEvent.HOTSPOT -> Icons.Rounded.WifiTethering
    IslandEvent.CALL -> Icons.Rounded.Call
    IslandEvent.MUSIC -> Icons.Rounded.MusicNote
    IslandEvent.NOTIFICATION -> Icons.Rounded.Notifications
    IslandEvent.NAVIGATION -> Icons.Rounded.Explore
    IslandEvent.LIVE_ACTIVITY -> Icons.Rounded.Navigation
    IslandEvent.DOWNLOAD -> Icons.Rounded.FileDownload
    IslandEvent.UPLOAD -> Icons.Rounded.FileUpload
    IslandEvent.BACKUP -> Icons.Rounded.Backup
    IslandEvent.FILE_COPY_MOVE -> Icons.Rounded.FileCopy
    IslandEvent.FILE_DELETE -> Icons.Rounded.DeleteSweep
    IslandEvent.APP_INSTALL -> Icons.Rounded.InstallMobile
    IslandEvent.FLASHLIGHT -> Icons.Rounded.FlashlightOn
    IslandEvent.SCREEN_RECORDING -> Icons.Rounded.Videocam
    IslandEvent.AUDIO_RECORDING -> Icons.Rounded.Mic
    IslandEvent.TIMER -> Icons.Rounded.HourglassBottom
    IslandEvent.STOPWATCH -> Icons.Rounded.AvTimer
    IslandEvent.FACE_UNLOCK -> Icons.Rounded.Face
}

/** One switch per activity, grouped. Lives on the Home tab. */
@Composable
fun EventTogglesSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    landscape: Boolean = false,
    onOpenFaceIdSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    var locationGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var locationOn by remember { mutableStateOf(isLocationServiceOn(context)) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationGranted = granted
    }
    // Permission and the location switch can change in system settings; re-read when coming back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                locationOn = isLocationServiceOn(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IslandEvent.entries.groupBy { it.group }.entries.forEachIndexed { groupIndex, (group, events) ->
            fun isOn(event: IslandEvent) =
                if (landscape) settings.isLandscapeEventEnabled(event.id) else settings.isEventEnabled(event.id)

            val onCount = events.count { isOn(it) }
            CollapsibleCard(
                title = group,
                stateKey = "events_${if (landscape) "land" else "port"}_$group",
                summary = "$onCount of ${events.size} on",
                icon = iconFor(events.first()),
                initiallyExpanded = groupIndex == 0,
                searchKeyPrefix = "event.$group.",
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Auto-expand for the whole group: on when every event that has the switch is on.
                        val switchable = events.filter { it.canSwitchAutoExpand }
                        if (switchable.isNotEmpty()) {
                            val allAuto = switchable.all { settings.isEventAutoExpand(it.id) }
                            AutoExpandBadge(
                                on = allAuto,
                                modifier = Modifier.size(32.dp),
                                onChange = { turnOn ->
                                    scope.launch {
                                        switchable.forEach { repository.setEventAutoExpand(it.id, turnOn) }
                                    }
                                }
                            )
                        }
                        Switch(
                            checked = onCount == events.size,
                            onCheckedChange = { turnOn ->
                                scope.launch {
                                    events.forEach { event ->
                                        if (landscape) repository.setLandscapeEventEnabled(event.id, turnOn)
                                        else repository.setEventEnabled(event.id, turnOn)
                                    }
                                }
                            }
                        )
                    }
                }
            ) {
                val columns = eventGridColumns(configuration.screenWidthDp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gridRows(events, columns).forEach { rowEvents ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEvents.forEach { event ->
                                EventTile(
                                    title = event.shortLabel,
                                    icon = iconFor(event),
                                    checked = isOn(event),
                                    autoExpand = if (event.canSwitchAutoExpand) settings.isEventAutoExpand(event.id) else null,
                                    onAutoExpandChange = { turnOn ->
                                        scope.launch { repository.setEventAutoExpand(event.id, turnOn) }
                                    },
                                    onOpenSettings = if (event == IslandEvent.FACE_UNLOCK) onOpenFaceIdSettings else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .searchAnchor(eventSearchKey(event)),
                                    onCheckedChange = { turnOn ->
                                        scope.launch {
                                            if (landscape) repository.setLandscapeEventEnabled(event.id, turnOn)
                                            else repository.setEventEnabled(event.id, turnOn)
                                        }
                                        if (turnOn && event == IslandEvent.WIFI && !locationGranted) {
                                            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    }
                                )
                            }
                            // Empty cells keep a short last row's tiles the same width as the rest.
                            repeat(columns - rowEvents.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }

                    // Prompts that belong to a row (location for Wi-Fi, network checks for
                    // VPN, Shizuku for screen sharing) live under the grid, with their button.
                    events.forEach { event ->
                        val needsLocation = event == IslandEvent.WIFI && !locationGranted
                        val needsLocationSwitch = event == IslandEvent.WIFI && locationGranted && !locationOn
                        val note = when {
                            needsLocation -> "Wi-Fi: grant location permission to show the network name"
                            needsLocationSwitch -> "Wi-Fi: turn on location services to show the network name"
                            event == IslandEvent.VPN && !settings.allowNetworkChecks -> "VPN: location needs network checks turned on"
                            else -> null
                        }
                        if (note != null) {
                            EventNote(
                                text = note,
                                actionLabel = when {
                                    needsLocation -> "Grant"
                                    needsLocationSwitch -> "Turn on"
                                    else -> null
                                },
                                onAction = {
                                    if (needsLocation) {
                                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    } else {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTile(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoExpand: Boolean? = null,
    onAutoExpandChange: (Boolean) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (checked) accent.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
                .border(
                    width = 1.dp,
                    color = if (checked) accent.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = shape
                )
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                color = if (checked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (autoExpand != null) {
            AutoExpandBadge(
                on = autoExpand,
                modifier = Modifier.align(Alignment.TopEnd).size(30.dp),
                dimmed = !checked,
                onChange = onAutoExpandChange
            )
        }
        if (onOpenSettings != null) {
            // Own touch target in the corner, so a tap here opens the event's settings and never toggles the event.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "$title settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (checked) 0.85f else 0.55f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AutoExpandBadge(
    on: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    val description = stringResource(if (on) R.string.event_auto_expand_on else R.string.event_auto_expand_off)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (on) Icons.Rounded.OpenInFull else Icons.Rounded.CloseFullscreen,
            contentDescription = description,
            tint = if (on) accent.copy(alpha = if (dimmed) 0.45f else 1f)
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dimmed) 0.3f else 0.55f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun EventNote(
    text: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun isLocationServiceOn(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true
    val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
        ?: return true
    return runCatching { manager.isLocationEnabled }.getOrDefault(true)
}
