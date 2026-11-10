/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.CalendarRules
import com.agupta07505.smartisland.model.DndModes
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.ModeKind
import com.agupta07505.smartisland.model.ModeStatus
import com.agupta07505.smartisland.model.NetConnection
import com.agupta07505.smartisland.model.NetUsage
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherStat
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.weatherLook
import com.agupta07505.smartisland.util.IdleWidgetData
import com.agupta07505.smartisland.util.quietly
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
internal fun ClockIslandBody(frame: IdleWidgetFrame) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", locale) }
    val dateFormat = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
    }
    val date = Date(frame.data.nowMs)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeFormat.format(date),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = dateFormat.format(date),
            color = SOFT_WHITE,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
internal fun DateIslandBody(frame: IdleWidgetFrame) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val is24 = DateFormat.is24HourFormat(context)
    val text = remember(frame.data.nowMs, locale) {
        val date = Date(frame.data.nowMs)
        if (WidgetIslandContent.usesEnglishOrdinalDate(locale.language)) {
            WidgetIslandContent.englishLongDate(
                weekdayName = SimpleDateFormat("EEEE", locale).format(date),
                day = SimpleDateFormat("d", locale).format(date).toIntOrNull() ?: 1,
                monthName = SimpleDateFormat("MMMM", locale).format(date),
                year = SimpleDateFormat("yyyy", locale).format(date).toIntOrNull() ?: 0
            )
        } else {
            SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMy"), locale).format(date)
        }
    }
    // Next calendar event within 24 hours under the date: title, start time and time until then
    val event = frame.data.nextEvent?.takeIf { CalendarRules.islandShows(it.beginMs, frame.data.nowMs) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = if (event == null) 3 else 2
        )
        if (event != null) {
            val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", locale) }
            val weekdayName = remember(locale) { SimpleDateFormat("EEE", locale) }
            val at = event.beginMs
            val whenText = (if (sameCalendarDay(at,
                frame.data.nowMs)) "" else weekdayName.format(Date(at)) + " ") + timeFormat.format(Date(at))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Rounded.Event, contentDescription = null, tint = RED,
                        modifier = Modifier.size(14.dp))
                    Text(
                        text = CalendarRules.shortTitle(event.title, 22) + " \u00B7 " + whenText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text(
                    text = "in " + WidgetIslandContent.durationText(at - frame.data.nowMs),
                    color = SOFT_WHITE,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
internal fun WeatherIslandBody(frame: IdleWidgetFrame) {
    val snapshot = frame.data.weather ?: return
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val available = remember(snapshot, frame.weatherReadouts) {
        WeatherDisplay.availableReadouts(frame.weatherReadouts, snapshot, System.currentTimeMillis())
    }
    val extras = snapshot.extras
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WeatherDisplay.ISLAND_GAP_DP.dp)
    ) {
        Row(
            modifier = Modifier.height(WeatherDisplay.ISLAND_HEADER_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IdleWidgetIcon(IdleWidgetKind.Weather, frame, size = 30.dp)
            Column {
                Text(
                    text = IdleWidgets.displayTemp(snapshot.temperatureC,
                        frame.fahrenheit) + if (frame.fahrenheit) "F" else "C",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = WidgetIslandContent.weatherName(snapshot.weatherCode, snapshot.isDay),
                    color = SOFT_WHITE,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
        if (extras != null && available.isNotEmpty()) {
            val stats = available.mapNotNull { WeatherDisplay.stat(it, extras, frame.fahrenheit, is24) }
            stats.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth().height(WeatherDisplay.ISLAND_STAT_ROW_DP.dp)) {
                    pair.forEach { stat -> WeatherStatCell(stat, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (WeatherReadouts.HOURLY in available) {
                Row(modifier = Modifier.fillMaxWidth().height(WeatherDisplay.ISLAND_HOURLY_DP.dp)) {
                    WeatherDisplay.upcomingHours(extras, System.currentTimeMillis()).forEach { hour ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = WeatherDisplay.hourLabel(hour.epochSec, extras.utcOffsetSec, is24),
                                color = SOFT_WHITE,
                                fontSize = 9.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = weatherIcon(weatherLook(hour.code),
                                    WeatherDisplay.isDayAt(extras, hour.epochSec)).first,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = IdleWidgets.displayTemp(hour.tempC, frame.fahrenheit),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherStatCell(stat: WeatherStat, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(text = stat.label, color = SOFT_WHITE, fontSize = 9.sp, maxLines = 1, softWrap = false)
        Text(text = stat.value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            softWrap = false)
    }
}

@Composable
internal fun WindIslandBody(frame: IdleWidgetFrame) {
    val snapshot = frame.data.weather ?: return
    val direction = IdleWidgets.compass(snapshot.windDirectionDeg)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IdleWidgetIcon(IdleWidgetKind.Wind, frame, size = 30.dp)
        Column {
            Text(
                text = IdleWidgets.windIslandText(snapshot.windKmh, frame.windUnit),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = listOfNotNull("Wind", direction).joinToString(" \u00B7 "),
                color = SOFT_WHITE,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun BatteryIslandBody(frame: IdleWidgetFrame, metric: String) {
    val data = frame.data
    val toFull = if (data.chargeTimeRemainingMs > 0L) WidgetIslandContent.durationText(data.chargeTimeRemainingMs) + " to full" else null
    val power = if (metric == WidgetIslandContent.BATTERY_VOLT) {
        data.batteryVoltageMv.takeIf { it > 0 }?.let { WidgetIslandContent.voltsText(it) }
    } else {
        WidgetIslandContent.batteryWatts(data.batteryCurrentRaw, data.batteryVoltageMv)
            ?.let { WidgetIslandContent.wattsText(it) }
    }
    val temp = data.batteryTempTenthsC.takeIf { it != IdleWidgetData.NO_TEMPERATURE }
        ?.let { WidgetIslandContent.batteryTempText(it, frame.fahrenheit) }
    // Not charging: when the battery runs out ("till 9:45 PM", with the weekday if not today) instead of time to full
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", locale) }
    val weekdayName = remember(locale) { SimpleDateFormat("EEE", locale) }
    val emptyAt = data.batteryEmptyAtMs
    val till = if (!data.charging && emptyAt > data.nowMs) {
        stringResource(
            R.string.battery_island_till,
            (if (sameCalendarDay(emptyAt,
                data.nowMs)) "" else weekdayName.format(Date(emptyAt)) + " ") + timeFormat.format(Date(emptyAt))
        )
    } else {
        null
    }
    val percentColor = when {
        data.charging -> GREEN
        data.batteryPercent in 0..15 -> RED
        else -> Color.White
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IdleWidgetIcon(IdleWidgetKind.Battery, frame, size = 18.dp)
            Text(
                text = if (data.batteryPercent < 0) "--" else "${data.batteryPercent}%",
                color = percentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        (toFull ?: till)?.let { Text(text = it, color = Color.White, fontSize = 11.sp, maxLines = 1, softWrap = false) }
        val detail = listOfNotNull(power, temp).joinToString("  \u00B7  ")
        if (detail.isNotEmpty()) Text(text = detail, color = SOFT_WHITE, fontSize = 10.sp, maxLines = 1,
            softWrap = false)
    }
}

@Composable
internal fun AlarmIslandBody(frame: IdleWidgetFrame) {
    val context = LocalContext.current
    val data = frame.data
    val is24 = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", locale) }
    val weekdayName = remember(locale) { SimpleDateFormat("EEE", locale) }
    val at = data.nextAlarmMs
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IdleWidgetIcon(IdleWidgetKind.NextAlarm, frame, size = 26.dp)
        Column {
            Text(
                // An alarm on another day names it ("Tue 06:00")
                text = (if (sameCalendarDay(at,
                    data.nowMs)) "" else weekdayName.format(Date(at)) + " ") + timeFormat.format(Date(at)),
                color = ORANGE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "in " + WidgetIslandContent.durationText(at - data.nowMs),
                color = SOFT_WHITE,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun SpeedIslandBody(frame: IdleWidgetFrame) {
    val data = frame.data
    val connection = data.connection
    val connectionName = stringResource(
        when (connection) {
            NetConnection.Wifi -> R.string.net_conn_wifi
            NetConnection.Mobile -> R.string.net_conn_mobile
            NetConnection.Other -> R.string.net_conn_other
            NetConnection.None -> R.string.net_conn_none
        }
    )
    val connectionIcon = when (connection) {
        NetConnection.Wifi -> Icons.Rounded.Wifi
        NetConnection.Mobile -> Icons.Rounded.SignalCellularAlt
        NetConnection.Other -> Icons.Rounded.SettingsEthernet
        NetConnection.None -> Icons.Rounded.SignalWifiOff
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        // Side by side: upload on the left, download on the right
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SpeedRow(Icons.Rounded.ArrowUpward, ORANGE, WidgetIslandContent.speedText(data.upBytesPerSecond))
            SpeedRow(Icons.Rounded.ArrowDownward, SKY, WidgetIslandContent.speedText(data.downBytesPerSecond))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = connectionIcon, contentDescription = null, tint = SOFT_WHITE,
                modifier = Modifier.size(12.dp))
            Text(
                text = if (connection == NetConnection.None) connectionName
                else connectionName + " \u00B7 " + stringResource(R.string.net_island_today,
                    NetUsage.sizeText(NetUsage.bytesFor(data.usageToday, connection))),
                color = SOFT_WHITE,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false
            )
        }
        if (data.dataSaver) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = Icons.Rounded.DataSaverOn, contentDescription = null, tint = GREEN,
                    modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.net_island_data_saver),
                    color = GREEN,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SpeedRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            softWrap = false)
    }
}

@Composable
internal fun NotificationsIslandBody(frame: IdleWidgetFrame) {
    val rows = NotificationCounts.rows(frame.data.notificationSummary)
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 10.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        rows.shown.forEach { app ->
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppIcon(app.packageName)
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = NotificationCounts.badgeText(app.count), color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
        if (rows.moreApps > 0) {
            Text(
                text = "+${rows.moreNotifications} more",
                color = SOFT_WHITE,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().height(14.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun AppIcon(packageName: String, size: Dp = 16.dp) {
    val context = LocalContext.current
    val icon: ImageBitmap? = remember(packageName) {
        quietly { context.packageManager.getApplicationIcon(packageName).toBitmap(48, 48).asImageBitmap() }
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = Color.White,
            modifier = Modifier.size(size))
    }
}

@Composable
internal fun ModesIslandBody(frame: IdleWidgetFrame) {
    val mode = frame.data.mode
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeIcon(mode, size = 28.dp)
        Column {
            Text(text = mode.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(text = DndModes.detail(mode), color = SOFT_WHITE, fontSize = 10.sp, maxLines = 2)
        }
    }
}

/**
 * Glyph of a mode, white: its own icon when the system exposes it (Android's, or the phone maker's), else a glyph for
 * its kind
 */
@Composable
internal fun ModeIcon(mode: ModeStatus, size: Dp) {
    val context = LocalContext.current
    val own: ImageBitmap? = remember(mode.iconPackage, mode.iconRes) {
        if (mode.iconRes == 0 || mode.iconPackage.isEmpty()) null else quietly {
            val resources = context.packageManager.getResourcesForApplication(mode.iconPackage)
            ResourcesCompat.getDrawable(resources, mode.iconRes, null)?.toBitmap(96, 96)?.asImageBitmap()
        }
    }
    if (own != null) {
        Image(bitmap = own, contentDescription = mode.name, colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(size))
    } else {
        Icon(imageVector = modeFallbackIcon(mode.kind), contentDescription = mode.name, tint = Color.White,
            modifier = Modifier.size(size))
    }
}

internal fun modeFallbackIcon(kind: ModeKind): ImageVector = when (kind) {
    ModeKind.Dnd -> Icons.Rounded.DoNotDisturbOn
    ModeKind.TotalSilence -> Icons.Rounded.VolumeOff
    ModeKind.AlarmsOnly -> Icons.Rounded.Alarm
    ModeKind.Bedtime -> Icons.Rounded.Bedtime
    ModeKind.Driving -> Icons.Rounded.DirectionsCar
    ModeKind.Theater -> Icons.Rounded.TheaterComedy
    ModeKind.Gaming -> Icons.Rounded.SportsEsports
    ModeKind.Work -> Icons.Rounded.Work
    ModeKind.Schedule -> Icons.Rounded.Schedule
    ModeKind.Custom -> Icons.Rounded.NotificationsOff
}

private fun sameCalendarDay(a: Long, b: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = a }
    val second = Calendar.getInstance().apply { timeInMillis = b }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}
