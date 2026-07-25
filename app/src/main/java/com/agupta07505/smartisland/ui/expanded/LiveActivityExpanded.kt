/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.LiveActivityParser

@Composable
fun LiveActivityExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {}
) {
    val context = LocalContext.current

    val brandColor = remember(notification?.packageName) {
        Color(LiveActivityParser.getBrandColor(notification?.packageName))
    }

    val (etaText, progressRatio, statusTitle, subStatusText) = remember(notification) {
        if (notification == null) {
            Tuple4("Active", 0.65f, "Live Tracking", "Tracking in real-time")
        } else {
            val text = "${notification.title} ${notification.text}"
            val matcher = java.util.regex.Pattern.compile("(\\d+)\\s*(?:mins?|minutes?|min|m)\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text)
            val eta = if (matcher.find()) "${matcher.group(1)} min" else "Active"
            val pct = if (notification.progressMax > 0) (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0.15f, 0.95f) else 0.65f
            val title = if (notification.title.isNotBlank()) notification.title else notification.appName
            val sub = if (notification.text.isNotBlank()) notification.text else "Live activity update"
            Tuple4(eta, pct, title, sub)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Section: Icon on Left (42.dp like NotificationExpanded) + Title/Text in Middle + ETA Badge on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val largeIcon = notification?.largeIcon
            val icon = notification?.icon
            val mainIcon = largeIcon ?: icon

            if (mainIcon != null) {
                val clipShape = if (largeIcon != null) CircleShape else RoundedCornerShape(8.dp)
                Box(modifier = Modifier.size(42.dp)) {
                    Image(
                        bitmap = mainIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(clipShape)
                    )
                    if (largeIcon != null && icon != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color.Black, CircleShape)
                                .padding(1.5.dp)
                        ) {
                            Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brandColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification?.appName?.firstOrNull()?.uppercase() ?: "L",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Middle Column: Title & Text (Matching NotificationExpanded layout)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subStatusText,
                    color = Color(0xFFD5DAE0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            // Right: ETA Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(brandColor.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = etaText,
                    color = brandColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Route & Distance Progress Visualizer
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val cy = size.height / 2f
                    val strokeWidth = 4.dp.toPx()

                    // Background path line
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(12.dp.toPx(), cy),
                        end = Offset(width - 12.dp.toPx(), cy),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Traveled progress fill line
                    val currentX = (12.dp.toPx() + (width - 24.dp.toPx()) * progressRatio).coerceIn(12.dp.toPx(), width - 12.dp.toPx())
                    drawLine(
                        color = brandColor,
                        start = Offset(12.dp.toPx(), cy),
                        end = Offset(currentX, cy),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Start node dot (Origin)
                    drawCircle(color = brandColor, radius = 4.dp.toPx(), center = Offset(12.dp.toPx(), cy))

                    // Current position dot (Traveled position)
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(currentX, cy))
                    drawCircle(color = brandColor, radius = 4.dp.toPx(), center = Offset(currentX, cy))

                    // Destination node dot
                    drawCircle(color = Color(0xFF64748B), radius = 4.dp.toPx(), center = Offset(width - 12.dp.toPx(), cy))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Initial", color = Color(0xFF94A3B8), fontSize = 10.sp)
                Text("Traveled ${(progressRatio * 100).toInt()}%", color = brandColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Destination", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }

        // Action Buttons Row & Collapse Arrow (Matching NotificationExpanded)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notification != null && notification.actionIntents.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    notification.actionIntents.take(2).forEach { action ->
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE2E8F0))
                                .bounceClick {
                                    if (action.pendingIntent != null) {
                                        triggerAction(context, notification.packageName, action.pendingIntent, action.title, notification.contentIntent)
                                    } else {
                                        Toast.makeText(context, "Clicked: ${action.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    val repo = SmartIslandRepositories.notificationRepository(context)
                                    repo.removeNotification(notification.key)
                                    repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(notification.key))
                                    onCollapse()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action.title,
                                color = Color(0xFF1F2937),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .bounceClick { onOpenNotification() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Open App",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
