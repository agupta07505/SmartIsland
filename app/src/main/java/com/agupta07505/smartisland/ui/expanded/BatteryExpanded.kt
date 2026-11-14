/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.components.BatteryVisual
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.rememberBatteryPulse
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
import com.agupta07505.smartisland.ui.rememberBatterySaverOn
import com.agupta07505.smartisland.util.IdleWidgetData
import com.agupta07505.smartisland.util.UtilityActions
import com.agupta07505.smartisland.util.readBattery
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.agupta07505.smartisland.ui.IslandText as Text

private const val TAG = "BatteryExpanded"

internal val BATTERY_EXPANDED_HEIGHT = 92.dp

/** Battery card in the Pixel glance look. */
@Composable
fun BatteryExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings
) {
    val context = LocalContext.current
    val pctText = notification.text?.replace("%", "")?.trim() ?: "49"
    val pct = pctText.toFloatOrNull() ?: 49f
    val progress = (pct / 100f).coerceIn(0f, 1f)

    val visual = resolveBatteryVisual(notification.category, notification.title, pct)
    val batteryColor = batteryVisualColor(visual, Color(settings.batteryColor), settings.cardEventColor)
    val saverOn by rememberBatterySaverOn(track = true)
    val pulse = rememberBatteryPulse(visual)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val live = rememberLiveBattery(context)
    val fahrenheit = IdleWidgets.useFahrenheit(settings.idleWidgetTempUnit, Locale.getDefault().country)
    val temp = live.batteryTempTenthsC.takeIf { it != IdleWidgetData.NO_TEMPERATURE }
        ?.let { WidgetIslandContent.batteryTempText(it, fahrenheit) }

    val timeText = rememberBatteryTimeText(context, pct, visual)

    val charging = visual == BatteryVisual.PluggedIn || visual == BatteryVisual.Full
    val title = notification.title.takeIf { it.isNotBlank() } ?: when (visual) {
        BatteryVisual.Saver -> stringResource(R.string.expanded_battery_saver)
        BatteryVisual.Low -> stringResource(R.string.expanded_battery_low)
        BatteryVisual.PluggedIn -> stringResource(R.string.expanded_battery_charging)
        BatteryVisual.PluggedOut -> "Unplugged"
        BatteryVisual.Full -> "Fully Charged"
    }
    val icon: ImageVector = when (visual) {
        BatteryVisual.Low -> Icons.Rounded.BatteryAlert
        BatteryVisual.Saver -> Icons.Rounded.BatterySaver
        BatteryVisual.PluggedOut -> Icons.Rounded.BatteryFull
        else -> Icons.Rounded.Bolt
    }
    val percentFont = if (charging) 46.sp else 40.sp
    val unitFont = if (charging) 20.sp else 18.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 1 fixed height for every battery look, so a line that comes or goes
            .heightIn(min = BATTERY_EXPANDED_HEIGHT)
            .padding(start = 22.dp, top = 8.dp, end = 14.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BatteryTitleRow(
                icon = icon,
                title = title,
                charging = charging,
                batteryColor = batteryColor,
                visual = visual
            )
            if (charging) {
                ChargingBody(
                    pct = pct,
                    percentFont = percentFont,
                    unitFont = unitFont,
                    batteryColor = batteryColor,
                    animatedProgress = animatedProgress,
                    live = live,
                    temp = temp,
                    timeText = timeText
                )
            } else {
                OnBatteryBody(
                    pct = pct,
                    percentFont = percentFont,
                    unitFont = unitFont,
                    batteryColor = batteryColor,
                    animatedProgress = animatedProgress,
                    live = live,
                    temp = temp,
                    timeText = timeText,
                    visual = visual,
                    pulse = pulse
                )
            }
        }
        // battery saver button: only on the Unplugged and Low cards
        if (visual == BatteryVisual.PluggedOut || visual == BatteryVisual.Low) {
            BatterySaverButton(saverOn = saverOn, settings = settings)
        }
    }
}

@Composable
private fun rememberLiveBattery(context: Context): IdleWidgetData {
    val live = remember { IdleWidgetData() }
    LaunchedEffect(Unit) {
        while (true) {
            val sticky = runCatchingLogged(TAG, "battery read failed") {
                context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
            if (sticky != null) runCatchingLogged(TAG, "readBattery failed") { readBattery(context.applicationContext, live, sticky) }
            delay(3000L)
        }
    }
    return live
}

@Composable
private fun rememberBatteryTimeText(context: Context, pct: Float, visual: BatteryVisual): String {
    val timeText = remember(pct, visual) {
        if (visual == BatteryVisual.PluggedIn) {
            "Charger connected"
        } else if (visual == BatteryVisual.PluggedOut) {
            "Running on battery"
        } else if (visual == BatteryVisual.Full) {
            "Battery full"
        } else if (visual == BatteryVisual.Saver) {
            "Power Saver active"
        } else if (visual == BatteryVisual.Low) {
            "Connect charger"
        } else {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val remainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatchingLogged(TAG, "computeChargeTimeRemaining failed") {
                    batteryManager?.computeChargeTimeRemaining() ?: -1L
                } ?: -1L
            } else {
                -1L
            }
            if (remainingMs > 0L) {
                val totalMins = remainingMs / 60000L
                val h = totalMins / 60L
                val m = totalMins % 60L
                if (h > 0) "${h} h ${m} m until full" else "${m} m until full"
            } else {
                val totalMins = ((100f - pct) * 1.5f).toInt()
                val h = totalMins / 60
                val m = totalMins % 60
                if (h > 0) "${h} h ${m} m until full" else "${m} m until full"
            }
        }
    }
    return timeText
}

@Composable
private fun BatteryDigits(pct: Float, color: Color, percentFont: TextUnit, unitFont: TextUnit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "${pct.toInt()}",
            color = color,
            fontSize = percentFont,
            lineHeight = percentFont,
            letterSpacing = (-1.5).sp,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = "%",
            color = color,
            fontSize = unitFont,
            lineHeight = unitFont,
            modifier = Modifier.padding(start = 1.dp, bottom = 4.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun BatteryTitleRow(
    icon: ImageVector,
    title: String,
    charging: Boolean,
    batteryColor: Color,
    visual: BatteryVisual
) {
    val palette = LocalIslandPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint =
            if (charging) batteryColor else if (visual == BatteryVisual.PluggedOut) palette.textTertiary else batteryColor, modifier = Modifier.size(15.dp))
        Text(text = title, color = palette.textTertiary, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChargingBody(
    pct: Float,
    percentFont: TextUnit,
    unitFont: TextUnit,
    batteryColor: Color,
    animatedProgress: Animatable<Float, AnimationVector1D>,
    live: IdleWidgetData,
    temp: String?,
    timeText: String
) {
    val palette = LocalIslandPalette.current
    val dim = batteryColor.copy(alpha = 0.38f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            BatteryDigits(pct, dim, percentFont, unitFont)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        clipRect(right = size.width * animatedProgress.value) { this@drawWithContent.drawContent() }
                    }
            ) { BatteryDigits(pct, batteryColor, percentFont, unitFont) }
        }
        val watts = WidgetIslandContent.batteryWatts(live.batteryCurrentRaw, live.batteryVoltageMv)
            ?.let { WidgetIslandContent.wattsText(it) }
        val detail = listOfNotNull(watts, temp).joinToString("  \u00B7  ")
        Column(modifier = Modifier.weight(1f)) {
            Text(text = timeText, color = palette.text, fontSize = 13.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(text = detail.ifEmpty { "\u00A0" }, color = palette.textMuted, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OnBatteryBody(
    pct: Float,
    percentFont: TextUnit,
    unitFont: TextUnit,
    batteryColor: Color,
    animatedProgress: Animatable<Float, AnimationVector1D>,
    live: IdleWidgetData,
    temp: String?,
    timeText: String,
    visual: BatteryVisual,
    pulse: State<Float>
) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    val numberColor = when {
        visual == BatteryVisual.Saver || visual == BatteryVisual.Low -> batteryColor
        pct <= 15f -> batteryColor
        else -> palette.text
    }
    val emptyAt = live.batteryEmptyAtMs
    val now = System.currentTimeMillis()
    val sub = if (visual == BatteryVisual.PluggedOut && emptyAt > now) {
        val is24 = DateFormat.is24HourFormat(context)
        val locale = Locale.getDefault()
        val clock = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", locale).format(Date(emptyAt))
        val sameDay = Calendar.getInstance().apply { timeInMillis = emptyAt }.get(Calendar.DAY_OF_YEAR) ==
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_YEAR)
                && emptyAt - now < 24L * 3_600_000L
        stringResource(R.string.battery_island_till, (if (sameDay) "" else SimpleDateFormat("EEE", locale)
            .format(Date(emptyAt)) + " ") + clock)
    } else {
        timeText
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.graphicsLayer { alpha =
            if (visual == BatteryVisual.Low) pulse.value else 1f }) { BatteryDigits(pct, numberColor, percentFont,
            unitFont) }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = sub, color = palette.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = temp ?: "\u00A0", color = palette.textMuted, fontSize = 12.sp, maxLines = 1)
        }
    }
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(palette.tint(0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress.value)
                .background(numberColor)
        )
    }
}

@Composable
private fun BatterySaverButton(saverOn: Boolean, settings: SmartIslandSettings) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    val saverTint = batteryVisualColor(BatteryVisual.Saver, Color(settings.batteryColor), settings.cardEventColor)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (saverOn) saverTint.copy(alpha = 0.22f) else palette.tint(0.14f))
            .bounceClick { UtilityActions.run(context, UtilityType.BatterySaver) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BatterySaver,
            contentDescription = stringResource(R.string.utility_battery_saver),
            tint = if (saverOn) saverTint else palette.text,
            modifier = Modifier.size(22.dp)
        )
    }
}
