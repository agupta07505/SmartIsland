/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.bounceClick

/**
 * The notification's own buttons (with a PendingIntent) that a card did not already show with a dedicated control:
 * exactly the buttons the app put in the shade.
 */
fun IslandNotification?.leftoverActions(vararg shown: IslandNotificationAction?): List<IslandNotificationAction> {
    if (this == null) return emptyList()
    return actionIntents.filter { act -> act.pendingIntent != null && shown.none { it === act } }
}

/** A small text button for one of the app's own notification buttons; fires it like the shade does. */
@Composable
fun ShadeActionChip(
    notification: IslandNotification,
    action: IslandNotificationAction,
    accent: Color
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .height(36.dp)
            .widthIn(max = 84.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.2f))
            .bounceClick { notification.fireAction(context, action) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action.title,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
