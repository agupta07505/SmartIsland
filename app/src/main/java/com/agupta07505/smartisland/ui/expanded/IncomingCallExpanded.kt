/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.ui.IslandPalette
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.CallTimer
import com.agupta07505.smartisland.ui.animateFloatCalm
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.rememberIslandAnimationsActive
import com.agupta07505.smartisland.util.GatedAction
import com.agupta07505.smartisland.util.IslandControl
import com.agupta07505.smartisland.util.OemDeviceRules

/** A WhatsApp / Meet / Telegram call (anything but the phone's own dialer) gets the app badge (Rule_Book §4.7). */
private fun IslandNotification.isVoipCall(): Boolean = !OemDeviceRules.isDialerCall(packageName)

/**
 * The caller's avatar; for a VoIP call the app's icon sits as a badge on its corner. Without a contact photo
 * the app icon (or a phone glyph) stands in. Used by the small pill, the companion bubble and the Medium card.
 */
@Composable
fun CallAvatar(
    notification: IslandNotification?,
    size: Dp,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val photo = notification?.largeIcon?.takeIf { !it.isRecycled }
    val appIcon = notification?.icon?.takeIf { !it.isRecycled }
    val main = photo ?: appIcon
    Box(modifier = Modifier.size(size)) {
        if (main != null) {
            val bitmap = remember(main) { main.asImageBitmap() }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(settings.callColor).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = Color(settings.callColor), modifier = Modifier.size(size * 0.55f))
            }
        }
        // The badge only adds information when the avatar is a contact photo, not the app icon itself.
        if (notification != null && photo != null && appIcon != null && notification.isVoipCall()) {
            val badge = remember(appIcon) { appIcon.asImageBitmap() }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.46f)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .padding(1.dp)
            ) {
                Image(
                    bitmap = badge,
                    contentDescription = notification.appName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
    }
}

/** Small state, right: a pulsing call icon while it rings, the live duration once answered. */
@Composable
fun CallCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings) {
    val color = Color(settings.callColor)
    if (notification?.isCallRinging == true) {
        val pulse = if (rememberIslandAnimationsActive()) {
            val transition = rememberInfiniteTransition(label = "callRing")
            val scale by transition.animateFloatCalm(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "callRingScale"
            )
            scale
        } else 1f
        Icon(
            Icons.Rounded.Call,
            contentDescription = stringResource(R.string.island_call_incoming),
            tint = color,
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    rotationZ = (pulse - 1f) * 60f
                }
        )
    } else {
        CallTimer(postTimeMillis = notification?.timeMillis ?: System.currentTimeMillis(), color = color)
    }
}

/**
 * Medium state (Rule_Book §4.7). Header: app icon and label. Body: avatar (badged for VoIP), caller and
 * "Incoming call…", Decline (red) and Accept (green). After Accept it becomes the Active Call card: the live
 * duration and Mute, Speaker and End Call, each shown only when the call's notification offers that action.
 * Every control works from the lockscreen without unlock (`GatedAction.CallControl`).
 */
@Composable
fun IncomingCallExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val isRinging = notification?.isCallRinging == true
    val callColor = Color(settings.callColor)
    val caller = notification?.title?.takeIf { it.isNotBlank() }
        ?: notification?.text?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.island_call_unknown_caller)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: the app making the call.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            val appIcon = notification?.icon?.takeIf { !it.isRecycled }
            if (appIcon != null) {
                val bitmap = remember(appIcon) { appIcon.asImageBitmap() }
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(16.dp).clip(CircleShape))
            }
            Text(
                text = notification?.appName.orEmpty(),
                color = IslandPalette.secondaryLabel,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallAvatar(notification = notification, size = 46.dp, settings = settings)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = caller,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isRinging) {
                    Text(
                        text = stringResource(R.string.island_call_incoming),
                        color = IslandPalette.secondaryLabel,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.island_call_ongoing), color = IslandPalette.secondaryLabel, fontSize = 13.sp, maxLines = 1)
                        CallTimer(postTimeMillis = notification?.timeMillis ?: System.currentTimeMillis(), color = callColor)
                    }
                }
            }
            if (isRinging) {
                val decline = notification.actionFor(IslandControl.CallDecline)
                val accept = notification.actionFor(IslandControl.CallAnswer)
                if (decline != null) {
                    CircleActionButton(
                        color = IslandPalette.danger,
                        icon = Icons.Rounded.CallEnd,
                        label = stringResource(R.string.island_call_decline)
                    ) {
                        fireIslandControl(context, notification, decline, GatedAction.CallControl, removeAlert = true, onDone = onCollapse)
                    }
                }
                if (accept != null) {
                    CircleActionButton(
                        color = callColor,
                        icon = Icons.Rounded.Call,
                        label = stringResource(R.string.island_call_accept)
                    ) {
                        // The card stays open and turns into the Active Call card when the call's notification updates.
                        fireIslandControl(context, notification, accept, GatedAction.CallControl, removeAlert = false)
                    }
                }
            }
        }

        if (!isRinging && notification != null) {
            val mute = notification.actionFor(IslandControl.CallMute)
            val speaker = notification.actionFor(IslandControl.CallSpeaker)
            val end = notification.actionFor(IslandControl.CallEnd)
            if (mute != null || speaker != null || end != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (mute != null) {
                        IslandRoundButton(stringResource(R.string.island_call_mute), Icons.Rounded.MicOff, IslandPalette.fillStrong) {
                            fireIslandControl(context, notification, mute, GatedAction.CallControl, removeAlert = false)
                        }
                    }
                    if (speaker != null) {
                        IslandRoundButton(stringResource(R.string.island_call_speaker), Icons.AutoMirrored.Rounded.VolumeUp, IslandPalette.fillStrong) {
                            fireIslandControl(context, notification, speaker, GatedAction.CallControl, removeAlert = false)
                        }
                    }
                    if (end != null) {
                        IslandRoundButton(stringResource(R.string.island_call_end), Icons.Rounded.CallEnd, IslandPalette.danger) {
                            fireIslandControl(context, notification, end, GatedAction.CallControl, removeAlert = true, onDone = onCollapse)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
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
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
