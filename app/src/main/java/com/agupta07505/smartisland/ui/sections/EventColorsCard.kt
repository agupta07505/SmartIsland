/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.EventColors
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.FACE_ID_NAVY
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.components.BatteryVisual
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.expanded.AIRPLANE_ACCENT
import com.agupta07505.smartisland.ui.expanded.CONNECTION_LOST_RED
import com.agupta07505.smartisland.ui.expanded.DND_ACCENT
import com.agupta07505.smartisland.ui.expanded.HEADPHONES_ACCENT
import com.agupta07505.smartisland.ui.expanded.MODE_ACCENT
import com.agupta07505.smartisland.ui.expanded.RING_ACCENT
import com.agupta07505.smartisland.ui.expanded.SILENT_ACCENT
import com.agupta07505.smartisland.ui.expanded.USB_ACCENT
import com.agupta07505.smartisland.ui.expanded.VIBRATE_ACCENT

/**
 * The colour [event] has without a colour of its own: the one the island has always drawn it with, so the row shows
 * what a reset gives back. It follows the group colours above ([SmartIslandSettings.batteryColor] and the others), and
 * a state's fixed colour (a low battery's red, USB's magenta) is kept as it was
 */
internal fun eventDefaultColor(event: IslandEvent, settings: SmartIslandSettings): Long {
    EventColors.defaultFor(event)?.let { return it }
    val color: Color = when (event) {
        IslandEvent.PLUGGED_IN -> batteryVisualColor(BatteryVisual.PluggedIn, Color(settings.batteryColor))
        IslandEvent.FULLY_CHARGED -> batteryVisualColor(BatteryVisual.Full, Color(settings.batteryColor))
        IslandEvent.PLUGGED_OUT -> batteryVisualColor(BatteryVisual.PluggedOut, Color(settings.batteryColor))
        IslandEvent.LOW_BATTERY -> batteryVisualColor(BatteryVisual.Low, Color(settings.batteryColor))
        IslandEvent.BATTERY_SAVER -> batteryVisualColor(BatteryVisual.Saver, Color(settings.batteryColor))
        IslandEvent.WIFI, IslandEvent.VPN, IslandEvent.BLUETOOTH -> Color(settings.bluetoothColor)
        IslandEvent.WIFI_DISCONNECTED, IslandEvent.VPN_DISCONNECTED -> CONNECTION_LOST_RED
        IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED -> USB_ACCENT
        IslandEvent.HEADPHONES_CONNECTED, IslandEvent.HEADPHONES_DISCONNECTED -> HEADPHONES_ACCENT
        IslandEvent.HOTSPOT -> Color(settings.hotspotColor)
        IslandEvent.AIRPLANE_MODE -> AIRPLANE_ACCENT
        IslandEvent.RING -> RING_ACCENT
        IslandEvent.VIBRATE -> VIBRATE_ACCENT
        IslandEvent.SILENT -> SILENT_ACCENT
        IslandEvent.DND -> DND_ACCENT
        IslandEvent.MODE -> MODE_ACCENT
        IslandEvent.CALL, IslandEvent.CALL_ENDED -> Color(settings.callColor)
        IslandEvent.MUSIC -> Color(settings.musicVisualizerColor)
        IslandEvent.NOTIFICATION -> Color(settings.notificationDotColor)
        IslandEvent.NAVIGATION -> Color(settings.navigationColor)
        IslandEvent.LIVE_ACTIVITY -> Color(settings.liveActivityColor)
        IslandEvent.DOWNLOAD, IslandEvent.UPLOAD, IslandEvent.BACKUP, IslandEvent.FILE_COPY_MOVE,
        IslandEvent.FILE_DELETE, IslandEvent.APP_INSTALL -> Color(settings.transferColor)
        IslandEvent.FLASHLIGHT -> Color(settings.flashlightColor)
        IslandEvent.SCREEN_RECORDING, IslandEvent.AUDIO_RECORDING -> Color(settings.screenRecordingColor)
        IslandEvent.TIMER, IslandEvent.TIMER_FINISHED -> Color(settings.timerColor)
        IslandEvent.STOPWATCH -> Color(settings.stopwatchColor)
        IslandEvent.FACE_UNLOCK -> FACE_ID_NAVY
    }
    return color.toArgb().toLong() and 0xFFFFFFFFL
}

/** What a row shows: the colour the user gave the event, else its usual one. */
internal fun eventShownColor(event: IslandEvent, settings: SmartIslandSettings): Long =
    settings.eventColors[event.id] ?: eventDefaultColor(event, settings)

internal class EventColorRow(val label: String, val icon: ImageVector, val events: List<IslandEvent>)

/**
 * The event rows that sit indented under the group row of the feature with that id ([FeatureColorConfig.id]): the group
 * row sets the colour its events follow, an event row below it is an optional exception for that one event
 */
private val EVENT_ROWS_BY_GROUP: Map<String, List<EventColorRow>> = mapOf(
    "battery" to listOf(
        EventColorRow("Charging", Icons.Rounded.BatteryChargingFull, listOf(IslandEvent.PLUGGED_IN, IslandEvent.FULLY_CHARGED)),
        EventColorRow("Unplugged", Icons.Rounded.PowerOff, listOf(IslandEvent.PLUGGED_OUT)),
        EventColorRow("Low battery", Icons.Rounded.BatteryAlert, listOf(IslandEvent.LOW_BATTERY)),
        EventColorRow("Battery saver", Icons.Rounded.BatterySaver, listOf(IslandEvent.BATTERY_SAVER))
    ),
    "bluetooth" to listOf(
        EventColorRow("Wi-Fi & VPN", Icons.Rounded.Wifi, listOf(IslandEvent.WIFI, IslandEvent.VPN)),
        EventColorRow("Wi-Fi & VPN lost", Icons.Rounded.WifiOff, listOf(IslandEvent.WIFI_DISCONNECTED, IslandEvent.VPN_DISCONNECTED)),
        EventColorRow("USB in", Icons.Rounded.Usb, listOf(IslandEvent.USB_CONNECTED)),
        EventColorRow("USB out", Icons.Rounded.Usb, listOf(IslandEvent.USB_DISCONNECTED)),
        EventColorRow("Headphone in", Icons.Rounded.Headphones, listOf(IslandEvent.HEADPHONES_CONNECTED)),
        EventColorRow("Headphone out", Icons.Rounded.Headphones, listOf(IslandEvent.HEADPHONES_DISCONNECTED)),
        EventColorRow("Airplane mode", Icons.Rounded.AirplanemodeActive, listOf(IslandEvent.AIRPLANE_MODE)),
        EventColorRow("Ring", Icons.Rounded.NotificationsActive, listOf(IslandEvent.RING)),
        EventColorRow("Vibrate", Icons.Rounded.Vibration, listOf(IslandEvent.VIBRATE)),
        EventColorRow("Silent", Icons.Rounded.VolumeOff, listOf(IslandEvent.SILENT)),
        EventColorRow("Do Not Disturb", Icons.Rounded.DoNotDisturbOn, listOf(IslandEvent.DND)),
        EventColorRow("Modes", Icons.Rounded.Bedtime, listOf(IslandEvent.MODE))
    )
)

/** The event rows under the group row of the feature with id [groupId], none for a feature without any. */
internal fun eventRowsOf(groupId: String): List<EventColorRow> = EVENT_ROWS_BY_GROUP[groupId].orEmpty()

/** Events with no group row of their own to sit under: listed after the groups, not indented. */
internal val OTHER_EVENT_ROWS: List<EventColorRow> = listOf(
    EventColorRow("Delete", Icons.Rounded.Delete, listOf(IslandEvent.FILE_DELETE)),
    EventColorRow("Face ID", Icons.Rounded.Face, listOf(IslandEvent.FACE_UNLOCK))
)

internal fun EventColorRow.shownColor(settings: SmartIslandSettings): Long = eventShownColor(events.first(), settings)

internal fun EventColorRow.hasOwnColor(settings: SmartIslandSettings): Boolean =
    events.any { settings.eventColors[it.id] != null }

/**
 * 1 event row of the Colors & Opacity card, with the same swatches as a group row. [indented] sets it inward under
 * the group row it belongs to
 */
@Composable
internal fun EventColorRowItem(
    row: EventColorRow,
    settings: SmartIslandSettings,
    indented: Boolean,
    onSetColor: (EventColorRow, Long) -> Unit,
    onPickCustom: (EventColorRow, Long) -> Unit,
    onReset: (EventColorRow) -> Unit
) {
    val shown = row.shownColor(settings)
    val own = row.hasOwnColor(settings)
    val badge = if (indented) 28.dp else 32.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) EVENT_ROW_INDENT else 0.dp, top = 6.dp, bottom = 6.dp),
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
                    .size(badge)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(shown).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    tint = Color(shown),
                    modifier = Modifier.size(if (indented) 14.dp else 16.dp)
                )
            }
            Column {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (own) {
                    Text(
                        text = stringResource(R.string.event_color_reset),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .bounceClick { onReset(row) }
                            .padding(end = 6.dp, top = 2.dp)
                    )
                }
            }
        }
        ColorPresetRow(
            selectedColor = shown,
            swatches = PRESET_COLORS,
            onColorSelected = { onSetColor(row, it) },
            onCustomClicked = { onPickCustom(row, shown) }
        )
    }
}

/** How far an event row sits in from its group row. */
internal val EVENT_ROW_INDENT = 16.dp
