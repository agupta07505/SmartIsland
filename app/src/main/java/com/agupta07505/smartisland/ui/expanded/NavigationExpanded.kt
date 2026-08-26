/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
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

    val navInfo = remember(notification) {
        if (notification == null) {
            Tuple3("In 300 m", "Turn left onto MG Road", "24 min remaining • 8.5 km", TurnDirection.LEFT)
        } else {
            val title = notification.title
            val text = notification.text
            val combined = "$title $text".lowercase()

            val pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(combined)
            val dist = if (matcher.find()) "In ${matcher.group(0)}" else "In 200 m"
            val turnDir = NavigationParser.parseTurnDirection(combined)

            val mainTitle = if (title.isNotBlank()) title else "Navigation"
            val subText = if (text.isNotBlank()) text else "Turn-by-turn guidance"

            Tuple3(dist, mainTitle, subText, turnDir)
        }
    }

    val (distanceText, maneuverTitle, subStatusText, turnDirection) = navInfo

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
            .wrapContentHeight()
            .clickable { onOpenNotification() }
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Section: App Icon + App Name & ETA Subtitle + Navigation Badge
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subStatusText,
                    color = Color(0xFFD5DAE0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            // Distance / Maneuver Badge
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

        // Maneuver Direction Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E293B))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(navColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (turnDirection == TurnDirection.DESTINATION) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = navColor,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = navColor,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(angle)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = maneuverTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = distanceText,
                    color = navColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action Buttons Row & Collapse Arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notification != null && notification.actionIntents.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    notification.actionIntents.take(2).forEach { action ->
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE2E8F0))
                                .bounceClick {
                                    if (action.pendingIntent != null) {
                                        triggerAction(context, notification.packageName, action.pendingIntent, action.title, notification.contentIntent)
                                    } else {
                                        Toast.makeText(context, "Clicked: ${action.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    val repo = SmartIslandRepositories.notificationRepository(context)
                                    repo.removeNotification(notification.key)
                                    repo.sendCommand(com.agupta07505.smartisland.data.SmartIslandCommand.CancelNotification(notification.key))
                                    onCollapse()
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

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .bounceClick { onOpenNotification() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Open App",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class Tuple3<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
