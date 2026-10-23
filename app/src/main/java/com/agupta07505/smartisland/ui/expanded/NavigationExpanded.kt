/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.NavigationCardText
import com.agupta07505.smartisland.util.NavigationParser
import com.agupta07505.smartisland.util.TurnDirection

@Composable
fun NavigationExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit = {},
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val navColor = Color(settings.navigationColor)

    val card = remember(notification) {
        if (notification == null) {
            NavigationCardText(
                distance = "In 300 m",
                instruction = "Turn left onto MG Road",
                detail = "Stay in the left lane",
                eta = "24 min remaining \u2022 8.5 km"
            )
        } else {
            NavigationParser.cardText(notification.title, notification.text)
        }
    }
    val turnDirection = remember(notification) {
        if (notification == null) TurnDirection.LEFT
        else NavigationParser.parseTurnDirection("${notification.title} ${notification.text}".lowercase())
    }
    val distanceText = card.distance

    val angle = when (turnDirection) {
        TurnDirection.LEFT -> -90f
        TurnDirection.RIGHT -> 90f
        TurnDirection.SLIGHT_LEFT -> -45f
        TurnDirection.SLIGHT_RIGHT -> 45f
        TurnDirection.U_TURN -> 180f
        else -> 0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NAVIGATION_EXPANDED_HEIGHT)
            .clickable { onOpenNotification() }
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val largeIcon = notification?.largeIcon
            val icon = notification?.icon
            val mainIcon = largeIcon ?: icon

            if (mainIcon != null) {
                Box(modifier = Modifier.size(42.dp)) {
                    Image(
                        bitmap = mainIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(navColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification?.appName ?: "Google Maps",
                    color = LocalIslandPalette.current.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = card.eta.ifEmpty { "Turn-by-turn guidance" },
                    color = LocalIslandPalette.current.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(navColor.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = distanceText,
                    color = navColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LocalIslandPalette.current.pick(Color(0xFF1E293B), Color(0xFFE4E2E6)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(NAVIGATION_ARROW_CIRCLE)
                    .clip(CircleShape)
                    .background(navColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (turnDirection == TurnDirection.DESTINATION) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = navColor,
                        modifier = Modifier.size(NAVIGATION_ARROW_ICON)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = navColor,
                        modifier = Modifier
                            .size(NAVIGATION_ARROW_ICON)
                            .rotate(angle)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.instruction,
                    color = LocalIslandPalette.current.text,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.detail.isNotEmpty()) {
                    Text(
                        text = card.detail,
                        color = LocalIslandPalette.current.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = distanceText,
                    color = navColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notification != null && notification.actionIntents.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    // Every button the app put in its notification, not just the first two.
                    notification.actionIntents.forEach { action ->
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LocalIslandPalette.current.chipFill)
                                .bounceClick {
                                    // Like the shade: fire the app's button and leave the card to the app,
                                    // which removes or updates its notification itself.
                                    notification.fireAction(context, action)
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action.title,
                                color = Color(0xFF1F2937),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private val NAVIGATION_ARROW_CIRCLE = 56.dp
private val NAVIGATION_ARROW_ICON = 36.dp

internal val NAVIGATION_EXPANDED_HEIGHT = 240.dp
