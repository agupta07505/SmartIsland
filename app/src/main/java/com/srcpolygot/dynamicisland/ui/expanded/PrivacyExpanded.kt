/*
 * Realistic Dynamic Island – Privacy indicators (Camera / Mic)
 * Matches iPhone 18 Pro Max orange/green privacy dots behaviour
 */

package com.srcpolygot.dynamicisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srcpolygot.dynamicisland.domain.model.IslandMode
import com.srcpolygot.dynamicisland.domain.model.IslandNotification

@Composable
fun PrivacyExpanded(
    notification: IslandNotification,
    modifier: Modifier = Modifier
) {
    val isCamera = notification.mode == IslandMode.CameraInUse
    val accent = Color(0xFFFF3B30) // Apple-style privacy red/orange

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF1C1C1E))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isCamera) Icons.Rounded.CameraAlt else Icons.Rounded.Mic,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .padding(6.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isCamera) "Camera in Use" else "Microphone in Use",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = notification.title ?: notification.packageName,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}
