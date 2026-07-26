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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
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

@Composable
fun DownloadExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val textCombined = "${notification.title} ${notification.text}".lowercase()
    val uploadKeywords = listOf("upload", "uploading", "sending", "posting", "exporting", "backing up", "backup")
    val isUpload = remember(textCombined) { uploadKeywords.any { textCombined.contains(it) } }

    val accentColor = if (isUpload) Color(0xFFAB47BC) else Color(0xFF26C6DA)
    val containerBadgeBg = if (isUpload) Color(0x33AB47BC) else Color(0x3326C6DA)

    val progressFraction = remember(notification.progress, notification.progressMax) {
        if (notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0f, 1f)
        } else {
            0.45f
        }
    }

    val pctText = remember(notification.progress, notification.progressMax) {
        if (notification.progressMax > 0) {
            "${(progressFraction * 100).toInt()}%"
        } else {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (notification.contentIntent != null) {
                    onOpenNotification()
                } else {
                    onCollapse()
                }
            }
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val icon = notification.largeIcon ?: notification.icon
                if (icon != null) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notification.appName.firstOrNull()?.uppercase() ?: "D",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = notification.appName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Transfer Mode Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerBadgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isUpload) Icons.Rounded.FileUpload else Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isUpload) "Uploading" else "Downloading",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Title & Description
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = notification.title.ifBlank { if (isUpload) "Uploading file" else "Downloading file" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.text.ifBlank { if (isUpload) "Uploading..." else "Downloading..." },
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (pctText.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pctText,
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated Smooth Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )
        }

        // Action Intents (if any, e.g. Pause / Cancel)
        if (notification.actionIntents.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                notification.actionIntents.forEach { action ->
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
                                repo.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
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
        }
    }
}
