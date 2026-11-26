/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.DeviceStates
import com.agupta07505.smartisland.model.HotspotText
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PulseValueText
import com.agupta07505.smartisland.model.StateWords
import com.agupta07505.smartisland.ui.components.BatteryVisual
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
import com.agupta07505.smartisland.ui.expanded.SlashableIcon
import com.agupta07505.smartisland.ui.expanded.connectivityAccent
import com.agupta07505.smartisland.ui.expanded.connectivityLeadingIcon
import com.agupta07505.smartisland.ui.expanded.isConnectionLost
import com.agupta07505.smartisland.ui.expanded.isWiredCategory
import kotlinx.coroutines.delay
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
internal fun HotspotCollapsedGlyph(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val hotspotColor = Color(settings.hotspotColor)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(hotspotColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiTethering,
            contentDescription = "Hotspot",
            tint = hotspotColor,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
internal fun HotspotCollapsedRight(
    notification: IslandNotification?,
    settings: SmartIslandSettings,
    label: Boolean = false
) {
    val count = remember(notification?.text, notification?.title) {
        HotspotText.parseDeviceCountOrNull(notification?.title, notification?.text)
    }
    if (label) {
        Text(
            text = PulseValueText.hotspot(count),
            color = Color(settings.hotspotColor),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        return
    }
    if (count == null) return

    Text(
        text = count.toString(),
        color = Color(settings.hotspotColor),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

/** The demo's glyph for battery events: a tinted circle with the state icon, the same in every layout. */
@Composable
internal fun BatteryCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings) {
    val pct = notification?.text?.replace("%", "")?.trim()?.toFloatOrNull() ?: 49f
    val visual = resolveBatteryVisual(notification?.category, notification?.title, pct)
    // plugged in and unplugged share the bolt: the opposite event takes over by the tint alone
    val accent = morphColor(batteryVisualColor(visual, Color(settings.batteryColor), settings.cardEventColor))
    val icon = when (visual) {
        BatteryVisual.PluggedIn, BatteryVisual.PluggedOut -> Icons.Rounded.Bolt
        BatteryVisual.Full -> Icons.Rounded.BatteryFull
        BatteryVisual.Low -> Icons.Rounded.BatteryAlert
        BatteryVisual.Saver -> Icons.Rounded.BatterySaver
    }
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        MorphSwap(icon) { shown ->
            Icon(
                imageVector = shown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** The demo's glyph for connection events: a tinted circle with the connection icon, ringed by the device battery. */
@Composable
internal fun ConnectionCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings) {
    // an opposite event (Wi-Fi lost, USB unplugged) takes over the running pulse: the tint eases, the slash draws on
    // or wipes off, an icon that differs (the VPN key) cross-fades
    val accent = morphColor(connectivityAccent(notification?.category, settings.bluetoothColor, settings.cardEventColor))
    val slash by animateFloatAsState(
        targetValue = if (StateWords.slashed(notification?.category)) 1f else 0f,
        animationSpec = pulseMorphSpec(),
        label = "PulseMorphSlash"
    )
    val deviceBattery = if (settings.showBluetoothBattery && isPlainBluetooth(notification)) {
        bluetoothBatteryLevel(notification)
    } else null
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (deviceBattery != null) {
            SmoothCircularRingProgress(
                progress = deviceBattery / 100f,
                color = accent,
                strokeWidthDp = GLYPH_RING_STROKE_DP,
                trackColor = accent.copy(alpha = 0.25f),
                modifier = Modifier.matchParentSize()
            )
        }
        // The off states carry the slash on the glyph, as the demo draws it
        MorphSwap(
            connectivityLeadingIcon(notification?.category),
            direction = rememberMorphDirection(notification?.category)
        ) { shown ->
            SlashableIcon(
                imageVector = shown,
                contentDescription = "Connection",
                tint = accent,
                slashed = StateWords.slashed(notification?.category),
                slashAmount = slash,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** The connected device's battery in percent, from the card's progress or a "NN%" in its text. */
internal fun bluetoothDeviceBattery(notif: IslandNotification): Int? =
    if (notif.progress in 1..100 && notif.progressMax == 100) notif.progress
    else Regex("""(\d{1,3})%""").find(notif.text)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)

/**
 * True when the collapsed value of [card] is a pure icon (the Bluetooth device picture, the transfer ring). The edge pill keeps such a value upright, like its glyph, and turns only text
 * along the strip. The Bluetooth card that swaps the picture for a battery reading stays turned, it carries text
 */
internal fun collapsedValueIsIcon(card: IslandNotification, settings: SmartIslandSettings): Boolean = when (card.mode) {
    IslandMode.DownloadUpload -> true
    // A connection or device-state card has a state word as its pulse value: text, which turns along the strip
    IslandMode.Bluetooth ->
        StateWords.of(card) == null && (!settings.showBluetoothBattery || bluetoothDeviceBattery(card) == null)
    else -> false
}

/** Turns an edge pill's value along the strip, unless it is an icon, which stays upright like the glyph. */
internal fun Modifier.valueAlongStrip(card: IslandNotification?, settings: SmartIslandSettings): Modifier =
    if (card != null && collapsedValueIsIcon(card, settings)) this else alongStrip()

@Composable
fun BluetoothCollapsedRight(
    notification: IslandNotification?,
    settings: SmartIslandSettings
) {
    // A connection or device-state card (Wi-Fi, VPN, USB, wired headphones, airplane mode, the ringer modes, Do Not
    // Disturb, modes) is told by its glyph alone: the state word belongs to the wide pulse ([PeakValue]), so the settled
    // pill, the bubble's mini pill and a card that skips its pulse draw nothing here
    if (notification != null && StateWords.of(notification) != null) return
    val batteryLevel = remember(notification?.text, notification?.progress) {
        notification?.let { bluetoothDeviceBattery(it) }
    }

    if (!settings.showBluetoothBattery || batteryLevel == null) {
        Image(
            painter = painterResource(id = R.drawable.ic_bluetooth_device),
            contentDescription = "Bluetooth Device",
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
        )
    } else {
        var showBattery by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000L)
                showBattery = !showBattery
            }
        }

        AnimatedContent(
            targetState = showBattery,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)) +
                        scaleIn(initialScale = 0.72f, animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f)) +
                                scaleOut(targetScale = 0.72f, animationSpec =
                                    spring(stiffness = 520f, dampingRatio = 0.72f))
                    )
            },
            label = "BluetoothCollapsedSwitch"
        ) { isBatteryState ->
            if (isBatteryState) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val batteryColor = if (batteryLevel <= 20) Color(0xFFEF4444) else Color(0xFF10B981)
                    val batteryIcon = when {
                        batteryLevel >= 80 -> Icons.Rounded.BatteryFull
                        batteryLevel >= 40 -> Icons.Rounded.Battery5Bar
                        batteryLevel >= 20 -> Icons.Rounded.BatteryChargingFull
                        else -> Icons.Rounded.BatteryAlert
                    }
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = "Battery",
                        tint = batteryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$batteryLevel%",
                        color = batteryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.bluetoothColor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = "Earbuds",
                        tint = Color(settings.bluetoothColor),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

internal fun isPlainBluetooth(notification: IslandNotification?): Boolean {
    val category = notification?.category
    return category != "wifi" && category != "wifi_disconnected" && category != "vpn" &&
        !isWiredCategory(category) &&
        !isConnectionLost(category) &&
        !DeviceStates.isDeviceState(category)
}

internal fun bluetoothBatteryLevel(notification: IslandNotification?): Int? = notification?.let { notif ->
    if (notif.progress in 1..100 && notif.progressMax == 100) notif.progress
    else Regex("(\\d{1,3})%").find(notif.text)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)
}
