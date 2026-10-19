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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Snooze
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

private val AlarmColor = IslandPalette.warning

/** The bell's pulse (scale), 1 while animations are paused (screen off) or system animations are off. */
@Composable
private fun rememberBellPulse(): Float {
    if (!rememberIslandAnimationsActive()) return 1f
    val transition = rememberInfiniteTransition(label = "alarmBell")
    val scale by transition.animateFloatCalm(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alarmBellScale"
    )
    return scale
}

/** Small state (pill left / companion bubble): the pulsing alarm bell (Rule_Book §4.10). */
@Composable
fun AlarmCollapsedGlyph(modifier: Modifier = Modifier) {
    val pulse = rememberBellPulse()
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(AlarmColor.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.NotificationsActive,
            contentDescription = stringResource(R.string.island_alarm_ringing),
            tint = AlarmColor,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
        )
    }
}

/** The current system time in the user's 12/24-hour format, ticking while the screen is on. */
@Composable
fun rememberSystemTimeText(): String {
    val context = LocalContext.current
    val active = rememberIslandAnimationsActive()
    val format = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        while (active) {
            now = System.currentTimeMillis()
            // Wake on the next minute boundary (plus a beat), not every second.
            kotlinx.coroutines.delay(60_000L - now % 60_000L + 50L)
        }
    }
    return remember(now, format) { format.format(java.util.Date(now)) }
}

/** Expanded small state, right: the current system time. */
@Composable
fun AlarmCollapsedRight() {
    Text(
        text = rememberSystemTimeText(),
        color = AlarmColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

/**
 * Medium state (340dp): alarm icon, "Alarm Ringing", the current time, and Snooze (gray) / Stop (red). Each
 * button fires the clock app's own action (matched by its label) and takes the alert off the island; a button
 * the notification does not offer is not shown. Both work from the lockscreen (Rule_Book §8.3).
 */
@Composable
fun AlarmExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val pulse = rememberBellPulse()
    val snooze = notification.actionFor(IslandControl.AlarmSnooze)
    val stop = notification.actionFor(IslandControl.AlarmStop)
    val label = notification?.let { n ->
        listOf(n.title, n.text).firstOrNull { it.isNotBlank() && !it.equals("alarm", ignoreCase = true) }
    }
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
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AlarmColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = AlarmColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.island_alarm_ringing),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (label != null) {
                    Text(
                        text = label,
                        color = IslandPalette.secondaryLabel,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = rememberSystemTimeText(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        if (notification != null && (snooze != null || stop != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (snooze != null) {
                    IslandPillButton(
                        label = stringResource(R.string.island_alarm_snooze),
                        icon = Icons.Rounded.Snooze,
                        background = IslandPalette.fillStrong,
                        modifier = Modifier.weight(1f)
                    ) {
                        fireIslandControl(context, notification, snooze, GatedAction.AlarmControl, removeAlert = true, onDone = onCollapse)
                    }
                }
                if (stop != null) {
                    IslandPillButton(
                        label = stringResource(R.string.island_alarm_stop),
                        icon = Icons.Rounded.AlarmOff,
                        background = IslandPalette.danger,
                        modifier = Modifier.weight(1f)
                    ) {
                        fireIslandControl(context, notification, stop, GatedAction.AlarmControl, removeAlert = true, onDone = onCollapse)
                    }
                }
            }
        }
    }
}
