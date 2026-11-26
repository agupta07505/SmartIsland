/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PulseValueText
import com.agupta07505.smartisland.model.TransferGlyphMotion
import com.agupta07505.smartisland.model.TransferTick
import com.agupta07505.smartisland.util.LiveActivityParser
import com.agupta07505.smartisland.util.NavigationParser
import com.agupta07505.smartisland.util.TurnDirection
import kotlinx.coroutines.delay

@Composable
internal fun CallTimer(postTimeMillis: Long, color: Color) {
    var elapsedSeconds by remember(postTimeMillis) {
        mutableLongStateOf(((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L))
    }
    LaunchedEffect(postTimeMillis) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L)
            delay(1000)
        }
    }
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val clock = "%02d:%02d".format(minutes, seconds)
    // Sized to its slot: a thin edge pill shows only whole minutes (seconds under 1)
    FitSlotText(
        text = clock,
        color = color,
        fontWeight = FontWeight.SemiBold,
        compact = if (minutes == 0L) "${seconds}s" else "${minutes}m"
    )
}

@Composable
internal fun LiveActivityCollapsedGlyph(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val brandColor = remember(notification?.packageName, settings.liveActivityColor, settings.cardEventColor) {
        if (settings.cardEventColor != null) {
            // the colour the user gave the event beats the app's own brand colour
            Color(settings.cardEventColor)
        } else if (notification != null) {
            Color(LiveActivityParser.getBrandColor(notification.packageName))
        } else {
            Color(settings.liveActivityColor)
        }
    }
    val progress = remember(notification) {
        if (notification != null && notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0.15f, 1f)
        } else {
            0.65f
        }
    }

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SmoothCircularRingProgress(
            progress = progress,
            color = brandColor,
            strokeWidthDp = GLYPH_RING_STROKE_DP,
            modifier = Modifier.size(24.dp)
        )
        val mainIcon = notification?.largeIcon ?: notification?.icon
        if (mainIcon != null) {
            Image(
                bitmap = mainIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
            )
        } else {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}

internal const val GLYPH_RING_STROKE_DP = 1.5f

internal val GLYPH_RING_SHOWN: () -> Float = { 1f }
internal val GLYPH_RING_HIDDEN: () -> Float = { 0f }

/** How visible the ring around a glyph is; the wide pulse's progress line takes its place while it runs. */
internal val LocalGlyphRingAlpha = compositionLocalOf { GLYPH_RING_SHOWN }

@Composable
internal fun SmoothCircularRingProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = GLYPH_RING_STROKE_DP,
    trackColor: Color = Color(0x4DFFFFFF)
) {
    val ringAlpha = LocalGlyphRingAlpha.current
    Canvas(modifier = modifier.graphicsLayer { alpha = ringAlpha() }) {
        val strokeWidth = strokeWidthDp.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeftOffset,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val sweepAngle = (360f * progress.coerceIn(0f, 1f))
        if (sweepAngle > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
internal fun LiveActivityCollapsedRight(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val brandColor = remember(notification?.packageName, settings.liveActivityColor, settings.cardEventColor) {
        if (settings.cardEventColor != null) {
            // the colour the user gave the event beats the app's own brand colour
            Color(settings.cardEventColor)
        } else if (notification != null) {
            Color(LiveActivityParser.getBrandColor(notification.packageName))
        } else {
            Color(settings.liveActivityColor)
        }
    }
    val etaText = remember(notification) {
        if (notification == null) "Active" else PulseValueText.eta(notification.title, notification.text)
    }

    FitSlotText(text = etaText, color = brandColor, fontWeight = FontWeight.Bold, compact = compactEta(etaText))
}

@Composable
internal fun NavigationCollapsedGlyph(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val navColor = Color(settings.navigationColor)
    val turnDirection = remember(notification) {
        val title = notification?.title.orEmpty()
        val text = notification?.text.orEmpty()
        NavigationParser.parseTurnDirection("$title $text".lowercase())
    }

    val arrow = rememberTurnArrowMotion(turnDirection)

    if (turnDirection == TurnDirection.DESTINATION) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = "Destination",
            tint = navColor,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = arrow.scale; scaleY = arrow.scale }
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Navigation,
            contentDescription = "Turn direction",
            tint = navColor,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = arrow.scale; scaleY = arrow.scale }
                .rotate(arrow.angleDeg)
        )
    }
}

@Composable
internal fun NavigationCollapsedRight(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val distanceText = remember(notification) {
        if (notification == null) "200 m" else PulseValueText.distance(notification.title, notification.text)
    }

    FitSlotText(text = distanceText, color = Color(settings.navigationColor), fontWeight = FontWeight.Bold,
        compact = compactDistance(distanceText))
}

@Composable
private fun CustomPremiumTransferIcon(
    isUpload: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    motionProgress: Float = 0.5f,
    alphaFraction: Float = 1f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val strokeWidth = 1.8.dp.toPx()
        val arrowHeadWidth = 3.5.dp.toPx()
        val arrowHeadHeight = 3.5.dp.toPx()
        val shaftLength = 6.dp.toPx()

        val startY = if (isUpload) h * 0.95f else -h * 0.15f
        val endY = if (isUpload) -h * 0.15f else h * 0.95f
        val currentCenterY = startY + (endY - startY) * motionProgress

        val tipY = if (isUpload) currentCenterY - shaftLength / 2f else currentCenterY + shaftLength / 2f
        val tailY = if (isUpload) currentCenterY + shaftLength / 2f else currentCenterY - shaftLength / 2f

        val drawAlpha = alphaFraction.coerceIn(0f, 1f)
        val drawColor = color.copy(alpha = drawAlpha)

        drawLine(
            color = drawColor,
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
            color = drawColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val particleOffsetY = if (isUpload) 4.5.dp.toPx() else -4.5.dp.toPx()
        val particleY = tailY + particleOffsetY
        val particleAlpha = (drawAlpha * 0.5f).coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = particleAlpha),
            radius = strokeWidth * 0.6f,
            center = Offset(cx, particleY)
        )
    }
}

@Composable
internal fun DownloadUploadCollapsedRight(
    notification: IslandNotification?,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val transferKind = notification?.transferKind
    val isUpload = transferKind?.pointsUp == true
    val accentColor = Color(settings.transferColor)

    val progress = remember(notification) {
        if (notification != null && notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0.1f, 1f)
        } else {
            0.55f
        }
    }
    val completed = notification != null && TransferTick.isDone(notification)
    val doneColor = Color(0xFF4ADE80)
    val ringColor by animateColorAsState(
        targetValue = if (completed) doneColor else accentColor,
        animationSpec = tween(200),
        label = "transferDoneColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "downloadUploadAnim")

    val motionFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrowFlow"
    )

    val arrowAlpha = when {
        motionFraction < 0.2f -> motionFraction / 0.2f
        motionFraction > 0.8f -> (1f - motionFraction) / 0.2f
        else -> 1f
    }.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        SmoothCircularRingProgress(
            progress = if (completed) 1f else progress,
            color = ringColor,
            strokeWidthDp = GLYPH_RING_STROKE_DP,
            modifier = Modifier.size(19.dp)
        )

        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = completed,
                transitionSpec = {
                    (scaleIn(initialScale = 0.4f, animationSpec =
                        spring(dampingRatio = 0.55f, stiffness = 500f)) + fadeIn(tween(120)))
                        .togetherWith(scaleOut(targetScale = 0.4f, animationSpec = tween(120)) + fadeOut(tween(100)))
                },
                label = "transferDoneIcon"
            ) { done ->
                if (done) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Done",
                        tint = doneColor,
                        modifier = Modifier.size(12.dp)
                    )
                } else if (transferKind != null && TransferGlyphMotion.hasOwnGlyph(transferKind)) {
                    TransferKindGlyph(
                        kind = transferKind,
                        color = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    CustomPremiumTransferIcon(
                        isUpload = isUpload,
                        color = accentColor,
                        motionProgress = motionFraction,
                        alphaFraction = arrowAlpha,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
