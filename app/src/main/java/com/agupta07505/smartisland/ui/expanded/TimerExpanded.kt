/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
fun TimerExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val timerColor = Color(settings.timerColor)

    val isNotificationPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val actions = notification?.actionIntents.orEmpty()
        actions.any {
            val t = it.title.lowercase()
            t.contains("resume") || t.contains("start") || t.contains("play") || t.contains("continue") || t.contains("unpause")
        } || notification?.text?.contains("pause", ignoreCase = true) == true ||
            notification?.title?.contains("pause", ignoreCase = true) == true
    }

    var isPaused by remember(notification?.key, isNotificationPaused) {
        mutableStateOf(isNotificationPaused)
    }

    var targetTime by remember(notification?.key, notification?.timeMillis) {
        mutableStateOf(notification?.timeMillis ?: (System.currentTimeMillis() + 300000L))
    }

    var remainingSec by remember(notification?.key, targetTime, isPaused) {
        val rem = if (targetTime > System.currentTimeMillis()) {
            ((targetTime - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        mutableStateOf(rem)
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

    val displayTime = remember(remainingSec) {
        TimerStopwatchParser.formatTime(remainingSec)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timerPulseScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotification() }
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hourglass / Timer Glyph
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(timerColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassBottom,
                    contentDescription = "Timer",
                    tint = timerColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            if (!isPaused && remainingSec > 0) {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        }
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
                val timerSubtitle = if (remainingSec == 0L && !isPaused) {
                    "Time's up!"
                } else if (isPaused) {
                    "Paused"
                } else {
                    val notifTitle = notification?.title.orEmpty()
                    val notifText = notification?.text.orEmpty()
                    if (notifTitle.isNotBlank() && !notifTitle.equals("Timer", ignoreCase = true) && !notifTitle.matches(Regex("""^[\d:.]+$"""))) {
                        notifTitle
                    } else if (notifText.isNotBlank() && !notifText.equals("Timer", ignoreCase = true) && !notifText.matches(Regex("""^[\d:.]+$"""))) {
                        notifText
                    } else {
                        "Timer Active"
                    }
                }
                Text(
                    text = timerSubtitle,
                    color = if (remainingSec == 0L && !isPaused) Color(0xFFEF4444) else timerColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: Interactive Quick Actions (Pause/Resume, Stop/Reset)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pause / Resume Button
            val pauseAction = if (isPaused) {
                notification?.actionIntents?.firstOrNull {
                    val t = it.title.lowercase()
                    t.contains("resume") || t.contains("start") || t.contains("play") || t.contains("continue") || t.contains("unpause")
                } ?: notification?.actionIntents?.firstOrNull()
            } else {
                notification?.actionIntents?.firstOrNull {
                    val t = it.title.lowercase()
                    t.contains("pause")
                } ?: notification?.actionIntents?.firstOrNull()
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(timerColor)
                    .bounceClick {
                        val newPaused = !isPaused
                        isPaused = newPaused
                        targetTime = System.currentTimeMillis() + remainingSec * 1000L

                        if (pauseAction?.pendingIntent != null && notification != null) {
                            triggerAction(context, notification.packageName, pauseAction.pendingIntent, pauseAction.title, notification.contentIntent)
                        } else if (notification != null) {
                            val repo = SmartIslandRepositories.notificationRepository(context)
                            val updatedActions = if (newPaused) {
                                listOf(
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Resume", null),
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Reset", null)
                                )
                            } else {
                                listOf(
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Pause", null),
                                    com.agupta07505.smartisland.model.IslandNotificationAction("Reset", null)
                                )
                            }
                            repo.postNotification(
                                notification.copy(
                                    title = if (newPaused) "Timer (Paused)" else "Timer",
                                    text = TimerStopwatchParser.formatTime(remainingSec),
                                    timeMillis = System.currentTimeMillis() + remainingSec * 1000L,
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
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. Reset / Stop / Cancel Button
            val stopAction = notification?.actionIntents?.firstOrNull {
                val t = it.title.lowercase()
                t.contains("stop") || t.contains("reset") || t.contains("cancel") || t.contains("delete") || t.contains("dismiss")
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27272A))
                    .bounceClick {
                        if (stopAction?.pendingIntent != null && notification != null) {
                            triggerAction(context, notification.packageName, stopAction.pendingIntent, stopAction.title, notification.contentIntent)
                        }
                        val repository = SmartIslandRepositories.notificationRepository(context)
                        notification?.key?.let { repository.removeNotification(it) }
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
