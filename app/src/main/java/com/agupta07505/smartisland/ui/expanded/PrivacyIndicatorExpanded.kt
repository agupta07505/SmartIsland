/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.agupta07505.smartisland.util.GatedAction
import com.agupta07505.smartisland.util.KeyguardGate
import com.agupta07505.smartisland.util.PrivacyIndicatorAlert
import com.agupta07505.smartisland.util.runCatchingLogged

/** Android's own privacy-indicator colours: green for the camera, orange for the microphone. */
object PrivacyColors {
    val camera = Color(0xFF00C853)
    val microphone = Color(0xFFFF9100)
}

/**
 * Non-expanded small state (the companion bubble): a solid green dot (camera), orange dot (microphone), or one
 * split green/orange pill when both are on (Rule_Book §4.15, §4.16).
 */
@Composable
fun PrivacyIndicatorDot(notification: IslandNotification?, modifier: Modifier = Modifier) {
    val sensors = PrivacyIndicatorAlert.sensorsOf(notification)
    if (sensors.both) {
        Row(modifier = modifier.clip(RoundedCornerShape(50))) {
            Box(Modifier.size(width = 8.dp, height = 10.dp).background(PrivacyColors.camera))
            Box(Modifier.size(width = 8.dp, height = 10.dp).background(PrivacyColors.microphone))
        }
    } else {
        Box(
            modifier = modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (sensors.microphone) PrivacyColors.microphone else PrivacyColors.camera)
        )
    }
}

/** Expanded small state, left: the green camera and/or orange microphone icon. */
@Composable
fun PrivacyIndicatorCollapsedGlyph(notification: IslandNotification?) {
    val sensors = PrivacyIndicatorAlert.sensorsOf(notification)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (sensors.camera || !sensors.microphone) {
            Icon(
                Icons.Rounded.Videocam,
                contentDescription = stringResource(R.string.island_camera_in_use),
                tint = PrivacyColors.camera,
                modifier = Modifier.size(16.dp)
            )
        }
        if (sensors.microphone) {
            Icon(
                Icons.Rounded.Mic,
                contentDescription = stringResource(R.string.island_microphone_in_use),
                tint = PrivacyColors.microphone,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/** Expanded small state, right: "Camera in use" / "Microphone in use" / "Camera & mic in use". */
@Composable
fun PrivacyIndicatorCollapsedRight(notification: IslandNotification?) {
    val sensors = PrivacyIndicatorAlert.sensorsOf(notification)
    Text(
        text = privacyLabel(sensors),
        color = if (sensors.microphone && !sensors.camera) PrivacyColors.microphone else PrivacyColors.camera,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
private fun privacyLabel(sensors: PrivacyIndicatorAlert.Sensors): String = when {
    sensors.both -> stringResource(R.string.island_camera_and_microphone_in_use)
    sensors.microphone -> stringResource(R.string.island_microphone_in_use)
    else -> stringResource(R.string.island_camera_in_use)
}

/**
 * Medium state (340dp): the sensor icon, "<App> is using your camera" (and/or microphone) and a shortcut to the
 * OS privacy dashboard. Opening Settings leaves the lockscreen, so it waits for unlock (Rule_Book §8.3).
 */
@Composable
fun PrivacyIndicatorExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val sensors = PrivacyIndicatorAlert.sensorsOf(notification)
    val cameraOn = sensors.camera || !sensors.microphone
    val accent = if (cameraOn) PrivacyColors.camera else PrivacyColors.microphone
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (cameraOn) Icons.Rounded.Videocam else Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                if (sensors.both) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(PrivacyColors.microphone),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(9.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (cameraOn) {
                    PrivacyLine(
                        text = notification?.title?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.island_app_using_camera, it) }
                            ?: stringResource(R.string.island_unknown_app_using_camera),
                        color = Color.White
                    )
                }
                if (sensors.microphone) {
                    PrivacyLine(
                        text = notification?.text?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.island_app_using_microphone, it) }
                            ?: stringResource(R.string.island_unknown_app_using_microphone),
                        color = if (cameraOn) IslandPalette.secondaryLabel else Color.White
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IslandPillButton(
                label = stringResource(R.string.island_privacy_dashboard),
                icon = Icons.Rounded.PrivacyTip,
                background = IslandPalette.fill
            ) {
                KeyguardGate.run(context, GatedAction.OpenApp, onCollapse) {
                    openPrivacyDashboard(context)
                    onCollapse()
                }
            }
        }
    }
}

@Composable
private fun PrivacyLine(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * The OS privacy dashboard: the permission controller's usage screen where available, else Privacy settings,
 * else Settings. Each step is tried in turn because OEMs remove or protect them.
 */
fun openPrivacyDashboard(context: Context) {
    val candidates = buildList {
        add(Intent(ACTION_REVIEW_PERMISSION_USAGE))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Intent(Settings.ACTION_PRIVACY_SETTINGS))
        add(Intent(Settings.ACTION_SETTINGS))
    }
    for (intent in candidates) {
        val started = runCatchingLogged("PrivacyIndicator", "Opening ${intent.action} failed") {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }
        if (started == true) return
    }
}

private const val ACTION_REVIEW_PERMISSION_USAGE = "android.intent.action.REVIEW_PERMISSION_USAGE"
