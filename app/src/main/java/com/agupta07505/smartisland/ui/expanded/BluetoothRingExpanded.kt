/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.UtilityActions
import kotlinx.coroutines.delay
import com.agupta07505.smartisland.ui.IslandText as Text

/** Bluetooth device card in the Pixel ring glance look. */
@Composable
internal fun BluetoothRingExpanded(
    notification: IslandNotification?,
    batteryLevel: Int?,
    bottomPadding: Dp,
    settings: SmartIslandSettings
) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    val accent = Color(settings.bluetoothColor)
    val deviceName = notification?.title?.ifBlank { null } ?: "Bluetooth device"
    val showBattery = settings.showBluetoothBattery && batteryLevel != null

    var turnedOff by remember(notification?.key) { mutableStateOf(false) }
    var presses by remember { mutableIntStateOf(0) }

    // with Shizuku the switch turns Bluetooth off
    LaunchedEffect(presses) {
        if (presses > 0) {
            delay(800L)
            turnedOff = !UtilityActions.bluetoothEnabled(context)
        }
    }

    val ringColor = if (turnedOff) palette.tint(0.2f) else accent
    val numberColor = if (turnedOff) palette.textSecondary else accent
    val progress = if (turnedOff) 0f else if (showBattery) (batteryLevel!! / 100f).coerceIn(0f, 1f) else 1f
    val trackColor = palette.tint(0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(64.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
                )
                if (progress > 0f) {
                    drawArc(
                        color = ringColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Icon(
                imageVector = if (turnedOff) Icons.Rounded.BluetoothDisabled else Icons.Rounded.BluetoothConnected,
                contentDescription = "Bluetooth",
                tint = if (turnedOff) palette.textSecondary else palette.text,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    turnedOff -> "Off"
                    showBattery -> "$batteryLevel%"
                    else -> "On"
                },
                color = numberColor,
                fontSize = 30.sp,
                lineHeight = 33.sp,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = if (turnedOff) "Bluetooth turned off" else deviceName,
                color = palette.textSecondary,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!turnedOff) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.5.dp, palette.tint(0.4f), CircleShape)
                    .clip(CircleShape)
                    .bounceClick {
                        UtilityActions.run(context, UtilityType.Bluetooth)
                        presses++
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = "Turn off Bluetooth",
                    tint = palette.text,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
