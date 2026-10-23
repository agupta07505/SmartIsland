/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.ExpandedBlurMode
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.bounceClick
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private val PRESET_COLORS = listOf(
    0xFF10B981L to "Emerald",
    0xFF38BDF8L to "Sky",
    0xFF6366F1L to "Indigo",
    0xFFFF6B9AL to "Pink",
    0xFFEF4444L to "Coral",
    0xFFF59E0BL to "Amber",
    0xFF8B5CF6L to "Purple"
)

internal val GRADIENT_PRESET_COLORS = listOf(
    0xFF2563EBL to "Blue",
    0xFF7C3AEDL to "Purple",
    0xFFDB2777L to "Pink",
    0xFFEA580CL to "Orange",
    0xFF10B981L to "Emerald",
    0xFF38BDF8L to "Sky"
)

internal val PILL_PRESET_COLORS = listOf(
    0xFF000000L to "Black",
    0xFF1C1C1EL to "Slate",
    0xFF0A192FL to "Navy",
    0xFF1E1B4BL to "Violet",
    0xFF18181BL to "Zinc",
    0xFF10B981L to "Emerald",
    0xFF38BDF8L to "Sky"
)

private data class FeatureColorConfig(
    val id: String,
    val titleRes: Int,
    val icon: ImageVector,
    val color: Long,
    val onColorChange: suspend (Long) -> Unit
)

@Composable
fun CustomizationsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var localOpacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
    var localMediaOpacity by remember(settings.mediaOpacity) { mutableFloatStateOf(settings.mediaOpacity) }
    var localMediaBlurRadius by remember(settings.mediaBlurRadius) { mutableFloatStateOf(settings.mediaBlurRadius) }
    var localBlurRadius by remember(settings.expandedBlurRadius) { mutableFloatStateOf(settings.expandedBlurRadius) }
    var localGradientStrength by remember(settings.expandedGradientStrength) { mutableFloatStateOf(settings.expandedGradientStrength) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var currentColorTarget by rememberSaveable { mutableStateOf("") }
    var colorPickerTitle by rememberSaveable { mutableStateOf("") }
    var initialColor by rememberSaveable { mutableLongStateOf(0xFF10B981L) }

    val featureConfigs = remember(settings) {
        listOf(
            FeatureColorConfig("music", R.string.color_music_visualizer, Icons.Rounded.MusicNote, settings.musicVisualizerColor) { repository.setMusicVisualizerColor(it) },
            FeatureColorConfig("call", R.string.color_phone_calls, Icons.Rounded.Call, settings.callColor) { repository.setCallColor(it) },
            FeatureColorConfig("battery", R.string.color_battery_charging, Icons.Rounded.BatteryChargingFull, settings.batteryColor) { repository.setBatteryColor(it) },
            FeatureColorConfig("notification", R.string.color_notification_dot, Icons.Rounded.Notifications, settings.notificationDotColor) { repository.setNotificationDotColor(it) },
            FeatureColorConfig("hotspot", R.string.color_hotspot_tethering, Icons.Rounded.WifiTethering, settings.hotspotColor) { repository.setHotspotColor(it) },
            FeatureColorConfig("navigation", R.string.color_maps_navigation, Icons.Rounded.Navigation, settings.navigationColor) { repository.setNavigationColor(it) },
            FeatureColorConfig("live_activity", R.string.color_live_activities, Icons.Rounded.Explore, settings.liveActivityColor) { repository.setLiveActivityColor(it) },
            FeatureColorConfig("transfer", R.string.color_file_downloads, Icons.Rounded.FileDownload, settings.transferColor) { repository.setTransferColor(it) },
            FeatureColorConfig("bluetooth", R.string.color_bluetooth_device, Icons.Rounded.BluetoothConnected, settings.bluetoothColor) { repository.setBluetoothColor(it) },
            FeatureColorConfig("flashlight", R.string.color_flashlight_torch, Icons.Rounded.FlashlightOn, settings.flashlightColor) { repository.setFlashlightColor(it) },
            FeatureColorConfig("screen_recording", R.string.color_screen_recording, Icons.Rounded.Videocam, settings.screenRecordingColor) { repository.setScreenRecordingColor(it) },
            FeatureColorConfig("timer", R.string.color_timer_countdown, Icons.Rounded.HourglassBottom, settings.timerColor) { repository.setTimerColor(it) },
            FeatureColorConfig("stopwatch", R.string.color_stopwatch_laps, Icons.Rounded.AvTimer, settings.stopwatchColor) { repository.setStopwatchColor(it) }
        )
    }

    if (showDialog) {
        RgbColorPickerDialog(
            title = colorPickerTitle,
            initialColor = initialColor,
            onDismiss = { showDialog = false },
            onSave = { color ->
                showDialog = false
                scope.launch {
                    if (currentColorTarget == "pill") {
                        repository.setPillColor(color)
                    } else if (currentColorTarget == "gradient") {
                        repository.setExpandedGradientColor(color)
                    } else {
                        featureConfigs.find { it.id == currentColorTarget }?.onColorChange?.invoke(color)
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.opacity_card_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                SliderSettingItem(
                    label = stringResource(R.string.slider_precision_opacity),
                    value = (localOpacity * 100f),
                    range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                    suffix = "%",
                    step = 5f,
                    onValueChange = { localOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY) },
                    onValueChangeFinished = { scope.launch { repository.setOpacity(localOpacity) } }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        1.0f to stringResource(R.string.opacity_solid),
                        0.85f to stringResource(R.string.opacity_dark),
                        0.70f to stringResource(R.string.opacity_glass),
                        0.50f to stringResource(R.string.opacity_clear)
                    ).forEach { (targetVal, label) ->
                        val isSelected = abs(localOpacity - targetVal) < 0.04f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .bounceClick {
                                    localOpacity = targetVal
                                    scope.launch { repository.setOpacity(targetVal) }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.color_pill_background),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.color_pill_background_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ColorPresetRow(
                        selectedColor = settings.pillColor,
                        swatches = PILL_PRESET_COLORS,
                        onColorSelected = { scope.launch { repository.setPillColor(it) } },
                        onCustomClicked = {
                            currentColorTarget = "pill"
                            colorPickerTitle = context.getString(R.string.color_pill_background)
                            initialColor = settings.pillColor
                            showDialog = true
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.expanded_style_card_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.expanded_theme_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.expanded_theme_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExpandedThemeSelector(
                        selected = settings.expandedTheme,
                        onSelected = { scope.launch { repository.setExpandedTheme(it) } }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))

                // Background blur: Off or Card blur. The strength slider applies to Card blur; the media card keeps its own blur below.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.toggle_expanded_blur_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.toggle_expanded_blur_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExpandedBlurModeSelector(
                        selected = settings.expandedBlurMode,
                        onSelected = { mode -> scope.launch { repository.setExpandedBlurMode(mode) } }
                    )
                }
                AnimatedVisibility(visible = settings.expandedBlurMode != ExpandedBlurMode.Off) {
                    SliderSettingItem(
                        label = stringResource(R.string.slider_expanded_blur_radius),
                        value = localBlurRadius,
                        range = SmartIslandSettings.MIN_EXPANDED_BLUR..SmartIslandSettings.MAX_EXPANDED_BLUR,
                        suffix = " dp",
                        step = 2f,
                        onValueChange = { localBlurRadius = it },
                        onValueChangeFinished = { scope.launch { repository.setExpandedBlurRadius(localBlurRadius) } }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                // The media (music) card has its own blur and opacity.
                Text(
                    text = stringResource(R.string.media_look_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.media_look_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SliderSettingItem(
                    label = stringResource(R.string.slider_media_opacity),
                    value = localMediaOpacity * 100f,
                    range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                    suffix = "%",
                    step = 5f,
                    onValueChange = { localMediaOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY) },
                    onValueChangeFinished = { scope.launch { repository.setMediaOpacity(localMediaOpacity) } }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.toggle_media_blur_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.mediaBlurEnabled,
                        onCheckedChange = { checked -> scope.launch { repository.setMediaBlurEnabled(checked) } }
                    )
                }
                AnimatedVisibility(visible = settings.mediaBlurEnabled) {
                    SliderSettingItem(
                        label = stringResource(R.string.slider_media_blur_radius),
                        value = localMediaBlurRadius,
                        range = SmartIslandSettings.MIN_EXPANDED_BLUR..SmartIslandSettings.MAX_EXPANDED_BLUR,
                        suffix = " dp",
                        step = 2f,
                        onValueChange = { localMediaBlurRadius = it },
                        onValueChangeFinished = { scope.launch { repository.setMediaBlurRadius(localMediaBlurRadius) } }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.toggle_expanded_gradient_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.toggle_expanded_gradient_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.expandedGradientEnabled,
                        onCheckedChange = { checked -> scope.launch { repository.setExpandedGradientEnabled(checked) } }
                    )
                }
                AnimatedVisibility(visible = settings.expandedGradientEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.color_expanded_gradient),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            ColorPresetRow(
                                selectedColor = settings.expandedGradientColor,
                                swatches = GRADIENT_PRESET_COLORS,
                                onColorSelected = { scope.launch { repository.setExpandedGradientColor(it) } },
                                onCustomClicked = {
                                    currentColorTarget = "gradient"
                                    colorPickerTitle = context.getString(R.string.color_expanded_gradient)
                                    initialColor = settings.expandedGradientColor
                                    showDialog = true
                                }
                            )
                        }
                        SliderSettingItem(
                            label = stringResource(R.string.slider_expanded_gradient_strength),
                            value = localGradientStrength * 100f,
                            range = (SmartIslandSettings.MIN_GRADIENT_STRENGTH * 100f)..(SmartIslandSettings.MAX_GRADIENT_STRENGTH * 100f),
                            suffix = "%",
                            step = 5f,
                            onValueChange = { localGradientStrength = (it / 100f) },
                            onValueChangeFinished = { scope.launch { repository.setExpandedGradientStrength(localGradientStrength) } }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.card_color_studio_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                featureConfigs.forEachIndexed { index, config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(config.color).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = config.icon,
                                    contentDescription = null,
                                    tint = Color(config.color),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = stringResource(config.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        ColorPresetRow(
                            selectedColor = config.color,
                            swatches = PRESET_COLORS,
                            onColorSelected = { newColor -> scope.launch { config.onColorChange(newColor) } },
                            onCustomClicked = {
                                currentColorTarget = config.id
                                colorPickerTitle = context.getString(config.titleRes)
                                initialColor = config.color
                                showDialog = true
                            }
                        )
                    }

                    if (index < featureConfigs.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColorPresetRow(
    selectedColor: Long,
    swatches: List<Pair<Long, String>>,
    onColorSelected: (Long) -> Unit,
    onCustomClicked: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.take(4).forEach { (colorValue, _) ->
            val isSelected = selectedColor == colorValue
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(colorValue))
                    .bounceClick { onColorSelected(colorValue) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        val isCustom = swatches.take(4).none { it.first == selectedColor }
        val rainbowBrush = remember {
            Brush.linearGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (isCustom) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                )
                .padding(2.dp)
                .clip(CircleShape)
                .then(
                    if (isCustom) Modifier.background(Color(selectedColor))
                    else Modifier.background(rainbowBrush)
                )
                .bounceClick(onCustomClicked),
            contentAlignment = Alignment.Center
        ) {
            if (isCustom) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Custom",
                    tint = if (Color(selectedColor).red * 0.299 + Color(selectedColor).green * 0.587 + Color(selectedColor).blue * 0.114 > 0.5) Color.Black else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
internal fun RgbColorPickerDialog(
    title: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val initialColorObj = Color(initialColor)
    // Saved, so a half-edited colour survives rotating the phone while the dialog is open.
    var red by rememberSaveable { mutableIntStateOf((initialColorObj.red * 255f).toInt().coerceIn(0, 255)) }
    var green by rememberSaveable { mutableIntStateOf((initialColorObj.green * 255f).toInt().coerceIn(0, 255)) }
    var blue by rememberSaveable { mutableIntStateOf((initialColorObj.blue * 255f).toInt().coerceIn(0, 255)) }
    var hexInput by rememberSaveable { mutableStateOf(String.format("%02X%02X%02X", red, green, blue)) }
    var isHexError by rememberSaveable { mutableStateOf(false) }

    fun updateFromRgb(newR: Int, newG: Int, newB: Int) {
        red = newR.coerceIn(0, 255)
        green = newG.coerceIn(0, 255)
        blue = newB.coerceIn(0, 255)
        hexInput = String.format("%02X%02X%02X", red, green, blue)
        isHexError = false
    }

    fun updateFromHex(input: String) {
        val clean = input.removePrefix("#").trim().uppercase()
        hexInput = clean
        if (clean.length == 6 && clean.all { it in "0123456789ABCDEF" }) {
            isHexError = false
            val parsedR = clean.substring(0, 2).toInt(16)
            val parsedG = clean.substring(2, 4).toInt(16)
            val parsedB = clean.substring(4, 6).toInt(16)
            red = parsedR
            green = parsedG
            blue = parsedB
        } else {
            isHexError = clean.isNotEmpty()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val previewColor = Color(0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$hexInput",
                        fontWeight = FontWeight.Bold,
                        color = if (previewColor.red * 0.299 + previewColor.green * 0.587 + previewColor.blue * 0.114 > 0.5) Color.Black else Color.White
                    )
                }

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { updateFromHex(it) },
                    label = { Text(stringResource(R.string.color_hex_input)) },
                    placeholder = { Text(stringResource(R.string.color_hex_hint)) },
                    prefix = { Text("#", fontWeight = FontWeight.Bold) },
                    isError = isHexError,
                    supportingText = if (isHexError) {
                        { Text(stringResource(R.string.color_hex_error), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0xFF000000L,
                        0xFF1C1C1EL,
                        0xFF0A192FL,
                        0xFF1E1B4BL,
                        0xFF10B981L,
                        0xFF38BDF8L,
                        0xFFEF4444L
                    ).forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(colorVal))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(6.dp)
                                )
                                .bounceClick {
                                    val c = Color(colorVal)
                                    updateFromRgb(
                                        (c.red * 255f).toInt(),
                                        (c.green * 255f).toInt(),
                                        (c.blue * 255f).toInt()
                                    )
                                }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.color_red), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$red", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = red.toFloat(),
                        onValueChange = { updateFromRgb(it.toInt(), green, blue) },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.color_green), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$green", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = green.toFloat(),
                        onValueChange = { updateFromRgb(red, it.toInt(), blue) },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.color_blue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$blue", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = blue.toFloat(),
                        onValueChange = { updateFromRgb(red, green, it.toInt()) },
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
                Text(stringResource(R.string.btn_save_color), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun ExpandedThemeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf(
        SmartIslandSettings.EXPANDED_THEME_DARK to stringResource(R.string.expanded_theme_dark),
        SmartIslandSettings.EXPANDED_THEME_LIGHT to stringResource(R.string.expanded_theme_light),
        SmartIslandSettings.EXPANDED_THEME_SYSTEM to stringResource(R.string.expanded_theme_system)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = selected == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandedBlurModeSelector(selected: ExpandedBlurMode, onSelected: (ExpandedBlurMode) -> Unit) {
    val options = listOf(
        ExpandedBlurMode.Off to stringResource(R.string.blur_mode_off),
        ExpandedBlurMode.Card to stringResource(R.string.blur_mode_card)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
