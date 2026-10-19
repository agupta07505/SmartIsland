/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.IslandPalette
import com.agupta07505.smartisland.ui.animateFloatCalm
import com.agupta07505.smartisland.ui.rememberIslandAnimationsActive
import com.agupta07505.smartisland.util.GatedAction
import com.agupta07505.smartisland.util.IslandControl
import com.agupta07505.smartisland.util.NotificationActionMatcher
import com.agupta07505.smartisland.util.TimerStopwatchParser
import com.agupta07505.smartisland.util.VoiceRecordingClock

private val RecordingRed = IslandPalette.danger

private fun IslandNotification?.isRecorderPaused(): Boolean =
    this != null && NotificationActionMatcher.isRecorderPaused(actionIntents, title, text)

/**
 * The breathing alpha (0.3 to 1.0) of the red recording dot (Rule_Book §4.3). It rests at 1 while paused, while
 * the screen is off (the lifecycle gate, so no frame is drawn for it) and with system animations off.
 */
@Composable
private fun rememberBreathingAlpha(breathing: Boolean): Float {
    if (!breathing || !rememberIslandAnimationsActive()) return 1f
    val transition = rememberInfiniteTransition(label = "voiceRecordingBreath")
    val alpha by transition.animateFloatCalm(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "voiceRecordingAlpha"
    )
    return alpha
}

/** The red recording dot, breathing while it records. Used for the pill's left slot and the companion bubble. */
@Composable
fun VoiceRecordingDot(notification: IslandNotification?, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val alpha = rememberBreathingAlpha(breathing = !notification.isRecorderPaused())
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(RecordingRed.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { this.alpha = alpha }
                .clip(CircleShape)
                .background(RecordingRed)
        )
    }
}

/** Seconds recorded so far; stands still while paused and only ticks while the screen is on. */
@Composable
fun rememberVoiceRecordingSeconds(notification: IslandNotification?): Long {
    val startMs = notification?.timeMillis ?: 0L
    val paused = notification.isRecorderPaused()
    val active = rememberIslandAnimationsActive()
    val shownSeconds = remember(notification?.title, notification?.text) {
        notification?.let { n ->
            listOf(n.text, n.title).firstNotNullOfOrNull { field ->
                field.takeIf { it.isNotBlank() }?.let(TimerStopwatchParser::parseTimeStringToSeconds)
            }
        }
    }
    val frozen = remember(notification?.key) { LongArray(1) { -1L } }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startMs, paused, active) {
        while (!paused && active) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(500L)
        }
    }
    val seconds = VoiceRecordingClock.displaySeconds(
        nowMs = now,
        startMs = startMs,
        paused = paused,
        shownElapsedSeconds = shownSeconds,
        frozenSeconds = frozen[0].takeIf { it >= 0 }
    )
    if (!paused) frozen[0] = seconds
    return seconds
}

/** Expanded small state, right: the live MM:SS duration. */
@Composable
fun VoiceRecordingCollapsedRight(notification: IslandNotification?) {
    Text(
        text = VoiceRecordingClock.format(rememberVoiceRecordingSeconds(notification)),
        color = RecordingRed,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

/**
 * Medium state (340dp): the duration with Pause/Resume and Stop/Save, each firing the recorder's own action. A
 * button the recorder's notification does not offer is not shown.
 */
@Composable
fun VoiceRecordingExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val paused = notification.isRecorderPaused()
    val seconds = rememberVoiceRecordingSeconds(notification)
    val pause = notification.actionFor(IslandControl.RecorderPause)
    val resume = notification.actionFor(IslandControl.RecorderResume)
    val stop = notification.actionFor(IslandControl.RecorderStop)
    val toggle = if (paused) resume ?: pause else pause ?: resume
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoiceRecordingDot(notification, size = 12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification?.appName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.island_voice_title),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(if (paused) R.string.island_voice_paused else R.string.island_voice_recording),
                    color = IslandPalette.secondaryLabel,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Text(
                text = VoiceRecordingClock.format(seconds),
                color = if (paused) IslandPalette.secondaryLabel else RecordingRed,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        if (notification != null && (toggle != null || stop != null)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (toggle != null) {
                    val resumes = toggle === resume && toggle !== pause
                    IslandPillButton(
                        label = stringResource(if (resumes) R.string.island_voice_resume else R.string.island_voice_pause),
                        icon = if (resumes) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        background = IslandPalette.fillStrong,
                        modifier = Modifier.weight(1f)
                    ) {
                        fireIslandControl(context, notification, toggle, GatedAction.RecorderControl, removeAlert = false)
                    }
                }
                if (stop != null) {
                    val saves = NotificationActionMatcher.hasWord(stop.title.lowercase(), "save")
                    IslandPillButton(
                        label = stringResource(if (saves) R.string.island_voice_save else R.string.island_voice_stop),
                        icon = if (saves) Icons.Rounded.Save else Icons.Rounded.Stop,
                        background = RecordingRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        fireIslandControl(context, notification, stop, GatedAction.RecorderControl, removeAlert = true, onDone = onCollapse)
                    }
                }
            }
        }
    }
}
