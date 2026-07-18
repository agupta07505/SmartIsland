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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.lerp
import com.agupta07505.smartisland.data.SmartIslandSettings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.components.DottedRing
import com.agupta07505.smartisland.util.runCatchingLogged

@Composable
fun BatteryExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings
) {
    val context = LocalContext.current
    val batteryColor = Color(settings.batteryColor)
    val midBatteryColor = lerp(batteryColor, Color.White, 0.35f)
    val lightestBatteryColor = lerp(batteryColor, Color.White, 0.65f)
    val pctText = notification.text?.replace("%", "")?.trim() ?: "49"
    val pct = pctText.toFloatOrNull() ?: 49f
    val progress = (pct / 100f).coerceIn(0f, 1f)

    // Animatable progress slide up
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val flowTransition = rememberInfiniteTransition(label = "electricFlow")
    val flowOffset by flowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowOffset"
    )

    val rotationAngle by flowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dottedRingRotation"
    )

    /*
     * ⚠️ HIDDEN API: BatteryManager.computeChargeTimeRemaining()
     *
     * This is a non-SDK interface restricted on Android 9+ (API 28+).
     * Behavior varies by device:
     *   - Some devices: returns accurate remaining time in milliseconds
     *   - Most devices: returns -1 (hidden API blocked)
     *   - Some OEMs: may throw or return 0
     *
     * We fall back to a heuristic: ~1.5 minutes per remaining percent.
     * This is intentionally a rough estimate — it does not account for
     * varying charge speeds (fast charging, wireless, etc.).
     */
    val timeText = remember(pct) {
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

    // FIX: Standardized to match Notification/Music expanded height to avoid glitch where
    // Battery island expanded more (taller) than other modes. Now uses same padding (18,20,18,bottom)
    // and constrained height similar to NotificationExpanded (90-120dp range).
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
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = notification.title.takeIf { it.isNotBlank() } ?: "Charging",
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
                Text(
                    text = timeText,
                    color = Color(0xFF98A2B3),
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
                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
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
