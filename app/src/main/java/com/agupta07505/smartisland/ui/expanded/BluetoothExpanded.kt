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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.VpnKeyOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    // Wi-Fi / VPN "disconnected" reuse this card with a warning accent, a red or amber status dot and no battery chip.
    val lost = isConnectionLost(notification?.category)
    val accent = connectivityAccent(notification?.category, settings.bluetoothColor)
    val batteryColor = if ((batteryLevel ?: 100) <= 20) LocalIslandPalette.current.danger else LocalIslandPalette.current.success
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
                    imageVector = connectivityLeadingIcon(notification?.category),
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
                            text = if (settings.showBluetoothBattery && batteryLevel != null && !lost) "Connected" else statusText,
                            color = if (lost) accent else LocalIslandPalette.current.success,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!lost && settings.showBluetoothBattery && batteryLevel != null) {
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
                }
            }
        }

        // Wi-Fi and VPN events reuse this card. Wi-Fi has no picture on the right; VPN shows a globe; only Bluetooth has earbuds.
        when (notification?.category) {
            // Wi-Fi: signal arcs that count 1 - 2 - 3 - 2 and round again; lost: the same glyph slashed, standing still.
            "wifi" -> {
                Spacer(Modifier.width(8.dp))
                WifiSignalBox(accent = Color(settings.bluetoothColor))
                return@Row
            }
            "wifi_disconnected" -> {
                Spacer(Modifier.width(8.dp))
                WifiSignalBox(accent = accent, lost = true)
                return@Row
            }
            "vpn" -> {
                Spacer(Modifier.width(8.dp))
                VpnGlobeBox(accent = Color(settings.bluetoothColor))
                return@Row
            }
            // Disconnected VPN: the same globe with a slash through it, in red.
            "vpn_disconnected" -> {
                Spacer(Modifier.width(8.dp))
                VpnGlobeBox(accent = accent, slashed = true)
                return@Row
            }
            // USB and wired headphones: one icon tile on the right, red when unplugged.
            "usb_connected", "usb_disconnected", "headphones_connected", "headphones_disconnected" -> {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SlashableIcon(
                        imageVector = connectivityIcon(notification?.category),
                        tint = accent,
                        slashed = isUnpluggedWired(notification?.category),
                        modifier = Modifier.size(24.dp)
                    )
                }
                return@Row
            }
        }
        Spacer(Modifier.width(8.dp))

        if (settings.showBluetoothBattery && batteryLevel != null) {
            var showBatterySide by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(3000L)
                    showBatterySide = !showBatterySide
                }
            }

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
}

/** Icon for the Bluetooth-style island card; Wi-Fi and VPN events reuse this card with a category. */
internal fun connectivityIcon(category: String?): androidx.compose.ui.graphics.vector.ImageVector = when (category) {
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

/**
 * Icon for the left glyph (the circle): a lost Wi-Fi keeps the plain Wi-Fi icon there, in red; the slash is on the
 * right-hand glyph. Everything else uses [connectivityIcon].
 */
internal fun connectivityLeadingIcon(category: String?): androidx.compose.ui.graphics.vector.ImageVector =
    if (category == "wifi_disconnected") Icons.Rounded.Wifi else connectivityIcon(category)

internal fun isConnectionLost(category: String?): Boolean =
    category == "wifi_disconnected" || category == "vpn_disconnected" ||
        category == "usb_disconnected" || category == "headphones_disconnected"

/** True for the USB and wired headphone cards, which reuse this card with a plain icon. */
internal fun isWiredCategory(category: String?): Boolean =
    category == "usb_connected" || category == "usb_disconnected" ||
        category == "headphones_connected" || category == "headphones_disconnected"

/**
 * The colour of a Wi-Fi / VPN / Bluetooth card: the Bluetooth colour from the settings for the connected cards, full-
 * strength red (the same red the Bluetooth card uses for a low battery) for a lost Wi-Fi or VPN.
 */
internal fun connectivityAccent(category: String?, bluetoothColor: Long): Color = when (category) {
    // USB and wired headphones keep one colour of their own whether plugged in or out; only the slash tells them apart.
    "usb_connected", "usb_disconnected" -> USB_ACCENT
    "headphones_connected", "headphones_disconnected" -> HEADPHONES_ACCENT
    else -> if (isConnectionLost(category)) CONNECTION_LOST_RED else Color(bluetoothColor)
}

internal val USB_ACCENT = Color(0xFFD946EF)
internal val HEADPHONES_ACCENT = Color(0xFF14B8A6)

/** USB and headphones are drawn as the plain icon with a slash through it when unplugged. */
internal fun isUnpluggedWired(category: String?): Boolean =
    category == "usb_disconnected" || category == "headphones_disconnected"

/** An icon with an optional slash from top left to bottom right, with a thin dark gap either side (same look as the VPN globe). */
@androidx.compose.runtime.Composable
internal fun SlashableIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    slashed: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint, modifier = Modifier.fillMaxSize())
        if (slashed) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val from = androidx.compose.ui.geometry.Offset(size.width * 0.19f, size.height * 0.19f)
                val to = androidx.compose.ui.geometry.Offset(size.width * 0.81f, size.height * 0.81f)
                drawLine(Color.Black.copy(alpha = 0.55f), from, to, strokeWidth = size.width * 0.19f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(tint, from, to, strokeWidth = size.width * 0.085f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

internal val CONNECTION_LOST_RED = Color(0xFFEF4444)
