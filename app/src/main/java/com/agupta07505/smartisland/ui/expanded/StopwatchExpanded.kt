/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.TimerStopwatchParser

@Composable
fun StopwatchExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val stopwatchColor = Color(settings.stopwatchColor)

    val isNotificationPaused = remember(notification?.key, notification?.actionIntents, notification?.text) {
        val actions = notification?.actionIntents.orEmpty()
        actions.any { it.title.contains("resume", ignoreCase = true) || it.title.contains("start", ignoreCase = true) } ||
            notification?.text?.contains("pause", ignoreCase = true) == true
    }

    var isPaused by remember(notification?.key, isNotificationPaused) {
        mutableStateOf(isNotificationPaused)
    }

    var startTime by remember(notification?.key, notification?.timeMillis) {
        mutableStateOf(notification?.timeMillis ?: System.currentTimeMillis())
    }

    var elapsedSeconds by remember(notification?.key, startTime, isPaused) {
        mutableStateOf(((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L))
    }

    var lapCount by remember(notification?.key) { mutableStateOf(1) }

    LaunchedEffect(notification?.key, startTime, isPaused) {
        if (!isPaused) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L)
                kotlinx.coroutines.delay(500L)
            }
        }
    }

    val displayTime = remember(elapsedSeconds) {
        TimerStopwatchParser.formatTime(elapsedSeconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stopwatchAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stopwatchRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotification() }
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Stopwatch Glyph & Time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(stopwatchColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AvTimer,
                    contentDescription = "Stopwatch",
                    tint = stopwatchColor,
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (!isPaused) Modifier.rotate(rotation) else Modifier)
                )
            }

            Column {
                Text(
                    text = displayTime,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                val notifText = notification?.text.orEmpty()
                val notifTitle = notification?.title.orEmpty()
                val subtitle = if (isPaused) {
                    "Paused"
                } else if (lapCount > 1) {
                    "Lap $lapCount"
                } else if (notifText.isNotBlank() && !notifText.equals("Stopwatch", ignoreCase = true) && !notifText.contains("00:")) {
                    notifText
                } else if (notifTitle.isNotBlank() && !notifTitle.equals("Stopwatch", ignoreCase = true) && !notifTitle.contains("00:")) {
                    notifTitle
                } else {
                    "Stopwatch Active"
                }
                Text(
                    text = subtitle,
                    color = stopwatchColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: Interactive Quick Actions (Lap, Pause/Resume, Reset)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Lap Button
            val lapAction = notification?.actionIntents?.firstOrNull {
                it.title.contains("lap", ignoreCase = true) || it.title.contains("split", ignoreCase = true)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27272A))
                    .bounceClick {
                        if (lapAction?.pendingIntent != null && notification != null) {
                            triggerAction(context, notification.packageName, lapAction.pendingIntent, lapAction.title, notification.contentIntent)
                        }
                        lapCount++
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Flag,
                    contentDescription = "Lap",
                    tint = Color(0xFFE4E4E7),
                    modifier = Modifier.size(16.dp)
                )
            }

            // 2. Pause / Resume Button
            val pauseAction = if (isPaused) {
                notification?.actionIntents?.firstOrNull {
                    it.title.contains("resume", ignoreCase = true) || it.title.contains("start", ignoreCase = true) || it.title.contains("play", ignoreCase = true) || it.title.contains("unpause", ignoreCase = true)
                } ?: notification?.actionIntents?.firstOrNull()
            } else {
                notification?.actionIntents?.firstOrNull {
                    it.title.contains("pause", ignoreCase = true)
                } ?: notification?.actionIntents?.firstOrNull()
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(stopwatchColor)
                    .bounceClick {
                        val newPaused = !isPaused
                        isPaused = newPaused
                        if (!newPaused) {
                            startTime = System.currentTimeMillis() - elapsedSeconds * 1000L
                        }

                        if (pauseAction?.pendingIntent != null && notification != null) {
                            triggerAction(context, notification.packageName, pauseAction.pendingIntent, pauseAction.title, notification.contentIntent)
                        } else if (notification != null) {
                            val repo = SmartIslandRepositories.notificationRepository(context)
                            val updatedActions = if (newPaused) {
                                listOf(
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Lap", null),
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Resume", null),
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Reset", null)
                                )
                            } else {
                                listOf(
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Lap", null),
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Pause", null),
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Reset", null)
                                )
                            }
                            repo.postNotification(
                                notification.copy(
                                    title = if (newPaused) "Stopwatch (Paused)" else "Stopwatch",
                                    text = TimerStopwatchParser.formatTime(elapsedSeconds),
                                    timeMillis = System.currentTimeMillis() - elapsedSeconds * 1000L,
                                    actionIntents = updatedActions
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 3. Reset / Stop Button
            val resetAction = notification?.actionIntents?.firstOrNull {
                val t = it.title.lowercase()
                t.contains("reset") || t.contains("stop") || t.contains("clear") || t.contains("cancel")
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3F3F46))
                    .bounceClick {
                        if (resetAction?.pendingIntent != null && notification != null) {
                            triggerAction(context, notification.packageName, resetAction.pendingIntent, resetAction.title, notification.contentIntent)
                        }
                        val repository = SmartIslandRepositories.notificationRepository(context)
                        notification?.key?.let { repository.removeNotification(it) }
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reset",
                    tint = Color(0xFFE4E4E7),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
