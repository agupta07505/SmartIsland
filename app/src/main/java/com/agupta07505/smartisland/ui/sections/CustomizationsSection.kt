/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.glass.Switch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val PRESET_COLORS = listOf(
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

/** The colour picker's target for one event: this and the event's id. */
private const val EVENT_COLOR_TARGET_PREFIX = "event:"

internal data class FeatureColorConfig(
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
    val localOpacity = remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
    val localMediaOpacity = remember(settings.mediaOpacity) { mutableFloatStateOf(settings.mediaOpacity) }
    val localMediaBlurRadius = remember(settings.mediaBlurRadius) { mutableFloatStateOf(settings.mediaBlurRadius) }
    val localBlurRadius = remember(settings.expandedBlurRadius) { mutableFloatStateOf(settings.expandedBlurRadius) }
    val localGradientStrength =
        remember(settings.expandedGradientStrength) { mutableFloatStateOf(settings.expandedGradientStrength) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var currentColorTarget by rememberSaveable { mutableStateOf("") }
    var colorPickerTitle by rememberSaveable { mutableStateOf("") }
    var initialColor by rememberSaveable { mutableLongStateOf(0xFF10B981L) }

    val featureConfigs = remember(settings) {
        listOf(
            FeatureColorConfig("music", R.string.color_music_visualizer, Icons.Rounded.MusicNote,
                settings.musicVisualizerColor) { repository.setMusicVisualizerColor(it) },
            FeatureColorConfig("call", R.string.color_phone_calls, Icons.Rounded.Call,
                settings.callColor) { repository.setCallColor(it) },
            FeatureColorConfig("battery", R.string.color_battery_charging, Icons.Rounded.BatteryChargingFull,
                settings.batteryColor) { repository.setBatteryColor(it) },
            FeatureColorConfig("notification", R.string.color_notification_dot, Icons.Rounded.Notifications,
                settings.notificationDotColor) { repository.setNotificationDotColor(it) },
            FeatureColorConfig("hotspot", R.string.color_hotspot_tethering, Icons.Rounded.WifiTethering,
                settings.hotspotColor) { repository.setHotspotColor(it) },
            FeatureColorConfig("navigation", R.string.color_maps_navigation, Icons.Rounded.Navigation,
                settings.navigationColor) { repository.setNavigationColor(it) },
            FeatureColorConfig("live_activity", R.string.color_live_activities, Icons.Rounded.Explore,
                settings.liveActivityColor) { repository.setLiveActivityColor(it) },
            FeatureColorConfig("transfer", R.string.color_file_downloads, Icons.Rounded.FileDownload,
                settings.transferColor) { repository.setTransferColor(it) },
            FeatureColorConfig("bluetooth", R.string.color_bluetooth_device, Icons.Rounded.BluetoothConnected,
                settings.bluetoothColor) { repository.setBluetoothColor(it) },
            FeatureColorConfig("flashlight", R.string.color_flashlight_torch, Icons.Rounded.FlashlightOn,
                settings.flashlightColor) { repository.setFlashlightColor(it) },
            FeatureColorConfig("screen_recording", R.string.color_screen_recording, Icons.Rounded.Videocam,
                settings.screenRecordingColor) { repository.setScreenRecordingColor(it) },
            FeatureColorConfig("timer", R.string.color_timer_countdown, Icons.Rounded.HourglassBottom,
                settings.timerColor) { repository.setTimerColor(it) },
            FeatureColorConfig("stopwatch", R.string.color_stopwatch_laps, Icons.Rounded.AvTimer,
                settings.stopwatchColor) { repository.setStopwatchColor(it) }
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
                    } else if (currentColorTarget.startsWith(EVENT_COLOR_TARGET_PREFIX)) {
                        currentColorTarget.removePrefix(EVENT_COLOR_TARGET_PREFIX).split(",")
                            .forEach { repository.setEventColor(it, color) }
                    } else {
                        featureConfigs.find { it.id == currentColorTarget }?.onColorChange?.invoke(color)
                    }
                }
            }
        )
    }

    val openColorPicker: (String, Int, Long) -> Unit = { target, titleRes, color ->
        currentColorTarget = target
        colorPickerTitle = context.getString(titleRes)
        initialColor = color
        showDialog = true
    }

    val openEventColorPicker: (EventColorRow, Long) -> Unit = { row, color ->
        currentColorTarget = EVENT_COLOR_TARGET_PREFIX + row.events.joinToString(",") { it.id }
        colorPickerTitle = row.label
        initialColor = color
        showDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PillLookCard(settings, repository, scope, localOpacity, openColorPicker)
        ExpandedStyleCard(
            settings = settings,
            repository = repository,
            scope = scope,
            blurRadius = localBlurRadius,
            mediaOpacity = localMediaOpacity,
            mediaBlurRadius = localMediaBlurRadius,
            gradientStrength = localGradientStrength,
            onPickColor = openColorPicker
        )
        ColorStudioCard(
            featureConfigs = featureConfigs,
            scope = scope,
            onPickColor = openColorPicker,
            settings = settings,
            onSetEventColor = { row, color -> scope.launch { row.events.forEach { repository.setEventColor(it.id, color) } } },
            onPickEventCustom = openEventColorPicker,
            onResetEvent = { row -> scope.launch { row.events.forEach { repository.clearEventColor(it.id) } } },
            onResetAllEvents = { scope.launch { repository.clearEventColors() } }
        )
    }
}

@Composable
private fun PillLookCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    opacityState: MutableFloatState,
    onPickColor: (String, Int, Long) -> Unit
) {
    var localOpacity by opacityState
    SettingsCard {
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
                onValueChange = { localOpacity = (it / 100f)
                    .coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY) },
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

            SettingsDivider(0.2f, modifier = Modifier.padding(vertical = 4.dp))

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
                    onCustomClicked = { onPickColor("pill", R.string.color_pill_background, settings.pillColor) }
                )
            }

            SettingsDivider(0.2f, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.toggle_oled_black_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.toggle_oled_black_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.oledBlack,
                    onCheckedChange = { checked -> scope.launch { repository.setOledBlack(checked) } }
                )
            }
        }
    }
}
