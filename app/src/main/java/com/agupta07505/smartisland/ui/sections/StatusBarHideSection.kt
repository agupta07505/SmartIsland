/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.StatusBarHider
import com.agupta07505.smartisland.util.StatusBarReapplier
import com.agupta07505.smartisland.util.StatusBarSwitch
import com.agupta07505.smartisland.util.StatusBarSystemIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shizuku readiness, in the order the user has to fix it. */
internal enum class ShizukuReadiness { NotInstalled, NotRunning, NoPermission, Ready }

internal fun shizukuReadiness(context: Context): ShizukuReadiness = when {
    !ShizukuManager.isInstalled(context) -> ShizukuReadiness.NotInstalled
    !ShizukuManager.isBinderAvailable() -> ShizukuReadiness.NotRunning
    !ShizukuManager.hasPermission() -> ShizukuReadiness.NoPermission
    else -> ShizukuReadiness.Ready
}

internal fun statusBarSystemIconLabel(icon: StatusBarSystemIcon): String = when (icon) {
    StatusBarSystemIcon.Battery -> "Battery"
    StatusBarSystemIcon.Wifi -> "Wi-Fi"
    StatusBarSystemIcon.MobileSignal -> "Mobile signal"
    StatusBarSystemIcon.MobileData -> "Mobile data type (4G, 5G)"
    StatusBarSystemIcon.Bluetooth -> "Bluetooth"
    StatusBarSystemIcon.Alarm -> "Alarm"
    StatusBarSystemIcon.Vpn -> "VPN"
    StatusBarSystemIcon.Airplane -> "Airplane mode"
    StatusBarSystemIcon.Hotspot -> "Hotspot"
    StatusBarSystemIcon.Location -> "Location"
    StatusBarSystemIcon.DoNotDisturb -> "Do not disturb"
    StatusBarSystemIcon.Cast -> "Cast"
    StatusBarSystemIcon.Nfc -> "NFC"
    StatusBarSystemIcon.Vibrate -> "Vibrate"
    StatusBarSystemIcon.Mute -> "Mute"
    StatusBarSystemIcon.Headset -> "Headset"
    StatusBarSystemIcon.Privacy -> "Microphone and camera"
    StatusBarSystemIcon.Sync -> "Sync"
    StatusBarSystemIcon.WorkProfile -> "Work profile"
    StatusBarSystemIcon.AutoRotate -> "Auto-rotate screen"
    StatusBarSystemIcon.Firewall -> "Firewall"
    StatusBarSystemIcon.Ethernet -> "Ethernet"
    StatusBarSystemIcon.DataSaver -> "Data saver"
    StatusBarSystemIcon.CallStrength -> "Call strength"
    StatusBarSystemIcon.Roaming -> "Roaming indicator"
    StatusBarSystemIcon.HdCalling -> "HD calling (VoLTE/VoNR)"
    StatusBarSystemIcon.WifiCalling -> "Wi-Fi calling (VoWiFi)"
}

internal fun statusBarStateLabel(
    hidden: Boolean,
    readiness: ShizukuReadiness,
    status: StatusBarReapplier.Status
): String = when {
    !hidden -> "Off"
    readiness != ShizukuReadiness.Ready || status == StatusBarReapplier.Status.ShizukuLost -> "Needs Shizuku"
    status == StatusBarReapplier.Status.Failed -> "Failed"
    status == StatusBarReapplier.Status.Applied -> "Hidden"
    else -> "Applying…"
}

@Composable
fun StatusBarHideSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by StatusBarReapplier.shared.collectAsState()

    // Shizuku can start or stop while this screen is open
    var readiness by remember { mutableStateOf(shizukuReadiness(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            readiness = shizukuReadiness(context)
            delay(1_500L)
        }
    }
    val restoringState = remember { mutableStateOf(false) }

    fun explainShizuku() {
        when (readiness) {
            ShizukuReadiness.NoPermission -> ShizukuManager.requestPermission()
            ShizukuReadiness.NotInstalled -> context.showToast("Install Shizuku and start it first.", long = true)
            else -> context.showToast("Shizuku is not running. Start it, then try again.", long = true)
        }
    }

    val stateLabel = statusBarStateLabel(settings.statusBarHidden, readiness, status)
    val needsShizuku = readiness != ShizukuReadiness.Ready

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusBarHeaderBlock(stateLabel = stateLabel)
            if (needsShizuku) {
                ShizukuNeededBlock(readiness = readiness)
            }
            HideSwitchBlock(
                settings = settings,
                repository = repository,
                scope = scope,
                status = status,
                needsShizuku = needsShizuku,
                explainShizuku = { explainShizuku() }
            )
            WhatToHideBlock(settings = settings, repository = repository, scope = scope)
            KeepAfterRebootBlock(settings = settings, repository = repository, scope = scope)
            RestoreBlock(
                repository = repository,
                scope = scope,
                context = context,
                needsShizuku = needsShizuku,
                restoringState = restoringState
            )
        }
    }
}

@Composable
private fun StatusBarHeaderBlock(stateLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hide the status bar",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stateLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = when (stateLabel) {
                "Hidden" -> Color(0xFF0F9F6E)
                "Failed", "Needs Shizuku" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
    Text(
        text = "Hides the parts of the status bar you pick (notification icons, clock, system icons) so the island is the " +
            "only thing at the top. Applied through Shizuku and put back again after a screen on, an unlock or a SystemUI restart.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ShizukuNeededBlock(readiness: ShizukuReadiness) {
    Text(
        text = when (readiness) {
            ShizukuReadiness.NotInstalled -> "Shizuku is not installed. This feature needs it."
            ShizukuReadiness.NotRunning -> "Shizuku is installed but not running. Start it to use this feature."
            else -> "Shizuku is running but has not been given permission to Smart Island."
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error
    )
    if (readiness == ShizukuReadiness.NoPermission) {
        OutlinedButton(onClick = { ShizukuManager.requestPermission() }, modifier = Modifier.fillMaxWidth()) {
            Text("Grant Shizuku permission")
        }
    }
}

@Composable
private fun HideSwitchBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    status: StatusBarReapplier.Status,
    needsShizuku: Boolean,
    explainShizuku: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hide status bar",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Switch(
            checked = settings.statusBarHidden,
            onCheckedChange = { value ->
                if (value && needsShizuku) {
                    explainShizuku()
                } else {
                    scope.launch { StatusBarSwitch.set(repository, value, "settings") }
                }
            }
        )
    }
    if (settings.statusBarHidden && status == StatusBarReapplier.Status.Failed) {
        Text(
            text = "The last attempt failed. It is retried on the next screen on and every 30 seconds.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun WhatToHideBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    Text(
        text = "What to hide",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    StatusBarSwitchRow(
        label = "Notification icons",
        checked = settings.statusBarHideNotificationIcons,
        onChange = { value -> scope.launch { repository.setStatusBarHideNotificationIcons(value) } }
    )
    StatusBarSwitchRow(
        label = "Clock",
        checked = settings.statusBarHideClock,
        onChange = { value -> scope.launch { repository.setStatusBarHideClock(value) } }
    )
    StatusBarSwitchRow(
        label = "System icons",
        checked = settings.statusBarHideAllSystemIcons,
        onChange = { value ->
            scope.launch {
                // Turning "all" off starts from every icon hidden
                if (!value && settings.statusBarHiddenSystemIcons.isEmpty()) {
                    repository.setStatusBarHiddenSystemIcons(StatusBarSystemIcon.entries.map { it.key }.toSet())
                }
                repository.setStatusBarHideAllSystemIcons(value)
            }
        }
    )
    if (settings.statusBarHideAllSystemIcons) {
        HintText("All system icons are hidden. Turn this off to choose them one by one.")
    } else {
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusBarSystemIcon.entries.forEach { icon ->
                StatusBarSwitchRow(
                    label = statusBarSystemIconLabel(icon),
                    checked = icon.key in settings.statusBarHiddenSystemIcons,
                    onChange = { value -> scope.launch { repository.setStatusBarSystemIconHidden(icon.key, value) } },
                    compact = true
                )
            }
        }
    }
    if (settings.statusBarHidden && settings.statusBarPlan().isEmpty) {
        Text(
            text = "Nothing is selected, so the status bar is left as it is.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun KeepAfterRebootBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Keep hidden after reboot",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = settings.statusBarHidePersistent,
            onCheckedChange = { value -> scope.launch { repository.setStatusBarHidePersistent(value) } }
        )
    }
    Text(
        text = "On: the hide is applied again when the phone boots or the app is updated. " +
            "Off: the status bar is put back to normal at boot.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RestoreBlock(
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    needsShizuku: Boolean,
    restoringState: MutableState<Boolean>
) {
    var restoring by restoringState
    OutlinedButton(
        onClick = {
            if (needsShizuku) {
                // Switch the setting off anyway
                scope.launch { repository.setStatusBarHidden(false) }
                context.showToast(
                    "Turned off. The status bar is restored as soon as Shizuku is available.",
                    long = true
                )
            } else {
                restoring = true
                scope.launch {
                    repository.setStatusBarHidden(false)
                    val result = StatusBarHider.restore()
                    restoring = false
                    context.showToast(
                        if (result.isSuccess) "Status bar restored to the system default." else "Restore failed. Try again.",
                        long = true
                    )
                }
            }
        },
        enabled = !restoring,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (restoring) "Restoring…" else "Restore all to system default")
    }
    Text(
        text = "Clears every flag and setting this feature wrote (hide flags, immersive policy and the icon list), " +
            "including leftovers from an earlier hide.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatusBarSwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontWeight = if (compact) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
