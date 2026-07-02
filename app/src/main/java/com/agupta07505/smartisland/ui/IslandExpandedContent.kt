package com.agupta07505.smartisland.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

@Composable
fun IslandExpandedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (mode) {
            IslandMode.Notification -> NotificationExpanded(notification)
            IslandMode.IncomingCall -> IncomingCallExpanded()
            IslandMode.Music -> MusicExpanded()
            IslandMode.Empty -> EmptyExpanded()
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun EmptyExpanded() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 36.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Smart Island", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Ready for notifications", color = Color(0xFFB7C0CA), fontSize = 13.sp)
    }
}

@Composable
private fun NotificationExpanded(notification: IslandNotification?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 34.dp, end = 18.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = notification?.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(notification?.appName?.firstOrNull()?.uppercase() ?: "S", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Notification",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity",
                color = Color(0xFFD5DAE0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            if (!notification?.actions.isNullOrEmpty()) {
                Text(
                    text = notification!!.actions.take(2).joinToString("  |  "),
                    color = Color(0xFF9CC7FF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = notification?.let { formatNotificationTime(it.timeMillis) } ?: "",
            color = Color(0xFFB7C0CA),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun IncomingCallExpanded() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 28.dp, end = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CallerName",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        CircleActionButton(color = Color(0xFFE11D48), icon = Icons.Rounded.Close)
        CircleActionButton(color = Color(0xFF79C943), icon = Icons.Rounded.Call)
    }
}

@Composable
private fun MusicExpanded() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 30.dp, end = 18.dp, bottom = 10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF6B9A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("SongName", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("ArtistName", color = Color(0xFFD5DAE0), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("02:05", color = Color.White, fontSize = 10.sp)
            LinearProgressIndicator(progress = { 0.58f }, modifier = Modifier.weight(1f).height(3.dp), color = Color.White, trackColor = Color(0xFF667085))
            Text("05:47", color = Color.White, fontSize = 10.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) { Icon(Icons.Rounded.Replay, contentDescription = null, tint = Color.White) }
            IconButton(onClick = {}) { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)) }
            IconButton(onClick = {}) { Icon(Icons.Rounded.FastForward, contentDescription = null, tint = Color.White) }
        }
    }
}

@Composable
private fun CircleActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
