/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import kotlinx.coroutines.launch

private val PRESET_COLORS = listOf(
    0xFF10B981L to "Emerald",
    0xFF38BDF8L to "Sky",
    0xFF6366F1L to "Indigo",
    0xFFFF6B9AL to "Pink",
    0xFFEF4444L to "Coral",
    0xFFF59E0BL to "Amber",
    0xFF8B5CF6L to "Purple"
)

@Composable
fun CustomizationsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var currentColorTarget by remember { mutableStateOf("") }
    var colorPickerTitle by remember { mutableStateOf("") }
    var initialColor by remember { mutableStateOf(0xFF10B981L) }

    if (showDialog) {
        RgbColorPickerDialog(
            title = colorPickerTitle,
            initialColor = initialColor,
            onDismiss = { showDialog = false },
            onSave = { color ->
                showDialog = false
                scope.launch {
                    when (currentColorTarget) {
                        "battery" -> repository.setBatteryColor(color)
                        "notification" -> repository.setNotificationDotColor(color)
                        "music" -> repository.setMusicVisualizerColor(color)
                        "hotspot" -> repository.setHotspotColor(color)
                        "call" -> repository.setCallColor(color)
                        "live_activity" -> repository.setLiveActivityColor(color)
                        "transfer" -> repository.setTransferColor(color)
                        "navigation" -> repository.setNavigationColor(color)
                        "bluetooth" -> repository.setBluetoothColor(color)
                        "flashlight" -> repository.setFlashlightColor(color)
                        "screen_recording" -> repository.setScreenRecordingColor(color)
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Music Player Experience
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Music & Media Player",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure ambient backdrop artwork and visualizer animations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ambient Album Artwork Backdrop",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Render soft blurred album art as background glow in expanded music island",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.enableMusicArtworkBackground,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setEnableMusicArtworkBackground(checked) }
                        }
                    )
                }
            }
        }

        // Card 2: Feature Accent Colors Studio
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Feature Accent Colors Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize the vibrant indicator colors for all 11 dynamic island modes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Feature Color Rows
                FeatureColorRow(
                    title = "Battery Charging",
                    subtitle = "Charge percentage, lightning bolt & progress ring",
                    icon = Icons.Rounded.BatteryChargingFull,
                    selectedColor = settings.batteryColor,
                    onColorSelected = { scope.launch { repository.setBatteryColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.batteryColor
                        currentColorTarget = "battery"
                        colorPickerTitle = "Battery Charging Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Notification Alert Dot",
                    subtitle = "Collapsed mode unread indicator badge",
                    icon = Icons.Rounded.Notifications,
                    selectedColor = settings.notificationDotColor,
                    onColorSelected = { scope.launch { repository.setNotificationDotColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.notificationDotColor
                        currentColorTarget = "notification"
                        colorPickerTitle = "Notification Dot Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Music Frequency Visualizer",
                    subtitle = "Jumping live audio equalizer bars and seekbar",
                    icon = Icons.Rounded.MusicNote,
                    selectedColor = settings.musicVisualizerColor,
                    onColorSelected = { scope.launch { repository.setMusicVisualizerColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.musicVisualizerColor
                        currentColorTarget = "music"
                        colorPickerTitle = "Music Visualizer Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Phone & Incoming Calls",
                    subtitle = "Call timer, wave animation and phone glyph",
                    icon = Icons.Rounded.Call,
                    selectedColor = settings.callColor,
                    onColorSelected = { scope.launch { repository.setCallColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.callColor
                        currentColorTarget = "call"
                        colorPickerTitle = "Call Accent Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Live Activities & Timers",
                    subtitle = "Food delivery, ride tracking & countdown ring",
                    icon = Icons.Rounded.Navigation,
                    selectedColor = settings.liveActivityColor,
                    onColorSelected = { scope.launch { repository.setLiveActivityColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.liveActivityColor
                        currentColorTarget = "live_activity"
                        colorPickerTitle = "Live Activity Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "File Downloads & Transfers",
                    subtitle = "Progress ring and download speed indicator",
                    icon = Icons.Rounded.FileDownload,
                    selectedColor = settings.transferColor,
                    onColorSelected = { scope.launch { repository.setTransferColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.transferColor
                        currentColorTarget = "transfer"
                        colorPickerTitle = "Transfer Progress Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Maps & Turn Navigation",
                    subtitle = "Direction turn arrow and distance indicator",
                    icon = Icons.Rounded.Explore,
                    selectedColor = settings.navigationColor,
                    onColorSelected = { scope.launch { repository.setNavigationColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.navigationColor
                        currentColorTarget = "navigation"
                        colorPickerTitle = "Navigation Accent Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Mobile Hotspot & Tethering",
                    subtitle = "Connected device badge and broadcast wave",
                    icon = Icons.Rounded.WifiTethering,
                    selectedColor = settings.hotspotColor,
                    onColorSelected = { scope.launch { repository.setHotspotColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.hotspotColor
                        currentColorTarget = "hotspot"
                        colorPickerTitle = "Hotspot Accent Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Bluetooth Device",
                    subtitle = "Headset icon and battery status indicator",
                    icon = Icons.Rounded.BluetoothConnected,
                    selectedColor = settings.bluetoothColor,
                    onColorSelected = { scope.launch { repository.setBluetoothColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.bluetoothColor
                        currentColorTarget = "bluetooth"
                        colorPickerTitle = "Bluetooth Accent Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Flashlight & Torch",
                    subtitle = "Torch indicator glow and toggle status",
                    icon = Icons.Rounded.FlashlightOn,
                    selectedColor = settings.flashlightColor,
                    onColorSelected = { scope.launch { repository.setFlashlightColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.flashlightColor
                        currentColorTarget = "flashlight"
                        colorPickerTitle = "Flashlight Color"
                        showDialog = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FeatureColorRow(
                    title = "Screen Recording",
                    subtitle = "Active recording pulse dot and timer",
                    icon = Icons.Rounded.Videocam,
                    selectedColor = settings.screenRecordingColor,
                    onColorSelected = { scope.launch { repository.setScreenRecordingColor(it) } },
                    onCustomClicked = {
                        initialColor = settings.screenRecordingColor
                        currentColorTarget = "screen_recording"
                        colorPickerTitle = "Screen Recording Color"
                        showDialog = true
                    }
                )

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.setBatteryColor(0xFF10B981L)
                            repository.setNotificationDotColor(0xFF38BDF8L)
                            repository.setMusicVisualizerColor(0xFFFF6B9AL)
                            repository.setHotspotColor(0xFFF59E0BL)
                            repository.setCallColor(0xFF22C55EL)
                            repository.setLiveActivityColor(0xFF8B5CF6L)
                            repository.setTransferColor(0xFF06B6D4L)
                            repository.setNavigationColor(0xFF10B981L)
                            repository.setBluetoothColor(0xFF38BDF8L)
                            repository.setFlashlightColor(0xFFF59E0BL)
                            repository.setScreenRecordingColor(0xFFEF4444L)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset All Colors to Studio Defaults", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FeatureColorRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    onCustomClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(selectedColor).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(selectedColor),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Swatches row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PRESET_COLORS.forEach { (colorValue, _) ->
                val isSelected = selectedColor == colorValue
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(colorValue))
                        .clickable { onColorSelected(colorValue) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Custom RGB button
            val isCustom = PRESET_COLORS.none { it.first == selectedColor }
            val rainbowBrush = remember {
                Brush.linearGradient(
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .then(
                        if (isCustom) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(rainbowBrush)
                    .clickable(onClick = onCustomClicked),
                contentAlignment = Alignment.Center
            ) {
                if (isCustom) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Custom",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RgbColorPickerDialog(
    title: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val initialColorObj = Color(initialColor)
    var red by remember { mutableStateOf((initialColorObj.red * 255f).toInt()) }
    var green by remember { mutableStateOf((initialColorObj.green * 255f).toInt()) }
    var blue by remember { mutableStateOf((initialColorObj.blue * 255f).toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title.ifEmpty { "Select Custom Color" },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val previewColor = Color(red, green, blue)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val hexCode = String.format("#%02X%02X%02X", red, green, blue)
                    Text(
                        text = hexCode,
                        color = if (red * 0.299 + green * 0.587 + blue * 0.114 > 186) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Red Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Red", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("$red", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = red.toFloat(),
                        onValueChange = { red = it.toInt() },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Green Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Green", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("$green", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = green.toFloat(),
                        onValueChange = { green = it.toInt() },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Blue Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Blue", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("$blue", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = blue.toFloat(),
                        onValueChange = { blue = it.toInt() },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalColor = (0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong())
                    onSave(finalColor)
                }
            ) {
                Text("Save Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
