/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.ui.animateFloatCalm
import com.agupta07505.smartisland.ui.IslandPalette
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.components.DottedRing
import com.agupta07505.smartisland.util.BatteryEstimate
import com.agupta07505.smartisland.util.runCatchingLogged

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

    val titleLower = notification.title.lowercase()
    val isCharging = notification.category == "battery_charging"
    val isBatterySaver = titleLower.contains("saver") || notification.category == "battery_saver"
    val isLowBattery = !isCharging && (titleLower.contains("low") || notification.category == "battery_low" || pct <= 20f)

    val batteryColor = when {
        isLowBattery -> IslandPalette.danger
        isBatterySaver -> IslandPalette.warning
        else -> Color(settings.batteryColor)
    }
    val midBatteryColor = lerp(batteryColor, Color.White, 0.35f)
    val lightestBatteryColor = lerp(batteryColor, Color.White, 0.65f)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val flowTransition = rememberInfiniteTransition(label = "electricFlow")
    val flowOffset by flowTransition.animateFloatCalm(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowOffset"
    )

    val rotationAngle by flowTransition.animateFloatCalm(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dottedRingRotation"
    )

    // Status subtext and the time estimate (Rule_Book §4.12), e.g. "Fast Charging · 35 m until full" or
    // "Discharging · 4 h 10 m left". The line is dropped entirely when there is nothing to add to the title.
    val statusLine = remember(pct, notification.title, isCharging) {
        val isFull = notification.title.contains("full", ignoreCase = true) || (isCharging && pct >= 100f)
        val (status, timeText) = readBatteryStatus(context, isCharging, isFull, pct)
        listOfNotNull(
            status.label.takeUnless { it.equals(notification.title, ignoreCase = true) },
            timeText
        ).joinToString(" · ").ifBlank { null }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding)
            .heightIn(min = 72.dp, max = 110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                DottedRing(
                    progress = progress,
                    rotationAngle = rotationAngle,
                    modifier = Modifier.size(44.dp),
                    color = batteryColor
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(batteryColor.copy(alpha = 0.12f), shape = CircleShape)
                        .border(1.2.dp, batteryColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isBatterySaver -> {
                            Icon(
                                Icons.Rounded.BatterySaver,
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        isLowBattery -> {
                            Icon(
                                Icons.Rounded.BatteryAlert,
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = notification.title.takeIf { it.isNotBlank() } ?: if (isBatterySaver) "Battery Saver ON" else if (isLowBattery) "Low Battery" else "Charging",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = "${pct.toInt()}%",
                    color = batteryColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 22.sp
                )
                if (statusLine != null) {
                    Text(
                        text = statusLine,
                        color = IslandPalette.secondaryLabel,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(32.dp)
                    .border(1.5.dp, IslandPalette.fillStrong, RoundedCornerShape(8.dp))
                    .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                    .padding(2.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.value)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    batteryColor,
                                    midBatteryColor,
                                    lightestBatteryColor,
                                    midBatteryColor,
                                    batteryColor
                                ),
                                startX = flowOffset,
                                endX = flowOffset + 300f,
                                tileMode = TileMode.Repeated
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                                startY = 0f,
                                endY = 40f
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(12.dp)
                    .background(Color(0x66FFFFFF), shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            )
        }
    }
}

/**
 * Charging speed from the sticky battery broadcast plus a time estimate: until full while charging (the OS value,
 * else a rough 1.5 min per percent), until empty while discharging (from charge and draw), or null when unknown.
 */
private fun readBatteryStatus(
    context: Context,
    isCharging: Boolean,
    isFull: Boolean,
    pct: Float
): Pair<BatteryEstimate.Status, String?> {
    val sticky = runCatchingLogged("BatteryExpanded", "Reading battery broadcast failed") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
    }
    val status = BatteryEstimate.status(
        charging = isCharging,
        full = isFull,
        maxMicroAmps = sticky?.getIntExtra("max_charging_current", -1) ?: -1,
        maxMicroVolts = sticky?.getIntExtra("max_charging_voltage", -1) ?: -1
    )
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val timeText = when (status) {
        BatteryEstimate.Status.Full -> null
        BatteryEstimate.Status.FastCharging, BatteryEstimate.Status.Charging -> {
            val remainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatchingLogged("BatteryExpanded", "computeChargeTimeRemaining failed") {
                    batteryManager?.computeChargeTimeRemaining()
                } ?: -1L
            } else -1L
            val minutes = if (remainingMs > 0L) remainingMs / 60_000L else ((100f - pct) * 1.5f).toLong()
            if (minutes > 0L) "${BatteryEstimate.formatMinutes(minutes)} until full" else null
        }
        BatteryEstimate.Status.Discharging -> {
            fun property(id: Int): Long = runCatchingLogged("BatteryExpanded", "Battery property $id failed") {
                batteryManager?.getIntProperty(id)?.takeIf { it != Int.MIN_VALUE }?.toLong()
            } ?: 0L
            BatteryEstimate.minutesUntilEmpty(
                chargeCounterMicroAh = property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                currentNowMicroA = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            )?.let { "${BatteryEstimate.formatMinutes(it)} left" }
        }
    }
    return status to timeText
}
