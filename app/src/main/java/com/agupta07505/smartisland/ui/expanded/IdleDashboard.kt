/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.expanded

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp

private val DashboardSurface = Color(0xFF222222)
private val WifiBlue = Color(0xFF3B82F6)
private val FlashAmber = Color(0xFFFBBF24)
private const val MAX_LAUNCHER_ICONS = 4

/**
 * The Large idle state: search + status badges, a mini launcher and a 2x2 quick-control matrix.
 * Typing in the search field filters installed apps and the controls; focus is reported through
 * [onInputStateChanged] so the overlay window becomes focusable only while typing.
 */
@Composable
internal fun IdleDashboard(
    apps: List<LaunchableApp>?,
    onLaunchApp: (String) -> Unit,
    onCollapse: () -> Unit,
    onInputStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val installed = remember { AppShortcutProvider.installedApps(context) }

    DisposableEffect(Unit) { onDispose { onInputStateChanged(false) } }

    val torch = rememberTorch()
    val wifiOn = rememberBroadcastValue(WifiManager.WIFI_STATE_CHANGED_ACTION) {
        runCatching { (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled }
            .getOrDefault(false)
    }
    val dndOn = rememberBroadcastValue(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .currentInterruptionFilter.let { it != NotificationManager.INTERRUPTION_FILTER_ALL && it != 0 }
    }

    val controls = listOf(
        QuickControl("Wi-Fi", Icons.Rounded.Wifi, wifiOn, WifiBlue, Color.White, null) {
            startSettingsActivity(context, Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            onCollapse()
        },
        QuickControl("Flashlight", Icons.Rounded.FlashlightOn, torch.on, FlashAmber, Color(0xFF1A1300),
            if (torch.on) "ON" else null) { torch.toggle() },
        QuickControl("Do Not Disturb", Icons.Rounded.DoNotDisturbOn, dndOn, Color(0xFF6366F1), Color.White, null) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(
                    if (dndOn) NotificationManager.INTERRUPTION_FILTER_ALL
                    else NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
            } else {
                startSettingsActivity(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                onCollapse()
            }
        },
        QuickControl("Island Settings", Icons.Rounded.Settings, false, DashboardSurface, Color.White, null) {
            onLaunchApp(context.packageName)
        }
    )

    val trimmed = query.trim()
    val shownControls = if (trimmed.isEmpty()) controls
    else controls.filter { it.label.contains(trimmed, ignoreCase = true) }
    val shownApps: List<LaunchableApp> = if (trimmed.isEmpty()) {
        favoriteApps(apps, installed)
    } else {
        installed.filter { it.label.contains(trimmed, ignoreCase = true) }.take(MAX_LAUNCHER_ICONS)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onFocusChanged = onInputStateChanged,
                modifier = Modifier.weight(1f)
            )
            StatusBadges()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF151515))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (shownApps.isEmpty()) {
                Text(
                    text = if (trimmed.isEmpty()) "No apps available" else "No apps match \"$trimmed\"",
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                shownApps.forEach { app ->
                    LauncherIcon(app = app, onClick = {
                        onLaunchApp(app.packageName)
                    })
                }
            }
        }

        if (shownControls.isNotEmpty()) {
            shownControls.chunked(2).forEach { rowControls ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowControls.forEach { control ->
                        ControlCard(control = control, modifier = Modifier.weight(1f))
                    }
                    if (rowControls.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuickControl(
    val label: String,
    val icon: ImageVector,
    val active: Boolean,
    val activeColor: Color,
    val activeContent: Color,
    val badge: String?,
    val onClick: () -> Unit
)

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    // The overlay window only becomes focusable after the focus report below reaches the service, so
    // the field re-claims focus and shows the keyboard once the window has been updated.
    LaunchedEffect(focused) {
        if (focused) {
            kotlinx.coroutines.delay(150)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DashboardSurface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    "Search apps or features…",
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        onFocusChanged(it.isFocused)
                    }
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun StatusBadges() {
    val context = LocalContext.current
    val battery = rememberBroadcastValue(Intent.ACTION_BATTERY_CHANGED) {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        Pair(if (level >= 0 && scale > 0) level * 100 / scale else -1, charging)
    }
    val bluetoothOn = rememberBroadcastValue(BluetoothAdapter.ACTION_STATE_CHANGED) {
        runCatching { (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true }
            .getOrDefault(false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (battery.first >= 0) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(DashboardSurface)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (battery.second) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                    contentDescription = null,
                    tint = if (battery.second) Color(0xFF34D399) else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text("${battery.first}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (bluetoothOn) WifiBlue.copy(alpha = 0.35f) else DashboardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Bluetooth,
                contentDescription = if (bluetoothOn) "Bluetooth on" else "Bluetooth off",
                tint = if (bluetoothOn) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LauncherIcon(app: LaunchableApp, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DashboardSurface),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(30.dp))
            }
        }
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ControlCard(control: QuickControl, modifier: Modifier = Modifier) {
    val background = if (control.active) control.activeColor else DashboardSurface
    val content = if (control.active) control.activeContent else Color(0xFFD1D5DB)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .clickable(onClick = control.onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(control.icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(
            control.label,
            color = content,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        control.badge?.let {
            Text(
                it,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/** Pinned/recent apps first; otherwise a sensible default set (Settings, Camera, Music, Maps). */
private fun favoriteApps(apps: List<LaunchableApp>?, installed: List<LaunchableApp>): List<LaunchableApp> {
    if (!apps.isNullOrEmpty()) return apps.take(MAX_LAUNCHER_ICONS)
    val preferred = listOf("settings", "camera", "music", "maps")
    val picked = preferred.mapNotNull { key ->
        installed.firstOrNull { it.label.contains(key, ignoreCase = true) }
    }.distinctBy { it.packageName }
    return (picked + installed.filter { it !in picked }).take(MAX_LAUNCHER_ICONS)
}

private fun startSettingsActivity(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private class TorchState(val on: Boolean, val toggle: () -> Unit)

@Composable
private fun rememberTorch(): TorchState {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember {
        runCatching {
            manager?.cameraIdList?.firstOrNull {
                val c = manager.getCameraCharacteristics(it)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    DisposableEffect(manager, cameraId) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) on = enabled
            }
        }
        if (cameraId != null) runCatching { manager?.registerTorchCallback(callback, null) }
        onDispose { runCatching { manager?.unregisterTorchCallback(callback) } }
    }
    return TorchState(on) {
        if (cameraId != null) runCatching { manager?.setTorchMode(cameraId, !on) }
    }
}

/** Re-reads [read] whenever one of the system [actions] is broadcast. */
@Composable
private fun <T> rememberBroadcastValue(vararg actions: String, read: () -> T): T {
    val context = LocalContext.current
    var value by remember { mutableStateOf(read()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                value = read()
            }
        }
        val filter = IntentFilter().apply { actions.forEach { addAction(it) } }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return value
}
