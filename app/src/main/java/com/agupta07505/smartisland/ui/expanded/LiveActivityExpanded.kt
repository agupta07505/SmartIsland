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
import androidx.compose.material.icons.rounded.Navigation
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

@Composable
fun LiveActivityExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {}
) {
    val context = LocalContext.current

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
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: App icon + App name + ETA Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = notification?.largeIcon ?: notification?.icon
                if (icon != null) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notification?.appName?.firstOrNull()?.uppercase() ?: "L",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = notification?.appName ?: "Live Activity",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // ETA Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = etaText,
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Title and SubStatus
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = statusTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subStatusText,
                color = Color(0xFFB7C0CA),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Route & Distance Progress Visualizer (Origin -> Current position -> Destination)
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                        color = Color(0xFF38BDF8),
                        start = Offset(12.dp.toPx(), cy),
                        end = Offset(currentX, cy),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Start node dot (Origin)
                    drawCircle(color = Color(0xFF38BDF8), radius = 4.dp.toPx(), center = Offset(12.dp.toPx(), cy))

                    // Current position dot (Traveled position)
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(currentX, cy))
                    drawCircle(color = Color(0xFF0EA5E9), radius = 4.dp.toPx(), center = Offset(currentX, cy))

                    // Destination node dot
                    drawCircle(color = Color(0xFF64748B), radius = 4.dp.toPx(), center = Offset(width - 12.dp.toPx(), cy))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Initial", color = Color(0xFF94A3B8), fontSize = 10.sp)
                Text("Traveled ${(progressRatio * 100).toInt()}%", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Destination", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }

        // Action Buttons Row & Collapse Arrow
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
