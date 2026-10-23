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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.formatNotificationTime

@Composable
fun DownloadExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val kind = notification.transferKind
    val isUpload = kind.pointsUp

    val state = notification.transferState
    val baseAccent = Color(settings.transferColor)
    // Paused/waiting turn amber and failed turns red so a stuck transfer reads at a glance.
    val accentColor = when (state) {
        com.agupta07505.smartisland.model.TransferState.FAILED -> LocalIslandPalette.current.danger
        com.agupta07505.smartisland.model.TransferState.PAUSED,
        com.agupta07505.smartisland.model.TransferState.WAITING -> LocalIslandPalette.current.warning
        else -> baseAccent
    }
    val badgeLabel = state.badge ?: kind.badge
    val containerBadgeBg = accentColor.copy(alpha = 0.2f)

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

    // Strip the duplicate percentage from the subtitle so it shows once, on the right.
    val cleanSubtitle = remember(notification.text, kind) {
        notification.text
            .replace(Regex("""\s*[•·-]?\s*\d+%\s*"""), "")
            .replace(Regex("""\s*\d+%\s*[•·-]?\s*"""), "")
            .trim()
            .ifBlank { "${kind.badge}..." }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                                .background(LocalIslandPalette.current.surface, CircleShape)
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
                    Text(
                        text = notification.appName.firstOrNull()?.uppercase() ?: "D",
                        color = LocalIslandPalette.current.text,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title.ifBlank { kind.fallbackTitle },
                    color = LocalIslandPalette.current.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = cleanSubtitle,
                    color = LocalIslandPalette.current.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBadgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CustomBadgeTransferIcon(
                            isUpload = isUpload,
                            color = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = badgeLabel,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = formatNotificationTime(notification.timeMillis),
                    color = LocalIslandPalette.current.textTertiary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notification.progressMax > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LocalIslandPalette.current.tint(0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                }
            } else if (state == com.agupta07505.smartisland.model.TransferState.ACTIVE) {
                // No known progress: a moving bar instead of a made-up 45% fill.
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = LocalIslandPalette.current.tint(0.2f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor.copy(alpha = 0.35f))
                )
            }

            if (pctText.isNotEmpty()) {
                Text(
                    text = pctText,
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Speed / ETA read from the notification, or the measured speed of an estimated card
        val detailLine = listOfNotNull(
            notification.transferDetail.takeIf { it.isNotBlank() },
            "Estimated from network speed".takeIf { notification.isEstimatedTransfer }
        ).joinToString(" · ")
        if (detailLine.isNotBlank()) {
            Text(
                text = detailLine,
                color = LocalIslandPalette.current.textTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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
                            .background(LocalIslandPalette.current.chipFill)
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

@Composable
private fun CustomBadgeTransferIcon(
    isUpload: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val strokeWidth = 1.6.dp.toPx()
        val arrowHeadWidth = 3f.dp.toPx()
        val arrowHeadHeight = 3f.dp.toPx()
        val shaftLength = 5.dp.toPx()

        val tipY = if (isUpload) cy - shaftLength / 2f else cy + shaftLength / 2f
        val tailY = if (isUpload) cy + shaftLength / 2f else cy - shaftLength / 2f

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx, tailY),
            end = androidx.compose.ui.geometry.Offset(cx, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val wingY = if (isUpload) tipY + arrowHeadHeight else tipY - arrowHeadHeight
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - arrowHeadWidth, wingY)
            lineTo(cx, tipY)
            lineTo(cx + arrowHeadWidth, wingY)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}
