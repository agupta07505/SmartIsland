/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.agupta07505.smartisland.util.formatNotificationTime

@Composable
fun NotificationExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        .background(Color(0xFF2563EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(notification?.appName?.firstOrNull()?.uppercase() ?: "S", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Notification",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity",
                    color = Color(0xFFD5DAE0),
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            // Time text on top right using the internal helper in IslandCollapsedContent
            Text(
                text = notification?.let { formatNotificationTime(it.timeMillis) } ?: "",
                color = Color(0xFFB7C0CA),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Bottom Section: Action buttons (left) and Down Arrow Button (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left part of Bottom Section: Action Buttons Row
            if (notification != null && notification.actionIntents.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    notification.actionIntents.forEach { action ->
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE2E8F0)) // light grey background matching the Telegram button
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
                                color = Color(0xFF1F2937), // dark grey text
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Down Arrow Button on bottom right
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
