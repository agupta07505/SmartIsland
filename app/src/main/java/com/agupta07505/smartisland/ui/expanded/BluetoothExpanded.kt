/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AirplanemodeInactive
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.VpnKeyOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.DeviceStates
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.modeFallbackIcon
import kotlinx.coroutines.delay
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
fun BluetoothExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val deviceName = notification?.title?.ifBlank { null } ?: "Bluetooth Device"
    val statusText = notification?.text?.ifBlank { null } ?: "Connected"

    val batteryLevel = remember(notification?.text, notification?.progress) {
        notification?.let { notif ->
            if (notif.progress in 1..100 && notif.progressMax == 100) notif.progress
            else {
                val match = Regex("""(\d{1,3})%""").find(notif.text)
                match?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)
            }
        }
    }

    // A Bluetooth device (no category) gets the Pixel ring look
    if (notification?.category == null) {
        BluetoothRingExpanded(
            notification = notification,
            batteryLevel = batteryLevel,
            bottomPadding = bottomPadding,
            settings = settings
        )
        return
    }

    // Wi-Fi / VPN "disconnected" reuse this card with a warning accent
    val lost = isConnectionLost(notification?.category)
    val accent = connectivityAccent(notification?.category, settings.bluetoothColor, settings.cardEventColor)
    val batteryColor =
        if ((batteryLevel ?: 100) <= 20) LocalIslandPalette.current.danger else LocalIslandPalette.current.success
    val batteryIcon = when {
        (batteryLevel ?: 100) >= 80 -> Icons.Rounded.BatteryFull
        (batteryLevel ?: 100) >= 40 -> Icons.Rounded.Battery5Bar
        (batteryLevel ?: 100) >= 20 -> Icons.Rounded.BatteryChargingFull
        else -> Icons.Rounded.BatteryAlert
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectionInfo(
            category = notification?.category,
            deviceName = deviceName,
            statusText = statusText,
            lost = lost,
            accent = accent,
            batteryLevel = batteryLevel,
            batteryColor = batteryColor,
            batteryIcon = batteryIcon,
            settings = settings
        )

        Spacer(Modifier.width(8.dp))
        if (hasCategoryGlyph(notification?.category)) {
            CategoryGlyph(notification?.category, accent, settings)
        } else {
            BluetoothTrailing(batteryLevel, batteryColor, batteryIcon, settings)
        }
    }
}

@Composable
private fun RowScope.ConnectionInfo(
    category: String?,
    deviceName: String,
    statusText: String,
    lost: Boolean,
    accent: Color,
    batteryLevel: Int?,
    batteryColor: Color,
    batteryIcon: ImageVector,
    settings: SmartIslandSettings
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f, fill = false)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = connectivityLeadingIcon(category),
                contentDescription = "Connection",
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }

        Column {
            Text(
                text = deviceName,
                color = LocalIslandPalette.current.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (lost) accent else LocalIslandPalette.current.success)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (settings.showBluetoothBattery && batteryLevel != null
                            && !lost) "Connected" else statusText,
                        color = if (lost) accent else LocalIslandPalette.current.success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

            if (!lost && settings.showBluetoothBattery && batteryLevel != null) {
                BatteryChip(batteryLevel, batteryColor, batteryIcon)
            }
            }
        }
    }
}

@Composable
private fun BatteryChip(batteryLevel: Int, batteryColor: Color, batteryIcon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(batteryColor.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = batteryIcon,
            contentDescription = null,
            tint = batteryColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "$batteryLevel%",
            color = batteryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun hasCategoryGlyph(category: String?): Boolean =
    category == "wifi" || category == "wifi_disconnected" || category == "vpn" ||
        category == "vpn_disconnected" || isWiredCategory(category) || DeviceStates.isDeviceState(category)

@Composable
private fun CategoryGlyph(category: String?, accent: Color, settings: SmartIslandSettings) {
    // Wi-Fi and VPN events reuse this card
    when (category) {
        // Wi-Fi: signal arcs that count 1 - 2 - 3 - 2 and round again; lost
        "wifi" -> {
            WifiSignalBox(accent = Color(settings.bluetoothColor))
        }
        "wifi_disconnected" -> {
            WifiSignalBox(accent = accent, lost = true)
        }
        "vpn" -> {
            VpnGlobeBox(accent = Color(settings.bluetoothColor))
        }
        // Disconnected VPN: the same globe with a slash through it, in red
        "vpn_disconnected" -> {
            VpnGlobeBox(accent = accent, slashed = true)
        }
        // USB and wired headphones: one icon tile on the right, red when unplugged
        "usb_connected", "usb_disconnected", "headphones_connected", "headphones_disconnected" ->
            ConnectionTile(category, accent, slashed = isUnpluggedWired(category))
        else -> if (DeviceStates.isDeviceState(category)) {
            ConnectionTile(category, accent, slashed = deviceStateSlashed(category))
        }
    }
}

@Composable
private fun ConnectionTile(category: String?, accent: Color, slashed: Boolean) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        SlashableIcon(
            imageVector = connectivityIcon(category),
            tint = accent,
            slashed = slashed,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun BluetoothTrailing(
    batteryLevel: Int?,
    batteryColor: Color,
    batteryIcon: ImageVector,
    settings: SmartIslandSettings
) {
    if (settings.showBluetoothBattery && batteryLevel != null) {
        var showBatterySide by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000L)
                showBatterySide = !showBatterySide
            }
        }

        BatterySwitchGlyph(showBatterySide, batteryLevel, batteryColor, batteryIcon, settings)
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_bluetooth_device),
            contentDescription = "Bluetooth Device Image",
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun BatterySwitchGlyph(
    showBatterySide: Boolean,
    batteryLevel: Int,
    batteryColor: Color,
    batteryIcon: ImageVector,
    settings: SmartIslandSettings
) {
    AnimatedContent(
        targetState = showBatterySide,
        transitionSpec = {
            (fadeIn(animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)) +
                    scaleIn(initialScale = 0.8f, animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)))
                .togetherWith(
                    fadeOut(animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)) +
                            scaleOut(targetScale = 0.8f, animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f))
                )
        },
        label = "BluetoothExpandedSwitch"
    ) { isBatteryState ->
        if (isBatteryState) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(batteryColor.copy(alpha = 0.12f))
                    .border(1.dp, batteryColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = "Battery Level",
                        tint = batteryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$batteryLevel%",
                        color = batteryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(settings.bluetoothColor).copy(alpha = 0.12f))
                    .border(1.dp, Color(settings.bluetoothColor).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Earbuds",
                    tint = Color(settings.bluetoothColor),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

internal fun connectivityIcon(category: String?): ImageVector = deviceStateIcon(category) ?: when (category) {
    "wifi" -> Icons.Rounded.Wifi
    "vpn" -> Icons.Rounded.VpnKey
    "wifi_disconnected" -> Icons.Rounded.WifiOff
    "vpn_disconnected" -> Icons.Rounded.VpnKeyOff
    "usb_connected" -> Icons.Rounded.Usb
    "usb_disconnected" -> Icons.Rounded.Usb
    "headphones_connected" -> Icons.Rounded.Headphones
    "headphones_disconnected" -> Icons.Rounded.Headphones
    else -> Icons.Rounded.BluetoothConnected
}

internal fun connectivityLeadingIcon(category: String?): ImageVector =
    if (category == "wifi_disconnected") Icons.Rounded.Wifi else connectivityIcon(category)

internal fun isConnectionLost(category: String?): Boolean =
    category == "wifi_disconnected" || category == "vpn_disconnected" ||
        category == "usb_disconnected" || category == "headphones_disconnected"

internal fun isWiredCategory(category: String?): Boolean =
    category == "usb_connected" || category == "usb_disconnected" ||
        category == "headphones_connected" || category == "headphones_disconnected"

internal fun connectivityAccent(category: String?, bluetoothColor: Long, eventColor: Long? = null): Color =
    eventColor?.let { Color(it) } ?: deviceStateAccent(category) ?: when (category) {
    "usb_connected", "usb_disconnected" -> USB_ACCENT
    "headphones_connected", "headphones_disconnected" -> HEADPHONES_ACCENT
    else -> if (isConnectionLost(category)) CONNECTION_LOST_RED else Color(bluetoothColor)
}

internal val USB_ACCENT = Color(0xFFD946EF)
internal val HEADPHONES_ACCENT = Color(0xFF14B8A6)
internal val AIRPLANE_ACCENT = Color(0xFF38BDF8)
internal val RING_ACCENT = Color(0xFF22C55E)
internal val VIBRATE_ACCENT = Color(0xFFF59E0B)
internal val SILENT_ACCENT = Color(0xFF94A3B8)
internal val DND_ACCENT = Color(0xFF8B5CF6)
internal val MODE_ACCENT = Color(0xFF6366F1)

internal fun deviceStateIcon(category: String?): ImageVector? = when (category) {
    DeviceStates.AIRPLANE_ON -> Icons.Rounded.AirplanemodeActive
    DeviceStates.AIRPLANE_OFF -> Icons.Rounded.AirplanemodeInactive
    DeviceStates.RINGER_RING -> Icons.Rounded.NotificationsActive
    DeviceStates.RINGER_VIBRATE -> Icons.Rounded.Vibration
    DeviceStates.RINGER_SILENT -> Icons.Rounded.VolumeOff
    else -> DeviceStates.kindOf(category)?.let { kind ->
        if (DeviceStates.eventOf(category) == IslandEvent.DND && DeviceStates.isOff(category)) {
            Icons.Rounded.DoNotDisturbOff
        } else {
            modeFallbackIcon(kind)
        }
    }
}

/** A mode that ended has no icon of its own for it, so its glyph is slashed; the others have an "off" icon. */
internal fun deviceStateSlashed(category: String?): Boolean =
    DeviceStates.eventOf(category) == IslandEvent.MODE && DeviceStates.isOff(category)

internal fun deviceStateAccent(category: String?): Color? = when (DeviceStates.eventOf(category)) {
    // Airplane mode going off is red by default; a colour the user picked for the event still wins over it
    IslandEvent.AIRPLANE_MODE -> if (DeviceStates.isOff(category)) CONNECTION_LOST_RED else AIRPLANE_ACCENT
    IslandEvent.RING -> RING_ACCENT
    IslandEvent.VIBRATE -> VIBRATE_ACCENT
    IslandEvent.SILENT -> SILENT_ACCENT
    IslandEvent.DND -> DND_ACCENT
    IslandEvent.MODE -> MODE_ACCENT
    else -> null
}

internal fun isUnpluggedWired(category: String?): Boolean =
    category == "usb_disconnected" || category == "headphones_disconnected"

/** Icon with an optional slash from top left to bottom right. */
@Composable
internal fun SlashableIcon(
    imageVector: ImageVector,
    tint: Color,
    slashed: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    /** How much of the slash is drawn, 0..1: a glyph that morphs into an off state draws it on, an on state wipes it off. */
    slashAmount: Float = if (slashed) 1f else 0f
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint,
            modifier = Modifier.fillMaxSize())
        if (slashAmount > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                val from = Offset(size.width * 0.19f, size.height * 0.19f)
                val full = Offset(size.width * 0.81f, size.height * 0.81f)
                val a = slashAmount.coerceIn(0f, 1f)
                val to = Offset(from.x + (full.x - from.x) * a, from.y + (full.y - from.y) * a)
                drawLine(Color.Black.copy(alpha = 0.55f), from, to, strokeWidth = size.width * 0.19f,
                    cap = StrokeCap.Round)
                drawLine(tint, from, to, strokeWidth = size.width * 0.085f, cap = StrokeCap.Round)
            }
        }
    }
}

internal val CONNECTION_LOST_RED = Color(0xFFEF4444)
