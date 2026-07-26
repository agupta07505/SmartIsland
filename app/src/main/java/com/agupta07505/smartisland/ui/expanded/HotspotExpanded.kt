/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.formatNotificationTime

@Composable
fun HotspotExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = Color(0xFFFF9800)
    val badgeBg = Color(0x33FF9800)

    val deviceCountText = remember(notification.text, notification.title) {
        val fullText = "${notification.title} ${notification.text}"
        val digits = Regex("""\b(\d+)\s*(device|connected|client)s?\b""", RegexOption.IGNORE_CASE)
            .find(fullText)?.groupValues?.get(1)
        when {
            digits != null -> "$digits connected"
            fullText.lowercase().contains("no device") || fullText.lowercase().contains("0 device") -> "0 connected"
            else -> "Active"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable {
                if (notification.contentIntent != null) {
                    onOpenNotification()
                } else {
                    onCollapse()
                }
            }
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Row matching NotificationExpanded alignment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App / Hotspot Icon (42.dp matching NotificationExpanded)
            val largeIcon = notification.largeIcon
            val icon = notification.icon
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
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiTethering,
                        contentDescription = "Hotspot",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title & Subtitle Column matching NotificationExpanded font sizes
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title.ifBlank { "Mobile Hotspot" },
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = notification.text.ifBlank { "Sharing mobile data" },
                    color = Color(0xFFD5DAE0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            // Right side: Badge Tag & Timestamp
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Hotspot",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = formatNotificationTime(notification.timeMillis),
                    color = Color(0xFFB7C0CA),
                    fontSize = 11.sp
                )
            }
        }

        // Connected Devices Detail Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x22FFFFFF))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Devices,
                        contentDescription = "Devices",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Connected Devices",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = deviceCountText,
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE2E8F0))
                    .bounceClick {
                        runCatching {
                            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            Toast.makeText(context, "Opening Hotspot settings...", Toast.LENGTH_SHORT).show()
                        }
                        onCollapse()
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hotspot Settings",
                    color = Color(0xFF1F2937),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (notification.actionIntents.isNotEmpty()) {
                notification.actionIntents.forEach { action ->
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x33FFFFFF))
                            .bounceClick {
                                if (action.pendingIntent != null) {
                                    triggerAction(context, notification.packageName, action.pendingIntent, action.title, notification.contentIntent)
                                } else {
                                    Toast.makeText(context, "Clicked: ${action.title}", Toast.LENGTH_SHORT).show()
                                }
                                val repo = SmartIslandRepositories.notificationRepository(context)
                                repo.removeNotification(notification.key)
                                repo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
                                onCollapse()
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = action.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
