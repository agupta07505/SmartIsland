/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.ui.IslandPalette
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.EarbudBattery
import com.agupta07505.smartisland.util.runCatchingLogged
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

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

    val batteryColor = if ((batteryLevel ?: 100) <= 20) IslandPalette.danger else IslandPalette.success
    val batteryIcon = when {
        (batteryLevel ?: 100) >= 80 -> Icons.Rounded.BatteryFull
        (batteryLevel ?: 100) >= 40 -> Icons.Rounded.Battery5Bar
        (batteryLevel ?: 100) >= 20 -> Icons.Rounded.BatteryChargingFull
        else -> Icons.Rounded.BatteryAlert
    }

    val context = LocalContext.current
    // L/R/case levels replace the single gauge when the alert carries them (Rule_Book §4.6).
    val earbuds = remember(notification?.text) { EarbudBattery.parse(notification?.text) }
    val showSplitGauges = settings.showBluetoothBattery && earbuds != null

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
                    .background(Color(settings.bluetoothColor).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BluetoothConnected,
                    contentDescription = stringResource(R.string.a11y_bluetooth_connected),
                    tint = Color(settings.bluetoothColor),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = deviceName,
                    color = Color.White,
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
                                .background(IslandPalette.success)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (settings.showBluetoothBattery && (batteryLevel != null || showSplitGauges)) "Connected" else statusText,
                            color = IslandPalette.success,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (settings.showBluetoothBattery && batteryLevel != null && !showSplitGauges) {
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

        Spacer(Modifier.width(8.dp))

        // Right side: L/R/case gauges, else earbuds / battery alternating animation
        if (showSplitGauges && earbuds != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EarbudGauge(label = "L", level = earbuds.left)
                EarbudGauge(label = "R", level = earbuds.right)
                EarbudGauge(label = "Case", level = earbuds.case)
            }
        } else if (settings.showBluetoothBattery && batteryLevel != null) {
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
                                contentDescription = stringResource(R.string.a11y_battery_level),
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
                            contentDescription = stringResource(R.string.a11y_earbuds),
                            tint = Color(settings.bluetoothColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_bluetooth_device),
                contentDescription = stringResource(R.string.a11y_bluetooth_device_image),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(Modifier.width(8.dp))

        // Shortcut to the OS Bluetooth settings: the island collapses first, then the dashboard opens.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IslandPalette.fill)
                .bounceClick {
                    onCollapse()
                    runCatchingLogged("BluetoothExpanded", "Opening Bluetooth settings failed") {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.a11y_bluetooth_settings),
                tint = IslandPalette.label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** One earbud or case level: a short label over a small fixed-size battery bar and its percentage. */
@Composable
private fun EarbudGauge(label: String, level: Int?) {
    val color = when {
        level == null -> IslandPalette.secondaryLabel
        level <= 20 -> IslandPalette.danger
        else -> IslandPalette.success
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width(34.dp)
    ) {
        Text(text = label, color = IslandPalette.secondaryLabel, fontSize = 10.sp, maxLines = 1)
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(IslandPalette.fill)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(((level ?: 0) / 100f).coerceIn(0f, 1f))
                    .background(color)
            )
        }
        Text(
            text = level?.let { "$it%" } ?: "--",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
