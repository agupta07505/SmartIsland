package com.srcpolygot.dynamicisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun BatteryExpanded(
    notification: IslandNotification,
    isCharging: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF1C1C1E))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isCharging) "Charging" else "Battery",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = notification.title ?: "—%",
            color = Color(0xFF30D158),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        notification.text?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = it, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }
    }
}
