/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.components.BatteryBadge
import com.agupta07505.smartisland.ui.components.BatteryVisual
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.rememberBatteryPulse
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
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

    val visual = resolveBatteryVisual(notification.category, notification.title, pct)
    val batteryColor = batteryVisualColor(visual, Color(settings.batteryColor))
    // The bar's sheen goes toward white on the dark island and toward black on the light one (a white sheen would vanish on pale).
    val lightLook = LocalIslandPalette.current.isLight
    val midBatteryColor = if (lightLook) lerp(batteryColor, Color.Black, 0.12f) else lerp(batteryColor, Color.White, 0.35f)
    val lightestBatteryColor = if (lightLook) lerp(batteryColor, Color.Black, 0.24f) else lerp(batteryColor, Color.White, 0.65f)
    val pulse = rememberBatteryPulse(visual)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val barColors = remember(batteryColor, midBatteryColor, lightestBatteryColor) {
        listOf(batteryColor, midBatteryColor, lightestBatteryColor, midBatteryColor, batteryColor)
    }
    val barShape = RoundedCornerShape(6.dp)

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
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val remainingMs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                runCatchingLogged("BatteryExpanded", "computeChargeTimeRemaining failed") {
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
            BatteryBadge(
                visual = visual,
                progress = progress,
                color = batteryColor,
                pulse = pulse,
                boxSize = 46.dp,
                iconSize = 20.dp,
                disc = true
            )

            Column {
                Text(
                    text = notification.title.takeIf { it.isNotBlank() } ?: when (visual) {
                        BatteryVisual.Saver -> "Battery Saver ON"
                        BatteryVisual.Low -> "Low Battery"
                        BatteryVisual.PluggedIn -> "Plugged in"
                        BatteryVisual.PluggedOut -> "Unplugged"
                        BatteryVisual.Full -> "Fully Charged"
                    },
                    color = LocalIslandPalette.current.text,
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
                Text(
                    text = timeText,
                    color = LocalIslandPalette.current.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
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
                    .border(1.5.dp, LocalIslandPalette.current.tint(0.2f), RoundedCornerShape(8.dp))
                    .background(LocalIslandPalette.current.tint(0.1f), RoundedCornerShape(8.dp))
                    .padding(2.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.value)
                        .graphicsLayer { alpha = if (visual == BatteryVisual.Low) pulse.value else 1f }
                        .drawBehind {
                            val x = 0f
                            drawOutline(
                                outline = barShape.createOutline(size, layoutDirection, this),
                                brush = Brush.horizontalGradient(
                                    colors = barColors,
                                    startX = x,
                                    endX = x + 300f,
                                    tileMode = TileMode.Repeated
                                )
                            )
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(LocalIslandPalette.current.tint(0.15f), Color.Transparent),
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
                    .background(LocalIslandPalette.current.tint(0.4f), shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            )
        }
    }
}
