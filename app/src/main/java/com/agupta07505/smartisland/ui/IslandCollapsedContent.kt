package com.agupta07505.smartisland.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IslandCollapsedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (mode) {
                IslandMode.Notification -> {
                    NotificationGlyph(notification = notification)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = notification?.title?.takeIf { it.isNotBlank() }
                            ?: notification?.appName
                            ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.size(32.dp))
                }
                IslandMode.IncomingCall -> {
                    Icon(Icons.Rounded.Call, contentDescription = null, tint = Color(0xFF7FD35E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        notification?.title?.takeIf { it.isNotBlank() } ?: "Call",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.size(34.dp))
                }
                IslandMode.Music -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6B9A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.size(40.dp))
                    Text(
                        notification?.title?.takeIf { it.isNotBlank() } ?: "Music",
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IslandMode.Empty -> Unit
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun NotificationGlyph(notification: IslandNotification?) {
    val icon = notification?.icon
    if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification?.appName?.firstOrNull()?.uppercase() ?: "S",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun formatNotificationTime(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
