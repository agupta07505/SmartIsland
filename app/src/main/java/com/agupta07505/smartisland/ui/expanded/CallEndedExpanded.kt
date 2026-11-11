/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CallEndReason
import com.agupta07505.smartisland.model.CallEnds
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.CallEndedGlyph
import com.agupta07505.smartisland.ui.CallHangUpColor
import com.agupta07505.smartisland.ui.IslandText as Text

/** A little taller than the incoming call card: caller, how the call ended, and how long it lasted. */
@Composable
fun CallEndedExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val end = notification?.callEnd
    val hungUp = end?.reason == CallEndReason.ByOther
    val palette = LocalIslandPalette.current
    val caller = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(min = 84.dp, max = 130.dp)
            .padding(start = 18.dp, top = 22.dp, end = 18.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallEndedGlyph(
            notification = notification,
            size = 48.dp,
            idleFill = palette.tint(0.2f),
            idleTint = palette.text
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = caller,
                color = palette.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(if (hungUp) R.string.call_hung_up else R.string.call_ended),
                color = if (hungUp) CallHangUpColor else palette.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = CallEnds.format(end?.durationMs ?: 0L),
                    color = Color(settings.callColor),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = stringResource(R.string.call_duration),
                color = palette.textMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
