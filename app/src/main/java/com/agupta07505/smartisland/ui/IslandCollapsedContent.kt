/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.util.formatNotificationTime
import com.agupta07505.smartisland.util.HotspotUtil
import com.agupta07505.smartisland.util.TimerStopwatchParser
import com.agupta07505.smartisland.ui.components.DottedRing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IslandCollapsedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    collapsedAlpha: Float,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier,
    cameraKeepoutDp: Float = 0f
) {
    val density = LocalDensity.current
    val maxTranslationPx = with(density) { COLLAPSED_TRANSLATION_MAX_DP.toPx() }
    val translationProgress = 1f - collapsedAlpha
    val translationXLeft = translationProgress * maxTranslationPx
    val translationXRight = -translationProgress * maxTranslationPx

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Neither slot may reach into the camera hole at the pill's center.
        val keepoutHalf = if (cameraKeepoutDp > 0f) (cameraKeepoutDp / 2f + CAMERA_KEEPOUT_MARGIN_DP).dp else 0.dp
        val leftSlotMax = (maxWidth / 2f - keepoutHalf - LEFT_SLOT_PADDING_START_DP.dp).coerceAtLeast(0.dp)
        val rightSlotMax = (maxWidth / 2f - keepoutHalf - RIGHT_SLOT_PADDING_END_DP.dp).coerceAtLeast(0.dp)

        // Left Slot (Icon / Glyphs)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = LEFT_SLOT_PADDING_START_DP.dp)
                .widthIn(max = leftSlotMax)
                .clipToBounds()
                .graphicsLayer {
                    translationX = translationXLeft
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> NotificationGlyph(notification = notification, settings = settings)
                IslandMode.IncomingCall -> {
                    val icon = notification?.largeIcon ?: notification?.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = null,
                            tint = Color(settings.callColor),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IslandMode.Music -> {
                    val artwork = notification?.largeIcon ?: notification?.icon
                    if (artwork != null) {
                        Image(
                            bitmap = artwork.asImageBitmap(),
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
                                .background(Color(settings.musicVisualizerColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                IslandMode.Battery -> {
                    BatteryCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.LiveActivity -> {
                    LiveActivityCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Navigation -> {
                    NavigationCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.DownloadUpload -> {
                    NotificationGlyph(notification = notification, settings = settings)
                }
                IslandMode.Hotspot -> {
                    HotspotCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Bluetooth -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(settings.bluetoothColor).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.BluetoothConnected,
                            contentDescription = "Bluetooth",
                            tint = Color(settings.bluetoothColor),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IslandMode.Flashlight -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(settings.flashlightColor).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = Color(settings.flashlightColor),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IslandMode.ScreenRecording -> {
                    ScreenRecordingCollapsedGlyph(settings = settings)
                }
                IslandMode.Timer -> {
                    TimerCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Stopwatch -> {
                    StopwatchCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Empty -> Unit
            }
        }

        // Right Slot (Visualizer / Indicators)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = RIGHT_SLOT_PADDING_END_DP.dp)
                .widthIn(max = rightSlotMax)
                .clipToBounds()
                .graphicsLayer {
                    translationX = translationXRight
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(settings.notificationDotColor))
                    )
                }
                IslandMode.IncomingCall -> {
                    if (notification?.isCallRinging == true) {
                        Text(
                            text = "Ringing...",
                            color = Color(settings.callColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val time = notification?.timeMillis ?: System.currentTimeMillis()
                        CallTimer(postTimeMillis = time, color = Color(settings.callColor))
                    }
                }
                IslandMode.Music -> {
                    AudioVisualizer(
                        isPlaying = notification?.mediaIsPlaying == true,
                        color = Color(settings.musicVisualizerColor)
                    )
                }
                IslandMode.Battery -> {
                    val pctText = notification?.text ?: "49%"
                    val title = notification?.title?.lowercase() ?: ""
                    val isBatterySaver = title.contains("saver") || notification?.category == "battery_saver"
                    val isLowBattery = title.contains("low") || notification?.category == "battery_low" || (pctText.replace("%", "").toFloatOrNull() ?: 50f) <= 20f
                    val textColor = when {
                        isLowBattery -> Color(0xFFEF4444)
                        isBatterySaver -> Color(0xFFF59E0B)
                        else -> Color(settings.batteryColor)
                    }
                    Text(
                        text = pctText,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.LiveActivity -> {
                    LiveActivityCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Navigation -> {
                    NavigationCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.DownloadUpload -> {
                    DownloadUploadCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Hotspot -> {
                    HotspotCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Bluetooth -> {
                    BluetoothCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Flashlight -> {
                    Text(
                        text = "ON",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.ScreenRecording -> {
                    val time = notification?.timeMillis ?: System.currentTimeMillis()
                    CallTimer(postTimeMillis = time, color = Color(0xFFEF4444))
                }
                IslandMode.Timer -> {
                    TimerCountdown(notification = notification, color = Color(settings.timerColor))
                }
                IslandMode.Stopwatch -> {
                    StopwatchTimer(notification = notification, color = Color(settings.stopwatchColor))
                }
                IslandMode.Empty -> Unit
            }
        }
    }
}

@Composable
internal fun HotspotCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val hotspotColor = Color(settings.hotspotColor)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(hotspotColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiTethering,
            contentDescription = "Hotspot",
            tint = hotspotColor,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun HotspotCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings) {
    val countText = remember(notification?.text, notification?.title) {
        HotspotUtil.parseDeviceCount(notification?.title, notification?.text).toString()
    }

    Text(
        text = countText,
        color = Color(settings.hotspotColor),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun BatteryCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings) {
    val pctText = notification?.text?.replace("%", "")?.trim() ?: "49"
    val pct = pctText.toFloatOrNull() ?: 49f
    val progress = (pct / 100f).coerceIn(0f, 1f)
    val title = notification?.title?.lowercase() ?: ""
    val isBatterySaver = title.contains("saver") || notification?.category == "battery_saver"
    val isLowBattery = title.contains("low") || notification?.category == "battery_low" || pct <= 20f

    val batteryColor = when {
        isLowBattery -> Color(0xFFEF4444)
        isBatterySaver -> Color(0xFFF59E0B)
        else -> Color(settings.batteryColor)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "batteryPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "batteryScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dottedRingRotation"
    )

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        DottedRing(
            progress = progress,
            rotationAngle = rotationAngle,
            modifier = Modifier.size(22.dp),
            color = batteryColor
        )
        when {
            isBatterySaver -> {
                Icon(
                    Icons.Rounded.BatterySaver,
                    contentDescription = "Battery Saver",
                    tint = batteryColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            isLowBattery -> {
                Icon(
                    Icons.Rounded.BatteryAlert,
                    contentDescription = "Low Battery",
                    tint = batteryColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                )
            }
            else -> {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = "Charging",
                    tint = batteryColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                )
            }
        }
    }
}

@Composable
internal fun NotificationGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val largeIcon = notification?.largeIcon
    val icon = notification?.icon
    val mainIcon = largeIcon ?: icon
    if (mainIcon != null) {
        Box(modifier = Modifier.size(22.dp)) {
            Image(
                bitmap = mainIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (largeIcon != null && icon != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.Black, CircleShape)
                        .padding(1.dp)
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
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(settings.notificationDotColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification?.appName?.firstOrNull()?.uppercase() ?: "S",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_visualizer")
        val heights = listOf(0.3f to 0.9f, 0.5f to 1.0f, 0.2f to 0.7f)

        heights.forEachIndexed { index, (min, max) ->
            val heightFraction by infiniteTransition.animateFloat(
                initialValue = min,
                targetValue = if (isPlaying) max else min,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + index * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .graphicsLayer {
                        scaleY = if (isPlaying) heightFraction else min
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}


@Composable
internal fun CallTimer(postTimeMillis: Long, color: Color) {
    var elapsedSeconds by remember(postTimeMillis) {
        mutableStateOf(((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L))
    }
    LaunchedEffect(postTimeMillis) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L)
            kotlinx.coroutines.delay(1000)
        }
    }
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    Text(
        text = "%02d:%02d".format(minutes, seconds),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun LiveActivityCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val brandColor = remember(notification?.packageName, settings.liveActivityColor) {
        if (notification != null) {
            Color(com.agupta07505.smartisland.util.LiveActivityParser.getBrandColor(notification.packageName))
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
            strokeWidthDp = 4.8f,
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

@Composable
private fun SmoothCircularRingProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 4.5f
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = strokeWidthDp.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeftOffset = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

        // Track background ring
        drawArc(
            color = Color(0x4DFFFFFF),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeftOffset,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Filled progress arc (360 degrees for completed 1.0f)
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
private fun LiveActivityCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val brandColor = remember(notification?.packageName, settings.liveActivityColor) {
        if (notification != null) {
            Color(com.agupta07505.smartisland.util.LiveActivityParser.getBrandColor(notification.packageName))
        } else {
            Color(settings.liveActivityColor)
        }
    }
    val etaText = remember(notification) {
        if (notification == null) return@remember "Active"
        val text = "${notification.title} ${notification.text}"
        val matcher = java.util.regex.Pattern.compile("(\\d+)\\s*(?:mins?|minutes?|min|m)\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text)
        if (matcher.find()) {
            "${matcher.group(1)} min"
        } else if (text.lowercase().contains("arrived")) {
            "Arrived"
        } else {
            "Active"
        }
    }

    Text(
        text = etaText,
        color = brandColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun NavigationCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val navColor = Color(settings.liveActivityColor)
    val turnDirection = remember(notification) {
        val title = notification?.title.orEmpty()
        val text = notification?.text.orEmpty()
        com.agupta07505.smartisland.util.NavigationParser.parseTurnDirection("$title $text".lowercase())
    }

    val angle = when (turnDirection) {
        com.agupta07505.smartisland.util.TurnDirection.LEFT -> -90f
        com.agupta07505.smartisland.util.TurnDirection.RIGHT -> 90f
        com.agupta07505.smartisland.util.TurnDirection.SLIGHT_LEFT -> -45f
        com.agupta07505.smartisland.util.TurnDirection.SLIGHT_RIGHT -> 45f
        com.agupta07505.smartisland.util.TurnDirection.U_TURN -> 180f
        else -> 0f
    }

    if (turnDirection == com.agupta07505.smartisland.util.TurnDirection.DESTINATION) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = "Destination",
            tint = navColor,
            modifier = Modifier.size(18.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Navigation,
            contentDescription = "Turn direction",
            tint = navColor,
            modifier = Modifier
                .size(18.dp)
                .rotate(angle)
        )
    }
}

@Composable
private fun NavigationCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val distanceText = remember(notification) {
        if (notification == null) return@remember "200 m"
        val title = notification.title
        val text = notification.text
        val pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher("$title $text")
        if (matcher.find()) matcher.group(0) else "In 200 m"
    }

    Text(
        text = distanceText,
        color = Color(settings.liveActivityColor),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun CustomPremiumTransferIcon(
    isUpload: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    motionProgress: Float = 0.5f,
    alphaFraction: Float = 1f
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
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

        // 1. Main Arrow Shaft
        drawLine(
            color = drawColor,
            start = androidx.compose.ui.geometry.Offset(cx, tailY),
            end = androidx.compose.ui.geometry.Offset(cx, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2. Main Arrow Head Wings
        val wingY = if (isUpload) tipY + arrowHeadHeight else tipY - arrowHeadHeight
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - arrowHeadWidth, wingY)
            lineTo(cx, tipY)
            lineTo(cx + arrowHeadWidth, wingY)
        }
        drawPath(
            path = path,
            color = drawColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        // 3. Subtle Trailing Particle Dot
        val particleOffsetY = if (isUpload) 4.5.dp.toPx() else -4.5.dp.toPx()
        val particleY = tailY + particleOffsetY
        val particleAlpha = (drawAlpha * 0.5f).coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = particleAlpha),
            radius = strokeWidth * 0.6f,
            center = androidx.compose.ui.geometry.Offset(cx, particleY)
        )
    }
}

@Composable
private fun DownloadUploadCollapsedRight(
    notification: IslandNotification?,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val textCombined = remember(notification) {
        "${notification?.title} ${notification?.text}".lowercase()
    }
    val uploadKeywords = listOf("upload", "uploading", "sending", "posting", "exporting", "backing up", "backup")
    val isUpload = remember(textCombined) { uploadKeywords.any { textCombined.contains(it) } }
    val accentColor = Color(settings.transferColor)

    val progress = remember(notification) {
        if (notification != null && notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0.1f, 1f)
        } else {
            0.55f
        }
    }

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
            progress = progress,
            color = accentColor,
            strokeWidthDp = 2.5f,
            modifier = Modifier.size(19.dp)
        )

        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
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

@Composable
internal fun ScreenRecordingCollapsedGlyph(settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val infiniteTransition = rememberInfiniteTransition(label = "collapsedRecordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val recordingColor = Color(settings.screenRecordingColor)

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(recordingColor.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer { alpha = pulseAlpha }
                .clip(CircleShape)
                .background(recordingColor)
        )
    }
}

@Composable
internal fun TimerCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val timerColor = Color(settings.timerColor)
    val isPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val resumeKeywords = listOf("resume", "start", "play", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
        val pausedKeywords = listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
        val actions = notification?.actionIntents.orEmpty()
        actions.any { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timerPulseScale"
    )

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(timerColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.HourglassBottom,
            contentDescription = "Timer",
            tint = timerColor,
            modifier = Modifier
                .size(13.dp)
                .graphicsLayer {
                    if (!isPaused) {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                }
        )
    }
}

@Composable
internal fun StopwatchCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val stopwatchColor = Color(settings.stopwatchColor)
    val infiniteTransition = rememberInfiniteTransition(label = "stopwatchRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stopwatchRot"
    )

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(stopwatchColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AvTimer,
            contentDescription = "Stopwatch",
            tint = stopwatchColor,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
internal fun TimerCountdown(notification: IslandNotification?, color: Color) {
    val isPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val resumeKeywords = listOf("resume", "start", "play", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
        val pausedKeywords = listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
        val actions = notification?.actionIntents.orEmpty()
        actions.any { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }

    val targetTime = notification?.timeMillis ?: remember { System.currentTimeMillis() + 300000L }

    var remainingSec by remember(notification?.key) {
        val parsed = notification?.let { TimerStopwatchParser.parseTimerRemainingSeconds(it) }
        val rem = if (parsed != null && parsed > 0) {
            parsed
        } else if (targetTime > System.currentTimeMillis()) {
            ((targetTime - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        mutableStateOf(rem)
    }

    LaunchedEffect(notification?.text, notification?.title) {
        if (isPaused && notification != null) {
            val parsed = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
            if (parsed != null && parsed > 0) {
                remainingSec = parsed
            }
        }
    }

    LaunchedEffect(notification?.key, targetTime, isPaused) {
        if (!isPaused) {
            while (true) {
                val now = System.currentTimeMillis()
                val rem = if (targetTime > now) {
                    ((targetTime - now + 500L) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                remainingSec = rem
                if (rem <= 0L) break
                kotlinx.coroutines.delay(500L)
            }
        }
    }

    val text = remember(remainingSec) {
        TimerStopwatchParser.formatTime(remainingSec)
    }

    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun StopwatchTimer(notification: IslandNotification?, color: Color) {
    val startTime = notification?.timeMillis ?: remember { System.currentTimeMillis() }
    val isPaused = remember(notification?.key, notification?.actionIntents) {
        notification?.actionIntents?.any {
            it.title.contains("resume", ignoreCase = true) || it.title.contains("start", ignoreCase = true)
        } == true
    }

    var elapsedSeconds by remember(notification?.key, startTime) {
        mutableStateOf(((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L))
    }

    LaunchedEffect(notification?.key, startTime, isPaused) {
        if (!isPaused) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L)
                kotlinx.coroutines.delay(500L)
            }
        }
    }

    val text = remember(elapsedSeconds) {
        com.agupta07505.smartisland.util.TimerStopwatchParser.formatTime(elapsedSeconds)
    }

    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun BluetoothCollapsedRight(
    notification: IslandNotification?,
    settings: SmartIslandSettings
) {
    val batteryLevel = remember(notification?.text, notification?.progress) {
        notification?.let { notif ->
            if (notif.progress in 1..100 && notif.progressMax == 100) notif.progress
            else {
                val match = Regex("""(\d{1,3})%""").find(notif.text)
                match?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)
            }
        }
    }

    if (!settings.showBluetoothBattery || batteryLevel == null) {
        Image(
            painter = painterResource(id = com.agupta07505.smartisland.R.drawable.ic_bluetooth_device),
            contentDescription = "Bluetooth Device",
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
        )
    } else {
        var showBattery by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(3000L)
                showBattery = !showBattery
            }
        }

        AnimatedContent(
            targetState = showBattery,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)) +
                        scaleIn(initialScale = 0.72f, animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)) +
                                scaleOut(targetScale = 0.72f, animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f))
                    )
            },
            label = "BluetoothCollapsedSwitch"
        ) { isBatteryState ->
            if (isBatteryState) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val batteryColor = if (batteryLevel <= 20) Color(0xFFEF4444) else Color(0xFF10B981)
                    val batteryIcon = when {
                        batteryLevel >= 80 -> Icons.Rounded.BatteryFull
                        batteryLevel >= 40 -> Icons.Rounded.Battery5Bar
                        batteryLevel >= 20 -> Icons.Rounded.BatteryChargingFull
                        else -> Icons.Rounded.BatteryAlert
                    }
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = "Battery",
                        tint = batteryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$batteryLevel%",
                        color = batteryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.bluetoothColor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = "Earbuds",
                        tint = Color(settings.bluetoothColor),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// Collapsed content animation
private val COLLAPSED_TRANSLATION_MAX_DP = 32.dp
private const val LEFT_SLOT_PADDING_START_DP = 8
private const val RIGHT_SLOT_PADDING_END_DP = 12
private const val CAMERA_KEEPOUT_MARGIN_DP = 3f
