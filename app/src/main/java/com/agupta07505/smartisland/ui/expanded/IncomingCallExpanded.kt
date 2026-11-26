/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CallActionRole
import com.agupta07505.smartisland.model.CallButtonPick
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.VoipCallDetector
import com.agupta07505.smartisland.ui.CallTimer
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.CallHangUps
import com.agupta07505.smartisland.ui.IslandText as Text

private data class CallControl(
    val action: IslandNotificationAction,
    val role: CallActionRole,
    /** Mute is on / speaker is on, judged from the button's own label. */
    val active: Boolean
)

private fun callControls(notification: IslandNotification?): List<CallControl> {
    if (notification == null) return emptyList()
    val real = notification.actionIntents.filter { it.pendingIntent != null }
    fun first(role: CallActionRole) = real.firstOrNull { VoipCallDetector.classifyCallAction(it.title) == role }
    val controls = mutableListOf<CallControl>()
    val unmute = first(CallActionRole.Unmute)
    val mute = first(CallActionRole.Mute)
    if (unmute != null) {
        controls.add(CallControl(unmute, CallActionRole.Mute, active = true))
    } else if (mute != null) {
        controls.add(CallControl(mute, CallActionRole.Mute, active = false))
    }
    first(CallActionRole.Speaker)?.let {
        controls.add(CallControl(it, CallActionRole.Speaker, VoipCallDetector.isSpeakerOnLabel(it.title)))
    }
    // dialers with 3+ buttons ("Decline with message", "Answer with voicemail"): pick the plain Decline and Answer
    val titles = real.map { it.title }
    CallButtonPick.pick(titles, CallActionRole.HangUp)?.let { controls.add(CallControl(real[it], CallActionRole.HangUp, false)) }
    CallButtonPick.pick(titles, CallActionRole.Answer)?.let { controls.add(CallControl(real[it], CallActionRole.Answer, false)) }
    return controls
}

@Composable
fun IncomingCallExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val isRinging = notification?.isCallRinging == true
    // only the call notification's own buttons, nothing is made up, so WhatsApp gets no mute / speaker button
    val controls = remember(notification?.key, notification?.actionIntents) { callControls(notification) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(min = 64.dp, max = 110.dp)
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification?.title?.takeIf { it.isNotBlank() }
                    ?: notification?.text?.takeIf { it.isNotBlank() }
                    ?: if (isRinging) "Incoming call" else "Active call",
                color = LocalIslandPalette.current.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (notification?.isCallConnecting == true) {
                // Dialing: nothing to time yet, show the app's own words ("Calling...", "Ringing...")
                Text(
                    text = notification.callStatusText,
                    color = Color(settings.callColor),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (!isRinging) {
                val time = notification?.timeMillis ?: System.currentTimeMillis()
                CallTimer(
                    postTimeMillis = time,
                    color = Color(settings.callColor)
                )
            }
        }

        controls.forEach { CallControlButton(it, isRinging, notification, settings, onCollapse) }
    }
}

@Composable
private fun CallControlButton(
    control: CallControl,
    isRinging: Boolean,
    notification: IslandNotification?,
    settings: SmartIslandSettings,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val (background, tint, icon) = when (control.role) {
        CallActionRole.HangUp -> Triple(Color(0xFFE11D48), Color.White, Icons.Rounded.CallEnd)
        CallActionRole.Answer -> Triple(Color(settings.callColor), Color.White, Icons.Rounded.Call)
        CallActionRole.Mute -> toggleStyle(control.active, Icons.Rounded.MicOff, Icons.Rounded.Mic)
        else -> toggleStyle(control.active, Icons.Rounded.VolumeUp, Icons.Rounded.VolumeUp)
    }
    CircleActionButton(
        color = background,
        tint = tint,
        icon = icon,
        description = control.action.title,
        onClick = {
            if (isRinging) {
                // Ringing card is island-only, the app will not post a removal for it
                if (notification?.sendSelectedAction(context, control.action) != true) return@CircleActionButton
            } else {
                // An ongoing call's notification stays in the shade; the card follows it
                if (control.role == CallActionRole.HangUp) notification?.let { CallHangUps.mark(it.key) }
                notification?.fireAction(context, control.action)
            }
            if (control.role == CallActionRole.HangUp || control.role == CallActionRole.Answer) {
                onCollapse()
            }
        }
    )
}

@Composable
private fun toggleStyle(active: Boolean, activeIcon: ImageVector,
        idleIcon: ImageVector): Triple<Color, Color, ImageVector> {
    val palette = LocalIslandPalette.current
    // active is a solid disc with the opposite colour glyph, idle a soft disc with the text colour
    return if (active) Triple(palette.inverseFill, palette.onInverse, activeIcon)
    else Triple(palette.tint(0.2f), palette.text, idleIcon)
}

@Composable
private fun CircleActionButton(
    color: Color,
    tint: Color,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(23.dp))
    }
}
