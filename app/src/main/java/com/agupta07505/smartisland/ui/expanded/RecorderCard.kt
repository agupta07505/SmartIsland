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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.formatRecordingTime
import com.agupta07505.smartisland.ui.rememberRecordingSeconds
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Shared expanded card for screen and audio recorders. */
@Composable
fun RecorderCard(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit,
    accent: Color,
    defaultTitle: String,
    statusLabel: String,
    leadingIcon: ImageVector?
) {
    val context = LocalContext.current
    val buttonSignature = notification?.actionIntents?.map { it.title }
    val actualPaused = notification?.recordingPaused == true || notification.offersResume()

    // The button press flips the card at once; the recorder's re-posted notification then confirms it
    // (the key below resets this guess as soon as the buttons change).
    var guessedPaused by remember(notification?.key, buttonSignature, notification?.recordingPaused) {
        mutableStateOf<Boolean?>(null)
    }
    val isPaused = guessedPaused ?: actualPaused

    // Frozen while paused: the clock comes from the recorder's own time, not from when we saw it.
    val timerText = formatRecordingTime(rememberRecordingSeconds(notification))

    // No pulse runs while the card waits collapsed (pre-composed): the transition only exists while the card is shown.
    val cardShown = com.agupta07505.smartisland.ui.LocalCardShown.current
    var pulseAlpha = 0.95f
    var pulseScale = 1f
    if (cardShown) {
        val pulse = rememberInfiniteTransition(label = "recorderPulse")
        val alphaAnim by pulse.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseAlpha"
        )
        val scaleAnim by pulse.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseScale"
        )
        pulseAlpha = alphaAnim
        pulseScale = scaleAnim
    }
    val animate = !isPaused

    val buttons = recorderButtons(notification, actualPaused)
    val toggleAction = buttons.toggle
    val stopAction = buttons.save

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .drawBehind { drawRect(accent.copy(alpha = (if (animate) pulseAlpha else 0.5f) * 0.3f)) },
                contentAlignment = Alignment.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                if (animate) {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                if (animate) {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                            }
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification?.title?.takeIf { it.isNotBlank() } ?: defaultTitle,
                    color = LocalIslandPalette.current.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPaused) "Paused • " else "$statusLabel • ",
                        color = LocalIslandPalette.current.textMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = timerText,
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (toggleAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LocalIslandPalette.current.pick(Color(0xFF27272A), Color(0xFFD9D8DB)))
                        .bounceClick {
                            guessedPaused = !isPaused
                            notification.fireAction(context, toggleAction)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = toggleAction.title.ifBlank { if (isPaused) "Resume" else "Pause" },
                        tint = LocalIslandPalette.current.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (stopAction != null && notification != null) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(accent)
                        .bounceClick {
                            notification.fireActionSilently(context, stopAction)
                            SmartIslandRepositories.notificationRepository(context).removeNotification(notification.key)
                            onCollapse()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (recorderSaveLabel(stopAction.title) == "Save") Icons.Rounded.Check else Icons.Rounded.Stop,
                            contentDescription = stopAction.title,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = recorderSaveLabel(stopAction.title),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

internal data class RecorderButtons(
    val toggle: IslandNotificationAction?,
    val save: IslandNotificationAction?
)

/**
 * Picks the recorder's real buttons: labels first (Pause, Resume, Save, Stop ...); when the wording is not
 * recognised, by position - the first button toggles pause / resume and the last saves / stops (how Google Recorder
 * and most OEM recorders lay them out).
 */
internal fun recorderButtons(notification: IslandNotification?, paused: Boolean): RecorderButtons {
    val real = notification?.actionIntents.orEmpty().filter { it.pendingIntent != null }
    if (real.isEmpty()) return RecorderButtons(null, null)

    val toggleRoles = if (paused) arrayOf(ActionRole.Resume) else arrayOf(ActionRole.Pause)
    var toggle = real.firstOrNull { classifyAction(it.title) in toggleRoles }
    var save = real.firstOrNull { it !== toggle && classifyAction(it.title) in arrayOf(ActionRole.Stop, ActionRole.Reset) }

    if (toggle == null) {
        // Unknown wording: the toggle is the first button that is not the save / stop one.
        toggle = real.firstOrNull { it !== save && classifyAction(it.title) != ActionRole.Stop }
    }
    if (save == null) {
        save = real.lastOrNull { it !== toggle }
    }
    if (toggle != null && toggle === save) save = null
    return RecorderButtons(toggle, save)
}

/** "Save recording" -> "Save", "Stop" -> "Stop"; anything else keeps its own (short) label. */
internal fun recorderSaveLabel(title: String): String {
    val t = title.trim()
    val lower = t.lowercase()
    return when {
        t.isBlank() -> "Stop"
        lower.contains("save") -> "Save"
        lower.contains("stop") -> "Stop"
        t.length > 12 -> t.take(12).trimEnd() + "…"
        else -> t
    }
}
