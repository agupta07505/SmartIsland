/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.WidgetIslandTimer
import com.agupta07505.smartisland.ui.search.searchAnchor
import com.agupta07505.smartisland.util.IdleWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Layout tab: live readouts (time, weather, ...) that the collapsed cutout shows while no event is on the island. One
 * setting for both orientations; the landscape idle circle shows just the value.
 */
@Composable
fun IdleWidgetsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    /** Opens Settings > Idle widgets, where the switches, the widget selection and the timers are. */
    onOpenSettings: () -> Unit = {}
) {
    // The switches are per orientation (Settings > Idle widgets); the display options below show while either one is on.
    val anyEnabled = settings.idleWidgetsEnabled || settings.idleWidgetsEnabledLandscape
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selected = remember(settings.idleWidgetIds) { IdleWidgets.parse(settings.idleWidgetIds) }
    val weatherOn = anyEnabled && (IdleWidgetKind.Weather in selected || IdleWidgetKind.Wind in selected)
    var locationGranted by remember { mutableStateOf(IdleWeather.hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationGranted = granted
    }
    // The app is in front here, so this is the moment the last known place can be read and kept for the weather widget.
    LaunchedEffect(weatherOn, locationGranted) {
        if (weatherOn && locationGranted) withContext(Dispatchers.IO) { IdleWeather.captureLocation(context) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .searchAnchor("layout.idle_widgets"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.idle_widgets_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(R.string.idle_widgets_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.idle_widgets_open_settings))
            }

            if (anyEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                ChoiceBlock(title = stringResource(R.string.idle_widgets_island_close_title)) {
                    ISLAND_CLOSE_CHOICES.forEach { seconds ->
                        ChoiceChip(
                            label = if (seconds == WidgetIslandTimer.NEVER) {
                                stringResource(R.string.idle_widgets_island_never)
                            } else {
                                stringResource(R.string.idle_widgets_seconds, seconds)
                            },
                            selected = settings.widgetIslandCloseSeconds == seconds,
                            modifier = Modifier.weight(1f),
                            onClick = { scope.launch { repository.setWidgetIslandCloseSeconds(seconds) } }
                        )
                    }
                }
                NoteText(stringResource(R.string.idle_widgets_island_close_desc))

                ChoiceBlock(title = stringResource(R.string.idle_widgets_glyph_scale_title)) {
                    IdleSlots.GLYPH_SCALES.forEach { percent ->
                        ChoiceChip(
                            label = "$percent%",
                            selected = settings.idleGlyphScale == percent,
                            modifier = Modifier.weight(1f),
                            onClick = { scope.launch { repository.setIdleGlyphScale(percent) } }
                        )
                    }
                }
                IslandFontPicker(
                    selected = settings.idleGlyphFont,
                    onSelected = { scope.launch { repository.setIdleGlyphFont(it) } },
                    title = stringResource(R.string.idle_widgets_glyph_font_title),
                    description = stringResource(R.string.idle_widgets_glyph_font_desc),
                    inheritRow = stringResource(R.string.idle_widgets_glyph_font_same) to stringResource(R.string.idle_widgets_glyph_font_same_note)
                )

                if (IdleWidgetKind.Battery in selected) {
                    ChoiceBlock(title = stringResource(R.string.idle_widgets_battery_metric_title)) {
                        listOf(
                            WidgetIslandContent.BATTERY_WATT to stringResource(R.string.idle_widgets_battery_metric_watt),
                            WidgetIslandContent.BATTERY_VOLT to stringResource(R.string.idle_widgets_battery_metric_volt)
                        ).forEach { (id, label) ->
                            ChoiceChip(
                                label = label,
                                selected = settings.widgetIslandBatteryMetric == id,
                                modifier = Modifier.weight(1f),
                                onClick = { scope.launch { repository.setWidgetIslandBatteryMetric(id) } }
                            )
                        }
                    }
                    NoteText(stringResource(R.string.idle_widgets_battery_metric_desc))
                }

                if (IdleWidgetKind.Weather in selected || IdleWidgetKind.Wind in selected) {
                    ChoiceBlock(title = stringResource(R.string.idle_widgets_unit_title)) {
                        listOf(
                            IdleWidgets.UNIT_AUTO to stringResource(R.string.idle_widgets_unit_auto),
                            IdleWidgets.UNIT_CELSIUS to stringResource(R.string.idle_widgets_unit_c),
                            IdleWidgets.UNIT_FAHRENHEIT to stringResource(R.string.idle_widgets_unit_f)
                        ).forEach { (id, label) ->
                            ChoiceChip(
                                label = label,
                                selected = settings.idleWidgetTempUnit == id,
                                modifier = Modifier.weight(1f),
                                onClick = { scope.launch { repository.setIdleWidgetTempUnit(id) } }
                            )
                        }
                    }
                    when {
                        !settings.allowNetworkChecks -> NoteText(stringResource(R.string.idle_widgets_weather_needs_network))
                        settings.weatherLocationSource == com.agupta07505.smartisland.model.WeatherConfig.SOURCE_MANUAL ->
                            NoteText(stringResource(R.string.idle_widgets_weather_manual_note))
                        settings.weatherLocationSource == com.agupta07505.smartisland.model.WeatherConfig.SOURCE_LAST_KNOWN ->
                            NoteText(stringResource(R.string.idle_widgets_weather_last_known_note))
                        locationGranted -> NoteText(stringResource(R.string.idle_widgets_weather_location_ok))
                        else -> {
                            NoteText(stringResource(R.string.idle_widgets_weather_no_location))
                            Button(
                                onClick = { permissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }
                            ) {
                                Text(stringResource(R.string.idle_widgets_allow_location))
                            }
                        }
                    }
                }

                NoteText(stringResource(R.string.idle_widgets_landscape_note))
            }
        }
    }
}

private val ISLAND_CLOSE_CHOICES = listOf(2, 3, 5, 10, WidgetIslandTimer.NEVER)

internal fun kindIcon(kind: IdleWidgetKind): ImageVector = when (kind) {
    IdleWidgetKind.Clock -> Icons.Rounded.Schedule
    IdleWidgetKind.LiveClock -> Icons.Rounded.AccessTime
    IdleWidgetKind.Date -> Icons.Rounded.CalendarToday
    IdleWidgetKind.Weather -> Icons.Rounded.WbSunny
    IdleWidgetKind.Wind -> Icons.Rounded.Air
    IdleWidgetKind.Battery -> Icons.Rounded.BatteryChargingFull
    IdleWidgetKind.NextAlarm -> Icons.Rounded.Alarm
    IdleWidgetKind.NetSpeed -> Icons.Rounded.Speed
    IdleWidgetKind.DayMoments -> Icons.Rounded.WbTwilight
    IdleWidgetKind.Notifications -> Icons.Rounded.Notifications
    IdleWidgetKind.Modes -> Icons.Rounded.DoNotDisturbOn
}

internal fun kindTint(kind: IdleWidgetKind): Color = when (kind) {
    IdleWidgetKind.Clock -> Color(0xFF6366F1)
    IdleWidgetKind.LiveClock -> Color(0xFF14B8A6)
    IdleWidgetKind.Date -> Color(0xFFEF4444)
    IdleWidgetKind.Weather -> Color(0xFFF59E0B)
    IdleWidgetKind.Wind -> Color(0xFF5EEAD4)
    IdleWidgetKind.Battery -> Color(0xFF10B981)
    IdleWidgetKind.NextAlarm -> Color(0xFFFF9F0A)
    IdleWidgetKind.NetSpeed -> Color(0xFF38BDF8)
    IdleWidgetKind.DayMoments -> Color(0xFFFB923C)
    IdleWidgetKind.Notifications -> Color(0xFF3B82F6)
    IdleWidgetKind.Modes -> Color(0xFF8B5CF6)
}

internal fun kindTitle(kind: IdleWidgetKind): Int = when (kind) {
    IdleWidgetKind.Clock -> R.string.idle_widget_clock_title
    IdleWidgetKind.LiveClock -> R.string.idle_widget_liveclock_title
    IdleWidgetKind.Date -> R.string.idle_widget_date_title
    IdleWidgetKind.Weather -> R.string.idle_widget_weather_title
    IdleWidgetKind.Wind -> R.string.idle_widget_wind_title
    IdleWidgetKind.Battery -> R.string.idle_widget_battery_title
    IdleWidgetKind.NextAlarm -> R.string.idle_widget_alarm_title
    IdleWidgetKind.NetSpeed -> R.string.idle_widget_speed_title
    IdleWidgetKind.DayMoments -> R.string.idle_widget_daymoments_title
    IdleWidgetKind.Notifications -> R.string.idle_widget_notifications_title
    IdleWidgetKind.Modes -> R.string.idle_widget_modes_title
}

internal fun kindDesc(kind: IdleWidgetKind): Int = when (kind) {
    IdleWidgetKind.Clock -> R.string.idle_widget_clock_desc
    IdleWidgetKind.LiveClock -> R.string.idle_widget_liveclock_desc
    IdleWidgetKind.Date -> R.string.idle_widget_date_desc
    IdleWidgetKind.Weather -> R.string.idle_widget_weather_desc
    IdleWidgetKind.Wind -> R.string.idle_widget_wind_desc
    IdleWidgetKind.Battery -> R.string.idle_widget_battery_desc
    IdleWidgetKind.NextAlarm -> R.string.idle_widget_alarm_desc
    IdleWidgetKind.NetSpeed -> R.string.idle_widget_speed_desc
    IdleWidgetKind.DayMoments -> R.string.idle_widget_daymoments_desc
    IdleWidgetKind.Notifications -> R.string.idle_widget_notifications_desc
    IdleWidgetKind.Modes -> R.string.idle_widget_modes_desc
}

@Composable
private fun ChoiceBlock(title: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            content = content
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoteText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
