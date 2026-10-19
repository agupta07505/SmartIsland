/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.IslandPalette
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.GatedAction
import com.agupta07505.smartisland.util.IslandControl
import com.agupta07505.smartisland.util.KeyguardGate
import com.agupta07505.smartisland.util.NotificationActionMatcher

/** The notification's own action behind [control], or null when the app does not offer it (the button hides). */
fun IslandNotification?.actionFor(control: IslandControl): IslandNotificationAction? =
    this?.let { NotificationActionMatcher.find(it.actionIntents, control) }

/**
 * Fires [action] through the lockscreen gate with a safe [gate] (calls, alarms, recorders all work without
 * unlock, Rule_Book §8.3). [removeAlert] takes the alert off the island at once (Stop / Snooze, Decline); the
 * source app's next update or removal brings the island in step otherwise.
 */
fun fireIslandControl(
    context: Context,
    notification: IslandNotification,
    action: IslandNotificationAction,
    gate: GatedAction,
    removeAlert: Boolean,
    onDone: () -> Unit = {}
) {
    KeyguardGate.run(context, gate) {
        action.pendingIntent?.let { sendIntentWithOptions(context, it) }
        val repository = SmartIslandRepositories.notificationRepository(context)
        repository.resetTimer()
        if (removeAlert) repository.removeNotification(notification.key)
        onDone()
    }
}

/** A labelled pill button (Snooze, Stop, Pause, Mute...). */
@Composable
internal fun IslandPillButton(
    label: String,
    icon: ImageVector?,
    background: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A round icon button with a caption under it (call controls). */
@Composable
internal fun IslandRoundButton(
    label: String,
    icon: ImageVector,
    background: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(background)
                .bounceClick(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(text = label, color = IslandPalette.secondaryLabel, fontSize = 11.sp, maxLines = 1)
    }
}
