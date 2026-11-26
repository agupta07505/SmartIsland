/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.agupta07505.smartisland.model.HotspotText
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.HotspotStateMonitor
import com.agupta07505.smartisland.util.HotspotUtil
import com.agupta07505.smartisland.ui.IslandText as Text

private val TURN_OFF_WORDS = listOf(
    "off", "disable", "stop", "apagar", "desactivar", "desligar", "desativar", "ausschalten", "deaktivieren",
    "désactiver", "éteindre", "disattiva", "关闭", "關閉", "オフ", "無効", "отключить", "выключить"
)

/** Hotspot card: a small accent glyph */
@Composable
fun HotspotExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    val accentColor = Color(settings.hotspotColor)

    var turnedOff by remember(notification.key) { mutableStateOf(false) }

    val knownCount = remember(notification.text, notification.title) {
        HotspotText.parseDeviceCountOrNull(notification.title, notification.text)
    }
    // The pulse and card used to show 0 when the device supplied no count.
    // The card shows Active in that case.
    val deviceCountText = knownCount?.let { "$it connected" } ?: "Active"

    val actions = remember(notification.actionIntents) {
        notification.actionIntents.filter { it.pendingIntent != null }
    }
    val powerAction: IslandNotificationAction? = remember(actions) {
        actions.firstOrNull { action -> TURN_OFF_WORDS.any { action.title.contains(it, ignoreCase = true) } }
            ?: actions.singleOrNull()
    }
    val otherActions = remember(actions, powerAction) { actions.filter { it !== powerAction } }

    val deviceCount = knownCount ?: 0
    val glyphFill = if (turnedOff) palette.tint(0.2f) else accentColor
    val glyphTint = if (turnedOff) palette.textSecondary else Color(0xFF1A1000)
    val labelColor = if (turnedOff) palette.textSecondary else accentColor
    val avatarFill = palette.pick(Color(0xFF262626), Color(0xFFD9D8DB))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable {
                HotspotUtil.openHotspotSettings(context, notification.contentIntent)
                onCollapse()
            }
            .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HotspotGlyph(glyphFill = glyphFill, glyphTint = glyphTint)

            HotspotLabels(
                turnedOff = turnedOff,
                labelColor = labelColor,
                deviceCountText = deviceCountText
            )

            if (!turnedOff && deviceCount > 0) {
                HotspotAvatars(deviceCount = deviceCount, accentColor = accentColor, avatarFill = avatarFill)
            }
        }

        HotspotActionRow(
            notification = notification,
            powerAction = powerAction,
            ownCard = notification.key == HotspotStateMonitor.KEY_HOTSPOT,
            turnedOff = turnedOff,
            accentColor = accentColor,
            onTurnedOff = { turnedOff = true },
            onCollapse = onCollapse
        )

        if (otherActions.isNotEmpty() && !turnedOff) {
            HotspotOtherActions(notification = notification, otherActions = otherActions)
        }
    }
}

@Composable
private fun HotspotGlyph(glyphFill: Color, glyphTint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(glyphFill),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiTethering,
            contentDescription = "Hotspot",
            tint = glyphTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun RowScope.HotspotLabels(
    turnedOff: Boolean,
    labelColor: Color,
    deviceCountText: String
) {
    val palette = LocalIslandPalette.current
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = if (turnedOff) "Hotspot off" else "Hotspot on",
            color = labelColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = if (turnedOff) "Turned off" else deviceCountText,
            color = palette.text,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HotspotAvatars(deviceCount: Int, accentColor: Color, avatarFill: Color) {
    val palette = LocalIslandPalette.current
    val shown = minOf(deviceCount, 3)
    val extra = deviceCount - shown
    Row {
        repeat(shown) { i ->
            Box(
                modifier = Modifier
                    .offset(x = (-8 * i).dp)
                    .size(28.dp)
                    // .shadow(2.dp, CircleShape) // separate overlapping devices
                    .clip(CircleShape)
                    .background(avatarFill)
                    .border(2.dp, palette.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (i % 2 == 0) Icons.Rounded.PhoneAndroid else Icons.Rounded.Laptop,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-8 * shown).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(avatarFill)
                    .border(2.dp, palette.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+$extra", color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun HotspotActionRow(
    notification: IslandNotification,
    powerAction: IslandNotificationAction?,
    ownCard: Boolean,
    turnedOff: Boolean,
    accentColor: Color,
    onTurnedOff: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (powerAction != null && !turnedOff) {
            PillButton(
                glyph = Icons.Rounded.PowerSettingsNew,
                label = "Turn off",
                description = powerAction.title,
                fill = accentColor,
                content = Color(0xFF1A1000),
                modifier = Modifier.weight(1f),
                onClick = {
                    // Like the shade: fire the app's button
                    if (notification.fireAction(context, powerAction)) onTurnedOff()
                }
            )
        }
        if (powerAction == null && ownCard && !turnedOff) {
            PillButton(
                glyph = Icons.Rounded.PowerSettingsNew,
                label = "Turn off",
                description = "Turn off hotspot",
                fill = accentColor,
                content = Color(0xFF1A1000),
                modifier = Modifier.weight(1f),
                onClick = {
                    HotspotUtil.turnOffFromCard(context)
                    onCollapse()
                }
            )
        }
        PillButton(
            glyph = Icons.Rounded.Settings,
            label = "Settings",
            description = "Hotspot settings",
            fill = palette.tint(0.2f),
            content = palette.text,
            modifier = Modifier.weight(1f),
            onClick = {
                HotspotUtil.openHotspotSettings(context, notification.contentIntent)
                onCollapse()
            }
        )
    }
}

@Composable
private fun HotspotOtherActions(notification: IslandNotification, otherActions: List<IslandNotificationAction>) {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        otherActions.forEach { action ->
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.tint(0.2f))
                    .bounceClick { notification.fireAction(context, action) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = action.title,
                    color = palette.text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    glyph: ImageVector,
    label: String,
    description: String,
    fill: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(fill)
            .bounceClick(onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = glyph, contentDescription = description, tint = content, modifier = Modifier.size(16.dp))
        Text(text = label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
