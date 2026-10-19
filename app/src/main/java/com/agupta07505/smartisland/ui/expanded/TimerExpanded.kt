/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.ui.animateFloatCalm
import com.agupta07505.smartisland.R
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.ui.IslandPalette
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

    val resumeKeywords = remember {
        listOf("resume", "start", "play", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
    }
    val pauseKeywords = remember {
        listOf("pause", "pausa", "pausar", "sospendi", "interrompi", "onderbreek", "stoppa", "रोकें", "暂停", "一時停止", "пауза")
    }
    val pausedKeywords = remember {
        listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
    }
    val destructiveKeywords = remember {
        listOf("stop", "reset", "cancel", "delete", "dismiss", "clear", "annuler", "abbrechen", "eliminar", "borrar")
    }

    val isNotificationPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val actions = notification?.actionIntents.orEmpty()
        actions.any { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }

    var isPaused by remember(notification?.key, isNotificationPaused) {
        mutableStateOf(isNotificationPaused)
    }

    var targetTime by remember(notification?.key, notification?.timeMillis) {
        mutableStateOf(notification?.timeMillis ?: (System.currentTimeMillis() + 300000L))
    }

    var remainingSec by remember(notification?.key) {
        val parsed = notification?.let { TimerStopwatchParser.parseTimerRemainingSeconds(it) }
        val rem = if (parsed != null && parsed > 0) {
            parsed
        } else {
            val t = notification?.timeMillis ?: (System.currentTimeMillis() + 300000L)
            if (t > System.currentTimeMillis()) {
                ((t - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
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

    val screenOn = com.agupta07505.smartisland.ui.LocalScreenOn.current
    LaunchedEffect(notification?.key, targetTime, isPaused, screenOn) {
        if (!isPaused && screenOn) {
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
    val pulseScale by infiniteTransition.animateFloatCalm(
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
                    contentDescription = stringResource(R.string.a11y_timer),
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
                    color = if (remainingSec == 0L && !isPaused) IslandPalette.danger else timerColor,
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
                notification?.actionIntents?.firstOrNull { act ->
                    val t = act.title.lowercase()
                    resumeKeywords.any { t.contains(it) }
                } ?: notification?.actionIntents?.firstOrNull { act ->
                    val t = act.title.lowercase()
                    !destructiveKeywords.any { t.contains(it) } && !t.contains("+1") && !t.contains("add")
                }
            } else {
                notification?.actionIntents?.firstOrNull { act ->
                    val t = act.title.lowercase()
                    pauseKeywords.any { t.contains(it) }
                } ?: notification?.actionIntents?.firstOrNull { act ->
                    val t = act.title.lowercase()
                    !destructiveKeywords.any { t.contains(it) } && !t.contains("+1") && !t.contains("add")
                }
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
                        }
                        if (notification != null) {
                            val repo = SmartIslandRepositories.notificationRepository(context)
                            val updatedActions = notification.actionIntents.map { act ->
                                val t = act.title.lowercase()
                                if (newPaused && pauseKeywords.any { t.contains(it) }) {
                                    act.copy(title = "Resume")
                                } else if (!newPaused && resumeKeywords.any { t.contains(it) }) {
                                    act.copy(title = "Pause")
                                } else {
                                    act
                                }
                            }
                            repo.postNotification(
                                notification.copy(
                                    title = if (newPaused) "${notification.title.replace(" (Paused)", "")} (Paused)" else notification.title.replace(" (Paused)", ""),
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
            val stopAction = notification?.actionIntents?.firstOrNull { act ->
                val t = act.title.lowercase()
                destructiveKeywords.any { t.contains(it) }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IslandPalette.fill)
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
                    contentDescription = stringResource(R.string.a11y_stop),
                    tint = IslandPalette.danger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
