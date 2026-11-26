/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.ActionRole
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.TransferState
import com.agupta07505.smartisland.model.classifyAction
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
fun DownloadExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val kind = notification.transferKind
    val isUpload = kind.pointsUp

    val state = notification.transferState
    val baseAccent = Color(settings.transferColor)
    val accentColor = when (state) {
        TransferState.FAILED -> LocalIslandPalette.current.danger
        TransferState.PAUSED,
        TransferState.WAITING -> LocalIslandPalette.current.warning
        else -> baseAccent
    }
    val badgeLabel = state.badge ?: kind.badge

    val progressFraction = remember(notification.progress, notification.progressMax) {
        if (notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0f, 1f)
        } else {
            0.45f
        }
    }

    // Strip the duplicate percentage from the subtitle so it shows once, on the right
    val cleanSubtitle = remember(notification.text, kind) {
        notification.text
            .replace(Regex("""\s*[•·-]?\s*\d+%\s*"""), "")
            .replace(Regex("""\s*\d+%\s*[•·-]?\s*"""), "")
            .trim()
            .ifBlank { "${kind.badge}..." }
    }

    val palette = LocalIslandPalette.current
    val numberColor = if (state == TransferState.ACTIVE ||
        state == TransferState.COMPLETE
    ) palette.text else accentColor
    val pctNumber = if (notification.progressMax > 0) "${(progressFraction * 100).toInt()}" else "--"
    val fileName = notification.title.takeIf { it.isNotBlank() && !it.equals(kind.fallbackTitle, ignoreCase = true) }
    val detailLine = listOfNotNull(
        cleanSubtitle.takeIf { it.isNotBlank() && !it.equals("${kind.badge}...", ignoreCase = true) },
        notification.transferDetail.takeIf { it.isNotBlank() },
        "Estimated from network speed".takeIf { notification.isEstimatedTransfer }
    ).joinToString(" \u00B7 ")
    // Up to 2 of the app's own buttons
    val buttons = notification.actionIntents.filter { it.pendingIntent != null }.take(2)

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
            .padding(start = 14.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DownloadAppColumn(notification = notification, accentColor = accentColor)

            DownloadReadout(
                notification = notification,
                isUpload = isUpload,
                accentColor = accentColor,
                badgeLabel = badgeLabel,
                pctNumber = pctNumber,
                numberColor = numberColor,
                fileName = fileName,
                detailLine = detailLine
            )

            if (buttons.isNotEmpty()) {
                DownloadButtons(notification = notification, buttons = buttons)
            }
        }

        DownloadProgressBar(
            notification = notification,
            state = state,
            progressFraction = progressFraction,
            numberColor = numberColor,
            accentColor = accentColor
        )
    }
}

@Composable
private fun DownloadAppColumn(notification: IslandNotification, accentColor: Color) {
    val palette = LocalIslandPalette.current
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val mainIcon = notification.largeIcon ?: notification.icon
        if (mainIcon != null) {
            Image(
                bitmap = mainIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.appName.firstOrNull()?.uppercase() ?: "D",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Text(
            text = notification.appName,
            color = palette.textTertiary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.DownloadReadout(
    notification: IslandNotification,
    isUpload: Boolean,
    accentColor: Color,
    badgeLabel: String,
    pctNumber: String,
    numberColor: Color,
    fileName: String?,
    detailLine: String
) {
    val palette = LocalIslandPalette.current
    Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CustomBadgeTransferIcon(isUpload = isUpload, color = accentColor, modifier = Modifier.size(12.dp))
            Text(text = badgeLabel, color = palette.textTertiary, fontSize = 13.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = pctNumber,
                color = numberColor,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                letterSpacing = (-1.5).sp,
                maxLines = 1,
                softWrap = false
            )
            if (notification.progressMax > 0) {
                Text(
                    text = "%",
                    color = numberColor,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        if (fileName != null) {
            Text(text = fileName, color = palette.text, fontSize = 14.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        // the speed / time left line comes and goes while a transfer runs
        Text(text = detailLine.ifBlank { "\u00A0" }, color = palette.textMuted, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DownloadButtons(notification: IslandNotification, buttons: List<IslandNotificationAction>) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        buttons.forEach { action ->
            val glyph = when (classifyAction(action.title)) {
                ActionRole.Pause -> Icons.Rounded.Pause
                ActionRole.Resume -> Icons.Rounded.PlayArrow
                ActionRole.Stop, ActionRole.Reset -> Icons.Rounded.Close
                else -> Icons.Rounded.MoreHoriz
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.5.dp, palette.tint(0.4f), CircleShape)
                    .clip(CircleShape)
                    .bounceClick { notification.fireAction(context, action) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = glyph, contentDescription = action.title, tint = palette.text,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DownloadProgressBar(
    notification: IslandNotification,
    state: TransferState,
    progressFraction: Float,
    numberColor: Color,
    accentColor: Color
) {
    val palette = LocalIslandPalette.current
    Box(modifier = Modifier.padding(top = 14.dp).fillMaxWidth()) {
        if (notification.progressMax > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.tint(0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(3.dp)
                        .background(numberColor)
                )
            }
        } else if (state == TransferState.ACTIVE) {
            // No known progress: a moving bar instead of a made-up fill
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = numberColor,
                trackColor = palette.tint(0.2f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun CustomBadgeTransferIcon(
    isUpload: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
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
            start = Offset(cx, tailY),
            end = Offset(cx, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val wingY = if (isUpload) tipY + arrowHeadHeight else tipY - arrowHeadHeight
        val path = Path().apply {
            moveTo(cx - arrowHeadWidth, wingY)
            lineTo(cx, tipY)
            lineTo(cx + arrowHeadWidth, wingY)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
