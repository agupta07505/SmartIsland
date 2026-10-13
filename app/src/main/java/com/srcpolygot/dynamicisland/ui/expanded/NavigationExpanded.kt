/*
 * Realistic Dynamic Island – Navigation expanded card
 */

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
fun NavigationExpanded(
    notification: IslandNotification,
    distance: String = "",
    eta: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF1C1C1E))
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Navigation",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = notification.title ?: "Continue straight",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOfNotNull(
                distance.takeIf { it.isNotBlank() },
                eta.takeIf { it.isNotBlank() },
                notification.text
            ).joinToString(" · "),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.sp
        )
    }
}
