/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick

@Composable
fun IncomingCallExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    // FIX: Standardized to match Notification/Music expanded – same padding (18,20,12,bottom),
    // constrained height (80-110dp), title 17sp with maxLines 1, and 48dp action buttons.
    // Previously used 20sp + 52dp buttons causing taller expansion and glitch.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(min = 64.dp, max = 110.dp)
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = notification?.title?.takeIf { it.isNotBlank() }
                ?: notification?.text?.takeIf { it.isNotBlank() }
                ?: "Incoming call",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        CircleActionButton(
            color = Color(0xFFE11D48),
            icon = Icons.Rounded.Close,
            onClick = { 
                notification.sendFirstAction(context, "decline", "reject", "hang", "end") 
                onCollapse()
            }
        )
        if (notification?.actionIntents?.any { action ->
                action.title.contains("answer", ignoreCase = true) ||
                action.title.contains("accept", ignoreCase = true)
            } == true) {
            CircleActionButton(
                color = Color(0xFF79C943),
                icon = Icons.Rounded.Call,
                onClick = { 
                    notification.sendFirstAction(context, "answer", "accept") 
                    onCollapse()
                }
            )
        }
    }
}

@Composable
private fun CircleActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
