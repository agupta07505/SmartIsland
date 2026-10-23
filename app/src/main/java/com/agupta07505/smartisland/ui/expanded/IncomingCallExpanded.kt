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
import com.agupta07505.smartisland.ui.IslandText as Text
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
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.CallActionRole
import com.agupta07505.smartisland.util.VoipCallDetector

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
    first(CallActionRole.HangUp)?.let { controls.add(CallControl(it, CallActionRole.HangUp, false)) }
    first(CallActionRole.Answer)?.let { controls.add(CallControl(it, CallActionRole.Answer, false)) }
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
    // Only the call notification's own buttons are shown. Nothing here is made up by the island,
    // so an app whose call notification has no mute / speaker button (WhatsApp) gets none.
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
                // Dialing: the other side has not picked up, so there is nothing to time yet.
                // The app's own words are shown ("Calling...", "Ringing...", "Dialing").
                Text(
                    text = notification.callStatusText,
                    color = Color(settings.callColor),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (!isRinging) {
                val time = notification?.timeMillis ?: System.currentTimeMillis()
                com.agupta07505.smartisland.ui.CallTimer(
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
                // The ringing card is island-only, so it is dropped here; the app will not post a removal for it.
                notification.sendFirstAction(context, control.action.title)
            } else {
                // An ongoing call's notification stays in the shade; the card follows it.
                notification?.fireAction(context, control.action)
            }
            if (control.role == CallActionRole.HangUp || control.role == CallActionRole.Answer) {
                onCollapse()
            }
        }
    )
}

@Composable
private fun toggleStyle(active: Boolean, activeIcon: ImageVector, idleIcon: ImageVector): Triple<Color, Color, ImageVector> {
    val palette = LocalIslandPalette.current
    // Active: a solid disc with the opposite colour glyph; idle: a soft disc with the text-coloured glyph.
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
