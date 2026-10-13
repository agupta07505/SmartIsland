package com.srcpolygot.dynamicisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srcpolygot.dynamicisland.domain.model.IslandNotification

@Composable
fun CallExpanded(
    notification: IslandNotification,
    isIncoming: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isIncoming) "Incoming Call" else "On Call",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = notification.title ?: "Unknown",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        notification.text?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = it, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row {
            if (isIncoming) {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158))
                ) {
                    Icon(Icons.Rounded.Call, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(24.dp))
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
            ) {
                Icon(Icons.Rounded.CallEnd, null, tint = Color.White)
            }
        }
    }
}
