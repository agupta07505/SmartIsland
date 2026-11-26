/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.CallEndReason
import com.agupta07505.smartisland.model.IslandNotification

internal val CallHangUpColor = Color(0xFFE11D48)

/** End-call icon on a disc: red for a hang-up from the island, a quiet disc for a call that simply ended. */
@Composable
internal fun CallEndedGlyph(
    notification: IslandNotification?,
    size: Dp = 22.dp,
    idleFill: Color = Color.White.copy(alpha = 0.18f),
    idleTint: Color = Color.White
) {
    val hungUp = notification?.callEnd?.reason == CallEndReason.ByOther
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (hungUp) CallHangUpColor else idleFill),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.CallEnd,
            contentDescription = null,
            tint = if (hungUp) Color.White else idleTint,
            modifier = Modifier.size(size * 0.64f)
        )
    }
}
